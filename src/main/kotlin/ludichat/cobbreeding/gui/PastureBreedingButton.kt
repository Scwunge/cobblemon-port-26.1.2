package ludichat.cobbreeding.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonSounds.PC_CLICK
import ludichat.cobbreeding.CustomProperties
import ludichat.cobbreeding.network.ToggleBreedingPacket
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.api.gui.blitk
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
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

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        Cobblemon.implementation.networkManager.sendToServer(ToggleBreedingPacket())
    }

    override fun setTooltip(tooltip: Tooltip?) {
        super.setTooltip(Tooltip.create(Component.translatable("cobbreeding.msg.pasture_button.tooltip")))
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        blitk(matrixStack = guiGraphics.pose(), texture = texture, x = x, y = y, width = width, height = height)
        state = Minecraft.getInstance().level!!.getBlockState(blockPos)
        if (!state.getOptionalValue(CustomProperties.BREEDING_ACTIVATED).orElse(false)) {
            blitk(matrixStack = guiGraphics.pose(), texture = cross, x = x + 3, y = y + 3, width = 14, height = 14)
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        val isOn = if (state.getOptionalValue(CustomProperties.BREEDING_ACTIVATED).orElse(false)) "ON" else "OFF"
        narrationElementOutput.add(NarratedElementType.TITLE, "Breeding $isOn")
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(PC_CLICK, 1.0f))
    }
}