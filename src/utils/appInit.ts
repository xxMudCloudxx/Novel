import { nativeEventListener } from './nativeEventListener';

/**
 * 初始化应用
 * 在应用启动时调用，设置必要的监听器和配置
 */
export function initializeApp() {
  console.log('[AppInit] Initializing application...');

  try {
    // 初始化原生事件监听器
    nativeEventListener.init();

    console.log('[AppInit] Application initialized successfully');
  } catch (error) {
    console.error('[AppInit] Failed to initialize application:', error);
  }
}

/**
 * 清理应用资源
 * 在应用卸载时调用
 */
export function cleanupApp() {
  console.log('[AppInit] Cleaning up application...');

  try {
    // 清理原生事件监听器
    nativeEventListener.cleanup();

    console.log('[AppInit] Application cleaned up successfully');
  } catch (error) {
    console.error('[AppInit] Failed to cleanup application:', error);
  }
}
