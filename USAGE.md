*...少女祈祷中...*

# 对话框 DSL 用法

> **测试源码位于 `xyz.axiumyu.paperDialogDsl.test.DialogTest`**

一般对话框使用 `DialogSetup` 或者 `RootDialogSetup` 创建（用于创建 `RootRoute` 类型的路由时可使用，会自动添加一个返回按钮）：

```kotlin
val dialog: BaseDialog = DialogSetup {
    //...
}
```

在 `AtomicRoute` 场景下，必须使用 `AtomicDialogSetup` 创建，此时不允许设置 `canCloseWithEscape()` 方法，会被覆盖为 `false`

```kotlin
val dialog: AtomicDialogBlueprint = AtomicDialogSetup {
    //...
}
```

`BaseDialog` 是一个包装器，`AtomicDialogBlueprint` 是它的 `value class` ~~，它唯一的作用就是限制在 AtomicRoute 下仅允许使用该方法创建对话框~~

`BaseDialog` 代表还未构建完成的对话框，可直接使用扩展方法在 Bootstrap 阶段注册，如果需要构建为静态对话框实时展示，则需要使用
`BaseDialog.build()` 方法

## 对话框内容

对话框由两部分组成，上半部分的对话框主体，和下半部分的按钮

对话框主体内可以设置文字，物品，各类输入框

> 由于 Paper 的一个 Bug [Issue #13555](https://github.com/PaperMC/Paper/issues/13555)，若要在 Bootstrap 阶段注册对话框，则不允许在对话框主体内设置物品展示
> 
> 动态构建和展示的对话框无此要求

**按钮只能设置在下半部分**，除了对话框列表类型的对话框，**其他对话框下半部分只能设置按钮**

### 对话框主体

使用 `DialogContent` 声明

```kotlin
val dialog = DialogSetup {
    DialogContent {
        // 文本，物品，输入控件等
    }
}
```

所有组件都可以设置宽度和悬浮文本，所有输入组件都拥有一个 id，初始值

允许使用的组件类型：

- Text，文本组件
- ItemDisplay，物品展示

输入组件，每一个输入组件都拥有一个 id，可以在下方的按钮中通过回调函数获取其值：

- NumRangeInput，浮点数值滑动条，可设定范围，步长等，使用 `getFloat(id: String): Float?` 获取值，当 id 不存在时返回 null
- TextInput，文本输入框，可设定最大字符数，换行策略等，使用 `getText(id: String): String?` 获取值，当 id 不存在时返回 null
- BoolInput，复选框，使用 `getBoolean(id: String): Boolean?` 获取值，当 id 不存在时返回 null

### 对话框类型

使用 `DialogType`声明，有以下几种类型：

- NOTICE: 允许 0/1 个按钮
- CONFIRMATION：允许 2 个按钮
- MULTI_ACTION：允许任意数量的按钮 ~~（只要屏幕放得下）~~ 和一个可选的退出按钮
- DIALOG_LIST：需要传入一个 `RegistrySet<Dialog>`，和一个可选的退出按钮
- SERVER_LINKS：仅允许一个可选的退出按钮

```kotlin
val dialog = DialogSetup {
    DialogContent {
        
    }
    DialogType(UIType.MULTI_ACTION) {
        // 按钮，或者什么都没有...
    }
}
```

### 按钮

Button 及其变种如 NavButton，BackButton，ExitButton 只能在这里使用

除 ExitButton 外，其它按钮都可以用于构建行为

NavButton 适合：

- 当按钮的唯一行为就是“跳转到另一个页面”或者“更新当前页面上的某些内容”时

BackButton 适合：

- 当按钮的唯一行为就是“返回到上一个页面”时

ExitButton 必须作为退出页面的按钮，可以无参调用，它应当承载关闭页面以及处理善后工作的功能，尤其是在 `AtomicRoute` 中。

---

按钮拥有一个回调函数，该函数传入两个参数： `DialogResponseView` 和 `Audience` ，前者用于从当前对话框状态中获取数据，后者为正在查看这个对话框的玩家（大多数时候都是玩家）

```kotlin
val dialog = DialogSetup {
    DialogContent {
        BoolInput("bool1", mm.deserialize("通知开关")) // 复选框 id 为 bool1
    }
    DialogType(UIType.NOTICE) {
        Button(mm.deserialize("切换通知"), mm.deserialize("你好"), 100) { view, audience ->
            val bool = view.getBoolean("bool1") // 获取在 DialogContent 中 id 为 bool1 的复选框的值
            audience.sendMessage("$bool") // 将获取到的值发送给当前玩家
        }
    }
}
```

# 路由系统

类似 Vue.js / Android Navigation ，如果你不了解这是什么，我对其的理解是在一组不同的对话框之间进行方便跳转，以及传递参数的工具

路由页面分为有参页面和无参页面，无参页面使用 `object` ，有参页面使用 `data class` ，参数及其类型在数据类的构造函数中定义

所有路由页面都需要间接继承 `RouteBase` ，根据页面的功能不同可选择继承 `RootRoute` / `Route` / `AtomicRoute` ，并实现 `render` 方法

该方法接受一个 `DialogRouteContext` ，用于后续的页面导航/跳转/返回，该方法需要返回一个 `BaseDialog` ，玩家打开该页面时渲染该对话框

```kotlin
object HomeRoute2 : RootRoute {
    override fun render(context: DialogRouteContext): BaseDialog = RootDialogSetup {
        DialogContent(mm.deserialize("主菜单")) {
            Text(mm.deserialize("你好，${context.player.name}"))
        }
        DialogType(UIType.MULTI_ACTION) {
            Button(mm.deserialize("<aqua>实体列表"), mm.deserialize(""), 100) { _, _ ->
                context.navigate(ListRoute) // 当玩家点击按钮时，导航到 ListRoute 页面
            }
        }
    }
}
```

`DialogRouteContext` 拥有以下常用方法：

- openRoot(Player, RouteBase)，清空玩家的返回栈，并打开一个新的页面
- navigate(RouteBase)，向返回栈中插入当前页面，并将玩家带到新的页面
- replace(RouteBase)，将玩家当前的页面替换为另一个页面，常用于更新当前页面
- goBack()，将玩家带回上一个页面，若没有上一个页面，则关闭玩家的页面

## 返回栈

按钮中的回调函数可以将玩家带到其他对话框界面，每一次 `navigate` 都会向返回栈中增加一个路由对象（除非该对象已经存在于返回栈内），该对象包含完整的路由参数和对话框

> 正如在 Android Navigation / Compose 导航中禁止向路由参数传递 Context / View 等，**禁止向路由参数中传递生命周期不受自己控制的对象，如区块，实体等等**
> 
> 建议尽可能只在路由参数中传递基本数据，例如 Int，String，以及只包括这些基本类型的数据结构等

## 页面更新与状态绑定

Minecraft 中的对话框是**不可变**的，一个对话框一旦展示给玩家，就无法再更改它的布局和内容，因此想要在对话框中更新内容，只能使用 replace 方法将当前页面替换为一个全新的页面

```kotlin
// 更新当前页面：定义页面的所有“可变状态”在主构造函数中，并设置默认值
data class SettingsRoute(
    val isNotificationsEnabled: Boolean = true,
    val clickCount: Int = 0
) : Route {

    override fun render(context: DialogRouteContext) = DialogSetup {

        DialogContent(mm.deserialize("what's up")) {
            Text(mm.deserialize("当前点击次数: <aqua>$clickCount</aqua>"))
            // 根据状态动态渲染图标
            val statusIcon = if (isNotificationsEnabled) "<green>开启" else "<red>关闭"
            Text(mm.deserialize("消息通知: $statusIcon"))

            TextInput("msg", mm.deserialize("这个对话框中的内容会在点击按钮后丢失"), "初始值")
        }

        DialogType(UIType.MULTI_ACTION, columns = 2) {

            // 按钮 1：开关
            Button(mm.deserialize("切换通知"), mm.deserialize(""), 100) { _, _ ->
                // 使用 Kotlin 的 copy() 生成一个只有布尔值取反的新状态
                // 然后用 replace() 替换当前路由，完成界面的“重绘”
                context.replace(this@SettingsRoute.copy(isNotificationsEnabled = !isNotificationsEnabled))
            }

            // 按钮 2：计数，同理
            Button(mm.deserialize("+1s"), mm.deserialize(""), 100) { _, _ ->
                context.replace(this@SettingsRoute.copy(clickCount = clickCount + 1))
            }

            // 退出与返回
            BackButton(context)
        }
    }
}
```

这么做存在一个问题，那些没有在 replace 方法中被传递的页面内容（例如实例中的输入框）将会丢失内容，因为它不在路由参数中，只存在于 `DialogResponseView` 中，因此按钮重绘时这些只存在于视图中的数据将丢失

解决方法就是将这些数据也改为参数，并在按钮中先获取这些内容，先复制到新的路由对象中，再进行下一步修改其他值

```kotlin
data class SettingsRoute(
    val clickCount: Int = 0,
    val msg: String // 将输入框的内容也加入参数
) : Route {
    fun bind(view: DialogResponseView): SettingsRoute {
        val text = view.getText("msg") ?: "" // 在这里先获取出输入框的内容
        return this.copy(msg = text) // 然后发送给新的对象
        // 如果还有其他输入框，其他对象也可以在这里处理
    }

    override fun render(context: DialogRouteContext) = DialogSetup {
        DialogContent(mm.deserialize("Never gonna give you up")) {
            Text(mm.deserialize("当前点击次数: <aqua>$clickCount</aqua>"))
            TextInput("msg", mm.deserialize("现在就不会丢失了"), msg) // 将初始值设置为参数
        }

        DialogType(UIType.MULTI_ACTION, columns = 2) {
            Button(mm.deserialize("更新页面"), mm.deserialize(""), 100) { view, _ ->
                // 先把view中的数据获取了，传递给bind复制出一个新的对象，然后再次更新其他内容
                context.replace(bind(view).copy(clickCount = clickCount + 1))
            }
            // 退出与返回，这个按钮不需要使用bind，因为它本身就有丢弃当前页面的意思
            BackButton(context)
        }
    }
}
```

如果这么做，所有的按钮都需要使用 `bind(view)` 方法来先获取视图中的数据后，再更改其他值

## 表单处理与值校验

Button 内部可以使用 `validate` 方法开启一个值校验域，这相当于一个 `runCatching` 块，内部发生的所有异常都会被捕获，然后中断后续执行，转而执行传入的处理块

`validate` 方法需要两个函数参数，第一个参数是当内部校验失败时，调用的方法，第二个是需要校验的内容

除此之外，还可以使用扩展方法 `withError` 来指定当前位置出错或者为空时，该发送什么错误消息

```kotlin
data class EditItemBasicRoute(
    val itemType: NameSpacedKey,
    val errorMsg: String? = null
) : Route {

    override fun render(context: DialogRouteContext) = DialogSetup {

        DialogContent(mm.deserialize("步骤 1/3：基础配置")) {
            if (errorMsg != null) {
                Text(mm.deserialize("<red>⚠ 校验失败: $errorMsg"))
            } else {
                Text(mm.deserialize("<gray>请输入你期望的自定义名称："))
            }

            TextInput("input_name", mm.deserialize("物品名称"))
        }
        DialogType(UIType.CONFIRMATION) {
            Button(mm.deserialize("<gold>下一步"), mm.deserialize("配置附魔属性"), 100) { view, _ ->
                validate({
                    // 内部任意一行语句发生异常则调用
                    context.replace(this@EditItemBasicRoute.copy(errorMsg = it))
                }) {
                    // 指定在该行发生异常或为空时的错误消息，若未发生异常且不为空则返回原本的非空类型，不需要 elvis 操作符
                    val input = view.getText("input_name").withError("未知输入框名称")
                    // 还可以使用 Boolean 类型判断自定义条件，当不满足条件时立刻退出，执行错误语句
                    input.isNotEmpty().withError("名称不能为空")
                    // 从注册表中读取物品类型，若注册表不存在（抛出异常，尽管不可能发生）或是物品未找到（返回null）都退出，执行错误语句
                    val type = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM)[itemType].withError("未找到该物品")
                    // 若代码能执行到这里，type 一定是 ItemType！
                    context.navigate(EditItemEnchantsRoute(type, input))
                }
            }

            Button(mm.deserialize("取消"), mm.deserialize("返回主菜单"), 100) { _, _ ->
                context.goBack()
            }
        }
    }
}
```