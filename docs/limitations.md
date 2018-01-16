# Limitations

#### Expressions using SSE must persist the cardinality
When you use SSE in a chart expression or in the Qlik load script (the `LOAD ... EXTENSION ...` statement excluded), you should preserve the cardinality and return a single column. In the case of aggregations, the response column should contain a single value (one row). In the case of tensor functions, the response column should contain the same number of rows as the request.
- If too little data is returned from the plugin, Qlik will add null values to match the number of rows expected.
- If higher-order data is returned, either too many rows or too many columns, Qlik will discard any additional data.

#### Chart expressions cannot consume a table returned from SSE plugin
It is only possible to consume a returned table from an SSE call in the Qlik load script when using the `LOAD ... EXTENSION` statement. In all other cases, including chart expressions, only the first column returned from the plugin will be used by Qlik.

#### Changes to plugins require engine restart (Qlik Limitation)
If you add, remove or change the capabilities of a plugin, you must restart the Qlik engine. For Qlik Sense this means either the engine service (for Qlik Sense Enterprise) or Qlik Sense Desktop. For QlikView, you must restart the QlikView Server service or QlikView Desktop.

It is only during engine startup that Qlik tries to contact the SSE plugin by calling the `GetCapability` plugin method.
