import React from 'react';
import { SafeAreaView, Text, StyleSheet } from 'react-native';
import { useNovelColors } from './src/utils/theme/colors';
import { typography } from './src/utils/theme/typography';

type Props = { nativeMessage?: string };

export default function App({ nativeMessage }: Props): React.JSX.Element {
  // 拿到当前主题下的所有颜色
  const colors = useNovelColors();

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.novelBackground }]}>
      {/* 标题 */}
      <Text style={[typography.titleLarge, { color: colors.novelText, marginBottom: 16 }]}>
        React Native 页面
      </Text>

      {/* 显示 Token */}
      <Text style={[typography.bodyMedium, { color: colors.novelTextGray }]}>
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
});
