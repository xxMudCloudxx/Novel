import React, { useEffect } from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import ProfilePage from './src/page/ProfilePage/ProfilePage';
import { initializeApp, cleanupApp } from './src/utils/appInit';
import { useUserStore } from './src/page/ProfilePage/store/userStore';
import { useHomeStore } from './src/page/ProfilePage/store/BookStore';
import { initializeTheme } from './src/utils/theme/themeStore';
import { useNovelColors } from './src/utils/theme/colors';

export default function App(): React.JSX.Element {
  const colors = useNovelColors();

  const styles = StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colors.novelBackground,
    },
  });

  useEffect(() => {
    let themeCleanup: (() => void) | undefined;

    const initializeAsync = async () => {
      try {
    // 初始化应用
    initializeApp();

        // 异步初始化主题并恢复缓存
        themeCleanup = await initializeTheme();
        console.log('[App] 🎨 主题初始化完成');
      } catch (error) {
        console.error('[App] 主题初始化失败:', error);
      }
    };

    // 启动异步初始化
    initializeAsync();

    // 监听store变化并打印日志
    const userUnsubscribe = useUserStore.subscribe((state) => {
      console.log('[App] 📱 用户状态更新:', {
        uid: state.uid,
        nickname: state.nickname,
        isLoggedIn: state.isLoggedIn,
      });
    });

    const homeUnsubscribe = useHomeStore.subscribe((state) => {
      console.log('[App] 🏠 首页状态更新:', {
        booksCount: state.recommendBooks.length,
        loading: state.loading,
        firstBookTitle: state.recommendBooks[0]?.title,
      });
    });

    // 清理函数
    return () => {
      cleanupApp();
      userUnsubscribe();
      homeUnsubscribe();
      if (themeCleanup) {
        themeCleanup();
        console.log('[App] 🎨 主题清理完成');
      }
    };
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <ProfilePage />
    </SafeAreaView>
  );
}
