import qlik.sse.ServerSideExtension;
//import io.grpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Metadata;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.stub.StreamObserver;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.URL;

import java.util.Collection;
import java.util.Collections;

public class PluginServer {

    private static final Logger logger = Logger.getLogger(PluginServer.class.getName());
    private final int port;
    private final Server server;
    private Metadata metadata;
    
    public PluginServer(int port) throws IOException { //Somethig more here?
        this.port = port;
        server = ServerBuilder.forPort(port).addService(new JavaPlugin())
        .intercept(new ServerInterceptor() {
            @Override
            public <RequestT,ResponseT>ServerCall.Listener<RequestT> interceptCall(
            ServerCall<RequestT,ResponseT> serverCall, Metadata metadata, ServerCallHandler<RequestT,ResponseT> serverCallHandler) {
                System.out.println("Intercepting call to get metadata.");
                PluginServer.this.metadata = metadata;
                return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){}, metadata);
            }
        })
        .build();
    }
    
    
    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on port " + port);
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() { //So that the server is stopped when someone press ctr-c. Hopefully good idéa?
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
            server.awaitTermination(); //So that the program is not shut down?
        }
    }

    private class JavaPlugin extends qlik.sse.ConnectorGrpc.ConnectorImplBase {
        
        @Override
        public void getCapabilities(qlik.sse.ServerSideExtension.Empty request,
            io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.Capabilities> responseObserver) {
            
            ServerSideExtension.Capabilities pluginCapabilities = ServerSideExtension.Capabilities.newBuilder()
                .setAllowScript(false)
                .setPluginIdentifier("Qlik java plugin")
                .setPluginVersion("v1.0.0")
                .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                    .setName("functionZero")
                    .setFunctionId(0)
                    .setFunctionType(ServerSideExtension.FunctionType.SCALAR)
                    .setReturnType(ServerSideExtension.DataType.NUMERIC)
                    .addParams(ServerSideExtension.Parameter.newBuilder()
                        .setName("param1")
                        .setDataType(ServerSideExtension.DataType.NUMERIC)))
                .build();    
                
            //addFunctions
            responseObserver.onNext(pluginCapabilities);
            responseObserver.onCompleted();
            System.out.println("getCapabilities called");
        }
        
        //responseObserver is not declared as final in example but compiler gives error otherwise
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {

            final qlik.sse.ServerSideExtension.FunctionRequestHeader header;
            
            try {
                header = qlik.sse.ServerSideExtension.FunctionRequestHeader
                .parseFrom(metadata.get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                System.out.println("Function nbr " + header.getFunctionId() + " was called.");
            } catch (Exception e) {
                System.out.println("Exception when trying to get the function request header.");
                System.out.println("Closing the plugin.");
                PluginServer.this.stop(); //Should probably do this in another way.
                return responseObserver; //Because the compiler stops complaining. Only works because this row is never executed.
            }
            
            final ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            
            
            return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
            
            @Override
            public void onNext(qlik.sse.ServerSideExtension.BundledRows bundledRows) {
                if(header != null) {
                    switch(header.getFunctionId()) {
                        case 0:  builder.addAllRows(function0(header, bundledRows).getRowsList());
                                 break;
                        default: System.out.println("Incorrect function id.");
                                 break;
                    }
                }
                //Do something
            }
            
            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "Encountered error in executeFunction", t);
            }
            
            @Override
            public void onCompleted() {
                //Do something
                responseObserver.onNext(builder.build()); //Do something more
                responseObserver.onCompleted();
            }
            };
        }
    
        private ServerSideExtension.BundledRows function0(ServerSideExtension.FunctionRequestHeader header, ServerSideExtension.BundledRows bundledRows){
            System.out.println("This is function 0 (Not yet implemented).");
            return bundledRows;
        }
    
        //public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
        //    io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
        //  return asyncUnimplementedStreamingCall(METHOD_EVALUATE_SCRIPT, responseObserver);
        //}
    }
	
    public static void main(String[] args) throws Exception {
        PluginServer server = new PluginServer(50071); //Get port from args, check if secure or unsecure connection, ServerCallInterceptor
        System.out.println("Main function");
        server.start();
        server.blockUntilShutdown(); //??? Needed to keep the server running
        return;
        
    }
}

