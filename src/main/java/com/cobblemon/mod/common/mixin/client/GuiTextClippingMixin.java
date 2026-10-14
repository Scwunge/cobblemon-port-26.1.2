/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.TextClipping;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * This mixin facilitates the gradual reveal of text with NPC dialogue. If there is a better way
 * to do this, I'd love to hear it.
 *
 * The idea is that somewhere static in the mod we have a control for how many characters can be drawn
 * and how many have been drawn so far, and this mixin will add to the latter and stop drawing characters
 * once it reaches the former.
 *
 * This relies STRONGLY on the code that sets the character cap to reset it to -1 after the text is rendered,
 * otherwise all text in Minecraft will stop drawing.
 *
 * Sites that take advantage of this mixin will do the following:
 * 1: Set the character cap, which resets the drawn counter to zero.
 * 2: Call whatever Minecraft function draws your text.
 * 3: Reset the character cap to -1, which will allow regular text to draw again.
 *
 * @author Hiroku
 * @since April 18th, 2025
 */
@Mixin(GuiTextRenderState.class)
public class GuiTextClippingMixin {
    /*
     * Text drawn in GUIs is laid out after the GUI is extracted since 1.21.6, long after the character cap is reset, so
     * Font.StringRenderOutput#accept can no longer see it. The characters are counted against the cap when the text is
     * submitted instead, and the text keeps only those.
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static FormattedCharSequence cobblemon$clipText(FormattedCharSequence text) {
        if (TextClipping.isUnlimited()) {
            return text;
        }
        int[] allowed = { 0 };
        text.accept((position, style, codepoint) -> {
            if (!TextClipping.canDrawAnotherCharacter()) return false;
            allowed[0]++;
            return true;
        });
        int limit = allowed[0];
        return sink -> {
            int[] drawn = { 0 };
            return text.accept((position, style, codepoint) -> drawn[0]++ < limit ? sink.accept(position, style, codepoint) : true);
        };
    }
}
