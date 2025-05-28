package com.novel.page.home.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novel.utils.wdp

/**
 * 首页相关工具函数
 */
object HomeUtils {
    
    /**
     * 获取首页标准内边距
     */
    val standardPadding: Dp = 16.wdp
    
    /**
     * 获取首页卡片间距
     */
    val cardSpacing: Dp = 12.wdp
    
    /**
     * 获取首页顶部间距
     */
    val topSpacing: Dp = 20.wdp
    
    /**
     * 计算安全区域内边距
     */
    @Composable
    fun calculateSafePadding(
        systemPadding: PaddingValues,
        additionalHorizontal: Dp = standardPadding,
        additionalVertical: Dp = 0.dp
    ): PaddingValues {
        val layoutDirection = LocalLayoutDirection.current
        return PaddingValues(
            start = systemPadding.calculateStartPadding(layoutDirection) + additionalHorizontal,
            end = systemPadding.calculateEndPadding(layoutDirection) + additionalHorizontal,
            top = systemPadding.calculateTopPadding() + additionalVertical,
            bottom = systemPadding.calculateBottomPadding() + additionalVertical
        )
    }
    
    /**
     * 格式化书籍数量显示
     */
    fun formatBookCount(count: Int): String {
        return when {
            count >= 10000 -> "${count / 10000}万"
            count >= 1000 -> "${count / 1000}k"
            else -> count.toString()
        }
    }
    
    /**
     * 格式化字数显示
     */
    fun formatWordCount(wordCount: Long): String {
        return when {
            wordCount >= 10000 -> "${String.format("%.1f", wordCount / 10000.0)}万字"
            wordCount >= 1000 -> "${String.format("%.1f", wordCount / 1000.0)}千字"
            wordCount > 0 -> "${wordCount}字"
            else -> "暂无"
        }
    }
    
    /**
     * 格式化阅读量显示
     */
    fun formatReadCount(readCount: Int): String {
        return when {
            readCount >= 10000 -> "${String.format("%.1f", readCount / 10000.0)}万"
            readCount >= 1000 -> "${String.format("%.1f", readCount / 1000.0)}k"
            readCount > 0 -> readCount.toString()
            else -> "0"
        }
    }
    
    /**
     * 格式化评分显示
     */
    fun formatRating(rating: Double): String {
        return String.format("%.1f", rating)
    }
    
    /**
     * 获取书籍状态文本
     */
    fun getBookStatusText(isCompleted: Boolean, isVip: Boolean): String {
        return when {
            isCompleted && isVip -> "完结·VIP"
            isCompleted -> "完结"
            isVip -> "连载·VIP"
            else -> "连载"
        }
    }
    
    /**
     * 格式化更新时间显示
     */
    fun formatUpdateTime(updateTime: String?): String {
        if (updateTime.isNullOrEmpty()) return "未知"
        
        // 处理 "02/05 20:31" 格式
        return try {
            if (updateTime.contains("/") && updateTime.contains(":")) {
                updateTime
            } else {
                "最近更新"
            }
        } catch (e: Exception) {
            "未知"
        }
    }
    
    /**
     * 获取分类颜色（根据分类名称）
     */
    fun getCategoryColor(categoryName: String): androidx.compose.ui.graphics.Color {
        return when (categoryName) {
            "玄幻" -> androidx.compose.ui.graphics.Color(0xFF6366F1)
            "武侠仙侠" -> androidx.compose.ui.graphics.Color(0xFF8B5CF6)
            "都市言情" -> androidx.compose.ui.graphics.Color(0xFFEC4899)
            "历史" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
            "科幻灵异" -> androidx.compose.ui.graphics.Color(0xFF10B981)
            "军事" -> androidx.compose.ui.graphics.Color(0xFFEF4444)
            "悬疑" -> androidx.compose.ui.graphics.Color(0xFF6B7280)
            else -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
        }
    }
} 