# Writing an SSE plugin using Go

* [Writing an SSE Plugin](../../docs/writing_a_plugin.md)
* [Protocol Description](../../docs/SSE_Protocol.md) (API reference)
* Documentation and tutorials for [gRPC](http://www.grpc.io/docs/) and [protobuf](https://developers.google.com/protocol-buffers/docs/overview)


## Implementing a server - Protobuf generated files
The interface between the Qlik Engine acting as a client and the Server-side extension acting as server is defined in 
the file [ServerSideExtension.proto](../../proto/ServerSideExtension.proto). 


## RPC methods
The RPC methods implemented in the Basic example are GetCapabilities and ExecuteFunction. There is no script support.

### GetCapabilities method
The GetCapabilities method returns a Capabilities object, describing the operations supported by the plugin.

### ExecuteFunction method
The ExecuteFunction method switches over the numeric function identifier sent in the *qlik-functionrequestheader-bin* header. 
Each case construct then iterates over the BundledRows elements packed into the request stream and writes the results to the output stream.  There are five functions implemented in the Go example plugin:

 - *EchoString* (echoes the supplied string back to Qlik)
 - *SumOfRow* (summarizes two columns, in a row-wise manner)
 - *SumOfColumn* (summarizes  one column)
 - *Cache* (example of SSE caching)
 - *NoCache* (example of disabling SSE cache)