package xyz.axiumyu.dialogdsl.route

import org.bukkit.entity.Player
import xyz.axiumyu.dialogdsl.dialog.build
import java.util.UUID

/**
 * 每个页面被渲染时，都会接收到这个上下文对象。
 * 它负责持有当前玩家、传递的参数，并提供跳转能力。
 */
interface DialogRouteContext {
    val player: Player

    fun navigate(route: RouteBase)
    fun replace(route: RouteBase)
    fun goBack()
    fun updateBack(route: RouteBase)
    fun abortTransaction()
}

object DialogRouter {
    // 路由栈状态 (绝对私有)
    private val historyStacks = mutableMapOf<UUID, ArrayDeque<RouteBase>>()

    /**
     * 打开一个全新的根菜单 (通常绑定在指令或 NPC 交互上)
     */
    fun openRoot(player: Player, route: RootRoute) {
        clearHistory(player)
        navigate(player, route)
    }

    /**
     * 供生命周期监听器 (死亡/掉线) 和底层 Button 使用
     */
    fun clearHistory(player: Player) {
        historyStacks.remove(player.uniqueId)
    }

    /**
     * 获取玩家当前打开的Dialog，以及判断生命周期监听器判断当前是否处于高危事务中
     */
    fun getCurrentRoute(player: Player): RouteBase? {
        return historyStacks[player.uniqueId]?.lastOrNull()
    }

    private fun navigate(player: Player, route: RouteBase) {
        val stack = historyStacks.getOrPut(player.uniqueId) { ArrayDeque() }
        stack.addLast(route)
        render(player, route)
    }

    private fun replace(player: Player, route: RouteBase) {
        val stack = historyStacks[player.uniqueId] ?: return
        if (stack.isNotEmpty()) stack.removeLast() // 弹出现有路由
        stack.addLast(route) // 压入新状态路由
        render(player, route)
    }

    private fun goBack(player: Player) {
        val stack = historyStacks[player.uniqueId] ?: return
        if (stack.size > 1) {
            stack.removeLast() // 弹出当前页
            render(player, stack.last()) // 拿着上一页的数据重新渲染
        } else {
            clearHistory(player)
            player.closeDialog()
        }
    }

    private fun updateBack(player: Player, route: RouteBase) {
        val stack = historyStacks[player.uniqueId] ?: return
        if (stack.size > 1) {
            stack.removeLast() // 弹出当前页
            stack.removeLast() // 弹出待更新的上一页
            stack.addLast(route) // 压入更新后的上一页
            render(player, route)
        } else {
            clearHistory(player) // 无上一页：关闭并清空
            player.closeDialog()
        }
    }

    private fun render(player: Player, route: RouteBase) {

        // 生成当前页面专属的上下文实体 (实现了刚才定义的接口)
        val context = object : DialogRouteContext {
            override val player = player

            // 内部直接代理到引擎的私有方法
            override fun navigate(route: RouteBase) = navigate(player, route)

            override fun replace(route: RouteBase) = replace(player, route)

            override fun goBack() = goBack(player)

            override fun updateBack(route: RouteBase) = updateBack(player, route)

            override fun abortTransaction() {
                if (route is AtomicRoute) route.onRollback(player)
                clearHistory(player)
                player.closeInventory()
            }
        }

        // 把这个受严格保护的 context 喂给业务代码
        val dialogSetup = route.render(context)
        player.showDialog(dialogSetup.build())
    }
}
