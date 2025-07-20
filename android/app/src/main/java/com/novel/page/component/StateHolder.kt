package com.novel.page.component

import androidx.compose.runtime.Stable

/**
 * 通用状态持有者
 * 
 * 功能：
 * - 封装数据、加载状态和错误信息
 * - 提供状态判断便捷方法
 * - 支持泛型数据类型
 * 
 * @param T 数据类型
 * @param data 业务数据
 * @param isLoading 是否正在加载
 * @param error 错误信息
 */
@Stable
data class StateHolderImpl<T>(
    val data: T,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /** 是否有错误 */
    val hasError: Boolean get() = error != null
    
    /** 是否为空状态 */
    val isEmpty: Boolean get() = data == null && !isLoading && !hasError
    
    /** 是否成功状态 */
    val isSuccess: Boolean get() = data != null && !isLoading && !hasError
}
