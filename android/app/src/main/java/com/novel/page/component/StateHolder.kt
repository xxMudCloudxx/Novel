package com.novel.page.component

/**
 * 通用状态持有者，用于封装数据、加载状态和错误信息
 */
data class StateHolderImpl<T>(
    val data: T,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasError: Boolean get() = error != null
    val isEmpty: Boolean get() = data == null && !isLoading && !hasError
    val isSuccess: Boolean get() = data != null && !isLoading && !hasError
}

/**
 * 状态持有者构建器
 */
object StateHolder {
    fun <T> loading(data: T? = null): StateHolderImpl<T?> = StateHolderImpl(
        data = data,
        isLoading = true,
        error = null
    )
    
    fun <T> success(data: T): StateHolderImpl<T> = StateHolderImpl(
        data = data,
        isLoading = false,
        error = null
    )
    
    fun <T> error(error: String, data: T? = null): StateHolderImpl<T?> = StateHolderImpl(
        data = data,
        isLoading = false,
        error = error
    )
} 