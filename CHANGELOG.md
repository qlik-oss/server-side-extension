# Previous versions
The table maps all released SSE versions with the appropriate Qlik versions. See the Changelog below for more details of each release.

| __SSE Version__ | __Qlik Sense Version__ | __QlikView Version__ |
| ----- | ----- | ----- |
| [v1.1.0](https://github.com/qlik-oss/server-side-extension/releases/tag/v1.1.0) | Qlik Sense February 2018 (or later) | No supported version |
| [v1.0.0](https://github.com/qlik-oss/server-side-extension/releases/tag/v1.0.0) | Qlik Sense June 2017 (or later) | QlikView November 2017 (or later) |

# Changelog

## v1.1.0
Release date: 2017-01-16

v1.1.0 is _backwards compatible_ with version v1.0.0, meaning a client and server can have different SSE versions and still use all functionality supported in v1.0.0.

Qlikview does not support the functionality of table load using SSE (`Load ... Extension ...`, see the [Qlik Sense Help](http://help.qlik.com/en-US/sense/February2018/Subsystems/Hub/Content/Scripting/ScriptRegularStatements/Load.htm)) and there is not yet a plan to support it. But because of the backwards compatability, Qlikview can connect to SSEs of version v1.1.0 without changing anything in the QlikView deployment, given that the new features in v1.1.0 are not used.

### New Features:  
[#22](https://github.com/qlik-oss/server-side-extension/pull/22) : `TableDescription` (and `FieldDescription`), send metadata of the table and fields returned to Qlik during data load. See [documentation](docs/writing_a_plugin.md#tabledescription). Both script examples written in python are updated with possibility to return multiple columns.

### Bugfixes:  
[#23](https://github.com/qlik-oss/server-side-extension/issues/23) : Docker files had wrong Python version specified

### Notable Changes:
[#17](https://github.com/qlik-oss/server-side-extension/pull/17) : Run Python examples in docker  
[#22](https://github.com/qlik-oss/server-side-extension/pull/22) : Python script example added, now using _pandas_ and _exec_ libraries. See commit [here](https://github.com/qlik-oss/server-side-extension/commit/f27f0e33270f6d9ec96cf11c7530e6b281c83306)  
[#22](https://github.com/qlik-oss/server-side-extension/pull/22) : C# example added. See commit [here](https://github.com/qlik-oss/server-side-extension/commit/cf1b8d28a1431fd192587aac13ebe73dad388c4c)  
[#13](https://github.com/qlik-oss/server-side-extension/pull/13) : Java example added  
[#14](https://github.com/qlik-oss/server-side-extension/pull/14) : Go example added  
[#18](https://github.com/qlik-oss/server-side-extension/pull/18) : Documentation and example for QlikView added  


## v1.0.0
Release date: 2017-06-26

Initial release.
