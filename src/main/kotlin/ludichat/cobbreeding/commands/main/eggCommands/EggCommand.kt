package ludichat.cobbreeding.commands.main.eggCommands

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object EggCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = Commands.literal("egg")
            .permission(
                CobbreedingPermission(
                    "command.cobbreeding.egg",
                    PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS
                )
            )
            .then(EggDecryptCommand.command)
            .then(EggGiveCommand.command)
            .then(EggHatchCommand.command)
}
