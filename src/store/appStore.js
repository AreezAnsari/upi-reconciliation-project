import { create } from 'zustand';

const useAppStore = create((set) => ({
  sidebarOpen: true,
  activeMenu: null,
  notifications: [],
  isLoading: false,
  menuRefreshKey: 0,
  
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  setActiveMenu: (menu) => set({ activeMenu: menu }),
  
  addNotification: (notification) => {
    const id = Date.now();
    set((state) => ({
      notifications: [...state.notifications, { ...notification, id }],
    }));
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
      set((state) => ({
        notifications: state.notifications.filter((n) => n.id !== id),
      }));
    }, 5000);
  },
  
  removeNotification: (id) => {
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    }));
  },
  
  setLoading: (loading) => set({ isLoading: loading }),
  triggerMenuRefresh: () => set((state) => ({ menuRefreshKey: state.menuRefreshKey + 1 })),
}));

export { useAppStore };
