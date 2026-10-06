package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

internal val compatibility = Compatibility(
    name = "DC Inside",
    packageName = "com.dcinside.app.android",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x495C9E,
    targets = listOf(
        AppTarget(version = "5.3.6", isExperimental = true),
        AppTarget(version = null, isExperimental = true)
    )
)

private const val CONFIG = "Lcom/dcinside/app/util/Rr;"
private const val CERT = "Llocal/privacy/SignerCompat;"
private const val JNI_DIGEST = "java/security/MessageDigest"
private const val LOCAL_DIGEST = "local/privacy/MessageDigest"

private val nativeCertificatePatch = resourcePatch {
    execute {
        val prefixTable = Class.forName("dev.lumi.dcinside.DcInsidePatchKt")
            .getResourceAsStream("/dcinside/lumi_ip_prefixes.txt")
            ?: error("IP prefix table missing from bundle")
        val prefixTarget = this["res/raw/lumi_ip_prefixes.txt"]
        prefixTarget.parentFile?.mkdirs()
        prefixTable.use { source -> prefixTarget.outputStream().use { source.copyTo(it) } }
        check(JNI_DIGEST.length == LOCAL_DIGEST.length)
        val paths = listApkEntries("lib/").filter { it.endsWith("/libnative-lib.so") }
        check(paths.isNotEmpty()) { "Native identity library missing; app version needs review" }
        paths.forEach { path ->
            val file = get(path)
            val before = file.readBytes().toString(Charsets.ISO_8859_1)
            val count = Regex(JNI_DIGEST).findAll(before).count()
            check(count >= 2) { "JNI fingerprint changed in $path" }
            val updated = before.replace(JNI_DIGEST, LOCAL_DIGEST).toByteArray(Charsets.ISO_8859_1)
            check(updated.size == file.length().toInt())
            file.writeBytes(updated)
        }
        document("AndroidManifest.xml").use { document ->
            val application = document.documentElement.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", "local.privacy.MorpheSettingsActivity")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.Light")
            application.appendChild(activity)
            val providers = document.getElementsByTagName("provider")
            for (index in providers.length - 1 downTo 0) {
                val item = providers.item(index) as Element
                if (item.getAttribute("android:name") in setOf(
                        "com.kakao.adfit.AdFitSdkInitProvider",
                        "com.applovin.sdk.AppLovinInitProvider",
                        "com.google.android.gms.ads.MobileAdsInitProvider"
                    )) item.parentNode.removeChild(item)
            }
            val permissions = document.getElementsByTagName("uses-permission")
            for (index in permissions.length - 1 downTo 0) {
                val item = permissions.item(index) as Element
                val name = item.getAttribute("android:name")
                if (name == "com.google.android.gms.permission.AD_ID" ||
                    name.startsWith("android.permission.ACCESS_ADSERVICES_"))
                    item.parentNode.removeChild(item)
            }
            val flags = mapOf(
                "firebase_analytics_collection_deactivated" to "true",
                "firebase_analytics_collection_enabled" to "false",
                "google_analytics_adid_collection_enabled" to "false",
                "google_analytics_default_allow_ad_personalization_signals" to "false",
                "firebase_performance_collection_deactivated" to "true",
                "firebase_crashlytics_collection_enabled" to "false",
                "firebase_sessions_enabled" to "false",
            )
            val metadata = document.getElementsByTagName("meta-data")
            flags.forEach { (name, value) ->
                val existing = (0 until metadata.length).map { metadata.item(it) as Element }
                    .firstOrNull { it.getAttribute("android:name") == name }
                val element = existing ?: document.createElement("meta-data").also {
                    document.documentElement.getElementsByTagName("application").item(0).appendChild(it)
                }
                element.setAttribute("android:name", name)
                element.setAttribute("android:value", value)
            }
        }
        document("res/layout/view_read_footer.xml").use { document ->
            val views = document.getElementsByTagName("*")
            var likesCount = 0
            var adCount = 0
            for (index in 0 until views.length) {
                val view = views.item(index) as Element
                when (view.getAttribute("android:id")) {
                    "@id/read_footer_likes" -> {
                        likesCount++
                        view.setAttribute("android:paddingTop", "12.0dp")
                        view.setAttribute("android:paddingBottom", "12.0dp")
                        view.setAttribute("android:layout_marginTop", "12.0dp")
                        view.setAttribute("app:layout_goneMarginTop", "12.0dp")
                    }
                    "@id/read_footer_ad_container" -> {
                        adCount++
                        view.setAttribute("android:visibility", "gone")
                        view.setAttribute("android:layout_height", "0.0dp")
                    }
                }
            }
            check(likesCount == 1 && adCount == 1) { "Read footer layout changed" }
        }
        document("res/layout/fragment_post_list.xml").use { document ->
            val views = document.getElementsByTagName("*")
            var adWrapCount = 0
            for (index in 0 until views.length) {
                val view = views.item(index) as Element
                if (view.getAttribute("android:id") == "@id/list_quick_ad_wrap") {
                    adWrapCount++
                    val parent = view.parentNode as Element
                    parent.setAttribute("android:visibility", "gone")
                    parent.setAttribute("android:layout_height", "0.0dp")
                }
            }
            check(adWrapCount == 1) { "Gallery ad strip layout changed" }
        }
        document("res/values/dimens.xml").use { document ->
            val adDimensions = setOf(
                "ad_main_small_native", "ad_minimum", "ad_minimum_tall",
                "main_ad_live_best_spacing", "read_ad_minimum", "image_ad"
            )
            val dimensions = document.getElementsByTagName("dimen")
            val matched = mutableSetOf<String>()
            for (index in 0 until dimensions.length) {
                val dimension = dimensions.item(index) as Element
                val name = dimension.getAttribute("name")
                if (name in adDimensions) {
                    check(matched.add(name)) { "Duplicate ad dimension: $name" }
                    dimension.textContent = "0dp"
                }
            }
            check(matched == adDimensions) { "Ad dimensions changed: missing ${adDimensions - matched}" }
        }
    }
}

/** Does not contain the APK, keys, or proprietary source. */
@Suppress("unused")
val dcInsidePersonalPatch = bytecodePatch(
    name = "DC Inside personal clean-up",
    description = "Disable app ad loaders and keep the locally signed build usable.",
    default = true
) {
    compatibleWith(compatibility)
    dependsOn(nativeCertificatePatch)
    extendWith("extensions/extension.mpe")
    execute {
        // Initialize the certificate adapter before native app-identity calls.
        val appCreate = Fingerprint(definingClass = "Lcom/dcinside/app/Application;", name = "onCreate", returnType = "V").method
        appCreate.addInstructions(0, "invoke-static {p0}, $CERT->init(Landroid/content/Context;)V")
        appCreate.addInstructions(0, "invoke-static {p0}, Llocal/privacy/SettingsState;->init(Landroid/content/Context;)V")
        val settingsView = Fingerprint(definingClass = "Lcom/dcinside/app/settings/K1;", name = "onViewCreated",
            parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"), returnType = "V").method
        val settingsSuper = settingsView.implementation!!.instructions.indexOfFirst { instruction ->
            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            instruction.opcode == Opcode.INVOKE_SUPER && ref?.name == "onViewCreated"
        }
        check(settingsSuper >= 0) { "Settings screen lifecycle changed" }
        settingsView.addInstructions(settingsSuper + 1,
            "invoke-static {p1}, Llocal/privacy/MorpheSettingsActivity;->bindSettingsShortcut(Landroid/view/View;)V")
        val crashSwitch = appCreate.implementation!!.instructions.indexOfFirst {
            (it as? ReferenceInstruction)?.reference.let { ref ->
                (ref as? MethodReference)?.let { m ->
                    m.definingClass == "Lcom/google/firebase/crashlytics/j;" &&
                        m.name == "l" && m.parameterTypes.toString() == "[Z]"
                } == true
            }
        }
        check(crashSwitch >= 0) { "Crashlytics switch changed" }
        // The boolean register is reused much later for Realm's UI-thread write setting.
        // Changing its value here makes post navigation fail with a Realm exception.
        // Manifest metadata deactivates Crashlytics; omit this explicit enable call.
        appCreate.replaceInstruction(crashSwitch, "nop")

        // Preserve the vendor allowlist; add only the certificate installed by Morphe.
        val allowed = Fingerprint(
            definingClass = "Lcom/dcinside/app/auth/b;", name = "f",
            parameters = listOf("Lcom/dcinside/app/auth/i0;"), returnType = "Ljava/util/Set;"
        ).method
        val returns = allowed.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_OBJECT }
        check(returns.size == 2) { "Certificate allowlist changed" }
        returns.asReversed().forEach { (index, instruction) ->
            val register = (instruction as OneRegisterInstruction).registerA
            allowed.addInstructions(index, """
                invoke-static {v$register}, $CERT->withInstalledSigner(Ljava/util/Set;)Ljava/util/Set;
                move-result-object v$register
            """.trimIndent())
        }

        // Fixed-value ad configuration; validate every method before changing it.
        Fingerprint(definingClass = CONFIG, name = "d", parameters = listOf("Ljava/lang/String;"),
            returnType = "Ljava/util/List;").method.addInstructions(0, """
                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object p0
                return-object p0
            """.trimIndent())
        listOf("r", "C0", "v0", "I0", "J0", "K0", "L0").forEach { name ->
            Fingerprint(definingClass = CONFIG, name = name, returnType = "Z")
                .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
        }
        listOf("t", "t0").forEach { name ->
            Fingerprint(definingClass = CONFIG, name = name, returnType = "I")
                .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
        }
        listOf("x", "y", "s0").forEach { name ->
            Fingerprint(definingClass = CONFIG, name = name, returnType = "Ljava/lang/String;")
                .method.addInstructions(0, "const-string p0, \"\"\nreturn-object p0")
        }
        Fingerprint(definingClass = "Lcom/dcinside/app/ad/support/J;", name = "a",
            parameters = listOf("Landroidx/appcompat/app/AppCompatActivity;"), returnType = "V")
            .method.addInstructions(0, "return-void")
        Fingerprint(definingClass = "LF/a\$a;", name = "a",
            parameters = listOf("Ljava/util/List;"), returnType = "I")
            .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
        Fingerprint(definingClass = "Lcom/dcinside/app/ad/support/a;", name = "b",
            parameters = listOf("Lkotlin/jvm/functions/Function1;"), returnType = "V")
            .method.addInstructions(0, """
                const/4 v0, 0x0
                invoke-interface {p1, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                return-void
            """.trimIndent())
        Fingerprint(definingClass = "Lcom/dcinside/app/util/b;", name = "b",
            parameters = listOf("Landroid/content/Context;", "Lcom/dcinside/app/util/b\$a;"),
            returnType = "V").method.addInstructions(0, "return-void")
        Fingerprint(definingClass = "Lcom/dcinside/app/post/h;", name = "h",
            parameters = listOf("Landroidx/appcompat/app/AppCompatActivity;"), returnType = "V")
            .method.addInstructions(0, "return-void")
        listOf("A2" to emptyList<String>(), "F2" to listOf("Ljava/lang/String;"),
            "x0" to emptyList()).forEach { (name, parameters) ->
            Fingerprint(definingClass = "Lcom/dcinside/app/post/fragments/z3;", name = name,
                parameters = parameters, returnType = "V")
                .method.addInstructions(0, "return-void")
        }
        Fingerprint(definingClass = "Lcom/dcinside/app/view/f0;", name = "x0", returnType = "V")
            .method.addInstructions(0, "return-void")
        // These ad views remain in RecyclerView/footer layouts even when ad requests are disabled.
        // Collapse their own bounds without touching post rows or user controls.
        listOf("t1" to "g", "v1" to "d").forEach { (name, field) ->
            Fingerprint(definingClass = "Lcom/dcinside/app/view/f0;", name = name,
                parameters = listOf("Landroidx/lifecycle/LifecycleOwner;", "Lcom/dcinside/app/read/V\$a;"),
                returnType = "V").method.addInstructions(0, """
                    iget-object v0, p0, Lcom/dcinside/app/view/f0;->I:LO/N7;
                    iget-object v1, v0, LO/N7;->$field:Landroid/widget/LinearLayout;
                    const/4 v2, 0x0
                    invoke-virtual {v1, v2}, Landroid/view/View;->setMinimumHeight(I)V
                    const/16 v2, 0x8
                    invoke-virtual {v1, v2}, Landroid/view/View;->setVisibility(I)V
                    iget-object v1, v0, LO/N7;->h:Landroid/view/View;
                    invoke-virtual {v1, v2}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent())
        }
        Fingerprint(definingClass = "Lcom/dcinside/app/post/fragments/E2;", name = "k",
            parameters = listOf("Landroidx/lifecycle/LifecycleOwner;", "Ljava/lang/String;", "I", "Z", "LF/d;"),
            returnType = "V").method.addInstructions(0, """
                const/16 v1, 0x8
                iget-object v0, p0, Lcom/dcinside/app/post/fragments/E2;->g:Landroid/widget/FrameLayout;
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                iget-object v0, p0, Lcom/dcinside/app/post/fragments/E2;->h:Landroid/view/View;
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                return-void
            """.trimIndent())
        listOf("c" to "k", "e" to "i").forEach { (owner, name) ->
            Fingerprint(definingClass = "Lcom/dcinside/app/ad/naver/$owner;", name = name,
                parameters = listOf("Landroidx/lifecycle/LifecycleOwner;", "Ljava/lang/String;",
                    "Ljava/lang/String;", "Lcom/dcinside/app/ad/naver/a;"), returnType = "V")
                .method.addInstructions(0, """
                    iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
                    const/16 v1, 0x8
                    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Landroid/view/View;->setMinimumHeight(I)V
                    return-void
                """.trimIndent())
        }
        Fingerprint(definingClass = "Lcom/dcinside/app/post/X2;", name = "a0",
            parameters = listOf("Lcom/dcinside/app/response/j;", "Z"), returnType = "Z")
            .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
        // Preserve the bottom navigation; override only the ad strip's later visibility toggle.
        val quickBar = Fingerprint(definingClass = "Lcom/dcinside/app/post/fragments/w2;", name = "f5",
            parameters = listOf("Z"), returnType = "V").method
        val visibilityCalls = quickBar.implementation!!.instructions.withIndex().filter { (_, insn) ->
            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Landroid/view/View;" && ref.name == "setVisibility" &&
                ref.parameterTypes.toString() == "[I]"
        }
        check(visibilityCalls.size == 2) { "Quick bar visibility layout changed" }
        val (adVisibilityIndex, adVisibilityInsn) = visibilityCalls.last()
        val adVisibilityCall = adVisibilityInsn as FiveRegisterInstruction
        quickBar.replaceInstruction(adVisibilityIndex,
            "invoke-static {v${adVisibilityCall.registerC}, v${adVisibilityCall.registerD}}, " +
                "Llocal/privacy/AdSpace;->keepGone(Landroid/view/View;I)V")

        // Reuse activated values on transient configuration failure; throttle only redundant home-stop refreshes.
        val configCallback = Fingerprint(definingClass = CONFIG, name = "c",
            parameters = listOf("LW3/o;", "Lcom/google/android/gms/tasks/Task;"), returnType = "V").method
        val taskResultIndex = configCallback.implementation!!.instructions.indexOfFirst {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Lcom/google/android/gms/tasks/Task;" && ref.name == "isSuccessful"
        }
        check(taskResultIndex >= 0 && configCallback.implementation!!.instructions[taskResultIndex + 1].opcode == Opcode.MOVE_RESULT)
        configCallback.addInstructions(taskResultIndex + 2, """
            invoke-static {v0}, Llocal/privacy/RemoteConfigFallback;->successOrActivated(Z)Z
            move-result v0
        """.trimIndent())
        val startupTimeout = Fingerprint(definingClass = "Lcom/dcinside/app/main/A0;", name = "F2",
            parameters = listOf("Lcom/dcinside/app/main/A0;", "Landroidx/fragment/app/FragmentActivity;",
                "Ljava/lang/Throwable;"), returnType = "V").method
        val startupCheck = startupTimeout.implementation!!.instructions.indexOfFirst {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Lcom/dcinside/app/util/ms;" && ref.name == "y"
        }
        check(startupCheck >= 0 && startupTimeout.implementation!!.instructions[startupCheck + 1].opcode == Opcode.MOVE_RESULT)
        startupTimeout.addInstructions(startupCheck + 2, """
            invoke-static {p1}, Llocal/privacy/RemoteConfigFallback;->successOrActivated(Z)Z
            move-result p1
        """.trimIndent())
        Fingerprint(definingClass = CONFIG, name = "b", parameters = listOf("LW3/o;"), returnType = "V")
            .method.addInstructions(0, "invoke-static {}, Llocal/privacy/RemoteConfigRefreshGate;->markAttempt()V")
        val homeStop = Fingerprint(definingClass = "Lcom/dcinside/app/main/E1;", name = "onStop", returnType = "V").method
        val superStop = homeStop.implementation!!.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER }
        check(superStop >= 0) { "Home lifecycle changed" }
        homeStop.addInstructionsWithLabels(superStop + 1, """
            invoke-static {}, Llocal/privacy/RemoteConfigRefreshGate;->shouldRefreshOnHomeStop()Z
            move-result v0
            if-nez v0, :continue_refresh
            return-void
            :continue_refresh
            nop
        """.trimIndent())
        Fingerprint(definingClass = "Lcom/dcinside/app/Application;", name = "p", returnType = "V")
            .method.addInstructions(0, "return-void")
    }
}
