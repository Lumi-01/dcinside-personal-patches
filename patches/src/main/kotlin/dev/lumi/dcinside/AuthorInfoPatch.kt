package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/** Author information present in the server response, at rendering call sites only. */
@Suppress("unused")
val authorInfoPatch = bytecodePatch(
    name = "작성자 아이디·IP 정보",
    description = "앱의 Morphe 설정에서 계정 아이디와 내장 IP 대역표의 추정 정보를 표시합니다.",
    default = true,
) {
    compatibleWith(compatibility)
    dependsOn(dcInsidePersonalPatch)

    execute {
        val header = Fingerprint(
            definingClass = "Lcom/dcinside/app/view/PostReadHeaderView;",
            name = "Y",
            parameters = listOf("Lcom/dcinside/app/model/PostInfo;", "Z", "Ljava/lang/String;"),
            returnType = "V",
        ).method
        // Both factory-created and reused headers pass through Y with their post data.
        // Insert at entry: a branch targets the final return and can skip code placed there.
        header.addInstructions(0,
            "invoke-static/range {p0 .. p1}, Llocal/privacy/AuthorInfo;->applyPostHeader(Landroid/view/View;Ljava/lang/Object;)V")

        // The app's shared author renderer appends the visible IP in parentheses.
        // Decorate its completed text so the hint follows the IP, not the nickname.
        val authorSpan = Fingerprint(
            definingClass = "Lcom/dcinside/app/span/g;",
            name = "t",
            returnType = "Landroid/text/Spannable;",
        ).method
        val authorSpanEnd = authorSpan.implementation!!.instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        check(authorSpanEnd >= 0) { "Author span return changed" }
        authorSpan.addInstructions(authorSpanEnd, """
            invoke-static {p0, p4}, Llocal/privacy/IpInfo;->decorateRendered(Landroid/text/Spannable;Ljava/lang/String;)Landroid/text/Spannable;
            move-result-object p0
        """.trimIndent())

        listOf(
            "Lcom/dcinside/app/post/X2;" to "s0",
            "Lcom/dcinside/app/post/fragments/u0;" to "i0",
        ).forEach { (owner, name) ->
            Fingerprint(definingClass = owner, name = name, returnType = "V")
                .method.decorateGetter("Lcom/dcinside/app/response/PostItem;", "z", "listName")
        }
        Fingerprint(
            definingClass = "Lcom/dcinside/app/read/V\$b;", name = "d",
            parameters = listOf("Landroid/content/Context;", "Lcom/dcinside/app/response/m;"),
            returnType = "Ljava/lang/CharSequence;",
        ).method.decorateGetter("Lcom/dcinside/app/response/m;", "Y", "commentName")
    }
}

private fun MutableMethod.decorateGetter(owner: String, getter: String, formatter: String) {
    val instructions = implementation?.instructions ?: error("$name has no bytecode")
    val calls = instructions.indices.filter { index ->
        val reference = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
        reference?.definingClass == owner && reference.name == getter &&
            reference.parameterTypes.isEmpty() && index + 1 < instructions.size &&
            instructions[index + 1].opcode == Opcode.MOVE_RESULT_OBJECT
    }
    check(calls.isNotEmpty()) { "$owner->$getter rendering anchor changed in $name" }
    calls.asReversed().forEach { index ->
        val receiver = (instructions[index] as? FiveRegisterInstruction)?.registerC
            ?: error("Unexpected getter invoke format")
        val nameRegister = (instructions[index + 1] as OneRegisterInstruction).registerA
        addInstructions(index + 2, """
            invoke-static/range {v$receiver .. v$receiver}, Llocal/privacy/AuthorInfo;->$formatter(Ljava/lang/Object;)Ljava/lang/String;
            move-result-object v$nameRegister
        """.trimIndent())
    }
}
