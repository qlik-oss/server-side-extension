package com.qlik.sse.basicexample;

import qlik.sse.ServerSideExtension;

import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

    
public class JavaPlugin extends qlik.sse.ConnectorGrpc.ConnectorImplBase {
     
    private static final Logger logger = Logger.getLogger(JavaPlugin.class.getName());
    private ThreadLocal<Metadata> metadata = new ThreadLocal<Metadata>();
    
    private static final int HELLO_WORLD = 0;
    private static final int SUM_OF_ROWS = 1;
    private static final int SUM_OF_COLUMN = 2;
    private static final int STRING_AGGREGATION = 3;
    private static final int CACHE = 4;
    private static final int NO_CACHE = 5;
    
    public void setMetadata(Metadata metadata) {
        this.metadata.set(metadata);
    }
    
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
                 .setFunctionId(HELLO_WORLD)
                 .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                 .setReturnType(ServerSideExtension.DataType.STRING)
                 .addParams(ServerSideExtension.Parameter.newBuilder()
                     .setName("str1")
                     .setDataType(ServerSideExtension.DataType.STRING)))
             .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                 .setName("SumOfRows")
                 .setFunctionId(SUM_OF_ROWS)
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
                 .setFunctionId(SUM_OF_COLUMN)
                 .setFunctionType(ServerSideExtension.FunctionType.AGGREGATION)
                 .setReturnType(ServerSideExtension.DataType.NUMERIC)
                 .addParams(ServerSideExtension.Parameter.newBuilder()
                     .setName("column")
                     .setDataType(ServerSideExtension.DataType.NUMERIC)))
             .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                 .setName("StringAggregation")
                 .setFunctionId(STRING_AGGREGATION)
                 .setFunctionType(ServerSideExtension.FunctionType.AGGREGATION)
                 .setReturnType(ServerSideExtension.DataType.STRING)
                 .addParams(ServerSideExtension.Parameter.newBuilder()
                     .setName("columnOfStrings")
                     .setDataType(ServerSideExtension.DataType.STRING)))
             .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                 .setName("Cache")
                 .setFunctionId(CACHE)
                 .setFunctionType(ServerSideExtension.FunctionType.TENSOR)
                 .setReturnType(ServerSideExtension.DataType.STRING)
                 .addParams(ServerSideExtension.Parameter.newBuilder()
                     .setName("columnOfStrings")
                     .setDataType(ServerSideExtension.DataType.STRING)))
             .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
                 .setName("NoCache")
                 .setFunctionId(NO_CACHE)
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
         final int functionId;
         
         try {
             functionId = ServerSideExtension.FunctionRequestHeader
             .parseFrom(metadata.get().get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER))).getFunctionId();
             logger.fine("Function nbr " + functionId + " was called.");
         } catch (Exception e) {
             logger.log(Level.WARNING, "Exception when trying to get the function request header.", e);
             responseObserver.onError(new Throwable("Exception when trying to get the function request header in executeFunction."));
             responseObserver.onCompleted();
             return responseObserver;
         }
         logger.info("executeFunction called. Function Id: " + functionId + ".");
         
         final ServerSideExtension.BundledRows.Builder bundledRowsBuilder = ServerSideExtension.BundledRows.newBuilder();
         final List<Double> columnSum = new ArrayList();
         final StringBuilder stringBuilder = new StringBuilder();
         
         return new io.grpc.stub.StreamObserver<qlik.sse.ServerSideExtension.BundledRows>() {
             
             @Override
             public void onNext(qlik.sse.ServerSideExtension.BundledRows bundledRows) {
                 logger.fine("onNext in executeFunction called.");
                 switch(functionId) {
                     case HELLO_WORLD:  responseObserver.onNext(helloWorld(bundledRows));
                              break;
                     case SUM_OF_ROWS:  responseObserver.onNext(sumOfRows(bundledRows));
                              break;
                     case SUM_OF_COLUMN:  columnSum.add(sumOfColumn(bundledRows));
                              break;
                     case STRING_AGGREGATION:  stringBuilder.append(stringAggregation(bundledRows));
                              break;
                     case CACHE:  responseObserver.onNext(cache(bundledRows));
                              break;
                     case NO_CACHE:  responseObserver.onNext(noCache(bundledRows));
                              break;
                     default: logger.log(Level.WARNING, "Incorrect function id.");
                              responseObserver.onError(new Throwable("Incorrect function id in onNext in executeFunction."));
                              responseObserver.onCompleted();
                              break;
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
                 switch(functionId) {
                     case SUM_OF_COLUMN: responseObserver.onNext(bundledRowsBuilder.addRows(ServerSideExtension.Row.newBuilder()
                         .addDuals(ServerSideExtension.Dual.newBuilder().setNumData(sum(columnSum)))).build());
                         break;
                     case STRING_AGGREGATION: responseObserver.onNext(bundledRowsBuilder.addRows(ServerSideExtension.Row.newBuilder()
                         .addDuals(ServerSideExtension.Dual.newBuilder().setStrData(stringBuilder.toString()))).build());
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
                 rowSum += dual.getNumData(); 
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
         StringBuilder strBuilder = new StringBuilder();
         for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
             strBuilder.append(row.getDuals(0).getStrData());
         }
         logger.fine("Function StringAggregation completed.");
         return strBuilder.toString();
     }
     
     private ServerSideExtension.BundledRows cache(ServerSideExtension.BundledRows bundledRows) {
         logger.fine("Function Cache called.");
         ServerSideExtension.BundledRows.Builder bundledRowsBuilder = ServerSideExtension.BundledRows.newBuilder();
         ServerSideExtension.Row.Builder rowBuilder;
         ServerSideExtension.Dual.Builder dualBuilder;
         
         for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
             StringBuilder strBuilder = new StringBuilder();
             strBuilder.append(row.getDuals(0).getStrData()).append("___").append(new Date().toString());
             rowBuilder = ServerSideExtension.Row.newBuilder();
             dualBuilder = ServerSideExtension.Dual.newBuilder();
             bundledRowsBuilder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(strBuilder.toString())));
         }
         logger.fine("Function Cache completed.");
         return bundledRowsBuilder.build();
     }
     
     private ServerSideExtension.BundledRows noCache(ServerSideExtension.BundledRows bundledRows) {
         logger.fine("Function NoCache called.");
         
         ServerSideExtension.BundledRows.Builder bundledRowsBuilder = ServerSideExtension.BundledRows.newBuilder();
         ServerSideExtension.Row.Builder rowBuilder;
         ServerSideExtension.Dual.Builder dualBuilder;
         
         for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
             StringBuilder strBuilder = new StringBuilder();
             strBuilder.append(row.getDuals(0).getStrData()).append("___").append(new Date().toString());
             rowBuilder = ServerSideExtension.Row.newBuilder();
             dualBuilder = ServerSideExtension.Dual.newBuilder();
             bundledRowsBuilder.addRows(rowBuilder.addDuals(dualBuilder.setStrData(strBuilder.toString())));
         }
         logger.fine("Function NoCache completed.");
         return bundledRowsBuilder.build();
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

         if(header != null ) {
             if(header.getParamsCount() == 0) {
                 ServerSideExtension.BundledRows result = evalScript(header, null);
                 if(result.getRowsCount() > 0) {
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
                     if(header.getFunctionType() == ServerSideExtension.FunctionType.AGGREGATION) {
                         logger.log(Level.WARNING, "Aggregation is not implemented in evaluate script.");
                         responseObserver.onCompleted();
                     }
                     ServerSideExtension.BundledRows result = evalScript(header, bundledRows);
                     if(result.getRowsCount() > 0) {
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
     
     private ServerSideExtension.BundledRows evalScript(ServerSideExtension.ScriptRequestHeader header, 
         ServerSideExtension.BundledRows bundledRows) {
         
         logger.fine("evalScript called");
         ScriptEngineManager manager = new ScriptEngineManager();
         ScriptEngine engine = manager.getEngineByName("JavaScript");
         ServerSideExtension.BundledRows.Builder bundledRowsBuilder = ServerSideExtension.BundledRows.newBuilder();
         
         String script = header.getScript();
         ServerSideExtension.DataType returnType = header.getReturnType();
         int nbrOfParams = header.getParamsCount();
         
         if(nbrOfParams == 0) {
             evalScript(script,bundledRowsBuilder, engine, returnType);
             logger.fine("evalScript completed");
             return bundledRowsBuilder.build();
         }
         
         Object[] args;
         for (ServerSideExtension.Row row : bundledRows.getRowsList()) {
             args = row.getDualsList().toArray();
             engine.put("args", args);             
             if(!evalScript(script,bundledRowsBuilder, engine, returnType)) {
                 return bundledRowsBuilder.build();
             }
         }
         logger.fine("evalScript completed");
         return bundledRowsBuilder.build();
     }
     
     private boolean evalScript(String script, ServerSideExtension.BundledRows.Builder bundledRowsBuilder, 
         ScriptEngine engine, ServerSideExtension.DataType returnType) {
         
         logger.finer("evalScript called from eval script");
         String result;
         try {
             Object res = engine.eval(script);
             result = res.toString();
             logger.finer("The string representation of the result: " + result);
         } catch (Exception e) {
             logger.log(Level.WARNING, "eval script did not work.", e);
             return false;
         }
         
         ServerSideExtension.Row.Builder rowBuilder = ServerSideExtension.Row.newBuilder();
         ServerSideExtension.Dual.Builder dualBuilder = ServerSideExtension.Dual.newBuilder();
         
         switch (returnType) {
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
                 return false;
         }
         return true;
     }
     
 }
