# Writing your own plugin
To help with writing your own plugin here follows an explanation of the basic java example. It is assumed that you have read [Running the example](".\Running the example.md") or know how to set up the environment you need. 
It is also assumed that you have read the general introduction to [writing an sse plugin](https://github.com/qlik-oss/server-side-extension/blob/master/docs/writing_a_plugin.md).

## Useful links
The following links are useful if you want to write your own plugin. This example is based on these examples.
* [Grpc tutorial java](https://grpc.io/docs/tutorials/basic/java.html)
* [Grpc quickstart java](https://grpc.io/docs/quickstart/java.html)
* [Protocol buffers java](https://developers.google.com/protocol-buffers/docs/javatutorial)
* [Grpc github java examples](https://github.com/grpc/grpc-java/tree/master/examples/src/main/java/io/grpc/examples)
* [SecureConnections](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#transport-security-tls)
* [Script engine](http://www.java2s.com/Tutorials/Java/Scripting_in_Java/0040__Scripting_in_Java_eval.htm)

## The plugin
Use the same .proto file as in this example as this is the .proto file the Qlik engine use. Start with generating the classes from the .proto file either with the protoc compiler or with maven (which uses the protoc compiler). Look at the file `ConnectorGrpc.java` in 
`my-app\target\generated-sources\protobuf\grpc-java\qlik\sse`. If you don't use maven this might be a part of `ServerSideExtension.java`, look for the class `ConnectorGrpc`. This class handles the connection between the plugin and the Qlik engine. 
In the class `ConnectorGrpc` there is an abstract static nested class called `ConnectorImplBase` that should be extended in the plugin.

```java
  public static abstract class ConnectorImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     *&#47; A handshake call for the Qlik engine to retrieve the capability of the plugin.
     * </pre>
     */
    public void getCapabilities(qlik.sse.ServerSideExtension.Empty request,
        io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.Capabilities> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_CAPABILITIES, responseObserver);
    }

    /**
     * <pre>
     *&#47; Requests a function to be executed as specified in the header.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
        io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_EXECUTE_FUNCTION, responseObserver);
    }

    /**
     * <pre>
     *&#47; Requests a script to be evaluated as specified in the header.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
        io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_EVALUATE_SCRIPT, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_GET_CAPABILITIES,
            asyncUnaryCall(
              new MethodHandlers<
                qlik.sse.ServerSideExtension.Empty,
                qlik.sse.ServerSideExtension.Capabilities>(
                  this, METHODID_GET_CAPABILITIES)))
          .addMethod(
            METHOD_EXECUTE_FUNCTION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                qlik.sse.ServerSideExtension.BundledRows,
                qlik.sse.ServerSideExtension.BundledRows>(
                  this, METHODID_EXECUTE_FUNCTION)))
          .addMethod(
            METHOD_EVALUATE_SCRIPT,
            asyncBidiStreamingCall(
              new MethodHandlers<
                qlik.sse.ServerSideExtension.BundledRows,
                qlik.sse.ServerSideExtension.BundledRows>(
                  this, METHODID_EVALUATE_SCRIPT)))
          .build();
    }
  }
```

Three methods need to be implemented: `getCapabilities`, `executeFunction`, and `evaluateScript`. All three methods have a streamObserver as an argument, this is where the result from the method is sent back. getCapabilities also 
have another argument but it is never used. In this example the class extending `ConnectorImplBase`, called `JavaPlugin`, is a private class in the public class `PluginServer`. `PluginServer` contains a server and adds `JavaPlugin` 
as a service to this server when the class is initialized. It therefore handles the starting and the stopping of the server and keeps track of which port to listen to. An overview of `JavaPlugin` can be found below.

```java
public class PluginServer {

    private final int port;
    private final Server server;
    private ThreadLocal<Metadata> metadata = new ThreadLocal<Metadata>();
    
    
    private static final Logger logger = Logger.getLogger(PluginServer.class.getName());
    private static final LogManager logManager = LogManager.getLogManager();
    static {
        try {
            logManager.readConfiguration(new FileInputStream("./javapluginlogger.properties"));
        } catch(IOException e) {
            logger.info("Could not read the javapluginlogger.properties file, using default settings.");
        }
    }
    
    public PluginServer(int port, String pemDir) throws IOException {
        ....
    }
    
    
    public void start() throws IOException {   
        ....
    }
    
    public void stop() {
        ....
    }
    
    private void blockUntilShutdown() throws InterruptedException {
        ....
    }
    
    ....

    private class JavaPlugin extends qlik.sse.ConnectorGrpc.ConnectorImplBase {
        
        @Override
        public void getCapabilities(qlik.sse.ServerSideExtension.Empty request,
            io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.Capabilities> responseObserver) {

            ....
        }
        
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {

            ....
        }
        
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
            
            ....
        }
         
         ....
    }

    public static void main(String[] args) throws Exception {
        
        ....        
    }
}
```

The logManager tries to read the logging settings for the logger from the properties file. If that fails it goes back to the default settings for the logger. To make the code easier to read a lot of logging statements 
will be removed but they can be found in the file `PluginServer.java`. An important thing to notice here is the line 

```java
private ThreadLocal<Metadata> metadata = new ThreadLocal<Metadata>();
```

Because multiple requests are sent from the engine at once the plugin needs to be able to handle multiple function calls at the same time. The metadata is different for each function call and therefore it needs to be 
different for each thread. The `ThreadLocal<>` object makes this possible. Note that in this example the metadata is not given a default value (so it will be empty until it is set). The metadata will be mentioned 
further in the constructor below.

```java
    public PluginServer(int port, String pemDir) throws IOException {
        this.port = port;
        ServerBuilder serverBuilder;
        
        if(!pemDir.isEmpty()) {
            try {
                serverBuilder = ServerBuilder.forPort(port).useTransportSecurity(new File(pemDir + "sse_server_cert.pem"), new File(pemDir + "sse_server_key.pk8"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not create a secure connection.", e);
                serverBuilder = ServerBuilder.forPort(port);
            }
        } else {
            serverBuilder = ServerBuilder.forPort(port);
        }
        
        server = serverBuilder.addService(new JavaPlugin())
        .intercept(new ServerInterceptor() {
            @Override
            public <RequestT,ResponseT>ServerCall.Listener<RequestT> interceptCall(
                ServerCall<RequestT,ResponseT> serverCall, final Metadata metadata, ServerCallHandler<RequestT,ResponseT> serverCallHandler) {
                
                PluginServer.this.metadata.set(metadata);
                return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){
                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        try {
                            ServerSideExtension.FunctionRequestHeader header = ServerSideExtension.FunctionRequestHeader
                            .parseFrom(metadata.get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER))); //Java 8
                            if(header.getFunctionId()==5) {
                                String value = "no-store";
                                responseHeaders.put(Metadata.Key.of("qlik-cache", ASCII_STRING_MARSHALLER),value);
                            } else {
                                String value = "no-store";
                                responseHeaders.remove(Metadata.Key.of("qlik-cache", ASCII_STRING_MARSHALLER),value);
                            }
                        } catch(Exception e) {}
                        super.sendHeaders(responseHeaders);
                    }
                }, metadata);
            }
        })
        .build();
    }
```

First, the port is set and then a server builder is created. If the plugin was given the command line argument `--pemDir` the server tries to use a secure connection with the certificates provided. If not, or if setting up a 
secure connection failed, an insecure connection is set up instead. Then the class `JavaPlugin` is added as a service followed by a `ServerInterceptor`. The `ServerInterceptor` is an interface so it needs to be implemented. 
In this example this is done with an anonymous class where the (only) method, interceptCall, is implemented. The reason for adding a `ServerInterceptor` is to get a hold of the metadata being sent with each function call and 
store it in the ThreadLocal<Metadata> metadata object. In the metadata, the information about which function to execute in `executeFunction` or which script to evaluate in `evaluateScript` can be found.

The secure connection in this example is set up with a static link to OpenSSL and the APR library but if you want a secure connection, take a look at [this](https://github.com/grpc/grpc-java/blob/master/SECURITY.md) page on 
Authentication at the grpc-java github.

When interceptCall is finished, it forwards the call by returning a Listener to a `SimpleForwardingServerCall` based of the intercepted call. Since `SimpleForwardingServerCall` is an abstract class this leads to another anonymous 
class that have to be implemented. If you are not interested in sending any metadata back to the Qlik engine you can leave the class body empty.

```java
return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){};
```

If you want to be able to send metadata back, like in this example when the caching is turned off, you must override the sendHeaders method. Here the `no-store` value for the caching is also removed from the metadata of the 
functions that have caching turned on to be extra sure that caching is not turned of, but this is not necessary and can be skipped. If you do not have Java 8 you need to write `JavaPlugin.this.metadata.get()`instead of `metadata` 
when trying to get the function request header.

The next part of the code is necessary for starting and shutting down the server.

```java
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port + ".");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                PluginServer.this.stop();
            }
        });
    }
    
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
```

And the main function can read the port number and the path to the certificates for a secure connection from the command line.

```java
    public static void main(String[] args) throws Exception {
        int port = 50071;
        String pemDir = "";
        for(int i = 0; i<args.length-1; i+=2) {
            if(args[i].equals("--port")) {
                try {
                    port = Integer.parseInt(args[i+1]);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Invalid port, using default value: " + port);
                }
            } else if (args[i].equals("--pemDir")) {
                pemDir = args[i+1];
            }
            
        }
        PluginServer server = new PluginServer(port, pemDir);
        server.start();
        server.blockUntilShutdown(); 
        return;
        
    }
```

Next in line is then the `JavaPlugin` class and the first method in this class is `getCapabilities`.

```java
        @Override
        public void getCapabilities(qlik.sse.ServerSideExtension.Empty request,
            io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.Capabilities> responseObserver) {
            
            ServerSideExtension.Capabilities pluginCapabilities = ServerSideExtension.Capabilities.newBuilder()
                .setAllowScript(true)
                .setPluginIdentifier("Qlik java plugin")
                .setPluginVersion("v1.0.0")
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("HelloWorld")
                    .setFunctionId(0)
                    .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                    .setReturnType(ServerSideExtension.DataType.STRING)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("str1")
                        .setDataType(ServerSideExtension.DataType.STRING)))
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("SumOfRows")
                    .setFunctionId(1)
                    .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                    .setReturnType(ServerSideExtension.DataType.NUMERIC)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("col1")
                        .setDataType(ServerSideExtension.DataType.NUMERIC))
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("col2")
                        .setDataType(ServerSideExtension.DataType.NUMERIC)))
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("SumOfColumn")
                    .setFunctionId(2)
                    .setFunctionType(ServerSideExtension.FunctionType.AGGREGATION)
                    .setReturnType(ServerSideExtension.DataType.NUMERIC)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("column")
                        .setDataType(ServerSideExtension.DataType.NUMERIC)))
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("StringAggregation")
                    .setFunctionId(3)
                    .setFunctionType(ServerSideExtension.FunctionType.AGGREGATION)
                    .setReturnType(ServerSideExtension.DataType.STRING)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("columnOfStrings")
                        .setDataType(ServerSideExtension.DataType.STRING)))
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("Cache")
                    .setFunctionId(4)
                    .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                    .setReturnType(ServerSideExtension.DataType.STRING)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("columnOfStrings")
                        .setDataType(ServerSideExtension.DataType.STRING)))
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("NoCache")
                    .setFunctionId(5)
                    .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                    .setReturnType(ServerSideExtension.DataType.STRING)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("columnOfStrings")
                        .setDataType(ServerSideExtension.DataType.STRING)))
                .build();

            responseObserver.onNext(pluginCapabilities);
            responseObserver.onCompleted();
        }
```

`getCapabilities` receives a streamObserver<Capabilities> that it uses to send capabilities with `onNext` and to tell the Qlik engine that it has nothing more to send with `onCompleted`. In the method, a Capabilities object is created
and has it attributes set to reflect the capabilities of the plugin. When the engine is started, it sends a call to the getCapabilities method to find out what the plugin can do. These methods are then added to the syntax in the 
load script and in chart expressions. The Capabilities class is generated from the `.proto` file and if you used maven to create the plugin it can be found in `ServerSideExtension.java` in 
`my-app\target\generated-sources\protobuf\java\qlik\sse`. More information about the classes can be found in the  [Protocol documentation](https://github.com/qlik-oss/server-side-extension/blob/master/docs/SSE_Protocol.md). 

The functions defined in `getCapabilities` can then be executed by calling `executeFunction`. 

```java
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {

            final ServerSideExtension.FunctionRequestHeader header;
            try {
                header = ServerSideExtension.FunctionRequestHeader
                .parseFrom(metadata.get().get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER)));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the function request header.", e);
                responseObserver.onError(new Throwable("Exception when trying to get the function request header in executeFunction."));
                responseObserver.onCompleted();
                return responseObserver;
            }
            
            final ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            final List<Double> columnSum = new ArrayList();
            final StringBuilder stringBuilder = new StringBuilder();
            
            return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
                
                @Override
                public void onNext(qlik.sse.ServerSideExtension.BundledRows bundledRows) {
                    if(header != null) {
                        switch(header.getFunctionId()) {
                            case 0:  responseObserver.onNext(helloWorld(bundledRows));
                                     break;
                            case 1:  responseObserver.onNext(sumOfRows(bundledRows));
                                     break;
                            case 2:  columnSum.add(sumOfColumn(bundledRows));
                                     break;
                            case 3:  stringBuilder.append(stringAggregation(bundledRows));
                                     break;
                            case 4:  responseObserver.onNext(cache(bundledRows));
                                     break;
                            case 5:  responseObserver.onNext(noCache(bundledRows));
                                     break;
                            default: logger.log(Level.WARNING, "Incorrect function id.");
                                     responseObserver.onError(new Throwable("Incorrect function id in onNext in executeFunction."));
                                     responseObserver.onCompleted();
                                     break;
                        }
                    } else {
                        logger.log(Level.WARNING, "The function request header is null.");
                        responseObserver.onError(new Throwable("The function request header is null in on next in executeFunction."));
                        responseObserver.onCompleted();
                    }
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error in executeFunction.", t);
                    responseObserver.onCompleted();
                }
                
                @Override
                public void onCompleted() {
                    switch(header.getFunctionId()) {
                        case 2: responseObserver.onNext(builder.addRows(ServerSideExtension.Row.newBuilder().addDuals(ServerSideExtension.Dual.newBuilder().setNumData(sum(columnSum)))).build());
                            break;
                        case 3: responseObserver.onNext(builder.addRows(ServerSideExtension.Row.newBuilder().addDuals(ServerSideExtension.Dual.newBuilder().setStrData(stringBuilder.toString()))).build());
                            break;
                        default:
                            break;
                    }
                    responseObserver.onCompleted();
                }
            };
        }
```

First, the metadata received in `interceptCall` is used to get the function request header. Then a `StreamObserver` is returned and implemented 
as an anonymous class. StreamObserver has three methods that need to be implemented, `onNext`, `onError` and `onCompleted`. To `onNext` the information needed to execute the functions are sent as `BundledRows` and, depending on the 
function Id, `onNext` forwards these `BundledRows` to the correct function. If the function can be executed row by row the result is sent back to the Qlik engine with the StreamObserver `responseObserver` received as an input parameter to `executeFunction`. 
Otherwise the result is stored since there might be many calls to `onNext`. `onError` is a way for the Qlik engine to tell the plugin that something went wrong and `onCompleted` is sent when the Qlik engine have no more `BundledRows` to send. In 
`onCompleted` the functions that could not calculate the result row by row (StringAggregation and SumOfColumn) send their results to the Qlik engine and all functions send the call `onCompleted` to the Qlik engine to signal that no more information 
will be coming. If the plugin encounters an error the `onError` method from the StreamObserver sent to the plugin is used to tell the Qlik engine that something went wrong.

Because java does not contain an `eval` function to evaluate scripts sent to the plugin written in java there are two alternatives if you want to be able to execute a java file from Qlik Sense or QlikView. Write your own compiler/parser
for java code or use an installed java compiler. Since both of those alternatives extends a basic example a third alternative has been chosen. Instead of evaluating a java file the `javax.script.ScriptEngine` class and the 
`javax.script.ScriptEngineManager` have been used to evaluate scripts written in JavaScript.     

```java
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
            
            final ServerSideExtension.ScriptRequestHeader header;
            
            try {
                header = qlik.sse.ServerSideExtension.ScriptRequestHeader
                .parseFrom(metadata.get().get(Metadata.Key.of("qlik-scriptrequestheader-bin", BINARY_BYTE_MARSHALLER)));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the script request header.", e);
                responseObserver.onError(new Throwable("Exception when trying to get the script request header in evaluateScript."));
                responseObserver.onCompleted();
                return responseObserver;
            }
            
            if(header != null ) {
                
                if(header.getParamsCount()==0) {
                    ServerSideExtension.BundledRows result = evalScript(header);
                    if(result.getRowsCount()>0) {
                        responseObserver.onNext(result);
                    } else {
                        responseObserver.onError(new Throwable("An error occured in evalScript in evaluateScript."));
                    }
                    responseObserver.onCompleted();
                    logger.fine("evaluateScript completed");
                }
            } else {
                logger.log(Level.WARNING, "The script request header is null.");
                responseObserver.onError(new Throwable("The script request header is null in evaluateScript."));
                responseObserver.onCompleted();
            }
            
            return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
                
                @Override
                public void onNext(ServerSideExtension.BundledRows bundledRows) {
                    if(header != null) {
                        if(header.getFunctionType()==ServerSideExtension.FunctionType.AGGREGATION) {
                            logger.log(Level.WARNING, "Aggregation is not implemented in evaluate script.");
                            responseObserver.onCompleted();
                        }
                        ServerSideExtension.BundledRows result = evalScript(header, bundledRows);
                        if(result.getRowsCount()>0) {
                            responseObserver.onNext(result);
                        } else {
                            responseObserver.onError(new Throwable("An error occured in evalScript in evaluateScript."));
                        }
                    } else {
                        logger.log(Level.WARNING, "The script request header is null.");
                        responseObserver.onError(new Throwable("The script request header is null in onNext in evaluateScript."));
                        responseObserver.onCompleted();
                    }
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error in evaluateScript", t);
                    responseObserver.onCompleted();
                }
                
                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
```

First of all, the metadata is used to get the JavaScript script from the `ScriptRequestHeader`. If no parameters are sent to the plugin the script can be evaluated immediately and then sent back to the Qlik engine with 
`responseObserver.onNext(result)` followed by `onCompleted()`. If parameters are needed to evaluate the script the StreamObserver that is sent back to the engine is used to obtain the parameters as `BundledRows` from `onNext`.
As in the `executeFunction` method the StreamObserver is implemented as an anonymous class. Aggregations are not supported for script evaluation in this example but if you want to support that, remember that the result must be 
sent back first when everything has been computed since only a single value should be returned. If it is not an aggregation the result is evaluated for each row in the bundled rows sent to the plugin and returned to the engine. 

The rest of the methods can be found in `PluginServer.java`.
