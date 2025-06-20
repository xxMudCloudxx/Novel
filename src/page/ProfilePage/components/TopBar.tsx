import React from 'react';
import { View, TouchableOpacity } from 'react-native';
import IconComponent from '../../../component/IconComponent';
import { commonSizes } from '../../../utils/theme/dimensions';
import { useThemeStore } from '../../../utils/theme/themeStore';

interface TopBarProps {
  styles: any;
  onSettingsPress?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ styles, onSettingsPress }) => {
  const { setTheme, currentTheme } = useThemeStore();

  const handleThemeToggle = () => {
    const nextTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    console.log('主题切换到:', nextTheme);
  };

  return (
  <View style={styles.topBar}>
    <TouchableOpacity onPress={() => console.log('QR Code')}>
      <IconComponent name="qrscan" width={commonSizes.iconSize} height={commonSizes.iconSize} />
    </TouchableOpacity>
      <TouchableOpacity onPress={handleThemeToggle}>
      <IconComponent name="moon_mode" width={commonSizes.iconSize} height={commonSizes.iconSize} />
    </TouchableOpacity>
    <TouchableOpacity onPress={onSettingsPress || (() => console.log('Settings'))}>
      <IconComponent name="settings" width={commonSizes.iconSize} height={commonSizes.iconSize} />
    </TouchableOpacity>
  </View>
);
};
