package com.novel.page.home.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 首页书籍实体
 */
@Entity(tableName = "home_books")
data class HomeBookEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val author: String,
    val coverUrl: String,
    val description: String,
    val category: String,
    val categoryId: Long = 0,
    val rating: Double = 0.0,
    val readCount: Int = 0,
    val wordCount: Long = 0, // 字数
    val commentCount: Int = 0, // 评论数
    val isCompleted: Boolean,
    val isVip: Boolean,
    val updateTime: Long,
    val lastChapterName: String? = null,
    val lastChapterUpdateTime: String? = null,
    val type: String, // 推荐类型：carousel, hot, new, vip等
    val sortOrder: Int = 0
)

/**
 * 首页轮播图实体
 */
@Entity(tableName = "home_banners")
data class HomeBannerEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val imageUrl: String,
    val linkUrl: String?,
    val sortOrder: Int,
    val isActive: Boolean = true
)

/**
 * 首页分类实体
 */
@Entity(tableName = "home_categories")
data class HomeCategoryEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val iconUrl: String?,
    val sortOrder: Int,
    val bookCount: Int = 0
)

/**
 * 网络响应数据类 - 书籍详情
 */
data class BookDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("picUrl") val picUrl: String,
    @SerializedName("bookName") val bookName: String,
    @SerializedName("authorId") val authorId: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("bookDesc") val bookDesc: String,
    @SerializedName("bookStatus") val bookStatus: String, // "0":连载 "1":完结
    @SerializedName("visitCount") val visitCount: String,
    @SerializedName("wordCount") val wordCount: String,
    @SerializedName("commentCount") val commentCount: String,
    @SerializedName("firstChapterId") val firstChapterId: String,
    @SerializedName("lastChapterId") val lastChapterId: String,
    @SerializedName("lastChapterName") val lastChapterName: String?,
    @SerializedName("updateTime") val updateTime: String?
)

/**
 * 网络响应数据类 - 排行榜书籍
 */
data class BookRankDto(
    @SerializedName("id") val id: String,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("picUrl") val picUrl: String,
    @SerializedName("bookName") val bookName: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("bookDesc") val bookDesc: String,
    @SerializedName("wordCount") val wordCount: String,
    @SerializedName("lastChapterName") val lastChapterName: String,
    @SerializedName("lastChapterUpdateTime") val lastChapterUpdateTime: String
)

/**
 * 网络响应数据类 - 首页推荐书籍
 */
data class HomeBookDto(
    @SerializedName("type") val type: Int, // 0-轮播图 1-顶部栏 2-本周强推 3-热门推荐 4-精品推荐
    @SerializedName("bookId") val bookId: Long,
    @SerializedName("picUrl") val picUrl: String,
    @SerializedName("bookName") val bookName: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("bookDesc") val bookDesc: String
)

data class HomeBannerDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("picUrl") val imageUrl: String,
    @SerializedName("linkUrl") val linkUrl: String?,
    @SerializedName("sort") val sortOrder: Int
)

data class HomeCategoryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("iconUrl") val iconUrl: String?,
    @SerializedName("sort") val sortOrder: Int,
    @SerializedName("bookCount") val bookCount: Int
)

/**
 * DTO 转 Entity 扩展函数
 */
fun HomeBookDto.toEntity(entityType: String): HomeBookEntity {
    return HomeBookEntity(
        id = bookId,
        title = bookName,
        author = authorName,
        coverUrl = picUrl,
        description = bookDesc,
        category = "",
        categoryId = 0,
        rating = 0.0,
        readCount = 0,
        wordCount = 0,
        commentCount = 0,
        isCompleted = false,
        isVip = false,
        updateTime = System.currentTimeMillis(),
        lastChapterName = null,
        lastChapterUpdateTime = null,
        type = entityType
    )
}

fun HomeBannerDto.toEntity(): HomeBannerEntity {
    return HomeBannerEntity(
        id = id,
        title = title,
        imageUrl = imageUrl,
        linkUrl = linkUrl,
        sortOrder = sortOrder
    )
}

fun HomeCategoryDto.toEntity(): HomeCategoryEntity {
    return HomeCategoryEntity(
        id = id,
        name = name,
        iconUrl = iconUrl,
        sortOrder = sortOrder,
        bookCount = bookCount
    )
}

/**
 * BookDetailDto 转 Entity 扩展函数
 */
fun BookDetailDto.toEntity(type: String): HomeBookEntity {
    return HomeBookEntity(
        id = id.toLongOrNull() ?: 0L,
        title = bookName,
        author = authorName,
        coverUrl = picUrl,
        description = bookDesc,
        category = categoryName,
        categoryId = categoryId.toLongOrNull() ?: 0L,
        rating = 0.0,
        readCount = visitCount.toIntOrNull() ?: 0,
        wordCount = wordCount.toLongOrNull() ?: 0L,
        commentCount = commentCount.toIntOrNull() ?: 0,
        isCompleted = bookStatus == "1",
        isVip = false,
        updateTime = System.currentTimeMillis(),
        lastChapterName = lastChapterName,
        lastChapterUpdateTime = updateTime,
        type = type
    )
}

/**
 * BookRankDto 转 Entity 扩展函数
 */
fun BookRankDto.toEntity(type: String): HomeBookEntity {
    return HomeBookEntity(
        id = id.toLongOrNull() ?: 0L,
        title = bookName,
        author = authorName,
        coverUrl = picUrl,
        description = bookDesc,
        category = categoryName,
        categoryId = categoryId.toLongOrNull() ?: 0L,
        rating = 0.0,
        readCount = 0,
        wordCount = wordCount.toLongOrNull() ?: 0L,
        commentCount = 0,
        isCompleted = false,
        isVip = false,
        updateTime = System.currentTimeMillis(),
        lastChapterName = lastChapterName,
        lastChapterUpdateTime = lastChapterUpdateTime,
        type = type
    )
} 