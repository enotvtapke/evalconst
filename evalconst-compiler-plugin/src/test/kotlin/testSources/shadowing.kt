fun main() {
    fun evalShadows1(a: Int): Int {
        if (true) {
            val a = 10
        }
        return a
    }

    fun evalShadows2(a: Int): Int {
        var b = 0
        if (true) {
            val a = 10
            b = a
        }
        return b
    }

    println(evalShadows1(5))
    println(evalShadows2(1))
}