# Limitations in this version of SSE plugins

#### Returning data to Qlik
There is no support for returning several columns or higher-order data, to Qlik than was sent to the plugin. The cardinality of the response from the plugin must be the same as sent from Qlik.
- If less rows, or lower cardinality, is sent back from the plugin, Qlik will add null values to match the number of rows sent from Qlik to the plugin originally. Note that the mapping may not work as intended if less data is sent back.
- If higher-order data is sent back, either too many rows or more than one column, Qlik will neglect the additional data and only take the first _n_ rows in the first column, assuming _n_ is the number of rows sent to the plugin. Note that the mapping may not work as intended if more data is sent back.

#### Load script data cardinality (Qlik Limitation)
There is no support for tensor calls from the load script. Only scalar and aggregation calls are supported.

#### Changes to plugins require engine restart (Qlik Limitation)
If you add, remove, or change a plugin, you must restart the Qlik engine. For Qlik Sense this means either the engine service (for Qlik Sense Enterprise) or Qlik Sense Desktop. For QlikView, you must restart the QlikView Server service or QlikView Desktop.

It is only during engine startup that the `GetCapability` plugin method is called.
