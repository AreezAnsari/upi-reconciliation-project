import { useState, useRef, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Home,
  ChevronDown,
  ChevronRight,
  User,
  LogOut,
  Settings,
} from 'lucide-react';
import Logo from '../Logo';
import { useAuthStore, useAppStore } from '../../../store';
import { MENU_CONFIG } from '../../../config';
import { formatDateTime } from '../../../utils';
import styles from './Navbar.module.css';
import { authAPI } from '../../../services';

// Build nested menu structure from flat API data
// Resolve URL for any menu item — falls back to constructing from processType + menuProcessId
const resolveMenuUrl = (menu) => {
  const pid = menu.menuProcessId;
  const pt = menu.processType?.trim().toUpperCase();

  // Prioritize constructed URL when processType and menuProcessId are present
  if (pid && String(pid).trim() !== '') {
    if (pt === 'EXTRACTION') return `/extraction/fileProcessing.extr?processid=${pid}`;
    if (pt === 'RECONCILIATION') return `/reconciliation/fileProcessing.extr?processid=${pid}`;
  }

  // Fall back to menuUrl from API
  const trimmed = menu.menuUrl?.trim();
  if (trimmed) return trimmed;

  console.warn('[Navbar] resolveMenuUrl returned null for menu:', menu.menuName, { processType: menu.processType, menuProcessId: menu.menuProcessId, menuUrl: menu.menuUrl });
  return null;
};

const buildNestedMenus = (menuData) => {
  if (!menuData || !Array.isArray(menuData)) return [];

  console.log('[Navbar] Raw menu data from API:', menuData);

  // Separate menus by type
  const masterMenus = menuData.filter(m => m.menuType === 'Master');
  const mainMenus = menuData.filter(m => m.menuType === 'Main');
  const subMenus = menuData.filter(m => m.menuType === 'Submenu');

  console.log('[Navbar] Submenu items:', subMenus.map(s => ({
    name: s.menuName, parentMenuCode: s.parentMenuCode, masterMenuParent: s.masterMenuParent,
    processType: s.processType, menuProcessId: s.menuProcessId, menuUrl: s.menuUrl
  })));

  // Build nested structure
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
        // Submenus: match by parentMenuCode = main menu NAME and masterMenuParent = master menu NAME
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

// Static Nav Item Component for MENU_CONFIG menus
const StaticNavItem = ({ menu, isActive }) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className={styles.navItem} ref={dropdownRef}>
      <button
        className={`${styles.navButton} ${isActive ? styles.active : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        {menu.label}
        <ChevronDown
          size={16}
          className={`${styles.chevron} ${isOpen ? styles.rotated : ''}`}
        />
      </button>
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className={styles.dropdown}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
          >
            {menu.items.map((item) => (
              <Link
                key={item.id}
                to={item.path}
                className={styles.dropdownItem}
                onClick={() => setIsOpen(false)}
              >
                {item.label}
              </Link>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

// Dynamic Nav Item Component with submenu support (for API menus)
const DynamicNavItem = ({ menu, isActive }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [openSubmenuId, setOpenSubmenuId] = useState(null);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
        setOpenSubmenuId(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const hasChildren = menu.children && menu.children.length > 0;

  return (
    <div className={styles.navItem} ref={dropdownRef}>
      <button
        className={`${styles.navButton} ${isActive ? styles.active : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        {menu.label}
        {hasChildren && (
          <ChevronDown
            size={16}
            className={`${styles.chevron} ${isOpen ? styles.rotated : ''}`}
          />
        )}
      </button>
      <AnimatePresence>
        {isOpen && hasChildren && (
          <motion.div
            className={styles.dropdown}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
          >
            {menu.children.map((child) => {
              const hasSubChildren = child.children && child.children.length > 0;

              return (
                <div
                  key={child.id}
                  className={styles.dropdownItemWrapper}
                  onMouseEnter={() => hasSubChildren && setOpenSubmenuId(child.id)}
                  onMouseLeave={() => setOpenSubmenuId(null)}
                >
                  {hasSubChildren ? (
                    <>
                      <div className={`${styles.dropdownItem} ${styles.hasSubmenu}`}>
                        {child.label}
                        <ChevronRight size={14} className={styles.submenuArrow} />
                      </div>
                      <AnimatePresence>
                        {openSubmenuId === child.id && (
                          <motion.div
                            className={styles.submenu}
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            transition={{ duration: 0.15 }}
                          >
                            {child.children.map((subChild) => (
                              <Link
                                key={subChild.id}
                                to={subChild.menuUrl || `/menu/${subChild.id}`}
                                className={styles.dropdownItem}
                                onClick={() => {
                                  setIsOpen(false);
                                  setOpenSubmenuId(null);
                                }}
                              >
                                {subChild.label}
                              </Link>
                            ))}
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </>
                  ) : (
                    <Link
                      to={child.menuUrl || `/menu/${child.id}`}
                      className={styles.dropdownItem}
                      onClick={() => setIsOpen(false)}
                    >
                      {child.label}
                    </Link>
                  )}
                </div>
              );
            })}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const Navbar = () => {
  const location = useLocation();
  const { user, token, logout } = useAuthStore();
  const menuRefreshKey = useAppStore((state) => state.menuRefreshKey);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [dynamicMenus, setDynamicMenus] = useState([]);
  const [menusLoading, setMenusLoading] = useState(true);
  const userMenuRef = useRef(null);
  const loginTime = useAuthStore((state) => state.loginTime);

  // Fetch menus based on user's role when logged in
  useEffect(() => {
    const fetchMenus = async () => {
      if (!token || !user?.roleId) {
        setMenusLoading(false);
        return;
      }

      try {
        const response = await authAPI.getMenuByRole(user.roleId, token);
        if (response.status === 'SUCCESS' && response.data) {
          const nestedMenus = buildNestedMenus(response.data);
          setDynamicMenus(nestedMenus);
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

  // Get active static menu
  const getActiveStaticMenu = () => {
    for (const [key, menu] of Object.entries(MENU_CONFIG)) {
      if (menu.items.some(item => location.pathname.startsWith(item.path))) {
        return key;
      }
    }
    return null;
  };

  // Get active dynamic menu
  const getActiveDynamicMenu = () => {
    for (const menu of dynamicMenus) {
      if (menu.children) {
        for (const child of menu.children) {
          if (child.menuUrl && location.pathname.startsWith(child.menuUrl)) {
            return menu.id;
          }
          if (child.children) {
            for (const subChild of child.children) {
              if (subChild.menuUrl && location.pathname.startsWith(subChild.menuUrl)) {
                return menu.id;
              }
            }
          }
        }
      }
    }
    return null;
  };

  const isChecker = user?.roleCode === 'CHECKER';

  // CHECKER: only Admin > User Approval; Others: everything except User Approval
  const filteredStaticMenus = isChecker
    ? {
        admin: {
          ...MENU_CONFIG.admin,
          items: MENU_CONFIG.admin.items.filter(item => item.id === 'user-approval'),
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

  const handleLogout = async () => {
    const refreshToken = useAuthStore.getState().refreshToken;
    try {
      if (refreshToken) {
        await authAPI.logout(refreshToken);
      }
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      logout();
      setUserMenuOpen(false);
    }
  };

  return (
    <nav className={styles.navbar}>
      <div className={styles.left}>
        <Link to="/dashboard" className={styles.logoLink}>
          <Logo size="md" />
        </Link>

        <div className={styles.navItems}>
          {/* Static menus */}
          {Object.entries(filteredStaticMenus).map(([key, menu]) => (
            <StaticNavItem
              key={key}
              menu={menu}
              isActive={activeStaticMenu === key}
            />
          ))}

          {/* Dynamic menus from API (hidden for CHECKER role) */}
          {!isChecker && (
            menusLoading ? (
              <span className={styles.loadingText}>Loading...</span>
            ) : (
              dynamicMenus.map((menu) => (
                <DynamicNavItem
                  key={menu.id}
                  menu={menu}
                  isActive={activeDynamicMenu === menu.id}
                />
              ))
            )
          )}
        </div>
      </div>

      <div className={styles.right}>
        <Link to="/dashboard" className={styles.iconButton} title="Home">
          <Home size={20} />
        </Link>

        <div className={styles.userSection} ref={userMenuRef}>
          <button
            className={styles.userButton}
            onClick={() => setUserMenuOpen(!userMenuOpen)}
          >
            <div className={styles.avatar}>
              {user?.name?.charAt(0) || 'U'}
            </div>
            <div className={styles.userInfo}>
              <span className={styles.userName}>Welcome: {user?.name}</span>
              <span className={styles.userMeta}>
                Login: {formatDateTime(loginTime)}
              </span>
              <span className={styles.userMeta}>
                Role: {user?.role}
              </span>
            </div>
            <ChevronDown
              size={16}
              className={`${styles.chevron} ${userMenuOpen ? styles.rotated : ''}`}
            />
          </button>

          <AnimatePresence>
            {userMenuOpen && (
              <motion.div
                className={styles.userDropdown}
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.15 }}
              >
                <Link to="/profile" className={styles.dropdownItem} onClick={() => setUserMenuOpen(false)}>
                  <User size={16} />
                  Profile
                </Link>
                <Link to="/settings" className={styles.dropdownItem} onClick={() => setUserMenuOpen(false)}>
                  <Settings size={16} />
                  Settings
                </Link>
                <hr className={styles.divider} />
                <button
                  className={styles.dropdownItem}
                  onClick={handleLogout}
                >
                  <LogOut size={16} />
                  Logout
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
