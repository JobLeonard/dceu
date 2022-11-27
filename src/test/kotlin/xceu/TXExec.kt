package xceu

import ceu.all
import ceu.lexer
import org.junit.Ignore
import org.junit.Test

class TXExec {

    // EMPTY IF

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

    // NO BLOCK
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
            var x = if (true) 1 else 0
            println(x)
        """)
        assert(out == "1\n") { out }
    }

    // IFS

    @Test
    fun ifs1() {
        val out = all("""
            var x = ifs {
                10 < 1 { 99 }
                5+5==0 { 99 }
                else { 10 }
            }
            println(x)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ifs2() {
        val out = all("""
            var x = ifs { true { `:number 1` } }
            println(x)
        """)
        assert(out == "1\n") { out }
    }

    // OPS: not, and, or

    @Test
    fun op_or_and() {
        val out = all("""
            println(true or println(1))
            println(false and println(1))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun op_not() {
        val out = all("""
            println(not nil and not false)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun op2_or_and() {
        val out = all("""
            println(1 or throw 5)
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }

    // YIELD

    @Test
    fun bcast1() {
        val out = all("""
            var tk = task () {
                println(evt)
                yield ()
                println(evt)                
            }
            var co1 = coroutine tk
            var co2 = coroutine tk
            broadcast 1
            broadcast 2
            broadcast 3
        """)
        assert(out == "1\n1\n2\n2\n") { out }
    }

    // SPAWN, PAR

    @Test
    fun par1() {
        val out = all("""
            spawn task () {
                par {
                    yield ()
                    yield ()
                    println(1)
                } with {
                    yield ()
                    println(2)
                } with {
                    println(3)
                }
            } ()
            broadcast ()
        """)
        assert(out == "3\n2\n") { out }
    }
    @Test
    fun spawn2() {
        val out = all("""
            spawn {
                println(1)
                yield ()
                println(3)
            }
            println(2)
            broadcast ()
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun spawn3() {
        val out = all("""
            spawn {
                spawn {
                    println(1)
                }
            }
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun spawn4() {
        val out = all("""
            (spawn in ts, T()) where {
            }
        """)
        assert(out == "anon : (lin 2, col 23) : access error : variable \"ts\" is not declared") { out }
    }

    // PARAND / PAROR / WATCHING

    @Test
    fun paror1() {
        val out = all("""
            spawn task () {
                paror {
                    yield ()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield ()
                    println(3)
                }
                println(999)
            } ()
        """)
        assert(out == "2\n999\n") { out }
    }
    @Test
    fun paror2() {
        val out = all("""
            spawn task () {
                paror {
                    defer { println(1) }
                    yield ()
                    yield ()
                    println(1)
                } with {
                    yield ()
                    println(2)
                } with {
                    defer { println(3) }
                    yield ()
                    yield ()
                    println(3)
                }
                println(999)
            } ()
            broadcast nil
        """)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand3() {
        val out = all("""
            spawn task () {
                parand {
                    yield ()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield ()
                    println(3)
                }
                println(999)
            } ()
            broadcast nil
        """, true)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand4() {
        val out = all("""
            spawn task () {
                parand {
                    defer { println(1) }
                    yield ()
                    yield ()
                    println(1)
                } with {
                    yield ()
                    println(2)
                } with {
                    defer { println(3) }
                    yield ()
                    yield ()
                    println(3)
                }
                println(999)
            } ()
            broadcast nil
            broadcast nil
        """, true)
        assert(out == "2\n1\n1\n3\n3\n999\n") { out }
    }
    @Test
    fun watching5() {
        val out = all("""
            spawn task () {
                watching evt==1 {
                    defer { println(2) }
                    yield ()
                    println(1)
                }
                println(999)
            } ()
            broadcast nil
            broadcast 1
        """)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun watching6_clk() {
        val out = ceu.all("""
            spawn task () {
                watching 10s {
                    defer { println(10) }
                    await false
                    println(1)
                }
                println(999)
            } ()
            println(0)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(1)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(2)
        """, true)
        assert(out == "0\n1\n10\n999\n2\n") { out }
    }
    @Test
    fun all7() {
        val out = ceu.all(
            """
            task Bird () {
                watching true {
                    par {
                    } with {
                    }
                }
            }            
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun all8() {
        val out = all("""
            spawn {
                par {
                    every 500ms {
                    }
                } with {
                    every true { }
                }
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun all9() {
        val out = all("""
            spawn task () {
                ^[9,29]yield nil                                          
            }()                                                       
            spawn task () {                                           
                ^[9,29]yield nil                       
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_all10() {
        val out = all("""
            task T () {
                watching (throw :error) {
                    await false
                }
            }            
            spawn in coroutines(), T()
            broadcast nil
        """)
        assert(out == "1\n") { out }
    }

    // INDEX: TUPLE / DICT

    @Test
    fun todo_ndex1_tuple() {
        val out = all("""
            var t = [1,2,3]
            println(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun todo_index2_dict() {
        val out = all("""
            var t = @[ (:x,1), (:y,2) ]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }

    // AWAIT / EVERY

    @Test
    fun await1() {
        val out = all("""
            spawn {
                println(0)
                await evt[:type]==:x
                println(99)
            }
            do {
                println(1)
                broadcast @[(:type,:y)]
                println(2)
                broadcast @[(:type,:x)]
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await2_err() {
        val out = all("""
            await f()
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun await3() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    await true                    
                    println(evt)
                }
            }()
            broadcast @[]
        """
        )
        assert(out == "@[]\n") { out }
    }
    @Test
    fun every4() {
        val out = all("""
            spawn {
                println(0)
                every evt[:type]==:x {
                    println(evt[:v])
                }
            }
            do {
                println(1)
                broadcast @[(:type,:x),(:v,10)]
                println(2)
                broadcast @[(:type,:y),(:v,20)]
                println(3)
                broadcast @[(:type,:x),(:v,30)]
                println(4)
            }
        """)
        assert(out == "0\n1\n10\n2\n3\n30\n4\n") { out }
    }
    @Test
    fun await5_clk() {
        val out = ceu.all("""
            spawn task () {
                while (true) {
                    await 10s                    
                    println(999)
                }
            }()
            println(0)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(1)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(2)
        """, true)
        assert(out == "0\n1\n999\n2\n") { out }
    }
    @Test
    fun every6_clk() {
        val out = ceu.all("""
            spawn task () {
                every 10s {
                    println(10)
                }
            }()
            println(0)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(1)
            broadcast @[(:type,:timer),(:dt,5000)]
            println(2)
            broadcast @[(:type,:timer),(:dt,10000)]
            println(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun todo_every7_clk() { // awake twice from single bcast
        val out = ceu.all("""
            spawn task () {
                every 10s {
                    println(10)
                }
            }()
            println(0)
            broadcast @[(:type,:timer),(:dt,20000)]
            println(1)
        """, true)
        assert(out == "0\n10\n10\n1") { out }
    }

    // FUNC / TASK

    @Test
    fun func1() {
        val out = ceu.all(
            """
            func f (x) {
                println(x)
            }
            f(10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun task2() {
        val out = ceu.all(
            """
            task f (x) {
                println(x)
            }
            spawn f (10)
        """
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun func3_err() {
        val out = ceu.all(
            """
            func f {
                println(x)
            }
        """
        )
        assert(out == "anon : (lin 2, col 20) : expected \"(\" : have \"{\"") { out }
    }
    @Test
    fun todo_task4_pub_fake_err() {
        val out = all("""
            spawn {
                watching evt==:a {
                    every evt==:b {
                        println(pub)    ;; no enclosing task
                    }
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun task5_pub_fake() {
        val out = all("""
            spawn (task () {
                set pub = 1
                watching evt==:a {
                    every evt==:b {
                        println(pub)
                    }
                }
            }) ()
            broadcast :b
            broadcast :b
            broadcast :a
            broadcast :b
        """)
        assert(out == "1\n1\n") { out }
    }

    // WHERE

    @Test
    fun where1() {
        val out = ceu.all(
            """
            println(x) where {
                var x = 1
            }
            var z = y + 10 where {
                var y = 20
            }
            println(z)
        """,true)
        assert(out == "1\n30\n") { out }
    }

    // THROW / CATCH

    @Test
    fun todo_catch3() {
        val out = all("""
            var x
            set x = catch :x {
                catch 2 {
                    throw (:x,10)
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
            catch :x {
                var x
                set x = []
                throw (:x, x)
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
                println(catch :x {
                    throw (:x,[10])
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
            set x = catch :x {
                var y
                set y = catch :y {
                    throw (:y,[10])
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
            catch :e1 {
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
            println(catch :x { while true { throw (:x,1) }})
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun todo_while2() {
        val out = all("""
            println(catch :x { while true { []; throw (:x,1) }})
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
                [x]:s
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
                set a = [1,2,3] :x
                set x = a
            }
            println(x)
        """)
        assert(out == "[1,2,3]") { out }
    }

}
