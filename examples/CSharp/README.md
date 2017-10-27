# Writing an SSE plugin using C#

General background information is found here:

* [Writing an SSE Plugin](../../docs/writing_a_plugin.md)
* [Protocol Description](../../docs/SSE_Protocol.md) (API reference)
* Documentation and tutorials for [gRPC](http://www.grpc.io/docs/) and [protobuf](https://developers.google.com/protocol-buffers/docs/overview)

## Configuration

### Configuration file settings

There are three settings in the configuration file. If you change these settings, the example program has to be restarted for new settings to take effect.
#### BasicExample.exe.config settings for grpcHost and grpcPort
These constitute the address of the gRPC server.

#### BasicExample.exe.config setting for certificateFolder
The certificate folder is where the server expects to find the public key to the root certificate and server certificate, along with the private key for the server certificate.
See the guide for [Generating certificates](../../generate_certs_guide/README.md) for more information on how to create and configure certificates.

### Configuring Qlik to use the sample gRPC server
By default, the C# sample plug-in runs on port 50054 on localhost, so for a Qlik installation, the following should be added to settings.ini:

[Settings 7] 

SSEPlugin=CSharp_Basic_example, localhost:50054;

Note that the string CSharp_Basic_example is the identifier that will prefix all plug-in functions when they are called from within Qlik.
Use a different identifier for your own plug-in, and remember that this exact string has to be used for the supplied qvf file to work with the extension.

The address (localhost:50054) should of course match the address in the server's configuration file.

For single-machine development and testing it is OK to use insecure communication, but for production scenarios you should use certificates. See [Generating certificates](../../generate_certs_guide/README.md).

## Starting the server

The C# gRPC sample server can be built and started from within the Visual Studio environment. 
It is implemented as a console program, with the Main function setting up a server object that is bound to a connector class.
RPC methods are implemented in the connector class BasicExampleConnnector.

## Implementing a server - Protobuf generated files
The interface between the Qlik Engine acting as a client and the Server-side extension acting as server is defined in 
the file [ServerSideExtension.proto](../../proto/ServerSideExtension.proto). The BasicExample C# project references the Grpc.Tools Nuget package 
which contains the Protobuf compiler protoc.exe and a plug-in for generating gRPC code in C#.

When the C# project is built, a pre-build event command line runs the protoc.exe compiler with the gRPC C# plug-in and generates the code found in the project 
folder ProtobufGenerated. This generated code contains data classes that are compatible with the Protobuf serialization used by Server-side extensions, as well 
as an abstract ConnectorBase class to be overridden by our implementation of an SSE server.
None of these generated classes are special for the BasicExample project, they only depend on the interface defined for Server-side extensions in general.

## RPC methods
The RPC methods implemented in the Basic example are GetCapabilities and ExecuteFunction. There is no script support.

### GetCapabilities method
The GetCapabilities method just returns a static Capabilities object.

### ExecuteFunction method
The ExecuteFunction method switches over the numeric function identifier sent in the qlik-functionrequestheader-bin header. 
Each case construct then iterates over the BundledRows elements packed into the request stream and writes the results to the output stream. 
The operations performed are so simple that all work is done inside the case construct.

Note that ExecuteFunction is an async method and that the await keyword is used with the asynchronous functions for reading and writing stream data. These asynchronous constructs aim at keeping the I/O operations non-blocking to allow gRPC to use the available thread pool as efficiently as possible. 
The exception is Concatenate, which uses the gRPC library's build-in function ToListAsync. 
The Concatenate function is also asynchronous but will first read all available data, then concatenate it and finally write out the result.
