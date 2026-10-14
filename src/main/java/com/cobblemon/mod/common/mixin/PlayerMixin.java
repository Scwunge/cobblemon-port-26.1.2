/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import com.cobblemon.mod.common.entity.player.ShoulderData;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.util.NbtCompatKt;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.world.entity.Avatar;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.item.LeftoversCreatedEvent;
import com.cobblemon.mod.common.api.orientation.OrientationController;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.riding.RidingStyle;
import com.cobblemon.mod.common.api.riding.behaviour.types.air.HoverBehaviour;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.duck.PlayerDuck;
import com.cobblemon.mod.common.duck.RidePassenger;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.CompoundTagExtensionsKt;
import com.cobblemon.mod.common.util.CompoundTagUtilities;
import com.cobblemon.mod.common.util.DataKeys;
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(Player.class)
public abstract class PlayerMixin extends Avatar implements ScannableEntity, OrientationControllable, PlayerDuck, RidePassenger {

    @Unique
    private Vector3f cobblemon$driverInput;
    @Unique
    private Vector3f cobblemon$lastSentDriverInput;

    @Shadow public abstract boolean isSpectator();

    @Shadow public abstract boolean addItem(ItemStack stack);

    @Shadow protected int jumpTriggerTime;
    @Unique private final OrientationController cobblemon$orientationController = new OrientationController(this);

    @Unique private float cobblemon$rideXRot = 0.0f;

    @Unique private float cobblemon$rideYRot = 0.0f;

    @Unique private Vec3 cobblemon$rideEyePos = Vec3.ZERO;

    protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.getVehicle() instanceof PokemonEntity) {
            return STANDING_DIMENSIONS;
        }
        return super.getDefaultDimensions(pose);
    }

    @Redirect(
            method = "getDestroySpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"
            )
    )
    private boolean cobblemon$considerHoverOnGround(Player player) {
        // If riding a Hover Mount, return true to bypass the 5x slowdown
        if (player.getVehicle() instanceof PokemonEntity vehicle) {

            var ridingController = vehicle.getRidingController();
            if (ridingController == null) player.onGround();

            var activeContext = ridingController.getContext();
            if (activeContext == null) player.onGround();

            if (activeContext.getStyle() == RidingStyle.AIR &&
                activeContext.getBehaviour().equals(HoverBehaviour.Companion.getKEY())) {
                return true;
            }
        }
        // Otherwise, return the actual value
        return player.onGround();
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    public void isInvulnerableTo(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this instanceof ServerPlayer player) {
            boolean invulnerableInBattle = level.getGameRules().get(CobblemonGameRules.BATTLE_INVULNERABILITY);
            boolean inBattle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
            if (invulnerableInBattle && inBattle) {
                ci.setReturnValue(true);
            }
        }
    }

    @Override @Nullable
    public PokedexEntityData resolvePokemonScan() {
        // Shoulder entities live on ServerPlayer since 1.21.9; both sides read the synced copy (see ShoulderData)
        CompoundTag right = PlayerExtensionsKt.getShoulderEntityRight((Player) (Object) this);
        CompoundTag left = PlayerExtensionsKt.getShoulderEntityLeft((Player) (Object) this);
        if (CompoundTagUtilities.isShoulderPokemon(right)){
            return getDataFromShoulderPokemon(right);
        }
        if (CompoundTagUtilities.isShoulderPokemon(left)){
            return getDataFromShoulderPokemon(left);
        }
        return null;
    }

    @Nullable @Unique
    private PokedexEntityData getDataFromShoulderPokemon(CompoundTag shoulderTag) {
        Pokemon pokemon;
        if (level().isClientSide()) {
            CompoundTag pokemonTag = shoulderTag.getCompoundOrEmpty(DataKeys.POKEMON);
            if (pokemonTag.isEmpty()) return null;
            Species species = PokemonSpecies.getByIdentifier(Identifier.parse(pokemonTag.getStringOr(DataKeys.POKEMON_SPECIES_IDENTIFIER, "")));
            if (species == null) return null;
            String formId = pokemonTag.getStringOr(DataKeys.POKEMON_FORM_ID, "");
            FormData form = species.getStandardForm();
            List<FormData> formList = species.getForms().stream().filter(it -> it.formOnlyShowdownId().equals(formId)).toList();
            if (!formList.isEmpty()) form = formList.getFirst();
            if (form == null) return null;
            String genderString = pokemonTag.getStringOr(DataKeys.POKEMON_GENDER, "");
            if (genderString.isEmpty()) return null;
            Gender gender = Gender.valueOf(genderString);
            boolean shiny = pokemonTag.getBooleanOr(DataKeys.POKEMON_SHINY, false);
            int level = pokemonTag.getIntOr(DataKeys.POKEMON_LEVEL, 0);
            Set<String> aspects = shoulderTag.getListOrEmpty(DataKeys.SHOULDER_ASPECTS).stream().map(tag -> tag.asString().orElse("")).collect(Collectors.toSet());

            pokemon = new Pokemon();
            pokemon.setSpecies(species);
            pokemon.setForm(form);
            pokemon.setGender(gender);
            pokemon.setShiny(shiny);
            pokemon.setLevel(level);
            pokemon.setForcedAspects(aspects);
        } else {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(this.getUUID(), this.registryAccess());
            pokemon = party.get(NbtCompatKt.getUUID(shoulderTag, DataKeys.SHOULDER_UUID));
        }
        return (pokemon == null) ? null : new PokedexEntityData(pokemon, null);
    }

    @Override
    public LivingEntity resolveEntityScan() {
        return this;
    }

    @Override
    public void absSnapTo(double x, double y, double z, float yaw, float pitch) {
        if (cobblemon$orientationController.getActive()) {
            this.absSnapTo(x, y, z);
            this.setYRot(yaw % 360.0f);
            this.setXRot(pitch % 360.0f);
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        else {
            this.absSnapTo(x, y, z);
            this.absSnapRotationTo(yaw, pitch);
        }
    }

    @WrapOperation(
            method = "rideTick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;wantsToStopRiding()Z")
    )
    public boolean delegateDismountToController(Player instance, Operation<Boolean> original) {
        if (this.getVehicle() instanceof PokemonEntity pokemonEntity) {
            return pokemonEntity.ifRidingAvailableSupply(false, (behaviour, settings, state) -> {
                return behaviour.dismountOnShift(settings, state, pokemonEntity) && original.call(instance);
            });
        }
        return original.call(instance);
    }

    @Override
    public OrientationController getOrientationController() {
        return cobblemon$orientationController;
    }

    @Override
    public void setDriverInput(Vector3f driverInput) {
        cobblemon$driverInput = driverInput;
    }

    @Override
    public Vector3f getDriverInput() {
        return cobblemon$driverInput;
    }

    @Override
    public void setLastSentDriverInput(Vector3f lastSentDriverInput) {
        cobblemon$lastSentDriverInput = lastSentDriverInput;
    }

    @Override
    public Vector3f getLastSentDriverInput() {
        return cobblemon$lastSentDriverInput;
    }

    @Override
    public float cobblemon$getRideXRot() {
        return this.cobblemon$rideXRot;
    }

    @Override
    public void cobblemon$setRideXRot(float rideXRot) {
        this.cobblemon$rideXRot = Mth.wrapDegrees(rideXRot);
    }

    @Override
    public float cobblemon$getRideYRot() {
        return this.cobblemon$rideYRot;
    }

    @Override
    public void cobblemon$setRideYRot(float rideYRot) {
        this.cobblemon$rideYRot = Mth.wrapDegrees(rideYRot);
    }

    @Override
    public Vec3 cobblemon$getRideEyePos() {
        return this.cobblemon$rideEyePos;
    }

    @Override
    public void cobblemon$setRideEyePos(Vec3 rideEyePos) {
        this.cobblemon$rideEyePos = rideEyePos;
    }

    /**
     * (Sweeping moved to doSweepAttack in 1.21.11.)
     * Sweeping attacks apply post-attack enchantment effects (e.g. Fire Aspect) to every entity caught
     * in the sweep, even if the damage itself was blocked. Skip them for Pokémon immune to the attack.
     */
    @WrapOperation(
            method = "doSweepAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V")
    )
    private void cobblemon$skipPostAttackEffectsOnImmunePokemon(ServerLevel level, Entity target, DamageSource source, Operation<Void> original) {
        if (target instanceof PokemonEntity pokemonEntity && pokemonEntity.isInvulnerableTo(level, source)) {
            return;
        }
        original.call(level, target, source);
    }

    /**
     * Sweeping attacks also knock back every entity caught in the sweep regardless of whether it took damage.
     */
    @WrapOperation(
            method = "doSweepAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V")
    )
    private void cobblemon$skipKnockbackOnImmunePokemon(LivingEntity target, double strength, double x, double z, Operation<Void> original, @Local(argsOnly = true) DamageSource source) {
        if (target instanceof PokemonEntity pokemonEntity && pokemonEntity.level() instanceof ServerLevel level && pokemonEntity.isInvulnerableTo(level, source)) {
            return;
        }
        original.call(target, strength, x, z);
    }

    /** See {@link ShoulderData}: define the synced shoulder data right after Player's own entity data. */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void cobblemon$defineShoulderDataAccessors(CallbackInfo ci) {
        ShoulderData.init();
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void cobblemon$defineShoulderData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(ShoulderData.LEFT, new CompoundTag());
        builder.define(ShoulderData.RIGHT, new CompoundTag());
    }
}
