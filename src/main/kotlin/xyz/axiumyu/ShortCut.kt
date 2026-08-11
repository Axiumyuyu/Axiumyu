package xyz.axiumyu

import io.papermc.paper.registry.RegistryAccess.registryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys.TREASURE
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Keyed
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder

val mm = MiniMessage.miniMessage()

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
 * 注册表获取
 */
fun <T : Keyed> getRegistry(category: RegistryKey<T>, name: NamespacedKey) =
    registryAccess().getRegistry(category).get(name)

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
fun d(s: String) = mm.deserialize(s)

/**
 * 序列化
 */
fun s(c: Component): String = mm.serialize(c)