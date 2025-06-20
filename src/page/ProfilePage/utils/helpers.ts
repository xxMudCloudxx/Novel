import { HomeBook } from '../store/BookStore';
import { Book } from '../types';
import { itemHeightCache, screenWidth } from './constants';
import { wp } from '../../../utils/theme/dimensions';

// 将HomeBook转换为Book格式的辅助函数
export const convertHomeBooksToBooks = (homeBooks: HomeBook[]): Book[] => {
  return homeBooks.map(homeBook => ({
    id: homeBook.bookId,
    title: homeBook.bookName,
    author: homeBook.authorName,
    description: homeBook.bookDesc,
    coverUrl: homeBook.picUrl,
    categoryId: homeBook.type,
    readCount: Math.floor(Math.random() * 10000), // 模拟数据
    rating: Math.random() * 5, // 模拟数据
  }));
};

// 格式化阅读数
export const formatReadCount = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
};

// 计算书籍项目高度（瀑布流用）
export const calculateBookItemHeight = (bookId: number): number => {
  if (itemHeightCache.has(bookId)) {
    return itemHeightCache.get(bookId)!;
  }
  const baseHeight = wp(180);
  const variableHeight = (bookId * 17) % wp(60);
  const height = baseHeight + variableHeight;
  itemHeightCache.set(bookId, height);
  return height;
};

// 根据描述长度决定显示行数
export const getDescriptionLines = (description?: string): number => {
  if (!description) {return 1;}
  if (description.length > 80) {return 3;}
  if (description.length > 40) {return 2;}
  return 1;
};

// 计算瀑布流书籍项目宽度
export const getBookItemWidth = (): number => {
  return (screenWidth - wp(45)) / 2;
};
