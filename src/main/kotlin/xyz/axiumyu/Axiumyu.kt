package xyz.axiumyu

import org.bukkit.plugin.java.JavaPlugin
import xyz.axiumyu.commanddsl.PermissionRegistry
import xyz.axiumyu.dialogdsl.RouteCleanUp

class Axiumyu : JavaPlugin() {

    override fun onEnable() {
        server.pluginManager.registerEvents(RouteCleanUp, this)
        PermissionRegistry.registerAll()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
