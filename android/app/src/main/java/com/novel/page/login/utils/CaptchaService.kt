package com.novel.page.login.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import com.novel.utils.network.api.front.resource.ResourceService
import com.novel.utils.security.SecurityConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 验证码服务 - 专门处理验证码获取、缓存和清理
 * ⚠ 安全检查: 验证码文件需要定期清理，防止缓存泄露
 */
@Singleton
class CaptchaService @Inject constructor(
    private val resourceService: ResourceService,
    private val securityConfig: SecurityConfig,
    @ApplicationContext private val context: Context
) {
    
    // 验证码状态管理
    private val _captchaState = MutableStateFlow(CaptchaState())
    val captchaState: StateFlow<CaptchaState> = _captchaState.asStateFlow()
    
    /**
     * 加载验证码
     * @return 是否加载成功
     */
    suspend fun loadCaptcha(): Boolean = withContext(Dispatchers.IO) {
        Log.d("CaptchaService", "开始加载验证码")
        
        // 设置加载状态
        _captchaState.value = _captchaState.value.copy(
            isLoading = true,
            error = null
        )
        
        runCatching {
            val response = resourceService.getImageVerifyCodeBlocking()
            
            response.data?.let { data ->
                // 验证数据安全性
                if (!isValidCaptchaData(data.imgBase64, data.sessionId)) {
                    throw SecurityException("验证码数据不安全")
                }
                
                // 解码Base64图片数据
                val imageBytes = Base64.decode(data.imgBase64, Base64.DEFAULT)
                
                // 验证文件大小
                if (!isFileSizeValid(imageBytes)) {
                    throw SecurityException("验证码文件大小超限")
                }
                
                // 创建临时文件
                val tempFile = createTempFile(data.sessionId, imageBytes)
                
                // 更新状态
                _captchaState.value = CaptchaState(
                    imagePath = tempFile.absolutePath,
                    sessionId = data.sessionId,
                    isLoading = false,
                    error = null
                )
                
                Log.d("CaptchaService", "验证码加载成功: sessionId=${securityConfig.sanitizeForLog(data.sessionId)}")
                true
            } ?: run {
                Log.e("CaptchaService", "验证码数据为空")
                _captchaState.value = _captchaState.value.copy(
                    isLoading = false,
                    error = "验证码数据为空"
                )
                false
            }
        }.getOrElse { exception ->
            Log.e("CaptchaService", "验证码加载失败", exception)
            _captchaState.value = _captchaState.value.copy(
                isLoading = false,
                error = exception.localizedMessage ?: "验证码加载失败"
            )
            false
        }
    }
    
    /**
     * 刷新验证码
     */
    suspend fun refreshCaptcha(): Boolean {
        // 先清理旧的验证码文件
        clearCurrentCaptcha()
        // 重新加载
        return loadCaptcha()
    }
    
    /**
     * 清理当前验证码
     */
    fun clearCurrentCaptcha() {
        val currentState = _captchaState.value
        if (currentState.imagePath.isNotEmpty()) {
            File(currentState.imagePath).delete()
        }
        
        _captchaState.value = CaptchaState()
    }
    
    /**
     * 清理所有临时验证码文件
     * ⚠ 安全检查: 定期清理防止缓存文件泄露
     */
    suspend fun clearAllCaptchaFiles() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val deletedCount = cacheDir.listFiles()
                ?.filter { securityConfig.isCaptchaFile(it.name) }
                ?.count { file ->
                    if (file.delete()) {
                        Log.d("CaptchaService", "已删除验证码文件: ${file.name}")
                        true
                    } else {
                        Log.w("CaptchaService", "删除验证码文件失败: ${file.name}")
                        false
                    }
                } ?: 0
                
            Log.d("CaptchaService", "已清理 $deletedCount 个验证码文件")
        } catch (e: Exception) {
            Log.e("CaptchaService", "清理验证码文件失败", e)
        }
    }
    
    /**
     * 获取当前验证码信息
     */
    fun getCurrentCaptchaInfo(): CaptchaInfo? {
        val state = _captchaState.value
        return if (state.sessionId.isNotEmpty() && state.imagePath.isNotEmpty()) {
            CaptchaInfo(
                sessionId = state.sessionId,
                imagePath = state.imagePath
            )
        } else null
    }
    
    /**
     * 创建临时验证码文件
     * @param sessionId 会话ID
     * @param imageBytes 图片字节数组
     * @return 临时文件
     */
    private suspend fun createTempFile(sessionId: String, imageBytes: ByteArray): File = 
        withContext(Dispatchers.IO) {
            val fileName = securityConfig.generateCaptchaFileName(sessionId)
            val file = File(context.cacheDir, fileName)
            file.writeBytes(imageBytes)
            file
        }
    
    /**
     * 验证验证码数据的安全性
     * ⚠ 安全检查: 验证Base64数据和会话ID的有效性
     */
    private fun isValidCaptchaData(base64Data: String, sessionId: String): Boolean {
        return try {
            // 检查Base64格式
            Base64.decode(base64Data, Base64.DEFAULT)
            
            // 检查会话ID格式
            sessionId.isNotBlank() && 
            sessionId.length <= 64 && // 限制会话ID长度
            sessionId.matches(Regex("^[a-zA-Z0-9\\-_]+$")) // 只允许安全字符
        } catch (e: Exception) {
            Log.w("CaptchaService", "验证码数据验证失败", e)
            false
        }
    }
    
    /**
     * 验证文件大小是否合法
     * ⚠ 安全检查: 防止恶意大文件攻击
     */
    private fun isFileSizeValid(fileBytes: ByteArray): Boolean {
        val fileSizeMB = fileBytes.size / (1024 * 1024)
        return fileSizeMB <= SecurityConfig.MAX_CACHE_FILE_SIZE_MB
    }
    
    /**
     * 定期清理过期的验证码文件
     */
    suspend fun cleanupExpiredFiles() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val expiryTime = System.currentTimeMillis() - 
                            (SecurityConfig.CACHE_CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000)
            
            val cleanedCount = cacheDir.listFiles()
                ?.filter { securityConfig.isCaptchaFile(it.name) && it.lastModified() < expiryTime }
                ?.count { it.delete() } ?: 0
                
            if (cleanedCount > 0) {
                Log.d("CaptchaService", "清理了 $cleanedCount 个过期验证码文件")
            } else {

            }
        } catch (e: Exception) {
            Log.e("CaptchaService", "清理过期文件失败", e)
        }
    }
}

/**
 * 验证码状态
 */
data class CaptchaState(
    val imagePath: String = "",
    val sessionId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 是否有有效的验证码
     */
    val hasValidCaptcha: Boolean
        get() = imagePath.isNotEmpty() && sessionId.isNotEmpty() && error == null
}

/**
 * 验证码信息
 */
data class CaptchaInfo(
    val sessionId: String,
    val imagePath: String
) 