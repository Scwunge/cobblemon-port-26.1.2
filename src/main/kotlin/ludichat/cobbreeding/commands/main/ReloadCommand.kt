package ludichat.cobbreeding.commands.main

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import ludichat.cobbreeding.Cobbreeding
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object ReloadCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = Commands.literal("reload")
            .permission(CobbreedingPermission("command.cobbreeding.reload", PermissionLevel.ALL_COMMANDS))
            .executes(::runReload)

    private fun runReload(context: CommandContext<CommandSourceStack>): Int {
        try {
            Cobbreeding.reloadConfig()

            context.source.sendSuccess(
                { Component.translatable("cobbreeding.msg.command.reload") },
                true
            )
        } catch (e: CommandSyntaxException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw SimpleCommandExceptionType(Component.translatable("cobbreeding.msg.command.error")).create()
        }

        return Command.SINGLE_SUCCESS
    }
}
