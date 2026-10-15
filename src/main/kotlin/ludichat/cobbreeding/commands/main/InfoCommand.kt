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

object InfoCommand: ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = Commands.literal("info")
            .permission(CobbreedingPermission("command.cobbreeding.info", PermissionLevel.MULTIPLAYER_MANAGEMENT))
            .executes(::runInfo)

    private fun runInfo(context: CommandContext<CommandSourceStack>): Int {
        try {
            context.source.sendSuccess(
                { Component.translatable("cobbreeding.msg.command.info", Cobbreeding.VERSION) },
                false
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