# Writing an SSE Plugin using Java

Execute functions -Java, Evaluate script -JavaScript (alternativt använd en java compilator...)

Synchronus/asynchronus --?

## Running the example
This example was created using maven. There are other ways to do this, what you need is the protocol buffer package and the grpc package (in that order) and the protoc compiler to generate the code from the .proto file.

If you want to run the example using maven, use the standard maven source tree (and make sure you have installed [maven](http://maven.apache.org/ )). Start with putting the *pom.xml* file in a directory somewhere (from now on called *my-app*). In the *my-app* directory create a folder called *src*. Place the *ServerSideExtension.proto* file both in *src/main/proto* and in *src/test/proto*. The *PluginServer.java* file should be placed in *src/main/java/Plugin* and the *AppTest.java* file should be placed in *src/test/java/Plugin*.


###?????Install Protocolbuffer?????
?????download a protoc compiler (protoc-[version]-[Platform].zip) and and the protocol buffer for java zip or tar.gz (protobuf-java-[version]-zip/tar.gz) from the [released protbuf stuff](https://github.com/google/protobuf/releases) ????????
?????Make sure the version number is the same. Extract the files, go to the java directory and follow the instructions in the [README](https://github.com/google/protobuf/blob/master/java/README.md). You have already done half of step two.???????

###?????Install grpc??????
?????Download [grpc-java](https://github.com/grpc/grpc-java) and in the grpc-java directory create a *gradle.properties* file with the text *skipCodegen=true*. **In a comand prompt:** go to this directory, run *gradlew.bat build* and *gradlew.bat install*.?????
?????This installs grpc in your local maven repositiry??????

###Generate the java plugin (and the classes from the .proto file)
**In a comand prompt:** go to the *my-app* directory and run *mvn package*.

Now you can see a new folder called *target* in the *my-app* directory. In the *target* folder you can find a jar file, that is the plugin. The code generated from the *.proto* file can be found in the *generated-sources* folder.

###Changing the default logging
Move the *javapluginlogger.properties* file to the *target* directory (it needs to be in the same directory as the *my-app-1.0-SNAPSHOT.jar* file).
In this file you can edit the settings for the logging. There are differnt [levels](https://docs.oracle.com/javase/7/docs/api/java/util/logging/Level.html) of logging depending on how much information you want (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL).

**.level** sets the default level for all loggers, in this case they are set to info. 
**java.util.logging.ConsoleHandler.level** sets the level for the console handler, i.e. the level of logging sent to the console output (can't be more than the level of the loggers). It is possible to add more handlers to different outputs (as an example you can add a file handler to send logging information to a file). 
**java.util.logging.ConsoleHandler.formatter** sets what format to use for the console output. 
**PluginServer.level** sets the level of the logger called PluginServer i.e. the logger used in the plugin.

###Running the plugin with ansecure connection
**In a command prompt:** go to the target directory and run *java -jar my-app-1.0-SNAPSHOT.jar*. This will start the java plugin listening on port 50071. If you want to specify which port the plugin should listen to run *java -jar my-app-1.0-SNAPSHOT.jar --port [Port number]* instead.

####Connecting to Qlik Sense Desktop
Install Qlik Sense Desktop June release (or later?).
To be able to connect to Sense you need to specify which port Sense shold look for a plugin at. Go to *Documents/Qlik/Sense* and create or edit the *Settings.ini* file so that it includes the line *SSEPlugin=javaPlugin,localhost:50071*. *javaPlugin* is the name of the plugin. This can be whatever you want but since the name of the plugin in the example app is set to *javaPlugin* the example will not work if you set it to something else (unless you change the name of the plugin in every call to the plugin in the app). *50071* is the port. You can change this to *[Port number]* if you specified *--port [Port number]* on the command line when you startet the plugin.
Move *SSEJavePlugin.qvf* to *Documents/Qlik/Sense/Apps*.
Start Qlik Sense Desktop (the plugin needs to be running when you do this) and open the app.

###Running the plugin with a secure conection
####Generate certificates
Follow the steps in [the generate certs guide](https://github.com/qlik-oss/server-side-extension/tree/master/generate_certs_guide).
Then you have two options: install some library (no idéa which one yet and make some changes to the code) or convert the server key to PKCS#8 format.
**In a command prompt:** Go to the folder *generate_certs_guide/sse_<Plugin_name>*_generated_certs/sse_<Plugin_name>_server_certs* and run *openssl pkcs8 -in sse_server_key.pem -topk8 -nocrypt -out sse_server_key.pk8*

####Connecting to Qlik Sense Enterprise
Install Qlik Sense Enterprise June release (or later?).
Go to the QMC ([don't forget to make sure you have permission to create apps](https://help.qlik.com/en-US/sense/June2017/Subsystems/ManagementConsole/Content/allocate-user-access.htm)).



##Writing your own plugin

[SecureConnections](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#transport-security-tls)



