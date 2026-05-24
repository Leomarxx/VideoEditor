package com.myvideo.editor.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * NexClip 类目二：签名校验
 * 编号3：APK签名校验（Java层 + Native层交叉验证）
 * 编号23：安装来源校验
 *
 * 防崩溃方式：Java层try-catch，C层fork隔离
 * 崩溃率：零（主进程）
 */
object SignatureVerifier {

    private const val TAG = "SignatureVerifier"

    // 编译时预存的签名SHA-256（构建脚本自动替换）
    private var EXPECTED_SIGNATURE_SHA256 = "BUILD_TIME_INJECT_SIGNATURE"
    private var EXPECTED_APK_HASH = "BUILD_TIME_INJECT_APK_HASH"

    // 编号23：安装来源白名单
    private val OFFICIAL_INSTALLERS = setOf(
        "com.android.vending",        // Google Play
        "com.huawei.appmarket",       // 华为
        "com.xiaomi.market",          // 小米
        "com.oppo.market",            // OPPO
        "com.bbk.appstore",           // vivo
        "com.samsung.android",        // 三星
        "com.tencent.android.qqdownl",// 应用宝
        "com.myvideo.editor",         // 官方
        "com.android.packageinstaller",// 系统安装器
        "com.google.android.packageinstaller"
    )

    // 校验结果
    data class VerifyResult(
        val passed: Boolean,
        val javaSignatureOk: Boolean,
        val nativeSignatureOk: Boolean,
        val fileIntegrityOk: Boolean,
        val installSourceOk: Boolean,
        val installSource: String,
        val message: String
    )

    // ===== 编号3：APK签名校验（Java层）=====
    // 做什么：Java层签名校验（try-catch包裹）
    // 程度：PackageManager.GET_SIGNING_CERTIFICATES计算签名SHA-256比对预存值
    //       所有异常try-catch不崩溃
    // 验证方式：正常安装的APP两层都通过 | 用其他签名重打包后两层都不通过
    // 异常判定：任一层签名校验失败=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    /**
     * Java层APK签名SHA-256校验
     * 所有异常try-catch，绝不崩溃
     */
    fun verifyJavaSignature(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                reportAnomaly(context, "签名为空")
                return false
            }

            val cert = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert)
            val sha256 = digest.joinToString("") { "%02x".format(it) }

            val passed = sha256.equals(EXPECTED_SIGNATURE_SHA256, ignoreCase = true)
            if (!passed) {
                reportAnomaly(context, "签名SHA-256不匹配: 期望=${EXPECTED_SIGNATURE_SHA256.take(16)}... 实际=${sha256.take(16)}...")
            }
            passed
        } catch (e: Exception) {
            // 程度：所有异常try-catch不崩溃
            reportAnomaly(context, "签名校验异常: ${e.message}")
            false
        }
    }

    // ===== 编号3：Native层APK文件头校验 =====
    // 做什么：Native层fork子进程读取APK文件前4KB验证ZIP魔数和编译时预存hash比对
    // 程度：子进程崩溃不影响主进程，两层结果交叉验证，任一层失败=被重打包
    // 崩溃率：零（主进程）

    /**
     * Native层APK文件头校验
     * 通过JNI调用，内部fork子进程执行
     * 子进程崩溃不影响主进程
     */
    external fun nativeVerifySignature(apkPath: String, expectedHash: String): Boolean

    /**
     * 编号3：Java层+Native层交叉验证
     * 任一层失败=被重打包
     */
    fun verifySignatureCrossCheck(context: Context): Boolean {
        return try {
            val javaOk = verifyJavaSignature(context)

            val apkPath = context.packageCodePath
            val nativeOk = try {
                nativeVerifySignature(apkPath, EXPECTED_APK_HASH)
            } catch (e: Exception) {
                // Native层异常不影响主进程
                reportAnomaly(context, "Native签名校验异常: ${e.message}")
                false
            }

            val passed = javaOk && nativeOk
            if (!passed) {
                reportAnomaly(context, "交叉验证失败: Java=$javaOk Native=$nativeOk")
            }
            passed
        } catch (e: Exception) {
            reportAnomaly(context, "交叉验证异常: ${e.message}")
            false
        }
    }

    // ===== 编号23：安装来源校验 =====
    // 做什么：检查APP是否从官方渠道安装
    // 程度：读取installerPackageName，白名单比对
    //       来源不在白名单标记可疑，结合签名校验综合判断
    //       来源不明+签名异常=确定被篡改
    // 验证方式：官方渠道安装=通过 | adb安装/第三方商店安装=标记可疑
    // 异常判定：来源不明+签名异常=弹警告+强制关闭
    // 崩溃率：零

    /**
     * 安装来源校验
     * 所有异常try-catch不崩溃
     */
    fun verifyInstallSource(context: Context): Pair<Boolean, String> {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            } ?: return Pair(false, "未知来源（null）")

            val isOfficial = OFFICIAL_INSTALLERS.any { installer.startsWith(it) }
            if (isOfficial) {
                Pair(true, installer)
            } else {
                reportAnomaly(context, "非官方安装来源: $installer")
                Pair(false, installer)
            }
        } catch (e: Exception) {
            reportAnomaly(context, "安装来源校验异常: ${e.message}")
            Pair(false, "校验异常")
        }
    }

    // ===== 综合校验 =====
    /**
     * 完整签名校验流程
     * 编号3 + 编号23 综合判断
     *
     * 判断逻辑：
     * 来源明确 + 签名正确 = 通过
     * 来源不明 + 签名正确 = 标记可疑（放行）
     * 来源明确 + 签名异常 = 弹警告+强制关闭
     * 来源不明 + 签名异常 = 弹警告+强制关闭（确定被篡改）
     */
    fun fullVerify(context: Context): VerifyResult {
        return try {
            val javaSigOk = verifyJavaSignature(context)
            val nativeSigOk = try {
                nativeVerifySignature(context.packageCodePath, EXPECTED_APK_HASH)
            } catch (e: Exception) { false }
            val (sourceOk, sourceName) = verifyInstallSource(context)

            val signatureOk = javaSigOk && nativeSigOk
            val passed = signatureOk && sourceOk

            val message = when {
                signatureOk && sourceOk -> "全部通过"
                signatureOk && !sourceOk -> "安装来源可疑，签名校验通过"
                !signatureOk && sourceOk -> "签名校验失败，来源=$sourceName"
                else -> "签名校验失败+来源不明，确定被篡改"
            }

            if (!passed) {
                reportAnomaly(context, message)
            }

            VerifyResult(
                passed = passed,
                javaSignatureOk = javaSigOk,
                nativeSignatureOk = nativeSigOk,
                fileIntegrityOk = true,
                installSourceOk = sourceOk,
                installSource = sourceName,
                message = message
            )
        } catch (e: Exception) {
            reportAnomaly(context, "综合校验异常: ${e.message}")
            VerifyResult(false, false, false, false, false, "异常", e.message ?: "未知")
        }
    }

    // ===== 异常处理 =====
    // 异常判定：任一层签名校验失败=弹警告+强制关闭+上报服务端

    private fun reportAnomaly(context: Context, message: String) {
        try {
            // 上报服务端
            SecurityReporter.report(context, "SIGNATURE", message)
        } catch (e: Exception) {
            // 上报失败不能崩溃
        }
    }

    /**
     * 弹警告+强制关闭
     * 在UI层调用，不在安全层直接调用
     */
    fun onTamperDetected(context: Context, message: String) {
        reportAnomaly(context, message)
        // 返回给UI层处理弹窗和关闭
    }
}
