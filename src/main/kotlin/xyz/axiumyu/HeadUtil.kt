package xyz.axiumyu

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.UUID
import kotlin.io.encoding.Base64

/**
 * 通过玩家名称构造头颅。
 * 客户端渲染时会自动解析该玩家的当前皮肤(需联网)。
 * [name] 须为 16 字符以内的合法玩家名。
 */
fun byName(name: String): ItemStack =
    head(ResolvableProfile.resolvableProfile().name(name).build())

/**
 * 通过玩家 UUID 构造头颅。
 * 客户端渲染时会自动解析该 UUID 对应的当前皮肤(需联网)。
 */
fun byUUID(uuid: UUID): ItemStack =
    head(ResolvableProfile.resolvableProfile().uuid(uuid).build())

/**
 * 通过材质贴图 URL 构造头颅。
 * 将 URL 编码为标准 Mojang 材质 JSON 后 Base64 写入静态 profile,
 * 不依赖玩家存在,也不触发任何网络请求。
 *
 * [url] 通常形如 `http://textures.minecraft.net/texture/<hash>`
 */
fun byTextureUrl(url: String): ItemStack {
    val base64 = Base64.encode(
        """{"textures":{"SKIN":{"url":"$url"}}}""".toByteArray()
    )
    val profile = ResolvableProfile.resolvableProfile()
        .addProperty(ProfileProperty("textures", base64))
        .build()
    return head(profile)
}

private fun head(profile: ResolvableProfile): ItemStack =
    ItemType.PLAYER_HEAD.createItemStack(1).apply {
        setData(DataComponentTypes.PROFILE, profile)
    }