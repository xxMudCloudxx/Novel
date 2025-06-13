package com.novel.page.search.usecase

import com.novel.page.search.viewmodel.CategoryFilter
import com.novel.utils.network.api.front.BookService
import javax.inject.Inject

/**
 * 获取分类筛选器用例
 */
class GetCategoryFiltersUseCase @Inject constructor(
    private val bookService: BookService
) {
    suspend fun execute(): Result<List<CategoryFilter>> {
        return try {
            val response = bookService.getBookCategoriesBlocking(0)
            if (response.code == "0" && response.data != null) {
                val categories = mutableListOf<CategoryFilter>()
                
                // 添加"所有"选项
                categories.add(CategoryFilter(id = -1, name = "所有"))
                
                // 添加从API获取的分类
                response.data.forEach { category ->
                    categories.add(
                        CategoryFilter(
                            id = category.id.toInt(),
                            name = category.name
                        )
                    )
                }
                
                Result.success(categories)
            } else {
                // 如果API失败，返回默认分类
                Result.success(getDefaultCategories())
            }
        } catch (e: Exception) {
            // 异常时返回默认分类
            Result.success(getDefaultCategories())
        }
    }
    
    private fun getDefaultCategories(): List<CategoryFilter> {
        return listOf(
            CategoryFilter(id = -1, name = "所有"),
            CategoryFilter(id = 1, name = "武侠玄幻"),
            CategoryFilter(id = 2, name = "都市言情"),
            CategoryFilter(id = 3, name = "历史军事"),
            CategoryFilter(id = 4, name = "游戏竞技"),
            CategoryFilter(id = 5, name = "科幻灵异"),
            CategoryFilter(id = 6, name = "网游科技"),
            CategoryFilter(id = 7, name = "其他")
        )
    }
} 