# A basic example

## Prerequisits
If you've not already read [Writing an SSE Plugin using C++](../README.md) and followed the installation guide for [gRPC](http://www.grpc.io/docs/quickstart/cpp.html) do so before continuing.

## Running the example

If the example was compiled successfully there should be an executable file in the same folder as the source code. The program has one optional command line argument `--pem_dir <dir/of/pem/and/key/>` which should be used to run the plugin in secure mode.

To get Qlik to connect to the plugin see [Configuring SSE plugins in Qlik](../../../docs/configuration.md).

## Implemented functionality

### Three functions added

In total there are three functions that have been added through this plugin, one of each function type scalar, aggregation and tensor.

* __SumOfRows__: A scalar function applied on two numeric fields that adds them up row wise.
* __SumOfColumn__: An aggregation function that takes a single numeric field and adds it up column wise.
* __TwoNormRanking__: A tensor function that takes two numeric fields as coordinates and computes their two-norm from the origin and returns ranking according to the distance.

### Script evaluation

The plugin has no script evaluation implemented. While C++ itself does not serve well for scripting purposes there is nothing stopping some scripting language to be added to a C++ plugin, R for instance.

### GetCapabilities

A description is constructed for each one of the added functions. The functions are given names, function types and data return type. Furthermore the parameters that a given function takes in are described as well with name and data type. This is the information the Qlik engine receives when connecting to the plugin.

If the plugin offered script execution as well then `AllowScript` would need to be set to `True` in the capabilities but `False` is the default value and nothing needs to be done for this plugin.

## Qlik Sense app

Along with the example there is a Qlik sense app that uses the plugin. If you add the plugin to Sense under the name `SSECpp` and then open the app you can see how the added functions can be used.
