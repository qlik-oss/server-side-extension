"""
Basic testing of SSE functionality.
"""
import grpc
import test.ServerSideExtension_pb2 as SSE
from test.utils import strings_to_duals
from test.utils import duals_to_rows
from test.utils import to_string_parameters

HELLO_WORLD_ID = 0


class TestHelloWorld:
    """
    Basic tests for the Hello World plugin.
    """

    def setUp(self):
        """
        Test setup.
        """
        self.channel = grpc.insecure_channel('localhost:50052')
        self.stub = SSE.ConnectorStub(self.channel)


    def test_getcapabilities(self):
        """
        Test GetCapabilities HelloWorld.

        This test calls the GetCapabilities function for the
        plugin and verifies some basic plugin properties.
        """
        capabilities = self.stub.GetCapabilities(SSE.Empty())

        assert capabilities.allowScript is True
        assert capabilities.pluginIdentifier == 'Hello World - Qlik'
        assert capabilities.pluginVersion == 'v1.0.0-beta1'


    def test_executefunction(self):
        """
        Test ExecuteFunction HelloWorld.

        This tests calls the ExecuteFunction function, with an
        id corresponding to the HelloWorld plugin function.
        """

        header = SSE.FunctionRequestHeader(functionId=HELLO_WORLD_ID, version="1")

        duals = strings_to_duals('Hello World!')
        rows = duals_to_rows(duals)

        # Trailing commas to make iterable sequences of tuples.
        bundled_rows = (SSE.BundledRows(rows=rows)),
        metadata = (('qlik-functionrequestheader-bin', header.SerializeToString()),)

        result = self.stub.ExecuteFunction(request_iterator=iter(bundled_rows), metadata=metadata)

        for bundled_row in result:
            for row in bundled_row.rows:
                for dual in row.duals:
                    assert dual.strData == 'Hello World!'

    def test_evaluatescript(self):
        """
        Test EvaluateScript HelloWorld.

        This test calls the EvaluateScript function with the
        script 'str1 + str2' and two strings as parameters,
        effectively concatenating the two input strings.
        """
        params = to_string_parameters('str1', 'str2')

        header = SSE.ScriptRequestHeader(script='args[0] + args[1]',
                                         functionType=SSE.TENSOR,
                                         returnType=SSE.STRING,
                                         params=params)

        duals = strings_to_duals('Hello', 'World')
        rows = duals_to_rows(duals)

        # Trailing commas to make iterable sequences of tuples.
        bundled_rows = (SSE.BundledRows(rows=rows)),
        metadata = (('qlik-scriptrequestheader-bin', header.SerializeToString()), )

        result = self.stub.EvaluateScript(request_iterator=iter(bundled_rows), metadata=metadata)

        for bundled_row in result:
            for row in bundled_row.rows:
                for dual in row.duals:
                    assert dual.strData == 'HelloWorld'
