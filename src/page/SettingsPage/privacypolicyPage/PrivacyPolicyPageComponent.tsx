import React from 'react';
import { AppRegistry } from 'react-native';
import PrivacyPolicyPage from './PrivacyPolicyPage';

/**
 * 用于在 Android 中嵌入的隐私政策页面组件
 */
const PrivacyPolicyPageComponent: React.FC = () => <PrivacyPolicyPage />;

AppRegistry.registerComponent('PrivacyPolicyPageComponent', () => PrivacyPolicyPageComponent);

export default PrivacyPolicyPageComponent;
