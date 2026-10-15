package ludichat.cobbreeding.commands.main.eggCommands

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.util.permission
import com.cobblemon.mod.common.util.player
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import ludichat.cobbreeding.EggUtilities
import ludichat.cobbreeding.PokemonEgg
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import ludichat.cobbreeding.components.CobbreedingComponents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand

object EggDecryptCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = literal<CommandSourceStack>("decrypt")
            .permission(
                CobbreedingPermission(
                    "command.cobbreeding.egg.decrypt",
                    PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS
                )
            ).then(argument("player", EntityArgument.player()).executes(::runEggDecrypt))

    private fun runEggDecrypt(context: CommandContext<CommandSourceStack>): Int {
        try {
            val player = context.player()

            val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
            if (stack.item !is PokemonEgg) {
                throw SimpleCommandExceptionType(Component.translatable("cobbreeding.msg.command.error.no_egg")).create()
            }

            val props = EggUtilities.extractProperties(stack)?.asString() ?: ""
            stack[CobbreedingComponents.POKEMON_PROPERTIES.get()] = props

            context.source.sendSystemMessage(Component.literal(props))
            context.source.sendSuccess(
                { Component.translatable("cobbreeding.msg.command.decrypt.success", player.displayName) },
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
