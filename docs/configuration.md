# Configuring SSE plugins in Qlik

## Qlik Sense
You must configure SSE plugins, referred to by Qlik as analytic connections, in the Qlik Management Console (QMC), in the settings.ini file or using the Qlik Repository Service API.

For an overview of analytic connections, see [Analytic connections](https://help.qlik.com/en-US/sense-developer/February2018/Content/AnalyticConnections.htm) in the Qlik Sense Developers help.

For instructions of adding a new SSE plugin in QMC, see [Creating an analytic connection](https://help.qlik.com/en-US/sense/February2018/Subsystems/ManagementConsole/Content/create-analytic-connection.htm) in the Qlik Sense help.

For information about SSE plugins for Qlik Sense Desktop, see [Configuring analytic connections in Qlik Sense Desktop](https://help.qlik.com/en-US/sense/February2018/Subsystems/Hub/Content/Introduction/configure-analytic-connection-desktop.htm) in the Qlik Sense help.


## QlikView
You must configure SSE plugins, referred to by Qlik as analytic connections, by editing the settings.ini file (either for QlikView Server or QlikView Desktop).

For an overview of analytic connections, including how to add a new SSE plugin, see [Analytic connections](https://help.qlik.com/en-US/qlikview/November2017/Subsystems/Client/Content/Analytic_connections.htm) in the QlikView help.

For step-by-step instructions on how to get started with SSE in QlikView, see [Get started with analytic connections](https://help.qlik.com/en-US/qlikview/November2017/Subsystems/Client/Content/Getting-started-with-analytic-connections.htm) in the QlikView help.

## Changed configuration
If you add, remove or change the configuration of a plugin you might need to restart Qlik engine depending on your environment. See [Limitations](limitations.md) for further details.
