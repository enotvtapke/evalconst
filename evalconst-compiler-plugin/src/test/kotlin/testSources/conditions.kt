fun main() {
    fun evalIsEven(a: Int): Boolean {
        if (a % 2 == 0) {
            return true
        } else {
            return false
        }
    }

    fun evalNestedIf(a: Int): Boolean {
        return if (a % 3 == 0) {
            if (a % 7 == 0) {
                true
            } else false
        } else false
    }

    fun evalIsDigit(a: Int): Boolean {
        when(a) {
            1 -> return true
            2 -> return true
            3 -> return true
            4 -> return true
            5 -> return true
            6 -> return true
            7 -> return true
            8 -> return true
            9 -> return true
            0 -> return true
            else -> return false
        }
    }

    println(evalIsEven(83))
    println(evalIsDigit(2))
    println(evalIsDigit(13))
    println(evalNestedIf(21))
    println(evalNestedIf(55))
}


//fun fac(n: Int): Int {
//    if (n == 1) return 1
//    return n * fac(n - 1)
//}