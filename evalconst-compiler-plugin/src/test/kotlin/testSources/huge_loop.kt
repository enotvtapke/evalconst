fun main() {
    fun evalIterate(a: Int): Int {
        var i = 0
        while (i < a) {
            i+=1
        }
        return i
    }

    fun evalIterateInfinitely(): Int {
        while (true) { }
        return 1
    }

    fun evalRecInfinitely(i: Int): Int {
        return evalRecInfinitely(i)
    }

    println(evalIterate(1_000_100))
    println(evalIterate(1000))
    println(evalIterateInfinitely())
    println(evalRecInfinitely(1000))
}