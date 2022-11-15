package xceu

import ceu.all
import org.junit.Ignore
import org.junit.Test

class TXExec {

    @Test
    fun if1() {
        val out = all("""
            var x
            set x = if (true) { 1 }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun if2() {
        val out = all("""
            var x
            set x = 10
            set x = if false { 1 }
            println(x)
        """)
        assert(out == "nil\n") { out }
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
        assert(out == "1\n") { out }
    }
    @Test
    fun if4_err() {
        val out = all("""
            println(if [] {})
        """.trimIndent())
        //assert(out == "anon : (lin 1, col 4) : if error : invalid condition\n") { out }
        assert(out == "nil\n") { out }
    }

    @Test
    fun op_or_and() {
        val out = all("""
            println(true or println(1))
            println(false and println(1))
        """, true)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_not() {
        val out = all("""
            println(not nil and not false)
        """, true)
        assert(out == "true\n") { out }
    }
    @Test
    fun op2_or_and() {
        val out = all("""
            println(1 or throw 5)
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """, true)
        assert(out == "1\n2\nnil\n2\n") { out }
    }

    @Test
    fun todo_catch3() {
        val out = all("""
            var x
            set x = catch @x {
                catch 2 {
                    throw (@x,10)
                    println(9)
                }
                println(9)
            }
            println(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun todo_catch6_err() {
        val out = all("""
            catch @x {
                var x
                set x = []
                throw (@x, x)
                println(9)
            }
            println(1)
        """.trimIndent())
        assert(out == "anon : (lin 4, col 15) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun todo_catch7() {
        val out = all("""
            do {
                println(catch @x {
                    throw (@x,[10])
                    println(9)
                })
            }
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun todo_catch8() {
        val out = all("""
            var x
            set x = catch @x {
                var y
                set y = catch @y {
                    throw (@y,[10])
                    println(9)
                }
                ;;println(1)
                y
            }
            println(x)
        """.trimIndent())
        assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun todo_catch10() {
        val out = all("""
            catch @e1 {
                throw []
                println(9)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_while1() {
        val out = all("""
            println(catch @x { while true { throw (@x,1) }})
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_while2() {
        val out = all("""
            println(catch @x { while true { []; throw (@x,1) }})
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_while3() {
        val out = all("""
            println(catch 2 { while true { throw (2,[1]) }})
        """)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun todo_while4() {
        val out = all("""
            println(catch 2 { while true {
                var x
                set x = [1] ;; memory released
                throw (2,1)
            }})
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_while5_err() {
        val out = all("""
            println(catch 2 { while true {
                var x
                set x = [1]
                throw (2,x)
            }})
        """.trimIndent())
        assert(out == "anon : (lin 4, col 14) : set error : incompatible scopes\n") { out }
    }


    @Test
    @Ignore
    fun todo_scope_func6() {
        val out = all("""
            var f
            set f = func (x,s) {
                [x]@s
            }
            var x
            set x = f(10)
            println(x)
        """)
        assert(out == "[10]\n") { out }
    }
    @Test
    @Ignore
    fun todo_scope_scope3() {
        val out = all("""
            var x
            do {
                var a
                set a = [1,2,3] @x
                set x = a
            }
            println(x)
        """)
        assert(out == "[1,2,3]") { out }
    }


    @Test
    @Ignore
    fun todo_if5_noblk() {
        val out = all("""
            var x
            set x = 10
            set x = if false 1
            println(x)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    @Ignore
    fun todo_if6_noblk() {
        val out = all("""
            var x
            set x = if (true) 1 else 0
            println(x)
        """)
        assert(out == "1\n") { out }
    }

}