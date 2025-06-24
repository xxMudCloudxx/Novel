import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import IconComponent from '../../../../component/IconComponent';
import { SettingItem } from '../types/index';
import { createSettingsPageStyles } from '../styles/SettingsPageStyles';
import { commonSizes } from '../../../../utils/theme/dimensions';
import { useNovelColors } from '../../../../utils/theme/colors';

interface SettingRowProps {
  item: SettingItem;
  onPress?: () => void;
}

/**
 * 设置项行组件
 * 支持多种类型：开关、箭头、切换按钮、操作按钮
 */
export const SettingRow: React.FC<SettingRowProps> = ({ item, onPress }) => {
  const colors = useNovelColors();
  const styles = createSettingsPageStyles(colors);

  const handlePress = () => {
    if (item.onPress) {
      item.onPress();
    } else if (onPress) {
      onPress();
    }
  };

  /**
   * 渲染右侧内容
   */
  const renderRightContent = () => {
    switch (item.type) {
      case 'switch':
        return (
          <TouchableOpacity
            onPress={() => item.onValueChange?.(!item.value)}
            disabled={item.disabled}
            style={[
              styles.customSwitch,
              item.value && styles.customSwitchActive,
            ]}
          >
            <View
              style={[
                styles.customSwitchThumb,
                item.value
                  ? styles.customSwitchThumbActive
                  : styles.customSwitchThumbInactive,
              ]}
            />
          </TouchableOpacity>
        );

      case 'toggle':
        return (
          <TouchableOpacity
            onPress={handlePress}
            style={styles.themeToggleButton}
            disabled={item.disabled}
          >
            <Text style={styles.themeToggleText}>
              {item.value === 'dark' ? '🌙' : '☀️'}
            </Text>
          </TouchableOpacity>
        );

      case 'action':
        return (
          <View style={styles.settingRight}>
            {item.value && (
              <Text style={styles.settingValue}>
                {item.value}
              </Text>
            )}
            <Text style={styles.arrow}>›</Text>
          </View>
        );

      case 'arrow':
      default:
        return (
          <View style={styles.settingRight}>
            {item.value && (
              <Text style={styles.settingValue}>
                {item.value}
              </Text>
            )}
            <Text style={styles.arrow}>›</Text>
          </View>
        );
    }
  };

  /**
   * 渲染左侧内容
   */
  const renderLeftContent = () => (
    <View style={styles.settingLeft}>
      {item.icon && (
        <View style={styles.settingIcon}>
          <IconComponent
            name={item.icon}
            width={commonSizes.iconSize}
            height={commonSizes.iconSize}
          />
        </View>
      )}
      <Text style={[
        styles.settingTitle,
        item.disabled && styles.disabledTitle,
      ]}>
        {item.title}
      </Text>
    </View>
  );

  const isInteractive = item.type === 'arrow' || item.type === 'action' || item.type === 'toggle';

  // 交互式设置项（可点击）
  if (isInteractive && !item.disabled) {
    return (
      <TouchableOpacity
        style={[
          styles.settingRow,
          item.disabled && styles.disabledRow,
        ]}
        onPress={handlePress}
        activeOpacity={0.7}
      >
        {renderLeftContent()}
        {renderRightContent()}
      </TouchableOpacity>
    );
  }

  // 非交互式设置项（仅显示）
  return (
    <View
      style={[
        styles.settingRow,
        item.disabled && styles.disabledRow,
      ]}
    >
      {renderLeftContent()}
      {renderRightContent()}
    </View>
  );
};
