import { DeviceEventEmitter } from 'react-native';
import { useUserStore } from '../store/userStore';
import { useHomeStore } from '../store/homeStore';

export interface NativeUserData {
  uid: string;
  token: string;
  nickname: string;
  photo: string;
  sex?: string;
}

export interface NativeBookData {
  books: {
    id: number;
    title: string;
    author: string;
    description: string;
    coverUrl: string;
    readCount?: number;
    rating?: number;
  }[];
}

class NativeEventListener {
  private userDataSubscription: any | null = null;
  private bookDataSubscription: any | null = null;

  /**
   * 初始化事件监听器
   */
  init() {
    this.setupUserDataListener();
    this.setupBookDataListener();
    console.log('[NativeEventListener] Event listeners initialized');
  }

  /**
   * 监听用户数据事件
   */
  private setupUserDataListener() {
    this.userDataSubscription = DeviceEventEmitter.addListener(
      'onUserDataReceived',
      (data: NativeUserData) => {
        console.log('[RN] 📱 收到用户数据:', data);
        
        // 直接调用Zustand store方法
        useUserStore.getState().handleNativeUserData(data);
      }
    );
  }

  /**
   * 监听推荐书籍数据事件
   */
  private setupBookDataListener() {
    this.bookDataSubscription = DeviceEventEmitter.addListener(
      'onRecommendBooksReceived',
      (data: NativeBookData) => {
        console.log('[RN] 📚 收到推荐书籍数据:', data);
        
        // 更新首页推荐书籍
        if (data.books && data.books.length > 0) {
          useHomeStore.getState().setRecommendBooks(data.books);
        }
      }
    );
  }

  /**
   * 清理事件监听器
   */
  cleanup() {
    if (this.userDataSubscription) {
      this.userDataSubscription.remove();
      this.userDataSubscription = null;
    }

    if (this.bookDataSubscription) {
      this.bookDataSubscription.remove();
      this.bookDataSubscription = null;
    }

    console.log('[NativeEventListener] Event listeners cleaned up');
  }

  /**
   * 手动触发用户数据接收（用于测试）
   */
  static simulateUserData(userData: NativeUserData) {
    DeviceEventEmitter.emit('onUserDataReceived', userData);
  }

  /**
   * 手动触发书籍数据接收（用于测试）
   */
  static simulateBookData(bookData: NativeBookData) {
    DeviceEventEmitter.emit('onRecommendBooksReceived', bookData);
  }
}

// 创建单例实例
export const nativeEventListener = new NativeEventListener();

// 默认导出类本身，方便其他地方使用静态方法
export default NativeEventListener; 