import qlik.sse.ServerSideExtension;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Metadata;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.stub.StreamObserver;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.util.Date;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class PluginServer {

    private static final Logger logger = Logger.getLogger(PluginServer.class.getName());
    private static final LogManager logManager = LogManager.getLogManager();
    static {
        try {
            logManager.readConfiguration(new FileInputStream("./javapluginlogger.properties"));
        } catch(IOException e) {
            logger.info("Could not read the javapluginlogger.properties file, using default settings.");
        }
    }
    private final int port;
    private final Server server;
    private ThreadLocal<Metadata> metadata = new ThreadLocal<Metadata>();
    
    public PluginServer(int port, String pemDir) throws IOException {
        this.port = port;
        ServerBuilder serverBuilder;
        
        if(!pemDir.isEmpty()) {
            try {
                serverBuilder = ServerBuilder.forPort(port)
                .useTransportSecurity(new File(pemDir + "sse_server_cert.pem"), new File(pemDir + "sse_server_key.pk8"));
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
                logger.finer("Intercepting call to get metadata.");
                PluginServer.this.metadata.set(metadata);
                logHeader(metadata);
                return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){
                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        logger.finest("Send headers.");
                        try {
                            ServerSideExtension.FunctionRequestHeader header = ServerSideExtension.FunctionRequestHeader
                            .parseFrom(metadata.get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER))); //Java 8
                            logger.finest("Function request header.");
                            logHeader(responseHeaders); //Java 8
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
            
            private void logHeader(Metadata header) {
                Set<String> keys = header.keys();
                logger.finest("Is header empty? " + keys.isEmpty());
                for(String key : keys) {
                    if(key.toLowerCase().contains("-bin")) {
                        logger.finest("Key: "+ key + " Value: " + header.get(Metadata.Key.of(key, BINARY_BYTE_MARSHALLER)));
                    } else {
                        logger.finest("Key: "+ key + " Value: " + header.get(Metadata.Key.of(key, ASCII_STRING_MARSHALLER)));
                    }
                    
                }
            }
        })
        .build();
    }
    
    
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

    private class JavaPlugin extends qlik.sse.ConnectorGrpc.ConnectorImplBase {
        
        @Override
        public void getCapabilities(qlik.sse.ServerSideExtension.Empty request,
            io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.Capabilities> responseObserver) {
            
            logger.info("getCapabilities called.");
            
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
            logger.fine("getCapabilities completed.");
        }
        
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> executeFunction(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {

            logger.fine("executeFunction called.");
            final ServerSideExtension.FunctionRequestHeader header;
            
            try {
                header = ServerSideExtension.FunctionRequestHeader
                .parseFrom(metadata.get().get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                logger.fine("Function nbr " + header.getFunctionId() + " was called.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the function request header.", e);
                responseObserver.onError(new Throwable("Exception when trying to get the function request header in executeFunction."));
                responseObserver.onCompleted();
                return responseObserver;
            }
            logger.info("executeFunction called. Function Id: " + header.getFunctionId() + ".");
            
            final ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            final List<Double> columnSum = new ArrayList();
            final StringBuilder stringBuilder = new StringBuilder();
            
            return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
                
                @Override
                public void onNext(qlik.sse.ServerSideExtension.BundledRows bundledRows) {
                    logger.fine("onNext in executeFunction called.");
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
                    logger.fine("onNext in executeFunction completed.");
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error in executeFunction.", t);
                    responseObserver.onCompleted();
                }
                
                @Override
                public void onCompleted() {
                    logger.fine("onCompleted in executeFunction called.");
                    switch(header.getFunctionId()) {
                        case 2: responseObserver.onNext(builder.addRows(ServerSideExtension.Row.newBuilder().addDuals(ServerSideExtension.Dual.newBuilder().setNumData(sum(columnSum)))).build());
                            break;
                        case 3: responseObserver.onNext(builder.addRows(ServerSideExtension.Row.newBuilder().addDuals(ServerSideExtension.Dual.newBuilder().setStrData(stringBuilder.toString()))).build());
                            break;
                        default:
                            break;
                    }
                    responseObserver.onCompleted();
                    logger.fine("onCompleted in executeFunction completed.");
                }
            };
        }
        
        private double sum(List<Double> list) {
            logger.finer("sum(List<Double>) called.");
            double sum = 0;
            for(double d : list) {
                sum += d;
            }
            logger.finer("sum(List<Double>) completed with sum: " + sum + ".");
            return sum;
        }
    
        private ServerSideExtension.BundledRows helloWorld(ServerSideExtension.BundledRows bundledRows){
            logger.fine("helloWorld called (and completed).");
            return bundledRows;
        }
        
        private ServerSideExtension.BundledRows sumOfRows(ServerSideExtension.BundledRows bundledRows) {
            logger.fine("Function SumOfRows called.");
            ServerSideExtension.BundledRows.Builder result = ServerSideExtension.BundledRows.newBuilder();
            ServerSideExtension.Row.Builder rowBuilder;
            ServerSideExtension.Dual.Builder dualBuilder;
            double rowSum;
            for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
                rowBuilder = ServerSideExtension.Row.newBuilder();
                dualBuilder = ServerSideExtension.Dual.newBuilder();
                rowSum = 0;
                for(ServerSideExtension.Dual dual : row.getDualsList()) {
                    rowSum +=dual.getNumData(); 
                }
                result.addRows(rowBuilder.addDuals(dualBuilder.setNumData(rowSum)));
            }
            logger.fine("Function SumOfRows completed.");
            return result.build();
        }
        
        private double sumOfColumn(ServerSideExtension.BundledRows bundledRows) {
            logger.fine("Function SumOfColumn called.");
            double columnSum = 0;
            for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
                columnSum += row.getDuals(0).getNumData();
            }
            logger.fine("Function SumOfColumn completed.");
            return columnSum;
        }
        
        private String stringAggregation(ServerSideExtension.BundledRows bundledRows) {
            logger.fine("Function StringAggregation called.");
            StringBuilder builder = new StringBuilder();
            for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
                builder.append(row.getDuals(0).getStrData());
            }
            logger.fine("Function StringAggregation completed.");
            return builder.toString();
        }
        
        private ServerSideExtension.BundledRows cache(ServerSideExtension.BundledRows bundledRows) {
            logger.fine("Function Cache called.");
            ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            ServerSideExtension.Row.Builder rowBuilder;
            ServerSideExtension.Dual.Builder dualBuilder;
            
            for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(row.getDuals(0).getStrData()).append("___").append(new Date().toString());
                rowBuilder = ServerSideExtension.Row.newBuilder();
                dualBuilder = ServerSideExtension.Dual.newBuilder();
                builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(strBuilder.toString())));
            }
            logger.fine("Function Cache completed.");
            return builder.build();
        }
        
        private ServerSideExtension.BundledRows noCache(ServerSideExtension.BundledRows bundledRows) {
            logger.fine("Function NoCache called.");
            
            ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            ServerSideExtension.Row.Builder rowBuilder;
            ServerSideExtension.Dual.Builder dualBuilder;
            
            for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(row.getDuals(0).getStrData()).append("___").append(new Date().toString());
                rowBuilder = ServerSideExtension.Row.newBuilder();
                dualBuilder = ServerSideExtension.Dual.newBuilder();
                builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(strBuilder.toString())));
            }
            logger.fine("Function NoCache completed.");
            return builder.build();
        }
    
        @Override
        public io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> evaluateScript(
            final io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows> responseObserver) {
            
            logger.info("evaluateScript called");
            final ServerSideExtension.ScriptRequestHeader header;
            
            try {
                header = qlik.sse.ServerSideExtension.ScriptRequestHeader
                .parseFrom(metadata.get().get(Metadata.Key.of("qlik-scriptrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                logger.fine("Got the script request header.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when trying to get the script request header.", e);
                responseObserver.onError(new Throwable("Exception when trying to get the script request header in evaluateScript."));
                responseObserver.onCompleted();
                return responseObserver;
            }
            
            //final ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            //&& header.getFunctionType()==ServerSideExtension.FunctionType.SCALAR
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
                    logger.fine("onNext in evaluateScript called");
                    if(header != null) {
                        if(header.getFunctionType()==ServerSideExtension.FunctionType.AGGREGATION) {
                            logger.log(Level.WARNING, "Aggregation is not implemented in evaluate script.");
                            responseObserver.onCompleted();
                        }
                        ServerSideExtension.BundledRows result = evalScript(header, bundledRows);
                        if(result.getRowsCount()>0) {
                            responseObserver.onNext(result);
                            logger.fine("onNext in evaluateScript completed");
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
                    logger.fine("onCompleted in evaluateScript called");
                    responseObserver.onCompleted();
                    logger.fine("onCompleted in evaluateScript completed");
                }
            };
        }
        
        private ServerSideExtension.BundledRows evalScript(ServerSideExtension.ScriptRequestHeader header) {
            
            logger.fine("evalScript called");
            ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            String result;
            try {
                Object res = engine.eval(header.getScript());
                result = res.toString();
                logger.finer("The string representation of the result: " + result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "eval script did not work.", e);
                return builder.build();
            }
            
            ServerSideExtension.Row.Builder rowBuilder = ServerSideExtension.Row.newBuilder();
            ServerSideExtension.Dual.Builder dualBuilder = ServerSideExtension.Dual.newBuilder();
            
            switch (header.getReturnType()) {
                case STRING : 
                    builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result)));
                    break;
                case NUMERIC : 
                    builder.addRows(rowBuilder.addDuals(dualBuilder.setNumData(Double.parseDouble(result))));
                    break;
                case DUAL : 
                    builder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(result).setNumData(Double.parseDouble(result))));
                    break;
                default :
                    logger.log(Level.WARNING, "Incorrect return type.");
                    return builder.build();
            }
            logger.fine("evalScript completed");
            return builder.build();
        }
        
        private ServerSideExtension.BundledRows evalScript(ServerSideExtension.ScriptRequestHeader header, ServerSideExtension.BundledRows bundledRows) {
            
            logger.fine("evalScript called");
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
                    logger.log(Level.WARNING, "eval script did not work.", e);
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
                        logger.log(Level.WARNING, "Incorrect return type.");
                        return bundledRowsBuilder.build();
                }
                
            }
            logger.fine("evalScript completed");
            return bundledRowsBuilder.build();
        }
    }

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
}

