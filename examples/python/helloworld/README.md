# Example: Hello World
This example demonstrates some very simple functionality of the SSE protocol.

We have tried to provide well documented code that you can easily follow along with. If something is unclear, please let us know so that we can update and improve our documentation. In this file, we guide you through a few key points in the implementation that are worth clarifying for this particular example.

## Content
* [Script evaluation](#script-evaluation)
* [Defined functions](#defined-functions)
* [Qlik documents](#qlik-documents)
* [Run the example!](#run-the-example)

## Script evaluation
Script evaluation is enabled in this example but only string data is handled by the plugin. That meaning, the only script functions supported by the plugin are `ScriptEvalStr`, `ScriptAggrStr`, `ScriptEvalExStr` and `ScriptAggrExStr`. The plugin will throw an error if any other data type than string is sent as parameter of the last two mentioned functions.  

The given script is evaluated with the python method `eval`. `eval` evaluates a python expression and does not work very well with more complex scripts. See documentation of the method `eval` [here](https://docs.python.org/3/library/functions.html#eval). For tensor and scalar functions the script is evaluated row wise and for aggregations the script is evaluated once after all data is retrieved.  

The parameters sent from Qlik are stored in a _list_ called `args` where the first element corresponds to the first parameter. Note how the function types affect the list storing the given parameters, and hence the script itself, when the script is evaluated:
* If the function type is an aggregation the type of `args[0]` is a list containing all rows of the first parameter.
* If the function type is scalar or tensor, the type of `args[0]` will be a single string representing the first row of the first parameter.

## Defined functions
In this plugin we have a couple of pre-defined functions, which cannot be modified from the UI. The function definitions are located in the  JSON file and include the following information:

| __Function Name__ | __ID__ | __Type__ | __ReturnType__ | __Parameters__ |
| ----- | ----- | ----- | ----- | ----- |
| HelloWorld | 0 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| HelloWorldAggr | 1 | 1 (aggregation)  | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| Cache | 2 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| NoCache | 3 | 2 (tensor) | 0 (string) | __name:__ 'str1', __type:__ 0 (string) |
| EchoTable_3 | 4 | 2 (tensor) | 0 (string) | __name:__ 'col1', __type:__ 0 (string); __name:__ 'col2', __type:__ 0 (string); __name:__ 'col3', __type:__ 0 (string) |

Both `HelloWorld` and `EchoTable_3` returns the same data as received, the difference is the number of columns. The latter is used to demonstrate the `Load ... Extension ...` syntax in the Qlik load script where you can return a table of multiple columns using SSE.

The `HelloWorldAggr` function is aggregating all rows to a single string.

The `Cache` and `NoCache` functions demonstrates how caching works by adding a date-time stamp in the end of each string value on each row. Caching is enabled by default and you can disable it by sending a header with metadata including the `qlik-cache` key set to `no-store`. In the example app the user will see that the date-time stamps will be updated for the `NoCache` function for all selections, but only for new selections for the `Cache` function.

``` python
md = (('qlik-cache', 'no-store'),)
context.send_initial_metadata(md)
```


## Qlik documents
We provide example documents for Qlik Sense (SSE_Hello_World.qvf) and QlikView (SSE_Hello_World.qvw).

There are a number of examples in the sheets demonstrating the same simple functionality using script functions as well as defined functions.

In the Qlik load script there is an example of the `Load ...  Extension ...` syntax for a table load using SSE. Since the `EchoTable_3` function, used in the load statement, does not send a `TableDescription` the returned columns have the default names _Field1_, _Field2_ and _Field3_, which are then renamed in the load statement. There are also examples of using SSE expressions within a regular load. In that case the SSE call is treated as a scalar or aggregation and only one column can be returned.

## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).
