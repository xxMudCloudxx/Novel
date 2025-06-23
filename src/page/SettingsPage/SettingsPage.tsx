import React from 'react';
import { View, ScrollView, Text } from 'react-native';
import { SettingRow } from './components/SettingRow';
import { useSettingsStore } from './store/settingsStore';
import { createSettingsPageStyles } from './styles/SettingsPageStyles';
import { SettingsSection } from './types';
import { useNovelColors } from '../../utils/theme/colors';
import { useThemeStore } from '../../utils/theme/themeStore';

/**
 * 设置页面主组件
 * 提供应用程序的各项设置功能
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
    setAutoSwitchNightMode,
    setUseMobileDataWhenWiFiPoor,
    setEnableFloatingWindow,
    setYouthMode,
    navigateToAbout,
    navigateToCustomerService,
    navigateToPrivacyPolicy,
    navigateToFontSettings,
    getCurrentDisplayTheme,
  } = useSettingsStore();

  // 初始化主题状态
  React.useEffect(() => {
    const initializeTheme = async () => {
      try {
        console.log('[SettingsPage] 🎯 开始初始化主题状态');
        await initializeFromNative();
        console.log('[SettingsPage] ✅ 主题状态初始化完成');
      } catch (error) {
        console.error('[SettingsPage] ❌ 主题状态初始化失败:', error);
      }
    };

    initializeTheme();
  }, [initializeFromNative]);

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
          type: 'switch',
          value: autoSwitchNightMode,
          onValueChange: (value) => setAutoSwitchNightMode(value as boolean),
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
    <View style={styles.container}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {settingsSections.map(renderSection)}
      </ScrollView>
    </View>
  );
};

export default SettingsPage;
