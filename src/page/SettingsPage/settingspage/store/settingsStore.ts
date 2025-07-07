import { create } from 'zustand';
import { NativeModules } from 'react-native';
import { SettingsStore, ColorScheme } from '../types';
import { useThemeStore } from '../../../../utils/theme/themeStore';

const { SettingsBridge, NavigationBridge } = NativeModules;

// Android原生缓存清理调用
const clearAppCache = async (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.clearAllCache) {
      SettingsBridge.clearAllCache((error: string | null, result: string) => {
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
    if (SettingsBridge?.calculateCacheSize) {
      SettingsBridge.calculateCacheSize((error: string | null, result: string) => {
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

// Android原生切换夜间模式 - 修复版本
const toggleNightModeNative = async (): Promise<{ message: string; newTheme: string }> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.toggleNightMode) {
      SettingsBridge.toggleNightMode((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          // 获取切换后的实际主题
          SettingsBridge.getCurrentActualTheme((err: string | null, actualTheme: string) => {
            if (err) {
              resolve({ message: result, newTheme: 'light' }); // 默认浅色
            } else {
              resolve({ message: result, newTheme: actualTheme });
            }
          });
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      const currentTheme = useThemeStore.getState().isDarkMode ? 'light' : 'dark';
      resolve({ message: `已切换至${currentTheme === 'dark' ? '深色' : '浅色'}模式`, newTheme: currentTheme });
    }
  });
};

// Android原生设置夜间模式 - 修复版本
const setNightModeNative = (mode: string): Promise<{ message: string; actualTheme: string }> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.setNightMode) {
      SettingsBridge.setNightMode(mode, (error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          // 获取设置后的实际主题
          SettingsBridge.getCurrentActualTheme((err: string | null, actualTheme: string) => {
            if (err) {
              resolve({ message: result, actualTheme: mode === 'dark' ? 'dark' : 'light' });
            } else {
              resolve({ message: result, actualTheme });
            }
          });
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      resolve({ message: `夜间模式已设置为: ${mode}`, actualTheme: mode === 'dark' ? 'dark' : 'light' });
    }
  });
};

// Android原生获取当前夜间模式
const getCurrentNightModeNative = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.getCurrentNightMode) {
      SettingsBridge.getCurrentNightMode((error: string | null, result: string) => {
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

// Android原生获取当前实际主题
const getCurrentActualThemeNative = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.getCurrentActualTheme) {
      SettingsBridge.getCurrentActualTheme((error: string | null, result: string) => {
        if (error) {
          reject(new Error(error));
        } else {
          resolve(result);
        }
      });
    } else {
      // 模拟数据（当在纯RN环境中运行时）
      resolve(useThemeStore.getState().isDarkMode ? 'dark' : 'light');
    }
  });
};

// Android原生获取是否跟随系统主题
const isFollowSystemThemeNative = (): Promise<boolean> => {
  return new Promise((resolve, reject) => {
    if (SettingsBridge?.isFollowSystemTheme) {
      SettingsBridge.isFollowSystemTheme((error: string | null, result: boolean) => {
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
      if (SettingsBridge?.setFollowSystemTheme) {
        SettingsBridge.setFollowSystemTheme(follow, async (error: string | null, result: string) => {
          if (error) {
            console.error('[SettingsStore] 设置跟随系统主题失败:', error);
          } else {
            console.log('[SettingsStore] 跟随系统主题设置结果:', result);
            
            // 设置成功后，同步获取当前实际主题状态
            try {
              const actualTheme = await getCurrentActualThemeNative();
              const currentMode = await getCurrentNightModeNative();
              
              // 同步到主题Store
              useThemeStore.getState().setTheme(currentMode as ColorScheme);
              
              console.log('[SettingsStore] 主题状态已同步:', { currentMode, actualTheme });
            } catch (e) {
              console.error('[SettingsStore] 同步主题状态失败:', e);
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
      console.log('[SettingsStore] 🎯 开始设置主题模式:', scheme);
      
      const result = await setNightModeNative(scheme);
      console.log('[SettingsStore] ✅ Android端设置完成:', result);

      // 同步到主题Store - 使用实际主题而不是设置的模式
      useThemeStore.getState().setTheme(result.actualTheme as ColorScheme);

      set({
        colorScheme: scheme,
        followSystemTheme: scheme === 'auto',
      });
      
      console.log('[SettingsStore] ✅ 主题设置完成:', {
        设置模式: scheme,
        实际主题: result.actualTheme,
        跟随系统: scheme === 'auto'
      });
      
    } catch (error) {
      console.error('[SettingsStore] ❌ 设置主题模式失败:', error);
    }
  },

  toggleColorScheme: async () => {
    try {
      console.log('[SettingsStore] 🎯 开始切换主题');
      
      const result = await toggleNightModeNative();
      console.log('[SettingsStore] ✅ Android端切换完成:', result);

      // 获取新的模式状态
      const newMode = await getCurrentNightModeNative();

      // 同步到主题Store - 使用实际主题
      useThemeStore.getState().setTheme(result.newTheme as ColorScheme);

      // 更新设置状态
      set({
        colorScheme: newMode as ColorScheme,
        followSystemTheme: newMode === 'auto',
      });
      
      console.log('[SettingsStore] ✅ 主题切换完成:', {
        新模式: newMode,
        实际主题: result.newTheme,
        跟随系统: newMode === 'auto'
      });
      
    } catch (error) {
      console.error('[SettingsStore] ❌ 切换夜间模式失败:', error);
    }
  },

  setAutoSwitchNightMode: (enabled: boolean) => {
    set({ autoSwitchNightMode: enabled });
    console.log('[SettingsStore] 自动切换夜间模式:', enabled);

    // 同步到Android端
    if (SettingsBridge?.setAutoNightMode) {
      SettingsBridge.setAutoNightMode(enabled, (error: string | null, result: string) => {
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
    if (SettingsBridge?.setNightModeTime) {
      SettingsBridge.setNightModeTime(start, end, (error: string | null, result: string) => {
        if (error) {
          console.error('[SettingsStore] 设置夜间模式时间失败:', error);
        } else {
          console.log('[SettingsStore] 夜间模式时间设置结果:', result);
        }
      });
    }
  },

  // 初始化设置状态 - 新增方法
  initializeSettings: async () => {
    try {
      console.log('[SettingsStore] 🎯 开始初始化设置状态');
      
      // 并行获取所有设置
      const [currentMode, actualTheme, followSystem, autoEnabled, startTime, endTime] = await Promise.all([
        getCurrentNightModeNative().catch(() => 'auto'),
        getCurrentActualThemeNative().catch(() => 'light'),
        isFollowSystemThemeNative().catch(() => true),
        new Promise<boolean>((resolve) => {
          if (SettingsBridge?.isAutoNightModeEnabled) {
            SettingsBridge.isAutoNightModeEnabled((err: string | null, result: boolean) => {
              resolve(err ? false : result);
            });
          } else {
            resolve(false);
          }
        }),
        new Promise<string>((resolve) => {
          if (SettingsBridge?.getNightModeStartTime) {
            SettingsBridge.getNightModeStartTime((err: string | null, result: string) => {
              resolve(err ? '22:00' : result);
            });
          } else {
            resolve('22:00');
          }
        }),
        new Promise<string>((resolve) => {
          if (SettingsBridge?.getNightModeEndTime) {
            SettingsBridge.getNightModeEndTime((err: string | null, result: string) => {
              resolve(err ? '06:00' : result);
            });
          } else {
            resolve('06:00');
          }
        })
      ]);

      // 更新状态
      set({
        colorScheme: currentMode as ColorScheme,
        followSystemTheme: followSystem,
        autoSwitchNightMode: autoEnabled,
        nightModeStartTime: startTime,
        nightModeEndTime: endTime,
      });

      // 同步到主题Store
      useThemeStore.getState().setTheme(actualTheme as ColorScheme);

      console.log('[SettingsStore] ✅ 设置初始化完成:', {
        currentMode,
        actualTheme,
        followSystem,
        autoEnabled,
        nightModeTime: `${startTime}-${endTime}`
      });
      
    } catch (error) {
      console.error('[SettingsStore] ❌ 初始化设置状态失败:', error);
    }
  },

  // 获取夜间模式时间设置
  loadNightModeTime: async () => {
    try {
      if (SettingsBridge?.getNightModeStartTime && SettingsBridge?.getNightModeEndTime) {
        const startTime = await new Promise<string>((resolve, reject) => {
          SettingsBridge.getNightModeStartTime((error: string | null, result: string) => {
            if (error) {reject(new Error(error));}
            else {resolve(result);}
          });
        });

        const endTime = await new Promise<string>((resolve, reject) => {
          SettingsBridge.getNightModeEndTime((error: string | null, result: string) => {
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
      if (SettingsBridge?.isAutoNightModeEnabled) {
        const enabled = await new Promise<boolean>((resolve, reject) => {
          SettingsBridge.isAutoNightModeEnabled((error: string | null, result: boolean) => {
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
    if (NavigationBridge?.navigateToHelpSupport) {
      NavigationBridge.navigateToHelpSupport();
    } else {
      // Fallback：直接创建 RN 视图
      if (NavigationBridge?.navigateToReactNativePage) {
        NavigationBridge.navigateToReactNativePage('HelpSupportPageComponent');
      } else {
        console.warn('[SettingsStore] NavigationBridge.navigateToHelpSupport not available');
      }
    }
  },

  navigateToPrivacyPolicy: () => {
    console.log('[SettingsStore] 导航到隐私政策页面');
    if (NavigationBridge?.navigateToPrivacyPolicy) {
      NavigationBridge.navigateToPrivacyPolicy();
    } else {
      if (NavigationBridge?.navigateToReactNativePage) {
        NavigationBridge.navigateToReactNativePage('PrivacyPolicyPageComponent');
      } else {
        console.warn('[SettingsStore] NavigationBridge.navigateToPrivacyPolicy not available');
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

// 初始化时计算缓存大小和加载设置
setTimeout(() => {
  const store = useSettingsStore.getState();
  store.calculateCacheSize();
  store.initializeSettings(); // 加载所有设置状态
}, 1000);
