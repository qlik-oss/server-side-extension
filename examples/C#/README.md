# Writing an SSE plugin using C#

General background information is found here:

* [Writing an SSE Plugin](../../docs/writing_a_plugin.md)
* [Protocol Description](../../docs/SSE_Protocol.md) (API reference)
* the documentation and tutorials for [gRPC](http://www.grpc.io/docs/) and [protobuf](https://developers.google.com/protocol-buffers/docs/overview), both with Python

## Content
* [Starting the server](#starting-the-server)
* [RPC methods](#rpc-methods)
    * [GetCapabilities method](#getcapabilities-method)
    * [ExecuteFunction method](#executefunction-method)


There is currently only one sample plugin for C#.
It demonstrates how to implement aggregate and scalar functions, and how to declare them in the return value of GetCapabilities.

## Configuring QlikSense to use the sample gRPC server
By default, the C# sample plug-in runs on port 50054, so for a QlikSense Desktop installation, the following should be added to settings.ini:
[Settings 7] 
SSEPlugin=CSharp_Basic_example, localhost:50054;

Note that the string CSharp_Basic_example is the identifier that will prefix all plug-in functions when they are called from within Qlik.
Use a different identifier for your own plug-in, and remember that this exact string has to be used for the supplied qvf file to work with the extension.

For single-machine development and testing it is OK to use insecure communication, but for production scenarios you should use certificates. See [Writing an SSE Plugin](../../docs/writing_a_plugin.md) for more information on how to create and configure certificates.

## Starting the server

The C# gRPC sample server can be built and started from within the Visual Studio environment. It is implemented as a console program, with the Main function setting up a server object that is bound to a connector class.
RPC methods are implemented in the connector class BasicExampleConnnector.

## RPC methods
The RPC methods are GetCapabilities and ExecuteFunction. There is no script support.

### GetCapabilities method
The GetCapabilities method just returns a static Capabilities object.

### ExecuteFunction method
The ExecuteFunction method switches over the numeric function identifier sent in the qlik-functionrequestheader-bin header. Each case statement then iterates over the BundledRows elements packed into the request stream and writes the results to the output stream.
Note that ExecuteFunction is an async method and that the await keyword is used with the asynchronous functions for reading and writing stream data. These asynchronous constructs aim at keeping the I/O operations non-blocking to allow gRPC to use the available thread pool as efficiently as possible. 


