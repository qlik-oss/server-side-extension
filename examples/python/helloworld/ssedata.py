from enum import Enum


class ArgType(Enum):
    """
    Represents data types that can be used
    as arguments in different script functions.
    """
    Undefined = -1
    Empty = 0
    String = 1
    Numeric = 2
    Mixed = 3


class ReturnType(Enum):
    """
    Represents return types that can
    be used in script evaluation.
    """
    Undefined = -1
    String = 0
    Numeric = 1
    Dual = 2


class FunctionType(Enum):
    """
    Represents function types.
    """
    Scalar = 0
    Aggregation = 1
    Tensor = 2
