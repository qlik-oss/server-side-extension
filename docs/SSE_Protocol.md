# Protocol Documentation
<a name="top"/>

## Table of Contents
* [ServerSideExtension.proto](#ServerSideExtension.proto)
 * [BundledRows](#qlik.sse.BundledRows)
 * [Capabilities](#qlik.sse.Capabilities)
 * [CommonRequestHeader](#qlik.sse.CommonRequestHeader)
 * [Dual](#qlik.sse.Dual)
 * [Empty](#qlik.sse.Empty)
 * [FieldDescription](#qlik.sse.FieldDescription)
 * [FunctionDefinition](#qlik.sse.FunctionDefinition)
 * [FunctionRequestHeader](#qlik.sse.FunctionRequestHeader)
 * [Parameter](#qlik.sse.Parameter)
 * [Row](#qlik.sse.Row)
 * [ScriptRequestHeader](#qlik.sse.ScriptRequestHeader)
 * [TableDescription](#qlik.sse.TableDescription)
 * [DataType](#qlik.sse.DataType)
 * [FunctionType](#qlik.sse.FunctionType)
 * [Connector](#qlik.sse.Connector)
* [Scalar Value Types](#scalar-value-types)

<a name="ServerSideExtension.proto"/>
<p align="right"><a href="#top">Top</a></p>

## ServerSideExtension.proto



<a name="qlik.sse.BundledRows"/>

### BundledRows
A number of rows collected in one message. The actual number will depend on the size of each row and is adjusted to optimize throughput.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| rows | [Row](#qlik.sse.Row) | repeated |  |


<a name="qlik.sse.Capabilities"/>

### Capabilities
A full description of the plugin, sent to the Qlik engine, listing all functions available and indicating whether script evaluation is allowed.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| allowScript | [bool](#bool) | optional | When true, the Qlik engine allows scripts to be sent to the plugin. |
| functions | [FunctionDefinition](#qlik.sse.FunctionDefinition) | repeated | The definitions of all available functions. |
| pluginIdentifier | [string](#string) | optional | The ID or name of the plugin. |
| pluginVersion | [string](#string) | optional | The version of the plugin. |


<a name="qlik.sse.CommonRequestHeader"/>

### CommonRequestHeader
A header sent at the start of both an EvaluateScript request and an ExecuteFunction request under the key "qlik-commonrequestheader-bin".

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| appId | [string](#string) | optional | The ID of the app the request was executed in. |
| userId | [string](#string) | optional | The ID of the user the request was executed by. |
| cardinality | [int64](#int64) | optional | The cardinality of the parameters. |


<a name="qlik.sse.Dual"/>

### Dual
The basic data type for the data stream. Can contain double, string, or both.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| numData | [double](#double) | optional | Numeric value as double. |
| strData | [string](#string) | optional | String. |


<a name="qlik.sse.Empty"/>

### Empty
An empty message used when nothing is to be passed in a call.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |


<a name="qlik.sse.FieldDescription"/>

### FieldDescription
Field definition for function and script calls.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| dataType | [DataType](#qlik.sse.DataType) | optional | The data type of the field. |
| name | [string](#string) | optional | The name of the field. |
| tags | [string](#string) | repeated | The tags of the field. |


<a name="qlik.sse.FunctionDefinition"/>

### FunctionDefinition
The definition of a function, which informs the Qlik engine how to use it.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional | The name of the function. |
| functionType | [FunctionType](#qlik.sse.FunctionType) | optional | The type of the function. |
| returnType | [DataType](#qlik.sse.DataType) | optional | The return type of the function. |
| params | [Parameter](#qlik.sse.Parameter) | repeated | The parameters the function takes. |
| functionId | [int32](#int32) | optional | A unique ID number for the function, set by the plugin, to be used in calls from the Qlik engine to the plugin. |


<a name="qlik.sse.FunctionRequestHeader"/>

### FunctionRequestHeader
A header sent at the start of an ExecuteFunction request under the key "qlik-functionrequestheader-bin".

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| functionId | [int32](#int32) | optional | The ID of the function to be executed. |
| version | [string](#string) | optional | A dummy variable as a workaround for an issue. |


<a name="qlik.sse.Parameter"/>

### Parameter
Parameter definition for functions and script calls.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| dataType | [DataType](#qlik.sse.DataType) | optional | The data type of the parameter. |
| name | [string](#string) | optional | The name of the parameter. |


<a name="qlik.sse.Row"/>

### Row
A row of duals.

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| duals | [Dual](#qlik.sse.Dual) | repeated | Row of duals. |


<a name="qlik.sse.ScriptRequestHeader"/>

### ScriptRequestHeader
A header sent at the start of an EvaluateScript request under the key "qlik-scriptrequestheader-bin".

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| script | [string](#string) | optional | The script to be executed. |
| functionType | [FunctionType](#qlik.sse.FunctionType) | optional | The function type of the script evaluation: scalar, aggregation or tensor. |
| returnType | [DataType](#qlik.sse.DataType) | optional | The return type from the script evaluation: numeric, string or both. |
| params | [Parameter](#qlik.sse.Parameter) | repeated | The parameters names and types passed to the script. |


<a name="qlik.sse.TableDescription"/>

### TableDescription
A header sent before returning data to Qlik, under the key "qlik-tabledescription-bin".

| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| fields | [FieldDescription](#qlik.sse.FieldDescription) | repeated | The fields of the table. |
| name | [string](#string) | optional | The name of the table. |
| numberOfRows | [int64](#int64) | optional | Number of rows in table. |



<a name="qlik.sse.DataType"/>

### DataType
Data types of the parameters and return values.

| Name | Number | Description |
| ---- | ------ | ----------- |
| STRING | 0 | Contains only string. |
| NUMERIC | 1 | Contains only double. |
| DUAL | 2 | Contains both a string and a double. |

<a name="qlik.sse.FunctionType"/>

### FunctionType
Types of functions (determined by their return values).

| Name | Number | Description |
| ---- | ------ | ----------- |
| SCALAR | 0 | The return value is a scalar per row. |
| AGGREGATION | 1 | All rows are aggregated into a single scalar. |
| TENSOR | 2 | Multiple rows in, multiple rows out. |



<a name="qlik.sse.Connector"/>

### Connector
The communication service provided between the Qlik engine and the plugin.

| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| GetCapabilities | [Empty](#qlik.sse.Empty) | [Capabilities](#qlik.sse.Capabilities) | A handshake call for the Qlik engine to retrieve the capability of the plugin. |
| ExecuteFunction | [BundledRows](#qlik.sse.BundledRows) | [BundledRows](#qlik.sse.BundledRows) | Requests a function to be executed as specified in the header. |
| EvaluateScript | [BundledRows](#qlik.sse.BundledRows) | [BundledRows](#qlik.sse.BundledRows) | Requests a script to be evaluated as specified in the header. |



<a name="scalar-value-types"/>

## Scalar Value Types

| .proto Type | Notes | C++ Type | Java Type | Python Type |
| ----------- | ----- | -------- | --------- | ----------- |
| <a name="double"/> double |  | double | double | float |
| <a name="float"/> float |  | float | float | float |
| <a name="int32"/> int32 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead. | int32 | int | int |
| <a name="int64"/> int64 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead. | int64 | long | int/long |
| <a name="uint32"/> uint32 | Uses variable-length encoding. | uint32 | int | int/long |
| <a name="uint64"/> uint64 | Uses variable-length encoding. | uint64 | long | int/long |
| <a name="sint32"/> sint32 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s. | int32 | int | int |
| <a name="sint64"/> sint64 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s. | int64 | long | int/long |
| <a name="fixed32"/> fixed32 | Always four bytes. More efficient than uint32 if values are often greater than 2^28. | uint32 | int | int |
| <a name="fixed64"/> fixed64 | Always eight bytes. More efficient than uint64 if values are often greater than 2^56. | uint64 | long | int/long |
| <a name="sfixed32"/> sfixed32 | Always four bytes. | int32 | int | int |
| <a name="sfixed64"/> sfixed64 | Always eight bytes. | int64 | long | int/long |
| <a name="bool"/> bool |  | bool | boolean | boolean |
| <a name="string"/> string | A string must always contain UTF-8 encoded or 7-bit ASCII text. | string | String | str/unicode |
| <a name="bytes"/> bytes | May contain any arbitrary sequence of bytes. | string | ByteString | str |
