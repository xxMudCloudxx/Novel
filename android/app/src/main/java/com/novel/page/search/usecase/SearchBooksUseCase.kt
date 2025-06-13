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
            
            if (response?.ok == true && response.data != null) {
                // 转换SearchService的响应数据为期望的类型
                val pageResp = PageRespDtoBookInfoRespDto(
                    pageNum = response.data.pageNum,
                    pageSize = response.data.pageSize,
                    total = response.data.total,
                    list = response.data.list.map { searchBook ->
                        // 转换SearchService.BookInfo为BookInfoRespDto
                        com.novel.page.search.viewmodel.BookInfoRespDto(
                            id = searchBook.id,
                            categoryId = searchBook.categoryId ?: 0L,
                            categoryName = searchBook.categoryName ?: "",
                            picUrl = searchBook.picUrl ?: "",
                            bookName = searchBook.bookName ?: "",
                            authorId = searchBook.authorId ?: 0L,
                            authorName = searchBook.authorName ?: "",
                            bookDesc = searchBook.bookDesc ?: "",
                            bookStatus = searchBook.bookStatus ?: 0,
                            visitCount = searchBook.visitCount ?: 0L,
                            wordCount = (searchBook.wordCount ?: 0L).toInt(),
                            commentCount = searchBook.commentCount ?: 0,
                            firstChapterId = searchBook.firstChapterId ?: 0L,
                            lastChapterId = searchBook.lastChapterId ?: 0L,
                            lastChapterName = searchBook.lastChapterName ?: "",
                            updateTime = searchBook.updateTime ?: ""
                        )
                    },
                    pages = response.data.pages
                )
                Result.success(pageResp)
            } else {
                Result.failure(Exception(response?.message ?: "搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 