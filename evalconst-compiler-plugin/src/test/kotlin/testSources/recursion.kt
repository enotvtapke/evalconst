fun main() {
    fun evalFac(n: Int): Int {
        return if (n <= 1) 1 else n * evalFac(n - 1)
    }

    fun evalFib(n: Int): Int {
        return if (n < 2) 1 else evalFib(n - 1) + evalFib(n - 2)
    }

    println(evalFac(6))
    println(evalFib(5))
}
