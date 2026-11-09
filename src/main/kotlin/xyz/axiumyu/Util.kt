@file:Suppress("UnstableApiUsage", "Unused")

package xyz.axiumyu

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemEnchantments
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.WrittenBookContent
import io.papermc.paper.registry.RegistryAccess.registryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.text.Filtered
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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
fun lore(vararg lines: String): List<Component> = lines.map { d(it) }

/**
 * 直接从mm文本列表转换为ItemLore
 */
fun itemLore(vararg line: String): ItemLore = ItemLore.lore(lore(*line))

/**
 * 从 pairs 直接转 ItemEnchantment
 */
fun itemEnch(vararg ens: Pair<Enchantment, Int>) : ItemEnchantments = ItemEnchantments.itemEnchantments(mapOf(*ens))

/**
 * 从 map 直接转 ItemEnchantment
 */
fun itemEnch(ens: Map<Enchantment, Int>) : ItemEnchantments = ItemEnchantments.itemEnchantments(ens)

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

/**
 * 递归移除该 Component 及其所有子组件上的点击事件。
 * 其他样式（颜色、悬停事件、插入文本等）均保留。
 */
fun Component.noClick(): Component {
    val cleanedChildren = children().map { it.noClick() }
    return clickEvent(null).children(cleanedChildren)
}

/**
 * 将 MiniMessage 字符串解析为 Component，并移除所有 click 标签。
 * 如果字符串中不包含 "<click"，则跳过正则，直接解析。
 */
fun String.noClick(): String {
    // 快速路径：90% 的消息其实没有 click 标签，避免不必要的正则开销
    val source = if (this.contains("<click", ignoreCase = true)) {
        this.replace(Regex("""</?click:[^>]*>"""), "")
    } else {
        this
    }
    return source
}

/**
 * 将首字母大写
 */
fun String.capitalize(): String{
    return lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * 给字符串加引号
 */
fun String.quote() = "\"$this\""

/**
 * 给字符串去引号
 */
fun String.deQuote() = removePrefix("\"").removeSuffix("\"")

/**
 * 直接减少物品耐久
 * 不会运行其他逻辑
 * 自动处理边缘情况
 */
fun ItemStack.damage(damage: Int = 1){
    val maxDmg = if (hasData(DataComponentTypes.MAX_DAMAGE)){
       getData(DataComponentTypes.MAX_DAMAGE)!!
    } else return
    val current = if (hasData(DataComponentTypes.DAMAGE)){
        getData(DataComponentTypes.DAMAGE)!!
    } else 0
    if (current + damage >= maxDmg) {
        subtract()
        return
    }
    setData(DataComponentTypes.DAMAGE, current + damage)
}

/**
 * 获取注册表的所有项列表
 */
fun <T : Keyed> getRegistries(category: RegistryKey<T>): List<String> =
    registryAccess().getRegistry(category).stream().map { it.key.toString() }.toList()

/**
 * 为玩家主手的物品签名，若为书与笔则按原版逻辑签名，若为成书则覆盖原有签名，均保留设置的自定义item_model（若有），若不含有这两个数据则不进行任何操作
 */
fun Player.signBook(title: String) {
    val item = inventory.itemInMainHand
    if (item.isEmpty) return
    item.signBook(title, name)
}

/**
 * 为玩家主手的物品签名，若为书与笔则按原版逻辑签名，若为成书则覆盖原有签名，均保留设置的自定义item_model（若有），若不含有这两个数据则不进行任何操作
 */
fun ItemStack.signBook(title: String, author: String) {

    val written = this.getData(DataComponentTypes.WRITTEN_BOOK_CONTENT)
    val writable = this.getData(DataComponentTypes.WRITABLE_BOOK_CONTENT)

    if (written != null) {
        // 已有成书组件：更新标题与作者，保留页面、generation、resolved
        val builder = WrittenBookContent.writtenBookContent(title, author)
            .generation(written.generation())
            .resolved(written.resolved())
        builder.addFilteredPages(written.pages() as List<Filtered<out ComponentLike>>)

        this.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.build())

        // 若同时存在书与笔组件，删除之
        if (writable != null) {
            this.unsetData(DataComponentTypes.WRITABLE_BOOK_CONTENT)
        }
    } else if (writable != null) {
        // 仅有书与笔组件：转换为成书
        val builder = WrittenBookContent.writtenBookContent(title, author)

        for (page in writable.pages()) {
            val raw = Component.text(page.raw())
            val filtered = page.filtered()?.let { Component.text(it) }
            if (filtered != null) {
                builder.addFilteredPage(Filtered.of(raw, filtered))
            } else {
                builder.addPage(raw)
            }
        }

        this.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.build())
        this.unsetData(DataComponentTypes.WRITABLE_BOOK_CONTENT)
    } else {
        // 既不是书与笔也不是成书，无操作
        return
    }

    // ITEM_MODEL 处理
    val currentModel = this.getData(DataComponentTypes.ITEM_MODEL)
    val writableBookModel = NamespacedKey.minecraft("writable_book")
    val writtenBookModel = NamespacedKey.minecraft("written_book")

    if (currentModel == null || currentModel == writableBookModel) {
        this.setData(DataComponentTypes.ITEM_MODEL, writtenBookModel)
    }
}