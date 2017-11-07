# A basic example


## Getting started

 - Install the [Go ](https://golang.org/) programming language. 

 - Download and install the [protobuf compiler](https://github.com/google/protobuf/releases) and add the *protoc* executable to your %PATH%. 
(scroll down to find a suitable package, e.g. protoc-[VERSION]-[PLATFORM].zip)

 - In a command prompt, execute: *go get -u github.com/golang/protobuf/protoc-gen-go*
(note that the [*protoc-gen-go*](https://github.com/golang/protobuf#installation) program also needs to be in your %PATH%).

 - In a command prompt, execute: *go get github.com/qlik-oss/server-side-extension/examples/go/basic_example*
(you will get an "error" saying there are no go files in the \gen folder, but that's  fine - we will generate a file in that folder in the following step).

 - From your Go SSE plugin folder (i.e. %GOPATH%\src\github.com\qlik-oss\server-side-extension\examples\go\basic_example), execute: *go generate*
(this will generate the gRPC/Protobuf file(s) that your server will use  for implementing the qlik.sse.Connector service).



### Configuring Qlik to use the sample gRPC server
By default, the Go sample plug-in runs on port 50055 on localhost, so for a Qlik installation, the following should be added to settings.ini:

*[Settings 7]* 
*SSEPlugin=SSEGo, localhost:50055[,PATH_TO_CERTIFICATE_FOLDER]*

Note that the string SSEGo is the identifier that will prefix all plug-in functions when they are called from within Qlik.
Use a different identifier for your own plug-in, and remember that this exact string has to be used for your Qlik applications to work with the extension.

The address (localhost:50055) should of course match the address in the server's configuration file.

For single-machine development and testing it is OK to use unsecure communication, but for production scenarios you should use certificates. See [Generating certificates](../../../generate_certs_guide/README.md).

## Starting the server

The Go gRPC sample server can be built and started using the go command:

*go run server.go*

This will start the server in unsecure mode. To run in secure mode, execute the following command instead:

*go run server.go -tls -cert_file=..\..\..\generate_certs_guide\sse_qliktest_generated_certs\sse_qliktest_server_certs\sse_server_cert.pem -key_file=..\..\..\generate_certs_guide\sse_qliktest_generated_certs\sse_qliktest_server_certs\sse_server_key.pem*

## Run the example app
Copy the file *SSE Go.qvf* to your Sense apps folder, i.e. *C:\Users\\[user]\Qlik\Sense\Apps* and start Sense desktop (make sure the SSE Go plugin is up and running).


