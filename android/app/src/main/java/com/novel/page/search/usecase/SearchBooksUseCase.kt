package com.novel.page.search.usecase

import com.novel.page.search.repository.SearchRepository
import com.novel.page.search.repository.PageRespDtoBookInfoRespDto
import com.novel.page.search.viewmodel.FilterState
import javax.inject.Inject

class SearchBooksUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    suspend fun execute(
        keyword: String,
        categoryId: Int? = null,
        filters: FilterState = FilterState(),
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<PageRespDtoBookInfoRespDto> {
        return try {
            val response = searchRepository.searchBooks(
                keyword = keyword,
                workDirection = null, // 可以根据需要设置
                categoryId = categoryId,
                isVip = filters.isVip.value,
                bookStatus = filters.updateStatus.value,
                wordCountMin = filters.wordCountRange.min,
                wordCountMax = filters.wordCountRange.max,
                updateTimeMin = null, // 可以根据需要设置
                sort = filters.sortBy.value,
                pageNum = pageNum,
                pageSize = pageSize
            )
            
            if (response.ok == true) {
                Result.success(response.data ?: PageRespDtoBookInfoRespDto())
            } else {
                Result.failure(Exception(response.message ?: "搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 