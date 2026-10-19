@file:Suppress("UnstableApiUsage", "Unused")
package xyz.axiumyu

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext

internal class AxiumyuBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {}

    override fun createPlugin(context: PluginProviderContext) = Axiumyu
}
