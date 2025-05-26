package com.novel.utils

/**
 * 将手机号中间四位替换为星号
 * 输入长度不足 7 位时，直接返回原字符串
 * @param number 手机号
 */
fun maskPhoneNumber(number: String): String {
    return if (number.length >= 9) {
        val start = number.take(3)
        val end   = number.takeLast(4)
        "$start****$end"
    } else {
        number
    }
}