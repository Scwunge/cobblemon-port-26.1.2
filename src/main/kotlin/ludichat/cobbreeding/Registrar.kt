/*
 * Copyright (c) 2023 Ludichat, Fuzuki
 *
 * Part of Cobbreeding, ported to Minecraft 26.1.2 and merged into this Cobblemon port; see
 * licenses/cobbreeding/LICENSE (MIT).
 */

package ludichat.cobbreeding

import com.cobblemon.mod.common.platform.PendingRegistration
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

/** Registers one Cobbreeding entry straight into a vanilla registry; replaces Architectury's DeferredRegister. */
class Registrar<T : Any>(private val registry: Registry<T>, private val registryKey: ResourceKey<out Registry<T>>) {
    /** A registered entry, readable with [get] like the Architectury RegistrySupplier it replaces. */
    class Entry<T>(val value: T) {
        fun get(): T = value
    }

    fun <E : T> register(name: String, factory: () -> E): Entry<E> {
        val id = Identifier.fromNamespaceAndPath(Cobbreeding.MOD_ID, name)
        val value = PendingRegistration.with(ResourceKey.create(registryKey, id), factory)
        Registry.register(registry, id, value)
        return Entry(value)
    }
}
