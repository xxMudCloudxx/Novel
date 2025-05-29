import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';

export interface Book {
  id: number;
  title: string;
  author: string;
  description: string;
  coverUrl: string;
  width?: number;
  height?: number;
  categoryId?: number;
  readCount?: number;
  rating?: number;
}

export interface HomeState {
  recommendBooks: Book[];
  loading: boolean;
  error: string | null;
  currentPage: number;
  pageSize: number;
  hasMore: boolean;
  
  isRefreshing: boolean;
  isLoadingMore: boolean;
  
  searchQuery: string;
  selectedCategoryId: number | null;
  
  rankBooks: Book[];
  selectedRankType: 'hot' | 'new' | 'recommend';
}

interface HomeActions {
  // 基础状态更新
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setRefreshing: (refreshing: boolean) => void;
  setLoadingMore: (loadingMore: boolean) => void;
  setSearchQuery: (query: string) => void;
  setCategoryFilter: (categoryId: number | null) => void;
  setRankType: (type: 'hot' | 'new' | 'recommend') => void;
  
  // 数据更新
  setRecommendBooks: (books: Book[]) => void;
  appendBooks: (books: Book[]) => void;
  
  // 异步操作
  fetchRecommendBooks: () => Promise<void>;
  refreshBooks: () => Promise<void>;
  loadMoreBooks: () => Promise<void>;
  fetchRankBooks: (type: 'hot' | 'new' | 'recommend') => Promise<void>;
}

type HomeStore = HomeState & HomeActions;

const initialState: HomeState = {
  recommendBooks: [],
  loading: false,
  error: null,
  currentPage: 0,
  pageSize: 20,
  hasMore: true,
  
  isRefreshing: false,
  isLoadingMore: false,
  
  searchQuery: '',
  selectedCategoryId: null,
  
  rankBooks: [],
  selectedRankType: 'hot',
};

export const useHomeStore = create<HomeStore>()(
  immer((set, get) => ({
    ...initialState,
    
    // 基础状态更新
    setLoading: (loading) => set((state) => {
      state.loading = loading;
    }),
    
    setError: (error) => set((state) => {
      state.error = error;
    }),
    
    setRefreshing: (refreshing) => set((state) => {
      state.isRefreshing = refreshing;
    }),
    
    setLoadingMore: (loadingMore) => set((state) => {
      state.isLoadingMore = loadingMore;
    }),
    
    setSearchQuery: (query) => set((state) => {
      state.searchQuery = query;
    }),
    
    setCategoryFilter: (categoryId) => set((state) => {
      state.selectedCategoryId = categoryId;
    }),
    
    setRankType: (type) => set((state) => {
      state.selectedRankType = type;
    }),
    
    // 数据更新
    setRecommendBooks: (books) => set((state) => {
      state.recommendBooks = books;
    }),
    
    appendBooks: (books) => set((state) => {
      state.recommendBooks.push(...books);
    }),
    
    // 异步操作
    fetchRecommendBooks: async () => {
      const state = get();
      set((draft) => {
        draft.loading = true;
        draft.error = null;
      });
      
      try {
        // 这里调用API
        const response = await fetch(`http://47.110.147.60:8080/api/front/home/book?page=1&size=${state.pageSize}`);
        const data = await response.json();
        
        set((draft) => {
          draft.loading = false;
          draft.recommendBooks = data.books || [];
          draft.hasMore = data.hasMore || false;
          draft.currentPage = 1;
        });
      } catch (error) {
        set((draft) => {
          draft.loading = false;
          draft.error = error instanceof Error ? error.message : '获取数据失败';
        });
      }
    },
    
    refreshBooks: async () => {
      const state = get();
      set((draft) => {
        draft.isRefreshing = true;
        draft.error = null;
        draft.currentPage = 0;
      });
      
      try {
        const response = await fetch(`http://47.110.147.60:8080/api/front/home/book?page=1&size=${state.pageSize}`);
        const data = await response.json();
        
        set((draft) => {
          draft.isRefreshing = false;
          draft.recommendBooks = data.books || [];
          draft.hasMore = data.hasMore || false;
          draft.currentPage = 1;
        });
      } catch (error) {
        set((draft) => {
          draft.isRefreshing = false;
          draft.error = error instanceof Error ? error.message : '刷新失败';
        });
      }
    },
    
    loadMoreBooks: async () => {
      const state = get();
      if (!state.hasMore || state.isLoadingMore) return;
      
      set((draft) => {
        draft.isLoadingMore = true;
        draft.error = null;
      });
      
      try {
        const nextPage = state.currentPage + 1;
        const response = await fetch(`http://47.110.147.60:8080/api/front/home/book?page=${nextPage}&size=${state.pageSize}`);
        const data = await response.json();
        
        set((draft) => {
          draft.isLoadingMore = false;
          draft.recommendBooks.push(...(data.books || []));
          draft.hasMore = data.hasMore || false;
          draft.currentPage = nextPage;
        });
      } catch (error) {
        set((draft) => {
          draft.isLoadingMore = false;
          draft.error = error instanceof Error ? error.message : '加载更多失败';
        });
      }
    },
    
    fetchRankBooks: async (type) => {
      set((draft) => {
        draft.loading = true;
        draft.error = null;
        draft.selectedRankType = type;
      });
      
      try {
        const response = await fetch(`http://47.110.147.60:8080/api/front/home/rank/${type}`);
        const data = await response.json();
        
        set((draft) => {
          draft.loading = false;
          draft.rankBooks = data.books || [];
        });
      } catch (error) {
        set((draft) => {
          draft.loading = false;
          draft.error = error instanceof Error ? error.message : '获取排行榜失败';
        });
      }
    },
  }))
); 