package ludichat.cobbreeding.commands.main

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import ludichat.cobbreeding.commands.main.eggCommands.EggCommand
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object CobbreedingCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = Commands.literal("cobbreeding")
            .permission(CobbreedingPermission("command.cobbreeding", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS))
            .then(InfoCommand.command)
            .then(ReloadCommand.command)
            .then(EggCommand.command)
}
