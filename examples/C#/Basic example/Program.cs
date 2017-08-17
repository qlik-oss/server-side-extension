using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Grpc.Core;

namespace Basic_example
{
    class Program
    {
        static void Main(string[] args)
        {
            var grpcHost = "localhost";
            int grpcPort = 50051;
            var certificateFolderFullPath =
                @"..\..\..\..\..\generate_certs_guide\sse_qliktest_generated_certs\sse_qliktest_server_certs";


            var sslCredentials = ServerCredentials.Insecure;

            if (certificateFolderFullPath.Length > 3)
            {
                var rootCertPath = Path.Combine(certificateFolderFullPath, @"root_cert.pem");
                var serverCertPath = Path.Combine(certificateFolderFullPath, @"sse_server_cert.pem");
                var serverKeyPath = Path.Combine(certificateFolderFullPath, @"sse_server_key.pem");
                if (File.Exists(rootCertPath) &&
                    File.Exists(serverCertPath) &&
                    File.Exists(serverKeyPath))
                {
                    var rootCert = File.ReadAllText(rootCertPath);
                    var serverCert = File.ReadAllText(serverCertPath);
                    var serverKey = File.ReadAllText(serverKeyPath);
                    var serverKeyPair = new KeyCertificatePair(serverCert, serverKey);
                    sslCredentials = new SslServerCredentials(new List<KeyCertificatePair>() { serverKeyPair }, rootCert, true);

                    Console.WriteLine($"Path to certificates ({certificateFolderFullPath}) and certificate files found. Opening secure channel with mutual authentication.");
                }
                else
                {
                    Console.WriteLine($"Path to certificates ({certificateFolderFullPath}) not found or files missing. Opening insecure channel instead.");
                }
            }
            else
            {
                Console.WriteLine("No certificates defined. Opening insecure channel.");
            }

            var server = new Grpc.Core.Server
            {
                Services = {Qlik.Sse.Connector.BindService(new BasicExampleConnector())},
                Ports = {new ServerPort(grpcHost, grpcPort, sslCredentials)}
            };

            server.Start();
            Console.WriteLine("Press any key to stop Basic example...");
            Console.WriteLine($"gRPC listening on port {grpcPort}");
            Console.ReadLine();
            Console.WriteLine("Shutting Basic example... Bye!");
            server?.ShutdownAsync().Wait();

        }
    }
}
