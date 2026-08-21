@file:Suppress("UnstableApiUsage")

package xyz.axiumyu.dialogdsl.dialog.dsl

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.text.Component
import xyz.axiumyu.mm

@PaperDialogDsl
@SuppressWarnings("UnstableApiUsage")
class DialogRootScope(@PublishedApi internal val entryBuilder: DialogRegistryEntry.Builder) {

    // DSL 上下文状态标记
    @PublishedApi internal var isAtomic = false
    @PublishedApi internal var isRoot = false
    inline fun DialogContent(title: Component, block: DialogBaseScope.() -> Unit = {}) {
        // 为了避免变量名冲突，把原生的 base builder 改名叫 baseBuilder
        val baseBuilder = DialogBase.builder(title)
        val scope = DialogBaseScope(baseBuilder)
        scope.block()
        if (isAtomic) {
            baseBuilder.canCloseWithEscape(false)
        }


        if (scope.bodyList.isNotEmpty()) {
            baseBuilder.body(scope.bodyList)
        }
        if (scope.inputList.isNotEmpty()) {
            baseBuilder.inputs(scope.inputList)
        }

        // 用外部传入的 entryBuilder 替代之前的 this
        entryBuilder.base(baseBuilder.build())
    }

    // 变成成员函数
    inline fun DialogType(
        type: UIType,
        columns: Int = 1,
        buttonWidth: Int = 200,
        dialogs: RegistrySet<Dialog>? = null,
        block: DialogTypeScope.() -> Unit = {}
    ) {
        val scope = DialogTypeScope().apply(block)
        if (isRoot && scope.exitButton == null) {
            scope.ExitButton(mm.deserialize("<red>关闭菜单"), mm.deserialize("彻底退出"), 100)
        }
        val btns = scope.buttons
        val finalExit = scope.exitButton

        val paperType = when (type) {
            UIType.NOTICE -> {
                if (btns.isEmpty()) DialogType.notice() else DialogType.notice(btns.first())
            }
            UIType.CONFIRMATION -> {
                require(btns.size == 2) { "【UI 错误】CONFIRMATION 必须包含且仅包含 2 个 Button" }
                DialogType.confirmation(btns[0], btns[1])
            }
            UIType.MULTI_ACTION -> {
                DialogType.multiAction(btns, finalExit, columns)
            }
            UIType.DIALOG_LIST -> {
                requireNotNull(dialogs) { "【UI 错误】DIALOG_LIST 必须通过参数传入 dialogs 集合" }
                DialogType.dialogList(dialogs, finalExit, columns, buttonWidth)
            }
            UIType.SERVER_LINKS -> {
                DialogType.serverLinks(finalExit, columns, buttonWidth)
            }
        }

        // 用外部传入的 entryBuilder 替代之前的 this
        entryBuilder.type(paperType)
    }
}