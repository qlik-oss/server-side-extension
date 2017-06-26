# Writing an SSE Plugin using C++

Using C++ to write SSE plugins can be desirable when high performance is needed as the throughput on the wire can be substantially higher. The implementation requires more knowledge of the developer though than many of the other programming languages.

## Prerequisites

Before running the examples or writing a plugin the reader should ideally be familiar with how gRPC works and how it is used for SSE plugins.
The following recourses can be used to acquire the needed information:

* [gRPC C++ Quickstart](http://www.grpc.io/docs/quickstart/cpp.html) that covers installation of gRPC and its dependencies.
* [Writing an SSE plugin](../../docs/writing_a_plugin.md) to understand how an SSE plugin works.

## Dependencies

In the provided example the dependencies are the same as for the gRPC examples so as long as gRPC has been set up properly, everything should be in place.

## Compiling and running

### Linux

A Makefile is provided with the example. If a gRPC example has been compiled successfully then the local example should compile as well.

### Windows

At this point there is unfortunately no support given to compile on Windows systems. For those interested this is still possible and we suggest having a look at the following repository https://github.com/plasticbox/grpc-windows/

## A note on synchronous and asynchronous implementations

The gRPC C++ implementation offers both synchronous and asynchronous interfaces. From our experience the performance is quite similar regardless of which one is used. The type of interface should be chosen with respect of what it is being integrated with and if asynchronicity is needed.
