package ludichat.cobbreeding.commands.main.eggCommands

import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.cobblemon.mod.common.util.permission
import com.cobblemon.mod.common.util.player
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import ludichat.cobbreeding.PokemonEgg
import ludichat.cobbreeding.PokemonEgg.Companion.TIMER
import ludichat.cobbreeding.commands.CobbreedingPermission
import ludichat.cobbreeding.commands.ICommand
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand

object EggHatchCommand : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = alias(literal("hatch"))

    @Suppress("SpellCheckingInspection")
    internal val commandAlias: LiteralArgumentBuilder<CommandSourceStack>
        get() = alias(literal("hatchpokemonegg"))

    private fun alias(node: LiteralArgumentBuilder<CommandSourceStack>): LiteralArgumentBuilder<CommandSourceStack> {
        return node
            .permission(
                CobbreedingPermission(
                    "command.cobbreeding.egg.hatch",
                    PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS
                )
            )
            .then(argument("player", EntityArgument.player()).executes(::runEggHatch))
    }

    private fun runEggHatch(context: CommandContext<CommandSourceStack>): Int {
        try {
            val player = context.player()

            val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
            if (stack.item !is PokemonEgg) {
                throw SimpleCommandExceptionType(Component.translatable("cobbreeding.msg.command.error.no_egg")).create()
            }

            stack.set(TIMER, 0)
            context.source.sendSuccess(
                { Component.translatable("cobbreeding.msg.command.hatch.success", player.displayName) },
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
