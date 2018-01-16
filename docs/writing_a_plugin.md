# Writing an SSE plugin

This section assumes you have read [Communication Flow](../docs/communication_flow.md) to get an understanding of the communication between Qlik (the client) and the SSE plugin (the server).  

## Content
* [Introduction](#introduction)
  * [Script evaluation](#script-evaluation)
  * [Function evaluation](#function-evaluation)
* [Implementation](#implementation)
  * [Protobuf generated files](#protobuf-generated-files)
  * [RPC methods](#rpc-methods)
    * [`GetCapabilities`](#getcapabilities)
    * [`EvaluateScript`](#evaluatescript)
    * [`ExecuteFunction`](#executefunction)
  * [Metadata sent from Qlik to the Plugin](#metadata-sent-from-qlik-to-the-plugin)
    * [`CommonRequestHeader`](#commonrequestheader)
    * [`FunctionRequestHeader`](#functionrequestheader)
    * [`ScriptRequestHeader`](#scriptrequestheader)
  * [Metadata sent from the plugin to Qlik](#metadata-sent-from-the-plugin-to-qlik)
    * [Cache control](#cache-control)
    * [`TableDescription`](#tabledescription)
* [Using SSE expressions in a Qlik app/document](#using-sse-expressions-in-a-qlik-appdocument)
  * [Sort order](#sort-order)

## Introduction
There are three possible RPC methods that Qlik can call.  

* `GetCapabilities`: called when Engine starts, returns the capabilities of the plugin.  
* `EvaluateScript`: called when a script function is used in the Qlik client.  
* `ExecuteFunction`: called when a plugin function is used in the Qlik client.  

But what is the difference between a _script function_ and a _plugin function_?  

For script functions you write any script directly in the Qlik client, and pass it as a parameter to the SSE plugin in one of the many pre-defined script functions. The plugin functions, on the other hand, are defined and implemented on the plugin side. The user calls the function directly by name and inserts the data as parameters in the Qlik client. The two options are described in detail below.

### Script evaluation
When script evaluation is enabled, by setting allowScript to true in `GetCapabilities`, eight script functions are automatically enabled in Qlik with pre-defined definitions. What should be covered in your plugin to fullfil the functionality is the implementation of the `EvaluateScript` RPC function, including fetching the parameter values, running the script and returning the result in the correct format.

The first four of the added functions all have the same data type for argument type and return type. Argument type refers to the data type of the parameters.

| __Function Name__ | __Function Type__ | __Argument Type__ | __Return Type__ |
| ----- | ----- | ----- | ----- |
| __ScriptEval__ | Scalar, Tensor | Numeric | Numeric |
| __ScriptEvalStr__ | Scalar, Tensor | String | String |
| __ScriptAggr__ | Aggregation | Numeric | Numeric |
| __ScriptAggrStr__ | Aggregation | String | String |

To call one of the functions above, write an expression (either in an object or in the load script) in the following form:  

`<EngineName>.<FunctionName>(<Script> [,<Parameter>...])`  

where:

* `<EngineName>` :  The mapping/alias to the plugin, as defined in *settings.ini* or in QMC
* `<FunctionName>` : The name of the function.
* `<Script>`: A string containing the script to be evaluated.
* `<Parameter>`: Additional parameters containing data from Qlik. Multiple parameters are comma separated.

The next four functions have `dual` as parameter type, that is, we can have arguments of different data types: `string`, `numeric`, or `dual`. However, the return type must be either `string` or `numeric`. Use cases for these functions could be text analysis and clustering, where number of clusters (numeric) is one parameter and the text data another (string).

| __Function Name__ | __Function Type__ | __Argument Type__ | __Return Type__ |
| ----- | ----- | ----- | ----- |
| __ScriptEvalEx__ | Scalar, Tensor | Dual | Numeric |
| __ScriptEvalExStr__ | Scalar, Tensor | Dual | String |
| __ScriptAggrEx__ | Aggregation | Dual | Numeric |
| __ScriptAggrExStr__ | Aggregation | Dual | String |

You call these functions in the same way as the preceding ones, except you must add a parameter before the script. The resulting expression has the form:

`<EngineName>.<FunctionName>(<ParameterDataTypes>, <Script> [,<Parameter>...])`  

where:

* `<ParameterDataTypes>` : A string containing the data types of the parameters, ordered according to the specified parameters. For example, 'NSD' means we have provided three parameters, the first one is numeric (N), the second one string (S), and the last one dual (D).  
__Note:__ There is a known issue, in the Sense June 2017 release, about error handling when _more_ parameters are sent than number of data types specified. Reproducing this issue requires restarting either the plugin or the Qlik engine, see the [Limitations](limitations.md) page for more information.

### Function evaluation
There are a number of reasons you might want to define your own functions instead of using the script functionality.
* It is easier to maintain control of what end users can do with user defined functions. Allowing users to run arbitrary scripts poses a security risk. By supporting only pre-defined functions, this threat is minimized.
* Some languages are not optimal to be used as script languages.
* You can avoid writing duplicate scripts in the Qlik expressions when you want to perform the same operations multiple times.
* It is easier to view and edit long scripts in separate files rather than in the expression editor or load script editor in Qlik.

To call a function from the expression editor or load script in Qlik, use the following form:

`<EngineName>.<FunctionName>([<Parameter>...])`  

where:

* `<EngineName>` : The mapping/alias to the plugin, as defined in *settings.ini* or in QMC
* `<FunctionName>`: The name of the function sent in `GetCapabilities`
* `<Parameter>`: The function parameter(s). Depending on the definition, there could be multiple parameters of different types. Multiple parameters are comma separated.

Remember that Qlik can receive multiple columns using the `Load ... Extension ...` statement in the Qlik load script, in all other cases the first column of data sent back from the plugin is used by Qlik; if there are more they are ignored. Read more about *function types* in the API reference, [Protocol Documentation](SSE_Protocol.md).

## Implementation
The implementation of the plugins are language-dependent. We have provided examples for some languages (see the */examples* folder for more information). However, there are some things to remember independently of the language you choose.

### Protobuf generated files
The interface between Qlik and your Server-side extension is defined in the file [ServerSideExtension.proto](../proto/ServerSideExtension.proto). For convenience, this file is used to generate the base communication source code for the language your plugin is written in and this is done by using gRPC Tools. The messages and methods in the proto file will be compiled to source code used by your plugin.

### RPC methods
The RPC methods available are `GetCapabilities`, `ExecuteFunction`, and `EvaluateScript`. The implementation of these methods differs depending on the supported functionality for your plugin. For instance, if you only support script evaluations you do not need to implement the `ExecuteFunction` method. However, the `GetCapabilities` method is mandatory, because that is where you define and send back the supported functionality to the client, i.e., the Qlik engine.

#### `GetCapabilities`
The `GetCapabilities` method includes the following properties you can use to define the capabilities of your plugin:  
* `allowScript`: a boolean value, set it to true for enabling/allowing script evaluation when `EvaluateScript` RPC method is called,  
* `functions`: repeated function definitions, to define what functions the `ExecuteFunction` RPC method implements.  
* `pluginIdentifier`: a string containing the ID or name of the plugin (only used to log the info in Qlik).  
* `pluginVersion`: a string containing the version of the plugin (only used to log the info in Qlik).  

#### `EvaluateScript`
The definition of each script function is added on the engine side, and your implementation of `EvaluateScript` must match these definitions. You can limit the support for different data types or function types by raising an error if the a particular type is encountered. The Python examples contain sample code for both error handling and full script support.

When a script function is called from the UI, the engine calls the `EvaluateScript` RPC method in your plugin. It is up to you as a developer to decide how to implement the support for the different script functions. One option is to have a specific implementation for each script function and then map the call from `EvaluteScript` to the appropriate one. Because there is a lot of repetitive code in this case, in the Python examples we have chosen to have the same methods for all script functions that handle all supported data types and function types.

#### `ExecuteFunction`
When the client (e.g., Qlik Sense) calls the RPC method `GetCapabilities`, the plugin must return the function definitions. This lets the Qlik engine know about the function capabilities for this specific plugin. Each function definition includes its **function name**, the **number of parameters** and their **types**, the **return type**, as well as **function type**. The function name is restricted to include alpha-numeric and underscore characters only.

When a function call is made from the UI, the engine calls the `ExecuteFunction` RPC method in your plugin. This method must delegate the call to the correct implementation of your functions. In the Python examples, we use the `getattr()` method and a dictionary that maps the function ID (which is sent in the request header) to the implemented function name. For more information, see [Writing an SSE plugin using Python](../examples/python/README.md).

### Metadata sent from Qlik to the plugin
There are a couple of headers sent from Qlik to the SSE plugin, containing metadata about the request.

#### `CommonRequestHeader`
This header is always sent from Qlik to the plugin, at the beginning of both an _EvaluateScript_ and an _ExecuteFunction_ request. The header contains information about what __app__ and __user__ the request originates from as well as the __cardinality__ of the parameters (number of rows). See more details in the [CommonRequestHeader](SSE_Protocol.md#qlik.see.CommonRequestHeader) section in the protocol documentation.

#### `FunctionRequestHeader`
The header is sent from Qlik to the plugin in the beginning of an _ExecuteFunction_ request containing the __function id__ of the SSE call. This information is particularly important when implementing the `ExecuteFunction` method for passing along the request to the correct method. See more details in the [FunctionRequestHeader](SSE_Protocol.md#qlik.see.FunctionRequestHeader) section in the protocol documentation.

#### `ScriptRequestHeader`
When a script function is called from Qlik, this header is sent in the beginning of the request, to the plugin. The header contains the __script__ as well as the __parameter names and types__. Script functions does not have a function id as the plugin defined functions, but has a __function type__ and a __return type__ which separates them apart, which are both included in the header.  See more details in the [ScriptRequestHeader](SSE_Protocol.md#qlik.see.ScriptRequestHeader) section in the protocol documentation.

### Metadata sent from the plugin to Qlik
The SSE plugin can also send metadata back to Qlik containing information about the result.

#### Cache control
The default behavior of the Qlik engine is to cache results from computations in order to reduce the workload and speed up response time. This is valid as well for computations performed by SSE plugins. For some cases caching might be undesirable and it is possible to toggle it off per request. When `EvaluateScript` or `ExecuteFunction` are called the http header key:value pair `qlik-cache:no-store` can be set in either the initial or trailing header data of the response. This functionality is demonstrated in the [Hello world example](../examples/python/HelloWorld/README.md#nocache-function).

Caching is automatically turned off in Qlik if a request fails due to the communication failure or an exception is raised during a call.

#### `TableDescription`
This header must be sent as initial metadata, that is _before_ the plugin starts sending back the response data to Qlik. The information is only handled by Qlik within a data load, more precisely, in a `Load ... Extension ...` statement. By sending the header, you can set metadata for the returned table such as __names__, __data types__, __tags__ etc.

There are a few things to keep in mind when sending a `TableDescription`:
* The number of `FieldDescriptions` in the message must match the number of returned fields to Qlik. Otherwise an error is raised.
* If a message field is not set, the default value according to protobuf will be used.
  * For example, if the datatype (int) is not set for a field (FieldDescription), the default value is 0, and the values of the returned field will be parsed in Qlik as strings.
  * An exception is the _field name_, which will be set by default to _Field{i}_ for the _i:th_ field returned to Qlik if no other name was set.

See more details in the [TableDescription](SSE_Protocol.md#qlik.see.TableDescription) section in the protocol documentation and in the example [FullScriptSupport using Pandas](../examples/python/FullScriptSupport_Pandas/README.md) written in Python where the header can be modified directly from the script written in Qlik.

## Using SSE expressions in a Qlik app/document

It is possible to make an SSE call from both the load script and from a chart expression.  

You can return a table from the SSE plugin using the `Load ... Extension ...` statement in the Qlik load script. Read more about the syntax for table load using SSE in the [Qlik Sense help](http://help.qlik.com/en-US/sense/February2018/Subsystems/Hub/Content/Scripting/ScriptRegularStatements/Load.htm) .  

Note that calls to SSE functions from within any other load statements, e.g. `Load ... From ...`, only support scalar and aggregation function types. This implies that data containing n rows, will result in n calls to the plugin unless you are aggregating the data resulting in a single call.

You will find sample code demonstrating calling an SSE plugin from the Qlik load script in all Python examples.

### Sort Order
Note that the sort order in the visualizations is not necessarily the same as the order sent to the plugin. It is up to you as a developer to preserve the order when you return the values from the plugin to the Qlik client.
