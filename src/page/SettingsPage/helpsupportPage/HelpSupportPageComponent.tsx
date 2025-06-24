import React from 'react';
import { AppRegistry } from 'react-native';
import HelpSupportPage from './HelpSupportPage';

/**
 * 用于在 Android 中嵌入的帮助与支持页面组件
 */
const HelpSupportPageComponent: React.FC = () => <HelpSupportPage />;

AppRegistry.registerComponent('HelpSupportPageComponent', () => HelpSupportPageComponent);

export default HelpSupportPageComponent;
