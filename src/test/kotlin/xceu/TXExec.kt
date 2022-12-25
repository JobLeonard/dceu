package xceu

import ceu.all
import ceu.lexer
import ceu.yield
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

    // IFS

    @Test
    fun ifs1() {
        val out = all("""
            var x = ifs {
                10 < 1 -> 99
                (5+5)==0 -> { 99 }
                else -> 10
            }
            println(x)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun ifs2() {
        val out = all("""
            var x = ifs { true -> `:number 1` }
            println(x)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun ifs3() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                == 20 -> true
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ifs4() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                true  -> true
                == 20 -> false
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun ifs5() {
        val out = all("""
            var x = ifs 20 {
                == 10 -> false
                else -> true
            }
            println(x)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun todo_ifs6_nocnd() {
        val out = all("""
            var x = ifs 20 {
                true -> ifs {
                    == 20 -> true   ;; err: no ifs expr
                }
            }
            println(x)
        """)
        assert(out == "ERROR\n") { out }
    }
    @Test
    fun ifs7() {
        val out = all("""
            var x = ifs 20 {
                is 10 -> false
                true  -> true
                is 20 -> false
                else  -> false
            }
            println(x)
        """)
        assert(out == "true\n") { out }
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
            println(1 or throw(5))
            println(1 and 2)
            println(nil and 2)
            println(nil or 2)
        """)
        assert(out == "1\n2\nnil\n2\n") { out }
    }
    @Test
    fun op3_or_and() {
        val out = all("""
            println(true and ([] or []))
        """)
        assert(out == "[]\n") { out }
    }

    // is, isnot

    @Test
    fun is1() {
        val out = all("""
            println([] is :bool)
            println([] is :tuple)
            println(1 isnot :tuple)
            println(1 isnot :number)
        """, true)
        assert(out == "false\ntrue\ntrue\nfalse\n") { out }
    }
    @Test
    fun is2() {
        val out = all("""
            var t
            set t = []
            tags(t,:x,true)
            println(t is :x)
            tags(t,:y,true)
            println(t isnot :y)
            tags(t,:x,false)
            println(t isnot :x)
        """, true)
        assert(out == "true\nfalse\ntrue\n") { out }
    }

    // YIELD

    @Test
    fun bcast1() {
        val out = all("""
            var tk = task () {
                println(evt)
                do { var ok; set ok=true; while ok { yield(nil); if (evt isnot :coro) { set ok=false } else { nil } } }
                ;;yield()
                println(evt)                
            }
            var co1 = coroutine(tk)
            var co2 = coroutine(tk)
            broadcast in :global, 1
            broadcast in :global, 2
            broadcast in :global, 3
        """, true)
        assert(out == "1\n1\n2\n2\n") { out }
    }

    // SPAWN, PAR

    @Test
    fun par1() {
        val out = all("""
            spawn task () {
                par {
                    do { var ok1; set ok1=true; while ok1 { yield(nil); if type(evt)/=:coro { set ok1=false } else { nil } } }
                    ;;yield()
                    do { var ok2; set ok2=true; while ok2 { yield(nil); if type(evt)/=:coro { set ok2=false } else { nil } } }
                    ;;yield()
                    println(1)
                } with {
                    do { var ok3; set ok3=true; while ok3 { yield(nil); if type(evt)/=:coro { set ok3=false } else { nil } } }
                    ;;yield()
                    println(2)
                } with {
                    println(3)
                }
            } ()
            broadcast in :global, nil
        """)
        assert(out == "3\n2\n") { out }
    }
    @Test
    fun spawn2() {
        val out = all("""
            spawn {
                println(1)
                yield()
                println(3)
            }
            println(2)
            broadcast in :global, nil
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
                nil
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
                    ${yield()}
                    println(1)
                } with {
                    println(2)
                } with {
                    ${yield()}
                    println(3)
                }
                println(999)
            } ()
        """, true)
        assert(out == "2\n999\n") { out }
    }
    @Test
    fun paror1a() {
        val out = all("""
            spawn task () {
                paror {
                    ${yield()}
                    println(1)
                } with {
                    defer { println(3) }
                    ${yield()}
                    println(2)
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "1\n3\n999\n") { out }
    }
    @Test
    fun paror1b() {
        val out = all("""
            spawn task () {
                paror {
                    defer { println(3) }
                    ${yield()}
                    println(1)
                } with {
                    println(2)
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "2\n3\n999\n") { out }
    }
    @Test
    fun paror2() {
        val out = all("""
            spawn task () {
                paror {
                    defer { println(1) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(1)
                } with {
                    ${yield()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(3)
                }
                println(999)
            } ()
            broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand3() {
        val out = all("""
            spawn task () {
                parand {
                    yield()
                    println(1)
                } with {
                    println(2)
                } with {
                    yield()
                    println(3)
                }
                println(999)
            } ()
             broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n3\n999\n") { out }
    }
    @Test
    fun parand4() {
        val out = all("""
            spawn task () {
                parand {
                    defer { println(1) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(1)
                } with {
                    ${yield()}
                    println(2)
                } with {
                    defer { println(3) }
                    ${yield("ok1")}
                    ${yield("ok2")}
                    println(3)
                }
                println(999)
            } ()
             broadcast in :global, nil
             broadcast in :global, nil
        """, true)
        assert(out == "2\n1\n1\n3\n3\n999\n") { out }
    }
    @Test
    fun watching5() {
        val out = all("""
            spawn task () {
                awaiting evt==1 {
                    defer { println(2) }
                    yield()
                    println(1)
                }
                println(999)
            } ()
             broadcast in :global, nil
             broadcast in :global, 1
        """, true)
        assert(out == "1\n2\n999\n") { out }
    }
    @Test
    fun watching6_clk() {
        val out = ceu.all("""
            spawn task () {
                awaiting 10s {
                    defer { println(10) }
                    await false
                    println(1)
                }
                println(999)
            } ()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
            println(2)
        """, true)
        assert(out == "0\n1\n10\n999\n2\n") { out }
    }
    @Test
    fun all7() {
        val out = ceu.all(
            """
            task Bird () {
                awaiting true {
                    par {
                    } with {
                    }
                }
            }            
            println(1)
        """, true)
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
                ^[9,29]yield(nil)                                          
            }()                                                       
            spawn task () {                                           
                ^[9,29]yield(nil)                       
            }()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun all10_err() {
        val out = all("""
            task T () {
                awaiting (throw(:error)) {
                    await false
                }
            }            
            spawn in coroutines(), T()
            broadcast in :global, nil
        """, true)
        assert(out == "anon : (lin 8, col 13) : broadcast in :global, nil\n" +
                "anon : (lin 3, col 27) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun paror11_ret() {
        val out = all("""
            spawn {
                var x = paror {
                    1
                } with {
                    999
                }
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror12_ret_func() {
        val out = all("""
            spawn {
                task f () {
                    paror {
                        1
                    } with {
                        999
                    }
                }
                var x = await spawn f()
                println(x)
            }
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror13_ret_func() {
        val out = all("""
            task T () {
                await evt==:x
            }
            spawn {
                paror {
                    await spawn T()
                } with {
                    await spawn T()
                }
            }
            broadcast in :global, :x
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror14() {
        val out = all("""
            spawn {
                paror {
                    await true
                } with {
                    await true
                }
            }
            broadcast in :global, true
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun paror15() {
        val out = all("""
            spawn {
                paror {
                    await true
                } with {
                    await true
                }
            }
            do {
                broadcast in :global, tags([40], :frame, true)
                broadcast in :global, tags([], :draw, true)
            }
            println(1)
        """, true)
        assert(out == "1\n") { out }
    }

    // TUPLE / VECTOR / DICT / STRING

    @Test
    fun todo_index1_tuple() {
        val out = all("""
            var t = [1,2,3]
            println(t.a, t.c)
        """)
        assert(out == "1\t3\n") { out }
    }
    @Test
    fun index2_dict() {
        val out = all("""
            var t = @[ (:x,1), (:y,2) ]
            println(t.x, t.y)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun todo_vector1_size() {
        val out = all("""
            var v = #[]
            println(${D}v, v)
            set v[#] = 1
            set v[#] = 2
            println(${D}v, v)
            var top = (set v[#] = nil)
            println(${D}v, v, v[#], top)
        """)
        assert(out == "3,#[1,2,3,4],#[1,2,3,4,?],#[1,2]\n") { out }
    }
    @Test
    fun string2() {
        val out = all("""
            var v = "abc"
            set v[#v] = 'a'
            set v[2] = 'b'
            println(v[0])
            `puts(${D}v.Dyn->Vector.mem);`
        """)
        assert(out == "a\nabba\n") { out }
    }
    @Test
    fun string3() {
        val out = all("""
            println("")
            println("a\tb")
            println("a\nb")
            println("a'\"b")
        """)
        assert(out == "#[]\na\tb\na\nb\na'\"b\n") { out }
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
                broadcast in :global, @[(:type,:y)]
                println(2)
                broadcast in :global, @[(:type,:x)]
                println(3)
            }
        """)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await2() {
        val out = all("""
            spawn {
                println(0)
                await evt is :x
                println(99)
            }
            do {
                println(1)
                broadcast in :global, tags([], :y, true)
                println(2)
                broadcast in :global, tags([], :x, true)
                println(3)
            }
        """, true)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await3() {
        val out = all("""
            spawn {
                println(0)
                await :x
                println(99)
            }
            do {
                println(1)
                broadcast in :global, tags([], :y, true)
                println(2)
                broadcast in :global, tags([], :x, true)
                println(3)
            }
        """, true)
        assert(out == "0\n1\n2\n99\n3\n") { out }
    }
    @Test
    fun await4_err() {
        val out = all("""
            await f()
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun await5() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    await true                    
                    println(evt)
                }
            }()
             broadcast in :global, @[]
        """
        )
        assert(out == "@[]\n") { out }
    }
    @Test
    fun every6() {
        val out = all("""
            spawn {
                println(0)
                every :x {
                    println(evt.0)
                }
            }
            do {
                println(1)
                broadcast in :global, tags([10], :x, true)
                println(2)
                broadcast in :global, tags([20], :y, true)
                println(3)
                broadcast in :global, tags([30], :x, true)
                println(4)
            }
        """, true)
        assert(out == "0\n1\n10\n2\n3\n30\n4\n") { out }
    }
    @Test
    fun await7_clk() {
        val out = ceu.all("""
            spawn task () {
                while (true) {
                    await 10s                    
                    println(999)
                }
            }()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
            println(2)
        """, true)
        assert(out == "0\n1\n999\n2\n") { out }
    }
    @Test
    fun every8_clk() {
        val out = ceu.all("""
            spawn task () {
                every 10s {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tags([5000], :frame, true)
            println(1)
            broadcast in :global, tags([5000], :frame, true)
            println(2)
            broadcast in :global, tags([10000], :frame, true)
            println(3)
        """, true)
        assert(out == "0\n1\n10\n2\n10\n3\n") { out }
    }
    @Test
    fun todo_every9_clk_multi() { // awake twice from single bcast
        val out = ceu.all("""
            spawn task () {
                every 10s {
                    println(10)
                }
            }()
            println(0)
            broadcast in :global, tags([20000], :frame, true)
            println(1)
        """, true)
        assert(out == "0\n10\n10\n1") { out }
    }
    @Test
    fun await10_task() {
        val out = all("""
            spawn {
                await spawn { 1 }
                println(1)
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun await11_task() {
        val out = all("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                }
                yield ()
                println(2)
            }
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun await12_task() {
        val out = all("""
            spawn {
                spawn {
                    yield ()
                    println(1)
                    broadcast in :global, nil
                    println(3)
                }
                yield ()
                println(2)
            }
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun await13_task_rets() {
        val out = all("""
            spawn {
                var y = await spawn {
                    yield ()
                    [2]
                }
                println(y)
            }
            broadcast in :global, nil
        """)
        assert(out == "[2]\n") { out }
    }
    @Test
    fun await14_task_err() {
        val out = all("""
            var x = await spawn in nil, nil()
        """)
        assert(out == "anon : (lin 2, col 27) : expected non-pool spawn : have \"spawn\"") { out }
    }
    @Test
    fun await15_task_rets() {
        val out = all("""
            spawn {
                var x = await spawn {
                    var y = []
                    y
                }
                println(x)
            }
        """)
        assert(out == "anon : (lin 2, col 20) : task :fake () { var x set x = do { var ceu_sp...)\n" +
                "anon : (lin 3, col 38) : task :fake () { var y set y = [] y }()\n" +
                "anon : (lin 3, col 52) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun await16_task_rets() {
        val out = all("""
            spawn {
                var x = await spawn {
                    1
                }
                var y = await spawn {
                    yield ()
                    [2]
                }
                task T () {
                    3
                }
                var z = await spawn T()
                println(x,y,z)
            }
            broadcast in :global, nil
        """)
        assert(out == "1\t[2]\t3\n") { out }
    }
    @Test
    fun await17_task() {
        val out = all("""
            task Main_Menu () {
                await false
            }            
            spawn {
                await spawn Main_Menu ()
                println(999)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
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
    fun task4_pub_fake_err() {
        val out = all("""
            spawn {
                awaiting evt==:a {
                    every evt==:b {
                        println(pub)    ;; no enclosing task
                    }
                }
            }
            println(1)
        """)
        assert(out == "anon : (lin 5, col 33) : pub error : expected enclosing task") { out }
    }
    @Test
    fun task5_pub_fake() {
        val out = all("""
            spawn (task () {
                set pub = 1
                awaiting evt==:a {
                    every evt==:b {
                        println(pub)
                    }
                }
            }) ()
             broadcast in :global, :b
             broadcast in :global, :b
             broadcast in :global, :a
             broadcast in :global, :b
        """, true)
        assert(out == "1\n1\n") { out }
    }
    @Test
    fun task6_pub_fake() {
        val out = all("""
            task T () {
                set pub = 10
                println(pub)
                spawn {
                    println(pub)
                    await false
                }
                nil
            }
            spawn T()
            broadcast in :global, nil
        """)
        assert(out == "10\n10\n") { out }
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

    // TOGGLE

    @Test
    fun toggle1_err() {
        val out = all("""
            toggle f() -> {
        """)
        assert(out == "anon : (lin 2, col 27) : expected expression : have \"{\"") { out }
    }
    @Test
    fun toggle2() {
        val out = all("""
            task T (v) {
                set pub = v
                toggle evt==:hide -> evt==:show {
                    println(pub)
                    every (evt is :dict) and (evt.sub==:draw) {
                        println(evt.v)
                    }
                }
            }
            spawn T (0)
            broadcast in :global, @[(:sub,:draw),(:v,1)]
            broadcast in :global, :hide
            broadcast in :global, @[(:sub,:draw),(:v,99)]
            broadcast in :global, :show
            broadcast in :global, @[(:sub,:draw),(:v,2)]
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun toggle3() {
        val out = all("""
            task T (v) {
                set pub = v
                toggle :hide -> :show {
                    println(pub)
                    every :draw {
                        println(evt.0)
                    }
                }
            }
            spawn T (0)
            broadcast in :global, tags([1], :draw, true)
            broadcast in :global, tags([], :hide, true)
            broadcast in :global, tags([99], :draw, true)
            broadcast in :global, tags([], :show, true)
            broadcast in :global, tags([2], :draw, true)
        """, true)
        assert(out == "0\n1\n2\n") { out }
    }

    // WHILE / BREAK

    @Test
    fun break1() {
        val out = all("""
            while true { {:break}
                throw(:break)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun break2() {
        val out = all("""
            while false {
                while true { {:break}
                    throw(:break)
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun break3() {
        val out = all("""
            while true { {:break2}
                while true { {:break1}
                    throw(:break2)
                }
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }

    // THROW / CATCH

    @Test
    fun catch3() {
        val out = all("""
            var x
            set x = catch :x {
                catch :2 {
                    throw(tags([10], :x, true))
                    println(9)
                }
                println(9)
            }.0
            println(x)
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun catch6_err() {
        val out = all("""
            catch err==[] {
                var x
                set x = []
                throw(x)
                println(9)
            }
            println(1)
        """, true)
        //assert(out == "anon : (lin 5, col 28) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 27) : set error : incompatible scopes\n" +
                "anon : (lin 5, col 17) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun catch7() {
        val out = all("""
            do {
                println(catch :x {
                    throw(tags([10],:x,true))
                    println(9)
                })
            }
        """, true)
        assert(out == "[10]\n") { out }
    }
    @Test
    fun catch8() {
        val out = all("""
            var x
            set x = catch :x {
                var y
                set y = catch :y {
                    throw(tags([10],:y,true))
                    println(9)
                }
                ;;println(1)
                y
            }
            println(x)
        """.trimIndent(), true)
        //assert(out == "anon : (lin 9, col 5) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 2, col 18) : set error : incompatible scopes\n" +
                "anon : (lin 5, col 9) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun while1() {
        val out = all("""
            println(catch :x { while true { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while2() {
        val out = all("""
            println(catch :x { while true { throw(tags([1],:x,true)) }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while3() {
        val out = all("""
            println(catch :2 { while true { throw(tags([1],:2,true)) }})
        """, true)
        assert(out == "[1]\n") { out }
    }
    @Test
    fun while4() {
        val out = all("""
            println(catch :x { while true {
                var x
                set x = [1] ;; memory released
                throw(tags([1],:x,true))
            }}.0)
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun while5_err() {
        val out = all("""
            println(catch :x { while true {
                var x
                set x = [1]
                throw(tags(x,:x,true))
            }})
        """.trimIndent(), true)
        //assert(out == "anon : (lin 4, col 14) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 1, col 31) : set error : incompatible scopes\n" +
                "anon : (lin 4, col 5) : throw error : uncaught exception\n") { out }
    }

    // ALL

    @Test
    fun all1() {
        val out = all("""
            task T (pos) {
                await true
                println(pos)
            }
            spawn {
                var ts = coroutines()
                do {
                    spawn in ts, T([])
                }
                await false
            }
            broadcast in :global, nil
        """, true)
        assert(out == "[]\n") { out }
    }
}
