package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Runtime home display choices for the verified 5.3.6 adapter. */
@Suppress("unused")
val homeDisplayOptionsPatch = bytecodePatch(
    name = "앱 안에서 홈 화면 설정",
    description = "앱의 Morphe 설정에서 홈 화면 영역을 각각 숨길 수 있습니다.",
    default = true,
) {
    compatibleWith(compatibility)
    dependsOn(dcInsidePersonalPatch)

    execute {
        val update = Fingerprint(
            definingClass = "Lcom/dcinside/app/main/adapter/f;",
            name = "Z",
            parameters = listOf("Ljava/util/List;"),
            returnType = "V",
        ).method
        update.addInstructions(0, """
            invoke-static {p1}, Llocal/privacy/HomeFilter;->filter(Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """.trimIndent())
    }
}

/** Based on the page-separator behavior in Ample's GPLv3 DC Inside patch. */
@Suppress("unused")
val hidePageIndicatorsPatch = bytecodePatch(
    name = "글 목록 페이지 표시 설정",
    description = "앱의 Morphe 설정에서 글 목록과 검색 결과의 Page N 표시를 숨길 수 있습니다.",
    default = true,
) {
    compatibleWith(compatibility)
    dependsOn(dcInsidePersonalPatch)

    execute {
        Fingerprint(
            definingClass = "Lcom/dcinside/app/post/X2;",
            name = "onBindViewHolder",
            parameters = listOf("Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;", "I"),
            returnType = "V",
        ).method.hidePageIndicator()
        Fingerprint(
            definingClass = "Lcom/dcinside/app/post/fragments/u0;",
            name = "h0",
            parameters = listOf("I", "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;", "I"),
            returnType = "V",
        ).method.hidePageIndicator()
    }
}

private fun MutableMethod.hidePageIndicator() {
    val instructions = implementation?.instructions ?: error("Page adapter has no code")
    val resourceIndex = instructions.indexOfFirst {
        (it as? NarrowLiteralInstruction)?.narrowLiteral == 0x7f150843
    }
    check(resourceIndex >= 0) { "Page separator resource changed" }
    val visibilityIndex = (resourceIndex - 1 downTo 0).firstOrNull { index ->
        val instruction = instructions[index]
        val method = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
            method?.definingClass == "Landroid/view/View;" &&
            method.name == "setVisibility" && method.parameterTypes.toString() == "[I]"
    } ?: error("Page separator visibility call changed")
    val call = instructions[visibilityIndex] as? FiveRegisterInstruction
        ?: error("Unexpected page separator call format")
    replaceInstruction(visibilityIndex,
        "invoke-static {v${call.registerC}, v${call.registerD}}, " +
            "Llocal/privacy/PageIndicator;->hide(Landroid/view/View;I)V")
}
