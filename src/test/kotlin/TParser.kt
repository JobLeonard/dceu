import org.junit.Test

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
    } catch (e: Throwable) {
        return e.message!!
    }

}
class TParser {

    // EXPR.VAR

    @Test
    fun expr_var () {
        val lexer = Lexer("anon", " x ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun expr_var_err1 () {
        val lexer = Lexer("anon", " { ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 2): expected expression : have \"{\"")
    }
    @Test
    fun expr_var_err2 () {
        val lexer = Lexer("anon", "  ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 3): expected expression : have end of file")
    }

    // EXPR.PARENS

    @Test
    fun expr_parens() {
        val lexer = Lexer("anon", " ( a ) ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun expr_parens_err() {
        val lexer = Lexer("anon", " ( a  ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 7): expected \")\" : have end of file")
    }

    // EXPR.NUM / EXPR.NIL / EXPR.BOOL

    @Test
    fun expr_num() {
        val lexer = Lexer("anon", " 1.5F ".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun expr_nil() {
        val lexer = Lexer("anon", "nil".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Nil && e.tk.str == "nil")
    }
    @Test
    fun expr_true() {
        val lexer = Lexer("anon", "true".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun expr_false() {
        val lexer = Lexer("anon", "false".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Bool && e.tk.str == "false")
    }

    // EXPR.ECALL

    @Test
    fun expr_call1() {
        val lexer = Lexer("anon", " f (1.5F, x) ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Call && e.tk.str=="f" && e.f is Expr.Acc && e.args.size==2)
    }
    @Test
    fun expr_call2() {
        val lexer = Lexer("anon", " f() ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Call && e.f.tk.str=="f" && e.f is Expr.Acc && e.args.size==0)
    }
    @Test
    fun expr_call3() {
        val lexer = Lexer("anon", " f(x,8)() ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Call && e.f is Expr.Call && e.args.size==0)
        assert(e.tostr() == "f(x,8)()")
    }
    @Test
    fun expr_call_err1() {
        val lexer = Lexer("anon", "f (999 ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 8): expected \")\" : have end of file")
    }
    @Test
    fun expr_call_err2() {
        val lexer = Lexer("anon", " f ({ ".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 5): expected expression : have \"{\"")
    }

    // EXPR.TUPLE

    @Test
    fun expr_tuple1() {
        val lexer = Lexer("anon", " [ 1.5F, x] ".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Tuple && e.args.size==2)
    }
    @Test
    fun expr_tuple2() {
        val lexer = Lexer("anon", "[[],[1,2,3]]".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e.tostr() == "[[],[1,2,3]]")
    }
    @Test
    fun expr_tuple_err() {
        val lexer = Lexer("anon", "[{".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 2): expected expression : have \"{\"")
    }

    // EXPR.INDEX

    @Test
    fun expr_index() {
        val lexer = Lexer("anon", "x[10]".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Index && e.col is Expr.Acc && e.idx is Expr.Num)
    }
    @Test
    fun expr_index_err() {
        val lexer = Lexer("anon", "x[10".reader())
        val parser = Parser(lexer)
        assert(trap { parser.exprN() } == "anon: (ln 1, col 5): expected \"]\" : have end of file")
    }

    // EXPRS

    @Test
    fun exprs_call() {
        val lexer = Lexer("anon", "f ()".reader())
        val parser = Parser(lexer)
        val es = parser.exprs()
        assert(es.size==1 && es[0] is Expr.Call && es[0].tostr() == "f()")
        assert(es.tostr() == "f()\n")
    }
    @Test
    fun exprs_call_err() {
        val lexer = Lexer("anon", "f".reader())
        val parser = Parser(lexer)
        val es = parser.exprs()
        assert(es.tostr() == "f\n")
    }

    // EXPRS

    @Test
    fun exprs_seq1() {
        val lexer = Lexer("anon", ";; f () ; g () h()\ni() ;\n;".reader())
        val parser = Parser(lexer)
        val es = parser.exprs()
        assert(es.tostr() == "f()\ng()\nh()\ni()\n") { es.tostr() }
    }
    @Test
    fun exprs_seq2() {
        val lexer = Lexer("anon", ";; f () \n (1) ; h()\ni() ;\n;".reader())
        val parser = Parser(lexer)
        val es = parser.exprs()
        assert(es.tostr() == "f()(1)\nh()\ni()\n") { es.tostr() }
    }

    // EXPR.DCL

    @Test
    fun expr_dcl() {
        val lexer = Lexer("anon", "var x".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Dcl && e.tk.str == "x")
        assert(e.tostr() == "var x")
    }
    @Test
    fun expr_dcl_err() {
        val lexer = Lexer("anon", "var [10]".reader())
        val parser = Parser(lexer)
        assert(trap { parser.expr1() } == "anon: (ln 1, col 5): expected identifier : have \"[\"")
    }

    // EXPR.SET

    @Test
    fun expr_set() {
        val lexer = Lexer("anon", "set x = [10]".reader())
        val parser = Parser(lexer)
        val e = parser.exprN()
        assert(e is Expr.Set && e.tk.str == "set")
        assert(e.tostr() == "set x = [10]")
    }
    @Test
    fun expr_err1() {  // set number?
        val lexer = Lexer("anon", "set 1 = 1".reader())
        val parser = Parser(lexer)
        //val e = parser.exprN()
        //assert(e.tostr() == "set 1 = 1")
        assert(trap { parser.exprN() } == "anon: (ln 1, col 1): invalid set : invalid destination")
    }
    @Test
    fun expr_err2() {  // set whole tuple?
        val lexer = Lexer("anon", "set [1] = 1".reader())
        val parser = Parser(lexer)
        //val e = parser.exprN()
        //assert(e.tostr() == "set [1] = 1")
        assert(trap { parser.exprN() } == "anon: (ln 1, col 1): invalid set : invalid destination")
    }

    // IF

    @Test
    fun expr_if1() {  // set whole tuple?
        val lexer = Lexer("anon", "if true { 1 } else { 0 }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\n0\n}\n") { e.tostr() }
    }
    @Test
    fun expr_if2() {  // set whole tuple?
        val lexer = Lexer("anon", "if true { 1 }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.If)
        assert(e.tostr() == "if true {\n1\n}\nelse {\nnil\n}\n") { e.tostr() }
    }

    // DO

    @Test
    fun expr_do1() {  // set whole tuple?
        val lexer = Lexer("anon", "do{}".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Do && e.es.size==1)
        assert(e.tostr() == "do {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_do2() {  // set whole tuple?
        val lexer = Lexer("anon", "do { var a; set a=1; print(a) }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Do && e.es.size==3)
        assert(e.tostr() == "do {\nvar a\nset a = 1\nprint(a)\n}\n") { e.tostr() }
    }

    // FUNC

    @Test
    fun expr_func1() {
        val lexer = Lexer("anon", "func () {}".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Func && e.args.size==0)
        assert(e.tostr() == "func () {\nnil\n}\n") { e.tostr() }
    }
    @Test
    fun expr_func2() {
        val lexer = Lexer("anon", "func (a,b) { 10 }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Func && e.args.size==2)
        assert(e.tostr() == "func (a,b) {\n10\n}\n") { e.tostr() }
    }

    // LOOP / BREAK

    @Test
    fun expr_loop1() {
        val lexer = Lexer("anon", "loop { break 1 }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Loop)
        assert(e.tostr() == "loop {\nbreak 1\n}\n") { e.tostr() }
    }
    @Test
    fun expr_loop2() {
        val lexer = Lexer("anon", "loop { break }".reader())
        val parser = Parser(lexer)
        val e = parser.expr1()
        assert(e is Expr.Loop)
        assert(e.tostr() == "loop {\nbreak nil\n}\n") { e.tostr() }
    }
}
