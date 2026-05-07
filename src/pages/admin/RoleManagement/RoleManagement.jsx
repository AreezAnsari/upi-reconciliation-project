import { useState, useEffect, useMemo, useCallback } from 'react';
import { motion } from 'framer-motion';
import { Plus, Eye, ArrowLeft, Loader2, Search, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Button, Card } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './RoleManagement.module.css';

// ─── Helper: build menu tree from flat list ───
const buildMenuTree = (menus) => {
  if (!menus || menus.length === 0) return [];

  const masters = menus.filter((m) => m.menuType === 'Master');
  const mains = menus.filter((m) => m.menuType === 'Main');
  const subs = menus.filter((m) => m.menuType === 'Submenu');

  return masters.map((master) => {
    const children = mains
      .filter((main) => String(main.parentMenuCode) === String(master.menuId))
      .map((main) => {
        const subChildren = subs.filter(
          (sub) =>
            sub.parentMenuCode === main.menuName &&
            sub.masterMenuParent === master.menuName
        );
        return { ...main, children: subChildren };
      });
    return { ...master, children };
  });
};

// ─── Helper: get all descendant menuIds from a tree node ───
const getDescendantIds = (node) => {
  const ids = [];
  (node.children || []).forEach((child) => {
    ids.push(child.menuId);
    (child.children || []).forEach((sub) => ids.push(sub.menuId));
  });
  return ids;
};

const getMainDescendantIds = (mainNode) => {
  return (mainNode.children || []).map((sub) => sub.menuId);
};

const RoleManagement = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewData, setViewData] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Table controls
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Create form state
  const [allMenus, setAllMenus] = useState([]);
  const [menuTree, setMenuTree] = useState([]);
  const [selectedMenuIds, setSelectedMenuIds] = useState(new Set());
  const [formData, setFormData] = useState({ roleName: '', roleCode: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (token) fetchRoles();
  }, [token]);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const res = await authAPI.getAllRoles(token);
      const data = res?.status === 'SUCCESS' ? res.data : res?.data || res || [];
      setRoles(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to fetch roles:', error);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch roles.' });
    } finally {
      setLoading(false);
    }
  };

  const fetchAllMenus = async () => {
    try {
      const res = await authAPI.getAllMenus(token);
      const data = res?.status === 'SUCCESS' ? res.data : res?.data || res || [];
      const menus = Array.isArray(data) ? data : [];
      setAllMenus(menus);
      setMenuTree(buildMenuTree(menus));
    } catch (error) {
      console.error('Failed to fetch menus:', error);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch menus.' });
    }
  };

  // ─── Filter & Sort ───
  const filteredRoles = useMemo(() => {
    if (!searchTerm) return roles;
    const term = searchTerm.toLowerCase();
    return roles.filter(
      (r) =>
        r.roleName?.toLowerCase().includes(term) ||
        r.roleCode?.toLowerCase().includes(term)
    );
  }, [roles, searchTerm]);

  const sortedRoles = useMemo(() => {
    if (!sortConfig.key) return filteredRoles;
    return [...filteredRoles].sort((a, b) => {
      let aVal = a[sortConfig.key] ?? '';
      let bVal = b[sortConfig.key] ?? '';
      if (sortConfig.key === 'menuCount') {
        aVal = a.menu?.length || 0;
        bVal = b.menu?.length || 0;
      }
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredRoles, sortConfig]);

  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  const getSortIcon = (key) => {
    if (sortConfig.key !== key) return <ArrowUpDown size={14} className={styles.sortIconInactive} />;
    return sortConfig.direction === 'asc'
      ? <ArrowUp size={14} className={styles.sortIconActive} />
      : <ArrowDown size={14} className={styles.sortIconActive} />;
  };

  const handleBack = () => {
    setViewData(null);
    setShowCreateForm(false);
  };

  const handleView = (role) => {
    setViewData(role);
  };

  const handleCreate = () => {
    setFormData({ roleName: '', roleCode: '' });
    setSelectedMenuIds(new Set());
    setShowCreateForm(true);
    fetchAllMenus();
  };

  // ─── Checkbox tree logic ───
  const toggleMenu = useCallback(
    (menuId, level, treeNode, parentMain, parentMaster) => {
      setSelectedMenuIds((prev) => {
        const next = new Set(prev);
        const isSelected = next.has(menuId);

        if (level === 'master') {
          // Toggle master + all descendants
          const descendantIds = getDescendantIds(treeNode);
          if (isSelected) {
            next.delete(menuId);
            descendantIds.forEach((id) => next.delete(id));
          } else {
            next.add(menuId);
            descendantIds.forEach((id) => next.add(id));
          }
        } else if (level === 'main') {
          // Toggle main + all sub-children
          const subIds = getMainDescendantIds(treeNode);
          if (isSelected) {
            next.delete(menuId);
            subIds.forEach((id) => next.delete(id));
            // If no siblings selected, deselect parent master
            if (parentMaster) {
              const siblingMains = parentMaster.children || [];
              const anyMainSelected = siblingMains.some(
                (m) => m.menuId !== menuId && next.has(m.menuId)
              );
              if (!anyMainSelected) next.delete(parentMaster.menuId);
            }
          } else {
            next.add(menuId);
            subIds.forEach((id) => next.add(id));
            // Auto-select parent master
            if (parentMaster) next.add(parentMaster.menuId);
          }
        } else if (level === 'sub') {
          if (isSelected) {
            next.delete(menuId);
            // If no sibling subs selected, deselect parent main
            if (parentMain) {
              const siblingSubs = parentMain.children || [];
              const anySubSelected = siblingSubs.some(
                (s) => s.menuId !== menuId && next.has(s.menuId)
              );
              if (!anySubSelected) {
                next.delete(parentMain.menuId);
                // Check if any sibling mains still selected for master
                if (parentMaster) {
                  const siblingMains = parentMaster.children || [];
                  const anyMainSelected = siblingMains.some(
                    (m) => m.menuId !== parentMain.menuId && next.has(m.menuId)
                  );
                  if (!anyMainSelected) next.delete(parentMaster.menuId);
                }
              }
            }
          } else {
            next.add(menuId);
            // Auto-select parent main and master
            if (parentMain) next.add(parentMain.menuId);
            if (parentMaster) next.add(parentMaster.menuId);
          }
        }

        return next;
      });
    },
    []
  );

  const handleSubmit = async () => {
    if (!formData.roleName.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Role Name is required.' });
      return;
    }
    if (!formData.roleCode.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Role Code is required.' });
      return;
    }
    if (selectedMenuIds.size === 0) {
      addNotification({ type: 'error', title: 'Validation', message: 'Select at least one menu.' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        menuIds: Array.from(selectedMenuIds),
      };
      await authAPI.createRole(payload, token);
      addNotification({ type: 'success', title: 'Success', message: 'Role created successfully.' });
      setShowCreateForm(false);
      fetchRoles();
    } catch (error) {
      console.error('Failed to create role:', error);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to create role. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  // ─── Create Role Form ───
  if (showCreateForm) {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBack} className={styles.backBtn}>
            Back to Role Management
          </Button>
          <h1>Create Role</h1>
          <p>Define a new role and assign menus</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          {/* Role Details */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>Role Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Role Name</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.roleName}
                  onChange={(e) => setFormData((prev) => ({ ...prev, roleName: e.target.value }))}
                  placeholder="Enter role name"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Role Code</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.roleCode}
                  onChange={(e) => setFormData((prev) => ({ ...prev, roleCode: e.target.value.toUpperCase() }))}
                  placeholder="Enter role code"
                />
              </div>
            </div>
          </div>

          {/* Menu Selection Tree */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>
              Assign Menus ({selectedMenuIds.size} selected)
            </div>
            <div className={styles.menuTreeScroll}>
              {menuTree.length === 0 ? (
                <div className={styles.loadingOverlay}>
                  <Loader2 size={20} className={styles.spinner} />
                  <span>Loading menus...</span>
                </div>
              ) : (
                menuTree.map((master) => (
                  <div key={master.menuId} className={styles.treeNode}>
                    {/* Master */}
                    <div className={`${styles.treeMaster}`}>
                      <label className={`${styles.treeLabel} ${styles.treeMasterLabel}`}>
                        <input
                          type="checkbox"
                          className={styles.treeCheckbox}
                          checked={selectedMenuIds.has(master.menuId)}
                          onChange={() => toggleMenu(master.menuId, 'master', master)}
                        />
                        {master.menuName}
                      </label>
                    </div>
                    {/* Main children */}
                    {(master.children || []).map((main) => (
                      <div key={main.menuId}>
                        <div className={styles.treeMain}>
                          <label className={`${styles.treeLabel} ${styles.treeMainLabel}`}>
                            <input
                              type="checkbox"
                              className={styles.treeCheckbox}
                              checked={selectedMenuIds.has(main.menuId)}
                              onChange={() => toggleMenu(main.menuId, 'main', main, null, master)}
                            />
                            {main.menuName}
                          </label>
                        </div>
                        {/* Submenu children */}
                        {(main.children || []).map((sub) => (
                          <div key={sub.menuId} className={styles.treeSub}>
                            <label className={`${styles.treeLabel} ${styles.treeSubLabel}`}>
                              <input
                                type="checkbox"
                                className={styles.treeCheckbox}
                                checked={selectedMenuIds.has(sub.menuId)}
                                onChange={() => toggleMenu(sub.menuId, 'sub', sub, main, master)}
                              />
                              {sub.menuName}
                            </label>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Actions */}
          <div className={styles.formActions}>
            <Button variant="secondary" onClick={handleBack}>
              Cancel
            </Button>
            <Button variant="gold" onClick={handleSubmit} disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 size={16} className={styles.spinner} />
                  Creating...
                </>
              ) : (
                <>
                  <Plus size={16} />
                  Create Role
                </>
              )}
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ─── View Role Page ───
  if (viewData) {
    const roleMenuTree = buildMenuTree(viewData.menu || []);
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBack} className={styles.backBtn}>
            Back to Role Management
          </Button>
          <h1>View Role</h1>
          <p>{viewData.roleName}</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          {/* Role Details */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>Role Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Role Name</span>
                <span className={styles.viewValue}>{viewData.roleName || '—'}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Role Code</span>
                <span className={styles.viewValue}>{viewData.roleCode || '—'}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Assigned Menus</span>
                <span className={styles.viewValue}>{viewData.menu?.length || 0}</span>
              </div>
            </div>
          </div>

          {/* Menu Tree */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>Assigned Menu Tree</div>
            <div className={styles.menuTreeContainer}>
              {roleMenuTree.length === 0 ? (
                <p style={{ color: 'var(--color-neutral-500)', fontStyle: 'italic', fontSize: 'var(--text-sm)' }}>
                  No menus assigned to this role.
                </p>
              ) : (
                roleMenuTree.map((master) => (
                  <div key={master.menuId} className={styles.treeNode}>
                    <div className={`${styles.treeMaster} ${styles.viewTreeNode}`}>
                      <span className={`${styles.viewTreeDot} ${styles.viewTreeDotMaster}`} />
                      <span className={`${styles.viewTreeText} ${styles.viewTreeMasterText}`}>{master.menuName}</span>
                    </div>
                    {(master.children || []).map((main) => (
                      <div key={main.menuId}>
                        <div className={`${styles.treeMain} ${styles.viewTreeNode}`}>
                          <span className={`${styles.viewTreeDot} ${styles.viewTreeDotMain}`} />
                          <span className={`${styles.viewTreeText} ${styles.viewTreeMainText}`}>{main.menuName}</span>
                        </div>
                        {(main.children || []).map((sub) => (
                          <div key={sub.menuId} className={`${styles.treeSub} ${styles.viewTreeNode}`}>
                            <span className={`${styles.viewTreeDot} ${styles.viewTreeDotSub}`} />
                            <span className={styles.viewTreeText}>{sub.menuName}</span>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                ))
              )}
            </div>
          </div>

          <div className={styles.viewActions}>
            <Button variant="secondary" onClick={handleBack}>
              Back
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ─── List Page ───
  return (
    <div className={styles.page}>
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
        <h1>Role Management</h1>
        <p>Manage roles and their menu assignments</p>
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <Card className={styles.tableCard}>
          {/* Controls */}
          <div className={styles.tableControls}>
            <div className={styles.showEntries}>
              <span>Showing {filteredRoles.length} of {roles.length} entries</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search roles..."
                />
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleCreate}>
                Create Role
              </Button>
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className={styles.loadingOverlay}>
              <Loader2 size={24} className={styles.spinner} />
              <span>Loading roles...</span>
            </div>
          ) : (
            <div className={styles.tableContainer}>
              <table className={styles.mainTable}>
                <thead>
                  <tr>
                    <th className={styles.sortable} onClick={() => handleSort('roleName')}>
                      <div className={styles.thContent}>
                        <span>Role Name</span>
                        {getSortIcon('roleName')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('roleCode')}>
                      <div className={styles.thContent}>
                        <span>Role Code</span>
                        {getSortIcon('roleCode')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('menuCount')}>
                      <div className={styles.thContent}>
                        <span>Assigned Menus</span>
                        {getSortIcon('menuCount')}
                      </div>
                    </th>
                    <th className={styles.actionCol}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedRoles.length === 0 ? (
                    <tr>
                      <td colSpan={4} className={styles.emptyCell}>
                        No roles found
                      </td>
                    </tr>
                  ) : (
                    sortedRoles.map((role) => (
                      <tr key={role.roleId} onClick={() => handleView(role)}>
                        <td>{role.roleName || '—'}</td>
                        <td>{role.roleCode || '—'}</td>
                        <td>
                          <span className={styles.menuCountBadge}>{role.menu?.length || 0}</span>
                        </td>
                        <td className={styles.actionCol} onClick={(e) => e.stopPropagation()}>
                          <div className={styles.actionBtns}>
                            <button
                              className={`${styles.iconBtn} ${styles.iconBtnView}`}
                              title="View"
                              onClick={() => handleView(role)}
                            >
                              <Eye size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </motion.div>
    </div>
  );
};

export default RoleManagement;
