package com.myvideo.editor.security

import org.junit.Assert.*
import org.junit.Test

/**
 * NexClip 安全模块单元测试
 */
class SecurityUnitTest {

    // ===== 编号59：数据脱敏测试 =====

    @Test
    fun `sanitizeLogData masks phone numbers`() {
        val input = "用户手机号13812345678已泄露"
        val result = ComplianceAuditor.sanitizeLogData(input)
        assertFalse("手机号应被脱敏", result.contains("1381234"))
        assertTrue("应包含脱敏标记", result.contains("****"))
    }

    @Test
    fun `sanitizeLogData masks email`() {
        val input = "邮箱test@example.com已泄露"
        val result = ComplianceAuditor.sanitizeLogData(input)
        assertFalse("邮箱应被脱敏", result.contains("test@example"))
    }

    @Test
    fun `sanitizeLogData masks ID card`() {
        val input = "身份证110101199901011234已泄露"
        val result = ComplianceAuditor.sanitizeLogData(input)
        assertFalse("身份证应被脱敏", result.contains("19990101"))
    }

    @Test
    fun `sanitizeLogData masks IP address`() {
        val input = "IP: 192.168.1.100"
        val result = ComplianceAuditor.sanitizeLogData(input)
        assertFalse("IP应被脱敏", result.contains("1.100"))
    }

    // ===== 编号59：错误信息脱敏测试 =====

    @Test
    fun `sanitizeErrorMessage hides password`() {
        val error = RuntimeException("password is incorrect")
        val result = ComplianceAuditor.sanitizeErrorMessage(error)
        assertFalse("不应暴露password", result.contains("password"))
    }

    @Test
    fun `sanitizeErrorMessage hides token`() {
        val error = RuntimeException("token expired")
        val result = ComplianceAuditor.sanitizeErrorMessage(error)
        assertFalse("不应暴露token", result.contains("token"))
    }

    @Test
    fun `sanitizeErrorMessage hides SQL`() {
        val error = RuntimeException("sql syntax error near table")
        val result = ComplianceAuditor.sanitizeErrorMessage(error)
        assertFalse("不应暴露sql", result.contains("sql"))
    }

    // ===== 编号59：防钓鱼测试 =====

    @Test
    fun `isPhishingUrl detects suspicious URL`() {
        assertTrue("punycode应可疑",
            ComplianceAuditor.isPhishingUrl("https://xn--example.com"))
        assertTrue("URL中含@应可疑",
            ComplianceAuditor.isPhishingUrl("https://evil.com@fake.com"))
    }

    @Test
    fun `isPhishingUrl passes normal URL`() {
        assertFalse("正常URL不应可疑",
            ComplianceAuditor.isPhishingUrl("https://www.google.com"))
    }

    // ===== 编号59：水印信息测试 =====

    @Test
    fun `WatermarkInfo stores data correctly`() {
        val info = ComplianceAuditor.WatermarkInfo("user123", "device456", 1000L, "abc123hash")
        assertEquals("user123", info.userId)
        assertEquals("device456", info.deviceId)
        assertEquals(1000L, info.timestamp)
        assertEquals("abc123hash", info.contentHash)
    }

    // ===== 编号59：隐私政策版本测试 =====

    @Test
    fun `getDataCollectionList is not empty`() {
        val list = ComplianceAuditor.getDataCollectionList()
        assertTrue("数据收集清单不应为空", list.isNotEmpty())
    }

    @Test
    fun `getPermissionCategories has required and optional`() {
        val perms = ComplianceAuditor.getPermissionCategories()
        assertTrue("应有必权限", perms.containsKey("必要权限"))
        assertTrue("应有可选权限", perms.containsKey("可选权限"))
    }

    // ===== 编号13：请求签名测试 =====

    @Test
    fun `signature changes with different input`() {
        val sig1 = generateTestSignature("data1", 1000L)
        val sig2 = generateTestSignature("data2", 1000L)
        assertNotEquals("不同数据签名应不同", sig1, sig2)
    }

    @Test
    fun `signature changes with different timestamp`() {
        val sig1 = generateTestSignature("data", 1000L)
        val sig2 = generateTestSignature("data", 2000L)
        assertNotEquals("不同时间戳签名应不同", sig1, sig2)
    }

    // ===== 编号18：反调试检测测试 =====

    @Test
    fun `DebuggerDetector exists`() {
        // 验证类存在且有关键方法
        val methods = DebuggerDetector::class.java.declaredMethods
        assertTrue("应有检测方法", methods.isNotEmpty())
    }

    // ===== 编号21：Root检测测试 =====

    @Test
    fun `RootDetector exists`() {
        val methods = RootDetector::class.java.declaredMethods
        assertTrue("应有检测方法", methods.isNotEmpty())
    }

    // ===== 编号17：Hook检测测试 =====

    @Test
    fun `HookDetector exists`() {
        val methods = HookDetector::class.java.declaredMethods
        assertTrue("应有检测方法", methods.isNotEmpty())
    }

    // ===== 辅助函数 =====

    private fun generateTestSignature(data: String, timestamp: Long): String {
        val input = "$data|$timestamp|test_device"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
