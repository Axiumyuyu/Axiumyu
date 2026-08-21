@file:Suppress("UnstableApiUsage")

package xyz.axiumyu

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.DialogKeys
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlotGroup
import xyz.axiumyu.commanddsl.NodeBuilder
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
 * 注册命令
 */
fun LifecycleEventManager<BootstrapContext>.registerCommand(
    block: NodeBuilder,
    desc: String = ""
) {
    registerEventHandler(LifecycleEvents.COMMANDS.newHandler { event ->
        event.registrar().register(
            block.buildLiteral().build(),
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

/**
 * 注册 LoreEnchant
 * **不允许动态注册，所有注册必须在Bootstrap阶段完成**
 */
fun LifecycleEventManager<BootstrapContext>.registerLoreEnchantment(
    namespace: String,
    key: String,
    desc: String,
    maxLevel: Int = 1
) {
    val translationKey = "enchantment.$namespace.$key"
    registerEventHandler(
        RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
            event.registry().register(
                EnchantmentKeys.create(Key.key("$namespace:$key"))
            ) { it
                .description(Component.translatable(translationKey, d(desc)))
                .anvilCost(100)
                .activeSlots(emptyList())
                .maxLevel(maxLevel)
                .supportedItems(RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.BARRIER))
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
            }
        }
    )
}

/**
 * 注册 LoreEnchant
 * **不允许动态注册，所有注册必须在Bootstrap阶段完成**
 */
fun LifecycleEventManager<BootstrapContext>.registerLoreEnchantment(
   namespacedKey: NamespacedKey,
    desc: String,
    maxLevel: Int = 1
) {
    val translationKey = "enchantment.${namespacedKey.namespace}.${namespacedKey.key}"
    registerEventHandler(
        RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
            event.registry().register(
                EnchantmentKeys.create(Key.key(namespacedKey.toString()))
            ) { it
                .description(Component.translatable(translationKey, d(desc)))
                .anvilCost(100)
                .activeSlots(emptyList())
                .maxLevel(maxLevel)
                .supportedItems(RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.BARRIER))
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
            }
        }
    )
}