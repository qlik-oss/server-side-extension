# Communication flow

The following diagram demonstrates the communication between the Qlik Client (both Qlik Sense and QlikView), the Qlik Engine service, and the SSE plugin:

![](SSE_communication_flow.png?raw=true)

The communication flow is as follows:

1. From April 2018 we will retry connections to all plugins, both from QRS or settings.ini, so the plugin is not required to run before starting engine.
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
