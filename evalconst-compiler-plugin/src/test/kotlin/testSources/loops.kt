fun main() {

    fun multiply(a: Int, b: Int): Int {
        var i = 0
        var res = 0
        while (i < b) {
            res += a
            i += 1
        }
        return res
    }

    fun nestedLoop(a: Int, b: Int): Int {
        var r = 0
        var i = 0
        var j = 0
        while (i < a) {
            while (j < b) {
                r += i + j
                j += 1
            }
            i += 1
        }
        return r
    }

    println(multiply(5, 10))
    println(nestedLoop(5, 10))
}