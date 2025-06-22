package com.novel.utils

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 字符串资源提供器接口
 * 
 * 设计目的：
 * - 抽象Android资源访问，便于单元测试
 * - 支持依赖注入和Mock替换
 * - 统一字符串资源管理接口
 * 
 * 使用场景：
 * - 业务逻辑层需要获取字符串资源
 * - 单元测试中Mock字符串返回值
 * - 国际化文本处理
 */
interface StringProvider {
    /**
     * 获取字符串资源
     * @param resId 资源ID
     * @param args 格式化参数
     * @return 格式化后的字符串
     */
    fun getString(resId: Int, vararg args: Any): String
}

/**
 * Android字符串资源提供器实现
 * 
 * 特点：
 * - Hilt单例自动注入
 * - 直接调用Android Context
 * - 支持字符串格式化
 * - 生产环境默认实现
 */
@Singleton
class AndroidStringProvider @Inject constructor(
    private val context: Context
) : StringProvider {
    
    /**
     * 获取Android字符串资源
     * 支持格式化参数传入
     */
    override fun getString(resId: Int, vararg args: Any): String =
        context.getString(resId, *args)
}
