import { create } from 'zustand';
import { DeviceEventEmitter, NativeModules } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

export type ThemeMode = 'light' | 'dark' | 'auto';

interface ThemeState {
  currentTheme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  isDarkMode: boolean;
  isInitialized: boolean;
}

const THEME_STORAGE_KEY = 'novel_theme_mode';

export const useThemeStore = create<ThemeState>((set) => ({
  currentTheme: 'auto',
  isDarkMode: false,
  isInitialized: false,

  setTheme: async (theme: ThemeMode) => {
    const isDark = theme === 'dark' || (theme === 'auto' && isSystemDarkMode());

    set({
      currentTheme: theme,
      isDarkMode: isDark,
    });

    // 保存到AsyncStorage
    try {
      await AsyncStorage.setItem(THEME_STORAGE_KEY, theme);
      console.log('[ThemeStore] 主题已保存到AsyncStorage:', theme);
    } catch (e) {
      console.warn('[ThemeStore] 保存主题到AsyncStorage失败:', e);
    }

    // 同步到Android原生
    try {
      if (NativeModules.NavigationUtilModule?.changeTheme) {
        await NativeModules.NavigationUtilModule.changeTheme(theme);
        console.log('[ThemeStore] 主题已同步到Android:', theme);
      }
    } catch (e) {
      console.warn('[ThemeStore] 同步主题到Android失败:', e);
    }
  },
}));

// 检测系统是否为深色模式（简化版本）
function isSystemDarkMode(): boolean {
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
  return false;
}

/**
 * 从AsyncStorage恢复主题设置
 */
export const restoreThemeFromStorage = async (): Promise<ThemeMode> => {
  try {
    const savedTheme = await AsyncStorage.getItem(THEME_STORAGE_KEY);
    if (savedTheme && ['light', 'dark', 'auto'].includes(savedTheme)) {
      console.log('[ThemeStore] 从AsyncStorage恢复主题:', savedTheme);
      return savedTheme as ThemeMode;
    }
  } catch (e) {
    console.warn('[ThemeStore] 从AsyncStorage恢复主题失败:', e);
  }

  // 默认返回auto模式
  console.log('[ThemeStore] 使用默认主题: auto');
  return 'auto';
};

/**
 * 清除主题缓存
 */
export const clearThemeCache = async (): Promise<void> => {
  try {
    await AsyncStorage.removeItem(THEME_STORAGE_KEY);
    console.log('[ThemeStore] 主题缓存已清除');
  } catch (e) {
    console.warn('[ThemeStore] 清除主题缓存失败:', e);
  }
};

// 初始化主题
export const initializeTheme = async (): Promise<() => void> => {
  try {
    // 从AsyncStorage恢复主题
    const savedTheme = await restoreThemeFromStorage();

    // 设置主题（不触发保存，因为已经是从存储中恢复的）
    const isDark = savedTheme === 'dark' || (savedTheme === 'auto' && isSystemDarkMode());
    useThemeStore.setState({
      currentTheme: savedTheme,
      isDarkMode: isDark,
      isInitialized: true,
    });

    // 监听原生主题变更事件
    const subscription = DeviceEventEmitter.addListener('ThemeChanged', (data: { colorScheme: string }) => {
      console.log('[ThemeStore] 收到Android主题变更事件:', data);
      const newTheme = data.colorScheme as ThemeMode;
      if (['light', 'dark', 'auto'].includes(newTheme)) {
        // 更新状态但不重新保存，因为这是从Android端传来的变更
        const isDark = newTheme === 'dark' || (newTheme === 'auto' && isSystemDarkMode());
        useThemeStore.setState({
          currentTheme: newTheme,
          isDarkMode: isDark,
        });

        // 同步保存到AsyncStorage
        AsyncStorage.setItem(THEME_STORAGE_KEY, newTheme).catch(e => {
          console.warn('[ThemeStore] 同步保存主题失败:', e);
        });
      }
    });

    console.log('[ThemeStore] 主题初始化完成:', savedTheme);

    // 返回清理函数
    return () => {
      subscription?.remove();
      console.log('[ThemeStore] 主题监听器已清理');
    };
  } catch (e) {
    console.warn('[ThemeStore] 主题初始化失败:', e);
    // 失败时使用默认设置
    useThemeStore.setState({
      currentTheme: 'auto',
      isDarkMode: isSystemDarkMode(),
      isInitialized: true,
    });
    return () => {};
  }
};
