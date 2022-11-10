import java.io.File
import java.io.PushbackReader
import java.io.StringReader

fun op2f (op: String): String {
    return when (op) {
        "+" -> "op_plus"
        "-" -> "op_minus"
        "*" -> "op_mult"
        "/" -> "op_div"
        else -> TODO(op)
    }
}

data class Pos (val file: String, var lin: Int, var col: Int)

class Lexer (name_: String, reader_: StringReader) {
    val stack = ArrayDeque(listOf(Pos(name_,1,1)))
    val reader = PushbackReader(reader_,2)

    fun err (pos: Pos, str: String) {
        error(pos.file + " : (lin ${pos.lin}, col ${pos.col}) : $str")
    }
    fun err (tk: Tk, str: String) {
        err(tk.pos, str)
    }
    fun err_expected (tk: Tk, str: String) {
        val have = when {
            (tk is Tk.Eof) -> "end of file"
            else -> '"' + tk.str + '"'
        }
        this.err(tk, "expected $str : have $have")
    }

    // TODO: reads 65535 after unreading -1
    fun iseof (n: Int): Boolean {
        return (n==-1 || n==65535)
    }

    fun PushbackReader.read2 (): Pair<Int,Char> {
        val n = this.read()
        val x = n.toChar()
        if (x == '\n') {
            stack.first().lin++; stack.first().col=1
        } else if (!iseof(n)) {
            stack.first().col++
        }
        return Pair(n,x)
    }
    fun PushbackReader.unread2 (n: Int) {
        val x = n.toChar()
        this.unread(n)
        when {
            iseof(n) -> {}
            (x == '\n') -> { stack.first().lin--; stack.first().col=0 }
            else -> stack.first().col--
        }
    }

    fun next (): Pair<Char?, Pos> {
        while (true) {
            val pos = this.stack.first().copy()
            val (n1,x1) = this.reader.read2()
            //println("$l + $c: $x1")
            when (x1) {
                ' ', '\n' -> {}
                '-' -> {
                    val (n2,x2) = this.reader.read2()
                    if (iseof(n2) || x2!='-') {
                        this.reader.unread2(n2)
                        return Pair('-', pos)
                    }
                    // found comment '--': ignore everything up to \n or EOF
                    while (true) {
                        val (n3,x3) = this.reader.read2()
                        when {
                            (x3 == '\n') -> break   // LN stops comment
                            iseof(n3) -> {         // EOF stops comment
                                this.reader.unread2(n3)
                                break
                            }
                        }
                    }
                }
                else -> {
                    return if (iseof(n1)) {
                        Pair(null, pos)
                    } else {
                        Pair(x1, pos)
                    }
                }
            }
        }
    }

    fun lex (): Sequence<Tk> = sequence {
        while (true) {
            val (x,pos) = next()
            when {
                (x == null) -> break
                (x in listOf('{','}',')','[',']', ',',';','=', '-','+','*','/')) -> yield(Tk.Fix(x.toString(), pos))
                (x == '(') -> {
                    val (n1,x1) = reader.read2()
                    if (x1 in listOf('-','+','*','/')) {
                        val (n2,x2) = reader.read2()
                        if (x2 == ')') {
                            yield(Tk.Id(op2f(x1.toString()), pos))
                        } else {
                            reader.unread2(n2)
                            yield(Tk.Fix("(", pos))
                            yield(Tk.Fix(x1.toString(), pos))
                        }
                    } else {
                        reader.unread2(n1)
                        yield(Tk.Fix("(", pos))
                    }
                }
                (x=='_' || x.isLetter()) -> {
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = reader.read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '_') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    when {
                        keywords.contains(pay) -> yield(Tk.Fix(pay, pos))
                        (pay != "native") -> yield(Tk.Id(pay, pos))
                        else -> {
                            val (x1,_) = next()
                            if (x1!='(' && x1!='{') {
                                err(pos,"lexer error : unterminated native token")
                            }

                            var open = x1
                            var close = if (x1 == '(') ')' else '}'
                            var open_close = 1

                            var nat = x1.toString()
                            while (true) {
                                val (n2,x2) = reader.read2()
                                when {
                                    iseof(n2) -> {
                                        err(pos, "lexer error : unterminated native token")
                                    }
                                    (x2 == open) -> open_close++
                                    (x2 == close) -> {
                                        open_close--
                                        if (open_close == 0) {
                                            nat += x2
                                            break
                                        }
                                    }
                                }
                                nat += x2
                            }
                            //println("#$pay#")
                            yield(Tk.Nat(nat, pos))
                        }
                    }
                }
                x.isDigit() -> {
                    var pay = x.toString()
                    while (true) {
                        val (n1,x1) = reader.read2()
                        when {
                            iseof(n1) -> break
                            (x1 == '.') -> {}
                            (x1.isLetterOrDigit()) -> {}
                            else -> {
                                reader.unread2(n1)
                                break
                            }
                        }
                        pay += x1
                    }
                    yield(Tk.Num(pay, pos))
                }
                (x == '^') -> {
                    val (_,x2) = reader.read2()
                    if (x2 != '[') {
                        err(pos, "lexer error : expected \"[\"")
                    }
                    var file: String?
                    var lin: Int?
                    var col: Int?
                    while (true) {
                        val (n3,x3) = reader.read2()
                        when {
                            iseof(n3) -> err(pos, "lexer error : unterminated ^ token")
                            (x3 == '"') -> {
                                var file = ""
                                val (lin,col) = all().let { Pair(it.lin,it.col) }
                                while (true) {
                                    x3 = all().read().second
                                    if (x3 == '"') {
                                        break
                                    }
                                    file += x3
                                }
                                assert(x3 == '"')
                                val f = File(file)
                                All_assert_tk(all().let{Tk.Err("",lin,col)}, f.exists()) {
                                    "file not found : $file"
                                }
                                alls.stack.addFirst(All(file, PushbackReader(StringReader(f.readText()), 2), true))

                            }
                            x3.isDigit() -> {
                                fun digits (): Int {
                                    assert(x3.isDigit())
                                    var pay = ""
                                    var n4 = 0
                                    var x4 = x3
                                    do {
                                        pay += x4
                                        reader.read2().let {
                                            n4 = it.first
                                            x4 = it.second
                                        }
                                    } while (x4.isDigit())
                                    reader.unread2(n4)
                                    return pay.toInt()
                                }

                                val lin = digits()
                                x4 = all().read().second
                                if (x4 != ',')  TODO()
                                x4 = all().read().second
                                if (!x4.isDigit()) TODO()
                                val col = digits()
                                x4 = all().read().second
                                if (x4 != ']') TODO()
                                stack.addFirst(Pos(lin, col))

                            }
                        }
                    }
                    when {
                        (file!=null && lin==null && col==null) -> {}
                        (file!=null && lin!=null && col!=null) -> {}
                        (file==null && lin==null && col==null) -> {}
                        else -> err(pos, "lexer error : invalid ^ token")
                    }
                }

                else -> {
                    TODO("$x - $pos")
                }
            }
        }
        yield(Tk.Eof(stack.first()))
    }
}