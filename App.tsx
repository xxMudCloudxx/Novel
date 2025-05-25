import React from 'react';
import { SafeAreaView, Text, StyleSheet } from 'react-native';
import { useNovelColors } from './src/utils/theme/colors';
import { typography } from './src/utils/theme/typography';

type Props = { nativeMessage?: string };

export default function App({ nativeMessage }: Props): React.JSX.Element {
  const colors = useNovelColors();

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.novelBackground }]}>
      {/* 标题 */}
      <Text style={styles.title}>
        React Native 页面
      </Text>

      {/* 显示 Token */}
      <Text style={styles.body}>
        Token 是：{nativeMessage}
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    ...typography.titleLarge,
    color: '#yourColorHere', // This will be overridden by dynamic color below
    marginBottom: 16,
  },
  body: {
    ...typography.bodyMedium,
    color: '#yourGrayColorHere', // This will be overridden by dynamic color below
  },
});