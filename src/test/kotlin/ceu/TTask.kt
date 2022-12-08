package ceu

import org.junit.Test

class TTask {

    // TASK / COROUTINE / RESUME / YIELD

    @Test
    fun task1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield (v+1) 
                println(v)          ;; 3
                set v = yield v+1 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = coroutine t
            var v
            set v = resume a(1)
            println(v)              ;; 2
            set v = resume a(v+1)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun task2_err() {
        val out = all("""
            coroutine func () {nil}
        """.trimIndent())
        assert(out == "anon : (lin 1, col 11) : coroutine error : expected task\n") { out }
    }
    @Test
    fun task3_err() {
        val out = all("""
            var f
            resume f()
        """.trimIndent())
        assert(out == "anon : (lin 2, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task4_err() {
        val out = all("""
            var co
            set co = coroutine task () {nil}
            resume co()
            resume co()
        """.trimIndent())
        assert(out == "anon : (lin 4, col 8) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task5_err() {
        val out = all("""
            var co
            set co = coroutine task () { nil
            }
            resume co()
            resume co(1,2)
        """)
        assert(out == "anon : (lin 6, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task6() {
        val out = all("""
            var co
            set co = coroutine task (v) {
                set v = yield nil 
                println(v)
            }
            resume co(1)
            resume co(2)
        """)
        assert(out == "2\n") { out }
    }
    @Test
    fun task7() {
        val out = all("""
            var co
            set co = coroutine task (v) {
                println(v)
            }
            println(1)
            resume co(99)
            println(2)
        """)
        assert(out == "1\n99\n2\n") { out }
    }
    @Test
    fun tak8_err() {
        val out = all("""
            var xxx
            resume xxx() ;;(xxx(1))
        """)
        assert(out == "anon : (lin 3, col 20) : resume error : expected yielded task\n") { out }
    }
    @Test
    fun task9_mult() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                println(x,y)
            }
            resume co(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun task10_err() {
        val out = all("""
            var co
            set co = coroutine task () {
                yield nil
            }
            resume co()
            resume co(1,2)
        """)
        assert(out.contains("bug found : not implemented : multiple arguments to resume")) { out }
    }
    @Test
    fun task11_class() {
        val out = all("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            resume (coroutine T) (1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun task12_tuple_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield nil
            }
            resume (coroutine T) ()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun task13_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(3)
                }
                println(1)
                yield nil   ;; never awakes
                println(2)
            }
            println(0)
            resume (coroutine T) ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }
    @Test
    fun yield14_err() {
        val out = all("""
            yield nil
        """)
        assert(out == "anon : (lin 2, col 13) : yield error : expected enclosing task") { out }
    }
    @Test
    fun yield15_err() {
        val out = all("""
            task () {
                func () {
                    yield nil
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : expected enclosing task") { out }
    }
    @Test
    fun task16_nest() {
        val out = all("""
            spawn (task (v1) {
                println(v1)
            }) (1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun task17_nest() {
        val out = all("""
            spawn task (v1) {
                spawn task (v2) {
                    spawn task (v3) {
                        println(v1,v2,v3)
                    }(3)
                }(2)
            }(1)
        """)
        assert(out == "1\t2\t3\n") { out }
    }
    @Test
    fun task18_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(3)
                }
                println(1)
                yield nil   ;; never awakes
                println(2)
            }
            var t
            set t = coroutine T
            println(0)
            resume t ()
            println(4)
        """)
        assert(out == "0\n1\n4\n3\n") { out }
    }

    // SPAWN

    @Test
    fun spawn1() {
        val out = all("""
            var t
            set t = task (v) {
                println(v)          ;; 1
                set v = yield (v+1) 
                println(v)          ;; 3
                set v = yield v+1 
                println(v)          ;; 5
                v+1
            }
            var a
            set a = spawn t(1)
            println(tags(a))
            var v
            set v = resume a(3)
            println(v)              ;; 4
            set v = resume a(v+1)
            println(v)              ;; 6
        """, true)
        assert(out == "1\n:coro\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun spawn2() {
        val out = all("""
            var T
            set T = task (x,y) {
                println(x,y)
            }
            spawn T(1,2)
        """)
        assert(out == "1\t2\n") { out }
    }
    @Test
    fun spawn3_err() {
        val out = all("""
            spawn (func () {nil}) ()
        """)
        assert(out == "anon : (lin 2, col 20) : coroutine error : expected task\n") { out }
    }
    @Test
    fun spawn4_err() {
        val out = all("""
            spawn (func () {nil})
        """)
        assert(out == "anon : (lin 3, col 9) : invalid spawn : expected call") { out }
    }
    @Test
    fun spawn5() {
        val out = all("""
            var t
            set t = task () {
                println(1)
                do {
                    println(2)
                    yield nil
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn t()
            resume co()
            println(5)
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun spawn6_err() {
        val out = all("""
            var x
            set x = do {
                spawn (task() {nil}) ()
            }
            println(2)
        """)
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun spawn67_err() {
        val out = all("""
            var t
            set t = task () {
                nil
            }
            var co
            set co = if true { spawn t() } else { nil }
        """)
        assert(out == "anon : (lin 7, col 32) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun spawn7_err() {
        val out = all("""
            var f
            set f = func () {
                spawn t()
            }
            var t
            set t = task () { nil }
            f()
        """)
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun spawn8_err() {
        val out = all("""
            var T
            set T = task () {
                spawn (task :fake () {
                    nil
                }) ()
            }
            spawn T()
        """)
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun spawn6() {
        val out = all("""
            var x
            set x = do {
                spawn (task() {println(1)}) ()
                nil
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun spawn67() {
        val out = all("""
            var t
            set t = task () {
                println(1)
                do {
                    println(2)
                    yield nil
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn (if true { t } else { nil }) ()
            resume co()
            println(5)
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun spawn7() {
        val out = all("""
            var f
            set f = func () {
                t
            }
            var t
            set t = task () {
                println(1)
                do {
                    println(2)
                    yield nil
                    println(3)
                }
                println(4)
            }
            var co
            set co = spawn f() ()
            resume co()
            println(5)
        """)
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun spawn9_err() {
        val out = all("""
            var T
            set T = task () {
                (task :fake () {
                    nil
                })
            }
            spawn T()
        """)
        assert(out == "anon : (lin 4, col 18) : return error : incompatible scopes\n") { out }
    }

    // THROW

    @Test
    fun throw1() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                throw :e2
            }
            catch :e2 {
                resume co(1,2)
                println(99)
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun throw2() {
        val out = all("""
            var co
            set co = coroutine task (x,y) {
                yield nil
                throw :e2
            }
            catch :e2 {
                resume co(1,2)
                println(1)
                resume co()
                println(2)
            }
            println(3)
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun throw3_err() {
        val out = all("""
            var T
            set T = task () {
                spawn task () {
                    yield nil
                    throw :error 
                }()
                yield nil
            }
            spawn in coroutines(), T()
            broadcast in :global, nil
        """)
        assert(out == "anon : (lin 6, col 21) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun throw4_err() {
        val out = all("""
            broadcast in :task, nil
        """)
        assert(out == "anon : (lin 2, col 26) : broadcast error : invalid target\n") { out }
    }
    @Test
    fun throw5_err() {
        val out = all("""
            spawn (task :fake () {
                broadcast in :task, nil
            }) ()
        """)
        assert(out == "anon : (lin 3, col 30) : broadcast error : invalid target\n") { out }
    }

    // BCAST / BROADCAST

    @Test
    fun bcast0() {
        val out = all("""
            println(broadcast in :global, 1)
        """)
        assert(out == "nil\n") { out }
    }
    @Test
    fun bcast001() {
        val out = ceu.all(
            """
            spawn task () {
                println(1)
                yield nil
                println(2)
            }()
             broadcast in :global, nil
        """
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bcast01() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v, evt)
                set v = yield nil
                println(v, evt)
            }
            var co
            set co = coroutine tk
             broadcast in :global, 1
             broadcast in :global, 2
             broadcast in :global, 3
        """)
        assert(out == "nil\t1\nnil\t2\n") { out }
    }
    @Test
    fun bcast1() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                println(evt)
                set v = yield nil
                println(v)
                println(evt)
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
             broadcast in :global, 1
             broadcast in :global, 2
             broadcast in :global, 3
        """)
        //assert(out == "nil\n1\nnil\n1\nnil\n2\nnil\n2\n") { out }
        assert(out.contains("nil\n1\nnil\n1\nnil\n2\nnil\npointer: 0x")) { out }
    }
    @Test
    fun bcast2() {
        val out = all("""
            var co1
            set co1 = coroutine task () {
                var co2
                set co2 = coroutine task () {
                    yield nil
                    println(2)
                }
                resume co2 ()
                yield nil
                println(1)
            }
            resume co1 ()
             broadcast in :global, nil
        """)
        assert(out == "2\n1\n") { out }
    }
    @Test
    fun bcast3() {
        val out = all("""
            var co1
            set co1 = coroutine task () {
                var co2
                set co2 = coroutine task () {
                    yield nil
                    throw :error
                }
                resume co2 ()
                yield nil
                println(1)
            }
            resume co1 ()
             broadcast in :global, nil
        """)
        assert(out == "anon : (lin 7, col 21) : throw error : uncaught exception\n") { out }
    }
    @Test
    fun bcast4() {
        val out = all("""
            var tk
            set tk = task () {
                do {
                    println(evt)
                    yield nil
                    println(evt)
                }
                do {
                    println(evt)
                    yield nil
                    println(evt)
                }
            }
            var co
            set co = coroutine tk
             broadcast in :global, 1
             broadcast in :global, 2
             broadcast in :global, 3
             broadcast in :global, 4
        """)
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun bcast5() {
        val out = all("""
            var tk
            set tk = task (v) {
                println(v)
                yield nil
                println(evt)
            }
            var co
            set co = coroutine tk
            resume co(1)
             broadcast in :global, 2
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun bcast6() {
        val out = all("""
            func () {
                 broadcast in :global, 1
            }
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun bcast7() {
        val out = all("""
            var tk
            set tk = task () {
                yield nil
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            do {
                 broadcast in :global, 1
                 broadcast in :global, 2
                 broadcast in :global, 3
            }
        """)
        //assert(out == "2\n2\n") { out }
        assert(out.contains("2\npointer: 0x")) { out }
    }
    @Test
    fun bcast8() {
        val out = all("""
            var tk
            set tk = task (v) {
                do { var ok; set ok=true; while ok { yield nil; if tags(evt)/=:pointer { set ok=false } else { nil } } }
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            func () {
                 broadcast in :global, 1
                 broadcast in :global, 2
                 broadcast in :global, 3
            }()
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun bcast9() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                set v = yield nil
                throw :1                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==:1 {
                func () {
                    println(1)
                     broadcast in :global, 1
                    println(2)
                     broadcast in :global, 2
                    println(3)
                     broadcast in :global, 3
                }()
            }
            println(99)
        """
        )
        assert(out == "1\n2\n99\n") { out }
    }
    @Test
    fun bcast10() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                println(v)
                do { var ok; set ok=true; while ok { yield nil; if tags(evt)/=:pointer { set ok=false } else { nil } } }
                ;;yield nil
                println(evt)                
                do { var ok2; set ok2=true; while ok2 { yield nil; if tags(evt)/=:pointer { set ok2=false } else { nil } } }
                ;;yield nil
                println(evt)                
            }
            var co1
            set co1 = coroutine tk
            var co2
            set co2 = coroutine tk
            catch err==:1 {
                func () {
                    println(1)
                    resume co1(10)
                    resume co2(10)
                    println(2)
                    broadcast in :global, [20]
                    println(3)
                    broadcast in :global, @[(30,30)]
                }()
            }
        """
        )
        assert(out == "1\n10\n10\n2\n[20]\n[20]\n3\n@[(30,30)]\n@[(30,30)]\n") { out }
    }
    @Test
    fun bcast11_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (v) {
                do {
                    set v = evt
                }
            }
            var co
            set co = coroutine tk
             broadcast in :global, []
        """
        )
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }

    // BCAST / SCOPE

    @Test
    fun bcast_in1() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                do { var ok; set ok=true; while ok { yield nil; if tags(evt)/=:pointer { set ok=false } else { nil } } }
                ;;yield nil
                println(v)
            }
            var t1
            set t1 = spawn T (1)
            var t2
            set t2 = spawn T (2)
             broadcast in t1, nil
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun bcast_in2_err() {
        val out = ceu.all(" broadcast in nil, nil")
        assert(out == "anon : (lin 1, col 15) : broadcast error : invalid target\n") { out }
    }
    @Test
    fun bcast_in3_err() {
        val out = ceu.all(" broadcast in :xxx, nil")
        assert(out == "anon : (lin 1, col 15) : broadcast error : invalid target\n") { out }
    }
    @Test
    fun bcast_in4() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                yield nil
                println(v)
            }
            var t1
            set t1 = spawn T (1)
            do {
                var t2
                set t2 = spawn T (2)
                broadcast in :local, nil
            }
        """
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun bcast_in5() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn (task () {
                    yield nil
                    println(:ok)
                }) ()
                broadcast in :global, :ok
            }
            spawn T (2)
        """
        )
        assert(out == ":ok\n") { out }
    }
    @Test
    fun bcast_in6() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn (task :fake () {
                    yield nil
                    println(v, evt)
                }) ()
                spawn (task :fake () {
                    do {
                        broadcast in :task, :ok
                    }
                }) ()
                yield nil
            }
            spawn (task () {
                yield nil
                println(999)
            }) ()
            spawn T (1)
            spawn T (2)
        """
        )
        assert(out == "1\t:ok\n2\t:ok\n") { out }
    }

    // POOL

    @Test
    fun pool0() {
        val out = ceu.all(
            """
            var T
            set T = task () { nil }
            var x
            spawn in coroutines(), T(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun pool01() {
        val out = ceu.all(
            """
            var x
            spawn in coroutines(), (task(){nil})(x)
            println(0)
        """
        )
        assert(out == "0\n") { out }
    }
    @Test
    fun pool1() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                println(v)
                yield nil
                println(evt)
            }
            do {
                spawn in ts, T(1)
            }
             broadcast in :global, 2
        """)
        assert(out == ":coros\n1\n2\n") { out }
    }
    @Test
    fun pool2_leak() {
        val out = all("""
            var T
            set T = task () {
                [1,2,3]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool4_defer() {
        val out = all("""
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            println(0)
        """)
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun pool5_scope() {
        val out = all("""
            do {
                var ts
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                    set v = yield nil
                    println(v)
                }
                spawn in ts, T(1)
            }
             broadcast in :global, 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool6_terminate() {
        val out = all("""
            do {
                var ts
                set ts = coroutines()
                var T
                set T = task (v) {
                    println(v)
                }
                spawn in ts, T(1)
                while t in ts {
                    throw 1     ;; never reached
                }
            }
             broadcast in :global, 2
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool7_leak() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pool8_err() {
        val out = all("""
            while x in nil {
                nil
            }
        """)
        assert(out == "anon : (lin 2, col 24) : while error : expected coroutines\n") { out }
    }
    @Test
    fun pool9_term() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                println(1)
                 broadcast in :global, 1
            }
            println(2)
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun pool10_term() {
        val out = all("""
            var T
            set T = task () {
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                println(1)
                 broadcast in :global, 1
                while yyy in ts {
                    println(2)
                }
            }
            println(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun pool11_err_scope() {
        val out = all("""
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var yyy
            while xxx in ts {
                set yyy = xxx
            }
        """)
        assert(out == "anon : (lin 9, col 21) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pool12_err_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                var yyy
                while zzz in ts {
                    set yyy = zzz
                }
                set yyy = xxx
            }
        """
        )
        assert(out == "anon : (lin 10, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun pool13_scope() {
        val out = ceu.all(
            """
            var T
            set T = task () { yield nil }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            while xxx in ts {
                var yyy
                while zzz in ts {
                    nil
                }
                set yyy = xxx
            }
            println(1)
        """
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun pool14_max_err() {
        val out = ceu.all(
            """
            coroutines(0)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n") { out }
    }
    @Test
    fun pool15_max_err() {
        val out = ceu.all(
            """
            coroutines(nil)
        """
        )
        assert(out == "anon : (lin 2, col 24) : coroutines error : expected positive number\n") { out }
    }
    @Test
    fun pool16_max() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(1)
            var T
            set T = task () { yield nil }
            var ok1
            set ok1 = spawn in ts, T()
            var ok2
            set ok2 = spawn in ts, T()
            broadcast in :global, nil
            var ok3
            set ok3 = spawn in ts, T()
            var ok4
            set ok4 = spawn in ts, T()
            println(ok1, ok2, ok3, ok4)
        """
        )
        assert(out == "true\tfalse\ttrue\tfalse\n") { out }
    }
    @Test
    fun pool17_term() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                println(10)
                defer {
                    println(20)
                }
                do { var ok1; set ok1=true; while ok1 { yield nil; if tags(evt)/=:pointer { set ok1=false } else { nil } } }
                ;;yield nil
                if v {
                    do { var ok; set ok=true; while ok { yield nil; if tags(evt)/=:pointer { set ok=false } else { nil } } }
                    ;;yield nil
                } else {
                    nil
                }
            }
            println(0)
            spawn in ts, T(false)
            spawn in ts, T(true)
            println(1)
             broadcast in :global, @[]
            println(2)
             broadcast in :global, @[]
            println(3)
        """
        )
        assert(out == "0\n10\n10\n1\n20\n2\n20\n3\n") { out }
    }
    @Test
    fun pool18_throw() {
        val out = ceu.all(
            """
            println(1)
            catch err==:ok {
                println(2)
                spawn task () {
                    println(3)
                    yield nil
                    println(6)
                    throw :ok
                } ()
                spawn task () {
                    catch :ok {
                        println(4)
                        yield nil
                    }
                    println(999)
                } ()
                println(5)
                 broadcast in :global, nil
                println(9999)
            }
            println(7)
        """
        )
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun pool19_throw() {
        val out = ceu.all(
            """
            var T
            set T = task (v) {
                spawn task () {
                    println(v)
                    yield nil
                    println(v)
                } ()
                while true { yield nil }
            }
            spawn T(1)
            spawn T(2)
             broadcast in :global, nil
        """
        )
        assert(out == "1\n2\n1\n2\n") { out }
    }
    @Test
    fun pool20_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield nil
                        if v == 1 {
                            throw :ok
                        } else {
                            nil
                        }
                        while true { yield nil }
                    } ()
                    while true { yield nil }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
             broadcast in :global, nil
             broadcast in :global, nil
            println(999)
        """
        )
        assert(out == "1\n1\n999\n2\n") { out }
    }
    @Test
    fun pool21_throw() {
        val out = ceu.all(
            """
            var ts
            set ts = coroutines(2)
            var T
            set T = task (v) {
                defer {
                    println(v)
                }
                catch err==:ok {
                    spawn task () {
                        yield nil
                        if v == 2 {
                            throw :ok
                        } else {
                            nil
                        }
                        while true { yield nil }
                    } ()
                    while true { yield nil }
                }
                println(v)
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            broadcast in :global, nil
            broadcast in :global, nil
            println(999)
        """
        )
        assert(out == "2\n2\n999\n1\n") { out }
    }
    @Test
    fun pool22() {
        val out = all("""
            var ts
            set ts = coroutines()
            println(tags(ts))
            var T
            set T = task (v) {
                set pub = v
                set v = yield nil
            }
            spawn in ts, T(1)
            spawn in ts, T(2)
            
            while t1 in ts {
                while t2 in ts {
                    println(t1.pub, t2.pub)
                }
            }
             broadcast in :global, 2
        """)
        assert(out == ":coros\n1\t1\n1\t2\n2\t1\n2\t2\n") { out }
    }

    // EVT

    @Test
    fun evt_hld1_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                set xxx = evt
            }
            var co
            set co = coroutine tk
             broadcast in :global, []
        """
        )
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun evt_hld2_err() {
        val out = ceu.all(
            """
            var tk
            set tk = task (xxx) {
                yield nil
                set xxx = evt
            }
            var co
            set co = coroutine tk
             broadcast in :global, 1
             broadcast in :global, []
        """
        )
        //assert(out == "anon : (lin 5, col 17) : return error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 5, col 21) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun evt_hld3() {
        val out = ceu.all(
        """
            var fff
            set fff = func (x) { x }
            spawn task () {
                yield nil
                while evt[:type]/=:x {
                    yield nil
                }
                println(99)
            }()
            println(1)
             broadcast in :global, @[(:type,:y)]
            println(2)
             broadcast in :global, @[(:type,:x)]
            println(3)
        """)
        assert(out == "1\n2\n99\n3\n") { out }
    }
    @Test
    fun evt_hld4() {
        val out = ceu.all(
            """
            var fff
            set fff = func (x) { x }
            spawn task () {
                println(1)
                do {
                    println(2)
                    yield nil
                    println(3)
                }
                println(4)
                fff(evt[:type])
                println(99)
            }()
            broadcast in :global, @[(:type,:y)]
            broadcast in :global, @[(:type,:x)]
        """
        )
        //assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\n2\n3\n4\n99\n") { out }
    }
    @Test
    fun evt45() {
        val out = ceu.all(
            """
            spawn task () {
                println(111)
                yield nil
                println(222)
            }()
            println(1)
            broadcast in :global, nil
            println(2)
        """
        )
        assert(out == "111\n1\n222\n2\n") { out }
    }
    @Test
    fun evt5() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    println(evt)
                    yield nil
                }
            }()
            broadcast in :global, @[]
        """
        )
        assert(out == "nil\n@[]\n") { out }
    }
    @Test
    fun evt6() {
        val out = ceu.all(
            """
            spawn task () {
                while (true) {
                    do {
                        yield nil
                    }
                    println(evt)
                }
            }()
            broadcast in :global, @[]
        """
        )
        assert(out == "@[]\n") { out }
    }
    @Test
    fun evt7_err() {
        val out = ceu.all(
            """
            var x
            set x = []
            broadcast in :global, x
        """
        )
        assert(out == "anon : (lin 4, col 13) : set error : incompatible scopes\n") { out }
    }

    // PUB

    @Test
    fun pub1_err() {
        val out = all("""
            var a
            a.pub
        """, true)
        assert(out == "anon : (lin 3, col 15) : pub error : expected coroutine\n") { out }
    }
    @Test
    fun pub2_err() {
        val out = all("""
            pub
        """, true)
        assert(out == "anon : (lin 2, col 13) : pub error : expected enclosing task") { out }
    }
    @Test
    fun pub3() {
        val out = all("""
            var t
            set t = task (v1) {
                set pub = v1
                var v2
                set v2 = yield nil
                set pub = pub + v2
            }
            var a
            set a = coroutine t
            println(a.pub)
            resume a(1)
            println(a.pub)
            resume a(2)
            println(a.pub)
        """, true)
        assert(out == "nil\n1\n3\n") { out }
    }
    @Test
    fun pub4_err() {
        val out = all("""
            var t
            set t = task () {
                set pub = []
            }
            var x
            do {
                var a
                set a = coroutine t
                resume a()
                set x = a.pub
            }
            println(x)
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 11, col 27) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun pub5() {
        val out = all("""
            var t
            set t = task () {
                set pub = 10
            }
            var a
            set a = coroutine t
            resume a()
            println(a.pub + a.pub)
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun pub56_pool() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]  ;; valgrind test
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pub562_pool() {
        val out = all("""
            var T
            set T = task () {
                pub ;; useless test
                nil
            }
            spawn T()
            println(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun pub6_pool() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while t in ts {
                println(t.pub[0]+t.pub[0])
            }
        """, true)
        assert(out == "20\n") { out }
    }
    @Test
    fun pub7_pool_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            var x
            while t in ts {
                set x = t.pub   ;; TODO: incompatible scope
            }
            println(999)
        """)
        //assert(out == "20\n") { out }
        assert(out == "anon : (lin 12, col 27) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun pub8_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = 1
                spawn (task :fake () {
                    println(pub)
                }) ()
            }) ()
        """, true)
        assert(out == "1\n") { out }
    }
    @Test
    fun pub9_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = []
                var x
                spawn (task :fake () {
                    set x = pub
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "anon : (lin 6, col 29) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun pub9_10_fake_task() {
        val out = all("""
            spawn (task () {
                set pub = [10]
                var x
                spawn (task :fake () {
                    set x = pub[0]
                }) ()
                println(x)
            }) ()
        """, true)
        assert(out == "10\n") { out }
    }
    @Test
    fun pub10_fake_err() {
        val out = all("""
            spawn (task :fake () {
                pub
            }) ()
        """, true)
        assert(out == "anon : (lin 3, col 17) : pub error : expected enclosing task") { out }
    }
    @Test
    fun pub11_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var y
            do {
                var t
                set t = coroutine(T)
                resume t ()
                var x
                set x = t.pub  ;; pub expose
                set y = t.pub  ;; incompatible scopes
            }
            println(999)
        """)
        //assert(out == "anon : (lin 14, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "anon : (lin 13, col 27) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun pub12_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var t
            set t = coroutine T
            resume t()
            var x
            set x = t.pub   ;; no expose
        """)
        assert(out == "anon : (lin 11, col 23) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun pub13_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var t
            set t = coroutine T
            resume t()
            println(t.pub)   ;; no expose
        """)
        assert(out == "anon : (lin 10, col 23) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun todo_pub14_index_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [[@[(:x,10)]]]
                yield nil
            }
            var t
            set t = coroutine T
            resume t()
            println(t.pub[0][:x])   ;; no expose
        """)
        assert(out == "anon : (lin 10, col 27) : invalid index : cannot expose dynamic public field\n") { out }
    }

    // STATUS

    @Test
    fun status1_err() {
        val out = all("""
            var a
            a.status
        """, true)
        assert(out == "anon : (lin 3, col 15) : status error : expected coroutine\n") { out }
    }
    @Test
    fun status2_err() {
        val out = all("""
            status
        """, true)
        assert(out == "anon : (lin 2, col 13) : status error : expected enclosing task") { out }
    }
    @Test
    fun status3_err() {
        val out = all("""
            var t
            set t = task () {
                set status = nil     ;; error: cannot assign to status
            }
        """, true)
        assert(out == "anon : (lin 4, col 17) : invalid set : invalid destination") { out }
    }
    @Test
    fun status4() {
        val out = all("""
            var t
            set t = task () {
                println(10, status)
                yield nil
                println(20, status)
            }
            var a
            set a = coroutine t
            println(1, a.status)
            resume a()
            println(2, a.status)
            resume a()
            println(3, a.status)
        """)
        //assert(out == "anon : (lin 11, col 25) : set error : incompatible scopes\n") { out }
        //assert(out == "anon : (lin 11, col 21) : set error : incompatible scopes\n") { out }
        assert(out == "1\t:yielded\n10\t:resumed\n2\t:yielded\n20\t:resumed\n3\t:terminated\n") { out }
    }

    // TOGGLE

    @Test
    fun todo_toggle1() { // should be rt error
        val out = all("""
            var T
            set T = task () {
                yield nil
                println(10)
            }
            var t
            set t = spawn T()
            toggle t (false)
            resume t ()
        """)
        assert(out.contains("Assertion `ceu_coro->Bcast.status==CEU_CORO_STATUS_YIELDED || (ceu_coro->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt==&CEU_EVT_CLEAR)' failed")) { out }
    }
    @Test
    fun toggle2_err() {
        val out = all("""
            toggle 1 (true)
        """)
        assert(out == "anon : (lin 2, col 20) : toggle error : expected yielded/toggled coroutine\n") { out }
    }
    @Test
    fun toggle3_coro() {
        val out = all("""
            var T
            set T = task () {
                yield nil
                println(10)
            }
            var t
            set t = spawn T()
            toggle t (false)
            println(1)
            broadcast in :global, nil
            toggle t (true)
            println(2)
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun toggle4_coros() {
        val out = all("""
            var T
            set T = task () {
                yield nil
                println(10)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            toggle ts (false)
            println(1)
            broadcast in :global, nil
            toggle ts (true)
            println(2)
            broadcast in :global, nil
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun toggle5_defer() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(10)
                }
                yield nil
                println(999)
            }
            var t
            set t = spawn T()
            toggle t (false)
            println(1)
            broadcast in :global, nil
            println(2)
        """)
        assert(out == "1\n2\n10\n") { out }
    }
    @Test
    fun toggle6_defer_coros() {
        val out = all("""
            var T
            set T = task () {
                defer {
                    println(10)
                }
                yield nil
                println(999)
            }
            var ts
            set ts = coroutines()
            spawn in ts, T()
            toggle ts (false)
            println(1)
            broadcast in :global, nil
            println(2)
        """)
        assert(out == "1\n2\n10\n") { out }
    }

    // ESCAPE

    @Test
    fun esc1() {
        val out = all("""
            var f
            set f = func () {
                coroutine(task() {nil})
            }
            println(f())
        """, true)
        assert(out == "anon : (lin 4, col 17) : return error : incompatible scopes\n") { out }
    }
    @Test
    fun esc2() {
        val out = all("""
            var T
            set T = task () { nil }
            var xxx
            do {
                var t
                set t = coroutine T
                set xxx = t ;; error
            }
        """)
        assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
    }

    // TRACK

    @Test
    fun track1_err() {
        val out = all("""
            track nil
        """)
        assert(out == "anon : (lin 2, col 19) : track error : expected coroutine\n") { out }
    }
    @Test
    fun track2() {
        val out = all("""
            var T
            set T = task () { nil }
            var t
            set t = coroutine T
            var x
            set x = track t
            println(t, x)
        """)
        assert(out.contains("track: 0x")) { out }
    }
    @Test
    fun track3_err() {
        val out = all("""
            var T
            set T = task () { nil }
            var t
            set t = coroutine T
            resume t()
            var x
            set x = track t ;; error: dead coro
            println(x)
        """)
        assert(out == "anon : (lin 8, col 27) : track error : expected unterminated coroutine\n") { out }
    }
    @Test
    fun track4() {
        val out = all("""
            var T
            set T = task () {
                set pub = 10
                yield nil
            }
            var t
            set t = coroutine T
            var x
            set x = track t
            println(x.pub) 
            resume t()
            println(x.pub) 
        """)
        assert(out == "nil\n10\n") { out }
    }
    @Test
    fun track5_err() {
        val out = all("""
            var x
            set x = nil
            println(x.pub)  ;; not coro/track 
        """)
        assert(out == "anon : (lin 4, col 23) : pub error : expected coroutine\n") { out }
    }
    @Test
    fun track5_err2() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var t
            set t = coroutine T
            resume t()
            var x
            set x = track t
            println(x.pub)      ;; expose
        """)
        assert(out == "anon : (lin 12, col 23) : invalid pub : cannot expose dynamic public field\n") { out }
    }
    @Test
    fun track6_err() {
        val out = all("""
            var T
            set T = task () { nil }
            var x
            do {
                var t
                set t = coroutine T
                set x = track t         ;; error scope
            }
        """)
        assert(out == "anon : (lin 8, col 21) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun track7() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var t
            set t = coroutine T
            resume t ()
            var x
            set x = track t
            println(x.pub[0])
            broadcast in :global, nil
            println(x.status)
        """)
        assert(out == "10\n:destroyed\n") { out }
    }
    @Test
    fun track8_err() {
        val out = all("""
            var T
            set T = task () {
                set pub = [10]
                yield nil
            }
            var x
            do {
                var t
                set t = coroutine T
                resume t ()
                set x = track t         ;; scope x < t
                println(x.pub[0])
            }
            println(x)
        """)
        //assert(out == "10\nnil\n") { out }
        assert(out == "anon : (lin 12, col 21) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun track9() {
        val out = all("""
            var T
            set T = task (v) {
                set pub = [v]
                yield nil
            }
            var x
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            while t in ts {
                set x = track(t)
            }
            println(x.pub[0])   ;; 2
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "2\n:destroyed\n") { out }
    }
    @Test
    fun track10() {
        val out = all("""
            var T
            set T = task (v) {
                set pub = [v]
                yield nil
            }
            var x
            var ts
            set ts = coroutines()
            do {
                spawn in ts, T(1)
                spawn in ts, T(2)
                while t in ts {
                    set x = track(t)    ;; track(t) hold in
                }
                println(x.pub[0])   ;; 2
                broadcast in :global, nil
                println(x.status)   ;; nil
            }
        """)
        assert(out == "2\n:destroyed\n") { out }
    }
    @Test
    fun track11_err() {
        val out = all("""
            var T
            set T = task (v) {
                set pub = [v]
                yield nil
            }
            var x
            do {
                var ts
                set ts = coroutines()
                spawn in ts, T(1)
                while t in ts {
                    set x = t       ;; err: escope 
                }
            }
        """)
        assert(out == "anon : (lin 13, col 25) : set error : incompatible scopes\n") { out }
    }
    @Test
    fun track12_throw() {
        val out = all("""
            var T
            set T = task (v) {
                set pub = [v]
                yield nil
            }
            var ts
            set ts = coroutines()
            spawn in ts, T(1)
            spawn in ts, T(2)
            var x = catch true {
                while t in ts {
                    throw track(t)
                }
            }
            println(x.pub[0])   ;; 2
            broadcast in :global, nil
            println(x.status)   ;; nil
        """)
        assert(out == "2\n:destroyed\n") { out }
    }
}
