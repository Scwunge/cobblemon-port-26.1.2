package ludichat.cobbreeding.commands.main.eggCommands

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType
import com.cobblemon.mod.common.util.permission
import com.cobblemon.mod.common.util.player
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandExceptionType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import ludichat.cobbreeding.EggUtilities
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

object EggGiveCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = alias(literal("give"))

    @Suppress("SpellCheckingInspection")
    internal val commandAlias: LiteralArgumentBuilder<CommandSourceStack>
        get() = alias(literal("givepokemonegg"))

    private fun alias(node: LiteralArgumentBuilder<CommandSourceStack>): LiteralArgumentBuilder<CommandSourceStack> {
        return node
            .permission(
                CobbreedingPermission(
                    "command.cobbreeding.egg.give",
                    PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS
                )
            )
            .then(
                argument("player", EntityArgument.player()).then(
                    argument(
                        "properties",
                        PokemonPropertiesArgumentType.properties()
                    ).executes(::runEggGive)
                )
            )
    }

    private fun runEggGive(context: CommandContext<CommandSourceStack>): Int {
        try {
            val player = context.player()
            val props = PokemonPropertiesArgumentType.getPokemonProperties(context, "properties")

            val stack = EggUtilities.getEggFromPokemonProperties(props)
                ?: throw SimpleCommandExceptionType(Component.translatable("cobbreeding.msg.command.give.no_species")).create()
            player.addItem(stack)

            context.source.sendSuccess(
                { Component.translatable("cobbreeding.msg.command.give.success", player.displayName) },
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
