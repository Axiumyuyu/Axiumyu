package xyz.axiumyu.dialogdsl.dialog.dsl

import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

@PaperDialogDsl
@SuppressWarnings("UnstableApiUsage")
class DialogBaseScope(
    private val nativeBuilder: DialogBase.Builder
) : DialogBase.Builder by nativeBuilder {
    val inputList = mutableListOf<DialogInput>()
    val bodyList = mutableListOf<DialogBody>() // 这里的 DialogBody 是 Paper 原生的接口

    // ==========================================
    // 1. 展示内容 (Body)
    // ==========================================

    // 纯文本消息展示 (合并了有 width 和无 width 的方法)
    fun Text(contents: Component, width: Int? = null) {
        if (width == null) {
            bodyList.add(DialogBody.plainMessage(contents))
        } else {
            bodyList.add(DialogBody.plainMessage(contents, width))
        }
    }

    // 物品展示 (方式 A：使用 Lambda 进一步配置 Builder，适合需要灵活控制时)
    inline fun ItemDisplay(item: ItemStack, block: ItemDialogBody.Builder.() -> Unit = {}) {
        val builder = DialogBody.item(item)
        builder.apply(block)
        bodyList.add(builder.build())
    }

    // 物品展示 (方式 B：全参数覆盖。经过优化，你只需要传 Component 描述即可，内部自动转换)
    fun ItemDisplay(
        item: ItemStack,
        description: Component? = null,
        showDecorations: Boolean = true,
        showTooltip: Boolean = true,
        width: Int = 16,
        height: Int = 16
    ) {
        // 自动将 Component 包装为底层的 PlainMessageDialogBody，让调用侧更清爽
        val descBody = description?.let { DialogBody.plainMessage(it) }
        bodyList.add(DialogBody.item(item, descBody, showDecorations, showTooltip, width, height))
    }

    // --- Bool Input ---
    fun BoolInput(id: String, name: Component) {
        inputList.add(DialogInput.bool(id, name).build())
    }

    fun BoolInput(
        id: String,
        name: Component,
        initial: Boolean,
        onTrue: String, // 注意：如果你使用的 Paper 版本这里接受的是 String，保持原样。通常可能是 Component
        onFalse: String
    ) {
        // 注意：根据你的原始代码，这里没有调用 .build()，如果 API 实际上返回的是 Builder，你需要补上 .build()
        inputList.add(DialogInput.bool(id, name, initial, onTrue, onFalse))
    }

    // --- Number Range Input ---
    fun NumRangeInput(id: String, name: Component, range: Pair<Float, Float>, init: Float = range.first, step: Float = 0.1f, width: Int = 150) {
        inputList.add(DialogInput.numberRange(id, name, range.first, range.second).initial(init).width(width).step(step).build())
    }

    // --- Single Option Input ---
    fun SingleOptionInput(
        id: String,
        name: Component,
        entries: List<SingleOptionDialogInput.OptionEntry>
    ) {
        inputList.add(DialogInput.singleOption(id, name, entries).build())
    }

    fun SingleOptionInput(
        id: String,
        width: Int,
        entries: List<SingleOptionDialogInput.OptionEntry>,
        name: Component,
        labelVisible: Boolean
    ) {
        inputList.add(DialogInput.singleOption(id, width, entries, name, labelVisible))
    }

    // --- Text Input ---
    fun TextInput(id: String, name: Component) {
        inputList.add(DialogInput.text(id, name).build())
    }

    fun TextInput(
        id: String,
        name: Component,
        initial: String,
        labelVisible: Boolean = true,
        maxLength: Int = 1024,
        width: Int = 150,
        multilineOptions: TextDialogInput.MultilineOptions? = null
    ) {
        inputList.add(
            DialogInput.text(
                id,
                width,
                name,
                labelVisible,
                initial,
                maxLength,
                multilineOptions
            )
        )
    }
}