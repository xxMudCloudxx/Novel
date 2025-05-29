package com.novel.utils

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableArray
import com.novel.MainApplication
import com.novel.utils.network.api.front.HomeService

object ReactNativeBridge {
    
    private const val TAG = "ReactNativeBridge"
    
    /**
     * 发送用户登录数据到RN
     */
    fun sendUserDataToRN(
        uid: String,
        token: String,
        nickname: String,
        photo: String,
        sex: String? = null
    ) {
        Log.d(TAG, "🚀 发送用户数据到RN: uid=$uid, nickname=$nickname")
        
        val application = MainApplication.getInstance()
        val reactContext = application?.reactNativeHost?.reactInstanceManager?.currentReactContext
        
        reactContext?.let { context ->
            val params = Arguments.createMap().apply {
                putString("uid", uid)
                putString("token", token)
                putString("nickname", nickname)
                putString("photo", photo)
                sex?.let { putString("sex", it) }
            }
            
            RCTDeviceEventEmitter.sendEvent(
                context,
                "onUserDataReceived",
                params
            )
            
            Log.d(TAG, "✅ 用户数据已发送到RN")
        } ?: run {
            Log.w(TAG, "❌ ReactContext为空，无法发送用户数据")
        }
    }
    
    /**
     * 发送推荐书籍数据到RN
     */
    fun sendRecommendBooksToRN(books: List<HomeService.HomeBook>) {
        Log.d(TAG, "📚 发送${books.size}本推荐书籍到RN")
        
        val application = MainApplication.getInstance()
        val reactContext = application?.reactNativeHost?.reactInstanceManager?.currentReactContext
        
        reactContext?.let { context ->
            val booksArray = Arguments.createArray()
            
            books.forEach { book ->
                val bookMap = Arguments.createMap().apply {
                    putInt("id", book.bookId.toInt())
                    putString("title", book.bookName)
                    putString("author", book.authorName)
                    putString("description", book.bookDesc)
                    putString("coverUrl", book.picUrl)
                    // 模拟一些额外数据
                    putInt("readCount", (Math.random() * 10000).toInt())
                    putDouble("rating", 4.0 + Math.random())
                }
                booksArray.pushMap(bookMap)
            }
            
            val params = Arguments.createMap().apply {
                putArray("books", booksArray)
            }
            
            RCTDeviceEventEmitter.sendEvent(
                context,
                "onRecommendBooksReceived",
                params
            )
            
            Log.d(TAG, "✅ ${books.size}本推荐书籍已发送到RN")
        } ?: run {
            Log.w(TAG, "❌ ReactContext为空，无法发送推荐书籍")
        }
    }
    
    /**
     * 发送测试用户数据到RN
     */
    fun sendTestUserDataToRN() {
        Log.d(TAG, "🧪 发送测试用户数据到RN")
        sendUserDataToRN(
            uid = "12345",
            token = "test-token-123456",
            nickname = "测试用户",
            photo = "https://example.com/avatar.jpg",
            sex = "男"
        )
    }
    
    /**
     * 发送测试推荐书籍数据到RN
     */
    fun sendTestRecommendBooksToRN() {
        Log.d(TAG, "🧪 发送测试推荐书籍到RN")
        
        val testBooks = listOf(
            HomeService.HomeBook(
                type = 3,
                bookId = 1001L,
                picUrl = "https://example.com/book1.jpg",
                bookName = "斗破苍穹",
                authorName = "天蚕土豆",
                bookDesc = "这里是斗气大陆，没有花俏的魔法，有的，仅仅是繁衍到巅峰的斗气！"
            ),
            HomeService.HomeBook(
                type = 3,
                bookId = 1002L,
                picUrl = "https://example.com/book2.jpg",
                bookName = "完美世界",
                authorName = "辰东",
                bookDesc = "一粒尘可填海，一根草斩尽日月星辰，弹指间天翻地覆。"
            ),
            HomeService.HomeBook(
                type = 3,
                bookId = 1003L,
                picUrl = "https://example.com/book3.jpg",
                bookName = "遮天",
                authorName = "辰东",
                bookDesc = "冰冷与黑暗并存的宇宙深处，九具庞大的龙尸拉着一口青铜古棺，亘古长存。"
            )
        )
        
        sendRecommendBooksToRN(testBooks)
    }
} 