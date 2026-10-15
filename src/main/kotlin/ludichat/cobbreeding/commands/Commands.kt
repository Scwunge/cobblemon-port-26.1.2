package ludichat.cobbreeding.commands

import com.mojang.brigadier.CommandDispatcher
import ludichat.cobbreeding.Cobbreeding.LOGGER
import ludichat.cobbreeding.commands.alias.GivePokemonEgg
import ludichat.cobbreeding.commands.alias.HatchPokemonEgg
import ludichat.cobbreeding.commands.main.CobbreedingCommand
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object Commands {
    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        registry: CommandBuildContext,
        selection: Commands.CommandSelection
    ) {
        val commands = listOf(
            CobbreedingCommand,
            GivePokemonEgg,
            HatchPokemonEgg,
        )

        LOGGER.info("Registering ${commands.size} commands")
        commands.forEach { it.register(dispatcher) }
        LOGGER.info("Commands registered")
    }
}
