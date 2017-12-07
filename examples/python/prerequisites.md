# Prerequisites for running the Python examples
To run the Python SSE plugin examples, you need __Python__ version 3.4 or higher. You can find links for installing Anaconda (a Python distribution including common libraries and packages) on the [Anaconda webpage](https://www.continuum.io/downloads). For Python without any libraries, see the [Python webpage](https://www.python.org/downloads/).

The following _python libraries_ are needed for the specified SSE plugins.

| __Name__ | __SSE plugin(s)__ | __Comments__ |  
| ----- | ----- | ----- |
|  __grpcio__ |all examples | Install the package using pip: `$ python -m pip install grpcio` |
| __numpy__ |_FullScriptSupport_ and _FullScriptSupport_Pandas_ | Included in most python distributions, including Anaconda. If needed, install using pip: `$ python -m pip install numpy`. |
| __pandas__ |_FullScriptSupport_Pandas_ |  Included in most python distributions, including Anaconda. If needed, install using pip: `$ python -m pip install pandas`. |
