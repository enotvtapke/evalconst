fun main() {
    fun isEven(a: Int): Boolean {
        if (a % 2 == 0) {
            return true
        } else {
            return false
        }
    }

    fun isDigit(a: Int): Boolean {
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

    println(isEven(83))
    println(isDigit(2))
    println(isDigit(13))
}


//fun fac(n: Int): Int {
//    if (n == 1) return 1
//    return n * fac(n - 1)
//}