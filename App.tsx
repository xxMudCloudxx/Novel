import React from 'react';
import { SafeAreaView, Text, StyleSheet, View } from 'react-native';
import { useNovelColors } from './src/utils/theme/colors';
import { typography } from './src/utils/theme/typography';
import Moon from './assets/image/moon_mode.svg';

type Props = { nativeMessage?: string };

export default function App({ nativeMessage }: Props): React.JSX.Element {
  const colors = useNovelColors();
  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.novelBackground }]}>
      {/* 标题 */}
      <Text style={styles.title}>
        React Native 页面
      </Text>

      {/* moon.svg 图标 */}
      <View style={styles.iconWrapper}>
        <Moon width={20} height={20} />
      </View>

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
    marginBottom: 16,
  },
  iconWrapper: {
    marginVertical: 16,
  },
  body: {
    ...typography.bodyMedium,
  },
});