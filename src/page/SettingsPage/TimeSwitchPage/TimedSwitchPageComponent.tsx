import React from 'react';
import { AppRegistry } from 'react-native';
import TimedSwitchPage from './TimedSwitchPage';

/**
 * 用于在Android中嵌入的定时切换页面组件
 * 与主应用分离，可以独立渲染
 */
const TimedSwitchPageComponent: React.FC = () => {
  return <TimedSwitchPage />;
};

// 注册为独立的RN组件
AppRegistry.registerComponent('TimedSwitchPageComponent', () => TimedSwitchPageComponent);

export default TimedSwitchPageComponent;
