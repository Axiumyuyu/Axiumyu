package xyz.axiumyu

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.DialogKeys
import org.bukkit.NamespacedKey
import xyz.axiumyu.dialogdsl.dialog.BaseDialog

/**
 * 注册命令
 */
fun LifecycleEventManager<BootstrapContext>.registerCommand(
    block: LiteralArgumentBuilder<CommandSourceStack>,
    desc: String = ""
) {
    registerEventHandler(LifecycleEvents.COMMANDS.newHandler { event ->
        event.registrar().register(
            block.build(),
            desc
        )
    })
}

/**
 * 注册对话框
 */
fun LifecycleEventManager<BootstrapContext>.registerDialog(
    namespacedKey: NamespacedKey,
    block: BaseDialog
) {
    registerEventHandler(RegistryEvents.DIALOG.compose().newHandler { event ->
        event.registry().register(
            DialogKeys.create(namespacedKey),
            block.buildAction
        )
    })
}