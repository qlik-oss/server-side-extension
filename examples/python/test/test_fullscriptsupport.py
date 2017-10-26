"""
Basic testing of SSE functionality.
"""
import grpc
import test.ServerSideExtension_pb2 as SSE
from test.utils import duals_to_rows
from test.utils import numbers_to_duals
from test.utils import to_numeric_parameters


class TestFullScriptSupport:
    """
    Basic tests for the FullScriptSupport plugin.
    """

    def setUp(self):
        """
        Test setup.
        """
        self.channel = grpc.insecure_channel('localhost:50051')
        self.stub = SSE.ConnectorStub(self.channel)


    def test_getcapabilities(self):
        """
        Test GetCapabilities FullScriptSupport.

        This test calls the GetCapabilities function for the
        plugin and verifies some basic plugin properties.
        """
        capabilities = self.stub.GetCapabilities(SSE.Empty())

        assert capabilities.allowScript is True
        assert capabilities.pluginIdentifier == 'Full Script Support - Qlik'
        assert capabilities.pluginVersion == 'v1.0.0-beta1'

    def test_evaluatescript(self):
        """
        Test EvaluateScript FullScriptSupport.

        This test calls the EvaluateScript function with the
        script 'num1 + num2' and two numbers as parameters,
        effectively summarizing the two input numbers.
        """
        params = to_numeric_parameters('num1', 'num2')

        header = SSE.ScriptRequestHeader(script='args[0] + args[1]',
                                         functionType=SSE.TENSOR,
                                         returnType=SSE.NUMERIC,
                                         params=params)

        duals = numbers_to_duals(42, 42)
        rows = duals_to_rows(duals)

        # Trailing commas to make iterable sequences of tuples.
        bundled_rows = (SSE.BundledRows(rows=rows)),
        metadata = (('qlik-scriptrequestheader-bin', header.SerializeToString()), )

        result = self.stub.EvaluateScript(request_iterator=iter(bundled_rows), metadata=metadata)

        for bundled_row in result:
            for row in bundled_row.rows:
                for dual in row.duals:
                    assert dual.numData == 84
