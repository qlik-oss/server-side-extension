# Prerequisites for running the Python examples
If you are able to run the Python examples for gRPC ([http://www.grpc.io/docs/](http://www.grpc.io/docs/)), you have all you need to get started.

Specifically, to run the Python SSE plugin examples, you need the following:

* __pip__, version 8 (or higher). pip is already installed if you're using python 2.0 >= 2.7 or 3 >= 3.4 binaries from python.org, but you will have to upgrade pip. You can update your version of pip by running `$ python -m pip install --upgrade pip`. You can find more information at [https://pip.pypa.io/en/stable/installing/](https://pip.pypa.io/en/stable/installing/). pip is also included in the Anaconda distribution, below.
* __Python__, version 3.4 (or higher). You can find links for installing Anaconda (a Python distribution including common libraries and packages) on the Anaconda webpage: [https://www.continuum.io/downloads](https://www.continuum.io/downloads). For Python without any libraries, see [https://www.python.org/downloads/](https://www.python.org/downloads/).
* __grpcio__: Install the package using pip: `$ python -m pip install grpcio`
* __numpy__: (_FullScriptSupport_ only) Included in most python distributions, including Anaconda. If needed, install using pip: `$ python -m pip install numpy`.
