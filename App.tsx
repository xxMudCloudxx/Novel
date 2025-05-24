import React from 'react';
import { SafeAreaView, Text, StyleSheet } from 'react-native';

type Props = { nativeMessage?: string };

export default function App({ nativeMessage }: Props): React.JSX.Element {
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>React Native 页面</Text>
      <Text style={styles.msg}>
        {nativeMessage ?? '未收到原生消息'}
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 16 },
  msg: { fontSize: 18 },
});