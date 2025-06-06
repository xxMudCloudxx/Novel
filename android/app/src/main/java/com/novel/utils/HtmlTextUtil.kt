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
            .replace("<br/><br/>", "\n\n\n")
            // 全局替换 &amp; 为 &
            .replace("&amp;", "&")
            // 全局替换 &lt; 为 <
            .replace("&lt;", "<")
            // 全局替换 &gt; 为 >
            .replace("&gt;", ">")
            // 全局替换 &quot; 为 "
            .replace("&quot;", "\"")
            // 全局替换 &#39; 为 '
            .replace("&#39;", "'")
            // 全局替换 <p> 为换行
            .replace("<p>", "")
            // 全局替换 </p> 为换行
            .replace("</p>", "")
    }

    /**
     * 去除HTML标签，保留纯文本内容
     */
    fun removeHtmlTags(input: String): String {
        return input
            // 去除HTML标签
            .replace(Regex("<[^>]+>"), "")
            // 替换HTML实体
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            // 清理多余的空白字符
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}