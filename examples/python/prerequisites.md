# Prerequisites for running the Python examples
To run the Python SSE plugin examples, you need __Python__ version 3.4 or higher along with a few _python libraries_.

Anaconda is a Python distribution pre-bundled with multiple extra libraries. An installer can be downloaded from the [Anaconda webpage](https://www.continuum.io/downloads). For a leaner installation, the default installer can be found on the webpage of the [Python Software Foundation](https://www.python.org/downloads/).

The following _python libraries_ are needed for the specified SSE plugins:

| __Name__ | __SSE plugin(s)__ |
| ----- | ----- |
|  __grpcio__ |all examples |
| __numpy__ | _FullScriptSupport_ and _FullScriptSupport_Pandas_ |
| __pandas__ | _FullScriptSupport_Pandas_ |

The simplest way to acquire the libraries is to use the Python package manager `pip`. Open up a command prompt, navigate to the `examples\python\` folder, and then run the command:

 `python -m pip install -r requirements.txt`.

  If Python is correctly set up on the machines all the dependencies should get installed.

__Note__
--------
Dependencies and recommended versions are specified in `requirements.txt`. If you install the libraries manually, make sure to take a _grpcio_ version that is compatible as there might be breaking changes between versions. If you need to have a different version installed for another Python project, consider using [virtualenv](https://virtualenv.pypa.io/en/stable/).
