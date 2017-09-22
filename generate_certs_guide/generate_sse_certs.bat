@echo off
IF %1.==. GOTO NoArg
echo on

REM Generate root key and root certificate.
openssl req -x509 -newkey rsa:2048 -keyout root_key.pem -out root_cert.pem -subj "/CN=sse_root"  -days 3650 -nodes -batch

REM Generate key for SSE server
openssl genrsa -out sse_server_key.pem 2048

REM Use SSE server key to generate Certificate Signing Request asking a Certificate Authority to sign a certificate with Common Name "sse_server".
openssl req -new -key sse_server_key.pem -out sse_server.csr -subj "/CN=sse_server" -batch

REM Use root certificate as Authority for creating a certificate according to the request and store it in a file called "sse_server_cert.pem".
REM Add extension information from the "sse_server_config.txt" file, where the Subject Alternative Name extension is used to cover a number of host names.
openssl x509 -req -days 3650 -in sse_server.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 01 -out sse_server_cert.pem -extfile sse_server_config.txt

REM Generate key for SSE client
openssl genrsa -out sse_client_key.pem 2048

REM Use SSE client key to generate Certificate Signing Request asking a Certificate Authority to sign a certificate with Common Name "sse_client".
openssl req -new -key sse_client_key.pem -out sse_client.csr -subj "/CN=sse_client" -batch

REM User root certificate as Authority for creating a certificate according to the request and store it in a file called "sse_client_cert.pem".
REM Add extension information from the "sse_client_config.txt" file.
openssl x509 -req -days 3650 -in sse_client.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 02 -out sse_client_cert.pem -extfile sse_client_config.txt

set pluginname=%1
mkdir sse_%pluginname%_generated_certs

REM Store away the root key in a safe place -- or even delete it if you are prepared to recreate and redeploy all certificates if you want to change something.
mkdir sse_%pluginname%_generated_certs\sse_%pluginname%_store_at_safe_location

REM Certificates for server
mkdir sse_%pluginname%_generated_certs\sse_%pluginname%_server_certs

REM Certificates for client
mkdir sse_%pluginname%_generated_certs\sse_%pluginname%_client_certs_used_by_qlik


move root_key.pem sse_%pluginname%_generated_certs\sse_%pluginname%_store_at_safe_location
move sse_client.csr sse_%pluginname%_generated_certs\sse_%pluginname%_store_at_safe_location
move sse_server.csr sse_%pluginname%_generated_certs\sse_%pluginname%_store_at_safe_location

copy root_cert.pem sse_%pluginname%_generated_certs\sse_%pluginname%_server_certs

move root_cert.pem sse_%pluginname%_generated_certs\sse_%pluginname%_client_certs_used_by_qlik
move sse_client_cert.pem sse_%pluginname%_generated_certs\sse_%pluginname%_client_certs_used_by_qlik
move sse_client_key.pem sse_%pluginname%_generated_certs\sse_%pluginname%_client_certs_used_by_qlik

move sse_server_cert.pem sse_%pluginname%_generated_certs\sse_%pluginname%_server_certs
move sse_server_key.pem sse_%pluginname%_generated_certs\sse_%pluginname%_server_certs

@echo off
GOTO End1

:NoArg
  ECHO No plugin name argument given. No certificates generated.

:End1
ECHO Done!!