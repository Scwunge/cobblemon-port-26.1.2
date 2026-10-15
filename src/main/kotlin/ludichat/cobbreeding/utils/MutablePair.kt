package ludichat.cobbreeding.utils

class MutablePair<A, B>(var first: A, var second: B) {
    fun toPair(): Pair<A, B> = Pair(first, second)
}

fun <A, B> Pair<A, B>.toMutablePair(): MutablePair<A, B> = MutablePair(first, second)