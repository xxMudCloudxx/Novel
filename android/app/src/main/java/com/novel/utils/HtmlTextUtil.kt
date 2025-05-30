package com.novel.utils

object HtmlTextUtil {

    /**
     * 将 HTML 中的 &nbsp; 替换为空格，
     * 将 <br/><br/> 替换为换行符
     */
    fun cleanHtml(input: String): String {
        return input
            // 全局替换 &nbsp; 为普通空格
            .replace("&nbsp;", " ")
            // 全局替换 <br/><br/> 为换行
            .replace("<br/><br/>", "\n")
    }
}