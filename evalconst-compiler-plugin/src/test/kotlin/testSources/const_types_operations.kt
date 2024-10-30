fun main() {
    fun sum(a: Int, b: Int): Int {
        return a + b
    }

    fun sub(a: Int, b: Int): Int {
        return a - b
    }

    fun not(b: Boolean): Boolean {
        return !b
    }

    fun concat(a: String, b: String): String {
        return a + b
    }

    fun len(a: String): Int {
        return a.length
    }

    println(sum(1, 2))
    println(sub(1, 2))
    println(not(false))
    println(concat("fiz", "buz"))
    println(len("fiz"))
}