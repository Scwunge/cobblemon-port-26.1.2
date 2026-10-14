/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;


import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.api.ai.config.task.PathToBeeHiveTaskConfig;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.world.feature.SaccharineTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mixin(BeehiveBlockEntity.Occupant.class)
public abstract class BeeOccupantMixin {

    /*
     * Pokémon stuffed into a bee nest upon world gen are a placeholder until they leave the hive: for technical reasons
     * we cannot create them as full entities during world gen. Occupants carry a real entity type since 1.21.9, so the
     * placeholder is Pokémon entity data marked with SaccharineTreeFeature.POKEMON_PLACEHOLD_ID (it used to be a fake
     * "id"), which vanilla would load as a default Pokémon; it is completed here first instead.
     */
    @Inject(method = "createEntity", at = @At("HEAD"), cancellable = true)
    private void cobblemon$createPlaceholderPokemon(Level level, BlockPos pos, CallbackInfoReturnable<Entity> cir) {
        final BeehiveBlockEntity.Occupant occupant = (BeehiveBlockEntity.Occupant) (Object) this;
        if (!occupant.entityData().contains(SaccharineTreeFeature.Companion.getPOKEMON_PLACEHOLD_ID())) return;
        CompoundTag compoundTag = occupant.entityData().copyTagWithoutId();
        Entity entity = null;
        if (compoundTag.contains("pokemonProperties")) {
            String properties = compoundTag.getStringOr("pokemonProperties", "");
            PokemonProperties pokemon = PokemonProperties.Companion.parse(properties);
            entity = pokemon.createEntity(level);
        }
        cir.setReturnValue(cobblemon$releasePokemon(level, pos, entity) ? entity : null);
    }

    @Inject(method = "createEntity", at = @At("RETURN"), cancellable = true)
    private void cobblemon$createEntity(Level level, BlockPos pos, CallbackInfoReturnable<Entity> cir) {
        // Needing to replicate all the post op stuff on the hive isn't great
        if (cir.getReturnValue() == null) {
            final BeehiveBlockEntity.Occupant occupant = (BeehiveBlockEntity.Occupant) (Object) this;
            Entity entity = EntityType.loadEntityRecursive(occupant.entityData().type(), occupant.entityData().copyTagWithoutId(), level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
            if (cobblemon$releasePokemon(level, pos, entity)) {
                cir.setReturnValue(entity);
            }
        }
    }

    /** Sets a Pokémon leaving the hive up; returns whether [entity] was a Pokémon. */
    @Unique
    private static boolean cobblemon$releasePokemon(Level level, BlockPos pos, Entity entity) {
        {
            if (entity instanceof PokemonEntity pokemonEntity) {
                var state = level.getBlockState(pos);
                var newPos = Vec3.ZERO;
                var newYaw = 0f;
                var brain = pokemonEntity.getBrain();
                if (state.is(BlockTags.BEEHIVES)) {
                    // position in front of the entrance, face away from the block
                    var facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    newPos = pos.relative(facing).getCenter();
                    newYaw = facing.toYRot();
                    if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                        brain.setMemory(CobblemonMemories.HIVE_LOCATION, pos); // This needs to be set in the case that the hive was picked up and moved.
                    }
                } else {
                    // Block has likely been destroyed
                    var center = pos.getCenter();
                    newPos = new Vec3(
                            center.x + level.getRandom().nextFloat() * 0.6 - 0.3,
                            center.y + level.getRandom().nextFloat() * 0.6 - 0.3,
                            center.z + level.getRandom().nextFloat() * 0.6 - 0.3
                    );
                    newYaw = level.getRandom().nextFloat() * 360F;
                    if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                        brain.eraseMemory(CobblemonMemories.HIVE_LOCATION);
                    }
                }
                entity.yRotO = newYaw;
                entity.setPos(newPos);
                // Do honey logic
                if (brain.checkMemory(CobblemonMemories.HAS_NECTAR, MemoryStatus.REGISTERED)) {
                    var hasNectar = brain.getMemory(CobblemonMemories.HAS_NECTAR).orElse(false);
                    if (hasNectar) {
                        // Remove nectar and reset got to hive cooldown
                        brain.eraseMemory(CobblemonMemories.HAS_NECTAR);
                        if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                            brain.setMemoryWithExpiry(CobblemonMemories.HIVE_COOLDOWN, true, PathToBeeHiveTaskConfig.STAY_OUT_OF_HIVE_COOLDOWN);
                        }
                        // Increment honey level of the hive
                        if (state.is(BlockTags.BEEHIVES, (blockStateBase) -> blockStateBase.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                            int i = state.getValue(BeehiveBlock.HONEY_LEVEL);
                            if (i < 5) {
                                int j = level.getRandom().nextInt(100) == 0 ? 2 : 1;
                                if (i + j > 5) {
                                    --j;
                                }
                                level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, i + j));
                            }
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void cobblemon$of(Entity entity, CallbackInfoReturnable<BeehiveBlockEntity.Occupant> cir) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            // The same tags as before, by their current names (equipment and drop chances were merged in 1.21.5)
            List<String> IGNORED_BEE_TAGS = Arrays.asList("Air", "drop_chances", "equipment", "CanPickUpLoot", "DeathTime", "fall_distance", "FallFlying", "Fire", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "Passengers", "leash", "UUID");
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
            entity.save(output);
            CompoundTag compoundTag = output.buildResult();
            Objects.requireNonNull(compoundTag);
            IGNORED_BEE_TAGS.forEach(compoundTag::remove);
            boolean hasNectar = pokemonEntity.getBrain().checkMemory(CobblemonMemories.HAS_NECTAR, MemoryStatus.REGISTERED)
                    && pokemonEntity.getBrain().getMemory(CobblemonMemories.HAS_NECTAR).orElse(false);
            cir.setReturnValue(new BeehiveBlockEntity.Occupant(TypedEntityData.of(entity.getType(), compoundTag), 0, hasNectar ? 2400 : 600));
            cir.cancel();
        }

    }
}