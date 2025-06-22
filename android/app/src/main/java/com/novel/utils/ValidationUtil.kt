package com.novel.utils

/**
 * 输入验证工具类
 * 
 * 功能职责：
 * - 用户名格式验证
 * - 密码强度验证
 * - 统一验证规则管理
 * 
 * 安全特性：
 * - 防止SQL注入字符
 * - 限制输入长度范围
 * - 白名单字符检查
 * 
 * 验证规则：
 * - 用户名：6-16位，字母数字下划线
 * - 密码：8-20位，不含空格
 */
object ValidationUtil {

    /**
     * 验证用户名格式
     * 
     * 规则：
     * - 长度：6-16个字符
     * - 字符：仅允许字母、数字、下划线
     * - 安全：防止特殊字符注入
     * 
     * @param username 待验证的用户名
     * @return true=格式正确，false=格式错误
     */
    fun isValidUsername(username: String): Boolean {
        return username.matches("^[a-zA-Z0-9_]{6,16}$".toRegex())
    }

    /**
     * 验证密码格式
     * 
     * 规则：
     * - 长度：8-20个字符
     * - 限制：不包含空格字符
     * - 安全：防止密码泄露风险
     * 
     * @param password 待验证的密码
     * @return true=格式正确，false=格式错误
     */
    fun isValidPassword(password: String): Boolean {
        return password.length in 8..20 && !password.contains(" ")
    }
}
