/**
 * Copied from Cobblemon's com.cobblemon.mod.common.api.permission.CobblemonPermission class
 */

package ludichat.cobbreeding.commands

import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionLevel
import ludichat.cobbreeding.Cobbreeding
import net.minecraft.resources.Identifier

data class CobbreedingPermission(
    private val node: String,
    override val level: PermissionLevel
): Permission {
    override val identifier: Identifier = Identifier.fromNamespaceAndPath(Cobbreeding.MOD_ID, node)
    override val literal = "${Cobbreeding.MOD_ID}.$node"
}
