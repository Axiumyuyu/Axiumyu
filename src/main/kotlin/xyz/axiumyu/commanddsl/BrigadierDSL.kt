package xyz.axiumyu.commanddsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.permissions.PermissionDefault
import xyz.axiumyu.addPerm


@DslMarker
annotation class BrigadierDsl

private fun debugLog(message: String) {
    println("[BrigadierDSL-Debug] $message")
}


/**
 * 权限延迟注册器
 * 用于解决 Paper Bootstrap 阶段 Bukkit.getPluginManager() 为 null 的问题
 */
object PermissionRegistry {
    private val pendingPermissions = mutableMapOf<String, PermissionDefault>()

    /**
     * 暂存待注册权限
     */
    internal fun defer(permName: String, default: PermissionDefault) {
        pendingPermissions.putIfAbsent(permName, default)
        debugLog("Deferred permission: '$permName' (Default: $default)")
    }

    /**
     * 必须在 JavaPlugin.onEnable() 中调用
     */
    internal fun registerAll() {
        try {
            val pm = getPluginManager()
            debugLog("Registering deferred permissions (${pendingPermissions.size} total)...")
            pendingPermissions.forEach { (permName, default) ->
                pm.addPerm(permName, default)
                debugLog("Registered permission in Bukkit: '$permName' ($default)")
            }
            pendingPermissions.clear()
        } catch (e: Exception) {
            debugLog("Error registering permissions: ${e.message}")
        }
    }
}

/**
 * 顶层独立字面量节点创建函数 (PascalCase DSL 入口)
 */
fun Node(
    name: String,
    permPrefix: String? = null,
    block: NodeBuilder.() -> Unit = {}
): NodeBuilder {
    return NodeBuilder(name, permPrefix).apply(block)
}

/**
 * 顶层独立参数节点创建函数
 */
fun <T> Argument(
    name: String,
    type: ArgumentType<T>,
    permPrefix: String? = null,
    block: ArgumentNodeBuilder<T>.() -> Unit = {}
): ArgumentNodeBuilder<T> {
    return ArgumentNodeBuilder(name, type, permPrefix).apply(block)
}

@BrigadierDsl
class NodeBuilder(
    val name: String,
    private val permPrefix: String?
) {
    internal var requirement: ((CommandSourceStack) -> Boolean)? = null
    internal val children = mutableListOf<ArgumentBuilder<CommandSourceStack, *>>()
    internal var executor: ((CommandContext<CommandSourceStack>) -> Unit)? = null

    /**
     * 克隆当前节点构建器，防止多分支复用时产生状态污染
     */
    fun copy(): NodeBuilder {
        val cloned = NodeBuilder(name, permPrefix)
        cloned.requirement = this.requirement
        cloned.children.addAll(this.children)
        cloned.executor = this.executor
        return cloned
    }

    operator fun NodeBuilder?.unaryPlus() {
        val node = requireNotNull(this) {
            "尝试挂载的 NodeBuilder 为 null！请检查顶层变量 (val) 的声明顺序：子节点必须定义在父节点之前，或者使用 'by lazy'。"
        }
        Attach(node)
    }

    operator fun ArgumentNodeBuilder<*>?.unaryPlus() {
        val arg = requireNotNull(this) {
            "尝试挂载的 ArgumentNodeBuilder 为 null！请检查顶层变量 (val) 的声明顺序：子节点必须定义在父节点之前，或者使用 'by lazy'。"
        }
        Attach(arg)
    }

    fun Perm(
        subName: String = name,
        default: PermissionDefault = PermissionDefault.OP
    ) {
        val fullPerm = if (permPrefix != null) "$permPrefix.$subName" else subName
        registerPermissionIfAbsent(fullPerm, default)
        requirement = { stack -> stack.sender.hasPermission(fullPerm) }
    }

    fun Require(predicate: (CommandSourceStack) -> Boolean) {
        this.requirement = predicate
    }

    fun Attach(child: NodeBuilder?) {
        val node = requireNotNull(child) { "Attach 接收到的 NodeBuilder 为 null！" }
        children.add(node.buildLiteral())
    }

    fun Attach(child: ArgumentNodeBuilder<*>?) {
        val arg = requireNotNull(child) { "Attach 接收到的 ArgumentNodeBuilder 为 null！" }
        children.add(arg.buildRequired())
    }

    fun Use(child: NodeBuilder?, block: NodeBuilder.() -> Unit = {}) {
        val node = requireNotNull(child) { "Use 接收到的 NodeBuilder 为 null！" }
        val cloned = node.copy().apply(block)
        children.add(cloned.buildLiteral())
    }

    fun <U> Use(child: ArgumentNodeBuilder<U>?, block: ArgumentNodeBuilder<U>.() -> Unit = {}) {
        val arg = requireNotNull(child) { "Use 接收到的 ArgumentNodeBuilder 为 null！" }
        val cloned = arg.copy().apply(block)
        children.add(cloned.buildRequired())
    }

    fun Node(name: String, block: NodeBuilder.() -> Unit) {
        val child = NodeBuilder(name, permPrefix).apply(block)
        children.add(child.buildLiteral())
    }

    fun <T> Argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentNodeBuilder<T>.() -> Unit
    ) {
        val child = ArgumentNodeBuilder(name, type, permPrefix).apply(block)
        children.add(child.buildRequired())
    }

    fun Execute(block: CommandExecutionScope.(params: CommandExecutionScope, source: CommandSourceStack) -> Unit) {
        this.executor = { ctx ->
            val scope = CommandExecutionScope(ctx)
            scope.block(scope, ctx.source)
        }
    }

    private fun registerPermissionIfAbsent(permName: String, default: PermissionDefault) {
        PermissionRegistry.defer(permName, default)
    }

    fun buildLiteral(): LiteralArgumentBuilder<CommandSourceStack> {
        debugLog("Building Literal Node: '$name' (Children: ${children.size}, HasExecutor: ${executor != null}, HasRequirement: ${requirement != null})")
        val builder = LiteralArgumentBuilder.literal<CommandSourceStack>(name)

        requirement?.let { req ->
            builder.requires { stack ->
                val result = try {
                    req(stack)
                } catch (e: Exception) {
                    debugLog("Exception in requirement check for Literal Node '$name': ${e.message}")
                    false
                }
                debugLog("Requirement check for Literal Node '$name' on sender '${stack.sender.name}': $result")
                result
            }
        }

        executor?.let { exec ->
            builder.executes { ctx ->
                debugLog("Executing Literal Node '$name' by '${ctx.source.sender.name}'")
                try {
                    exec(ctx)
                } catch (e: Exception) {
                    debugLog("Error executing Literal Node '$name': ${e.message}")
                    e.printStackTrace()
                }
                Command.SINGLE_SUCCESS
            }
        }

        children.forEach { child ->
            builder.then(child)
        }
        return builder
    }
}

@BrigadierDsl
class ArgumentNodeBuilder<T>(
    val name: String,
    private val type: ArgumentType<T>,
    private val permPrefix: String?
) {
    internal var requirement: ((CommandSourceStack) -> Boolean)? = null
    internal val children = mutableListOf<ArgumentBuilder<CommandSourceStack, *>>()
    internal var executor: ((CommandContext<CommandSourceStack>) -> Unit)? = null
    internal var suggestionProvider: ((CommandContext<CommandSourceStack>, SuggestionsBuilder) -> Unit)? = null

    fun copy(): ArgumentNodeBuilder<T> {
        val cloned = ArgumentNodeBuilder(name, type, permPrefix)
        cloned.requirement = this.requirement
        cloned.children.addAll(this.children)
        cloned.executor = this.executor
        cloned.suggestionProvider = this.suggestionProvider
        return cloned
    }

    operator fun NodeBuilder?.unaryPlus() {
        val node = requireNotNull(this) { "尝试挂载的 NodeBuilder 为 null！" }
        Attach(node)
    }

    operator fun ArgumentNodeBuilder<*>?.unaryPlus() {
        val arg = requireNotNull(this) { "尝试挂载的 ArgumentNodeBuilder 为 null！" }
        Attach(arg)
    }

    fun Perm(
        subName: String = name,
        default: PermissionDefault = PermissionDefault.OP
    ) {
        val fullPerm = if (permPrefix != null) "$permPrefix.$subName" else subName
        registerPermissionIfAbsent(fullPerm, default)
        requirement = { stack -> stack.sender.hasPermission(fullPerm) }
    }

    fun Require(predicate: (CommandSourceStack) -> Boolean) {
        this.requirement = predicate
    }

    fun Suggest(provider: (params: CommandExecutionScope, source: CommandSourceStack) -> List<String>) {
        this.suggestionProvider = { ctx, builder ->
            val scope = CommandExecutionScope(ctx)
            debugLog("Suggest requested for Argument Node '$name', input buffer remaining: '${builder.remaining}'")
            val candidates = try {
                provider(scope, ctx.source)
            } catch (e: Exception) {
                debugLog("Exception in Suggest provider lambda for '$name': ${e.message}")
                emptyList()
            }
            debugLog("Suggest candidates generated for '$name': $candidates")

            var addedCount = 0
            candidates.forEach { candidate ->
                if (candidate.startsWith(builder.remaining, ignoreCase = true)) {
                    builder.suggest(candidate)
                    addedCount++
                }
            }
            debugLog("Added $addedCount suggestions matching '${builder.remaining}' to builder for '$name'")
        }
    }

    fun Suggest(provider: () -> List<String>) {
        Suggest { _, _ -> provider() }
    }

    fun Attach(child: NodeBuilder?) {
        val node = requireNotNull(child) { "Attach 接收到的 NodeBuilder 为 null！" }
        children.add(node.buildLiteral())
    }

    fun Attach(child: ArgumentNodeBuilder<*>?) {
        val arg = requireNotNull(child) { "Attach 接收到的 ArgumentNodeBuilder 为 null！" }
        children.add(arg.buildRequired())
    }

    fun Use(child: NodeBuilder?, block: NodeBuilder.() -> Unit = {}) {
        val node = requireNotNull(child) { "Use 接收到的 NodeBuilder 为 null！" }
        val cloned = node.copy().apply(block)
        children.add(cloned.buildLiteral())
    }

    fun <U> Use(child: ArgumentNodeBuilder<U>?, block: ArgumentNodeBuilder<U>.() -> Unit = {}) {
        val arg = requireNotNull(child) { "Use 接收到的 ArgumentNodeBuilder 为 null！" }
        val cloned = arg.copy().apply(block)
        children.add(cloned.buildRequired())
    }

    fun Node(name: String, block: NodeBuilder.() -> Unit) {
        val child = NodeBuilder(name, permPrefix).apply(block)
        children.add(child.buildLiteral())
    }

    fun <U> Argument(
        name: String,
        type: ArgumentType<U>,
        block: ArgumentNodeBuilder<U>.() -> Unit
    ) {
        val child = ArgumentNodeBuilder(name, type, permPrefix).apply(block)
        children.add(child.buildRequired())
    }

    fun Execute(block: CommandExecutionScope.(params: CommandExecutionScope, source: CommandSourceStack) -> Unit) {
        this.executor = { ctx ->
            val scope = CommandExecutionScope(ctx)
            scope.block(scope, ctx.source)
        }
    }

    private fun registerPermissionIfAbsent(permName: String, default: PermissionDefault) {
        PermissionRegistry.defer(permName, default)
    }

    internal fun buildRequired(): RequiredArgumentBuilder<CommandSourceStack, T> {
        debugLog("Building Required Argument Node: '$name' (Children: ${children.size}, HasExecutor: ${executor != null}, HasRequirement: ${requirement != null}, HasSuggest: ${suggestionProvider != null})")
        val builder = RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

        requirement?.let { req ->
            builder.requires { stack ->
                val result = try {
                    req(stack)
                } catch (e: Exception) {
                    debugLog("Exception in requirement check for Argument Node '$name': ${e.message}")
                    false
                }
                debugLog("Requirement check for Argument Node '$name' on sender '${stack.sender.name}': $result")
                result
            }
        }

        suggestionProvider?.let { provider ->
            builder.suggests { ctx, suggestionsBuilder ->
                try {
                    provider(ctx, suggestionsBuilder)
                } catch (e: Exception) {
                    debugLog("Error processing suggests for Argument Node '$name': ${e.message}")
                }
                suggestionsBuilder.buildFuture()
            }
        }

        executor?.let { exec ->
            builder.executes { ctx ->
                debugLog("Executing Argument Node '$name' by '${ctx.source.sender.name}'")
                try {
                    exec(ctx)
                } catch (e: Exception) {
                    debugLog("Error executing Argument Node '$name': ${e.message}")
                    e.printStackTrace()
                }
                Command.SINGLE_SUCCESS
            }
        }

        children.forEach { child ->
            builder.then(child)
        }
        return builder
    }
}

/**
 * 命令执行/补全作用域，提供类型安全的参数提取
 */
@JvmInline
@BrigadierDsl
value class CommandExecutionScope(val context: CommandContext<CommandSourceStack>) {
    inline operator fun <reified T> get(name: String): T {
        return context.getArgument(name, T::class.java)
    }

    inline operator fun <reified T> invoke(name: String): T {
        return context.getArgument(name, T::class.java)
    }

    inline fun <reified T> getOrNull(name: String): T? {
        return try {
            context.getArgument(name, T::class.java)
        } catch (_: Exception) {
            null
        }
    }
}