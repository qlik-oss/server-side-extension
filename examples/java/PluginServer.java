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
import java.util.List;
import java.io.IOException;
import java.net.URL;

import java.util.Collection;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
                PluginServer.this.metadata = metadata;
                return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){}, metadata);
            }
        })
        .build();
    }
    
    
    public void start() throws IOException {
        server.start();
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
                .setAllowScript(true)
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
            logger.info("getCapabilities called.");
        }
        
        //responseObserver is not declared as final in example but compiler gives error otherwise
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {

            final qlik.sse.ServerSideExtension.FunctionRequestHeader header;
            
            try {
                header = qlik.sse.ServerSideExtension.FunctionRequestHeader
                .parseFrom(metadata.get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                logger.info("Function nbr " + header.getFunctionId() + " was called.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the function request header.");
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
                            default: logger.log(Level.WARNING, "Incorrect function id.");
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
                    responseObserver.onNext(builder.build()); //Do something more?
                    responseObserver.onCompleted();
                }
            };
        }
    
        private ServerSideExtension.BundledRows function0(ServerSideExtension.FunctionRequestHeader header, ServerSideExtension.BundledRows bundledRows){
            logger.info("This is function 0 (Not yet implemented).");
            return bundledRows;
        }
    
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
            
            final ServerSideExtension.ScriptRequestHeader header;
            
            try {
                header = qlik.sse.ServerSideExtension.ScriptRequestHeader
                .parseFrom(metadata.get(Metadata.Key.of("qlik-scriptrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                logger.info("Evaluate script was called.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the script request header.");
                PluginServer.this.stop(); //Should probably do this in another way.
                return responseObserver; //Because the compiler stops complaining. Only works because this row is never executed.
            }
            
            final ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            final ServerSideExtension.Row.Builder rowBuilder = ServerSideExtension.Row.newBuilder();
            final ServerSideExtension.Dual.Builder dualBuilder = ServerSideExtension.Dual.newBuilder();
            //&& header.getFunctionType()==ServerSideExtension.FunctionType.SCALAR
            if(header != null ) {
                
                if(header.getParamsCount()==0) {
                    ScriptEngineManager manager = new ScriptEngineManager();
                    ScriptEngine engine = manager.getEngineByName("JavaScript");
                    String result;
                    try {
                        Object res = engine.eval(header.getScript());
                        result = res.toString();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "eval script did not work");
                        e.printStackTrace();
                        PluginServer.this.stop();
                        return responseObserver;
                    }
                    
                    if(header.getReturnType()==ServerSideExtension.DataType.NUMERIC) {
                        logger.info("Numeric with 0 params");
                        builder.addRows(rowBuilder.addDuals(dualBuilder.setNumData(Double.parseDouble(result))));
                    } else if(header.getReturnType()==ServerSideExtension.DataType.STRING){
                        logger.info("String with 0 params");
                        builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result)));
                    } else if(header.getReturnType()==ServerSideExtension.DataType.DUAL){
                        logger.info("Dual with 0 params");
                        builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result).setNumData(Double.parseDouble(result))));
                    } else {
                        logger.log(Level.WARNING, "Wrong return type in evaluate script.");
                        PluginServer.this.stop();
                        return responseObserver;
                    }
                    responseObserver.onNext(builder.build());
                    responseObserver.onCompleted();
                }
            }
            
            return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
                
                @Override
                public void onNext(ServerSideExtension.BundledRows bundledRows) {
                    logger.info("On next was called");
                    if(header != null) {
                        builder.mergeFrom(evalScript(header, bundledRows));
                    }
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error in evaluateScript", t);
                }
                
                @Override
                public void onCompleted() {
                    responseObserver.onNext(builder.build()); //Do something more?
                    responseObserver.onCompleted();
                }
            };
        }
        
        private ServerSideExtension.BundledRows evalScript(ServerSideExtension.ScriptRequestHeader header, ServerSideExtension.BundledRows bundledRows) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            int nbrOfParams = header.getParamsCount();
            String result;
            Object res;
            ServerSideExtension.BundledRows.Builder bundledRowsBuilder = ServerSideExtension.BundledRows.newBuilder();
            
            //ServerSideExtension.Dual[] args;
            Object[] args;
            for (ServerSideExtension.Row row : bundledRows.getRowsList()) {
                ServerSideExtension.Row.Builder rowBuilder = ServerSideExtension.Row.newBuilder();
                ServerSideExtension.Dual.Builder dualBuilder = ServerSideExtension.Dual.newBuilder();
                args = row.getDualsList().toArray();
                engine.put("args", args);
                try {
                    res = engine.eval(header.getScript());
                    result = res.toString();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "eval script did not work");
                    e.printStackTrace();
                    PluginServer.this.stop();
                    return bundledRowsBuilder.build();
                }
                switch (header.getReturnType()) {
                    case STRING : 
                        bundledRowsBuilder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result)));
                        break;
                    case NUMERIC : 
                        bundledRowsBuilder.addRows(rowBuilder.addDuals(dualBuilder.setNumData(Double.parseDouble(result))));
                        break;
                    case DUAL : 
                        bundledRowsBuilder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result).setNumData(Double.parseDouble(result))));
                        break;
                    default :
                        logger.log(Level.WARNING, "Incorrect return type");
                        PluginServer.this.stop();
                        return bundledRowsBuilder.build();
                }
                
            }
            return bundledRowsBuilder.build();
        }
    }
	
    public static void main(String[] args) throws Exception {
        PluginServer server = new PluginServer(50071); //Get port from args, check if secure or unsecure connection, ServerCallInterceptor
        server.start();
        server.blockUntilShutdown(); //??? Needed to keep the server running
        return;
        
    }
}

