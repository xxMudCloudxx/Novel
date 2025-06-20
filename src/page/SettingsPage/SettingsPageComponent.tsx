import React from 'react';
import { AppRegistry } from 'react-native';
import SettingsPage from './SettingsPage';

/**
 * 用于在Android中嵌入的设置页面组件
 * 与主应用分离，可以独立渲染
 */
const SettingsPageComponent: React.FC = () => {
  return <SettingsPage />;
};

// 注册为独立的RN组件
AppRegistry.registerComponent('SettingsPageComponent', () => SettingsPageComponent);

export default SettingsPageComponent;
