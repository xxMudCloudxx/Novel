package com.novel.page.search.usecase

import com.novel.page.search.repository.SearchRepository
import javax.inject.Inject

/**
 * 获取搜索历史UseCase
 */
class GetSearchHistoryUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    /**
     * 获取搜索历史列表
     */
    suspend operator fun invoke(): List<String> {
        return searchRepository.getSearchHistory()
    }
} 