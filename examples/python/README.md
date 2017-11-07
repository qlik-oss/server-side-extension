# Writing an SSE plugin using Python

This section assumes you have read the following:

* [Writing an SSE Plugin](../../docs/writing_a_plugin.md)
* [Protocol Description](../../docs/SSE_Protocol.md) (API reference)
* the documentation and tutorials for [gRPC](http://www.grpc.io/docs/) and [protobuf](https://developers.google.com/protocol-buffers/docs/overview), both with Python

## Content
* [Implementation](#implementation)
* [Creating the server](#creating-the-server)
* [RPC methods](#rpc-methods)
    * [`GetCapabilities` method](#getcapabilities-method)
    * [`EvaluateScript` method](#evaluatescript-method)
    * [`ExecuteFunction` method](#executefunction-method)
      * [Function definitions](#function-definitions)

## Implementation

The example plugins provided are all based on the same structure. They have more or less functionality supported depending on the complexity of the specific example.  

In general, each plugin has the following files:

| __File__ | __Content__ |
| ------ | ------ |
| `ExtensionService_<examplename>.py` | The class `ExtensionService` containing the implementation of the RPC methods and the creation of the gRPC server. This file is the main file for the plugin and is the one that needs to be running before the Qlik engine is started.|
| `ScriptEval_<examplename>.py` | Used for script evaluation. The class `ScriptEval` contains methods for evaluating the script, retrieving data types or arguments etc. |
| `SSEData_<examplename>.py`| Currently used for script evaluation only. Containing class enumerates of data types and function types. |

The `<examplename>` is unique for each example.

## Creating the server

The first step is to create the gRPC server and to add the `ConnectorServicer` to that server. Next, simply start the server.

``` python
import grpc
import ServerSideExtension_pb2 as SSE

class ExtensionExpression(SSE.ConnectorServicer):
    ...
    def Serve(self):
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        SSE.add_ConnectorServicer_to_server(self, server)
        server.start()
```
Of course this is just a skeleton implementation. In the examples, you will find demonstrations of how to start the server with insecure/secure communication, how to change the port the server is listening to, and so on.

## RPC methods

The RPC methods are `GetCapabilities`, `ExecuteFunction`, and `EvaluateScript`. The implementation of these methods differs depending on the supported functionality for your plugin. For instance, if you only support script evaluations you do not need to implement the `ExecuteFunction` method. However, the `GetCapabilities` method is mandatory, because that is where you define and send back the supported functionality to the client, i.e., the Qlik engine.

### `GetCapabilities` method
The `GetCapabilities` method includes the following variables you can use to define the capabilities for your plugin:
* `allowScript`: a boolean enabling/disabling script evaluation,
* `functions`: repeated function definitions.
* `pluginIdentifier`: the ID or name of the plugin.
* `pluginVersion`: the version of the plugin.

In the Python examples, we use a separate JSON file in which we have collected all function definitions. This makes it easy to add each function to the `GetCapabilities` method. See an example of this JSON in the [Function Definitions](#function-definitions) section, below.

Note that both the request and the context are sent in every RPC call to the plugin. Furthermore, we need to add those as parameters even though we are not actively using them in this method.

```python
import ServerSideExtension_pb2 as SSE

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    def GetCapabilities(self, request, context):
        # Set values for pluginIdentifier and pluginVersion
        capabilities = SSE.Capabilities(allowScript=True,
                                        pluginIdentifier='Hello World - Qlik',
                                        pluginVersion='v1.0.0-beta1')

        # If user defined functions supported, add the definitions to the message
        with open(self.function_definitions) as json_file:
            # Iterate over each function definition and add data to the Capabilities grpc message
            for definition in json.load(json_file)['Functions']:
                function = capabilities.functions.add()
                function.name = definition['Name']
                function.functionId = definition['Id']
                function.functionType = definition['Type']
                function.returnType = definition['ReturnType']

                # Retrieve name and type of each parameter
                for param_name, param_type in sorted(definition['Params'].items()):
                    function.params.add(name=param_name, dataType=param_type)

        return capabilities
```


### `EvaluateScript` method
When you enable script evaluation, several script functions are automatically added to the functionality of the plugin, as described in [Writing an SSE Plugin](../../docs/writing_a_plugin.md). The example plugins provided for Python are fairly similar to one another. The difference is the supported functionality for different data types.

In example plugins with limited support, we check the function type in the `EvaluateScript` function and, depending on the answer, we either raise an "unimplemented" error or continue with our evaluation. In the example code below, we support functionality for aggregation and tensor functions. __Note__ that you must set the _status code_ in the context to be able to include the correct error code in the SSE logs from the Qlik engine. Otherwise the logging will show undefined error. The details including the message will also be visible in the SSE logs if set.

If you are interested in implementing a plugin that supports script evaluation, see the [FullScriptSupport](FullScriptSupport/README.md) example.

```python
import ServerSideExtension_pb2 as SSE
from ScriptEval_helloworld import ScriptEval

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    def __init__(self):
        self.scriptEval = ScriptEval()

    def EvaluateScript(self, request, context):
        # Parse header for script request
        metadata = dict(context.invocation_metadata())
        header = SSE.ScriptRequestHeader()
        header.ParseFromString(metadata['qlik-scriptrequestheader-bin'])

        # Retrieve function type
        func_type = self.ScriptEval.get_func_type(header)

        # Verify function type
        if (func_type == FunctionType.Aggregation) or (func_type == FunctionType.Tensor):
            return self.ScriptEval.EvaluateScript(header, request, context, func_type)
        else:
            # This plugin does not support other function types than aggregation  and tensor.
            # Make sure the error handling, including logging, works as intended in the client
            msg = 'Function type {} is not supported in this plugin.'.format(func_type.name)
            context.set_code(grpc.StatusCode.UNIMPLEMENTED)
            context.set_details(msg)
            # Raise error on the plugin-side
            raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)
```

### `ExecuteFunction` method
When the client (i.e., the Qlik engine) calls a plugin function, the function ID is sent in a header to the server, which runs the `ExecuteFunction` method. In the examples we use a mapping between the function ID and the method name of the user defined functions (see the `functions` method in the following code example).

```python
import ServerSideExtension_pb2 as SSE

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    @property
    def functions(self):
        # Function ID maps with name of method of the user defined function
        return {
            0: '_hello_world'
        }

    def ExecuteFunction(self, request_iterator, context):
        # Retrieve function ID
        func_id = self._get_function_id(context)

        # Call corresponding function
        return getattr(self, self.functions[func_id])(request_iterator)
```


#### Function definitions

The following code, which is taken from the [Hello World](HelloWorld/README.md) example, shows the structure of a JSON function definition file:

```json
{
    "Functions" : [
        {
            "Id" : 0,
            "Name" : "HelloWorld",
            "Type" : 2,
            "ReturnType": 0,
            "Params" : {
                    "str1" : 0
                }
        }
    ]
}
```

where:

* `"Type" : 2` indicates that the _function type_ is tensor.
* `"ReturnType" : 0` indicates that the _data type_ of the return value is string.
* `"str1" : 0` indicates that the first parameter, named `"str1"`, is of _data type_ string.

The function types are mapped as follows:

| | __Function Type__ |
| ----- | ----- |
| __0__ | Scalar |
| __1__ | Aggregation |
| __2__ | Tensor |

And the data types, both of parameters as well as return types, are mapped as follows:

| | __Data Type__ |
| ----- | ----- |
| __0__ | String |
| __1__ | Numeric |
| __2__ | Dual |
