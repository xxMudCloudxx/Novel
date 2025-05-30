export interface IconData {
  id: string;
  name: string;
  icon: string;
  onPress: () => void;
}

export interface BookItemProps {
  book: any;
  onPress?: () => void;
  index: number;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  description: string;
  coverUrl: string;
  categoryId: number;
  readCount: number;
  rating: number;
}

export interface LoadMoreIndicatorProps {
  loading: boolean;
  hasMore: boolean;
}

export interface RefreshIndicatorState {
  isPullingDown: boolean;
  isRefreshing: boolean;
  pullDistance: number;
  threshold: number;
} 