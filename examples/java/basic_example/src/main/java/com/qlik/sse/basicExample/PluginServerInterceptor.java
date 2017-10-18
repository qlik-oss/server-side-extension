package com.qlik.sse.basicexample;

import qlik.sse.ServerSideExtension;

import io.grpc.ServerInterceptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Metadata;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;


public class PluginServerInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(PluginServerInterceptor.class.getName());

    private JavaPlugin plugin;
    
    public PluginServerInterceptor(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public <RequestT,ResponseT>ServerCall.Listener<RequestT> interceptCall(
        ServerCall<RequestT,ResponseT> serverCall, final Metadata metadata, ServerCallHandler<RequestT,ResponseT> serverCallHandler) {
        logger.finer("Intercepting call to get metadata.");
        plugin.setMetadata(metadata);
        logHeader(metadata);
        return serverCallHandler.startCall(new SimpleForwardingServerCall<RequestT,ResponseT>(serverCall){
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                logger.finest("Send headers.");
                try {
                    ServerSideExtension.FunctionRequestHeader header = ServerSideExtension.FunctionRequestHeader
                    .parseFrom(metadata.get(Metadata.Key.of("qlik-functionrequestheader-bin", BINARY_BYTE_MARSHALLER)));
                    logger.finest("Function request header.");
                    logHeader(responseHeaders);
                    if(header.getFunctionId() == 5) {
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
}