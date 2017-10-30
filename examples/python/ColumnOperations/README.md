# Example: Column operations
In this example we cover some basic calculations and support for different function types. Compared with the Hello World example we cover numerics only, to keep simplicity.

## Content
* [Script evaluation](#script-evaluation)
    * [Implementation](#implementation)
* [Defined functions](#defined-functions)
    * [`SumOfRows` function](#sumofrows-function)
    * [`SumOfColumn` function](#sumofcolumn-function)
* [Qlik documents](#qlik-documents)
* [Run the example!](#run-the-example)

## Script evaluation
Script evaluation is enabled, however, we support only numeric as data type. Supported function types in this plugin are tensor and aggregation. The script functions we are using are therefore `ScriptEval` and `ScriptAggr`.

### Implementation
As mentioned in the general Python documentation we start by checking the function type in the `EvaluateScript` method.

```python
import ServerSideExtension_pb2 as SSE

class ExtensionService(SSE.ConnectorServicer):
    ...
    def EvaluateScript(self, request, context):
      # Retrieve header from request
      metadata = dict(context.invocation_metadata())
      header = SSE.ScriptRequestHeader()
      header.ParseFromString(metadata['qlik-scriptrequestheader-bin'])

      # Retrieve function type
      func_type = self.scriptEval.get_func_type(header)

      # Verify function type
      if (func_type == FunctionType.Tensor) or (func_type == FunctionType.Aggregation):
          return self.scriptEval.EvaluateScript(request, header, func_type)
      else:
          # This plugin does not support other function types than tensor and aggregation.
          raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                              'Function type {} is not supported in this plugin.'.format(func_type.name))
```

If the function type is supported, we continue by retrieving data types and check for parameters. Depending on function type we either evaluate the script row wise (tensor) or collect all row values to evaluate the script once (aggregation).

```python
import grpc
from SSEData_column import ArgType, \
                           ReturnType, \
                           FunctionType

import ServerSideExtension_pb2 as SSE


class ScriptEval:
    def EvaluateScript(self, request, header, func_type):
      # Retrieve data types form header
      arg_types = self.get_arg_types(header)
      ret_type = self.get_return_type(header)

      aggr = (func_type == FunctionType.Aggregation)

      # Check if parameters are provided
      if header.params:
          # Verify argument type
          if arg_types == ArgType.Numeric:
              # Create an empty list if tensor function
              if aggr:
                  all_rows = []

              # Iterate over bundled rows
              for request_rows in request:
                  # Iterate over rows
                  for row in request_rows.rows:
                      # Retrieve numerical data from duals
                      params = self.get_arguments(arg_types, row.duals)

                      if aggr:
                          # Append value to list, for later aggregation
                          all_rows.append(params)
                      else:
                          # Evaluate script row wise
                          yield self.evaluate(header.script, ret_type, params=params)

              # Evaluate script based on data from all rows
              if aggr:
                  params = [list(param) for param in zip(*all_rows)]
                  yield self.evaluate(header.script, ret_type, params=params)

          else:
              # This plugin does not support other argument types than numeric.
              raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                                  'Argument type: {} not supported in this plugin.'.format(arg_types))
      else:
          # This plugin does not support script evaluation without parameters
          raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                              'Script evaluation with no parameters is not supported in this plugin.')

```

In the same class we have defined both the `get_arguments` and `evaluate` methods. The parameters are fetched based on data type and if the data type is not supported, an error is raised. The `evaluate` method does the evaluating of the script itself, as well as transforming the result to the desired form, bundled rows.

```python
class ScriptEval:
  ...
  @staticmethod
  def get_arguments(arg_types, duals):
    if arg_types == ArgType.Numeric:
        # All parameters are of numeric type
        script_args = [d.numData for d in duals]
    else:
        # This plugin does not support other arg types than numeric
        raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                            'Argument type {} is not supported in this plugin.'.format(arg_types))

      return script_args

  @staticmethod
  def evaluate(script, ret_type, params=[]):
    if ret_type == ReturnType.Numeric:
        # Evaluate script
        result = eval(script, {'args': params})
        # Transform the result to an iterable of dual data
        duals = iter([SSE.Dual(numData=result)])

        # Create row data out of duals
        return SSE.BundledRows(rows=[SSE.Row(duals=duals)])
    else:
        # This plugin does not support other return types than numeric
        raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                            'Return type {} is not supported in this plugin.'.format(ret_type))
```

## Defined functions
This plugin has two user defined functions, `SumOfRows` and `SumOfColumn`, both operating on numerical data. The `ExecuteFunction` method in the `ExtensionService` class is the same for any of the example plugins, but the JSON file and the `functions` method are different. The JSON file for this plugin includes the following information:

| __Function Name__ | __Id__ | __Type__ | __ReturnType__ | __Parameters__ |
| ----- | ----- | ----- | ------ | ----- |
| SumOfRows | 0 | 2 (tensor) | 1 (numeric) | __name:__ 'col1', __type:__ 1 (numeric); __name:__ 'col2', __type:__ 1(numeric) |
| SumOfColumn | 1 | 1 (aggregation) | 1 (numeric) | __name:__ 'col1', __type:__ 1 (numeric) |

The ID is mapped to the implemented function name in the `functions` method, below:
```python
import ServerSideExtension_pb2 as SSE

class ExtensionService(SSE.ConnectorServicer):
    ...
    @property
    def functions(self):
        return {
            0: '_sum_of_rows',
            1: '_sum_of_column'
        }
```

### `SumOfRows` function
The first function is a tensor function adding two columns row wise. We iterate over the `BundledRows` and extract the numerical values, which we then add togehter and transform into the desired form.

```python
    @staticmethod
    def _sum_of_rows(request):
        # Iterate over bundled rows
        for request_rows in request:
            response_rows = []
            # Iterating over rows
            for row in request_rows.rows:
                # Retrieve the numerical value of the parameters
                # Two columns are sent from the client, hence the length of params will be 2
                params = [d.numData for d in row.duals]

                # Sum over each row
                result = sum(params)

                # Create an iterable of Dual with a numerical value
                duals = iter([SSE.Dual(numData=result)])

                # Append the row data constructed to response_rows
                response_rows.append(SSE.Row(duals=duals))

            # Yield Row data as Bundled rows
            yield SSE.BundledRows(rows=response_rows)
```

### `SumOfColumn` function
The second function is an aggregation and sums the values in a column. We iterate over the `BundledRows` again and retrieve the numerical values, appending them to a list. After iterating over all rows, we add the values together and then return the result as bundled rows.

```python
    @staticmethod
    def _sum_of_column(request):
        params = []

        # Iterate over bundled rows
        for request_rows in request:
            # Iterating over rows
            for row in request_rows.rows:
                # Retrieve numerical value of parameter and append to the params variable
                # Length of param is 1 since one column is received, the [0] collects the first value in the list
                param = [d.numData for d in row.duals][0]
                params.append(param)

        # Sum all rows collected the the params variable
        result = sum(params)

        # Create an iterable of dual with numerical value
        duals = iter([SSE.Dual(numData=result)])

        # Yield the row data constructed
        yield SSE.BundledRows(rows=[SSE.Row(duals=duals)])
```

## Qlik documents
An example document is given for Qlik Sense (SSE_Column_Operations.qvf) and QlikView (SSE_Column_Operations.qvw). There are are two sheets in this example. One contains script calls and the other contains user defined function calls. We demonstrate a tensor function, which sums two columns row-wise, and an aggregating function, which sums all rows in a column returning a single value. The data loaded in the Data Load Editor are two fields, *A* and *B*, each of which contain five numeric values.

The aggregating script function is called with the expression `Column.ScriptAggr('sum(args[0])', A)`, where `'sum(args[0])'` is the script and `A` is the data field. The script returns a single value when evaluated. The second script call is `Column.ScriptEval('args[0]+args[1]',A,B)` with the script `'args[0]+args[1]'` adding the two parameters `A` and `B` row-wise. The result is an array with five values, each a sum of the  corresponding values in `A` and `B`.

The user defined function calls are straightforward as implemented. The aggregating function `SumOfColumn` is called with `Column.SumOfColumn(A)` and the tensor function `SumOfRows` is called with `Column.SumOfRows(A,B)`.

## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).