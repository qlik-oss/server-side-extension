# Communication flow

The following diagram demonstrates the communication between the Qlik Client (both Qlik Sense and QlikView), the Qlik Engine service, and the SSE plugin:

![](SSE_communication_flow.png?raw=true)

The communication flow is as follows:

1. The start order of the SSE plugin (the server) and the Qlik Engine service depends on how you are configuring the plugin.
    * Using QMC and _Sense Enterprise April 2018_: Any changes in the configurations can be done during runtime and you can start in any order you would like.  
    * Configuration using Settings.ini or QMC and earlier releases: The SSE plugin must be running before the Qlik Engine service is started. Any changes in the configuration requires a restart of the Qlik Engine service. See [Limitations](limitation.md) for more information.
2. The Qlik Engine checks the SSE configuration. Are there any plugins configured? What are their names and on what addresses are they running? Are they running with secure or insecure connection?
3. The Qlik Engine uses the address and the name fetched in the configuration and makes the first call to the plugin using `GetCapabilities`, an RPC method, to fetch the capabilities of the plugin.
4. The plugin answers with its capabilities. Is script evaluation enabled? Are any functions added? Does the plugin have an identifier and a version?
5. The Qlik Engine publishes script functions (if script evaluation is enabled) and possibly plugin functions to the BNF to enable syntax checking in the expression and load script editors.
6. In the Qlik client, an object or a sequence in the load script contains an SSE expression.
7. The Qlik client sends a `GetLayout` request to the engine.
8. The engine determines if the SSE call is a script function or a plugin function. If it's the former, the RPC method `EvaluateScript` is called, otherwise `ExecuteFunction` is called.
9. Either `EvaluateScript` or `ExecuteFunction` is executed in the plugin and returns the result values to the engine.
10. The engine returns the result to the client.
11. The client finishes rendering the object or loading the data in the data script and the result is visible for the user.
