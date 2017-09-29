# Running the example

## Prerequisites
This example was created using Maven, so the easiest way to run the example is to install [Maven](http://maven.apache.org/ ). 

There are other ways to build the example plugin. If you don't use Maven, what you need is the protocol buffer package and the grpc package (in that order) and the protoc 
compiler to generate the code from the .proto file. You also need Java 8.

## Generate the Java plugin (and the classes from the .proto file)
**In a command prompt:** go to the `basic_example` directory and run `mvn package`.

Now, go to the `target` directory in the `basic_example` directory. In the `target` folder, you can find a jar file, that is the plugin. The code generated from the `.proto` file can be found 
in the `generated-sources` folder.

### If you are having trouble...
It is possible to install the protobuf and the grpc packages manually if they for some reason are not downloaded by maven when you run `mvn package`. If you need to do this, install the protocol buffer first
since grpc is dependent on the protocol buffer. 

#### Install Protocol buffer
Download a protoc compiler (protoc-[version]-[Platform].zip) and and the protocol buffer for Java zip or tar.gz (protobuf-java-[version]-zip/tar.gz) from the 
[release page for protbuf](https://github.com/google/protobuf/releases). Make sure the version number is the same. Extract the files, go to the java directory and follow the instructions 
in the [README](https://github.com/google/protobuf/blob/master/java/README.md). You have already done half of step two.

#### Install Grpc
Download [grpc-java](https://github.com/grpc/grpc-java) and in the grpc-java directory create a `gradle.properties` file with the text `skipCodegen=true`. 

**In a command prompt:** go to the grpc-java directory, run `gradlew.bat build` and `gradlew.bat install`. This installs grpc in your local maven repository (usually located at `C:/Users/<user>/.m2`).

## Changing the default logging
In the `basic_example/target` directory you find the `javapluginlogger.properties` file.
In this file you can edit the settings for the logging. There are different [levels](https://docs.oracle.com/javase/7/docs/api/java/util/logging/Level.html) of logging depending on how much 
information you want (`OFF`, `SEVERE`, `WARNING`, `INFO`, `CONFIG`, `FINE`, `FINER`, `FINEST`, `ALL`).

## Running the plugin with an insecure connection
**In a command prompt:** go to the target directory and run `java -jar basic-example-1.0-SNAPSHOT.jar`. This will start the Java plugin listening on port 50071. If you want to specify which port 
the plugin should listen to run `java -jar basic-example-1.0-SNAPSHOT.jar --port <Port number>` instead.

### Connecting to Qlik Sense Desktop
Install Qlik Sense Desktop June release or later.
To be able to connect to Sense you need to specify which port Sense should look for a plugin at. Go to `Documents/Qlik/Sense` and create or edit the `Settings.ini` file so that it includes 
the line `SSEPlugin=javaPlugin,localhost:50071`. `javaPlugin` is the name of the plugin that is used by the example app in Sense. The name is arbitrary, if you use the same name in the app as in the settings file. 
`50071` is the port. You should change this to `<Port number>` if you specified `--port <Port number>` in the command prompt when you started the plugin.

Move `SSEJavaPlugin.qvf` to `Documents/Qlik/Sense/Apps`, start Qlik Sense Desktop (the plugin needs to be running when you do this) and open the app.

## Running the plugin with a secure connection
### Generate certificates
Follow the steps in the [generate certs guide](https://github.com/qlik-oss/server-side-extension/tree/master/generate_certs_guide).
You also need to convert the server key to PKCS#8 format.

**In a command prompt:** Go to the folder `generate_certs_guide/sse_<Plugin_name>_generated_certs/sse_<Plugin_name>_server_certs` and run `openssl pkcs8 -in sse_server_key.pem -topk8 -nocrypt -out sse_server_key.pk8`

### Connecting to Qlik Sense Enterprise
Install Qlik Sense Enterprise June release or later.
Go to the QMC ([don't forget to make sure you have permission to create apps](https://help.qlik.com/en-US/sense/June2017/Subsystems/ManagementConsole/Content/allocate-user-access.htm)).
In the `start` drop down menu in the upper left corner choose `Analytic connections`. Choose `Create new`, give it the name `javaPlugin`, set host to `localhost`, add the certificates used by Qlik and the port number you want to use.
Return to the `start` drop down menu, go to `Apps`, choose `Import` and navigate to the example app `SSEJavaPlugin`. 
Start the plugin. 

**In a command prompt:** Go to `basic_example/target` and run `java -jar basic-example-1.0-SNAPSHOT.jar --pemDir [some location]\generate_certs_guide\sse_<Plugin name>_generated_certs\sse_<Plugin name>_server_certs`. 
If you want, you can specify the port with `--port <Port number>`.
Restart Qlik Sense Enterprise and go to the hub.
