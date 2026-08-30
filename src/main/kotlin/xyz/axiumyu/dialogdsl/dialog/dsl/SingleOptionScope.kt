@file:Suppress("UnstableApiUsage", "Unused")

package xyz.axiumyu.dialogdsl.dialog.dsl

import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import net.kyori.adventure.text.Component

@PaperDialogDsl
class SingleOptionScope(
    private val initialId: String?
) {
    val entries = mutableListOf<SingleOptionDialogInput.OptionEntry>()

    infix fun String.to(display: Component) {
        entries.add(SingleOptionDialogInput.OptionEntry.create(this, display, this == initialId))
    }
}