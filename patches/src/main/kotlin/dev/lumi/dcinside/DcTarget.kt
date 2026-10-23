package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint

/** Verified names for the two supplied official APKs, not a guess for future releases. */
internal class DcTarget(version: String) {
    val modern = version == "5.3.18"
    init {
        check(version in setOf("5.3.6", "5.3.18")) {
            "DC Inside $version needs an APK compatibility review; supported baselines: 5.3.6, 5.3.18"
        }
    }

    private val classes = mapOf(
        "Lcom/dcinside/app/util/Rr;" to "Lcom/dcinside/app/util/Ur;",
        "Lcom/dcinside/app/util/ms;" to "Lcom/dcinside/app/util/ps;",
        "Lcom/dcinside/app/wv/a;" to "Lcom/dcinside/app/wv/c;",
        "Lcom/dcinside/app/wv/l;" to "Lcom/dcinside/app/wv/n;",
        "Lcom/dcinside/app/wv/r;" to "Lcom/dcinside/app/wv/t;",
        "Lcom/dcinside/app/ad/support/J;" to "Lcom/dcinside/app/ad/support/E;",
        "Lcom/dcinside/app/main/A0;" to "Lcom/dcinside/app/main/D0;",
        "Lcom/dcinside/app/main/E1;" to "Lcom/dcinside/app/main/H1;",
        "LW3/o;" to "LN3/o;", "LF/a\$a;" to "LB/a\$a;", "LF/d;" to "LB/d;",
        "LO/N7;" to "LK/L7;",
    )
    private val methods = mapOf(
        "com/dcinside/app/main/adapter/f:Z" to "G0",
        "com/dcinside/app/post/X2:onBindViewHolder" to "Y",
        "com/dcinside/app/post/X2:a0" to "H0",
        "com/dcinside/app/post/X2:s0" to "Z0",
        "com/dcinside/app/post/fragments/u0:h0" to "O0",
        "com/dcinside/app/post/fragments/u0:i0" to "P0",
        "com/dcinside/app/post/fragments/E2:k" to "c0",
        "com/dcinside/app/post/fragments/z3:A2" to "y2",
        "com/dcinside/app/post/fragments/z3:x0" to "q0",
        "com/dcinside/app/post/fragments/w2:f5" to "d5",
        "com/dcinside/app/view/f0:x0" to "q0",
        "com/dcinside/app/view/f0:t1" to "l1",
        "com/dcinside/app/view/f0:v1" to "o1",
        "com/dcinside/app/ad/naver/c:k" to "c0",
        "com/dcinside/app/ad/naver/e:i" to "a0",
        "com/dcinside/app/ad/support/a:b" to "c",
        "com/dcinside/app/main/A0:F2" to "K2",
        "com/dcinside/app/response/PostItem:z" to "y",
    )
    private val fields = mapOf(
        "Lcom/dcinside/app/view/f0;->I:LO/N7;" to "Lcom/dcinside/app/view/f0;->D9:LK/L7;",
        "Lcom/dcinside/app/post/fragments/E2;->g:Landroid/widget/FrameLayout;" to
            "Lcom/dcinside/app/post/fragments/E2;->F9:Landroid/widget/FrameLayout;",
        "Lcom/dcinside/app/post/fragments/E2;->h:Landroid/view/View;" to
            "Lcom/dcinside/app/post/fragments/E2;->G9:Landroid/view/View;",
        "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;->itemView:Landroid/view/View;" to
            "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;->a:Landroid/view/View;",
    )

    fun type(type: String): String = if (modern) classes[type] ?: type else type
    fun name(owner: String, name: String): String = if (modern)
        methods[owner.removePrefix("L").removeSuffix(";") + ":" + name] ?: name else name
    fun smali(code: String): String {
        if (!modern) return code
        var updated = code
        fields.forEach { (old, new) -> updated = updated.replace(old, new) }
        classes.forEach { (old, new) -> updated = updated.replace(old, new) }
        return updated
    }
    val pageLabelId get() = if (modern) 0x7f15082f else 0x7f150843

    fun fingerprint(definingClass: String, name: String, parameters: List<String>? = null,
                    returnType: String? = null): Fingerprint = Fingerprint(
        definingClass = type(definingClass), name = this.name(definingClass, name),
        parameters = parameters?.map(::type), returnType = returnType?.let(::type),
    )
}
