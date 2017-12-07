using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Globalization;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Google.Protobuf;
using Grpc.Core;
using Grpc.Core.Utils;
using NLog;
using Qlik.Sse;

namespace Basic_example
{
    /// <summary>
    /// The BasicExampleConnector inherits the generated class Qlik.Sse.Connector.ConnectorBase
    /// </summary>
    class BasicExampleConnector : Connector.ConnectorBase
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        private enum FunctionConstant
        {
            Add42,
            SumOfAllNumbers,
            Concatenate,
            CallCounter,
            CallCounterNoCache
        };

        private static readonly Capabilities ConnectorCapabilities = new Capabilities
        {
            PluginIdentifier = "CSharp Basic example",
            PluginVersion = "1.0.0",
            AllowScript = false,
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
                    Params = {new Parameter {Name = "SingleNumericColumn", DataType = DataType.Numeric} },
                    ReturnType = DataType.Numeric
                },
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.Concatenate,
                    FunctionType = FunctionType.Aggregation,
                    Name = "Concatenate",
                    Params = {new Parameter {Name = "ColumnarData", DataType = DataType.Dual} },
                    ReturnType = DataType.String
                },
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.CallCounter,
                    FunctionType = FunctionType.Scalar,
                    Name = "CallCounter",
                    Params = {new Parameter {Name = "DummyData", DataType = DataType.Dual}},
                    ReturnType = DataType.Numeric
                },
                new FunctionDefinition {
                    FunctionId = (int)FunctionConstant.CallCounterNoCache,
                    FunctionType = FunctionType.Scalar,
                    Name = "CallCounterNoCache",
                    Params = {new Parameter {Name = "DummyInt", DataType = DataType.Dual} },
                    ReturnType = DataType.Numeric
                }
            }
        };

        public override Task<Capabilities> GetCapabilities(Empty request, ServerCallContext context)
        {
            if (Logger.IsTraceEnabled)
            {
                Logger.Trace("-- GetCapabilities --");

                TraceServerCallContext(context);
            }
            else
            {
                Logger.Debug("GetCapabilites called");
            }

            return Task.FromResult(ConnectorCapabilities);
        }

        public override async Task ExecuteFunction(IAsyncStreamReader<BundledRows> requestStream, IServerStreamWriter<BundledRows> responseStream, ServerCallContext context)
        {
            if (Logger.IsTraceEnabled)
            {
                Logger.Trace("-- ExecuteFunction --");

                TraceServerCallContext(context);
            }
            else
            {
                Logger.Debug("ExecuteFunction called");
            }

            var functionRequestHeaderStream = context.RequestHeaders.SingleOrDefault(header => header.Key == "qlik-functionrequestheader-bin");

            if (functionRequestHeaderStream == null)
            {
                throw new Exception("ExecuteFunction called without Function Request Header in Request Headers.");
            }

            var functionRequestHeader = new FunctionRequestHeader();
            functionRequestHeader.MergeFrom(new CodedInputStream(functionRequestHeaderStream.ValueBytes));

            Logger.Trace($"FunctionRequestHeader.FunctionId String : {(FunctionConstant)functionRequestHeader.FunctionId}");

            switch (functionRequestHeader.FunctionId)
            {
                case (int)FunctionConstant.Add42:
                    {
                        while (await requestStream.MoveNext())
                        {
                            var resultBundle = new BundledRows();

                            foreach (var row in requestStream.Current.Rows)
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

                        while (await requestStream.MoveNext())
                        {
                            foreach (var row in requestStream.Current.Rows)
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
                case (int)FunctionConstant.Concatenate:
                    {
                        var requestAsList = await requestStream.ToListAsync();

                        var concatenatedStrings = String.Join(", ",
                            requestAsList.SelectMany(bundledRows => bundledRows.Rows).SelectMany(row => row.Duals)
                                .Select(dual => dual.StrData));

                        var resultBundle = new BundledRows();
                        var resultRow = new Row();
                        resultRow.Duals.Add(new Dual { StrData = concatenatedStrings });
                        resultBundle.Rows.Add(resultRow);
                        await responseStream.WriteAsync(resultBundle);
                        break;
                    }
                case (int)FunctionConstant.CallCounter:
                    {
                        var currentIncrement = Interlocked.Increment(ref _callCounter);

                        while (await requestStream.MoveNext())
                        {
                            var resultBundle = new BundledRows();

                            foreach (var row in requestStream.Current.Rows)
                            {
                                var resultRow = new Row();
                                resultRow.Duals.Add(new Dual { NumData = currentIncrement });
                                resultBundle.Rows.Add(resultRow);
                            }
                            await responseStream.WriteAsync(resultBundle);
                        }
                        break;
                    }
                case (int)FunctionConstant.CallCounterNoCache:
                    {
                        context.ResponseTrailers.Add("qlik-cache", "no-store");
                        var currentIncrement = Interlocked.Increment(ref _callCounter);

                        while (await requestStream.MoveNext())
                        {
                            var resultBundle = new BundledRows();

                            foreach (var row in requestStream.Current.Rows)
                            {
                                var resultRow = new Row();
                                resultRow.Duals.Add(new Dual { NumData = currentIncrement });
                                resultBundle.Rows.Add(resultRow);
                            }
                            await responseStream.WriteAsync(resultBundle);
                        }
                        break;
                    }
                default:
                    break;
            }

            Logger.Trace("-- (ExecuteFunction) --");
        }

        private static long _callCounter = 0;

        private static void TraceServerCallContext(ServerCallContext context)
        {
            var authContext = context.AuthContext;

            Logger.Trace($"ServerCallContext.Method : {context.Method}");
            Logger.Trace($"ServerCallContext.Host : {context.Host}");
            Logger.Trace($"ServerCallContext.Peer : {context.Peer}");
            foreach (var contextRequestHeader in context.RequestHeaders)
            {
                Logger.Trace(
                    $"{contextRequestHeader.Key} : {(contextRequestHeader.IsBinary ? "<binary>" : contextRequestHeader.Value)}");

                if (contextRequestHeader.Key == "qlik-functionrequestheader-bin")
                {
                    var functionRequestHeader = new FunctionRequestHeader();
                    functionRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Logger.Trace($"FunctionRequestHeader.FunctionId : {functionRequestHeader.FunctionId}");
                    Logger.Trace($"FunctionRequestHeader.Version : {functionRequestHeader.Version}");
                }
                else if (contextRequestHeader.Key == "qlik-commonrequestheader-bin")
                {
                    var commonRequestHeader = new CommonRequestHeader();
                    commonRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Logger.Trace($"CommonRequestHeader.AppId : {commonRequestHeader.AppId}");
                    Logger.Trace($"CommonRequestHeader.Cardinality : {commonRequestHeader.Cardinality}");
                    Logger.Trace($"CommonRequestHeader.UserId : {commonRequestHeader.UserId}");
                }
                else if (contextRequestHeader.Key == "qlik-scriptrequestheader-bin")
                {
                    var scriptRequestHeader = new ScriptRequestHeader();
                    scriptRequestHeader.MergeFrom(new CodedInputStream(contextRequestHeader.ValueBytes));

                    Logger.Trace($"ScriptRequestHeader.FunctionType : {scriptRequestHeader.FunctionType}");
                    Logger.Trace($"ScriptRequestHeader.ReturnType : {scriptRequestHeader.ReturnType}");

                    int paramIdx = 0;

                    foreach (var parameter in scriptRequestHeader.Params)
                    {
                        Logger.Trace($"ScriptRequestHeader.Params[{paramIdx}].Name : {parameter.Name}");
                        Logger.Trace($"ScriptRequestHeader.Params[{paramIdx}].DataType : {parameter.DataType}");
                        ++paramIdx;
                    }
                    Logger.Trace($"CommonRequestHeader.Script : {scriptRequestHeader.Script}");
                }
            }

            Logger.Trace($"ServerCallContext.AuthContext.IsPeerAuthenticated : {authContext.IsPeerAuthenticated}");
            Logger.Trace(
                $"ServerCallContext.AuthContext.PeerIdentityPropertyName : {authContext.PeerIdentityPropertyName}");
            foreach (var authContextProperty in authContext.Properties)
            {
                var loggedValue = authContextProperty.Value;
                var firstLineLength = loggedValue.IndexOf('\n');

                if (firstLineLength > 0)
                {
                    loggedValue = loggedValue.Substring(0, firstLineLength) + "<truncated at linefeed>";
                }

                Logger.Trace($"{authContextProperty.Name} : {loggedValue}");
            }
        }
    }
}