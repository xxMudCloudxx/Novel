package com.novel.utils.network.cache

import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.NewsService
import com.novel.utils.network.api.front.user.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 缓存键生成器
 */
object CacheKeys {
    fun bookInfo(bookId: Long) = "book_info_$bookId"
    fun bookChapters(bookId: Long) = "book_chapters_$bookId"
    fun bookContent(chapterId: Long) = "book_content_$chapterId"
    fun visitRankBooks() = "visit_rank_books"
    fun updateRankBooks() = "update_rank_books"
    fun newestRankBooks() = "newest_rank_books"
    fun bookCategories(workDirection: Int) = "book_categories_$workDirection"
    fun searchBooks(params: String) = "search_books_$params"
    fun homeBooks() = "home_books"
    fun friendLinks() = "friend_links"
    fun latestNews() = "latest_news"
    fun newsInfo(newsId: Long) = "news_info_$newsId"
    fun userInfo() = "user_info"
    fun userComments(pageNum: Int, pageSize: Int) = "user_comments_${pageNum}_$pageSize"
}

/**
 * 常用缓存配置
 */
object CacheConfigs {
    val SHORT_CACHE = CacheConfig(maxAge = TimeUnit.MINUTES.toMillis(5)) // 5分钟
    val MEDIUM_CACHE = CacheConfig(maxAge = TimeUnit.MINUTES.toMillis(30)) // 30分钟
    val LONG_CACHE = CacheConfig(maxAge = TimeUnit.HOURS.toMillis(2)) // 2小时
    val EXTRA_LONG_CACHE = CacheConfig(maxAge = TimeUnit.HOURS.toMillis(24)) // 24小时
}

/**
 * BookService 缓存扩展
 */
suspend fun BookService.getBookByIdCached(
    bookId: Long,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookInfoResponse) -> Unit)? = null
): CacheResult<BookService.BookInfoResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.bookInfo(bookId),
        config = CacheConfigs.MEDIUM_CACHE,
        networkCall = { getBookByIdBlocking(bookId) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getBookChaptersCached(
    bookId: Long,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookChapterResponse) -> Unit)? = null
): CacheResult<BookService.BookChapterResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.bookChapters(bookId),
        config = CacheConfigs.MEDIUM_CACHE,
        networkCall = { getBookChaptersBlocking(bookId) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getBookContentCached(
    chapterId: Long,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookContentResponse) -> Unit)? = null
): CacheResult<BookService.BookContentResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.bookContent(chapterId),
        config = CacheConfigs.LONG_CACHE, // 章节内容缓存时间长一些
        networkCall = { getBookContentBlocking(chapterId) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getVisitRankBooksCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookRankResponse) -> Unit)? = null
): CacheResult<BookService.BookRankResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.visitRankBooks(),
        config = CacheConfigs.SHORT_CACHE, // 排行榜更新频繁，缓存时间短
        networkCall = { getVisitRankBooksBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getUpdateRankBooksCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookRankResponse) -> Unit)? = null
): CacheResult<BookService.BookRankResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.updateRankBooks(),
        config = CacheConfigs.SHORT_CACHE,
        networkCall = { getUpdateRankBooksBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getNewestRankBooksCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookRankResponse) -> Unit)? = null
): CacheResult<BookService.BookRankResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.newestRankBooks(),
        config = CacheConfigs.SHORT_CACHE,
        networkCall = { getNewestRankBooksBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun BookService.getBookCategoriesCached(
    workDirection: Int,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((BookService.BookCategoryResponse) -> Unit)? = null
): CacheResult<BookService.BookCategoryResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.bookCategories(workDirection),
        config = CacheConfigs.EXTRA_LONG_CACHE, // 分类信息基本不变，缓存时间长
        networkCall = { getBookCategoriesBlocking(workDirection) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

/**
 * SearchService 缓存扩展
 */
suspend fun SearchService.searchBooksCached(
    keyword: String? = null,
    workDirection: Int? = null,
    categoryId: Int? = null,
    isVip: Int? = null,
    bookStatus: Int? = null,
    wordCountMin: Int? = null,
    wordCountMax: Int? = null,
    updateTimeMin: String? = null,
    sort: String? = null,
    pageNum: Int = 1,
    pageSize: Int = 20,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((SearchService.BookSearchResponse) -> Unit)? = null
): CacheResult<SearchService.BookSearchResponse> {
    // 生成搜索参数的哈希作为缓存键的一部分
    val paramsHash = listOf(
        keyword, workDirection, categoryId, isVip, bookStatus,
        wordCountMin, wordCountMax, updateTimeMin, sort, pageNum, pageSize
    ).joinToString("_").hashCode().toString()
    
    return cacheManager.getCachedData(
        key = CacheKeys.searchBooks(paramsHash),
        config = CacheConfigs.SHORT_CACHE, // 搜索结果缓存时间短
        networkCall = {
            searchBooksBlocking(
                keyword = keyword,
                workDirection = workDirection,
                categoryId = categoryId,
                isVip = isVip,
                bookStatus = bookStatus,
                wordCountMin = wordCountMin,
                wordCountMax = wordCountMax,
                updateTimeMin = updateTimeMin,
                sort = sort,
                pageNum = pageNum,
                pageSize = pageSize
            )
        },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

/**
 * HomeService 缓存扩展
 */
suspend fun HomeService.getHomeBookseCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((HomeService.HomeBooksResponse) -> Unit)? = null
): CacheResult<HomeService.HomeBooksResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.homeBooks(),
        config = CacheConfigs.MEDIUM_CACHE,
        networkCall = { getHomeBooksBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun HomeService.getFriendLinksCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((HomeService.FriendLinksResponse) -> Unit)? = null
): CacheResult<HomeService.FriendLinksResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.friendLinks(),
        config = CacheConfigs.EXTRA_LONG_CACHE, // 友情链接变动较少
        networkCall = { getFriendLinksBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

/**
 * NewsService 缓存扩展
 */
suspend fun NewsService.getLatestNewsCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((NewsService.NewsListResponse) -> Unit)? = null
): CacheResult<NewsService.NewsListResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.latestNews(),
        config = CacheConfigs.SHORT_CACHE, // 新闻更新频繁
        networkCall = { getLatestNewsBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun NewsService.getNewsByIdCached(
    newsId: Long,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((NewsService.NewsInfoResponse) -> Unit)? = null
): CacheResult<NewsService.NewsInfoResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.newsInfo(newsId),
        config = CacheConfigs.LONG_CACHE,
        networkCall = { getNewsByIdBlocking(newsId) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

/**
 * UserService 缓存扩展
 */
suspend fun UserService.getUserInfoCached(
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((UserService.UserInfoResponse?) -> Unit)? = null
): CacheResult<UserService.UserInfoResponse?> {
    return cacheManager.getCachedData(
        key = CacheKeys.userInfo(),
        config = CacheConfigs.MEDIUM_CACHE,
        networkCall = { getUserInfoBlocking() },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

suspend fun UserService.getUserCommentsCached(
    pageRequest: UserService.PageRequest,
    cacheManager: NetworkCacheManager,
    strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
    onCacheUpdate: ((UserService.UserCommentsResponse) -> Unit)? = null
): CacheResult<UserService.UserCommentsResponse> {
    return cacheManager.getCachedData(
        key = CacheKeys.userComments(pageRequest.pageNum, pageRequest.pageSize),
        config = CacheConfigs.SHORT_CACHE,
        networkCall = { getUserCommentsBlocking(pageRequest) },
        strategy = strategy,
        onCacheUpdate = onCacheUpdate
    )
}

/**
 * 缓存结果处理扩展
 */
inline fun <T> CacheResult<T>.onSuccess(action: (data: T, fromCache: Boolean) -> Unit): CacheResult<T> {
    if (this is CacheResult.Success) {
        action(data, fromCache)
    }
    return this
}

inline fun <T> CacheResult<T>.onError(action: (error: Throwable, cachedData: T?) -> Unit): CacheResult<T> {
    if (this is CacheResult.Error) {
        action(error, cachedData)
    }
    return this
}

inline fun <T> CacheResult<T>.fold(
    onSuccess: (data: T, fromCache: Boolean) -> Unit,
    onError: (error: Throwable, cachedData: T?) -> Unit
) {
    when (this) {
        is CacheResult.Success -> onSuccess(data, fromCache)
        is CacheResult.Error -> onError(error, cachedData)
    }
}

/**
 * 异步缓存更新帮助函数
 */
fun <T> CoroutineScope.launchCacheUpdate(
    cacheManager: NetworkCacheManager,
    key: String,
    config: CacheConfig,
    networkCall: suspend () -> T,
    onUpdate: ((T) -> Unit)? = null
) {
    launch(Dispatchers.IO) {
        try {
            val data = networkCall()
            // 手动保存到缓存（这里需要添加对应的方法到NetworkCacheManager）
            onUpdate?.invoke(data)
        } catch (e: Exception) {
            // 更新失败，静默处理
        }
    }
} 