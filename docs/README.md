# Overview

The Qlik server-side extension protocol is built using [gRPC - An RPC library and framework](http://github.com/grpc/grpc).  To read more about gRPC, see [gRPC - A high performance, open-source universal RPC framework](http://www.grpc.io/). 

Using gRPC provides a number of benefits:
- The SSE plugin (or analytic connection) can be deployed on a computer other than the one Qlik is deployed on.
- You can easily create SSE plugins in any of the languages that gRPC supports out of the box (over ten languages).

The SSE protocol also makes use of [protocol buffers](https://developers.google.com/protocol-buffers/), Google's language-neutral, platform-neutral, extensible mechanism for serializing structured data. We have specified all default values according to [Optional Fields And Default Values](https://developers.google.com/protocol-buffers/docs/proto#optional).

The following diagram shows an example of SSE plugins communicating with Qlik:

![](SSE_overview.png)

**Note:** In the context of SSE communication:  
- Qlik is the **client**
- The SSE plugin (Python, etc.) is the **server**.  

This means that the server (that is, the plugin) must be started and running before the Qlik engine is started.

## Considerations

* **General security attention**  
If you allow scripts to be executed in your plugin, consider the following.
Since scripts can be very powerful and you will never know what script will be executed by your plugin (and the script engine used) you must be extra careful to secure the machine that your plugin and the script engine are deployed to as much as you can. If possible, sandbox the execution. Be aware of which user account that is starting the plugin and script engine and what access rights this user got in the machine and in your domain to minimize any harm a malicious script can cause. 

* **Secure connection using certificates**  
Use secure connection between the plugin server and Qlik by using certificates with mutual authentication. See [Generating certificates](../generate_certs_guide/README.md) that explains how to generate proper certificates.

* **Potential bottleneck**  
SSE plugins will feel slower than the normal Qlik engine calculations that use in-memory data. SSE plugins may also become a bottleneck if massive usage is applied (many measures, many users, many apps, a lot of data), so be sure to architect your SSE deployment very carefully.

* **Order of rows (data) sent from Qlik**  
The order of the data sent to the SSE plugin may not be the same as the order you see in the visualization. It is up to you as a developer to preserve the order when you return the values from the plugin to the Qlik client.

* **Recovering lost connection**  
If the connection to the plugin is lost, the Qlik engine attempts to reconnect after 20 seconds by default. You can configure a different timeout interval in QMC.

For more information, see [Limitations](limitations.md).