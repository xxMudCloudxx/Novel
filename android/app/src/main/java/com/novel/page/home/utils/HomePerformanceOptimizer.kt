package com.novel.page.home.utils

/**
 * 首页性能优化工具类 - 精简版
 */
object HomePerformanceOptimizer {
    
    /**
     * 图片高度缓存 - 避免重复计算随机高度
     * ⚠ 安全检查：确保高度值在合理范围内
     */
    private val imageHeightCache = mutableMapOf<String, Int>()
    
    fun getOptimizedImageHeight(bookId: String, minHeight: Int = 280, maxHeight: Int = 330): Int {
        // ⚠ 安全检查：验证高度参数
        val safeMinHeight = minHeight.coerceAtLeast(100)
        val safeMaxHeight = maxHeight.coerceAtMost(500).coerceAtLeast(safeMinHeight)
        
        return imageHeightCache.getOrPut(bookId) {
            (safeMinHeight..safeMaxHeight).random()
        }
    }
    
    /**
     * 内存优化 - 定期清理缓存
     */
    fun optimizeMemory() {
        if (imageHeightCache.size > 1000) {
            // 保留最近使用的500个缓存项
            val allEntries = imageHeightCache.toList()
            val recentEntries = allEntries.takeLast(500)
            imageHeightCache.clear()
            imageHeightCache.putAll(recentEntries.toMap())
        }
    }
} 