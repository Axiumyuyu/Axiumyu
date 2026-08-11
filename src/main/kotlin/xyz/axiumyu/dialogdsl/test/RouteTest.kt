package xyz.axiumyu.dialogdsl.test

import xyz.axiumyu.d
import xyz.axiumyu.dialogdsl.dialog.DialogSetup
import xyz.axiumyu.dialogdsl.dialog.RootDialogSetup
import xyz.axiumyu.dialogdsl.dialog.dsl.UIType
import xyz.axiumyu.dialogdsl.route.AutoRootSetup
import xyz.axiumyu.dialogdsl.route.DialogRouteContext
import xyz.axiumyu.dialogdsl.route.RootRoute
import xyz.axiumyu.dialogdsl.route.Route

// ==================
// 页面 1：首页 (无参数，使用 object)
// ==================
object HomeRoute2 : RootRoute {
    override fun render(context: DialogRouteContext) = RootDialogSetup {
        DialogContent(d("主菜单")) {
            Text(d("你好，${context.player.name}"))
        }
        DialogType(UIType.MULTI_ACTION) {
            Button(d("实体列表"), d(""), 100) { _, _ ->
                context.navigate(ListRoute)
            }
            Button(d("EditRoute"), d(""), 100) { _, _ ->
                context.navigate(EditRoute("uuid-1234", "Pig"))
            }
            Button(d("SettingRoute"), d(""), 100) { _, _ ->
                context.navigate(SettingsRoute())
            }
        }
    }
}

object HomeRoute3 : RootRoute {
    override fun render(context: DialogRouteContext) = RootDialogSetup {
        DialogContent(d("主菜单")) {
            Text(d("你好，${context.player.name}"))
        }
        DialogType(UIType.MULTI_ACTION) {
            NavButton(context, d("实体列表"), d("点击前往"), 100,
                ListRoute
            )
            NavButton(
                context, d("EditRoute"), d("点击前往"), 100,
                EditRoute("uuid-1234", "Pig")
            )
            NavButton(
                context, d("SettingRoute"), d("点击前往"), 100,
                SettingsRoute()
            )
        }
    }
}

object HomeRoute : RootRoute {
    override fun render(context: DialogRouteContext) =
        AutoRootSetup(context, d("主菜单")) {
            "实体列表" to ListRoute
            "编辑" to EditRoute("uuid-1234", "Pig")
            "设置" to SettingsRoute()
            "物品" to ItemEditorMenu
        }
}

// ==================
// 页面 2：列表页 (无参数，使用 object)
// ==================
object ListRoute : Route {
    override fun render(context: DialogRouteContext) = DialogSetup {
        DialogContent(d("实体列表"))
        DialogType(UIType.MULTI_ACTION, columns = 2) {
            Button(d("编辑这只猪"), d(""), 100) { _, _ ->
                context.navigate(EditRoute("uuid-1234", "Pig"))
            }
            BackButton(context)
            ExitButton()
        }
    }
}

// ==================
// 页面 3：编辑详情页 (带参数，使用 data class)
// ==================
data class EditRoute(val targetId: String, val entityType: String) : Route {
    override fun render(context: DialogRouteContext) = DialogSetup {
        DialogContent(d("编辑：$entityType")) {
            Text(d("当前正在操作 UUID: $targetId"))
        }
        DialogType(UIType.NOTICE) {
            BackButton(context)
        }
    }
}

// 更新当前页面：定义页面的所有“可变状态”在主构造函数中，并设置默认值
data class SettingsRoute(
    val isNotificationsEnabled: Boolean = true,
    val clickCount: Int = 0
) : Route {

    override fun render(context: DialogRouteContext) = DialogSetup {

        DialogContent(d("个人设置")) {
            Text(d("当前点击次数: <aqua>$clickCount</aqua>"))
            // 根据状态动态渲染图标
            val statusIcon = if (isNotificationsEnabled) "<green>开启" else "<red>关闭"
            Text(d("消息通知: $statusIcon"))
        }

        DialogType(UIType.MULTI_ACTION, columns = 2) {

            // 按钮 1：开关
            Button(d("切换通知"), d(""), 100) { _, _ ->
                // 使用 Kotlin 的 copy() 生成一个只有布尔值取反的新状态
                // 然后用 replace() 替换当前路由，完成界面的“重绘”
                context.replace(this@SettingsRoute.copy(isNotificationsEnabled = !isNotificationsEnabled))
            }

            // 按钮 2：计数
            Button(d("+1s"), d(""), 100) { _, _ ->
                context.replace(this@SettingsRoute.copy(clickCount = clickCount + 1))
            }

            // 退出与返回
            BackButton(context)
        }
    }
}
