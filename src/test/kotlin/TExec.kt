import org.junit.Test
import java.io.File

class TExec {

    fun all(inp: String): String {
        val lexer = Lexer("anon", inp.reader())
        val parser = Parser(lexer)
        val es = try {
            parser.exprs()
        } catch (e: Throwable) {
            return e.message!!
        }
        val c = Code(Expr.Do(Tk.Fix("",0,0),es))
        File("out.c").writeText(c)
        val (ok2, out2) = exec("gcc -Werror out.c -o out.exe")
        if (!ok2) {
            return out2
        }
        val (_, out3) = exec("./out.exe")
        //println(out3)
        return out3
    }

    // PRINT

    @Test
    fun print1() {
        val out = all("""
            print([10])
        """.trimIndent()
        )
        assert(out == "[10.000000]") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print(10)
            println(20)
        """.trimIndent()
        )
        assert(out == "10.00000020.000000\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([[],[1,2,3]])
        """.trimIndent()
        )
        assert(out == "[[],[1.000000,2.000000,3.000000]]\n") { out }
    }
    @Test
    fun print_err1() {
        val out = all("""
            println(1)
        """.trimIndent()
        )
        assert(out.contains("1")) { out }
    }
    @Test
    fun print_err2() {
        val out = all("""
            print(1)
            print()
            print(2)
            println()
            println(3)
        """.trimIndent()
        )
        assert(out.contains("1.0000002.000000\n3.000000\n")) { out }
    }
    @Test
    fun print4() {
        val out = all("print(nil)")
        assert(out == "nil") { out }
    }
    @Test
    fun print5() {
        val out = all("print(true)")
        assert(out == "true") { out }
    }
    @Test
    fun print6() {
        val out = all("println(false)")
        assert(out == "false\n") { out }
    }

    // INDEX

    @Test
    fun index() {
        val out = all("""
            println([1,2,3][1])
        """.trimIndent()
        )
        assert(out == "2.000000\n") { out }
    }
    @Test
    fun index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent()
        )
        assert(out.contains("index error : expected tuple")) { out }
    }
    @Test
    fun index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent()
        )
        assert(out.contains("index error : expected number")) { out }
    }
    @Test
    fun index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent()
        )
        assert(out.contains("index error : out of bounds")) { out }
    }

    // DCL

    @Test
    fun dcl() {
        val out = all("""
            var x
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = all("""
            var x
            set x = [10]
            println(x)
        """.trimIndent()
        )
        assert(out == "[10.000000]\n") { out }
    }
    @Test
    fun set2() {
        val out = all("""
            var x
            set x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            println(x)
        """.trimIndent()
        )
        assert(out == "[10.000000,22.000000,[33.000000]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent()
        )
        assert(out == "anon: (ln 1, col 1): invalid set : invalid destination") { out }
    }
    @Test
    fun set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent()
        )
        assert(out == "anon: (ln 1, col 1): invalid set : invalid destination") { out }
    }
    @Test
    fun set_index() {
        val out = all("""
            var i
            set i = 1
            println([1,2,3][i])
        """.trimIndent()
        )
        assert(out == "2.000000\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = all("""
            do {}
        """.trimIndent()
        )
        assert(out == "") { out }
    }
    @Test
    fun do2() {
        val out = all("""
            do {
                var a
                set a = 1
                println(a)
            }
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun do3() {
        val out = all("""
            var x
            set x = do {
                var a
                set a = 10
                a
            }
            print(x)
        """.trimIndent()
        )
        assert(out == "10.000000") { out }
    }

    // SCOPE

    @Test
    fun scope1() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,2.000000,3.000000]\n") { out }
    }
    @Test
    fun scope_err2() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3]
                set x = a
            }
        """.trimIndent()
        )
        assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun todo_scope_scope3() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3] @x
                set x = a
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,2.000000,3.000000]") { out }
    }
    @Test
    fun scope4() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                set x[1] = [4,5,6]
                do {
                    var y
                    set y = [10,20,30]
                    set y[1] = x[1]
                    set x[2] = y[1]
                }
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,[4.000000,5.000000,6.000000],[4.000000,5.000000,6.000000]]\n") { out }
    }
    @Test
    fun scope5_err() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = [10,20,30]
                set x[2] = y
            }
            println(x)
        """.trimIndent()
        )
        assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun scope6() {
        val out = all("""
            var x
            do {
                set x = [1,2,3]
                var y
                set y = 30
                set x[2] = y
            }
            println(x)
        """.trimIndent()
        )
        assert(out == "[1.000000,2.000000,30.000000]\n") { out }
    }
    @Test
    fun scope7() {
        val out = all("""
            var xs
            set xs = do {
                [10]
            }
            println(xs)
        """.trimIndent()
        )
        assert(out.contains("[10.000000]")) { out }
    }

    // IF

    @Test
    fun if1() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun if3() {
        val out = all("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun if_err() {
        val out = all("""
            if [] {}
        """.trimIndent()
        )
        assert(out.contains("if error : invalid condition")) { out }
    }

    // FUNC / CALL

    @Test
    fun func1() {
        val out = all("""
            var f
            set f = func () {}
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func2() {
        val out = all("""
            var f
            set f = func () {
                1
            }
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun func3() {
        val out = all("""
            var f
            set f = func (x) {
                x
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out == "10.000000\n") { out }
    }
    @Test
    fun func4() {
        val out = all("""
            var f
            set f = func (x) {
                x
            }
            var x
            set x = f()
            println(x)
        """.trimIndent()
        )
        assert(out == "nil\n") { out }
    }
    @Test
    fun func5_err() {
        val out = all("""
            var f
            set f = func (x) {
                [x]
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun todo_scope_func6() {
        val out = all("""
            var f
            set f = func (x,s) {
                [x]@s
            }
            var x
            set x = f(10)
            println(x)
        """.trimIndent()
        )
        assert(out == "[10.000000]\n") { out }
    }
    @Test
    fun func7_err() {
        val out = all("1(1)")
        assert(out.contains("call error : expected function")) { out }
    }

    // LOOP / BREAK

    @Test
    fun loop1() {
        val out = all("""
            println(loop { break 1 })
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun loop2() {
        val out = all("""
            println(loop { []; break 1 })
        """.trimIndent()
        )
        assert(out == "1.000000\n") { out }
    }
    @Test
    fun todo_leak_loop3() {
        val out = all("""
            println(loop { break [1] })
        """.trimIndent()
        )
        assert(out == "[1.000000]\n") { out }
    }
}