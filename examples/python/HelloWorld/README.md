# Example: Hello World
This example demonstrates some very simple functionality of the SSE protocol. For simplicity and readability, this example is scaled down to cover only the basics.

## Content
* [Script evaluation](#script-evaluation)
    * [Implementation](#implementation)
* [Defined functions](#defined-functions)
    * [`HelloWorld` function](#helloworld-function)
    * [`HelloWorldAggr` function](#helloworldaggr-function)
    * [`Cache` function](#cache-function)
    * [ `NoCache` function](#nocache-function)
* [Qlik documents](#qlik-documents)
* [Run the example!](#run-the-example)

## Script evaluation
Script evaluation is enabled in this example, but we support only strings as data types. We have not implemented support for numerics or duals. For examples showing support of different data types, see the other examples. The script functions we can use with strings are `ScriptEvalStr` and `ScriptAggrStr`. However, in the implementation we take the data types into consideration to determine what script functions are supported. We support tensor and aggregation in this example.

### Implementation
As discussed in [Writing an SSE plugin using Python](..\README.md), we start by checking the function type.

```python
import ServerSideExtension_pb2 as SSE
from ScriptEval_helloworld import ScriptEval

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    def __init__(self):
        self.scriptEval = ScriptEval()

    def EvaluateScript(self, request, context):
        # Parse header for script request
        metadata = dict(context.invocation_metadata())
        header = SSE.ScriptRequestHeader()
        header.ParseFromString(metadata['qlik-scriptrequestheader-bin'])

        # Retrieve function type
        func_type = self.ScriptEval.get_func_type(header)

        # Verify function type
        if (func_type == FunctionType.Aggregation) or (func_type == FunctionType.Tensor):
            return self.ScriptEval.EvaluateScript(header, request, func_type)
        else:
            # This plugin does not support other function types than aggregation  and tensor.
            raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                                'Function type {} is not supported in this plugin.'.format(func_type.name))
```

If the function type is supported, we continue by retrieving the argument types and return type. We then check for parameters, argument types, and finally we retrieve the data itself.

```python
import grpc
from SSEData_helloworld import ArgType, \
                               ReturnType, \
                               FunctionType

import ServerSideExtension_pb2 as SSE


class ScriptEval:
  def EvaluateScript(self, header, request, func_type):
      # Retrieve data types from header
      arg_types = self.get_arg_types(header)
      ret_type = self.get_return_type(header)

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
              # This plugin does not support other argument types than string.
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
      if arg_types == ArgType.String:
          # All parameters are of string type
          script_args = [d.strData for d in duals]
      else:
          # This plugin does not support other arg types than string
          raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                              'Argument type {} is not supported in this plugin.'.format(arg_types))

      return script_args

  @staticmethod
  def evaluate(script, ret_type, params=[]):
      if ret_type == ReturnType.String:
          # Evaluate script
          result = eval(script, {'args': params})
          # Transform the result to an iterable of Dual data with a string value
          duals = iter([SSE.Dual(strData=result)])

          # Create row data out of duals
          return SSE.BundledRows(rows=[SSE.Row(duals=duals)])
      else:
          # This plugin does not support other return types than string
          raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED,
                              'Return type {} is not supported in this plugin.'.format(ret_type))
```

## Defined functions
In this plugin we have four user defined functions: one returns the same values as it receives (tensor), one aggregates all values to a single string (aggregation), and the last two enable and disable caching by adding a date-time stamp to the end of each string value (tensor).

The JSON file includes the following information:

| __Function Name__ | __ID__ | __Type__ | __ReturnType__ | __Parameters__ |
| ----- | ----- | ----- | ----- | ----- |
| HelloWorld | 0 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| HelloWorldAggr | 1 | 1 (aggregation)  | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| Cache | 2 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| NoCache | 3 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |

The ID is mapped to the implemented method name in the `functions` method, below:

```python
import ServerSideExtension_pb2 as SSE

class ExtensionService(SSE.ConnectorServicer):
    ...
    @property
    def functions(self):
        return {
            0: '_hello_world',
            1: '_hello_world_aggr',
            2: '_cache',
            3: '_no_cache'
        }
```

### `HelloWorld` function

The `HelloWorld` function is the tensor function that returns the same values it recieves. In the implementation we iterate over each `BundledRows` object in the request and return the same values again without any modification.

``` python
import ServerSideExtension_pb2 as SSE

class ExtensionService(SSE.ConnectorServicer):
    ...
    @staticmethod
    def _hello_world(request, context):
        for request_rows in request:
            yield request_rows
```

### `HelloWorldAggr` function

In the aggregating function, we need to retrieve the actual string data of each row. And after joining all strings together, we reconstruct the result to the desired form.

``` python
import ServerSideExtension_pb2 as SSE

class ExtensionService(SSE.ConnectorServicer):
    ...
    @staticmethod
    def _hello_world_aggr(request, context):
        params = []

        # Iterate over bundled rows
        for request_rows in request:
            # Iterate over rows
            for row in request_rows.rows:
                # Retrieve string value of parameter and append to the params variable
                # Length of param is 1 since one column is received, the [0] collects the first value in the list
                param = [d.strData for d in row.duals][0]
                params.append(param)

        # Aggregate parameters to a single string
        result = ', '.join(params)

        # Create an iterable of dual with the result
        duals = iter([SSE.Dual(strData=result)])

        # Yield the row data as bundled rows
        yield SSE.BundledRows(rows=[SSE.Row(duals=duals)])
```

### `Cache` function
This function demonstrates the default behaviour with cached enabled, by adding the date-time stamp in the end of the string value on each row. The user will see that the values are only updated if a new combination of selections is made.  

```python
@staticmethod
def _cache(request, context):
    # Iterate over bundled rows
    for request_rows in request:
        # Iterate over rows
        for row in request_rows.rows:
            # Retrieve string value of parameter and append to the params variable
            # Length of param is 1 since one column is received, the [0] collects the first value in the list
            param = [d.strData for d in row.duals][0]

            # Join with current timedate stamp
            result = param + ' ' + datetime.now().isoformat()
            # Create an iterable of dual with the result
            duals = iter([SSE.Dual(strData=result)])

            # Yield the row data as bundled rows
            yield SSE.BundledRows(rows=[SSE.Row(duals=duals)])
```

### `NoCache` function
If for some reason you need to update the result each time you make a selection, you can disable the cache by setting the `qlik-cache` variable to 'no-store' in the header. Except for setting the header, the function is exactly the same as the `Cache` function described above. In the resulting Qlik document you will notice that the date-time stamp is updated every time you change your selections.

```python
@staticmethod
def _no_cache(request, context):
    # Disable caching.
    md = (('qlik-cache', 'no-store'),)
    context.send_initial_metadata(md)

    # Iterate over bundled rows
    for request_rows in request:
        # Iterate over rows
        for row in request_rows.rows:
            # Retrieve string value of parameter and append to the params variable
            # Length of param is 1 since one column is received, the [0] collects the first value in the list
            param = [d.strData for d in row.duals][0]

            # Join with current timedate stamp
            result = param + ' ' + datetime.now().isoformat()
            # Create an iterable of dual with the result
            duals = iter([SSE.Dual(strData=result)])

            # Yield the row data as bundled rows
            yield SSE.BundledRows(rows=[SSE.Row(duals=duals)])
```

## Qlik documents
An example document is given for Qlik Sense (SSE_Hello_World.qvf) and QlikView (SSE_Hello_World.qvw). The example consists of three sheets, one with script function calls and two with user defined function calls. The function calls on each of the first two sheets demonstrate the same functionality. We use a table for the tensor call and a KPI object for the aggregation call. On the third sheet we demonstrate enabling and disabling the cache for a specific function. A field called __HelloWorldData__ consisting of two rows of strings is loaded into the Qlik engine.

For the user defined functions we use the expressions `HelloWorld.HelloWorld(HelloWorldData)`, `HelloWorld.HelloWorldAggr(HelloWorldData)`, `HelloWorld.Cache(HelloWorldData)` and `HelloWorld.NoCache(HelloWorldData)`. The calls are straightforward, with the data field sent as a parameter to each function.

For the script calls we use `HelloWorld.ScriptEvalStr('args[0]', HelloWorldData)` and `HelloWorld.ScriptAggrStr('", ".join(args[0])', HelloWorldData)` where `'args[0]'` and `'", ".join(args[0])'` are the scripts and `HelloWorldData` is the parameter. The parameters are reached in Python by the variable name `args` and is a list with each parameter as an entry. Therefore, `[0]` refers to the first parameter.

## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).