package com.novel.page.search.usecase

import com.novel.page.search.repository.SearchRepository
import javax.inject.Inject

/**
 * 切换搜索历史展开状态UseCase
 */
class ToggleHistoryExpansionUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    /**
     * 切换搜索历史展开状态
     * @param currentState 当前展开状态
     * @return 新的展开状态
     */
    suspend operator fun invoke(currentState: Boolean): Boolean {
        val newState = !currentState
        searchRepository.saveHistoryExpansionState(newState)
        return newState
    }
} 