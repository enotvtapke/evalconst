fun main() {
    fun evalNestedLoop(a: Int, b: Int): Int {
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

    println(evalNestedLoop(5, 10))
}