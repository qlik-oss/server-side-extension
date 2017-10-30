# Example: Full script support
This example plugin includes support for all script functionality.  

## Content
* [Script evaluation](#script-evaluation)
    * [Implementation](#implementation)
* [Qlik Documents](#qlik-documents)
    * [Load script](#load-script)
    * [Chart expressions](#chart-expressions)
* [Run the Example!](#run-the-example)

## Script evaluation
To support all script functionality we must consider all combinations of function types and data types (both arguments and return type). As discussed in [Writing an SSE Plugin using Python](../README.md) we have eight script functions:

| __Function Name__ | __Function Type__ | __Argument Type__ | __Return Type__ |
| ----- | ----- | ----- | ----- |
| __ScriptEval__ | Scalar, Tensor | Numeric | Numeric |
| __ScriptEvalStr__ | Scalar, Tensor | String | String |
| __ScriptAggr__ | Aggregation | Numeric | Numeric |
| __ScriptAggrStr__ | Aggregation | String | String |
| __ScriptEvalEx__ | Scalar, Tensor | Dual | Numeric |
| __ScriptEvalExStr__ | Scalar, Tensor | Dual | String |
| __ScriptAggrEx__ | Aggregation | Dual | Numeric |
| __ScriptAggrExStr__ | Aggregation | Dual | String |

### Implementation
The `EvaluateScript` method in the `ExpressionExtension` class is kept simple. It consists of parsing the header, which is then sent together with the request in the `ScriptEval.EvaluateScript` method.

```python
import ServerSideExtension_pb2 as SSE
from ScriptEval_helloworld import ScriptEval

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    def __init__(self):
        self.ScriptEval = ScriptEval()

    def EvaluateScript(self, request, context):
      # Parse header for script request
      metadata = dict(context.invocation_metadata())
      header = SSE.ScriptRequestHeader()
      header.ParseFromString(metadata['qlik-scriptrequestheader-bin'])

      return self.ScriptEval.EvaluateScript(header, request)
```

We continue by retrieving the function types and data types and checking for parameters. Then we iterate over the rows in the bundled rows, fetching parameters and evaluating the script either row-wise or after collecting all parameters, depending on the function type.

```python
import grpc
import numpy as np
from SSEData_script import ArgType, \
                           FunctionType, \
                           ReturnType

import ServerSideExtension_pb2 as SSE

class ScriptEval:
    def EvaluateScript(self, header, request):
      # Retrieve function type
      func_type = self.get_func_type(header)

      # Retrieve data types from header
      arg_types = self.get_arg_types(header)
      ret_type = self.get_return_type(header)

      aggr = (func_type == FunctionType.Aggregation)

      # Check if parameters are provided
      if header.params:
          # Create an empty list if tensor function
          if aggr:
              all_rows = []

          # Iterate over bundled rows
          for request_rows in request:
              # Iterate over rows
              for row in request_rows.rows:
                  # Retrieve parameters
                  params = self.get_arguments(arg_types, row.duals, header)

                  if aggr:
                      # Append value to list, for later aggregation
                      all_rows.append(params)
                  else:
                      # Evaluate script row wise
                      yield self.evaluate(header.script, ret_type, params=params)

          # Evaluate script based on data from all rows
          if aggr:
              # First element in the parameter list should contain the data of the first parameter.
              params = [list(param) for param in zip(*all_rows)]
              if arg_types == ArgType.Mixed:
                  # Each parameter list should contain two lists, the first one consisting of numeric
                  # representations and the second one, the string representations.
                  params = [[list(datatype) for datatype in zip(*param)] for param in params]
              yield self.evaluate(header.script, ret_type, params=params)

      else:
          # No parameters provided
          yield self.evaluate(header.script, ret_type)
```
In the `get_arguments` method, implemented in the same class, we determine how the parameters, retrieved by row, are stored when retrieved. This affects how to write the scripts in the client. The parameters are stored in a list where each element is either a string or numeric value. However, if the data type is dual we save each element in the list as a tuple, including the numeric followed by the string representation. For tensor and script functions, the script is evaluated per row.

In the `EvaluateScript` method above, we reconstruct the data to follow the same pattern as for tensor and scalar functions. The first element in the list of parameters should be the first parameter. Furthermore, if the argument types are mixed, we format the data of each parameter list to a list of two lists, one containing the numeric representations and the other containing the string representations. We want to ensure that regardless of the script function you choose, the data is reached in the same way. For example, `args[0]` refers to the first parameter and `args[1][0]` is the numeric representation (argument type is mixed) of the second parameter.  

```python
class ScriptEval:
  ...
  @staticmethod
  def get_arguments(arg_types, duals, header):
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
    else:
        # Undefined argument types
        raise grpc.RpcError(grpc.StatusCode.UNKNOWN,
                            'Undefined argument type: '.format(arg_types))
    return script_args
```

Independently of the parameters and function type, only string or numeric data can be returned. In the `evaluate` method, we evaluate the script with the specified parameters and then transform the result to iterables of duals and return it as `BundledRows`.

``` python
class ScriptEval:
  ...
  @staticmethod
  def evaluate(script, ret_type, params=[]):
    # Evaluate script
    result = eval(script, {'args': params, 'np': np})

    # Transform the result to an iterable of Dual data
    if ret_type == ReturnType.String:
        duals = iter([SSE.Dual(strData=result)])
    elif ret_type == ReturnType.Numeric:
        duals = iter([SSE.Dual(numData=result)])

    return SSE.BundledRows(rows=[SSE.Row(duals=duals)])
```


## Qlik documents
This section describes the SSE calls in the Qlik documents. An example document is given for Qlik Sense (SSE_Full_Script_Support.qvf) and QlikView (SSE_Full_Script_Support.qvw). The explanation covers the load-script and chart expressions separately. The section on chart expressions also describes the different sheets.

### Load script
Three tables are loaded into the Qlik engine, _Table1_, _SSELoad_ and _SSELoadAggr_. _Table1_ consists of three columns of data: __ID__, __String__ and __Mixed__. The __String__ and __Mixed__ columns refer to the data types of the values they contain. The __ID__ column is used as a key between _Table1_ and _SSELoad_. The __Numeric__ column in _SSELoad_ is loaded by an SSE expression: `Script.ScriptEval('args[0] + 1', ID)`. The script evaluated in the plugin is `'args[0] + 1'` and adds a one to each row of the given parameter __ID__.

Note that SSE calls in the load-script are currently limited to the scalar function and aggregation function types. This implies that there is one call to the plugin per row of data returned. For example the __Numeric__ field is interpreted as scalar and will result in four calls to the plugin. An aggregation call on the other hand results in a single call since we are returning only one value, independently of how many rows any given parameter has.

The third table _SSELoadAggr_ contains a single value in the field __TotalCount__, which is loaded by an aggregation call to the SSE plugin. The syntax used here is `Script.ScriptAggr('len(args[0])', ID)` where `'len(args[0])'`is the script executed, returning the length of the __ID__ field sent as the first, and only, parameter.

### Chart expressions
The Qlik document consists of six sheets, one providing no parameters, one describing the SSE calls from load script, and the rest are based on specific data types (numeric, string and dual).

The first sheet demonstrates running scripts with no parameters. This is likely not a common use case, and because we do not send any parameters, only a single value can be returned. In the top KPI object we use `Script.ScriptEvalStr('"The answer to the Ultimate Question of Life, the Universe, and Everything"')` to return the string. Note the double pair of quotes, where the outer represents the script itself and the inner is defining the string. The second call adds two numbers: `Script.ScriptEval('40+2')`.

The second sheet shows a simple tensor and aggregation call using string data. As in the [Hello World](../HelloWorld/README.md) example, we return the same values with `Script.ScriptEvalStr('args[0]', String)` and aggregate all strings to a single comma-separated string with `Script.ScriptAggrStr('", ".join(args[0])',String)`.

The third sheet is similar to the [Column operations](../ColumnOperations/README.md) example. We return the same values with `Script.ScriptEval('args[0]', Numeric)` and sums all values with `Script.ScriptAggr('sum(args[0])',Numeric)`.

Now we come to the mixed data types and tensor (or scalar) function types, and we need to handle both the numeric and string representation. As described earlier, the parameters are represented as a list of tuples. Referring to the [Script evaluation](#script-evaluation) section above, we can see that we access the numeric representations using the expression `Script.ScriptEvalEx('D','args[0][0]',Mixed)` and the string representation using `Script.ScriptEvalExStr('D','args[0][1]',Mixed)`. Similarly, we can add the numeric values in _Numeric_ to the numeric representation in _Mixed_, using the expression `Script.ScriptEvalEx('DN','args[0][0] + args[1]',Mixed,Numeric)`. Finally, we can perform a similar operation with strings. We join the strings in _String_ and the string representation in _Mixed_ using the expression `Script.ScriptEvalExStr('DS','args[0][1]+", " + args[1]',Mixed,String)`.

In the next sheet we work with mixed data types and aggregation calls. We access the data in the same way as in the tensor functions described above. The first element in `args` is the first parameter which now contains numeric representations and string representations. The difference when aggregating is that instead of a tuple of one numeric value and one string value we have collected all numeric and string values in separate lists. The top KPI object joins the string representation (of the first, and only, parameter) to a single string by `Script.ScriptAggrExStr('D','", ".join(args[0][1])',Mixed)` and the lower object is returning the number of rows that have a numeric representation in the first(and only) parameter, `Script.ScriptAggrEx('D', 'np.count_nonzero(~np.isnan(args[0][0]))',Mixed)`.

Note the first parameter in all `Script*Ex*` functions is a string representing the data types of the parameters. For example, 'DS' means the first parameter is dual (D) and the second one numeric (N). The third option is 'S' for string.

The two fields __Numeric__ and __TotalCount__ loaded in the load script are described shortly and shown in the last sheet. For exact syntax see the [Load Script](#load-script) section above.

## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).
