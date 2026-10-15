package ludichat.cobbreeding.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import ludichat.cobbreeding.NeuterProperty
import ludichat.cobbreeding.network.ToggleNeuterPacket
import com.cobblemon.mod.common.api.gui.blitk
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents

/**
* Button to neuter or un-neuter the Pokémon.
* Only the Original Trainer can toggle it.
*/
class ToggleNeuterButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    var pokemon: Pokemon
) : AbstractWidget(x, y, width, height, Component.empty()) {
    protected var texture: Identifier = Identifier.fromNamespaceAndPath("cobbreeding", "textures/gui/neuter_button.png")
    protected var cross: Identifier = Identifier.fromNamespaceAndPath("cobbreeding", "textures/gui/cross.png")

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        Cobblemon.implementation.networkManager.sendToServer(ToggleNeuterPacket(pokemon.uuid))
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        blitk(matrixStack = guiGraphics.pose(), texture = texture, x = x, y = y, width = width, height = height)
        if (NeuterProperty.get(pokemon)) {
            blitk(matrixStack = guiGraphics.pose(), texture = cross, x = x + 3, y = y + 3, width = 14, height = 14)
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        val isOn = if (!NeuterProperty.get(pokemon)) "ON" else "OFF"
        narrationElementOutput.add(NarratedElementType.TITLE, "Breeding $isOn")
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
    }

}