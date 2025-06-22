import { nativeEventListener } from './nativeEventListener';

// 模块级别的页面状态缓存
const pageStateCache: Record<string, any> = {};

/**
 * 保存页面状态
 */
export function savePageState(pageId: string, state: any) {
  console.log(`[AppInit] Saving state for page: ${pageId}`);
  pageStateCache[pageId] = state;
}

/**
 * 获取页面状态
 */
export function getPageState(pageId: string): any {
  const state = pageStateCache[pageId];
  console.log(`[AppInit] Restoring state for page: ${pageId}`, state);
  return state;
}

/**
 * 清除页面状态
 */
export function clearPageState(pageId: string) {
  delete pageStateCache[pageId];
  console.log(`[AppInit] Cleared state for page: ${pageId}`);
}

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
    
    // 清理页面状态缓存
    Object.keys(pageStateCache).forEach(key => {
      delete pageStateCache[key];
    });

    console.log('[AppInit] Application cleaned up successfully');
  } catch (error) {
    console.error('[AppInit] Failed to cleanup application:', error);
  }
}
