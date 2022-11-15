package ceu

import Lexer
import Parser
import Expr
import Coder
import Pos
import exec
import main
import org.junit.Ignore
import org.junit.Test
import java.io.File

// search in tests output for
//  definitely|Invalid read|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
val THROW = false
//val ceu.getVALGRIND = "valgrind "
//val ceu.getTHROW = true

fun all (inp: String, pre: Boolean=false): String {
    val inps = listOf(Pair("anon", inp.reader())) + if (!pre) emptyList() else {
        listOf(Pair("prelude.ceu", File("prelude.ceu").reader()))
    }
    val lexer = Lexer(inps)
    val parser = Parser(lexer)
    val es = try {
        parser.exprs()
    } catch (e: Throwable) {
        return e.message!!
    }
    val c = try {
        val coder = Coder(Expr.Block(Tk.Fix("", Pos("anon", 0, 0)), es))
        coder.main()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!!
    }
    File("out.c").writeText(c)
    val (ok2, out2) = exec("gcc -Werror out.c -lm -o out.exe")
    if (!ok2) {
        return out2
    }
    val (_, out3) = exec("$VALGRIND./out.exe")
    //println(out3)
    return out3
}

class TExec {

    // PRINT

    @Test
    fun print1() {
        val out = all("""
            print([10])
        """)
        assert(out == "[10]") { out }
    }
    @Test
    fun print2() {
        val out = all("""
            print(10)
            println(20)
        """)
        assert(out == "1020\n") { out }
    }
    @Test
    fun print3() {
        val out = all("""
            println([[],[1,2,3]])
            println(func () { nil })
        """)
        assert(out.contains("[[],[1,2,3]]\nfunc: 0x")) { out }
    }
    @Test
    fun print_err1() {
        val out = all("""
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun print_err2() {
        val out = all("""
            print(1)
            print()
            print(2)
            println()
            println(3)
        """)
        assert(out == "12\n3\n") { out }
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
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun index_err1() {
        val out = all("""
            println(1[1])
        """.trimIndent())
        assert(out == "anon : (lin 1, col 9) : index error : expected tuple\n") { out }
    }
    @Test
    fun index_err2() {
        val out = all("""
            println([1][[]])
        """.trimIndent())
        assert(out == "anon : (lin 1, col 13) : index error : expected number\n") { out }
    }
    @Test
    fun index_err3() {
        val out = all("""
            println([1][2])
        """.trimIndent())
        assert(out == "anon : (lin 1, col 13) : index error : out of bounds\n") { out }
    }

    // DCL

    @Test
    fun dcl() {
        val out = all("""
            var x
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun dcl_chars() {
        val out = all("""
            var x'
            var f!
            var even?
            println(x')
            println(f!)
            println(even?)
        """)
        assert(out == "nil\nnil\nnil\n") { out }
    }
    @Test
    fun dcl_redeclaration_err() {
        val out = all("""
            var x
            var x
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : variable \"x\" is already declared") { out }
    }

    // SET

    @Test
    fun set1() {
        val out = all("""
            var x
            set x = [10]
            println(x)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun set2() {
        val out = all("""
            var x
            set x = [10,20,[30]]
            set x[1] = 22
            set x[2][0] = 33
            println(x)
        """)
        assert(out == "[10,22,[33]]\n") { out }
    }
    @Test
    fun set_err1() {
        val out = all("""
            set 1 = 1
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_err2() {
        val out = all("""
            set [1] = 1
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : invalid set : invalid destination") { out }
    }
    @Test
    fun set_index() {
        val out = all("""
            var i
            set i = 1
            println([1,2,3][i])
        """)
        assert(out == "2\n") { out }
    }

    // DO

    @Test
    fun do1() {  // set whole tuple?
        val out = all("""
            do { nil }
        """)
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
        """)
        assert(out == "1\n") { out }
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
        """)
        assert(out == "10") { out }
    }
    @Test
    fun do4() {
        val out = all("""
            var x
            set x = do {nil}
            println(x)
        """)
        assert(out == "nil\n") { out }
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
        """)
        assert(out == "[1,2,3]\n") { out }
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
        """.trimIndent())
        assert(out == "anon : (lin 5, col 13) : set error : incompatible scopes\n") { out }
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
        """)
        assert(out == "[1,[4,5,6],[4,5,6]]\n") { out }
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
        """.trimIndent())
        assert(out == "anon : (lin 6, col 16) : set error : incompatible scopes\n") { out }
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
        """)
        assert(out == "[1,2,30]\n") { out }
    }
    @Test
    fun scope7() {
        val out = all("""
            var xs
            set xs = do {
                [10]
            }
            println(xs)
        """)
        assert(out == "[10]\n") { out }
    }

    // IF

    @Test
    fun if1() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """)
        assert(out == "anon : (lin 4, col 13) : expected \"else\" : have \"println\"") { out }
    }
    @Test
    fun if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "anon : (lin 5, col 13) : expected \"else\" : have \"println\"") { out }
    }
    @Test
    fun if3() {
        val out = all("""
            var x
            set x = 10
            set x = if (nil) {} else { 1 }
            println(x)
        """.trimIndent())
        //assert(out == "anon : (lin 3, col 13) : if error : invalid condition\n") { out }
        assert(out == "anon : (lin 3, col 19) : expected expression : have \"}\"") { out }
    }
    @Test
    fun if4_err() {
        val out = all("""
            println(if [] {nil} else {nil})
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }

    // FUNC / CALL

    @Test
    fun func1() {
        val out = all("""
            var f
            set f = func () { nil }
            var x
            set x = f()
            println(x)
        """)
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
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun func3() {
        val out = all("""
            var f
            set f = func (xxx) {
                xxx
            }
            var yyy
            set yyy = f(10)
            println(yyy)
        """)
        assert(out == "10\n") { out }
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
        """)
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
        """)
        assert(out == "[10]\n") { out }
        //assert(out.contains("set error : incompatible scopes")) { out }
    }
    @Test
    fun func7_err() {
        val out = all("1(1)")
        assert(out == "anon : (lin 1, col 1) : call error : expected function\n") { out }
    }
    @Test
    fun func8() {
        val out = all("""
            1
            (1)
        """)
        //assert(out == "anon : (lin 2, col 2) : call error : \"(\" in the next line") { out }
        assert(out == "") { out }
    }
    @Test
    fun func9() {
        val out = all("""
            var f
            set f = func (a,b) {
                [a,b]
            }
            println(f())
            println(f(1))
            println(f(1,2))
            println(f(1,2,3))
        """)
        assert(out == "[nil,nil]\n[1,nil]\n[1,2]\n[1,2]\n") { out }
    }
    @Test
    fun func10_err() {
        val out = all("""
            f()
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : access error : variable \"f\" is not declared") { out }
    }

    // WHILE

    @Test
    fun while6() {
        val out = all("""
            var x
            set x = true
            while x {
                set x = false
            }
            println(x)
        """)
        assert(out == "false\n") { out }
    }

    // ceu.getTHROW / CATCH

    @Test
    fun catch1() {
        val out = all("""
            catch @x {
                throw @x
                println(9)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun catch2_err() {
        val out = all("""
            catch @x {
                throw @y
                println(9)
            }
            println(1)
        """.trimIndent())
        assert(out == "anon : (lin 2, col 5) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun catch4() {
        val out = all("""
            var f
            set f = func () {
                catch @xxx {
                    throw @yyy
                    println(91)
                }
                println(9)
            }
            catch @yyy {
                catch @xxx {
                    f()
                    println(92)
                }
                println(93)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun catch5_err() {
        val out = all("""
            catch @x {
                throw []
                println(9)
            }
            println(1)
        """.trimIndent())
        assert(out == "anon : (lin 2, col 5) : throw error : expected tag\n") { out }
    }
    @Test
    fun catch9() {
        val out = all("""
            catch @e1 {
                catch @e2 {
                    catch @e3 {
                        catch @e4 {
                            println(1)
                            throw @e3
                            println(99)
                        }
                        println(99)
                    }
                    println(2)
                    throw @e1
                    println(99)
                }
                println(99)
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun catch11_err() {
        val out = all("""
            catch 1 {
                throw @y
                println(9)
            }
            println(1)
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : catch error : expected tag\n") { out }
    }

    // NATIVE

    @Test
    fun native1() {
        val out = all("""
            native {
                printf("xxx\n");
            }
        """)
        assert(out == "xxx\n") { out }
    }
    @Test
    fun native2() {
        val out = all("""
            var x
            set x = native {
                return 1.5;
            }
            println(x)
        """)
        assert(out == "1.5\n") { out }
    }
    @Test
    fun native3() {
        val out = all("""
            var x
            set x = native {}
            println(x)
        """)
        assert(out == "0\n") { out }
    }
    @Test
    fun native4() {
        val out = all("""
            var x
            set x = 10
            set x = native {
                printf(">>> %g\n", ${D}x);
                return ${D}x*2;
            }
            println(x)
        """)
        assert(out == ">>> 10\n20\n") { out }
    }
    @Test
    fun native5() {
        val out = all("""
            var x
            native {
                ${D}x = 20;
            }
            println(x)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun native6_err() {
        val out = all("""
             native {
                $D 
             }
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 2, col 4) : invalid identifier") { out }
    }
    @Test
    fun native7_err() {
        val out = all("""
             native {
             
                $D{x.y}
                
             }
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 3, col 4) : invalid identifier") { out }
    }
    @Test
    fun native8_err() {
        val out = all("""
            native ($D)
        """.trimIndent())
        assert(out == "anon : (lin 1, col 1) : native error : (lin 1, col 2) : unterminated token") { '.'+out+'.' }
    }

    // OPERATORS

    @Test
    fun op_umn() {
        val out = all("""
            println(-10)
        """, true)
        assert(out == "-10\n") { out }
    }
    @Test
    fun op_id1() {
        val out = all("""
            println({-}(10,4))
        """, true)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_arithX() {
        val out = all("""
            println((10 + -20*2)/5)
        """, true)
        assert(out == "-4\n") { out }
    }
    @Ignore
    fun todo_scope_op_id2() {
        val out = all("""
            set (+) = (-)
            println((+)(10,4))
        """)
        assert(out == "6\n") { out }
    }
    @Test
    fun op_cmp() {
        val out = all("""
            println(1 > 2)
            println(1 < 2)
            println(1 == 1)
            println(1 /= 1)
            println(2 >= 1)
            println(2 <= 1)
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\ntrue\nfalse\n") { out }
    }
    @Test
    fun op_eq() {
        val out = all("""
            println(1 == 1)
            println(1 /= 1)
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_assoc() {
        val out = all("""
            println(2 * 3 - 1)
        """, true)
        assert(out == "5\n") { out }
    }

    // TAGS

    @Test
    fun tag1() {
        val out = all("""
            var t
            set t = tags(1)
            println(t)
            println(tags(t))
            println(tags(tags(t)))
        """)
        assert(out == "@number\n@tag\n@tag\n") { out }
    }
    @Test
    @Ignore
    fun todo_tag2_err() {
        val out = all("""
            tags()
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    @Ignore
    fun todo_tag3_err() {
        val out = all("""
            tags(1,2)
        """)
        assert(out == "-10\n") { out }
    }
    @Test
    fun tag2() {
        val out = all("""
            println(@xxx)
            println(@xxx == @yyy)
            println(@xxx /= @yyy)
            println(@xxx == @xxx)
            println(@xxx /= @xxx)
        """)
        assert(out == "@xxx\nfalse\ntrue\ntrue\nfalse\n") { out }
    }

    // DEFER

    @Test
    fun defer1() {
        val out = all("""
            var f
            set f = func () {
                println(111)
                defer { println(222) }
                println(333)
            }
            defer { println(1) }
            do {
                println(2)
                defer { println(3) }
                println(4)
                do {
                    println(5)
                    f()
                    defer { println(6) }
                    println(7)
                }
                println(8)
                defer { println(9) }
                println(10)
            }
            println(11)
            defer { println(12) }
            println(13)
        """)
        assert(out == "2\n4\n5\n111\n333\n222\n7\n6\n8\n10\n9\n3\n11\n13\n12\n1\n") { out }
    }


    // MISC

    @Test
    @Ignore
    fun todo_closure() {    // TODO: solution: pass tuple that is compared on return
        val out = all("""
            var smallerc
            set smallerc = func (x) {
                func (y) {  // TODO: cannot return func that uses x in this block
                    if x { x } else { y }
                }
            }
            println(smallerc(3)(1))
        """)
        assert(out == "3\n") { out }
    }

    @Test
    @Ignore
    fun todo_err() {
        val out = all("""
            var t1
            var v
            set v = (if t1 {t1} else {nil})
            set v = (t1 or nil)
        """)
        assert(out == "3\n") { out }
    }
}