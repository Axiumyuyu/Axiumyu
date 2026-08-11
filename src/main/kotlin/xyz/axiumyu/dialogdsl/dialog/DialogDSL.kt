package xyz.axiumyu.dialogdsl.dialog

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import xyz.axiumyu.dialogdsl.dialog.dsl.DialogRootScope

// 1. 基础图纸接口
sealed interface BaseDialog {
    val buildAction: DialogRegistryEntry.Builder.() -> Unit
}

@JvmInline value class NormalDialog(override val buildAction: DialogRegistryEntry.Builder.() -> Unit) : BaseDialog
@JvmInline value class AtomicDialog(override val buildAction: DialogRegistryEntry.Builder.() -> Unit) : BaseDialog

// 3. 扩展构建方法保持不变
fun BaseDialog.build(): Dialog {
    return Dialog.create { it.empty().apply(this.buildAction) }
}

fun Component.plainText() = PlainTextComponentSerializer.plainText().serialize(this)

inline fun DialogSetup(crossinline block: DialogRootScope.() -> Unit): BaseDialog {
    return NormalDialog { DialogRootScope(this).block() }
}

/**
 * 专为 RootRoute 准备的 DSL。
 * 会在末尾自动注入 ExitButton。
 */
inline fun RootDialogSetup(crossinline block: DialogRootScope.() -> Unit): BaseDialog {
    return NormalDialog { DialogRootScope(this).apply { isRoot = true }.block() }
}

/**
 * 专为 AtomicRoute 准备的 DSL。
 * 会自动禁用 ESC 强退。
 */
inline fun AtomicDialogSetup(crossinline block: DialogRootScope.() -> Unit): AtomicDialog {
    return AtomicDialog { DialogRootScope(this).apply { isAtomic = true }.block() }
}