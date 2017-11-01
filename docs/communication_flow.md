# Communication flow

The following diagram demonstrates the communication between the Qlik Sense client, the Qlik engine service, and the SSE plugin (the communication between _QlikView_ and the SSE plugin works in the same way):

![](SSE_communication_flow.png?raw=true)

The communication flow is as follows:

1. The SSE plugin (the server), is started.
    * The specified port, e.g. 50051, opens.
2. The engine service is started.
3. The engine checks the SSE configuration. Are there any plugins configured? What are their names and on what addresses are they running? Are they running with secure or insecure connection?
4. The engine uses the address and the name fetched in the configuration and makes the first call to the plugin using `GetCapabilities`, an RPC method, to fetch the capabilities of the plugin.
5. The plugin answers with its capabilities. Is script evaluation enabled? Are any functions added? Does the plugin have an identifier and a version?
6. The engine publishes script functions (if script evaluation is enabled) and possibly plugin functions to the BNF to enable syntax checking in the expression and load script editors.
7. In the Sense client, an object or a sequence in the load script contains an SSE expression.
8. The Sense client sends a `GetLayout` request to the engine.
9. The engine determines if the SSE call is a script function or a plugin function. If it's the former, the RPC method `EvaluateScript` is called, otherwise `ExecuteFunction` is called.
10. Either `EvaluateScript` or `ExecuteFunction` is executed in the plugin and returns the result values to the engine.
11. The engine returns the result to the client.
12. The client finishes rendering the object or loading the data in the data script and the result is visible for the user.

