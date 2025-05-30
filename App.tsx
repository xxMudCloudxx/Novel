import React, { useEffect } from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import HomePage from './src/page/HomePage/HomePage';
import { initializeApp, cleanupApp } from './src/utils/appInit';
import { useUserStore } from './src/page/HomePage/store/userStore';
import { useHomeStore } from './src/page/HomePage/store/homeStore';

export default function App(): React.JSX.Element {
  const userStore = useUserStore();
  const homeStore = useHomeStore();

  useEffect(() => {
    // 初始化应用
    initializeApp();

    // 监听store变化并打印日志
    const userUnsubscribe = useUserStore.subscribe((state) => {
      console.log('[App] 📱 用户状态更新:', {
        uid: state.uid,
        nickname: state.nickname,
        isLoggedIn: state.isLoggedIn
      });
    });

    const homeUnsubscribe = useHomeStore.subscribe((state) => {
      console.log('[App] 🏠 首页状态更新:', {
        booksCount: state.recommendBooks.length,
        loading: state.loading,
        firstBookTitle: state.recommendBooks[0]?.title
      });
    });

    // 清理函数
    return () => {
      cleanupApp();
      userUnsubscribe();
      homeUnsubscribe();
    };
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <HomePage />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
});
