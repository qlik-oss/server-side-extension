import logging
import logging.config

import grpc
from ssedata import ArgType, ReturnType, FunctionType

import ServerSideExtension_pb2 as SSE


class ScriptEval:
    """
    Class for SSE plugin ScriptEval functionality.
    """

    def EvaluateScript(self, header, request, context, func_type):
        """
        Evaluates script provided in the header, given the
        arguments provided in the sequence of RowData objects, the request.

        :param header:
        :param request: an iterable sequence of RowData.
        :param context: the context sent from client
        :param func_type: function type.
        :return: an iterable sequence of RowData.
        """
        # Retrieve data types from header
        arg_types = self.get_arg_types(header)
        ret_type = self.get_return_type(header)

        logging.info('EvaluateScript: {} ({} {}) {}'
                     .format(header.script, arg_types, ret_type, func_type))

        aggr = (func_type == FunctionType.Aggregation)

        # Check if parameters are provided
        if header.params:
            # Verify argument type
            if arg_types == ArgType.String:
                # Create an empty list if tensor function
                if aggr:
                    all_rows = []

                # Iterate over bundled rows
                for request_rows in request:
                    # Iterate over rows
                    for row in request_rows.rows:
                        # Retrieve numerical data from duals
                        params = self.get_arguments(context, arg_types, row.duals)

                        if aggr:
                            # Append value to list, for later aggregation
                            all_rows.append(params)
                        else:
                            # Evaluate script row wise
                            yield self.evaluate(context, header.script, ret_type, params=params)

                # Evaluate script based on data from all rows
                if aggr:
                    params = [list(param) for param in zip(*all_rows)]
                    yield self.evaluate(context, header.script, ret_type, params=params)
            else:
                # This plugin does not support other argument types than string.
                # Make sure the error handling, including logging, works as intended in the client
                msg = 'Argument type: {} not supported in this plugin.'.format(arg_types)
                context.set_code(grpc.StatusCode.UNIMPLEMENTED)
                context.set_details(msg)
                # Raise error on the plugin-side
                raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)

        else:
            # This plugin does not support script evaluation without parameters
            # Make sure the error handling, including logging, works as intended in the client
            msg = 'Script evaluation with no parameters is not supported in this plugin.'
            context.set_code(grpc.StatusCode.UNIMPLEMENTED)
            context.set_details(msg)
            # Raise error on the plugin-side
            raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)

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
    def get_arguments(context, arg_types, duals):
        """
        Gets the array of arguments based on
        the duals, and the type (string, numeric)
        specified in the header.
        :param context: the context sent from client
        :param arg_types: argument types
        :param duals: an iterable sequence of duals.
        :return: list of string arguments
        """
        if arg_types == ArgType.String:
            # All parameters are of string type
            script_args = [d.strData for d in duals]
        else:
            # This plugin does not support other arg types than string
            # Make sure the error handling, including logging, works as intended in the client
            msg = 'Argument type {} is not supported in this plugin.'.format(arg_types)
            context.set_code(grpc.StatusCode.UNIMPLEMENTED)
            context.set_details(msg)
            # Raise error on the plugin-side
            raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)

        return script_args

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
    def evaluate(context, script, ret_type, params=[]):
        """
        Evaluates a script with given parameters.
        :param context: the context sent from client
        :param script:  script to evaluate
        :param ret_type: return data type
        :param params: params to evaluate. Default: []
        :return: a RowData of string dual
        """
        if ret_type == ReturnType.String:
            # Evaluate script
            result = eval(script, {'args': params})
            # Transform the result to an iterable of Dual data with a string value
            duals = iter([SSE.Dual(strData=result)])

            # Create row data out of duals
            return SSE.BundledRows(rows=[SSE.Row(duals=duals)])
        else:
            # This plugin does not support other return types than string
            # Make sure the error handling, including logging, works as intended in the client
            msg = 'Return type {} is not supported in this plugin.'.format(ret_type)
            context.set_code(grpc.StatusCode.UNIMPLEMENTED)
            context.set_details(msg)
            # Raise error on the plugin-side
            raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)
