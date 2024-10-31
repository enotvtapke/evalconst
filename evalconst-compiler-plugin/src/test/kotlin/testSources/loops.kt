fun main() {

    fun evalMultiply(a: Int, b: Int): Int {
        var i = 0
        var res = 0
        while (i < b) {
            res += a
            i += 1
        }
        return res
    }

    println(evalMultiply(5, 10))
}