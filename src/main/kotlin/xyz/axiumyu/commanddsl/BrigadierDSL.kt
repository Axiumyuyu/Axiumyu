@file:Suppress("UnstableApiUsage", "Unused")
package xyz.axiumyu.commanddsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.permissions.PermissionDefault
import xyz.axiumyu.addPerm

/**
 * 标记 DSL 作用域，防止内层闭包误用外层作用域的隐式接收者，
 * 保证 `Node`/`Argument`/`Perm` 等成员函数只在正确的层级被解析到。
 */
@DslMarker
annotation class BrigadierDsl

// =====================================================================================
// 1. 节点蓝图基类
// =====================================================================================

/**
 * 所有节点蓝图的公共基类。
 *
 * 重要：本类及其子类（[LiteralNodeBuilder]、[ArgumentNodeBuilder]）**仅作为配置蓝图**，
 * 不持有任何 Brigadier 原生 `CommandNode` 实例（见需求 4.1）。真正的 Brigadier 树只在
 * [registerCommand] 触发的转换阶段才会被实例化（见需求 4.2）。
 */
@BrigadierDsl
sealed class BaseNode(val name: String, val permPrefix: String? = null) {

    /** 子节点列表（蓝图级别，非 Brigadier 节点）。 */
    val children: MutableList<BaseNode> = mutableListOf()

    /** `Require { ... }` 累积的谓词，最终与权限谓词一起以 && 组合（需求 5.3）。 */
    internal val requirements: MutableList<(CommandSourceStack) -> Boolean> = mutableListOf()

    /**
     * `Perm(name, default)` 累积的待解析权限条目：(覆盖名, 默认值)。
     * `name` 为 null 时代表不覆盖，直接使用节点自身的 [pathSegment]。
     */
    internal val permEntries: MutableList<Pair<String?, PermissionDefault>> = mutableListOf()

    /** `Execute { ... }` 设置的执行闭包，后设置者覆盖前者。 */
    internal var executeBlock: ((CommandParams, CommandSourceStack) -> Unit)? = null

    /** 本节点参与权限路径拼接的标识符：优先 permPrefix，否则用 name（需求 5.2）。 */
    internal val pathSegment: String get() = permPrefix ?: name

    /** 递归深拷贝，产出与原蓝图完全独立的新实例（需求 3.2 / 4.3）。 */
    abstract fun deepCopy(): BaseNode

    /** 子类在深拷贝时调用，复制公共状态与子节点。 */
    protected fun copyCommonInto(target: BaseNode) {
        target.requirements.addAll(this.requirements)
        target.permEntries.addAll(this.permEntries)
        target.executeBlock = this.executeBlock
        this.children.forEach { child -> target.children.add(child.deepCopy()) }
    }

    // ------------------------------------------------------------------
    // 节点创建：作为成员函数时，自动把新建节点挂载到 `this`（当前蓝图）之下。
    // 当在顶层（无隐式接收者）调用时，Kotlin 会解析到文件顶层的同名函数，
    // 那里只创建并返回蓝图，不做任何挂载（见 §3.4 顶层复用蓝图场景）。
    // ------------------------------------------------------------------

    /** 在当前节点下创建一个 literal 子节点（PascalCase：`Node(...)`，需求 3.1）。 */
    fun Node(
        name: String,
        permPrefix: String? = null,
        block: LiteralNodeBuilder.() -> Unit = {}
    ): LiteralNodeBuilder {
        val node = LiteralNodeBuilder(name, permPrefix)
        node.block()
        children.add(node)
        return node
    }

    /** 在当前节点下创建一个参数子节点（PascalCase：`Argument(...)`，需求 3.1）。 */
    fun <T : Any> Argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentNodeBuilder.() -> Unit = {}
    ): ArgumentNodeBuilder {
        val node = ArgumentNodeBuilder(name, type)
        node.block()
        children.add(node)
        return node
    }

    /**
     * 权限声明（需求 5.1）：
     * 1. 等价于 `Require { it.sender.hasPermission(完整权限字符串) }`；
     * 2. 完整权限字符串与默认值会在 [registerCommand] 转换阶段解析并暂存至 [PermissionRegistry]。
     *
     * `name` **覆盖当前节点自身在权限路径中的段名**，而不是在其后追加：
     * 完整权限 = 父节点权限路径 + "." + (name ?: 当前节点的 [pathSegment])。
     * 不传 `name` 时效果等价于直接对当前节点的路径声明权限；传入 `name` 时相当于
     * “借用”当前位置，声明一个路径最后一段不同的权限。
     *
     * ```
     * Node("testcmd") {
     *     Node("usethis") {
     *         Perm()          // testcmd.usethis
     *         Perm("over")    // testcmd.over
     *     }
     * }
     * ```
     *
     * 注意：完整权限路径依赖节点在最终树中的位置，而蓝图可能被多处复用，
     * 因此这里只记录“覆盖名 + 默认值”，真正的路径拼接推迟到树被固化之后（转换阶段）完成。
     */
    fun Perm(name: String? = null, default: PermissionDefault = PermissionDefault.OP) {
        permEntries.add(name to default)
    }

    /** 追加一个自定义可见性谓词，多次调用以 && 组合（需求 5.3）。 */
    fun Require(predicate: (CommandSourceStack) -> Boolean) {
        requirements.add(predicate)
    }

    /** 设置执行逻辑；闭包内异常直接抛出，返回值固定为成功（需求 7.2）。 */
    fun Execute(block: (CommandParams, CommandSourceStack) -> Unit) {
        executeBlock = block
    }

    /**
     * 一元 `+` 运算符：将已定义节点变量深拷贝后挂载到当前节点（需求 3.2）。
     * 定义为 `BaseNode` 的成员扩展函数，只有存在隐式接收者（即位于某个 Node/Argument 闭包内）时才可用。
     */
    operator fun BaseNode.unaryPlus() {
        this@BaseNode.children.add(this.deepCopy())
    }

    /**
     * `Use(blueprint) { ... }`：深拷贝蓝图并挂载到当前节点，随后对拷贝后子树的
     * 所有叶子节点批量应用闭包中的追加/覆盖操作（需求 3.3，核心规则）。
     */
    fun Use(blueprint: BaseNode, block: LeafGroupScope.() -> Unit = {}) {
        val copy = blueprint.deepCopy()
        children.add(copy)
        val leaves = findLeaves(copy)
        LeafGroupScope(leaves).block()
    }
}

/** literal 类型节点蓝图，对应 Brigadier `LiteralArgumentBuilder`。 */
@BrigadierDsl
class LiteralNodeBuilder(name: String, permPrefix: String? = null) : BaseNode(name, permPrefix) {
    override fun deepCopy(): LiteralNodeBuilder {
        val copy = LiteralNodeBuilder(name, permPrefix)
        copyCommonInto(copy)
        return copy
    }
}

/** 参数类型节点蓝图，对应 Brigadier `RequiredArgumentBuilder`。 */
@BrigadierDsl
class ArgumentNodeBuilder(
    name: String,
    val type: ArgumentType<*>,
    permPrefix: String? = null
) : BaseNode(name, permPrefix) {

    /** TAB 补全闭包：`(CommandParams, CommandSourceStack) -> List<String>`（需求 7.1）。 */
    internal var suggestBlock: ((CommandParams, CommandSourceStack) -> List<String>)? = null

    fun Suggest(block: (CommandParams, CommandSourceStack) -> List<String>) {
        suggestBlock = block
    }

    fun Suggest(block: () -> List<String>) {
        Suggest { _, _ -> block() }
    }

    override fun deepCopy(): ArgumentNodeBuilder {
        val copy = ArgumentNodeBuilder(name, type, permPrefix)
        copyCommonInto(copy)
        copy.suggestBlock = this.suggestBlock
        return copy
    }
}

// =====================================================================================
// 2. 顶层 DSL 入口函数（用于蓝图的“无接收者”定义场景，例如 `val x = Node("x") { ... }`）
// =====================================================================================

/**
 * 顶层版本的 `Node`：仅创建并返回蓝图，不挂载到任何父节点。
 * 当该调用出现在某个 Node/Argument 闭包内部时，Kotlin 会优先解析到 [BaseNode.Node] 成员函数，
 * 从而自动完成挂载；只有在没有隐式接收者的顶层作用域，才会落到这里。
 */
fun Node(
    name: String,
    permPrefix: String? = null,
    block: LiteralNodeBuilder.() -> Unit = {}
): LiteralNodeBuilder {
    val node = LiteralNodeBuilder(name, permPrefix)
    node.block()
    return node
}

/** 顶层版本的 `Argument`，语义同上。 */
fun <T : Any> Argument(
    name: String,
    type: ArgumentType<T>,
    block: ArgumentNodeBuilder.() -> Unit = {}
): ArgumentNodeBuilder {
    val node = ArgumentNodeBuilder(name, type)
    node.block()
    return node
}

// =====================================================================================
// 3. Use 闭包的“多叶子”作用域
// =====================================================================================

/**
 * `Use(blueprint) { ... }` 闭包的接收者。
 *
 * 闭包内的操作会同时施加于拷贝后子树的**所有叶子节点**（需求 3.3）：
 * - 追加类操作（`Node`/`Argument`/`+`/`Use`）→ 每个叶子各自获得一份独立深拷贝的新子节点；
 * - 覆盖类操作（`Perm`/`Require`/`Execute`）→ 同时作用于每一个叶子节点自身的配置。
 *
 * 两类操作互不干扰，可在同一个闭包内混用（需求 3.3 末尾说明）。
 */
@BrigadierDsl
class LeafGroupScope(private val leaves: List<BaseNode>) {

    fun Node(
        name: String,
        permPrefix: String? = null,
        block: LiteralNodeBuilder.() -> Unit = {}
    ): LiteralNodeBuilder {
        // 先构建一份“模板”，再为每个叶子生成独立深拷贝，避免多个叶子共享同一节点实例。
        val template = LiteralNodeBuilder(name, permPrefix)
        template.block()
        leaves.forEach { leaf -> leaf.children.add(template.deepCopy()) }
        return template
    }

    fun <T : Any> Argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentNodeBuilder.() -> Unit = {}
    ): ArgumentNodeBuilder {
        val template = ArgumentNodeBuilder(name, type)
        template.block()
        leaves.forEach { leaf -> leaf.children.add(template.deepCopy()) }
        return template
    }

    /** 覆盖操作：同时作用于每一个叶子节点的权限队列（语义同 [BaseNode.Perm]：覆盖节点自身段名而非追加）。 */
    fun Perm(name: String? = null, default: PermissionDefault = PermissionDefault.OP) {
        leaves.forEach { it.permEntries.add(name to default) }
    }

    /** 覆盖操作：同时作用于每一个叶子节点的 requirement 列表。 */
    fun Require(predicate: (CommandSourceStack) -> Boolean) {
        leaves.forEach { it.requirements.add(predicate) }
    }

    /** 覆盖操作：同时覆盖每一个叶子节点的执行闭包。 */
    fun Execute(block: (CommandParams, CommandSourceStack) -> Unit) {
        leaves.forEach { it.executeBlock = block }
    }

    /** 追加操作：为每一个叶子挂载一份独立深拷贝。 */
    operator fun BaseNode.unaryPlus() {
        val original = this
        this@LeafGroupScope.leaves.forEach { leaf -> leaf.children.add(original.deepCopy()) }
    }

    /** 追加操作：支持无限级嵌套 `Use`（需求 4.3）。每个叶子各自拥有独立的拷贝与内层叶子集合。 */
    fun Use(blueprint: BaseNode, block: LeafGroupScope.() -> Unit = {}) {
        leaves.forEach { leaf ->
            val copy = blueprint.deepCopy()
            leaf.children.add(copy)
            val innerLeaves = findLeaves(copy)
            LeafGroupScope(innerLeaves).block()
        }
    }
}

/** 遍历子树，找出所有没有子 Node/Argument 的末端节点（需求 3.3 步骤 2）。 */
private fun findLeaves(node: BaseNode): List<BaseNode> {
    return if (node.children.isEmpty()) {
        listOf(node)
    } else {
        node.children.flatMap { findLeaves(it) }
    }
}

// =====================================================================================
// 4. 参数获取包装类
// =====================================================================================

/**
 * 统一的参数获取包装类（需求 6）。
 * `params["key"]` 与 `params.get<T>("key")` 均等价于 `context.getArgument(key, T::class.java)`。
 */
@BrigadierDsl
class CommandParams(val context: CommandContext<CommandSourceStack>) {
    inline operator fun <reified T> get(key: String): T =
        context.getArgument(key, T::class.java)
}

// =====================================================================================
// 5. 权限注册中心
// =====================================================================================

/**
 * 权限队列。`Perm()` 在树转换阶段将完整权限字符串暂存于此，
 * 必须在 `onEnable()` 中显式调用 [registerAll] 才会真正写入 `PluginManager`（需求 5.1 / §8）。
 */
object PermissionRegistry {

    private val queue: MutableList<Pair<String, PermissionDefault>> = mutableListOf()

    /** 由框架内部在树转换阶段调用，将完整权限字符串暂存入队列。 */
    @Synchronized
    fun queue(permission: String, default: PermissionDefault) {
        queue.add(permission to default)
    }

    /** 必须在 `onEnable()` 中调用：一次性将队列中权限刷入 Bukkit `PluginManager`。 */
    @Synchronized
    fun registerAll() {
        val pluginManager = Bukkit.getPluginManager()
        queue.distinctBy { it.first }.forEach { (permission, default) ->
            pluginManager.addPerm(permission, default)
        }
        queue.clear()
    }
}

// =====================================================================================
// 6. 蓝图 -> Brigadier 原生树 的转换逻辑
// =====================================================================================

/**
 * 递归将蓝图节点转换为 Brigadier 原生 `ArgumentBuilder`。
 *
 * - 在此阶段才会解析权限路径（需求 5.2）：路径 = 祖先链上每个节点 pathSegment 以 "." 连接。
 * - 同时把解析出的完整权限字符串连同默认值送入 [PermissionRegistry] 排队。
 * - `Perm` 与 `Require` 的所有谓词以 && 组合成单一 requirement（需求 5.3）。
 * - TAB 补全按 remaining 做不区分大小写前缀过滤，直接填充原生 builder（需求 7.1）。
 * - 执行闭包异常直接抛出，返回值固定为成功（需求 7.2）。
 */
internal fun buildBrigadier(
    node: BaseNode,
    parentPath: String?
): ArgumentBuilder<CommandSourceStack, *> {

    val currentPath = if (parentPath == null) node.pathSegment else "$parentPath.${node.pathSegment}"

    // 解析权限队列：完整权限 = 父路径 + "." + (覆盖名 ?: 节点自身段名)，即覆盖而非追加（需求变更）。
    val permPredicates: List<(CommandSourceStack) -> Boolean> = node.permEntries.map { (overrideName, default) ->
        val segment = overrideName ?: node.pathSegment
        val fullPermission = if (parentPath == null) segment else "$parentPath.$segment"
        PermissionRegistry.queue(fullPermission, default)
        val predicate: (CommandSourceStack) -> Boolean = { src -> src.sender.hasPermission(fullPermission) }
        predicate
    }

    val allPredicates: List<(CommandSourceStack) -> Boolean> = node.requirements + permPredicates
    val combinedRequirement: (CommandSourceStack) -> Boolean =
        if (allPredicates.isEmpty()) {
            { true }
        } else {
            { src -> allPredicates.all { predicate -> predicate(src) } }
        }

    val builder: ArgumentBuilder<CommandSourceStack, *> = when (node) {
        is LiteralNodeBuilder -> {
            val literalBuilder = Commands.literal(node.name)
            literalBuilder.requires { src -> combinedRequirement(src) }
            node.executeBlock?.let { exec -> literalBuilder.executes(buildCommand(exec)) }
            node.children.forEach { child -> literalBuilder.then(buildBrigadier(child, currentPath)) }
            literalBuilder
        }
        is ArgumentNodeBuilder -> {
            @Suppress("UNCHECKED_CAST")
            val argumentType = node.type as ArgumentType<Any>
            val argumentBuilder = Commands.argument(node.name, argumentType)
            node.suggestBlock?.let { suggest -> argumentBuilder.suggests(buildSuggestionProvider(suggest)) }
            argumentBuilder.requires { src -> combinedRequirement(src) }
            node.executeBlock?.let { exec -> argumentBuilder.executes(buildCommand(exec)) }
            node.children.forEach { child -> argumentBuilder.then(buildBrigadier(child, currentPath)) }
            argumentBuilder
        }
    }

    return builder
}

/** 包装执行闭包：异常直接抛出，成功固定返回 [Command.SINGLE_SUCCESS]（需求 7.2）。 */
private fun buildCommand(block: (CommandParams, CommandSourceStack) -> Unit): Command<CommandSourceStack> {
    return Command { context ->
        val params = CommandParams(context)
        block(params, context.source)
        Command.SINGLE_SUCCESS
    }
}

/**
 * 包装补全闭包：对 `suggestionsBuilder.remaining` 做不区分大小写前缀匹配过滤，
 * 结果直接充填至原生 `suggestionsBuilder`（需求 7.1）。异常不做额外处理，直接抛出。
 */
private fun buildSuggestionProvider(
    block: (CommandParams, CommandSourceStack) -> List<String>
): SuggestionProvider<CommandSourceStack> {
    return SuggestionProvider { context, suggestionsBuilder ->
        val params = CommandParams(context)
        val candidates = block(params, context.source)
        val remaining = suggestionsBuilder.remaining.lowercase()
        candidates
            .filter { candidate -> candidate.lowercase().startsWith(remaining) }
            .forEach { candidate -> suggestionsBuilder.suggest(candidate) }
        suggestionsBuilder.buildFuture()
    }
}
