import logging
import logging.config

import ServerSideExtension_pb2 as SSE
import grpc
import numpy
import pandas
from SSEData_scriptPandas import ArgType,       \
                                 FunctionType,  \
                                 ReturnType


class ScriptEval:
    """
    Class for SSE plugin ScriptEval functionality.
    """

    def EvaluateScript(self, header, request, context):
        """
        Evaluates script provided in the header, given the
        arguments provided in the sequence of RowData objects, the request.

        :param header:
        :param request: an iterable sequence of RowData.
        :param context: the context sent from client
        :return: an iterable sequence of RowData.
        """
        # Retrieve function type
        func_type = self.get_func_type(header)

        # Retrieve data types from header
        arg_types = self.get_arg_types(header)
        ret_type = self.get_return_type(header)

        logging.info('EvaluateScript: {} ({} {}) {}'
                     .format(header.script, arg_types, ret_type, func_type))

        # Create a panda data frame, for retrieved parameters
        q = pandas.DataFrame()

        # Check if parameters are provided
        if header.params:
            # Iterate over bundled rows
            for request_rows in request:
                # Iterate over rows
                for row in request_rows.rows:
                    # Retrieve parameters and append to data frame
                    params, dual_exist = self.get_arguments(context, arg_types, row.duals, header)
                    q = q.append(params, ignore_index=True)

            # Rename columns based on arg names in header
            arg_names = [param.name for param in header.params]
            if dual_exist:
                # find what column(s) are dual
                param_types = [param.dataType for param in header.params]
                col_index = [i for i, arg_type in enumerate(param_types) if arg_type == SSE.DUAL]
                # add _num and _str columns representing the dual column
                # for an easier access in the script
                for col in col_index:
                    arg_names.insert(col + 1, arg_names[col] + '_str')
                    arg_names.insert(col + 2, arg_names[col] + '_num')
            q.rename(columns=lambda i: arg_names[i], inplace=True)

            yield self.evaluate(context, header.script, ret_type, q)

        else:
            # No parameters provided
            yield self.evaluate(context, header.script, ret_type, q)

    @staticmethod
    def get_func_type(header):
        """
        Retrieves the function type.
        :param header:
        :return:
        """
        func_type = header.functionType
        if func_type == SSE.SCALAR:
            return FunctionType.Scalar
        elif func_type == SSE.AGGREGATION:
            return FunctionType.Aggregation
        elif func_type == SSE.TENSOR:
            return FunctionType.Tensor

    @staticmethod
    def raise_grpc_error(context, status_code, msg):
        # Make sure the error handling, including logging, works as intended in the client
        context.set_code(status_code)
        context.set_details(msg)
        # Raise error on the plugin-side
        raise grpc.RpcError(status_code, msg)

    def get_arguments(self, context, arg_types, duals, header):
        """
        Gets the array of arguments based on
        the duals, and the type (string, numeric)
        specified in the header.
        :param context: the context sent from client
        :param arg_types: the argument data type
        :param duals: an iterable sequence of duals.
        :param header: the script header.
        :return: a panda Series containing (potentially mixed data type) arguments.
        """
        dual_type = False
        if arg_types == ArgType.String:
            # All parameters are of string type
            script_args = [d.strData for d in duals]
        elif arg_types == ArgType.Numeric:
            # All parameters are of numeric type
            script_args = [d.numData for d in duals]
        elif arg_types == ArgType.Mixed:
            # Parameters can be either string, numeric or dual
            script_args = []
            for dual, param in zip(duals, header.params):
                if param.dataType == SSE.STRING:
                    script_args.append(dual.strData)
                elif param.dataType == SSE.NUMERIC:
                    script_args.append(dual.numData)
                elif param.dataType == SSE.DUAL:
                    script_args.append((dual.numData, dual.strData))
                    # We add additional columns with string and numeric representation
                    # for easier access in script
                    script_args.append(dual.strData)
                    script_args.append(dual.numData)
                    dual_type = True
        else:
            # Undefined argument types
            msg = 'Undefined argument type: '.format(arg_types)
            self.raise_grpc_error(context, grpc.StatusCode.INVALID_ARGUMENT, msg)

        return pandas.Series(script_args), dual_type

    @staticmethod
    def get_arg_types(header):
        """
        Determines the argument types for all parameters.
        :param header:
        :return: ArgType
        """
        data_types = [param.dataType for param in header.params]

        if not data_types:
            return ArgType.Empty
        elif len(set(data_types)) > 1 or all(data_type == SSE.DUAL for data_type in data_types):
            return ArgType.Mixed
        elif all(data_type == SSE.STRING for data_type in data_types):
            return ArgType.String
        elif all(data_type == SSE.NUMERIC for data_type in data_types):
            return ArgType.Numeric
        else:
            return ArgType.Undefined

    @staticmethod
    def get_return_type(header):
        """
        :param header:
        :return: Return type
        """
        if header.returnType == SSE.STRING:
            return ReturnType.String
        elif header.returnType == SSE.NUMERIC:
            return ReturnType.Numeric
        elif header.returnType == SSE.DUAL:
            return ReturnType.Dual
        else:
            return ReturnType.Undefined

    @staticmethod
    def get_duals(result, ret_type):
        if isinstance(result, str) or not hasattr(result, '__iter__'):
            result = [result]
        # Transform the result to an iterable of Dual data
        if ret_type == ReturnType.String:
            return iter([SSE.Dual(strData=col) for col in result])
        elif ret_type == ReturnType.Numeric:
            return iter([SSE.Dual(numData=col) for col in result])

    @staticmethod
    def send_table_description(table, context):
        """
        # TableDescription is only handled in Qlik if sent from a 'Load ... Extension ...' script.
        # If tableDescription is set when evaluating an expression the header will be ignored
        # when received by Qlik.
        :param qResult: the result from evaluating the script
        :param table: the table description specified in the script
        :param context: the request context
        :return: nothing
        """
        logging.debug('tableDescription sent to Qlik: {}'.format(table))
        # send table description
        table_header = (('qlik-tabledescription-bin', table.SerializeToString()),)
        context.send_initial_metadata(table_header)

    def evaluate(self, context, script, ret_type, q):
        """
        Evaluates a script with given parameters and construct the result to a Row of duals.
        :param context:
        :param script:  script to evaluate
        :param ret_type: return data type
        :param q: data frame of received parameters, empty if no parameter was sent
        :return: a RowData of string dual
        """
        table = SSE.TableDescription()
        logging.debug('Received data frame (q): {}'.format(q))
        locals_added = {}  # The variables set while executing the script will be saved to this dict
        # Evaluate script, the result must be saved to the qResult object
        exec(script, {'q': q, 'numpy': numpy, 'pandas': pandas, 'table': table}, locals_added)

        if 'qResult' in locals_added:
            qResult = locals_added['qResult']
            logging.debug('Result (qResult): {}'.format(qResult))

            if 'tableDescription' in locals_added and locals_added['tableDescription'] is True:
                self.send_table_description(table, context)

            # Transform the result to bundled rows
            bundledRows = SSE.BundledRows()
            if isinstance(qResult, str) or not hasattr(qResult, '__iter__'):
                # A single value is returned
                bundledRows.rows.add(duals=self.get_duals(qResult, ret_type))
            else:
                for row in qResult:
                    bundledRows.rows.add(duals=self.get_duals(row, ret_type))

            return bundledRows
        else:
            # No result was saved to qResult object
            msg = 'No result was saved to qResult, check your script.'
            self.raise_grpc_error(context, grpc.StatusCode.INVALID_ARGUMENT, msg)
