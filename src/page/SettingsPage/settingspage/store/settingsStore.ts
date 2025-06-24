import { create } from 'zustand';
import { NativeModules } from 'react-native';
import { SettingsStore, ColorScheme } from '../types';
import { useThemeStore } from '../../../../utils/theme/themeStore';

const { NavigationUtil } = NativeModules;

// Android原生缓存清理调用
const clearAppCache = async (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.clearAllCache) {
      NavigationUtil.clearAllCache((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      setTimeout(() => {
        resolve('已清理 12.5MB 缓存');
      }, 2000);
    }
  });
};

// Android原生计算缓存大小
const calculateCacheSize = async (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.calculateCacheSize) {
      NavigationUtil.calculateCacheSize((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      setTimeout(() => {
        const sizes = ['12.5MB', '25.3MB', '8.7MB', '45.2MB', '67.1MB'];
        const randomSize = sizes[Math.floor(Math.random() * sizes.length)];
        resolve(randomSize);
      }, 1000);
    }
  });
};

// Android原生切换夜间模式
const toggleNightModeNative = async (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.toggleNightMode) {
      NavigationUtil.toggleNightMode((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      resolve('已切换至深色模式');
    }
  });
};

// Android原生设置夜间模式
const setNightModeNative = (mode: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.setNightMode) {
      NavigationUtil.setNightMode(mode, (error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      resolve(`夜间模式已设置为: ${mode}`);
    }
  });
};

// Android原生获取当前夜间模式
const getCurrentNightModeNative = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.getCurrentNightMode) {
      NavigationUtil.getCurrentNightMode((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      resolve('auto');
    }
  });
};

// Android原生获取是否跟随系统主题
const isFollowSystemThemeNative = (): Promise<boolean> => {
  return new Promise((resolve, reject) => {
    if (NavigationUtil?.isFollowSystemTheme) {
      NavigationUtil.isFollowSystemTheme((error: string | null, result: boolean) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      console.warn('[SettingsStore] isFollowSystemTheme not available, falling back to true.');
      resolve(true); // 默认启用
    }
  });
};

export const useSettingsStore = create<SettingsStore>((set, get) => ({
  // 初始状态
  cacheSize: '计算中...',
  pushNotificationEnabled: true,
  benefitNotificationEnabled: true,
  followSystemTheme: true,
  colorScheme: 'auto' as ColorScheme,
  autoSwitchNightMode: false,
  nightModeStartTime: '22:00',
  nightModeEndTime: '06:00',
  fontSize: 16,
  useMobileDataWhenWiFiPoor: true,
  enableFloatingWindow: false,
  youthModeEnabled: false,
  privacySettingsVersion: '1.0.0',

  // 缓存操作
  clearCache: async () => {
    try {
      set({ cacheSize: '清理中...' });
      const result = await clearAppCache();
      console.log('[SettingsStore] 缓存清理结果:', result);
      // 重新计算缓存大小
      const newSize = await calculateCacheSize();
      set({ cacheSize: newSize });
    } catch (error) {
      console.error('[SettingsStore] 清理缓存失败:', error);
      set({ cacheSize: '计算失败' });
    }
  },

  calculateCacheSize: async () => {
    try {
      const size = await calculateCacheSize();
      set({ cacheSize: size });
    } catch (error) {
      console.error('[SettingsStore] 计算缓存大小失败:', error);
      set({ cacheSize: '计算失败' });
    }
  },

  // 通知设置
  setPushNotification: (enabled: boolean) => {
    set({ pushNotificationEnabled: enabled });
    console.log('[SettingsStore] 推送通知设置:', enabled);
  },

  setBenefitNotification: (enabled: boolean) => {
    set({ benefitNotificationEnabled: enabled });
    console.log('[SettingsStore] 福利通知设置:', enabled);
  },

  // 获取当前实际的主题模式（用于显示icon）
  getCurrentDisplayTheme: () => {
    const themeStore = useThemeStore.getState();

    // 始终返回实际的主题状态（从themeStore中获取）
    // 这样可以正确反映Android端传来的实际主题状态
    return themeStore.isDarkMode ? 'dark' : 'light';
  },

  // 主题设置
  setFollowSystemTheme: async (follow: boolean) => {
    try {
      if (NavigationUtil?.setFollowSystemTheme) {
        NavigationUtil.setFollowSystemTheme(follow, (error: string | null, result: string) => {
          if (error) {
            console.error('[SettingsStore] 设置跟随系统主题失败:', error);
          } else {
            console.log('[SettingsStore] 跟随系统主题设置结果:', result);
            // 设置成功后，如果是开启跟随系统，同步主题状态到主题Store
            if (follow) {
              console.log('[SettingsStore] 开启跟随系统主题，等待Android端发送实际主题状态');
            }
          }
        });
      }

      set({
        followSystemTheme: follow,
        colorScheme: follow ? 'auto' : get().colorScheme,
      });

      // 同步到主题Store
      if (follow) {
        useThemeStore.getState().setTheme('auto');
      }

      console.log('[SettingsStore] 跟随系统主题:', follow);
    } catch (error) {
      console.error('[SettingsStore] 设置跟随系统主题失败:', error);
    }
  },

  setColorScheme: async (scheme: ColorScheme) => {
    try {
      await setNightModeNative(scheme);

      // 同步到主题Store
      useThemeStore.getState().setTheme(scheme);

      set({
        colorScheme: scheme,
        followSystemTheme: scheme === 'auto',
      });
      console.log('[SettingsStore] 主题模式:', scheme);
    } catch (error) {
      console.error('[SettingsStore] 设置主题模式失败:', error);
    }
  },

  toggleColorScheme: async () => {
    try {
      const result = await toggleNightModeNative();
      console.log('[SettingsStore] 切换夜间模式结果:', result);

      // 获取新的模式状态
      const newMode = await getCurrentNightModeNative();

      // 同步到主题Store
      useThemeStore.getState().setTheme(newMode as ColorScheme);

      // 更新设置状态
      set({
        colorScheme: newMode as ColorScheme,
        followSystemTheme: newMode === 'auto',
      });
    } catch (error) {
      console.error('[SettingsStore] 切换夜间模式失败:', error);
    }
  },

  setAutoSwitchNightMode: (enabled: boolean) => {
    set({ autoSwitchNightMode: enabled });
    console.log('[SettingsStore] 自动切换夜间模式:', enabled);

    // 同步到Android端
    if (NavigationUtil?.setAutoNightMode) {
      NavigationUtil.setAutoNightMode(enabled, (error: string | null, result: string) => {
        if (error) {
          console.error('[SettingsStore] 设置自动切换夜间模式失败:', error);
        } else {
          console.log('[SettingsStore] 自动切换夜间模式设置结果:', result);
        }
      });
    }
  },

  setNightModeTime: (start: string, end: string) => {
    set({
      nightModeStartTime: start,
      nightModeEndTime: end,
    });
    console.log('[SettingsStore] 夜间模式时间:', start, '-', end);

    // 同步到Android端
    if (NavigationUtil?.setNightModeTime) {
      NavigationUtil.setNightModeTime(start, end, (error: string | null, result: string) => {
        if (error) {
          console.error('[SettingsStore] 设置夜间模式时间失败:', error);
        } else {
          console.log('[SettingsStore] 夜间模式时间设置结果:', result);
        }
      });
    }
  },

  // 获取夜间模式时间设置
  loadNightModeTime: async () => {
    try {
      if (NavigationUtil?.getNightModeStartTime && NavigationUtil?.getNightModeEndTime) {
        const startTime = await new Promise<string>((resolve, reject) => {
          NavigationUtil.getNightModeStartTime((error: string | null, result: string) => {
            if (error) {reject(new Error(error));}
            else {resolve(result);}
          });
        });

        const endTime = await new Promise<string>((resolve, reject) => {
          NavigationUtil.getNightModeEndTime((error: string | null, result: string) => {
            if (error) {reject(new Error(error));}
            else {resolve(result);}
          });
        });

        set({
          nightModeStartTime: startTime,
          nightModeEndTime: endTime,
        });

        console.log('[SettingsStore] 夜间模式时间已加载:', startTime, '-', endTime);
      }
    } catch (error) {
      console.error('[SettingsStore] 加载夜间模式时间失败:', error);
    }
  },

  // 加载自动切换夜间模式设置
  loadAutoSwitchNightMode: async () => {
    try {
      if (NavigationUtil?.isAutoNightModeEnabled) {
        const enabled = await new Promise<boolean>((resolve, reject) => {
          NavigationUtil.isAutoNightModeEnabled((error: string | null, result: boolean) => {
            if (error) {reject(new Error(error));}
            else {resolve(result);}
          });
        });

        set({ autoSwitchNightMode: enabled });
        console.log('[SettingsStore] 自动切换夜间模式设置已加载:', enabled);
      }
    } catch (error) {
      console.error('[SettingsStore] 加载自动切换夜间模式设置失败:', error);
    }
  },

  // 字体设置
  setFontSize: (size: number) => {
    const clampedSize = Math.max(12, Math.min(24, size));
    set({ fontSize: clampedSize });
    console.log('[SettingsStore] 字体大小:', clampedSize);
  },

  // 网络设置
  setUseMobileDataWhenWiFiPoor: (enabled: boolean) => {
    set({ useMobileDataWhenWiFiPoor: enabled });
    console.log('[SettingsStore] WiFi较差时使用移动网络:', enabled);
  },

  // 播放设置
  setEnableFloatingWindow: (enabled: boolean) => {
    set({ enableFloatingWindow: enabled });
    console.log('[SettingsStore] 小窗播放:', enabled);
  },

  // 青少年模式
  setYouthMode: (enabled: boolean) => {
    set({ youthModeEnabled: enabled });
    console.log('[SettingsStore] 青少年模式:', enabled);
  },

  // 导航操作
  navigateToAbout: () => {
    console.log('[SettingsStore] 导航到关于页面');
    // TODO: 实现导航逻辑
  },

  navigateToCustomerService: () => {
    console.log('[SettingsStore] 导航到客服页面');
    if (NavigationUtil?.navigateToHelpSupport) {
      NavigationUtil.navigateToHelpSupport();
    } else {
      // Fallback：直接创建 RN 视图
      if (NavigationUtil?.navigateToReactNativePage) {
        NavigationUtil.navigateToReactNativePage('HelpSupportPageComponent');
      } else {
        console.warn('[SettingsStore] NavigationUtil.navigateToHelpSupport not available');
      }
    }
  },

  navigateToPrivacyPolicy: () => {
    console.log('[SettingsStore] 导航到隐私政策页面');
    if (NavigationUtil?.navigateToPrivacyPolicy) {
      NavigationUtil.navigateToPrivacyPolicy();
    } else {
      if (NavigationUtil?.navigateToReactNativePage) {
        NavigationUtil.navigateToReactNativePage('PrivacyPolicyPageComponent');
      } else {
        console.warn('[SettingsStore] NavigationUtil.navigateToPrivacyPolicy not available');
      }
    }
  },

  navigateToFontSettings: () => {
    console.log('[SettingsStore] 导航到字体设置页面');
    // TODO: 实现导航逻辑
  },

  loadFollowSystemTheme: async () => {
    try {
      const enabled = await isFollowSystemThemeNative();
      set({ followSystemTheme: enabled });
      console.log('[SettingsStore] 跟随系统主题设置已加载:', enabled);
    } catch (error) {
      console.error('[SettingsStore] 加载跟随系统主题设置失败:', error);
    }
  },
}));

// 初始化时计算缓存大小
setTimeout(() => {
  useSettingsStore.getState().calculateCacheSize();
}, 1000);
