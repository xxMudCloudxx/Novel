package com.novel.page.component

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel : ViewModel() {

    private val loadingComponentImpl by lazy { LoadingComponentImpl() }
    val loadingComponent: LoadingComponent get() = loadingComponentImpl

    protected fun showLoading(show: Boolean) {
        loadingComponentImpl.showLoading(show)
    }

    protected fun cancelLoading() {
        loadingComponentImpl.cancelLoading()
    }

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

    private class LoadingComponentImpl : LoadingComponent {

        private val _loading = mutableStateOf(false)
        private val loadingJobs = mutableMapOf<UUID, Job>()

        override val loading: Boolean get() = _loading.value
        override val containsCancelable: Boolean get() = loadingJobs.isNotEmpty()

        override fun showLoading(show: Boolean) {
            _loading.value = show
        }

        override fun cancelLoading() {
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
                jobs[key] = this
            }
            invokeOnCompletion {
                if (cancelable) {
                    jobs.remove(key)
                }
                if (jobs.isEmpty()) {
                    showLoading(false)
                }
            }
        }
    }
}
