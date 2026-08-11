package xyz.axiumyu.dialogdsl.dialog.dsl

import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import xyz.axiumyu.d
import xyz.axiumyu.dialogdsl.route.AtomicRoute
import xyz.axiumyu.dialogdsl.route.DialogRouteContext
import xyz.axiumyu.dialogdsl.route.DialogRouter
import xyz.axiumyu.dialogdsl.route.RouteBase
import java.time.Duration

enum class UIType {
    NOTICE, CONFIRMATION, MULTI_ACTION, DIALOG_LIST, SERVER_LINKS
}

@PaperDialogDsl
@SuppressWarnings("UnstableApiUsage")
class DialogTypeScope {
    val buttons = mutableListOf<ActionButton>()
    var exitButton: ActionButton? = null

    // 常规按钮
    fun Button(
        label: Component, hover: Component, width: Int,
        maxUses: Int = 1, keepTime: Duration = ClickCallback.DEFAULT_LIFETIME,
        action: (view: DialogResponseView, audience: Audience) -> Unit
    ) {
        buttons.add(
            ActionButton.create(
                label, hover, width, DialogAction.customClick(
                    action, ClickCallback.Options.builder().lifetime(keepTime).uses(maxUses).build()
                )
            )
        )
    }

    // 无回调的按钮
    fun Button(label: Component, hover: Component, width: Int, action: DialogAction? = null) {
        buttons.add(ActionButton.create(label, hover, width, action))
    }

    // 导航按钮
    fun NavButton(context: DialogRouteContext, label: Component, hover: Component, width: Int, to: RouteBase) {
        Button(label, hover, width) { _, _ ->
            context.navigate(to)
        }
    }

    fun BackButton(
        context: DialogRouteContext,
        label: Component = d("返回上一级"),
        hover: Component = Component.empty(),
        width: Int = 150
    ) {
        Button(label, hover, width) { _, _ ->
            context.goBack()
        }
    }

    // 专属的退出按钮声明
    fun ExitButton(
        label: Component = d("退出菜单"),
        hover: Component = Component.empty(),
        width: Int = 100
    ) {
        // 自动注入回调拦截
        val autoAction = DialogAction.customClick(
            { _, audience ->
                // Audience 在服务端上下文中通常就是 Player
                if (audience is Player) {
                    val currentRoute = DialogRouter.getCurrentRoute(audience)

                    // 无论是因为什么原因导致玩家要退出当前原子事务，直接触发兜底钩子
                    if (currentRoute is AtomicRoute) {
                        currentRoute.onRollback(audience)
                    }

                    DialogRouter.clearHistory(audience)
                    audience.closeDialog()
                }
            },
            // 退出按钮通常点一次就够了，配置 lifetime 为默认即可
            ClickCallback.Options.builder().uses(1).build()
        )

        exitButton = ActionButton.create(label, hover, width, autoAction)
    }
}
