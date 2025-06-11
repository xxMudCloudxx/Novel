package com.novel.page.search.utils

import android.util.Log

/**
 * 搜索相关的工具类
 */
object SearchUtils {
    
    private const val TAG = "SearchUtils"
    
    /**
     * 清理搜索关键词
     * 去除首尾空格，过滤特殊字符
     */
    fun cleanSearchQuery(query: String): String {
        return query.trim()
            .replace(Regex("[\\n\\r\\t]"), "") // 移除换行符、制表符
            .replace(Regex("\\s+"), " ") // 多个空格合并为一个
    }
    
    /**
     * 验证搜索关键词是否有效
     */
    fun isValidSearchQuery(query: String): Boolean {
        val cleanedQuery = cleanSearchQuery(query)
        return cleanedQuery.isNotBlank() && cleanedQuery.length >= 1
    }
    
    /**
     * 生成搜索建议
     * 基于输入的关键词生成相关搜索建议
     */
    fun generateSearchSuggestions(query: String, history: List<String>): List<String> {
        if (query.isBlank()) return emptyList()
        
        val cleanedQuery = cleanSearchQuery(query).lowercase()
        
        return history.filter { historyItem ->
            historyItem.lowercase().contains(cleanedQuery)
        }.take(5) // 最多返回5个建议
    }
    
    /**
     * 高亮搜索关键词
     * 在文本中高亮显示搜索关键词
     */
    fun highlightSearchKeyword(text: String, keyword: String): String {
        if (keyword.isBlank()) return text
        
        val cleanedKeyword = cleanSearchQuery(keyword)
        if (cleanedKeyword.isBlank()) return text
        
        return text.replace(
            oldValue = cleanedKeyword,
            newValue = "<b>$cleanedKeyword</b>",
            ignoreCase = true
        )
    }
    
    /**
     * 格式化搜索结果数量
     */
    fun formatSearchResultCount(count: Int): String {
        return when {
            count == 0 -> "暂无结果"
            count < 1000 -> "${count}个结果"
            count < 10000 -> "${count / 1000}.${(count % 1000) / 100}k个结果"
            else -> "${count / 10000}.${(count % 10000) / 1000}w个结果"
        }
    }
    
    /**
     * 检查是否为空搜索
     */
    fun isEmptySearch(query: String): Boolean {
        return cleanSearchQuery(query).isBlank()
    }
    
    /**
     * 记录搜索事件
     */
    fun logSearchEvent(query: String, resultCount: Int) {
        Log.d(TAG, "搜索事件: 关键词='$query', 结果数量=$resultCount")
    }
} 