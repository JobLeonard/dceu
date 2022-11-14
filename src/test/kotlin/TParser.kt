import org.junit.Ignore
import org.junit.Test

class TParser {

    // EXPR.VAR

    @Test
    fun expr_var () {
        val l = lexer(" x ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun expr_var_err1 () {
        val l = lexer(" { ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun expr_var_err2 () {
        val l = lexer("  ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 3) : expected expression : have end of file")
    }

    // EXPR.PARENS

    @Test
    fun expr_parens() {
        val l = lexer(" ( a ) ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun expr_parens_err() {
        val l = lexer(" ( a  ")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 7) : expected \")\" : have end of file")
    }

    // EXPR.NUM / EXPR.NIL / EXPR.BOOL

    @Test
    fun expr_num() {
        val l = lexer(" 1.5F ")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun expr_nil() {
        val l = lexer("nil")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Nil && e.tk.str == "nil")
    }
    @Test
    fun expr_true() {
        val l = lexer("true")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun expr_false() {
        val l = lexer("false")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Bool && e.tk.str == "false")
    }

    // EXPR.ECALL

    @Test
    fun expr_call1() {
        val l = lexer(" f (1.5F, x) ")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Call && e.tk.str=="f" && e.f is Expr.Acc && e.args.size==2)
    }
    @Test
    fun expr_call2() {
        val l = lexer(" f() ")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Call && e.f.tk.str=="f" && e.f is Expr.Acc && e.args.size==0)
    }
    @Test
    fun expr_call3() {
        val l = lexer(" f(x,8)() ")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Call && e.f is Expr.Call && e.args.size==0)
        assert(e.tostr() == "f(x,8)()")
    }
    @Test
    fun expr_call_err1() {
        val l = lexer("f (999 ")
        val parser = Parser(l)
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 8) : expected \")\" : have end of file")
    }
    @Test
    fun expr_call_err2() {
        val l = lexer(" f ({ ")
        val parser = Parser(l)
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 5) : expected expression : have \"{\"")
    }

    // EXPR.TUPLE

    @Test
    fun expr_tuple1() {
        val l = lexer(" [ 1.5F, x] ")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Tuple && e.args.size==2)
    }
    @Test
    fun expr_tuple2() {
        val l = lexer("[[],[1,2,3]]")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e.tostr() == "[[],[1,2,3]]")
    }
    @Test
    fun expr_tuple_err() {
        val l = lexer("[{")
        val parser = Parser(l)
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }

    // EXPR.INDEX

    @Test
    fun expr_index() {
        val l = lexer("x[10]")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Num)
    }
    @Test
    fun expr_index_err() {
        val l = lexer("x[10")
        val parser = Parser(l)
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 5) : expected \"]\" : have end of file")
    }

    // EXPRS

    @Test
    fun exprs_call() {
        val l = lexer("f ()")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.size==1 && es[0] is Expr.Call && es[0].tostr() == "f()")
        assert(es.tostr() == "f()\n")
    }
    @Test
    fun exprs_call_err() {
        val l = lexer("f")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f\n")
    }

    // EXPRS

    @Test
    fun exprs_seq1() {
        val l = lexer("; f () ; g () h()\ni() ;\n;")
        val parser = Parser(l)
        val es = parser.exprs()
        assert(es.tostr() == "f()\ng()\nh()\ni()\n") { es.tostr() }
    }
    @Test
    fun exprs_seq2() {
        val l = lexer("; f () \n (1) ; h()\ni() ;\n;")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "f()\n1\nh()\ni()\n") { es.tostr() }
        //assert(trap { parser.exprs() } == "anon : (lin 2, col 3) : call error : \"(\" in the next line")
    }
    @Test
    fun exprs_seq3() {
        val l = lexer("var v2\n[tp,v1,v2]")
        val parser = Parser(l)
        // TODO: ambiguous
        val es = parser.exprs()
        assert(es.tostr() == "var v2\n[tp,v1,v2]\n") { es.tostr() }
    }

    // EXPR.DCL

    @Test
    fun expr_dcl() {
        val l = lexer("var x")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Dcl && e.tk.str == "x")
        assert(e.tostr() == "var x")
    }
    @Test
    fun expr_dcl_err() {
        val l = lexer("var [10]")
        val parser = Parser(l)
        assert(trap { parser.exprPrim() } == "anon : (lin 1, col 5) : expected identifier : have \"[\"")
    }

    // EXPR.SET

    @Test
    fun expr_set() {
        val l = lexer("set x = [10]")
        val parser = Parser(l)
        val e = parser.exprFixs()
        assert(e is Expr.Set && e.tk.str == "set")
        assert(e.tostr() == "set x = [10]")
    }
    @Test
    fun expr_err1() {  // set number?
        val l = lexer("set 1 = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set 1 = 1")
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }
    @Test
    fun expr_err2() {  // set whole tuple?
        val l = lexer("set [1] = 1")
        val parser = Parser(l)
        //val e = parser.exprN()
        //assert(e.tostr() == "set [1] = 1")
        assert(trap { parser.exprFixs() } == "anon : (lin 1, col 1) : invalid set : invalid destination")
    }

    // IF

    @Test
    fun expr_if1() {  // set whole tuple?
        val l = lexer("if true { 1 } else { 0 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\n0\n}\n") { e.tostr() }
    }
    @Test
    fun expr_if2() {  // set whole tuple?
        val l = lexer("if true { 1 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\nnil\n}\n") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_expr_if3_noblk() {
        val l = lexer("if true 1")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else nil\n") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_expr_if4_noblk() {
        val l = lexer("if true 1 else 2")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.If)
        assert(e.tostr() == "if true 1 else 2\n") { e.tostr() }
    }

    // DO

    @Test
    fun expr_do1() {  // set whole tuple?
        val l = lexer("do{}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Block && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_do2() {  // set whole tuple?
        val l = lexer("do { var a; set a=1; print(a) }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Block && e.es.size==3)
        assert(e.tostr() == "do {\nvar a\nset a = 1\nprint(a)\n}\n") { e.tostr() }
    }

    // FUNC

    @Test
    fun expr_func1() {
        val l = lexer("func () {}")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Func && e.args.size==0)
        assert(e.tostr() == "func () {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_func2() {
        val l = lexer("func (a,b) { 10 }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Func && e.args.size==2)
        assert(e.tostr() == "func (a,b) {\n10\n}\n") { e.tostr() }
    }

    // WHILE

    @Test
    fun expr_while1() {
        val l = lexer("while true { }")
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.While && e.body.es[0] is Expr.Nil)
        assert(e.tostr() == "while true {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_while2_err() {
        val l = lexer("while {")
        val parser = Parser(l)
        assert(trap { parser.exprBins() } == "anon : (lin 1, col 7) : expected expression : have \"{\"")
    }

    // THROW / CATCH

    @Test
    fun todo_catch1() {
        val l = lexer("""
            set x = catch @e1 {
                throw @e1
                throw (@e1,10)
                throw (@e1)
            }
            
        """)
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e.tostr() == "set x = catch 1 {\nthrow (1,nil)\nthrow (1,10)\nthrow (1,nil)\n}\n") { e.tostr() }
    }

    // NATIVE

    @Test
    fun native1() {
        val l = lexer("""
            native {
                printf("xxx\n");
            }
        """.trimIndent())
        val parser = Parser(l)
        val e = parser.exprPrim()
        assert(e is Expr.Nat)
        assert(e.tostr() == "native {\n    printf(\"xxx\\n\");\n}") { "."+e.tostr()+"." }
    }

    // BINARY / UNARY / OPS

    @Test
    fun bin1_err() {
        val l = lexer("(10+)")
        val parser = Parser(l)
        assert(trap { parser.exprBins() } == "anon : (lin 1, col 5) : expected expression : have \")\"")
    }
    @Test
    fun bin2() {
        val l = lexer("10+1")
        val parser = Parser(l)
        val e = parser.exprBins()
        assert(e is Expr.Call)
        assert(e.tostr() == "{+}(10,1)") { e.tostr() }
    }
    @Test
    fun bin3() {
        val l = lexer("10/=1")
        val parser = Parser(l)
        val e = parser.exprBins()
        assert(e is Expr.Call)
        assert(e.tostr() == "{/=}(10,1)") { e.tostr() }
    }
    @Test
    fun pre1() {
        val l = lexer("- not - 1")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{-}(if {-}(1) {\nfalse\n}\nelse {\ntrue\n}\n)") { e.tostr() }
    }
    @Test
    fun bin4() {
        val l = lexer("1 + 2 + 3")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e is Expr.Call)
        assert(e.tostr() == "{+}({+}(1,2),3)") { e.tostr() }
    }
    @Test
    @Ignore
    fun todo_bin5_not_or_and() {    // blk->dcl->set->if
        val l = lexer("not true and false or true")
        val parser = Parser(l)
        val e = parser.expr()
        assert(e.tostr() == "if if if true {\nfalse\n}\nelse {\ntrue\n}\n {\nfalse\n}\nelse {\nfalse\n}\n {\ntrue\n}\nelse {\ntrue\n}\n") { e.tostr() }
    }

    // TASK / YIELD / RESUME

    @Test
    fun task1() {
        val l = lexer("""
            set t = task (v) {
                set v = yield (1) 
                yield (2) 
            }
            spawn t
            set v = resume a(1)
            resume a(2)
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == """
            set t = task (v) {
            set v = yield (1)
            yield (2)
            }

            spawn t
            set v = resume a(1)
            resume a(2)
            
        """.trimIndent()) { e.tostr() }
    }
    @Test
    fun task2_err() {
        val l = lexer("""
            resume a
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 9) : expected invalid resume : expected call : have end of file")
    }
    @Test
    fun task3_err() {
        val l = lexer("""
            yield
            1
        """.trimIndent())
        val parser = Parser(l)
        //assert(trap { parser.expr() } == "anon : (lin 2, col 1) : expected \"(\" : have \"1\"")
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
    }
    @Test
    fun task4_err() {
        val l = lexer("""
            yield
            (1)
        """.trimIndent())
        val parser = Parser(l)
        assert(trap { parser.expr() } == "anon : (lin 1, col 1) : yield error : line break before expression")
    }

    // DEFER

    @Test
    fun defer() {
        val l = lexer("defer {}")
        val parser = Parser(l)
        val e = parser.exprs()
        assert(e.tostr() == "defer {\nnil\n}\n\n") { e.tostr() }
    }

    // MISC

    @Test
    fun misc1() {
        val l = lexer("""
        """)
        val parser = Parser(l)
        val e = parser.exprs()
        //println(e.tostr())
    }
}
