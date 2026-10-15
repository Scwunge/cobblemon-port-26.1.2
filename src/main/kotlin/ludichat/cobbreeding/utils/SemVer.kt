package ludichat.cobbreeding.utils

class SemVer(val components: List<String>) {
    val major: Int
        get() = components.getOrNull(0)?.toInt() ?: 0
    val minor: Int
        get() = components.getOrNull(1)?.toInt() ?: 0
    val patch: Int
        get() = components.getOrNull(2)?.toInt() ?: 0
    val others: List<String>
        get() = if (components.size > 3) components.subList(3, components.size) else listOf()

    operator fun compareTo(ver: Int): Int {
        return major.compareTo(ver)
    }

    operator fun compareTo(ver: String): Int {
        return compareTo(ver.toSemVer())
    }

    operator fun compareTo(ver: SemVer): Int {
        val ma = major.compareTo(ver.major)
        val mi = minor.compareTo(ver.minor)
        val pa = patch.compareTo(ver.patch)
        return if (ma == 0) {
            if (mi == 0) {
                if (pa == 0) {
                    val ot1 = others.isNotEmpty()
                    val ot2 = ver.others.isNotEmpty()
                    if (ot1 && !ot2) 1
                    else if (!ot1 && ot2) -1
                    else 0
                } else pa
            } else mi
        } else ma
    }
}

fun String.toSemVer(): SemVer {
    return if (this.matches(Regex("""^\d(\.\d(\.\d([.-_].+)?)?)?$"""))) SemVer(this.split(".", "-"))
    else SemVer(listOf())
}