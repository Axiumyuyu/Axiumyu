package xyz.axiumyu.dialogdsl.test

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import xyz.axiumyu.dialogdsl.route.DialogRouter

fun node(name: String) = Commands.literal(name)

// 2. 构建 Brigadier 命令 /testdialog
/**
 * 构建时使用
 * ```
 * ./gradlew build -PaddTest
 * ```
 * 将 `test` 文件夹内的代码参与编译，并在 `PaperDialogDSLBootStrap` 里面注册，然后在游戏内输入该命令来测试
 */
val testCmd: LiteralArgumentBuilder<CommandSourceStack> = node("testdialog")
    .requires { source -> source.sender is Player && source.sender.isOp } // 仅限 OP 玩家使用
    .executes { ctx ->
        val player = ctx.source.sender as Player
        // 调用我们刚才写的全局路由引擎，直接推入根栈
        DialogRouter.openRoot(player, HomeRoute)
        1 // 返回执行成功状态码
    }
