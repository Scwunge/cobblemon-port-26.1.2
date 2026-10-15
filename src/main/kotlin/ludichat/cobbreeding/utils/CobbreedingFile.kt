package ludichat.cobbreeding.utils

import ludichat.cobbreeding.Cobbreeding
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import java.io.File
import java.io.IOException
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


object CobbreedingFile {
    private fun logIOError(path: String, read: Boolean) {
        Cobbreeding.LOGGER.error("Couldn't ${if (read) "read" else "write"} in file $path")
    }

    /**
     * Add an entry into a NBT file
     */
    @JvmStatic
    fun addCompoundTag(file: File, key: String, posValue: IntArray?, uuidValue: UUID?)
    {
        var nbt = CompoundTag()
        try {
            // Getting existing content
            if (file.exists())
                nbt = NbtIo.read(file.toPath())!!

            // Adding the entry
            if (posValue != null && posValue.isNotEmpty())
                nbt.putIntArray(key, posValue)
            if (uuidValue != null)
                nbt.putIntArray(key, UUIDUtil.uuidToIntArray(uuidValue))

            // Writing with the new entry
            NbtIo.write(nbt, file.toPath())
        } catch (_: IOException)
        {
            logIOError(file.path, false)
        }
    }

    /**
     * Delete an entry from a NBT file
     */
    @JvmStatic
    fun removeCompoundTag(file: File, key: String)
    {
        var nbt = CompoundTag()
        try {
            // Getting existing content
            if (file.exists())
                nbt = NbtIo.read(file.toPath())!!

            // Removing the entry
            nbt.remove(key)

            // Writing with the new entry
            NbtIo.write(nbt, file.toPath())
        } catch (_: IOException)
        {
            logIOError(file.path, false)
        }
    }

    @JvmStatic
    fun getUuid(file: File, key: String): UUID?
    {
        var nbt = CompoundTag()
        try {
            // Getting existing content
            if (file.exists())
                nbt = NbtIo.read(file.toPath())!!

            return UUIDUtil.uuidFromIntArray(nbt.getIntArray(key).orElseThrow())

        } catch (_: IOException)
        {
            logIOError(file.path, true)
        }
        catch (_: NullPointerException)
        {
            // mute
        }
        return null
    }

    @JvmStatic
    fun getNumberOfActivatedPastures(file: File): Int
    {
        var nbt = CompoundTag()
        try {
            // Getting existing content
            if (file.exists())
                nbt = NbtIo.read(file.toPath())!!

            return nbt.size()

        } catch (_: IOException)
        {
            logIOError(file.path, true)
        }
        catch (_: NullPointerException)
        {
            return 0
        }
        return -1
    }

    @JvmStatic
    fun generateAESKey(encryptionFile: File, keySize: Int = 128) {
        try
        {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(keySize)

            encryptionFile.writeText(Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded))
        } catch (_: IOException)
        {
            Cobbreeding.LOGGER.error("Couldn't create encryption key.")
        }
    }

    @JvmStatic
    fun getEncryptionKey(): SecretKey
    {
        val encryptionFile = File(Cobbreeding.ENCRYPTION_KEY_PATH)

        if (!encryptionFile.exists())
        {
            generateAESKey(encryptionFile)
        }

        val decodedKey: ByteArray = Base64.getDecoder().decode(encryptionFile.readText())
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }
}