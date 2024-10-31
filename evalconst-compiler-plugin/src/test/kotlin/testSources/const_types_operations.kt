fun main() {
    fun evalSum(a: Int, b: Int): Int {
        return a + b
    }

    fun evalSub(a: Int, b: Int): Int {
        return a - b
    }

    fun evalNot(b: Boolean): Boolean {
        return !b
    }

    fun evalConcat(a: String, b: String): String {
        return a + b
    }

    fun evalLen(a: String): Int {
        return a.length
    }

    println(evalSum(1, 2))
    println(evalSub(1, 2))
    println(evalNot(false))
    println(evalConcat("fiz", "buz"))
    println(evalConcat("fiz", "buz").length)
    println(evalLen("fiz"))
}