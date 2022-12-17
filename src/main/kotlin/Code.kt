fun Tk.dump (): String {
    return "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})\n"
}

class Coder (val outer: Expr.Block, val ups: Ups) {
    val tags = TAGS.toMutableList()
    val tops: Triple<MutableList<String>, MutableList<String>, MutableList<String>> = Triple(mutableListOf(),mutableListOf(), mutableListOf())
    val mem: String = outer.mem()
    val code: String = outer.code(false, null)

    fun Expr.Block.toc (isptr: Boolean): String {
        return "ceu_mem->block_${this.n}".let {
            if (isptr) "(&($it))" else it
        }
    }
    fun Expr.Block.id2c (id: String): String {
        fun Expr.aux (n: Int): String {
            val xblock = ups.xblocks[this]!!
            val bup = ups.proto_or_block(this)
            val fup = ups.func(this)
            val ok = xblock.syms.contains(id)
            return when {
                (ok && this==outer) -> "(ceu_mem_${outer.n}->$id)"
                (ok && n==0) -> "(ceu_mem->$id)"
                (ok && n!=0) -> {
                    //println(id)
                    //println(this)
                    val blk = if (this is Expr.Proto) this.n else fup!!.n
                    "(((CEU_Proto_Mem_$blk*) ceu_frame ${"->proto->up".repeat(n)}->mem)->$id)"
                }
                (this is Expr.Block) -> bup!!.aux(n)
                (this is Expr.Proto) -> bup!!.aux(n+1)
                else -> error("bug found")
            }
        }
        return this.aux(0)
    }

    fun Expr.fupc (): String? {
        var n = 0
        var fup = ups.func(this)
        while (fup!=null && fup.isFake) {
            n++
            fup = ups.func(fup)
        }
        return if (fup == null) null else "(ceu_frame${"->proto->up".repeat(n)})"
    }

    fun Expr.gcall (): Boolean {
        return ups.ups[this].let { it is Expr.Call && it.proto.let {
            it is Expr.Acc && it.tk.str in listOf("print","println","tags","{==}","{/=}") }
        }
    }

    // assrc_dst: calling expr is a source and here's its destination
    // asdst_src: calling expr is a destination and here's its source
    fun Expr.code (issrc: Boolean, asdst_src: String?): String {
        fun assrc (v: String): String {
            return if (issrc) "ceu_acc = $v;\n" else "ceu_acc = (CEU_Value) { CEU_VALUE_NIL };\n"
        }
        return when (this) {
            is Expr.Proto -> {
                val isfunc = (this.tk.str == "func")
                val istask = (this.tk.str == "task")
                val type = """ // TYPE ${this.tk.dump()}
                typedef struct {
                    ${this.args.map {
                        """
                        CEU_Value ${it.str};
                        CEU_Block* _${it.str}_;
                        """
                }.joinToString("")}
                    ${this.body.mem()}
                } CEU_Proto_Mem_$n;
                """
                val func = """ // BODY ${this.tk.dump()}
                CEU_Value ceu_proto_f_$n (
                    CEU_Frame* ceu_frame,
                    int ceu_n,
                    CEU_Value* ceu_args[]
                ) {
                    ${isfunc.cond{"""
                        CEU_Proto_Mem_$n _ceu_mem_;
                        CEU_Proto_Mem_$n* ceu_mem = &_ceu_mem_;
                        ceu_frame->mem = (char*) ceu_mem;
                    """}}
                    ${istask.cond{"""
                        CEU_Dynamic* ceu_coro = ceu_frame->Task.coro;
                        assert(ceu_coro->Bcast.status==CEU_CORO_STATUS_YIELDED || (ceu_coro->Bcast.status==CEU_CORO_STATUS_TOGGLED && ceu_evt==&CEU_EVT_CLEAR));
                        ceu_coro->Bcast.status = CEU_CORO_STATUS_RESUMED;
                        CEU_Proto_Mem_$n* ceu_mem = (CEU_Proto_Mem_$n*) ceu_frame->mem;
                    """}}
                    CEU_Proto_Mem_$n* ceu_mem_$n = ceu_mem;
                    """ +
                    """ // WHILE
                    do { // func
                        ${istask.cond{"""
                            switch (ceu_frame->Task.pc) {
                                case -1:
                                    assert(0 && "bug found");
                                    break;
                                case 0: {
                                    if (ceu_has_throw_clear()) { // started with BCAST-CLEAR
                                        continue; // from BCAST-CLEAR: escape enclosing block
                                    }
                        """}}
                        { // ARGS
                            // no block yet, set now, will be reset in body
                            int ceu_i = 0;
                            ${this.args.map {
                            val id = it.str.noSpecial()
                            """
                                ceu_mem->_${id}_ = ${this.body.toc(true)};
                                if (ceu_i < ceu_n) {
                                    ceu_mem->$id = *ceu_args[ceu_i];
                                } else {
                                    ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };
                                }
                                ceu_i++;
                                """
                        }.joinToString("")}
                        }
                        // BODY
                        ${this.body.code(true, null)}
                        ${istask.cond{"}\n}\n"}}
                    } while (0); // func
                    """ +
                    """ // TERMINATE
                        ${istask.cond{"""
                            ceu_frame->Task.pc = -1;
                            {
                                CEU_Value ceu_evt_$n = { CEU_VALUE_POINTER, {.Pointer=ceu_coro} };
                                ceu_bcast_blocks(ceu_coro->hold, &ceu_evt_$n);
                            }
                            ceu_coro->Bcast.status = CEU_CORO_STATUS_TERMINATED;
                            if (ceu_coro->Bcast.Coro.coros != NULL) {
                                if ( ceu_coro->Bcast.Coro.coros->Bcast.Coros.open == 0) {
                                    ceu_coros_destroy( ceu_coro->Bcast.Coro.coros, ceu_coro);
                                }
                            }
                        """}}
                    return ceu_acc;
                }
                """
                tops.second.add(type)
                tops.third.add(func)
                """
                CEU_Dynamic* ceu_proto_$n = ceu_proto_create (
                    ${ups.block(this)!!.toc(true)},
                    CEU_VALUE_${this.tk.str.uppercase()},
                    ceu_frame,
                    ceu_proto_f_$n,
                    sizeof(CEU_Proto_Mem_$n)
                );
                assert(ceu_proto_$n != NULL);
                ${assrc("(CEU_Value) { CEU_VALUE_${this.tk.str.uppercase()}, {.Dyn=ceu_proto_$n} }")}
                """
            }
            is Expr.Block -> {
                val bup = ups.block(this)
                val f_b = ups.proto_or_block(this)
                val depth = when {
                    (f_b == null) -> "(0 + 1)"
                    (f_b is Expr.Proto) -> "(ceu_frame->up->depth + 1)"
                    else -> "(${bup!!.toc(false)}.depth + 1)"
                }
                val es = this.es.mapIndexed { i,it ->
                    it.code(issrc && i==this.es.size-1, null)
                }.joinToString("")
                """
                { // BLOCK ${this.tk.dump()}
                    ceu_mem->block_$n = (CEU_Block) { $depth, NULL, {NULL,NULL} };
                    ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond {
                        " ceu_coro->Bcast.Coro.block = &ceu_mem->block_$n;"}
                    }
                    ${(f_b is Expr.Block).cond {
                        "ceu_mem->block_${bup!!.n}.bcast.block = &ceu_mem->block_$n;"}
                    }
                    do { // block
                        $es
                    } while (0); // block
                    ${(f_b != null).cond {
                        val up = if (f_b is Expr.Proto) "ceu_frame->up" else bup!!.toc(true)
                        """
                        { // move up dynamic ceu_acc, ceu_err
                            if (ceu_acc.tag > CEU_VALUE_DYNAMIC) {
                                char* ceu_err_$n = ceu_block_set($up, ceu_acc.Dyn, 0);
                                if (ceu_err_$n != NULL) {
                                    // ${this.tk}
                                    snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                    //continue;
                                }
                            }
                            if (ceu_err.tag > CEU_VALUE_DYNAMIC) {
                                char* ceu_err_$n = ceu_block_set($up, ceu_err.Dyn, 0);
                                if (ceu_err_$n != NULL) {
                                    // ${this.tk}
                                    snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                    //continue;
                                }
                            }
                        }
                        """
                    }}
                    ceu_bcast_blocks(&ceu_mem->block_$n, &CEU_EVT_CLEAR);
                    { // DEFERS ${this.tk.dump()}
                        int ceu_has_throw_old = ceu_has_throw;
                        ceu_has_throw = 0;
                        int ceu_has_bcast_old = ceu_has_bcast;
                        ceu_has_bcast = 0;
                        do {
                            ${ups.xblocks[this]!!.defers!!.reversed().joinToString("")}
                        } while (0);
                        ceu_has_bcast = ceu_has_bcast_old;
                        ceu_has_throw = ceu_has_throw || ceu_has_throw_old;
                    }
                    ${(f_b is Expr.Block).cond{"ceu_mem->block_${bup!!.n}.bcast.block = NULL;"}}
                    ${ups.proto_or_block(this).let { it!=null && it !is Expr.Block && it.tk.str=="task" }.cond{" ceu_coro->Bcast.Coro.block = NULL;"}}
                    ceu_block_free(&ceu_mem->block_$n);
                    if (ceu_has_throw_clear()) {
                        continue;   // escape to end of enclosing block
                    }
                }
                """
            }
            is Expr.Dcl -> {
                val id = this.tk_.fromOp().noSpecial()
                """
                { // DCL ${this.tk.dump()}
                    ${this.init.cond{"ceu_mem->$id = (CEU_Value) { CEU_VALUE_NIL };"}}
                    ceu_mem->_${id}_ = ${ups.block(this)!!.toc(true)};   // can't be static b/c recursion
                    ${assrc("ceu_mem->$id")}
                }
                """
            }
            is Expr.Set -> """
                { // SET ${this.tk.dump()}
                    ${this.src.code(true, null)}
                    ceu_mem->set_$n = ceu_acc;
                    ${this.dst.code(issrc, "ceu_mem->set_$n")}
                    ${assrc("ceu_mem->set_$n")}
                }
                """
            is Expr.If -> """
                { // IF ${this.tk.dump()}
                    ${this.cnd.code(true, null)}
                    if (ceu_as_bool(&ceu_acc)) {
                        ${this.t.code(issrc, null)}
                    } else {
                        ${this.f.code(issrc, null)}
                    }
                }
                """
            is Expr.While -> """
                { // WHILE ${this.tk.dump()}
                CEU_WHILE_START_$n:;
                    ${this.cnd.code(true, null)}
                    if (ceu_as_bool(&ceu_acc)) {
                        ${this.body.code(false, null)}
                        goto CEU_WHILE_START_$n;
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            is Expr.Catch -> """
                { // CATCH ${this.tk.dump()}
                    do { // catch
                        ${this.body.code(issrc, null)}
                    } while (0); // catch
                    if (ceu_has_bcast>0 && ceu_evt==&CEU_EVT_CLEAR) {
                        // do not catch anything while clearing up
                        continue; // escape enclosing block;
                    }
                    if (ceu_has_throw_clear()) {
                        ceu_has_throw = 0;
                        ${this.cnd.code(true, null)}
                        if (!ceu_as_bool(&ceu_acc)) {
                            ceu_has_throw = 1; // UNCAUGHT: escape to outer
                            continue; // escape enclosing block;
                        }
                        ${assrc("ceu_acc = ceu_err")}
                        ceu_err = (CEU_Value) { CEU_VALUE_NIL };
                    }
                }
                """
            is Expr.Throw -> """
                { // THROW ${this.tk.dump()}
                    ${this.ex.code(true, null)}
                    ceu_throw(ceu_acc);
                    strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : throw error : uncaught exception", 256);
                    continue; // escape enclosing block;
                }
                """
            is Expr.Defer -> { ups.xblocks[ups.block(this)!!]!!.defers!!.add(this.body.code(false, null)); "" }

            is Expr.Coros -> {
                """
                { // COROS ${this.tk.dump()}
                    ${this.max.cond { """
                        ${it.code(true, null)}
                        if (ceu_acc.tag!=CEU_VALUE_NUMBER || ceu_acc.Number<=0) {                
                            ceu_throw(CEU_ERR_ERROR);
                            strncpy(ceu_err_error_msg,
                                "${it.tk.pos.file} : (lin ${it.tk.pos.lin}, col ${it.tk.pos.col}) : coroutines error : expected positive number",
                                 256);
                            continue; // escape enclosing block;
                        }
                    """}}
                    ceu_coros_create (
                        ${ups.block(this)!!.toc(true)},
                        ${if (this.max==null) 0 else "ceu_acc.Number"},
                        &ceu_acc
                    );
                }
                """
            }
            is Expr.Coro -> {
                """
                { // CORO ${this.tk.dump()}
                    ${this.task.code(true, null)}
                    CEU_Value ceu_coro_$n;
                    char* ceu_err_$n = ceu_coro_create(${ups.block(this)!!.toc(true)}, &ceu_acc, &ceu_coro_$n);
                    if (ceu_err_$n != NULL) {
                        ceu_throw(CEU_ERR_ERROR);
                        snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.task.tk.pos.lin}, col ${this.task.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    ${assrc("ceu_acc = ceu_coro_$n")}
                }
                """
            }
            is Expr.Spawn -> this.call.code(issrc, null)
            is Expr.Iter -> {
                val loc = this.loc.str
                """
                { // ITER ${this.tk.dump()}
                    ${this.coros.code(true, null)}
                    ceu_mem->coros_$n = ceu_acc;
                    if (ceu_mem->coros_$n.tag != CEU_VALUE_COROS) {                
                        ceu_throw(CEU_ERR_ERROR);
                        strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.coros.tk.pos.lin}, col ${this.coros.tk.pos.col}) : while error : expected coroutines", 256);
                        continue; // escape enclosing block;
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open++;
                    ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->coros_$n.Dyn->Bcast.Coros.first} };
                    do { // iter
                CEU_ITER_$n:;
                        if (ceu_mem->$loc.Dyn == NULL) {
                            continue; // escape enclosing block
                        }
                        ceu_mem->hold_$n = ceu_mem->$loc.Dyn->hold;  
                        ceu_mem->$loc.Dyn->hold = ${this.body.toc(true)}; // tmp coro.hold to nested block
                        ${this.body.code(false, null)}
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n;
                        ceu_mem->$loc = (CEU_Value) { CEU_VALUE_CORO, {.Dyn=ceu_mem->$loc.Dyn->Bcast.next} };
                        goto CEU_ITER_$n;
                    } while (0); // iter
                    if (ceu_mem->$loc.Dyn != NULL) { // repeat in case body error
                        ceu_mem->$loc.Dyn->hold = ceu_mem->hold_$n;
                    }
                    ceu_mem->coros_$n.Dyn->Bcast.Coros.open--;
                    if (ceu_mem->coros_$n.Dyn->Bcast.Coros.open == 0) {
                        ceu_coros_cleanup(ceu_mem->coros_$n.Dyn);
                    }
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            }
            is Expr.Bcast -> {
                val bupc = ups.block(this)!!.toc(true)
                """
                { // BCAST ${this.tk.dump()}
                    ${this.evt.code(true, null)}
                    ceu_mem->evt_$n = ceu_acc;
                    if (ceu_acc.tag > CEU_VALUE_DYNAMIC) {
                        assert(NULL == ceu_block_set($bupc, ceu_mem->evt_$n.Dyn, 1));
                    }
                    ${this.xin.code(true, null)}
                    int ceu_err_$n = 0;
                    if (ceu_acc.tag == CEU_VALUE_CORO) {
                        ceu_bcast_dyn(ceu_acc.Dyn, &ceu_mem->evt_$n);
                    } else if (ceu_acc.tag == CEU_VALUE_TAG) {
                        if (ceu_acc.Tag == CEU_TAG_global) {
                            ceu_bcast_blocks(&ceu_mem_${outer.n}->block_${outer.n}, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_local) {
                            ceu_bcast_blocks($bupc, &ceu_mem->evt_$n);
                        } else if (ceu_acc.Tag == CEU_TAG_task) {
                            ${this.fupc().let {
                                if (it == null) {
                                    "ceu_err_$n = 1;"
                                } else {
                                    "ceu_bcast_dyn($it->Task.coro, &ceu_mem->evt_$n);"
                                }
                            }}
                        } else {
                            ceu_err_$n = 1;
                        }
                    } else {
                        ceu_err_$n = 1;
                    }
                    if (ceu_err_$n) {
                        ceu_throw(CEU_ERR_ERROR);
                        strncpy(ceu_err_error_msg, "${this.xin.tk.pos.file} : (lin ${this.xin.tk.pos.lin}, col ${this.xin.tk.pos.col}) : broadcast error : invalid target", 256);
                        continue; // escape enclosing block;
                    }
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ceu_acc = (CEU_Value) { CEU_VALUE_NIL };
                }
                """
            }
            is Expr.Yield -> """
                { // YIELD ${this.tk.dump()}
                    ${this.arg.code(true, null)}
                    ceu_frame->Task.pc = $n;      // next resume
                    ceu_coro->Bcast.status = CEU_CORO_STATUS_YIELDED;
                    if (ceu_acc.tag > CEU_VALUE_DYNAMIC) {
                        char* ceu_err_$n = ceu_block_set(ceu_frame->up, ceu_acc.Dyn, 0);
                        if (ceu_err_$n != NULL) {
                            snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                            continue;
                        }
                    }
                    return ceu_acc;
                case $n:                    // resume here
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    assert(ceu_n <= 1 && "bug found : not implemented : multiple arguments to resume");
                    ${assrc("*ceu_args[0]")} // resume single argument
                }
                """
            is Expr.Resume -> this.call.code(issrc, null)
            is Expr.Toggle -> """
                ${this.on.code(true, null)}
                ceu_mem->on_$n = ceu_acc;
                ${this.coro.code(true, null)}
                if (ceu_acc.tag<CEU_VALUE_BCAST || (ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_acc.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                    ceu_throw(CEU_ERR_ERROR);
                    strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : toggle error : expected yielded/toggled coroutine", 256);
                    continue; // escape enclosing block;
                }
                ceu_acc.Dyn->Bcast.status = (ceu_as_bool(&ceu_mem->on_$n) ? CEU_CORO_STATUS_YIELDED : CEU_CORO_STATUS_TOGGLED);
                """
            is Expr.Pub -> """
                { // PUB
                    CEU_Dynamic* ceu_dyn_$n;
                    ${if (this.coro == null) {
                        "ceu_dyn_$n = ${this.fupc()}->Task.coro;"
                    } else { """
                        ${this.coro.code(true, null)}
                        ${(this.tk.str=="status").cond { """
                            // track with destroyed coro: status -> :destroyed
                            if (ceu_acc.tag == CEU_VALUE_TRACK) {
                                ceu_acc = ceu_track_to_coro(&ceu_acc);
                                if (ceu_acc.tag != CEU_VALUE_CORO) {
                                    ${assrc("(CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_destroyed} }")}
                                    goto CEU_PUB_$n;    // special case, skip everything else
                                }
                            }
                            """
                         }}
                        ceu_acc = ceu_track_to_coro(&ceu_acc);
                        if (ceu_acc.tag != CEU_VALUE_CORO) {                
                            ceu_throw(CEU_ERR_ERROR);
                            strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : ${this.tk.str} error : expected coroutine", 256);
                            continue; // escape enclosing block;
                        }
                        ceu_dyn_$n = ceu_acc.Dyn;
                    """ }}
                    ${if (asdst_src != null) {
                            """ // PUB - SET
                            if ($asdst_src.tag > CEU_VALUE_DYNAMIC) {
                                char* ceu_err_$n = ceu_block_set(ceu_dyn_$n->hold, $asdst_src.Dyn, 0);
                                if (ceu_err_$n != NULL) {
                                    snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                    continue;
                                }
                            }
                            ceu_dyn_$n->Bcast.Coro.frame->Task.pub = $asdst_src;
                            """
                    } else {
                        val inidx = (ups.pred(this) { it is Expr.Index } != null)
                        """
                        { // PUB - read
                            ${(!inidx && !this.gcall() && this.tk.str=="pub").cond { """
                                if (ceu_dyn_$n->Bcast.Coro.frame->Task.pub.tag > CEU_VALUE_DYNAMIC) {
                                    ceu_throw(CEU_ERR_ERROR);
                                    strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : invalid ${this.tk.str} : cannot expose dynamic \"pub\" field", 256);
                                    continue; // escape enclosing block;
                                }                                    
                            """ }}
                            ${assrc(if (this.tk.str=="pub") {
                                "ceu_dyn_$n->Bcast.Coro.frame->Task.pub"
                            } else {
                                "(CEU_Value) { CEU_VALUE_TAG, {.Tag=ceu_dyn_$n->Bcast.status + CEU_TAG_resumed} }"
                            })}
                        }
                        """
                    }}
                    CEU_PUB_$n:;
                }
                """
            is Expr.Track -> """
                { // TRACK
                    ${this.coro.code(true, null)}
                    if (ceu_acc.tag != CEU_VALUE_CORO) {                
                        ceu_throw(CEU_ERR_ERROR);
                        strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected coroutine", 256);
                        continue; // escape enclosing block;
                    } else if (ceu_acc.Dyn->Bcast.status == CEU_CORO_STATUS_TERMINATED) {                
                        ceu_throw(CEU_ERR_ERROR);
                        strncpy(ceu_err_error_msg, "${this.coro.tk.pos.file} : (lin ${this.coro.tk.pos.lin}, col ${this.coro.tk.pos.col}) : track error : expected unterminated coroutine", 256);
                        continue; // escape enclosing block;
                    }
                    {
                        CEU_Value ceu_$n;
                        assert(NULL == ceu_track_create(ceu_acc.Dyn, &ceu_$n));
                        ${assrc("ceu_$n")}
                    }
                }
                """

            is Expr.Nat -> {
                val bup = ups.block(this)!!
                val body = this.tk.str.let {
                    var ret = ""
                    var i = 0

                    var lin = 1
                    var col = 1
                    fun read (): Char {
                        //assert(i < it.length) { "bug found" }
                        if (i >= it.length) {
                            err(tk, "native error : (lin $lin, col $col) : unterminated token")
                        }
                        val x = it[i++]
                        if (x == '\n') {
                            lin++; col=0
                        } else {
                            col++
                        }
                        return x
                    }

                    while (i < it.length) {
                        ret += if (it[i] != '$') read() else {
                            read()
                            val (l,c) = Pair(lin,col)
                            var id = ""
                            var x = read()
                            while (x.isLetterOrDigit() || x=='_') {
                                id += x
                                x = read()
                            }
                            if (id.length == 0) {
                                err(tk, "native error : (lin $l, col $c) : invalid identifier")
                            }
                            ups.assertIsDeclared(bup, id, this.tk)
                            id = bup.id2c(id)
                            "($id)$x"
                        }
                    }
                    ret
                }
                val (pre,pos) = when (this.tk_.tag) {
                    null -> Pair(null, body)
                    ":pre" -> Pair(body, "")
                    else -> {
                        val (TAG,Tag) = this.tk_.tag.drop(1).let {
                            Pair(it.uppercase(), it.first().uppercase()+it.drop(1))
                        }
                        val v = assrc("((CEU_Value){ CEU_VALUE_$TAG, {.$Tag=($body)} })")
                        Pair(null, """
                        //{ // NATIVE ${this.tk.dump()} // (use comment b/c native may declare var to be used next)
                            $v
                            if (ceu_has_throw_clear()) {
                                continue; // escape enclosing block;
                            }
                        //}
                        """)
                    }
                }
                if (pre != null) {
                    tops.first.add(pre)
                }
                pos
            }
            is Expr.Acc -> {
                val bup = ups.block(this)!!
                val id = this.tk_.fromOp().noSpecial()
                ups.assertIsDeclared(bup, id, this.tk)
                if (asdst_src == null) {
                    assrc(bup.id2c(id)) // ACC ${this.tk.dump()}
                } else {
                    ups.assertIsDeclared(bup, "_${id}_", this.tk)
                    """
                    { // ACC - SET
                        if ($asdst_src.tag > CEU_VALUE_DYNAMIC) {
                            char* ceu_err_$n = ceu_block_set(${bup.id2c("_${id}_")}, $asdst_src.Dyn, 1);
                            if (ceu_err_$n != NULL) {
                                snprintf(ceu_err_error_msg, 256, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : %s", ceu_err_$n);
                                continue;
                            }
                        }
                        ${bup.id2c(id)} = $asdst_src;
                    }
                    """
                }
            }
            is Expr.EvtErr -> {
                when (this.tk.str) {
                    "err" -> assrc("ceu_err")
                    "evt" -> {
                        val inidx = (ups.pred(this) { it is Expr.Index } != null)
                        (!inidx && !this.gcall()).cond { """
                            if (ceu_evt->tag > CEU_VALUE_DYNAMIC) {
                                ceu_throw(CEU_ERR_ERROR);
                                strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : invalid evt : cannot expose dynamic \"evt\"", 256);
                                continue; // escape enclosing block;
                            }                                    
                        """ } + assrc("(*ceu_evt)")
                    }
                    else -> error("impossible case")
                }
            }
            is Expr.Nil -> assrc("((CEU_Value) { CEU_VALUE_NIL })")
            is Expr.Tag -> {
                val tag = this.tk.str.drop(1)
                if (!tags.contains(tag)) {
                    tags.add(tag)
                }
                assrc("((CEU_Value) { CEU_VALUE_TAG, {.Tag=CEU_TAG_$tag} })")
            }
            is Expr.Bool -> assrc("((CEU_Value) { CEU_VALUE_BOOL, {.Bool=${if (this.tk.str == "true") 1 else 0}} })")
            is Expr.Num -> assrc("((CEU_Value) { CEU_VALUE_NUMBER, {.Number=${this.tk.str}} })")

            is Expr.Tuple -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(true, null) + """
                    ceu_mem->arg_${i}_$n = ceu_acc;
                    """
                }.joinToString("")
                """
                { // TUPLE ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_tup_$n = ceu_tuple_create(${ups.block(this)!!.toc(true)}, ${this.args.size}, ceu_args_$n);
                    assert(ceu_tup_$n != NULL);
                    ${assrc("(CEU_Value) { CEU_VALUE_TUPLE, {.Dyn=ceu_tup_$n} }")}
                }
                """
            }
            is Expr.Vector -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.code(true, null) + """
                    ceu_mem->arg_${i}_$n = ceu_acc;
                    """
                }.joinToString("")
                """
                { // VECTOR ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}] = {
                        ${this.args.mapIndexed { i, _ -> "ceu_mem->arg_${i}_$n" }.joinToString(",")}
                    };
                    int ceu_tag_$n = CEU_VALUE_NIL;
                    { // check if vector is homogeneous
                        for (int i=0; i<${this.args.size}; i++) {
                            if (i == 0) {
                                ceu_tag_$n = ceu_args_$n[i].tag;
                            } else if (ceu_tag_$n != ceu_args_$n[i].tag) {
                                ceu_throw(CEU_ERR_ERROR);
                                strncpy(ceu_err_error_msg, "${this.tk.pos.file} : (lin ${this.tk.pos.lin}, col ${this.tk.pos.col}) : vector error : non homogeneous arguments", 256);
                                break;
                            }
                        }
                        if (ceu_has_throw_clear()) {
                            continue;   // escape to end of enclosing block
                        }
                    }
                    CEU_Dynamic* ceu_vec_$n = ceu_vector_create(${ups.block(this)!!.toc(true)}, ceu_tag_$n, ${this.args.size}, ceu_args_$n);
                    assert(ceu_vec_$n != NULL);
                    ${assrc("(CEU_Value) { CEU_VALUE_VECTOR, {.Dyn=ceu_vec_$n} }")}
                }
                """
            }
            is Expr.Dict -> {
                val args = this.args.mapIndexed { i, it ->
                    // allocate in the same scope of set (set.first) or use default block
                    it.first.code(true, null)  + "ceu_mem->arg_${i}_a_$n = ceu_acc;\n" +
                    it.second.code(true, null) + "ceu_mem->arg_${i}_b_$n = ceu_acc;\n"
                }.joinToString("")
                """
                { // DICT ${this.tk.dump()}
                    $args
                    CEU_Value ceu_args_$n[${this.args.size}][2] = {
                        ${this.args.mapIndexed { i, _ -> "{ceu_mem->arg_${i}_a_$n,ceu_mem->arg_${i}_b_$n}" }.joinToString(",")}
                    };
                    CEU_Dynamic* ceu_dict_$n = ceu_dict_create(${ups.block(this)!!.toc(true)}, ${this.args.size}, &ceu_args_$n);
                    assert(ceu_dict_$n != NULL);
                    ${assrc("(CEU_Value) { CEU_VALUE_DICT, {.Dyn=ceu_dict_$n} }")}
                }
                """
            }
            is Expr.Index -> {
                fun Expr.Index.has_pub_evt (): String? {
                    val up = ups.ups[this]
                    return when {
                        (this.col is Expr.Pub) -> "pub"
                        (this.col is Expr.EvtErr && this.col.tk.str=="evt") -> "evt"
                        (up == null) -> null
                        (up !is Expr.Index) -> null
                        else -> up.has_pub_evt()
                    }
                }
                """
                { // INDEX  ${this.tk.dump()}
                    // IDX
                    ${this.idx.code(true, null)}
                    ceu_mem->idx_$n = ceu_acc;
                    // COL
                    ${this.col.code(true, null)}
                    char* ceu_err_$n = ceu_col_check(&ceu_acc, &ceu_mem->idx_$n);
                    ${asdst_src.cond { """
                        if (ceu_err_$n==NULL && $it.tag>CEU_VALUE_DYNAMIC) {
                            ceu_err_$n = ceu_block_set(ceu_acc.Dyn->hold, $it.Dyn, 0);
                        }
                    """}}
                    if (ceu_err_$n != NULL) {                
                        snprintf(ceu_err_error_msg, 256, "${this.col.tk.pos.file} : (lin ${this.col.tk.pos.lin}, col ${this.col.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    switch (ceu_acc.tag) { // OK
                        case CEU_VALUE_TUPLE:                
                            ${if (asdst_src != null) {
                                "ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number] = $asdst_src;\n"
                            } else {
                                val x = this.has_pub_evt()
                                """
                                ${(x!=null && !this.gcall()).cond { """
                                    if (ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number].tag > CEU_VALUE_DYNAMIC) {
                                        ceu_throw(CEU_ERR_ERROR);
                                        strncpy(ceu_err_error_msg, "${this.idx.tk.pos.file} : (lin ${this.idx.tk.pos.lin}, col ${this.idx.tk.pos.col}) : invalid index : cannot expose dynamic \"$x\" field", 256);
                                        continue; // escape enclosing block;
                                    }
                                """}}
                                ${assrc("ceu_acc.Dyn->Tuple.mem[(int) ceu_mem->idx_$n.Number]")}
                                """
                            }}
                            break;
                        case CEU_VALUE_DICT: {
                            int idx = ceu_dict_key_index(ceu_acc.Dyn, &ceu_mem->idx_$n);
                            ${if (asdst_src != null) {
                                    """ // SET
                                    if (idx == -1) {
                                        idx = ceu_dict_new_index(ceu_acc.Dyn);
                                        (*ceu_acc.Dyn->Dict.mem)[idx][0] = ceu_mem->idx_$n;
                                    }
                                    (*ceu_acc.Dyn->Dict.mem)[idx][1] = $asdst_src;
                                    """
                            } else {
                                assrc("((idx==-1) ? (CEU_Value) { CEU_VALUE_NIL } : (*ceu_acc.Dyn->Dict.mem)[idx][1])")
                            }}
                            break;
                        }
                    }
                }
                """
            }
            is Expr.Call -> {
                val up = ups.ups[this]
                val bupc = ups.block(this)!!.toc(true)
                val resume = (if (up is Expr.Resume) up else null)
                val spawn  = (if (up is Expr.Spawn)  up else null)
                val iscall = (resume==null && spawn==null)
                val iscoros = (spawn?.coros != null)
                val frame = if (iscall) "(&ceu_frame_$n)" else "(ceu_coro_$n.Dyn->Bcast.Coro.frame)"

                val (args_sets,args_vs) = this.args.mapIndexed { i,e ->
                    Pair (
                        e.code(true, null) + "ceu_mem->arg_${i}_$n = ceu_acc;\n",
                        "&ceu_mem->arg_${i}_$n"
                    )
                }.unzip().let {
                    Pair(it.first.joinToString(""), it.second.joinToString(", "))
                }

                """
                { // SETS
                    $args_sets
                }
                { // CALL (open)
                """ +

                iscall.cond{"""
                // CALL ${this.tk.dump()}
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_proto_$n = ceu_acc;
                    char* ceu_err_$n = NULL;
                    if (ceu_proto_$n.tag != CEU_VALUE_FUNC) {
                        ceu_err_$n = "call error : expected function";
                    }
                    CEU_Frame ceu_frame_$n = { &ceu_proto_$n.Dyn->Proto, $bupc, NULL, {} };
                """} +

                spawn.cond{"""
                // SPAWN/CORO ${this.tk.dump()}
                    ${iscoros.cond {
                        spawn!!.coros!!.code(true, null) + """
                        ceu_mem->coros_${spawn!!.n} = ceu_acc;
                        """
                    }}
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_task_$n = ceu_acc;
                    CEU_Value ceu_coro_$n;
                    ${iscoros.cond { "CEU_Value ceu_ok_$n = { CEU_VALUE_BOOL, {.Bool=1} };" }}
                    ${if (!iscoros) {
                        """
                        char* ceu_err_$n = ceu_coro_create($bupc, &ceu_task_$n, &ceu_coro_$n);
                        """
                    } else {
                        """
                        char* ceu_err_$n = ceu_coro_create_in (
                            $bupc,
                            ceu_mem->coros_${spawn!!.n}.Dyn,
                            &ceu_task_$n,
                            &ceu_coro_$n,
                            &ceu_ok_$n.Bool
                        );
                        if (ceu_ok_$n.Bool) {
                            // call task only if ok
                        //} // closes below
                        """
                    }}
                """} +

                resume.cond{"""
                // RESUME ${this.tk.dump()}
                    ${this.proto.code(true, null)}
                    CEU_Value ceu_coro_$n = ceu_acc;
                    char* ceu_err_$n = NULL;
                    if (ceu_coro_$n.tag<CEU_VALUE_BCAST || (ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_YIELDED && ceu_coro_$n.Dyn->Bcast.status!=CEU_CORO_STATUS_TOGGLED)) {                
                        ceu_err_$n = "resume error : expected yielded task";
                    }
                """} +

                """
                    if (ceu_err_$n != NULL) {
                        ceu_throw(CEU_ERR_ERROR);
                        snprintf(ceu_err_error_msg, 256, "${this.proto.tk.pos.file} : (lin ${this.proto.tk.pos.lin}, col ${this.proto.tk.pos.col}) : %s", ceu_err_$n);
                        continue; // escape enclosing block;
                    }
                    CEU_Value* ceu_args_$n[] = { $args_vs };
                    CEU_Value ceu_$n = $frame->proto->f (
                            $frame,
                            ${this.args.size},
                            ceu_args_$n
                        );
                    if (ceu_has_throw_clear()) {
                        continue; // escape enclosing block
                    }
                    ${iscoros.cond{"}"}}
                    ${iscall.cond{ assrc("ceu_$n") }}
                    ${spawn.cond{ assrc(if (iscoros) "ceu_ok_$n" else "ceu_coro_$n") }}
                    ${resume.cond{ assrc("ceu_$n") }}
                } // CALL (close)
                """
            }

            is Expr.XSeq -> error("bug found")
        }
    }
}
