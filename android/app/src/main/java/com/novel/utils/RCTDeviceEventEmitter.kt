package com.novel.utils

import com.facebook.react.ReactApplication
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class RCTDeviceEventEmitter {
    companion object {
        fun sendEvent(
            reactContext: ReactContext?,
            eventName: String,
            params: WritableMap?
        ) {
            reactContext?.let { context ->
                context
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    ?.emit(eventName, params)
            }
        }
    }
}