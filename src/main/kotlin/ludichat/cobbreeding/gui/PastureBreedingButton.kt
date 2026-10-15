package ludichat.cobbreeding.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonSounds.PC_CLICK
import ludichat.cobbreeding.CustomProperties
import ludichat.cobbreeding.network.ToggleBreedingPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState


class PastureBreedingButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    protected val blockPos: BlockPos
) : AbstractWidget(x, y, width, height, Component.empty()) {
    protected var texture: Identifier = Identifier.fromNamespaceAndPath("cobbreeding", "textures/gui/breeding_button.png")
    protected var cross: Identifier = Identifier.fromNamespaceAndPath("cobbreeding", "textures/gui/cross.png")
    protected var state: BlockState

    init {
        state = Minecraft.getInstance().level!!.getBlockState(blockPos)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        Cobblemon.implementation.networkManager.sendToServer(ToggleBreedingPacket())
    }

    override fun setTooltip(tooltip: Tooltip?) {
        super.setTooltip(Tooltip.create(Component.translatable("cobbreeding.msg.pasture_button.tooltip")))
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.blit(
            texture,
            x, y,
            width, height,
            0f, 0f,
            width, height,
            width, height
        )
        state = Minecraft.getInstance().level!!.getBlockState(blockPos)
        if (!state.getValue(CustomProperties.BREEDING_ACTIVATED)) {
            guiGraphics.blit(
                cross,
                x+3, y+3,
                14, 14,
                0f, 0f,
                14, 14,
                14, 14
            )
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        val isOn = if (state.getValue(CustomProperties.BREEDING_ACTIVATED)) "ON" else "OFF"
        narrationElementOutput.add(NarratedElementType.TITLE, "Breeding $isOn")
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(PC_CLICK, 1.0f))
    }
}