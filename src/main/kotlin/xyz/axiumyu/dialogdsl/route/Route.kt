package xyz.axiumyu.dialogdsl.route

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.axiumyu.d
import xyz.axiumyu.dialogdsl.dialog.AtomicDialog
import xyz.axiumyu.dialogdsl.dialog.BaseDialog
import xyz.axiumyu.dialogdsl.dialog.RootDialogSetup
import xyz.axiumyu.dialogdsl.dialog.dsl.UIType

sealed interface RouteBase {
    fun render(context: DialogRouteContext): BaseDialog

}

/**
 * 简单无外部状态页面，允许后退回这类页面，大多数页面应实现这个接口
 * 默认应使用 `DialogSetup` 创建对话框
 */
interface Route : RouteBase

/**
 * 根节点 / 聚合页 (通常是一个模块的主菜单)应实现这个接口
 * 可直接使用 `AutoRootSetup` 方法零样本代码快速创建对话框
 */
interface RootRoute : RouteBase

/**
 * 涉及外部状态和副作用的原子事务路由 (高危操作)
 * - 默认且绝对禁止使用 ESC 关闭菜单。
 * - 如果需要增加退出按钮，必须开发者手动处理下一步或回滚操作。
 * - 适用场景：涉及经济扣费、数据库写入、多步且不可中断的复杂交互。
 * 需要在 `onRollback`中处理因各种意外关闭对话框的情况，如果在此添加了ExitButton，按钮将自动使用该回调
 * 应使用 `AtomicDialogSetup` 创建对话框
 */
interface AtomicRoute : RouteBase{

    /**
     * 事务异常中断钩子
     * 当玩家在此页面发生断线、死亡等意外，或业务逻辑主动调用 rollback 时触发。
     * @param player 发生中断的玩家
     */
    fun onRollback(player: Player)

    override fun render(context: DialogRouteContext): AtomicDialog
}

/**
 * 零样板代码的自动菜单路由。
 * 开发者只需继承它并提供子路由列表，框架将自动生成完整 UI 与退出按钮。
 */
inline fun AutoRootSetup(
    context: DialogRouteContext,
    title: Component,
    crossinline block: AutoMenuScope.() -> Unit
): BaseDialog {
    // 收集路由配置
    val menuScope = AutoMenuScope().apply(block)

    // 直接返回一个打包好的 RootDialogSetup 图纸
    return RootDialogSetup {
        DialogContent(title) {
            Text(d("请选择以下操作："))
        }

        // 自动计算排版
        val cols = if (menuScope.menuItems.size > 4) 2 else 1

        DialogType(UIType.MULTI_ACTION, columns = cols) {
            menuScope.menuItems.forEach { (name, targetRoute) ->
                Button(name, d("点击进入"), 100) { _, _ ->
                    context.navigate(targetRoute)
                }
            }
        }
    }
}