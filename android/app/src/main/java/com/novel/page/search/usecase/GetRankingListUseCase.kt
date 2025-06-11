package com.novel.page.search.usecase

import com.novel.page.search.SearchRankingItem
import com.novel.page.search.repository.RankingData
import com.novel.page.search.service.SearchService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 获取榜单列表UseCase
 */
class GetRankingListUseCase @Inject constructor(
    private val searchService: SearchService
) {
    
    /**
     * 获取所有榜单数据
     * @return 榜单数据对象
     */
    suspend operator fun invoke(): RankingData {
        return coroutineScope {
            // 并行获取三个榜单数据
            val novelRankingDeferred = async { searchService.getHotNovelRanking() }
            val dramaRankingDeferred = async { searchService.getHotDramaRanking() }
            val newBookRankingDeferred = async { searchService.getNewBookRanking() }
            
            RankingData(
                novelRanking = novelRankingDeferred.await(),
                dramaRanking = dramaRankingDeferred.await(),
                newBookRanking = newBookRankingDeferred.await()
            )
        }
    }
} 