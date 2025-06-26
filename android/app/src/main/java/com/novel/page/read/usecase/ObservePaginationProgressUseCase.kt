package com.novel.page.read.usecase

import com.novel.page.read.repository.ProgressiveCalculationState
import com.novel.page.read.service.PaginationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePaginationProgressUseCase @Inject constructor(
    private val paginationService: PaginationService
) {
    fun execute(): Flow<ProgressiveCalculationState> {
        return paginationService.paginationState
    }
} 