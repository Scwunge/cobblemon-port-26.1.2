package ludichat.cobbreeding.commands.alias

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ludichat.cobbreeding.commands.ICommand
import ludichat.cobbreeding.commands.main.eggCommands.EggHatchCommand
import net.minecraft.commands.CommandSourceStack

object HatchPokemonEgg : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = EggHatchCommand::commandAlias.get()
}
