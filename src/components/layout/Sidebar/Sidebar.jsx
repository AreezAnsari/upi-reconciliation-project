import { useState, useRef, useEffect, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Home,
  Shield,
  Settings,
  FileText,
  Cog,
  Scale,
  Layers,
  BarChart2,
  ChevronRight,
  User,
  LogOut,
  Loader2,
} from 'lucide-react';
import { useAuthStore, useAppStore } from '../../../store';
import { MENU_CONFIG } from '../../../config';
import { formatDateTime } from '../../../utils';
import { authAPI } from '../../../services';
import styles from './Sidebar.module.css';

// ── Icon map ──
const ICON_MAP = { Shield, Settings, FileText, Cog, Home, Scale, Layers, BarChart2 };
const getIcon = (name) => ICON_MAP[name] || FileText;

// ── Menu building (unchanged business logic) ──
const resolveMenuUrl = (menu) => {
  const pid = menu.menuProcessId;
  const pt = menu.processType?.trim().toUpperCase();
  if (pid && String(pid).trim() !== '') {
    if (pt === 'EXTRACTION') return `/extraction/fileProcessing.extr?processid=${pid}`;
    if (pt === 'RECONCILIATION') return `/reconciliation/fileProcessing.extr?processid=${pid}`;
  }
  const trimmed = menu.menuUrl?.trim();
  if (trimmed) return trimmed;
  return null;
};

const buildNestedMenus = (menuData) => {
  if (!menuData || !Array.isArray(menuData)) return [];
  const masterMenus = menuData.filter(m => m.menuType === 'Master');
  const mainMenus = menuData.filter(m => m.menuType === 'Main');
  const subMenus = menuData.filter(m => m.menuType === 'Submenu');

  return masterMenus.map(master => ({
    id: master.menuId,
    label: master.menuName,
    description: master.menuDescription,
    menuUrl: master.menuUrl?.trim() || null,
    children: mainMenus
      .filter(main => main.parentMenuCode === String(master.menuId))
      .map(main => ({
        id: main.menuId,
        label: main.menuName,
        description: main.menuDescription,
        menuUrl: resolveMenuUrl(main),
        children: subMenus
          .filter(sub =>
            sub.parentMenuCode === main.menuName &&
            sub.masterMenuParent?.trim() === master.menuName
          )
          .map(sub => ({
            id: sub.menuId,
            label: sub.menuName,
            description: sub.menuDescription,
            menuUrl: resolveMenuUrl(sub),
            processType: sub.processType,
          })),
      })),
  }));
};

// ── Tooltip (fixed-pos, only when sidebar is collapsed) ──
const Tooltip = ({ label, anchorRef, show }) => {
  const [pos, setPos] = useState(null);

  useEffect(() => {
    if (!show || !anchorRef.current) { setPos(null); return; }
    const rect = anchorRef.current.getBoundingClientRect();
    setPos({ top: rect.top + rect.height / 2, left: rect.right + 10 });
  }, [show, anchorRef]);

  if (!show || !pos) return null;

  return (
    <span
      className={styles.tooltip}
      style={{ top: pos.top, left: pos.left, transform: 'translateY(-50%)' }}
    >
      {label}
    </span>
  );
};

// ── Grandchild Link ──
const GrandchildItem = ({ item, pathname }) => {
  const url = item.menuUrl || item.path || `/menu/${item.id}`;
  const isActive = pathname.startsWith(url);

  return (
    <li>
      <Link
        to={url}
        className={`${styles.grandchildButton} ${isActive ? styles.active : ''}`}
      >
        <span className={styles.grandchildLabel}>{item.label}</span>
      </Link>
    </li>
  );
};

// ── Child Item (may have grandchildren) ──
const ChildItem = ({ item, pathname }) => {
  const hasGrandchildren = item.children && item.children.length > 0;
  const url = item.menuUrl || item.path || `/menu/${item.id}`;

  // Auto-open if any grandchild is active
  const isGrandchildActive = hasGrandchildren && item.children.some(gc => {
    const gcUrl = gc.menuUrl || gc.path || `/menu/${gc.id}`;
    return pathname.startsWith(gcUrl);
  });
  const [open, setOpen] = useState(isGrandchildActive);
  const isActive = !hasGrandchildren && pathname.startsWith(url);

  // Simple link when no grandchildren
  if (!hasGrandchildren) {
    return (
      <li className={styles.childItem}>
        <Link
          to={url}
          className={`${styles.childButton} ${isActive ? styles.active : ''}`}
        >
          <span className={styles.childLabel}>{item.label}</span>
        </Link>
      </li>
    );
  }

  return (
    <li className={styles.childItem}>
      <button
        className={`${styles.childButton} ${isGrandchildActive ? styles.active : ''}`}
        onClick={() => setOpen(prev => !prev)}
      >
        <span className={styles.childLabel}>{item.label}</span>
        <ChevronRight
          size={12}
          className={`${styles.childChevron} ${open ? styles.open : ''}`}
        />
      </button>
      <ul className={`${styles.grandchildList} ${open ? styles.open : ''}`}>
        {item.children.map(gc => (
          <GrandchildItem key={gc.id} item={gc} pathname={pathname} />
        ))}
      </ul>
    </li>
  );
};

// ── Top-level Nav Item (accordion) ──
const SidebarNavItem = ({ icon: Icon, label, items, isActive, to, pathname }) => {
  const hasItems = items && items.length > 0;
  const [open, setOpen] = useState(isActive);
  const [hovered, setHovered] = useState(false);
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const btnRef = useRef(null);
  const itemRef = useRef(null);

  // Track sidebar hover to hide tooltip
  useEffect(() => {
    const sidebar = itemRef.current?.closest('nav');
    if (!sidebar) return;
    const onEnter = () => setSidebarExpanded(true);
    const onLeave = () => setSidebarExpanded(false);
    sidebar.addEventListener('mouseenter', onEnter);
    sidebar.addEventListener('mouseleave', onLeave);
    return () => {
      sidebar.removeEventListener('mouseenter', onEnter);
      sidebar.removeEventListener('mouseleave', onLeave);
    };
  }, []);

  const showTooltip = hovered && !sidebarExpanded;

  // Simple link (no children)
  if (to && !hasItems) {
    return (
      <li
        ref={itemRef}
        className={styles.navItem}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        <Link
          ref={btnRef}
          to={to}
          className={`${styles.navItemButton} ${isActive ? styles.active : ''}`}
          aria-label={label}
        >
          <span className={styles.navIcon}><Icon size={20} /></span>
          <span className={styles.navLabel}>{label}</span>
        </Link>
        <Tooltip label={label} anchorRef={btnRef} show={showTooltip} />
      </li>
    );
  }

  return (
    <li
      ref={itemRef}
      className={styles.navItem}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <button
        ref={btnRef}
        className={`${styles.navItemButton} ${isActive ? styles.active : ''}`}
        onClick={() => setOpen(prev => !prev)}
        aria-label={label}
        aria-expanded={open}
      >
        <span className={styles.navIcon}><Icon size={20} /></span>
        <span className={styles.navLabel}>{label}</span>
        {hasItems && (
          <ChevronRight
            size={14}
            className={`${styles.chevron} ${open ? styles.open : ''}`}
          />
        )}
      </button>
      <Tooltip label={label} anchorRef={btnRef} show={showTooltip} />

      {hasItems && (
        <ul className={`${styles.childList} ${open ? styles.open : ''}`}>
          {items.map(child => (
            <ChildItem key={child.id} item={child} pathname={pathname} />
          ))}
        </ul>
      )}
    </li>
  );
};

// ── Wrapper to inject pathname ──
const NavItem = (props) => {
  const location = useLocation();
  return <SidebarNavItem {...props} pathname={location.pathname} />;
};

// ── Main Sidebar ──
const Sidebar = () => {
  const location = useLocation();
  const { user, token, logout } = useAuthStore();
  const menuRefreshKey = useAppStore((state) => state.menuRefreshKey);
  const loginTime = useAuthStore((state) => state.loginTime);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [dynamicMenus, setDynamicMenus] = useState([]);
  const [menusLoading, setMenusLoading] = useState(true);
  const [pinned, setPinned] = useState(false);
  const userMenuRef = useRef(null);

  useEffect(() => {
    const fetchMenus = async () => {
      if (!token || !user?.roleId) { setMenusLoading(false); return; }
      try {
        const response = await authAPI.getMenuByRole(user.roleId, token);
        if (response.status === 'SUCCESS' && response.data) {
          setDynamicMenus(buildNestedMenus(response.data));
        }
      } catch (error) {
        console.error('Failed to fetch menus:', error);
      } finally {
        setMenusLoading(false);
      }
    };
    fetchMenus();
  }, [token, user?.roleId, menuRefreshKey]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setUserMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Active-state helpers
  const getActiveStaticMenu = useCallback(() => {
    for (const [key, menu] of Object.entries(MENU_CONFIG)) {
      if (menu.items.some(item => location.pathname.startsWith(item.path))) return key;
    }
    return null;
  }, [location.pathname]);

  const getActiveDynamicMenu = useCallback(() => {
    for (const menu of dynamicMenus) {
      if (menu.children) {
        for (const child of menu.children) {
          if (child.menuUrl && location.pathname.startsWith(child.menuUrl)) return menu.id;
          if (child.children) {
            for (const sub of child.children) {
              if (sub.menuUrl && location.pathname.startsWith(sub.menuUrl)) return menu.id;
            }
          }
        }
      }
    }
    return null;
  }, [location.pathname, dynamicMenus]);

  const isChecker = user?.roleCode === 'CHECKER';

  const filteredStaticMenus = isChecker
    ? {
        admin: {
          ...MENU_CONFIG.admin,
          items: MENU_CONFIG.admin.items.filter(item => item.id === 'user-approval'),
        },
        dispute: {
          ...MENU_CONFIG.dispute,
          items: MENU_CONFIG.dispute.items.filter(item => item.id === 'checker-approval'),
        },
      }
    : Object.fromEntries(
        Object.entries(MENU_CONFIG).map(([key, menu]) => [
          key,
          key === 'admin'
            ? { ...menu, items: menu.items.filter(item => item.id !== 'user-approval') }
            : menu,
        ])
      );

  const activeStaticMenu = getActiveStaticMenu();
  const activeDynamicMenu = getActiveDynamicMenu();
  const isDashboardActive = location.pathname === '/dashboard' || location.pathname === '/';

  const handleLogout = async () => {
    const refreshToken = useAuthStore.getState().refreshToken;
    try {
      if (refreshToken) await authAPI.logout(refreshToken);
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      logout();
      setUserMenuOpen(false);
    }
  };

  return (
    <nav
      className={styles.sidebar}
      data-expanded={pinned || undefined}
      aria-label="Main navigation"
    >
      {/* Logo */}
      <div className={styles.logoSection}>
        <Link to="/dashboard" className={styles.logoLink}>
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" width="36" height="36" style={{ flexShrink: 0 }}>
            <rect width="40" height="40" rx="10" fill="url(#sidebar-logo-gradient)" />
            <path d="M12 20C12 15.5817 15.5817 12 20 12V12C24.4183 12 28 15.5817 28 20V20C28 24.4183 24.4183 28 20 28V28" stroke="white" strokeWidth="3" strokeLinecap="round" />
            <path d="M20 16L24 20L20 24" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            <circle cx="16" cy="20" r="2" fill="white" />
            <defs>
              <linearGradient id="sidebar-logo-gradient" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                <stop stopColor="#17a398" />
                <stop offset="1" stopColor="#0d6963" />
              </linearGradient>
            </defs>
          </svg>
          <span className={styles.logoText}>
            Kal<span className={styles.logoHighlight}> Infotech</span>
          </span>
        </Link>
      </div>

      {/* Navigation */}
      <div className={styles.navSection}>
        <ul className={styles.navList}>
          <NavItem
            icon={Home}
            label="Dashboard"
            to="/dashboard"
            isActive={isDashboardActive}
          />

          <hr className={styles.divider} />

          {Object.entries(filteredStaticMenus).map(([key, menu]) => (
            <NavItem
              key={key}
              icon={getIcon(menu.icon)}
              label={menu.label}
              items={menu.items}
              isActive={activeStaticMenu === key}
            />
          ))}

          {!isChecker && (
            <>
              {menusLoading ? (
                <li className={styles.loadingItem}>
                  <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
                </li>
              ) : (
                dynamicMenus.map(menu => (
                  <NavItem
                    key={menu.id}
                    icon={FileText}
                    label={menu.label}
                    items={menu.children}
                    isActive={activeDynamicMenu === menu.id}
                  />
                ))
              )}
            </>
          )}
        </ul>
      </div>

      {/* User */}
      <div className={styles.bottomSection} ref={userMenuRef}>
        <button
          className={styles.userButton}
          onClick={() => setUserMenuOpen(prev => !prev)}
          aria-label={`User menu for ${user?.name || 'User'}`}
        >
          <div className={styles.avatar}>
            {user?.name?.charAt(0) || 'U'}
          </div>
          <div className={styles.userInfo}>
            <span className={styles.userName}>{user?.name || 'User'}</span>
            <span className={styles.userRole}>{user?.role || ''}</span>
          </div>
        </button>

        <AnimatePresence>
          {userMenuOpen && (
            <motion.div
              className={styles.userDropdown}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 8 }}
              transition={{ duration: 0.15 }}
            >
              <div className={styles.userDropdownHeader}>
                <div className={styles.userDropdownName}>Welcome: {user?.name}</div>
                <div className={styles.userDropdownMeta}>Login: {formatDateTime(loginTime)}</div>
                <div className={styles.userDropdownMeta}>Role: {user?.role}</div>
              </div>
              <Link to="/profile" className={styles.userDropdownItem} onClick={() => setUserMenuOpen(false)}>
                <User size={16} /> Profile
              </Link>
              <Link to="/settings" className={styles.userDropdownItem} onClick={() => setUserMenuOpen(false)}>
                <Settings size={16} /> Settings
              </Link>
              <hr className={styles.userDropdownDivider} />
              <button className={styles.userDropdownItem} onClick={handleLogout}>
                <LogOut size={16} /> Logout
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </nav>
  );
};

export default Sidebar;
