using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Google.Protobuf;
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

        private enum FunctionConstant
        {
            Add42,
            SumOfAllNumbers,
            SmartGuessDate
        };

        private static readonly Capabilities ConnectorCapabilities = new Capabilities
        {
            PluginIdentifier = "CSharp Basic example",
            PluginVersion = "1.0.0",
            AllowScript = true,
            Functions =
            {
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.Add42,
                    FunctionType = FunctionType.Scalar,
                    Name = "Add42",
                    Params = {new Parameter {Name = "SingleNumericColumn", DataType = DataType.Numeric} },
                    ReturnType = DataType.Numeric
                },
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.SumOfAllNumbers,
                    FunctionType = FunctionType.Aggregation,
                    Name = "SumOfAllNumbers",
                    Params = {new Parameter {Name = "AnyShapeOfTable", DataType = DataType.Numeric} },
                    ReturnType = DataType.Numeric
                },
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.SmartGuessDate,
                    FunctionType = FunctionType.Scalar,
                    Name = "SmartGuessDate",
                    Params = {new Parameter {Name = "DateString", DataType = DataType.String}, new Parameter {Name = "CultureString", DataType = DataType.String} },
                    ReturnType = DataType.Dual
                }
            }
        };
        public override Task<Capabilities> GetCapabilities(Empty request, ServerCallContext context)
        {
            Console.WriteLine("-- GetCapabilities --");

            TraceServerCallContext(context);

            return Task.FromResult(ConnectorCapabilities);
        }

        public override async Task ExecuteFunction(IAsyncStreamReader<BundledRows> requestStream, IServerStreamWriter<BundledRows> responseStream, ServerCallContext context)
        {

            Console.WriteLine("-- ExecuteFunction --");

            TraceServerCallContext(context);

            var functionRequestHeaderStream = context.RequestHeaders.SingleOrDefault(header => header.Key == "qlik-functionrequestheader-bin");

            if (functionRequestHeaderStream == null)
            {
                throw new Exception("ExecuteFunction called without Function Request Header in Request Headers.");
            }

            var functionRequestHeader = new FunctionRequestHeader();
            functionRequestHeader.MergeFrom(new CodedInputStream(functionRequestHeaderStream.ValueBytes));



            Console.WriteLine($"FunctionRequestHeader.FunctionId : {functionRequestHeader.FunctionId}");
            Console.WriteLine($"FunctionRequestHeader.Version : {functionRequestHeader.Version}");

            var requestAsList = await requestStream.ToListAsync(); // We want to be sure to keep the order of rows when executing and writing to the response.

            switch (functionRequestHeader.FunctionId)
            {
                case (int)FunctionConstant.Add42:
                    {
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
                        break;
                    }
                case (int)FunctionConstant.SumOfAllNumbers:
                    {
                        double sum = 0.0;
                        foreach (var bundledRows in requestAsList)
                        {

                            foreach (var row in bundledRows.Rows)
                            {
                                sum = sum + row.Duals.Select(d => d.NumData).Sum();
                            }

                        }

                        var resultBundle = new BundledRows();
                        var resultRow = new Row();
                        resultRow.Duals.Add(new Dual { NumData = sum });
                        resultBundle.Rows.Add(resultRow);
                        await responseStream.WriteAsync(resultBundle);
                        break;
                    }
                case (int)FunctionConstant.SmartGuessDate:
                {
                    foreach (var bundledRows in requestAsList)
                    {
                        var resultBundle = new BundledRows();
                        foreach (var row in bundledRows.Rows)
                        {
                            var dateStringParam = row.Duals[0].StrData;
                            var cultureParam = row.Duals[1].StrData;

                            var guessedDate =
                                CultureGuessingDateParser.DateFromStringGuessingCulture(dateStringParam, cultureParam);
                            
                            

                            var resultRow = new Row();
                            resultRow.Duals.Add(new Dual { NumData = (guessedDate-CultureGuessingDateParser.QlikDateBeforeFirstDate).Days, StrData = guessedDate.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)});
                            resultBundle.Rows.Add(resultRow);
                        }
                        await responseStream.WriteAsync(resultBundle);
                    }
                    break;
                }
                default:
                    break;

            }



        }

        private static void TraceServerCallContext(ServerCallContext context)
        {
            var authContext = context.AuthContext;

            Console.WriteLine($"ServerCallContext.Method : {context.Method}");
            Console.WriteLine($"ServerCallContext.Host : {context.Host}");
            Console.WriteLine($"ServerCallContext.Peer : {context.Peer}");
            foreach (var contextRequestHeader in context.RequestHeaders)
            {
                Console.WriteLine(
                    $"{contextRequestHeader.Key} : {(contextRequestHeader.IsBinary ? "<binary>" : contextRequestHeader.Value)}");

                if (contextRequestHeader.Key == "qlik-functionrequestheader-bin")
                {
                    var functionRequestHeader = new FunctionRequestHeader();
                    functionRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Console.WriteLine($"FunctionRequestHeader.FunctionId : {functionRequestHeader.FunctionId}");
                    Console.WriteLine($"FunctionRequestHeader.Version : {functionRequestHeader.Version}");
                }
                else if (contextRequestHeader.Key == "qlik-commonrequestheader-bin")
                {
                    var commonRequestHeader = new CommonRequestHeader();
                    commonRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Console.WriteLine($"CommonRequestHeader.FunctionId : {commonRequestHeader.AppId}");
                    Console.WriteLine($"CommonRequestHeader.Cardinality : {commonRequestHeader.Cardinality}");
                    Console.WriteLine($"CommonRequestHeader.UserId : {commonRequestHeader.UserId}");
                }
                else if (contextRequestHeader.Key == "qlik-scriptrequestheader-bin")
                {
                    var scriptRequestHeader = new ScriptRequestHeader();
                    scriptRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Console.WriteLine($"ScriptRequestHeader.FunctionType : {scriptRequestHeader.FunctionType}");
                    Console.WriteLine($"ScriptRequestHeader.ReturnType : {scriptRequestHeader.ReturnType}");

                    int paramIdx = 0;
                    foreach (var parameter in scriptRequestHeader.Params)
                    {
                        Console.WriteLine($"ScriptRequestHeader.Params[{paramIdx}].Name : {parameter.Name}");
                        Console.WriteLine($"ScriptRequestHeader.Params[{paramIdx}].DataType : {parameter.DataType}");
                        ++paramIdx;
                    }
                    Console.WriteLine($"CommonRequestHeader.Script : {scriptRequestHeader.Script}");
                }
            }

            Console.WriteLine($"ServerCallContext.AuthContext.IsPeerAuthenticated : {authContext.IsPeerAuthenticated}");
            Console.WriteLine(
                $"ServerCallContext.AuthContext.PeerIdentityPropertyName : {authContext.PeerIdentityPropertyName}");
            foreach (var authContextProperty in authContext.Properties)
            {
                var loggedValue = authContextProperty.Value;
                var firstLineLength = loggedValue.IndexOf('\n');
                if (firstLineLength > 0)
                {
                    loggedValue = loggedValue.Substring(0, firstLineLength) + "<truncated at linefeed>";
                }

                Console.WriteLine($"{authContextProperty.Name} : {loggedValue}");
            }
        }

        public override async Task EvaluateScript(IAsyncStreamReader<BundledRows> requestStream, IServerStreamWriter<BundledRows> responseStream, ServerCallContext context)
        {
            Console.WriteLine("-- EvaluateScript --");

            TraceServerCallContext(context);

            await base.EvaluateScript(requestStream, responseStream, context);
        }
    }
}
