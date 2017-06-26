@echo off
IF %1.==. GOTO NoArg
echo on

openssl req -x509 -newkey rsa:2048 -keyout root_key.pem -out root_cert.pem -subj "/CN=sse_root"  -days 3650 -nodes -batch

openssl genrsa -out sse_server_key.pem 2048

openssl req -new -key sse_server_key.pem -out sse_server.csr -subj "/CN=sse_server" -batch

openssl x509 -req -days 3650 -in sse_server.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 01 -out sse_server_cert.pem -extfile sse_server_config.txt

openssl genrsa -out sse_client_key.pem 2048

openssl req -new -key sse_client_key.pem -out sse_client.csr -subj "/CN=sse_client" -batch

openssl x509 -req -days 3650 -in sse_client.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 02 -out sse_client_cert.pem -extfile sse_client_config.txt

set pluginname=%1
mkdir sse_%pluginname%_generated_certs
mkdir sse_%pluginname%_generated_certs\sse_%pluginname%_store_at_safe_location
mkdir sse_%pluginname%_generated_certs\sse_%pluginname%_server_certs
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