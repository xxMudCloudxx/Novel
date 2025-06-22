package com.novel.page.component

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 基础ViewModel类
 * 
 * 核心功能：
 * - 提供统一的加载状态管理
 * - 封装协程启动和生命周期管理
 * - 自动处理加载指示器的显示和隐藏
 * - 支持可取消和不可取消的异步任务
 * 
 * 设计特点：
 * - 内置LoadingComponent实现，避免重复代码
 * - 使用UUID跟踪异步任务，确保状态一致性
 * - 支持多任务并发，自动管理加载状态
 * - 提供类型安全的协程扩展函数
 */
abstract class BaseViewModel : ViewModel() {

    companion object {
        private const val TAG = "BaseViewModel"
    }

    /** 懒加载的加载组件实现，避免不必要的初始化开销 */
    private val loadingComponentImpl by lazy { 
        Log.d(TAG, "初始化LoadingComponent")
        LoadingComponentImpl() 
    }

    /**
     * 启动带加载状态的协程
     * 
     * @param context 协程上下文，默认为EmptyCoroutineContext
     * @param start 协程启动模式，默认为DEFAULT
     * @param cancelable 是否可取消，默认为true
     * @param block 协程执行体
     * @return Job对象，用于管理协程生命周期
     */
    protected fun CoroutineScope.launchWithLoading(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        cancelable: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ): Job = with(loadingComponentImpl) {
        launchWithLoading(
            context = context,
            start = start,
            cancelable = cancelable,
            block = block
        )
    }

    /**
     * 加载组件的内部实现
     * 
     * 职责：
     * - 管理全局加载状态
     * - 跟踪活跃的可取消任务
     * - 自动显示/隐藏加载指示器
     * - 提供批量任务取消功能
     */
    private class LoadingComponentImpl : LoadingComponent {

        /** 响应式加载状态，用于UI绑定 */
        private val _loading = mutableStateOf(false)
        /** 活跃任务映射表，用于任务生命周期管理 */
        private val loadingJobs = mutableMapOf<UUID, Job>()

        /** 当前是否处于加载状态 */
        override val loading: Boolean get() = _loading.value
        /** 是否包含可取消的任务 */
        override val containsCancelable: Boolean get() = loadingJobs.isNotEmpty()

        /**
         * 设置加载状态
         * @param show 是否显示加载状态
         */
        override fun showLoading(show: Boolean) {
            if (_loading.value != show) {
                Log.d(TAG, "更新加载状态: $show")
                _loading.value = show
            }
        }

        /**
         * 取消所有可取消的加载任务
         * 清理所有活跃任务并隐藏加载指示器
         */
        override fun cancelLoading() {
            Log.d(TAG, "取消所有加载任务，当前任务数: ${loadingJobs.size}")
            showLoading(false)
            val jobs = loadingJobs
            if (jobs.isEmpty()) {
                return
            }

            jobs.forEach { job ->
                job.value.cancel()
            }
            jobs.clear()
        }

        /**
         * 启动带加载状态管理的协程
         * 
         * 自动处理：
         * - 启动时显示加载状态
         * - 完成时隐藏加载状态（如果没有其他任务）
         * - 可取消任务的注册和清理
         * - 异常处理和状态恢复
         */
        fun CoroutineScope.launchWithLoading(
            context: CoroutineContext = EmptyCoroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            cancelable: Boolean = true,
            block: suspend CoroutineScope.() -> Unit
        ): Job = launch(
            context = context,
            start = start,
            block = block
        ).apply {
            showLoading(true)
            val jobs = loadingJobs
            val key = UUID.randomUUID()
            
            if (cancelable) {
                Log.d(TAG, "注册可取消任务: $key")
                jobs[key] = this
            }
            
            // 任务完成时的清理逻辑
            invokeOnCompletion { exception ->
                if (cancelable) {
                    jobs.remove(key)
                    Log.d(TAG, "清理任务: $key, 剩余任务数: ${jobs.size}")
                }
                if (jobs.isEmpty()) {
                    showLoading(false)
                }
                
                // 记录异常信息（不影响性能）
                exception?.let { 
                    Log.w(TAG, "任务完成时发生异常: ${it.message}")
                }
            }
        }
    }
}
