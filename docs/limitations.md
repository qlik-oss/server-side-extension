# Limitations in this version of SSE plugins

#### Syntax warnings and errors in data load editor (Qlik Sense Limitation)
The functions that the SSE plugins provide may not show up properly in the data load editor (script editor) in Qlik Sense which means the intellisense may complain about it and show errors, even though it works just fine to execute.

#### Returning data to Qlik Sense
There is no support for returning several columns or higher-order data, to Qlik Sense than was sent to the plugin. The cardinality of the response from the plugin must be the same as sent from Qlik Sense.
- If less rows, or lower cardinality, is sent back from the plugin, Sense will add null values to match the number of rows sent from Sense to the plugin originally. Note that the mapping may not work as intended if less data is sent back.
- If higher-order data is sent back, either too many rows or more than one column, Qlik Sense will neglect the additional data and only take the first _n_ rows in the first column, assuming _n_ is the number of rows sent to the plugin. Note that the mapping may not work as intended if more data is sent back.

#### Load script data cardinality (Qlik Sense Limitation)
There is no support for tensor calls from the load script. Only scalar and aggregation calls are supported.

#### Changes to plugins require engine restart (Qlik Sense Limitation)
If you add, remove, or change a plugin, you must restart either the engine service (for Qlik Sense Enterprise) or Qlik Sense Desktop.

It is only during engine startup that the `GetCapability` plugin method is called.

#### QlikView
SSE is not supported in QlikView yet. We are planning to release SSE support in QlikView during 2017-H2.
