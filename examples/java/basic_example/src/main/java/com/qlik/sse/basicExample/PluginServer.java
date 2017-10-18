package com.qlik.sse.basicexample;

import qlik.sse.ServerSideExtension;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

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
    private JavaPlugin plugin = new JavaPlugin();
    
    public PluginServer(int port, String pemDir) throws IOException {
        this.port = port;
        ServerBuilder serverBuilder;
        
        if(!pemDir.isEmpty()) {
            try {
                serverBuilder = ServerBuilder.forPort(port)
                .useTransportSecurity(new File(pemDir, "sse_server_cert.pem"), new File(pemDir, "sse_server_key.pk8"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not create a secure connection.", e);
                serverBuilder = ServerBuilder.forPort(port);
            }
            
        } else {
            serverBuilder = ServerBuilder.forPort(port);
        }
        
        server = serverBuilder.addService(plugin)
        .intercept(new PluginServerInterceptor(plugin))
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

    public static void main(String[] args) throws Exception {
        int port = 50071;
        String pemDir = "";
        for(int i = 0; i < args.length-1; i += 2) {
            if(args[i].equals("--port")) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Invalid port, using default value: " + port);
                }
            } else if (args[i].equals("--pemDir")) {
                pemDir = args[i + 1];
            }
            
        }
        PluginServer server = new PluginServer(port, pemDir);
        server.start();
        server.blockUntilShutdown(); 
        return;
        
    }
}

