@file:Suppress("UnstableApiUsage", "Unused")
package xyz.axiumyu

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS
import io.papermc.paper.datacomponent.DataComponentTypes.STORED_ENCHANTMENTS
import io.papermc.paper.registry.RegistryAccess.registryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys.TREASURE
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.Keyed
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.plugin.PluginManager

private val mm = MiniMessage.miniMessage()

private val safeMM = MiniMessage.builder()
    .tags(
        TagResolver.builder()
            .resolver(StandardTags.color())
            .resolver(StandardTags.decorations())
            .resolver(StandardTags.font())
            .resolver(StandardTags.gradient())
            .resolver(StandardTags.rainbow())
            .resolver(StandardTags.shadowColor())
            .resolver(StandardTags.sprite())
            .resolver(StandardTags.transition())
            .resolver(StandardTags.reset())
            .resolver(StandardTags.newline())
            .resolver(StandardTags.hoverEvent())
            .resolver(StandardTags.insertion())
            .resolver(StandardTags.keybind())
            .resolver(StandardTags.translatable())
            .resolver(StandardTags.translatableFallback())
            .resolver(StandardTags.pride())
            .resolver(StandardTags.shadowColor())
            .resolver(StandardTags.clickEvent())
//            .resolver(StandardTags.selector())
//            .resolver(StandardTags.score())
//            .resolver(StandardTags.nbt())
            .resolver(StandardTags.sequentialHead())
            .build()
    ).build()

/**
 * 从整数创建 Location
 */
fun Location(world: World, x: Int, y: Int, z: Int) : Location {
    return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
}

/**
 * 将带小数点和百分比的数字转换为 Number
 */
fun String?.toNumber() : Number{
    if (this == null) return 0
    val number = this.trim()
    return if (number.endsWith('%')){
        number.dropLast(1).toDouble() / 100.0
    } else if (number.contains('.')){
        number.toDouble()
    } else{
        number.toInt()
    }
}

/**
 * PDC缩写
 */
val <T : PersistentDataHolder> T.pdc: PersistentDataContainer
    get() = persistentDataContainer

/**
 * 获取物品的默认名称
 */
val ItemType.name
    get() = this.getDefaultData(DataComponentTypes.ITEM_NAME)?.plainText

/**
 * 注册表获取
 */
fun <T : Keyed> getRegistry(category: RegistryKey<T>, name: NamespacedKey) =
    registryAccess().getRegistry(category).get(name)

/**
 * 注册表获取
 */
fun <T : Keyed> RegistryKey<T>.get(name: NamespacedKey) =
    registryAccess().getRegistry(this).get(name)

/**
 * 获取物品
 */
fun getItem(name: String) = getRegistry(RegistryKey.ITEM, NamespacedKey.minecraft(name))

/**
 * 附魔是否为宝藏
 */
val Enchantment.isTreasures get() = registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTagValues(TREASURE).contains(this)

/**
 * 附魔是否为诅咒
 */
val Enchantment.isCurse get() = registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTagValues(EnchantmentTagKeys.CURSE).contains(this)

/**
 * 反序列化
 */
fun d(s: String, mode: MMMode = MMMode.ADMIN) = when (mode){
    MMMode.SAFE -> safeMM.deserialize(s).noCommand()
    MMMode.ADMIN -> mm.deserialize(s)
    MMMode.STRICT_NOCLICK -> safeMM.deserialize(s).noClick()
}

/**
 * 序列化
 */
fun s(c: Component, mode: MMMode = MMMode.ADMIN): String = when (mode) {
    MMMode.SAFE -> safeMM.serialize(c.noCommand())
    MMMode.ADMIN -> mm.serialize(c)
    MMMode.STRICT_NOCLICK -> safeMM.serialize(c.noClick())
}

private fun Component.noCommand(): Component {
    // 1. 递归处理子组件
    val newChildren = this.children().map { it.noCommand() }

    // 2. 判断当前节点是否带有命令类 ClickEvent
    val clickEvent = this.clickEvent()
    val hasCommandClick = clickEvent != null &&
            (clickEvent.action() == ClickEvent.Action.RUN_COMMAND ||
                    clickEvent.action() == ClickEvent.Action.SUGGEST_COMMAND)

    // 3. 若需要移除，则通过 style 重建（保留 style 中其余所有属性）
    val stripped = if (hasCommandClick) {
        this.style(this.style().clickEvent(null))
    } else {
        this
    }

    // 4. 挂接处理后的子组件并返回
    return stripped.children(newChildren)
}

/**
 * 添加权限
 */
fun PluginManager.addPerm(perm: String, def: PermissionDefault = PermissionDefault.OP) {
    val perm = Permission(perm, def)
    if (!permissions.contains(perm)) {
        addPermission(perm)
    }
}

/**
 * 需求附魔并返回，若不存在直接报错
 */
fun requireEnchantment(namespace: String = Key.MINECRAFT_NAMESPACE, key: String): Enchantment {
    return getRegistry(RegistryKey.ENCHANTMENT, NamespacedKey(namespace, key)) ?: throw IllegalStateException("Enchantment: $namespace:$key not found!")
}

/**
 * 需求附魔并返回，若不存在直接报错
 */
fun requireEnchantment(key: NamespacedKey): Enchantment {
    return getRegistry(RegistryKey.ENCHANTMENT, key) ?: throw IllegalStateException("Enchantment: $key not found!")
}

/**
 * 添加附魔；若该附魔已存在则改为 lvl（lvl 不允许为 0）
 */
fun ItemStack.addEnch(ench: Enchantment, lvl: Int = 1) {
    require(lvl != 0) { "lvl 不能为 0（删除附魔请用 removeEnch）" }

    val type = if (getData(STORED_ENCHANTMENTS) != null) {
        STORED_ENCHANTMENTS
    } else {
        ENCHANTMENTS
    }
    val enchants: MutableMap<Enchantment, Int> = mutableMapOf()

    getData(type)?.enchantments()?.let { enchants.putAll( it ) }

    enchants[ench] = lvl
    setData(type, itemEnch(enchants))
}

/**
 * 移除附魔；若移除后组件已空，则连同组件一起移除
 */
fun ItemStack.removeEnch(ench: Enchantment) {
    val type = if (getData(STORED_ENCHANTMENTS) != null) {
        STORED_ENCHANTMENTS
    } else {
        ENCHANTMENTS
    }
    val current = getData(type)?.enchantments() ?: return
    if (ench !in current) return

    val enchants = mutableMapOf<Enchantment, Int>()

    enchants.putAll(current)
    enchants.remove(ench)

    if (enchants.isEmpty()) {
        unsetData(type)
    } else {
        setData(type, itemEnch(enchants))
    }
}