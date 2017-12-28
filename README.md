# Server Side Extension

This repository provides a server-side extension (SSE) protocol based on gRPC that allows you to extend the Qlik built-in expression library with functionality from external calculation engines. You can use external calculation engines in both load scripts and charts. In Qlik Sense and QlikView, you connect to these server-side extensions (SSEs) by defining analytic connections.  
This repository includes documentation that describes the SSE protocol, how to use it and how to build your own plugins. Currently we have examples written in a few different languages only but examples in more languages will come later.  

## Status
[![CircleCI](https://circleci.com/gh/qlik-oss/server-side-extension.svg?style=shield)](https://circleci.com/gh/qlik-oss/server-side-extension)  

__Latest SSE Version:__ [v1.1.0](https://github.com/qlik-oss/server-side-extension/releases/latest)  
__Examples Disclaimer:__ The examples provided are just examples, therefore __use them at your own risk__.   

| __Latest Product Version__ | __SSE Supported__ |
| ----- | ----- |
| Qlik Sense February 2018 | v1.1.0 |
| QlikView November 2017| v1.0.0 |

[Previous Versions](CHANGELOG.md)

## Documentation

* [Overview](docs/README.md)
* [Communication flow](docs/communication_flow.md)
* [Generating certificates for secure connection](generate_certs_guide/README.md)
* [Configuring SSE plugins in Qlik Sense and QlikView](docs/configuration.md)
* [Writing an SSE plugin](docs/writing_a_plugin.md)
* [Protocol Documentation](docs/SSE_Protocol.md) (API reference)

## Examples

### Python
* [Getting started with the Python examples](examples/python/GetStarted.md)
* [Writing an SSE plugin using Python](examples/python/README.md)

### C++
* [Writing an SSE Plugin using C++](examples/cpp/README.md)
* [A basic example](examples/cpp/basic_example/README.md)

### C#
* [Writing an SSE Plugin using C#](examples/CSharp/README.md)
* The [SSE R-plugin](https://github.com/qlik-oss/sse-r-plugin) is written in C#

### Java
* [A basic Java example](examples/java/basic_example/README.md)
* [Writing an SSE plugin using Java](examples/java/basic_example/WritingAnSSEPluginUsingJava.md)

### Go
* [A basic Go example](examples/go/basic_example/README.md)
* [Writing an SSE Plugin using Go](examples/go/README.md)

## Contributing
Please follow the instructions in [CONTRIBUTING.md](.github/CONTRIBUTING.md).
