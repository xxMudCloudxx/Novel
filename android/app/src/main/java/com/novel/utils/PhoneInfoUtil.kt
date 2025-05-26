package com.novel.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PhoneInfo(val phoneNumber: String, val operatorName: String)

/**
 * 获取本机号码和运营商信息
 */
class PhoneInfoUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @SuppressLint("HardwareIds")
    suspend fun fetch(): PhoneInfo = withContext(Dispatchers.IO) {
        // —— 1. 主流程：SubscriptionManager / TelephonyManager
        val subMgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val tm     = context.getSystemService(Context.TELEPHONY_SERVICE)            as TelephonyManager
        val rawNum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                == PermissionChecker.PERMISSION_GRANTED
            ) subMgr.getPhoneNumber(SubscriptionManager.getDefaultSubscriptionId())
            else ""
        } else {
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PermissionChecker.PERMISSION_GRANTED
            ) {
                val num = tm.createForSubscriptionId(SubscriptionManager.getDefaultSubscriptionId())
                    .line1Number.orEmpty()
                Log.d("PhoneInfoUtil", "rawNum: $num")
                num
            }
            else "手机号获取失败"
        }

        // —— 3. 并行取运营商名称
        val operator = tm.simOperatorName.takeIf(String::isNotBlank)
            ?: tm.networkOperatorName.takeIf(String::isNotBlank)
            ?: "未知运营商"

        PhoneInfo(rawNum, operator)
    }
}
