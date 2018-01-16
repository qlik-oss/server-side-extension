# Writing an SSE plugin using Python

This section assumes you have read the following:

* [Writing an SSE Plugin](../../docs/writing_a_plugin.md)
* [Protocol Description](../../docs/SSE_Protocol.md) (API reference)
* the documentation and tutorials for [gRPC](http://www.grpc.io/docs/) and [protobuf](https://developers.google.com/protocol-buffers/docs/overview), both with Python

## Content
* [Introduction](#introduction)
* [Creating the server - with insecure/secure connection](#creating-the-server-with-insecuresecure-connection)
* [`GetCapabilities` - Mandatory for all plugins](#getcapabilities)
* [`EvaluateScript` - Support script evaluation!](#evaluatescript)
* [`ExecuteFunction` - Add your own functions!](#executefunction)
  * [Function definitions](#function-definitions)
* [Metadata sent from Qlik to the Plugin](#metadata-sent-from-qlik-to-the-plugin)
* [Metadata sent from the plugin to Qlik](#metadata-sent-from-the-plugin-to-qlik)
  * [Cache control](#cache-control)
  * [`TableDescription`](#tabledescription)
* [Error handling](#error-handling)

## Introduction
We have tried to provide well documented code in the examples that you can easily follow along with. If something is unclear, please let us know so that we can update and improve our documentation. In this file, we give you examples of how different functionalities _can be_ implemented using Python. Note that a different implementation might be better suited for your use case.

The example plugins provided are all based on the same file structure with the following files:

| __File__ | __Content__ |
| ------ | ------ |
| `ExtensionService_<examplename>.py` | The class `ExtensionService` containing the implementation of the RPC methods and the creation of the gRPC server. This file is the main file for the plugin and is the one that needs to be running before the Qlik engine is started.|
| `ScriptEval_<examplename>.py` | Used for script evaluation. The class `ScriptEval` contains methods for evaluating the script, retrieving data types or arguments etc. |
| `SSEData_<examplename>.py`| Currently used for script evaluation only. Containing class enumerates of data types and function types. |

The `<examplename>` is unique for each example and can be found in [Getting started with the Python examples](GetStarted.md).

## Creating the server - with insecure/secure connection

All examples support secure connection. If a path, to where the certificates are located, was added as a command argument when starting the server, a secure connection will be set up.

Assume `pem_dir` is the path to the certificates and `private_key`, `cert_chain` and `root_cert` the certificates themselves (read more in the [Generating certificates](../../generate_certs_guide/README.md) guide). `port` is the port the plugin listens to. Then the server can be set up and started as follows:

```python
import grpc
import ServerSideExtension_pb2 as SSE

server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
SSE.add_ConnectorServicer_to_server(self, server)
if pem_dir:
    # secure connection
    credentials = grpc.ssl_server_credentials([(private_key, cert_chain)], root_cert, True)
    server.add_secure_port('[::]:{}'.format(port), credentials)
else:
    # insecure connection
    server.add_insecure_port('[::]:{}'.format(port))
server.start()
```

## `GetCapabilities`
The `GetCapabilities` method is mandatory for all plugins and is responsible for letting Qlik know what capabilities the plugin has.

In the Python examples, we use a separate JSON file in which we have collected all function definitions. This makes it easy to add each function to the `Capabilities` message. See an example of this JSON file in the [Function Definitions](#function-definitions) section.

Note that both the request and the context are sent in every RPC call to the plugin. Furthermore, we need to add those as parameters even though we are not actively using them in this method.

```python
import ServerSideExtension_pb2 as SSE

class ExtensionExpression(SSE.ConnectiorServicer):
    ...
    def GetCapabilities(self, request, context):
        # The plugin supports script evaluation
        # Set values for pluginIdentifier and pluginVersion
        capabilities = SSE.Capabilities(allowScript=True,
                                        pluginIdentifier='Hello World - Qlik',
                                        pluginVersion='v1.0.0')

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

## `EvaluateScript`
When you enable script evaluation, several script functions are automatically added to the functionality of the plugin, as described in [Writing an SSE Plugin](../../docs/writing_a_plugin.md). After the metadata sent in `ScriptRequestHeader` is fetched (see the  [Metadata sent from Qlik to the Plugin](#metadata-sent-from-qlik-to-the-plugin) section below), we can choose to support specific function or data types. The [HelloWorld](HelloWorld/README.md) example supports for example only strings and [ColumnOperations](ColumnOperations/README.md) only numerics.

In example plugins with limited support, we check the function type in the `EvaluateScript` function and, depending on the answer, we either raise an "unimplemented" error or we continue with our evaluation. In the example code below, we support functionality for aggregation and tensor functions. Please look at the implementation in any of the examples, to see the rest of the flow in script evaluation.

If you are interested in implementing a plugin that supports all script functions, see the [FullScriptSupport using Pandas](FullScriptSupport_Pandas/README.md) example.

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

## `ExecuteFunction`
In the provided Python examples we have mapped each function Id to the name of the function implemented. The function Id is retrieved from the `FunctionRequestHeader` (see more in the [Metadata sent from Qlik to the plugin]() section below).

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

### Function definitions

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

The data types and function types are described in the SSE Protocol Documentation [here](../../docs/SSE_Protocol.md#qlik.sse.DataType).

## Metadata sent from Qlik to the Plugin
The context of the request contains the metadata sent from Qlik to the plugin. From the dictionary the different request headers can be retrieved as follows:
```python
metadata = dict(context.invocation_metadata())
header = SSE.<RequestHeader>()                                    # first letters should be capital letters
header.ParseFromString(metadata['qlik-<requestheader>-bin'])      # lower-case
```
Where _\<RequestHeader\>_ is one of the three possible headers mentioned below e.g. `CommonRequestHeader`. The _\<requestheader\>_ is the same but with lower-case letters e.g. `commonrequestheader`.

The `CommonRequestHeader` is not used in any example provided for Python, but can be useful for user or plugin version restrictions.

The `ScriptRequestHeader` is used in all examples for retrieving function type, return type, script etc.

The `FunctionRequestHeader` is used in [HelloWorld](HelloWorld/README.md) and [ColumnOperations](ColumnOperations/README.md) where we have demonstrated plugin defined functions. The header is used for retrieving function id which we mapped to the implementation of the specific functions.

## Metadata sent from the plugin to Qlik
### Cache control
Cache metadata can be sent to Qlik both as initial and trailing metadata. See the [HelloWorld](HelloWorld/README.md) example for a practical example.
```python
md = (('qlik-cache', 'no-store'),)
context.send_initial_metadata(md)
```

### `TableDescription`
The [ColumnOperations](ColumnOperations/README.md) example is demonstrating this in a plugin defined function. [FullScriptSupport using pandas](FullScriptSupport_Pandas/README.md) is demonstrating how the message can be modified from the script.

Note that the `TableDescription` must be sent as _initial_ metadata and that the number of columns of data sent back to Qlik must match the number of fields in the `TableDescription`.
```python
table = SSE.TableDescription(name='MaxOfColumns', numberOfRows=1)
table.fields.add(name='Max1', dataType=SSE.NUMERIC)
table.fields.add(name='Max2', dataType=SSE.NUMERIC)
md = (('qlik-tabledescription-bin', table.SerializeToString()),)
context.send_initial_metadata(md)
```

## Error handling
You can set a GRPC status code and extra details to the context when an error occur to pass the information to Qlik. The message will then be logged in the SSE log file. If no status code is provided, _undefined error_ will be used.

In the example code below, a specific function type is not supported.
```python
msg = 'Function type {} is not supported in this plugin.'.format(func_type.name)
context.set_code(grpc.StatusCode.UNIMPLEMENTED)
context.set_details(msg)
# Raise error on the plugin-side
raise grpc.RpcError(grpc.StatusCode.UNIMPLEMENTED, msg)
```
