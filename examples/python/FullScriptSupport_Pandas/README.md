# Example: Full script support using Pandas
This example plugin includes support for all script functionality and is based on the original [Full Script Support](../FullScriptSupport/README.md) Python example. The implementation of this plugin differs mainly in the use of the Pandas library. In addition, the data received from Qlik is now saved to a Pandas data frame. In this example, we use the `exec` method to evaluate the script rather than the `eval` method, as we did in the original example plugin. This change makes it possible to pass a multiline script from Qlik.

## Content
* [Implementation](#implementation)
    * [Parameters sent from Qlik](#parameters-sent-from-qlik)
    * [TableDescription](#tabledescription)
    * [Result](#result)
* [Qlik Documents](#qlik-documents)
* [Run the Example!](#run-the-example)

## Implementation
We have tried to provide well documented code that you can easily follow along with. If something is unclear, please let us know so that we can update and improve our documentation. In this file, we guide you through a few key points in the implementation that are worth clarifying.

### Parameters sent from Qlik
The parameters sent from Qlik are now stored in a `pandas.DataFrame` object called `q`. The names of the parameters, and hence the column names of `q`, are set to the names sent from Qlik in the _ScriptRequestHeader_. For instance if you send a parameter called `Foo` in Qlik, you will reach the parameter by writing `q.Foo` or `q["Foo"]` in the script.

If the parameter is of type _Dual_ the plugin will create two additional columns in the `q` data frame, with the string and numerical representation. The column names will have the base as the parameter name but will end with '_str' and '_num' respectively. For example, a parameter called `Bar` with datatype _Dual_ will result in three columns in `q`: `Bar`, `Bar_str` and `Bar_num`. `Bar` will contain strings and numerics, `Bar_str` will contain only strings and `Bar_num` only numerics.

### TableDescription
In the load script, when using the `Load ... Extension ...` syntax you can create the `TableDescription` message within the script. This can be useful if, for example, you want to name, set tags for, or change the datatype of the fields you are sending back to Qlik. Read more about what metadata can be included in the `TableDescription` in the [SSE_Protocol.md](../../../docs/SSE_Protocol.md#qlik.sse.TableDescription).

An instance of the `TableDescription` message is available from the script by the name `table`. To that instance you can add metadata according to the protocol. A few simple examples:

- `table.name = "Table1"` sets the table name to be _Table1_
- `table.fields.add(name="firstField", dataType=1, tags=["tag1", "tag2"])` adds a _numeric_ field called _firstField_ with the tags _tag1_ and _tag2_.

Note that if a `TableDescription` is sent, the number of fields in the message must match the number of fields of data sent back to Qlik.

### Result
With the change to using the `exec` method to evaluate the script, there are some changes regarding what's possible to write in the script. See the Python documentation of `exec` [here](https://docs.python.org/3/library/functions.html#exec). One change is that the `exec` method does not return anything. We must therefore set the result to a specific variable, which we have chosen to call `qResult`. If nothing was set to the variable, no data will be returned to Qlik. Note that `qResult` is not required to be a Pandas data frame.

For example, if you want to return the same parameters as received from Qlik you can use the script `'qResult = q.values'`. Note that if I wrote `'qResult = q'` the entire data frame, including the column names as the first row, will be passed along to where the duals and BundledRows are created. This could result in an error if the column names are strings and you are supposed to return numerics.


## Qlik documents
We provide an example Qlik Sense document (SSE_Full_Script_Support_pandas.qvf). It's the same as the original Full Script Support example, but with modified scripts to work with the Pandas implementation and the use of `exec`.

In the load script there is an example of the `Load ...  Extension ...` syntax for a table load using SSE. There are also examples of using SSE expressions within a regular load. In that case the SSE call is treated as a scalar or aggregation and only one column can be returned.

In comparison to the original Full Script Support example, the table load using the `Load ... Extension ...` also includes an example of how to write the `TableDescription` from the script.

There are a number of examples in the sheets of how to retrieve the data from the script, and how to make simple calculations.


## Run the example!
To run this example, follow the instructions in [Getting started with the Python examples](../GetStarted.md).
