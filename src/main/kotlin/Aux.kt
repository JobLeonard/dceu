fun Pos.isSameLine (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin)
}

fun Tk.Id.fromOp (): String {
    val MAP = mapOf(
        Pair('+', "plus"),
        Pair('-', "minus"),
        Pair('*', "mul"),
        Pair('/', "div"),
        Pair('>', "gt"),
        Pair('<', "lt"),
        Pair('=', "eq"),
        Pair('!', "not"),
        Pair('|', "or"),
        Pair('&', "and"),
    )
    return if (this.str[0] != '{') this.str else {
        "op_" + this.str.drop(1).dropLast(1).toList().map { MAP[it] }.joinToString("_")
    }
}

fun Expr.Func.isTask (): Boolean {
    return (this.tk.str == "task")
}

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
    err(tk, "expected $str : have $have")
}
