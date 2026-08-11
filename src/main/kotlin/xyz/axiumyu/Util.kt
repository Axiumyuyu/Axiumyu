package xyz.axiumyu

import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

/**
 * 将 pitch 和 yaw 转换为 org.bukkit.util.Vector
 */
fun pitchYaw2Vector(pitch: Number, yaw: Number): Vector {
    //调整yaw方向：在Minecraft中，+yaw是向东转，所以我们需要使用-yaw
    val adjustedYaw = -Math.toRadians(yaw.toDouble())

    // Pitch不需要调整，但是要记得Minecraft的pitch是向下为正
    val pitchRad = Math.toRadians(pitch.toDouble())

    //计算方向向量的各个分量
    val x = -sin(adjustedYaw) * cos(pitchRad) //注意X轴方向
    val y = sin(pitchRad)                     //在Minecraft中Y轴向上为正
    val z = cos(adjustedYaw) * cos(pitchRad)  //注意Z轴方向

    return Vector(x, y, z)
}

/**
 * 将MiniMessage文本列表转换为Component列表
 */
fun lore(vararg lines: String): List<Component> = lines.map { mm.deserialize(it) }

/**
 * 直接从mm文本列表转换为ItemLore
 */
fun itemLore(vararg line: String): ItemLore = ItemLore.lore(lore(*line))

/**
 * 将Componet 转换为 PlainText
 */
val Component.plainText: String get() = PlainTextComponentSerializer.plainText().serialize(this)

/**
 * 将字符串转为NamespacedKey
 */
fun String.toNamespacedKey(): NamespacedKey {
    val value = trim().split(":")
    return if (value.size == 1){
        NamespacedKey.minecraft(value[0])
    } else {
        NamespacedKey(value[0], value[1])
    }
}