package dev.lumi.dcinside

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private val compatibility = Compatibility(
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
        val androidNs = "http://schemas.android.com/apk/res/android"
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider")
            for (index in providers.length - 1 downTo 0) {
                val item = providers.item(index) as Element
                if (item.getAttributeNS(androidNs, "name") in setOf(
                        "com.kakao.adfit.AdFitSdkInitProvider",
                        "com.applovin.sdk.AppLovinInitProvider",
                        "com.google.android.gms.ads.MobileAdsInitProvider"
                    )) item.parentNode.removeChild(item)
            }
            val permissions = document.getElementsByTagName("uses-permission")
            for (index in permissions.length - 1 downTo 0) {
                val item = permissions.item(index) as Element
                val name = item.getAttributeNS(androidNs, "name")
                if (name == "com.google.android.gms.permission.AD_ID" ||
                    name.startsWith("android.permission.ACCESS_ADSERVICES_"))
                    item.parentNode.removeChild(item)
            }
        }
        document("res/layout/view_read_footer.xml").use { document ->
            val views = document.getElementsByTagName("*")
            for (index in 0 until views.length) {
                val view = views.item(index) as Element
                when (view.getAttributeNS(androidNs, "id")) {
                    "@id/read_footer_likes" -> {
                        view.setAttributeNS(androidNs, "android:paddingTop", "12.0dp")
                        view.setAttributeNS(androidNs, "android:paddingBottom", "12.0dp")
                    }
                    "@id/read_footer_ad_container" -> {
                        view.setAttributeNS(androidNs, "android:visibility", "gone")
                        view.setAttributeNS(androidNs, "android:layout_height", "0.0dp")
                    }
                }
            }
        }
        document("res/layout/fragment_post_list.xml").use { document ->
            val views = document.getElementsByTagName("*")
            for (index in 0 until views.length) {
                val view = views.item(index) as Element
                if (view.getAttributeNS(androidNs, "id") == "@id/list_quick_ad_wrap") {
                    val parent = view.parentNode as Element
                    parent.setAttributeNS(androidNs, "android:visibility", "gone")
                    parent.setAttributeNS(androidNs, "android:layout_height", "0.0dp")
                }
            }
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
        Fingerprint(definingClass = "Lcom/dcinside/app/Application;", name = "onCreate", returnType = "V")
            .method.addInstructions(0, "invoke-static {p0}, $CERT->init(Landroid/content/Context;)V")

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
        Fingerprint(definingClass = "Lcom/dcinside/app/post/X2;", name = "a0",
            parameters = listOf("Lcom/dcinside/app/response/j;", "Z"), returnType = "Z")
            .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
        Fingerprint(definingClass = "Lcom/dcinside/app/Application;", name = "p", returnType = "V")
            .method.addInstructions(0, "return-void")
    }
}
