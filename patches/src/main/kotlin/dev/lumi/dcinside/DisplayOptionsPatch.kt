package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Opt-in display choices for the verified 5.3.6 adapter. */
@Suppress("unused")
val homeDisplayOptionsPatch = bytecodePatch(
    name = "Hide home screen sections",
    description = "Choose which home sections to hide when patching the APK (5.3.6).",
    default = false,
) {
    compatibleWith(compatibility)
    dependsOn(dcInsidePersonalPatch)

    val search = booleanOption("hideHomeSearch", false, title = "Hide search/menu")
    val recent = booleanOption("hideHomeRecent", false, title = "Hide recent galleries")
    val recommendedGalleries = booleanOption("hideHomeRecommendedGalleries", false, title = "Hide recommended galleries")
    val ranking = booleanOption("hideHomeRanking", false, title = "Hide gallery ranking")
    val liveBest = booleanOption("hideHomeLiveBest", false, title = "Hide live best")
    val recommendedPosts = booleanOption("hideHomeRecommendedPosts", false, title = "Hide recommended posts")

    execute {
        val mask = (if (search.value == true) 1 else 0) or
            (if (recent.value == true) 2 else 0) or
            (if (recommendedGalleries.value == true) 4 else 0) or
            (if (ranking.value == true) 8 else 0) or
            (if (liveBest.value == true) 16 else 0) or
            (if (recommendedPosts.value == true) 32 else 0)
        if (mask == 0) return@execute

        val update = Fingerprint(
            definingClass = "Lcom/dcinside/app/main/adapter/f;",
            name = "Z",
            parameters = listOf("Ljava/util/List;"),
            returnType = "V",
        ).method
        update.addInstructions(0, """
            const/16 v0, 0x${mask.toString(16)}
            invoke-static {p1, v0}, Llocal/privacy/HomeFilter;->filter(Ljava/util/List;I)Ljava/util/List;
            move-result-object p1
        """.trimIndent())
    }
}

/** Based on the page-separator behavior in Ample's GPLv3 DC Inside patch. */
@Suppress("unused")
val hidePageIndicatorsPatch = bytecodePatch(
    name = "Hide post list page indicators",
    description = "Hide Page N labels between post-list pages and in search results (5.3.6).",
    default = false,
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
