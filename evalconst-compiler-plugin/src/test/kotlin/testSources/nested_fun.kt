fun main() {
    fun evalSum(a: Int, b: Int): Int {
        return a + b
    }

    fun evalSub(a: Int, b: Int): Int {
        return a - b
    }

    fun eval_a2_sub_b2(a: Int, b: Int): Int {
        val x = evalSum(a, b)
        val y = evalSub(a, b)
        return x * y
    }

    println(eval_a2_sub_b2(4, 3))
}
