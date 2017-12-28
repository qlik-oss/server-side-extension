# Example: Full script support
This example plugin includes support for all script functions.  

## Content
* [Script functions](#script-functions)
* [Implementation](#implementation)
  * [Parameters sent from Qlik](#parameters-sent-from-qlik)
  * [Script evaluation and result](#script-evaluation-and-result)
* [Qlik Documents](#qlik-documents)
* [Run the Example!](#run-the-example)

## Script functions
To support all script functionality we must consider all eight script functions. See [Writing an SSE Plugin using Python](../README.md) for more details about the differences among them.

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

## Implementation
When a script function is called from Qlik, the plugin receives a call to the `EvaluateScript` rpc method together with a `ScriptRequestHeader` in the beginning of the request. The header does _not_ contain the name of the script function that is called, but gives us enough information about the call for us to know what is expected. The information includes the names and types of the parameters as well as the return data type and the function type.

Note that the `CommonRequestHeader` is also sent to the plugin in the beginning of the request and includes information about the app, the user and the cardinality of the request. But we have _not_ used this information in this plugin.

We have tried to provide well documented code that you can easily follow along with. If something is unclear, please let us know so that we can update and improve our documentation. In this file, we guide you through a few key points in the implementation that are worth clarifying.

### Parameters sent from Qlik
The parameters sent from Qlik are stored in a _list_ called `args` where the first element corresponds to the first parameter.

If the parameter has the datatype _Dual_, containing both numeric and string representations, we have stored them in two separate lists in the element for the particular parameter. For example, assume the second parameter is _Dual_ then:
* `args[1]` represents the second parameter, containing two lists of string and numeric representations
* `args[1][0]` represents a list of the string representations of the second parameter
* `args[1][1]` represents a list of the numeric representations of the second parameter

### Script evaluation and result
The given script is evaluated with the python method `eval` after all rows have been collected. `eval` evaluates a python expression and does not work very well with more complex scripts. See documentation of the method `eval` [here](https://docs.python.org/3/library/functions.html#eval).

For multiline scripts or if you want to write more complex scripts we recommend you to take a look at the [FullScriptSupport using Pandas](../FullScriptSupport_Pandas/README.md) example plugin. The difference is mainly the use of `Pandas` for storing parameters and the method `exec` for evaluating the script.

The result is expected to be row wise, that meaning that the first element is the first row of the result. If multiple columns are returned in a `Load ... Extension ...` statement, the first element should have the same length as number of parameters.

## Qlik documents
An example document is given for Qlik Sense (SSE_Full_Script_Support.qvf) and QlikView (SSE_Full_Script_Support.qvw).

In the load script there is an example of the `Load ...  Extension ...` syntax for a table load using SSE. There are also examples of using SSE expressions within a regular load. In that case the SSE call is treated as a scalar or aggregation and only one column can be returned.

There are a number of examples in the sheets of how to retrieve the data from the script, and how to make simple calculations.

## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).
