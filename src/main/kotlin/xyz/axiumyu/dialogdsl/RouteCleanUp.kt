package xyz.axiumyu.dialogdsl

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.axiumyu.dialogdsl.route.AtomicRoute
import xyz.axiumyu.dialogdsl.route.DialogRouter

object RouteCleanUp : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        handleExit(event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: EntityDeathEvent) {
        handleExit(event.entity as? Player ?: return)
    }

    private fun handleExit(player: Player) {
        val currentRoute = DialogRouter.getCurrentRoute(player)

        // 只有玩家正处于高危原子事务中时，才触发回滚
        if (currentRoute is AtomicRoute) {
            currentRoute.onRollback(player)
        }

        // 无论如何，清空内存栈，防止泄露
        DialogRouter.clearHistory(player)
    }
}
