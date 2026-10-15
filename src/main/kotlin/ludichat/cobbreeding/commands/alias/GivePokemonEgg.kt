package ludichat.cobbreeding.commands.alias

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ludichat.cobbreeding.commands.ICommand
import ludichat.cobbreeding.commands.main.eggCommands.EggGiveCommand
import net.minecraft.commands.CommandSourceStack

object GivePokemonEgg : ICommand {
    override val command: LiteralArgumentBuilder<CommandSourceStack>
        get() = EggGiveCommand::commandAlias.get()
}
