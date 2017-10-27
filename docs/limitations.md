# Limitations in this version of SSE plugins

#### Syntax warnings and errors (Qlik Limitation)
- The functions that the SSE plugins provide may not show up properly in the data load editor (script editor) in Qlik which means the intellisense may complain about it and show errors, even though it works just fine to execute.
- Error handling not working when _more_ parameters are sent in the Ex-script functions (ScriptEvalEx, ScriptEvalExStr, ScriptAggrEx and ScriptAggrExStr) than data types specified (first parameter stating the data types for each parameter of data). Currently you will get stuck with a "spinning wheel" in that object which requires you to shut down the plugin and then restart, alternatively restart engine. For example: trying to evaluate `Script.ScriptEvalEx('D', 'args[0][0]', Mixed, Numeric)` will reproduce this issue because only one data type, the 'D' parameter, is stated but two parameters are sent: _Mixed_ and _Numeric_. The _correct_ syntax would be `Script.ScriptEvalEx('DN', 'args[0][0]', Mixed, Numeric)`, with matching number of specified data types and given parameters. See the _Script evaluation_ section in [Writing an SSE plugin](writing_a_plugin.md) for more information about script functions.   

#### Returning data to Qlik
There is no support for returning several columns or higher-order data, to Qlik than was sent to the plugin. The cardinality of the response from the plugin must be the same as sent from Qlik.
- If less rows, or lower cardinality, is sent back from the plugin, Qlik will add null values to match the number of rows sent from Qlik to the plugin originally. Note that the mapping may not work as intended if less data is sent back.
- If higher-order data is sent back, either too many rows or more than one column, Qlik will neglect the additional data and only take the first _n_ rows in the first column, assuming _n_ is the number of rows sent to the plugin. Note that the mapping may not work as intended if more data is sent back.

#### Load script data cardinality (Qlik Limitation)
There is no support for tensor calls from the load script. Only scalar and aggregation calls are supported.

#### Changes to plugins require engine restart (Qlik Limitation)
If you add, remove, or change a plugin, you must restart the Qlik engine. For Qlik Sense this means either the engine service (for Qlik Sense Enterprise) or Qlik Sense Desktop. For QlikView, you must restart the QlikView Server service or QlikView Desktop.

It is only during engine startup that the `GetCapability` plugin method is called.
