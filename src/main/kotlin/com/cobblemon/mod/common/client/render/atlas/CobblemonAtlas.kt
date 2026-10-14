/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.atlas

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.resources.Identifier

/**
 * A sprite atlas of Cobblemon's, stitched from the atlas definition [atlasId] into the texture [textureId].
 *
 * Atlases are owned by the game's AtlasManager since 1.21.9; this was a TextureAtlasHolder before.
 */
class CobblemonAtlas(val textureId: Identifier, val atlasId: Identifier) {
    val config = AtlasManager.AtlasConfig(textureId, atlasId, false)

    val textureAtlas: TextureAtlas
        get() = Minecraft.getInstance().atlasManager.getAtlasOrThrow(atlasId)

    val width: Int
        get() = textureAtlas.texture.getWidth(0)

    val height: Int
        get() = textureAtlas.texture.getHeight(0)

    fun getSprite(id: Identifier): TextureAtlasSprite = textureAtlas.getSprite(id)
}
