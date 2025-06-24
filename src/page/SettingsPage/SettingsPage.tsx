import React from 'react';
import { View, ScrollView, Text, TouchableOpacity, SafeAreaView } from 'react-native';
import { SettingRow } from './components';
import { useSettingsStore } from './store/settingsStore';
import { createSettingsPageStyles } from './styles/SettingsPageStyles';
import { SettingsSection } from './types';
import { useNovelColors } from '../../utils/theme/colors';
import { useThemeStore } from '../../utils/theme/themeStore';
import { NativeModules } from 'react-native';

const { NavigationUtil } = NativeModules;

/**
 * 设置页面主组件
 * 提供应用程序的各项设置功能
 * 包含顶部导航栏和设置内容
 */
const SettingsPage: React.FC = () => {
  const colors = useNovelColors();
  const styles = createSettingsPageStyles(colors);

  // 添加主题store
  const { initializeFromNative } = useThemeStore();

  const {
    // 状态
    cacheSize,
    pushNotificationEnabled,
    benefitNotificationEnabled,
    followSystemTheme,

    autoSwitchNightMode,
    useMobileDataWhenWiFiPoor,
    enableFloatingWindow,
    youthModeEnabled,

    // 操作
    clearCache,
    setPushNotification,
    setBenefitNotification,
    setFollowSystemTheme,
    toggleColorScheme,
    setUseMobileDataWhenWiFiPoor,
    setEnableFloatingWindow,
    setYouthMode,
    navigateToAbout,
    navigateToCustomerService,
    navigateToPrivacyPolicy,
    navigateToFontSettings,
    getCurrentDisplayTheme,
  } = useSettingsStore();

  // 初始化主题状态和设置
  React.useEffect(() => {
    const initializeSettings = async () => {
      try {
        console.log('[SettingsPage] 🎯 开始初始化设置状态');
        await initializeFromNative();
        // 加载所有相关设置
        const { loadFollowSystemTheme, loadAutoSwitchNightMode, loadNightModeTime } = useSettingsStore.getState();
        await Promise.all([
            loadFollowSystemTheme(),
            loadAutoSwitchNightMode(),
            loadNightModeTime(),
        ]);
        console.log('[SettingsPage] ✅ 设置状态初始化完成');
      } catch (error) {
        console.error('[SettingsPage] ❌ 设置状态初始化失败:', error);
      }
    };

    initializeSettings();
  }, [initializeFromNative]);

  /**
   * 处理返回按钮点击
   */
  const handleBackPress = () => {
    if (NavigationUtil?.navigateBack) {
      NavigationUtil.navigateBack('SettingsPageComponent');
    } else {
      console.log('NavigationUtil.navigateBack not available');
    }
  };

  /**
   * 创建设置项配置
   */
  const createSettingsSections = (): SettingsSection[] => [
    {
      id: 'cache',
      title: '存储管理',
      items: [
        {
          id: 'clearCache',
          title: '清理缓存',
          type: 'action',
          value: cacheSize,
          onPress: clearCache,
        },
      ],
    },
    {
      id: 'notifications',
      title: '通知设置',
      items: [
        {
          id: 'pushNotification',
          title: '推送通知',
          type: 'switch',
          value: pushNotificationEnabled,
          onValueChange: (value) => setPushNotification(value as boolean),
        },
        {
          id: 'benefitNotification',
          title: '福利领取提示',
          type: 'switch',
          value: benefitNotificationEnabled,
          onValueChange: (value) => setBenefitNotification(value as boolean),
        },
      ],
    },
    {
      id: 'reading',
      title: '阅读设置',
      items: [
        {
          id: 'fontSize',
          title: '字体大小',
          type: 'arrow',
          onPress: navigateToFontSettings,
        },
      ],
    },
    {
      id: 'theme',
      title: '主题设置',
      items: [
        {
          id: 'colorSchemeToggle',
          title: '主题模式',
          type: 'toggle',
          value: getCurrentDisplayTheme(),
          onPress: toggleColorScheme,
        },
        {
          id: 'followSystemTheme',
          title: '跟随系统主题',
          type: 'switch',
          value: followSystemTheme,
          onValueChange: (value) => setFollowSystemTheme(value as boolean),
        },
        {
          id: 'nightModeSwitch',
          title: '定时切换日夜间模式',
          type: 'arrow',
          value: autoSwitchNightMode ? '已开启' : '已关闭',
          onPress: () => {
            if (NavigationUtil?.navigateToTimedSwitch) {
              NavigationUtil.navigateToTimedSwitch();
            } else {
              console.log('NavigationUtil.navigateToTimedSwitch not available');
            }
          },
          disabled: followSystemTheme, // 跟随系统时禁用定时切换
        },
      ],
    },
    {
      id: 'playback',
      title: '播放设置',
      items: [
        {
          id: 'floatingWindow',
          title: '退出应用后开启小窗播放',
          type: 'switch',
          value: enableFloatingWindow,
          onValueChange: (value) => setEnableFloatingWindow(value as boolean),
        },
      ],
    },
    {
      id: 'network',
      title: '网络设置',
      items: [
        {
          id: 'mobileData',
          title: 'WiFi较差时使用移动网络优化体验',
          type: 'switch',
          value: useMobileDataWhenWiFiPoor,
          onValueChange: (value) => setUseMobileDataWhenWiFiPoor(value as boolean),
        },
      ],
    },
    {
      id: 'privacy',
      title: '隐私设置',
      items: [
        {
          id: 'privacyPolicy',
          title: '第三方信息共享清单',
          type: 'arrow',
          onPress: navigateToPrivacyPolicy,
        },
      ],
    },
    {
      id: 'youth',
      title: '青少年模式',
      items: [
        {
          id: 'youthMode',
          title: '青少年模式',
          type: 'switch',
          value: youthModeEnabled,
          onValueChange: (value) => setYouthMode(value as boolean),
        },
      ],
    },
    {
      id: 'support',
      title: '帮助与支持',
      items: [
        {
          id: 'customerService',
          title: '客服',
          type: 'arrow',
          onPress: navigateToCustomerService,
        },
        {
          id: 'about',
          title: '关于备份',
          type: 'arrow',
          onPress: navigateToAbout,
        },
      ],
    },
  ];

  /**
   * 渲染顶部导航栏
   */
  const renderTopBar = () => (
    <View style={styles.topBar}>
      {/* 返回按钮 */}
      <TouchableOpacity 
        style={styles.backButton}
        onPress={handleBackPress}
        activeOpacity={0.7}
      >
        <Text style={styles.backArrow}>‹</Text>
      </TouchableOpacity>
      
      {/* 设置标题 */}
      <View style={styles.titleContainer}>
        <Text style={styles.topBarTitle}>设置</Text>
      </View>
      
      {/* 右侧占位，保持标题居中 */}
      <View style={styles.rightPlaceholder} />
    </View>
  );

  /**
   * 渲染设置分组
   */
  const renderSection = (section: SettingsSection) => (
    <View key={section.id}>
      {section.title && (
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>{section.title}</Text>
        </View>
      )}
      {section.items.map((item) => (
        <SettingRow key={item.id} item={item} />
      ))}
    </View>
  );

  const settingsSections = createSettingsSections();

  return (
    <SafeAreaView style={styles.container}>
      {/* 顶部导航栏 */}
      {renderTopBar()}
      
      {/* 设置内容 */}
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {settingsSections.map(renderSection)}
      </ScrollView>
    </SafeAreaView>
  );
};

export default SettingsPage;
