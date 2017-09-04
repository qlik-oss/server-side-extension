using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Grpc.Core;
using NLog;

namespace Basic_example
{
    class Program
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        static void Main(string[] args)
        {
            Logger.Info(
                $"{Path.GetFileName(System.Reflection.Assembly.GetExecutingAssembly().Location)} uses NLog. Set log level by adding or changing logger rules in NLog.config, setting minLevel=\"Info\" or \"Debug\" or \"Trace\".");

            Logger.Info(
                $"Changes to NLog config are immediately reflected in running application, unless you change the setting autoReload=\"true\".");

            var grpcHost = Convert.ToString(Properties.Settings.Default.grpcHost ?? "localhost");
            int grpcPort = Convert.ToInt32(Properties.Settings.Default.grpcPort ?? "50054");

            var certificateFolderFullPath = Convert.ToString(Properties.Settings.Default.certificateFolderFullPath ?? "");

            var sslCredentials = ServerCredentials.Insecure;

            Logger.Info("Looking for certificates according to certificateFolderFullPath in config file.");

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

                    Logger.Info($"Path to certificates ({certificateFolderFullPath}) and certificate files found. Opening secure channel with mutual authentication.");
                }
                else
                {
                    Logger.Warn($"Path to certificates ({certificateFolderFullPath}) not found or files missing. Opening insecure channel instead.");
                }
            }
            else
            {
                Logger.Info("No certificates defined. Opening insecure channel.");
            }

            var server = new Grpc.Core.Server
            {
                Services = {Qlik.Sse.Connector.BindService(new BasicExampleConnector())},
                Ports = {new ServerPort(grpcHost, grpcPort, sslCredentials)}
            };

            server.Start();
            Logger.Info($"gRPC listening on port {grpcPort}");

            Logger.Info("Press any key to stop gRPC server and exit...");

            Console.ReadKey();
            server.ShutdownAsync().Wait();

        }
    }
}
