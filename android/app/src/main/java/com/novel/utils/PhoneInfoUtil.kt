package com.novel.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.novel.utils.TimberLogger
import androidx.core.content.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 手机信息数据类
 * @param phoneNumber 手机号码
 * @param operatorName 运营商名称
 */
data class PhoneInfo(val phoneNumber: String, val operatorName: String)

/**
 * 手机信息获取工具类
 * 
 * 功能特点：
 * - 多版本API兼容（Android 13+ / 旧版本）
 * - 权限动态检查处理
 * - 多种方式获取运营商信息
 * - 异常安全处理机制
 * 
 * 权限要求：
 * - Android 13+: READ_PHONE_NUMBERS
 * - 旧版本: READ_PHONE_STATE
 * 
 * 技术实现：
 * - SubscriptionManager多卡支持
 * - TelephonyManager系统服务
 * - 协程异步IO线程处理
 */
class PhoneInfoUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PhoneInfoUtil"
    }
    
    /**
     * 获取手机信息（号码+运营商）
     * 支持多版本兼容和权限检查
     * @return PhoneInfo 手机信息对象
     */
    @SuppressLint("HardwareIds")
    suspend fun fetch(): PhoneInfo = withContext(Dispatchers.IO) {
        TimberLogger.d(TAG, "开始获取手机信息...")
        
        // 初始化系统服务
        val subMgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        // 根据API版本获取手机号码
        val rawNum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用READ_PHONE_NUMBERS权限
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                == PermissionChecker.PERMISSION_GRANTED
            ) {
                val phoneNumber = subMgr.getPhoneNumber(SubscriptionManager.getDefaultSubscriptionId())
                TimberLogger.d(TAG, "Android 13+ 获取手机号: ${phoneNumber.take(3)}***")
                phoneNumber
            } else {
                TimberLogger.w(TAG, "缺少READ_PHONE_NUMBERS权限")
                "权限不足"
            }
        } else {
            // 旧版本使用READ_PHONE_STATE权限
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PermissionChecker.PERMISSION_GRANTED
            ) {
                val num = tm.createForSubscriptionId(SubscriptionManager.getDefaultSubscriptionId())
                    .line1Number.orEmpty()
                TimberLogger.d(TAG, "旧版本获取手机号: ${num.take(3)}***")
                num
            } else {
                TimberLogger.w(TAG, "缺少READ_PHONE_STATE权限")
                "权限不足"
            }
        }

        // 获取运营商名称（多种方式兜底）
        val operator = tm.simOperatorName.takeIf(String::isNotBlank)
            ?: tm.networkOperatorName.takeIf(String::isNotBlank)
            ?: "未知运营商"
        
        TimberLogger.d(TAG, "运营商信息: $operator")
        
        val result = PhoneInfo(rawNum, operator)
        TimberLogger.d(TAG, "手机信息获取完成")
        result
    }
}
