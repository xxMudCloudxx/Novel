package com.novel.page.search.usecase

import androidx.compose.runtime.Stable
import com.novel.utils.TimberLogger
import com.novel.page.search.viewmodel.CategoryFilter
import com.novel.utils.network.api.front.BookService
import javax.inject.Inject

/**
 * 获取分类筛选器业务用例
 * 
 * 功能职责：
 * - 获取书籍分类筛选选项
 * - API失败时提供默认分类
 * - 数据模型转换适配
 * 
 * 兜底策略：
 * - API成功：返回服务端分类 + "所有"选项
 * - API失败：返回预设默认分类列表
 * - 异常处理：确保界面正常显示
 * 
 * 技术特点：
 * - Result包装错误处理
 * - 优雅降级机制
 * - 数据一致性保证
 */
@Stable
class GetCategoryFiltersUseCase @Inject constructor(
    @Stable private val bookService: BookService
) {
    
    companion object {
        private const val TAG = "GetCategoryFiltersUseCase"
    }
    
    /**
     * 执行分类筛选器获取
     * 
     * 处理流程：
     * 1. 调用BookService获取分类数据
     * 2. 成功时添加"所有"选项并转换数据
     * 3. 失败时返回预设默认分类
     * 4. 异常时也返回默认分类保证稳定性
     * 
     * @return Result<List<CategoryFilter>> 分类筛选器列表
     */
    suspend fun execute(): Result<List<CategoryFilter>> {
        return try {
            TimberLogger.d(TAG, "开始获取书籍分类...")
            
            val response = bookService.getBookCategoriesBlocking(0)
            if (response.code == "0" && response.data != null) {
                TimberLogger.d(TAG, "API获取分类成功，共${response.data.size}个分类")
                
                val categories = mutableListOf<CategoryFilter>()
                
                // 添加"所有"选项作为默认选择
                categories.add(CategoryFilter(id = -1, name = "所有"))
                
                // 添加从API获取的分类数据
                response.data.forEach { category ->
                    categories.add(
                        CategoryFilter(
                            id = category.id.toInt(),
                            name = category.name
                        )
                    )
                }
                
                TimberLogger.d(TAG, "分类数据处理完成，共${categories.size}个选项")
                Result.success(categories)
            } else {
                TimberLogger.w(TAG, "API返回失败，使用默认分类: ${response.code}")
                // 如果API失败，返回默认分类
                Result.success(getDefaultCategories())
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "获取分类异常，使用默认分类", e)
            // 异常时返回默认分类
            Result.success(getDefaultCategories())
        }
    }
    
    /**
     * 获取默认分类列表
     * 当API不可用时的兜底数据
     * 
     * @return 预设的分类筛选器列表
     */
    private fun getDefaultCategories(): List<CategoryFilter> {
        TimberLogger.d(TAG, "使用默认分类列表")
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