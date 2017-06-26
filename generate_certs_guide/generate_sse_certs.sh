#!/bin/bash

#set -ex

if [[ "$#" -ne 1 ]]; then
    echo "No plugin name argument given. No certificates generated."
    echo ""
    echo "Usage: $0 <plugin_name>"
    echo ""
    exit 1
fi

openssl req -x509 -newkey rsa:2048 -keyout root_key.pem -out root_cert.pem -subj "/CN=sse_root"  -days 3650 -nodes -batch

openssl genrsa -out sse_server_key.pem 2048

openssl req -new -key sse_server_key.pem -out sse_server.csr -subj "/CN=sse_server" -batch

openssl x509 -req -days 3650 -in sse_server.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 01 -out sse_server_cert.pem -extfile sse_server_config.txt

openssl genrsa -out sse_client_key.pem 2048

openssl req -new -key sse_client_key.pem -out sse_client.csr -subj "/CN=sse_client" -batch

openssl x509 -req -days 3650 -in sse_client.csr -CA root_cert.pem -CAkey root_key.pem -set_serial 02 -out sse_client_cert.pem -extfile sse_client_config.txt

pluginname=$1
folder1=sse_"${pluginname}"_generated_certs
folder2=sse_"${pluginname}"_generated_certs/sse_"${pluginname}"_store_at_safe_location
folder3=sse_"${pluginname}"_generated_certs/sse_"${pluginname}"_server_certs
folder4=sse_"${pluginname}"_generated_certs/sse_"${pluginname}"_client_certs_used_by_qlik
mkdir -p $folder1
mkdir -p $folder2
mkdir -p $folder3
mkdir -p $folder4

mv root_key.pem $folder2/
mv sse_client.csr $folder2
mv sse_server.csr $folder2
cp root_cert.pem $folder3
mv root_cert.pem $folder4
mv sse_client_cert.pem $folder4
mv sse_client_key.pem $folder4
mv sse_server_cert.pem $folder3
mv sse_server_key.pem $folder3

echo "Done!!"