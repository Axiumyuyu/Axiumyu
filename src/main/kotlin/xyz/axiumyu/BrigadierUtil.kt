@file:Suppress("UnstableApiUsage", "Unused")
package xyz.axiumyu

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import org.bukkit.entity.Entity

fun chooseSelectedEntity(ctx: CommandContext<CommandSourceStack>, argumentName: String): Entity? =
    ctx.getArgument(argumentName, EntitySelectorArgumentResolver::class.java)
        .resolve(ctx.source).firstOrNull()

fun <T> SuggestionsBuilder.suggestAll(list : Collection<T>) {
    list.forEach {
        suggest(it.toString())
    }
}

fun node(name: String) = Commands.literal(name)

fun error(ctx: CommandContext<CommandSourceStack>, msg: String): Int {
    ctx.source.sender.sendMessage(d("<red>$msg", MMMode.ADMIN))
    return 0
}