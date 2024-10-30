fun main() {
    fun evalFac(n: Int): Int {
        if (n <= 1) return 1
        return n * evalFac(n - 1)
    }

    fun evalFib(n: Int): Int {
        if (n < 2) return 1
        return evalFib(n - 1) + evalFib(n - 2)
    }

//    println(evalFac(6))
    println(evalFib(5))
}
