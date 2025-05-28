package com.novel.utils.network.api.front

import android.util.Log
import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_FRONT
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookService @Inject constructor() {
    
    // region 数据结构
    data class BookInfoResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: BookInfo?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookInfo(
        @SerializedName("id") val id: Long,
        @SerializedName("categoryId") val categoryId: Long,
        @SerializedName("categoryName") val categoryName: String,
        @SerializedName("picUrl") val picUrl: String,
        @SerializedName("bookName") val bookName: String,
        @SerializedName("authorId") val authorId: Long,
        @SerializedName("authorName") val authorName: String,
        @SerializedName("bookDesc") val bookDesc: String,
        @SerializedName("bookStatus") val bookStatus: Int,
        @SerializedName("visitCount") val visitCount: Long,
        @SerializedName("wordCount") val wordCount: Int,
        @SerializedName("commentCount") val commentCount: Int,
        @SerializedName("firstChapterId") val firstChapterId: Long,
        @SerializedName("lastChapterId") val lastChapterId: Long,
        @SerializedName("lastChapterName") val lastChapterName: String,
        @SerializedName("updateTime") val updateTime: String
    )

    data class BookListResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: List<BookInfo>?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookRankResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: List<BookRank>?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookRank(
        @SerializedName("id") val id: Long,
        @SerializedName("categoryId") val categoryId: Long,
        @SerializedName("categoryName") val categoryName: String,
        @SerializedName("picUrl") val picUrl: String,
        @SerializedName("bookName") val bookName: String,
        @SerializedName("authorName") val authorName: String,
        @SerializedName("bookDesc") val bookDesc: String,
        @SerializedName("wordCount") val wordCount: Int,
        @SerializedName("lastChapterName") val lastChapterName: String,
        @SerializedName("lastChapterUpdateTime") val lastChapterUpdateTime: String
    )

    data class BookChapterResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: List<BookChapter>?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookChapter(
        @SerializedName("id") val id: Long,
        @SerializedName("bookId") val bookId: Long,
        @SerializedName("chapterNum") val chapterNum: Int,
        @SerializedName("chapterName") val chapterName: String,
        @SerializedName("chapterWordCount") val chapterWordCount: Int,
        @SerializedName("chapterUpdateTime") val chapterUpdateTime: String,
        @SerializedName("isVip") val isVip: Int
    )

    data class BookContentResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: BookContentAbout?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookContentAbout(
        @SerializedName("bookInfo") val bookInfo: BookInfo,
        @SerializedName("chapterInfo") val chapterInfo: BookChapter,
        @SerializedName("bookContent") val bookContent: String
    )

    data class BookCommentResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: BookComment?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookComment(
        @SerializedName("commentTotal") val commentTotal: Long,
        @SerializedName("comments") val comments: List<CommentInfo>
    )

    data class CommentInfo(
        @SerializedName("id") val id: Long,
        @SerializedName("commentContent") val commentContent: String,
        @SerializedName("commentUser") val commentUser: String,
        @SerializedName("commentUserId") val commentUserId: Long,
        @SerializedName("commentUserPhoto") val commentUserPhoto: String,
        @SerializedName("commentTime") val commentTime: String
    )

    data class BookCategoryResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: List<BookCategory>?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookCategory(
        @SerializedName("id") val id: Long,
        @SerializedName("name") val name: String
    )

    data class ChapterIdResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: Long?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookChapterAboutResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: BookChapterAbout?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class BookChapterAbout(
        @SerializedName("chapterInfo") val chapterInfo: BookChapter,
        @SerializedName("chapterTotal") val chapterTotal: Long,
        @SerializedName("contentSummary") val contentSummary: String
    )

    data class BaseResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: Any?,
        @SerializedName("ok") val ok: Boolean?
    )
    // endregion

    // region 网络请求方法
    
    /**
     * 小说信息查询接口
     */
    fun getBookById(
        bookId: Long,
        callback: (BookInfoResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getBookById()，参数：$bookId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/$bookId",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookInfoResponse::class.java, callback)
        }
    }

    /**
     * 小说点击榜查询接口
     */
    fun getVisitRankBooks(
        callback: (BookRankResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getVisitRankBooks()")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/visit_rank",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookRankResponse::class.java, callback)
        }
    }

    /**
     * 小说更新榜查询接口
     */
    fun getUpdateRankBooks(
        callback: (BookRankResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getUpdateRankBooks()")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/update_rank",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookRankResponse::class.java, callback)
        }
    }

    /**
     * 小说新书榜查询接口
     */
    fun getNewestRankBooks(
        callback: (BookRankResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getNewestRankBooks()")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/newest_rank",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookRankResponse::class.java, callback)
        }
    }

    /**
     * 小说推荐列表查询接口
     */
    fun getRecommendBooks(
        bookId: Long,
        callback: (BookListResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getRecommendBooks()，参数：$bookId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/rec_list",
            params = mapOf("bookId" to bookId.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookListResponse::class.java, callback)
        }
    }

    /**
     * 小说章节列表查询接口
     */
    fun getBookChapters(
        bookId: Long,
        callback: (BookChapterResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getBookChapters()，参数：$bookId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/chapter/list",
            params = mapOf("bookId" to bookId.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookChapterResponse::class.java, callback)
        }
    }

    /**
     * 小说内容相关信息查询接口
     */
    fun getBookContent(
        chapterId: Long,
        callback: (BookContentResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getBookContent()，参数：$chapterId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/content/$chapterId",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookContentResponse::class.java, callback)
        }
    }

    /**
     * 小说最新评论查询接口
     */
    fun getNewestComments(
        bookId: Long,
        callback: (BookCommentResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getNewestComments()，参数：$bookId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/comment/newest_list",
            params = mapOf("bookId" to bookId.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookCommentResponse::class.java, callback)
        }
    }

    /**
     * 小说分类列表查询接口
     */
    fun getBookCategories(
        workDirection: Int,
        callback: (BookCategoryResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getBookCategories()，参数：$workDirection")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/category/list",
            params = mapOf("workDirection" to workDirection.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookCategoryResponse::class.java, callback)
        }
    }

    /**
     * 获取上一章节ID接口
     */
    fun getPreChapterId(
        chapterId: Long,
        callback: (ChapterIdResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getPreChapterId()，参数：$chapterId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/pre_chapter_id/$chapterId",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, ChapterIdResponse::class.java, callback)
        }
    }

    /**
     * 获取下一章节ID接口
     */
    fun getNextChapterId(
        chapterId: Long,
        callback: (ChapterIdResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getNextChapterId()，参数：$chapterId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/next_chapter_id/$chapterId",
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, ChapterIdResponse::class.java, callback)
        }
    }

    /**
     * 小说最新章节相关信息查询接口
     */
    fun getLastChapterAbout(
        bookId: Long,
        callback: (BookChapterAboutResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 getLastChapterAbout()，参数：$bookId")
        
        ApiService.get(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/last_chapter/about",
            params = mapOf("bookId" to bookId.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BookChapterAboutResponse::class.java, callback)
        }
    }

    /**
     * 增加小说点击量接口
     */
    fun addVisitCount(
        bookId: Long,
        callback: (BaseResponse?, Throwable?) -> Unit
    ) {
        Log.d("BookService", "开始 addVisitCount()，参数：$bookId")
        
        ApiService.post(
            baseUrl = BASE_URL_FRONT,
            endpoint = "book/visit",
            params = mapOf("bookId" to bookId.toString()),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, BaseResponse::class.java, callback)
        }
    }

    // endregion

    // region 协程版本
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBookByIdBlocking(bookId: Long): BookInfoResponse {
        return suspendCancellableCoroutine { cont ->
            getBookById(bookId) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBookContentBlocking(chapterId: Long): BookContentResponse {
        return suspendCancellableCoroutine { cont ->
            getBookContent(chapterId) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBookChaptersBlocking(bookId: Long): BookChapterResponse {
        return suspendCancellableCoroutine { cont ->
            getBookChapters(bookId) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }
    // endregion

    // region 响应处理
    private fun <T> handleResponse(
        response: String?,
        error: Throwable?,
        clazz: Class<T>,
        callback: (T?, Throwable?) -> Unit
    ) {
        when {
            error != null -> {
                callback(null, error)
            }
            response != null -> {
                try {
                    callback(Gson().fromJson(response, clazz), null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }
            else -> {
                callback(null, Exception("Response is null"))
            }
        }
    }
    // endregion
} 