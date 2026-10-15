package ludichat.cobbreeding.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

interface ICommand {
    val command: LiteralArgumentBuilder<CommandSourceStack>

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(command)
    }
}
