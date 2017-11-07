# Generating certificates

SSE plugins use certificates to enable a secure connection between the SSE plugins and Qlik. We provide Windows and Linux scripts for generating these certificates. Use the operating system that suits you best. You only need to run the script once. These scripts generate certificates and keys to enable mutual authentication (both server authentication and client authentication). Recall that Qlik acts as the client, and the SSE plugin acts as the server.

## Prerequisites
You must have OpenSSL installed:

* On Windows, one alternative is to use [this](https://slproweb.com/products/Win32OpenSSL.html) prebuilt package. The executable can be downloaded directly by going [here](https://slproweb.com/download/Win64OpenSSL-1_1_0e.exe).

* Most Linux distributions have OpenSSL installed by default. If it is not present on your computer, make sure to install the OpenSSL package before following the instructions below.

## Generating certificates on Windows

Do the following:

1. In file named *sse_server_config.txt*, edit the `[alt_names]` list to include the domains you require in the certificates.

2. Run the batch script from a command prompt, passing the SSE plugin name as an argument.  
Example:  
    >`$ generate_sse_certs.bat <myplugin>`


## Generating certificates on Linux

Do the following:

1. In file named *sse_server_config.txt*, edit the `[alt_names]` list to include the domains you require in the certificates.

2. Run the *.sh* script in a terminal, passing the SSE plugin name as an argument.  
Example:  
    > `./generate_sse_certs.sh <myplugin>`

If you have problems executing the script, make sure you have execute permissions on the file:  
>`chmod u+x generate_sse_certs.sh`

## Using the generated certificates

Do the following:

1. Copy the folder named *sse_qliktest_client_certs_used_by_qlik* to the Qlik computer (the client). You must configure the SSE plugin in Qlik; make sure you refer to the file location where you copied the certificates in the __Certificate file path__ field of the __Analytic connections__ section in the QMC, or when editing the SSEPlugin setting in the settings.ini file. See [Configuring SSE plugins in Qlik](../docs/configuration.md).

2. The SSE plugin is the server in the SSE communication, so copy and refer to the folder named *sse_qliktest_server_certs* in the plugin server.

**Note:** Do not rename the certificate files. The client file names must be exactly as the script names them (*root_cert.pem*, *sse_client_cert.pem*, *sse_client_key.pem*), otherwise the Qlik engine will not be able to find them.
