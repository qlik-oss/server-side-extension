using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Grpc.Core;
using Grpc.Core.Utils;
using Qlik.Sse;

namespace Basic_example
{
    /// <summary>
    /// The BasicExampleConnector inherits the generated class Qlik.Sse.Connector.ConnectorBase
    /// </summary>
    class BasicExampleConnector : Qlik.Sse.Connector.ConnectorBase
    {
        public override Task<Capabilities> GetCapabilities(Empty request, ServerCallContext context)
        {
            Console.WriteLine("GetCapabilities");
            return Task.FromResult(new Capabilities
            {
                PluginIdentifier = "CSharp Basic example",
                PluginVersion = "1.0.0",
                AllowScript = false,
                Functions =
                {
                    new FunctionDefinition {FunctionId = 0, FunctionType = FunctionType.Scalar, Name = "OneConstantPerRow", Params = {new Parameter {Name = "SingleUnusedColumn", DataType = DataType.Dual} }, ReturnType = DataType.Numeric}
                }                
            }
                
            );
        }

        public override async Task ExecuteFunction(IAsyncStreamReader<BundledRows> requestStream, IServerStreamWriter<BundledRows> responseStream, ServerCallContext context)
        {
            Console.WriteLine("ExecuteFunction");

            var requestAsList = await requestStream.ToListAsync(); // We want to be sure to keep the order of rows when executing and writing to the response.

            foreach (var bundledRows in requestAsList)
            {
                var resultBundle = new BundledRows();
                foreach (var row in bundledRows.Rows)
                {
                    var resultRow = new Row();
                    resultRow.Duals.Add(new Dual { NumData = row.Duals[0].NumData + 42.0 });
                    resultBundle.Rows.Add(resultRow);
                }
                await responseStream.WriteAsync(resultBundle);
            }
        }

        public override async Task EvaluateScript(IAsyncStreamReader<BundledRows> requestStream, IServerStreamWriter<BundledRows> responseStream, ServerCallContext context)
        {
            Console.WriteLine("EvaluateScript");
            await base.EvaluateScript(requestStream, responseStream, context);
        }
    }
}
