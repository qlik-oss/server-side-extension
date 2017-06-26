"""
Utils for SSE tests.
"""
import test.ServerSideExtension_pb2 as SSE

def to_string_parameters(*args):
    """
    Creates a list of string Parameters.
    """
    params = [SSE.Parameter(dataType=SSE.STRING, name=s)
              for s in list(args)]

    return params

def to_numeric_parameters(*args):
    """
    Creates a list of numeric Parameters.
    """
    params = [SSE.Parameter(dataType=SSE.NUMERIC, name=s)
              for s in list(args)]

    return params

def strings_to_duals(*args):
    """
    Converts a number of strings to a list of Duals.
    """
    duals = [SSE.Dual(strData=s) for s in list(args)]

    return duals

def numbers_to_duals(*args):
    """
    Converts a number of numbers to a list of Duals.
    """
    duals = [SSE.Dual(numData=n) for n in list(args)]

    return duals

def duals_to_rows(*args):
    """
    Converts a number of duals to a list of rows.
    """
    rows = [SSE.Row(duals=d) for d in list(args)]

    return rows

