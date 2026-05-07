import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, X, Clock, Users, Search, ShieldCheck, UserX } from 'lucide-react';
import { Button, Card, PageHeader } from '../../../components/common';
import { formatDateTime } from '../../../utils';
import { useAppStore, useAuthStore } from '../../../store';
import { authAPI } from '../../../services/api';
import styles from './UserApproval.module.css';

const TABS = [
  { id: 'pending', label: 'Pending Approvals', icon: Clock },
  { id: 'approved', label: 'Approved Users', icon: ShieldCheck },
];

const StatChip = ({ icon: Icon, label, value, color }) => (
  <div className={`${styles.statChip} ${styles[color]}`}>
    <div className={styles.statChipIcon}><Icon size={18} /></div>
    <div>
      <div className={styles.statChipValue}>{value}</div>
      <div className={styles.statChipLabel}>{label}</div>
    </div>
  </div>
);

const UserApproval = () => {
  const { addNotification } = useAppStore();
  const { token } = useAuthStore();

  const [pendingUsers, setPendingUsers] = useState([]);
  const [activeUsers, setActiveUsers] = useState([]);
  const [loadingPending, setLoadingPending] = useState(true);
  const [loadingActive, setLoadingActive] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('pending');

  useEffect(() => {
    fetchPendingUsers();
    fetchActiveUsers();
  }, []);

  const fetchPendingUsers = async () => {
    setLoadingPending(true);
    try {
      const res = await authAPI.getApprovedUsers('N', token);
      if (res.status === 'SUCCESS') setPendingUsers(res.data || []);
    } catch {
      // Error already reflected in UI (empty state)
    } finally {
      setLoadingPending(false);
    }
  };

  const fetchActiveUsers = async () => {
    setLoadingActive(true);
    try {
      const res = await authAPI.getApprovedUsers('Y', token);
      if (res.status === 'SUCCESS') setActiveUsers(res.data || []);
    } catch {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch active users.' });
    } finally {
      setLoadingActive(false);
    }
  };

  const handleApproveReject = async (user, approved) => {
    setActionLoading(user.userId);
    try {
      const payload = {
        userId: String(user.userId),
        institution: user.institution || '',
        designation: user.designation || '',
        emailId: user.emailId || '',
        type: user.type || '',
        userStatus: user.userStatus || '',
        roleName: user.role?.roleName || '',
        roleId: String(user.role?.roleId || ''),
        userName: user.userName || '',
        userPassword: user.userPassword || '',
        mobileNumber: String(user.mobileNumber || ''),
        createdBy: user.createdBy || '',
        updatedAt: new Date().toISOString(),
        updatedBy: 'system',
        approvedYn: approved ? 'Y' : 'N',
        approvedBy: 'system',
      };
      const res = await authAPI.approveRejectUser(payload, token);
      if (res.status === 'SUCCESS') {
        addNotification({
          type: approved ? 'success' : 'info',
          title: approved ? 'User Approved' : 'User Rejected',
          message: `${user.userName} has been ${approved ? 'approved' : 'rejected'} successfully.`,
        });
        // Small delay to allow backend to commit before re-fetching
        await new Promise((r) => setTimeout(r, 500));
        await fetchPendingUsers();
        await fetchActiveUsers();
      } else {
        addNotification({ type: 'error', title: 'Error', message: res.statusMsg || 'Operation failed.' });
      }
    } catch {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to process request.' });
    } finally {
      setActionLoading(null);
    }
  };

  const filteredActive = activeUsers.filter(u => {
    const term = searchTerm.toLowerCase();
    return (
      (u.userName || '').toLowerCase().includes(term) ||
      (u.emailId || '').toLowerCase().includes(term) ||
      (u.institution || '').toLowerCase().includes(term) ||
      (u.role?.roleName || '').toLowerCase().includes(term)
    );
  });

  const isLoading = activeTab === 'pending' ? loadingPending : loadingActive;

  return (
    <div className={styles.page}>
      <PageHeader
        title="User Approval"
        description="Review and manage user registration requests"
      />

      {/* Stats Row */}
      <motion.div
        className={styles.statsRow}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <StatChip icon={Clock} label="Pending" value={pendingUsers.length} color="chipWarning" />
        <StatChip icon={ShieldCheck} label="Approved" value={activeUsers.length} color="chipSuccess" />
        <StatChip icon={Users} label="Total Users" value={pendingUsers.length + activeUsers.length} color="chipInfo" />
      </motion.div>

      {/* Tab Switcher */}
      <motion.div
        className={styles.tabBar}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
      >
        {TABS.map(tab => {
          const Icon = tab.icon;
          const count = tab.id === 'pending' ? pendingUsers.length : activeUsers.length;
          return (
            <button
              key={tab.id}
              className={`${styles.tab} ${activeTab === tab.id ? styles.tabActive : ''}`}
              onClick={() => { setActiveTab(tab.id); setSearchTerm(''); }}
            >
              <Icon size={16} />
              {tab.label}
              <span className={`${styles.tabBadge} ${activeTab === tab.id ? styles.tabBadgeActive : ''}`}>
                {count}
              </span>
            </button>
          );
        })}
      </motion.div>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        {activeTab === 'pending' && (
          <motion.div
            key="pending"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
          >
            {loadingPending ? (
              <div className={styles.loadingState}>
                <div className={styles.spinner} />
                <span>Loading pending users…</span>
              </div>
            ) : pendingUsers.length === 0 ? (
              <div className={styles.emptyState}>
                <div className={styles.emptyIcon}><ShieldCheck size={36} /></div>
                <h3>All caught up!</h3>
                <p>There are no pending approval requests at the moment.</p>
              </div>
            ) : (
              <Card className={styles.tableCard}>
                <div className={styles.table}>
                  <div className={`${styles.tableHead} ${styles.pendingHead}`}>
                    <span>User</span>
                    <span>Email</span>
                    <span>Role</span>
                    <span>Institution</span>
                    <span>Designation</span>
                    <span>Registered</span>
                    <span>Actions</span>
                  </div>
                  <div className={styles.tableBody}>
                    {pendingUsers.map((user, i) => (
                      <motion.div
                        key={user.userId}
                        className={`${styles.tableRow} ${styles.pendingRow}`}
                        initial={{ opacity: 0, x: -8 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: i * 0.04 }}
                      >
                        <span className={styles.nameCell}>
                          <div className={styles.avatarWrap}>
                            <div className={styles.miniAvatar}>
                              {(user.userName || 'U').charAt(0).toUpperCase()}
                            </div>
                            <span className={styles.pendingDot} />
                          </div>
                          <span>{user.userName}</span>
                        </span>
                        <span className={styles.emailCell}>{user.emailId || '—'}</span>
                        <span>
                          {user.role?.roleName
                            ? <span className={styles.rolePill}>{user.role.roleName}</span>
                            : '—'}
                        </span>
                        <span>{user.institution || '—'}</span>
                        <span>{user.designation || '—'}</span>
                        <span className={styles.dateCell}>
                          {user.createdAt ? formatDateTime(user.createdAt) : '—'}
                        </span>
                        <span className={styles.actionCell}>
                          <Button
                            variant="danger"
                            size="sm"
                            leftIcon={<X size={14} />}
                            loading={actionLoading === user.userId}
                            onClick={() => handleApproveReject(user, false)}
                          >
                            Reject
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            leftIcon={<Check size={14} />}
                            loading={actionLoading === user.userId}
                            onClick={() => handleApproveReject(user, true)}
                          >
                            Approve
                          </Button>
                        </span>
                      </motion.div>
                    ))}
                  </div>
                </div>
              </Card>
            )}
          </motion.div>
        )}

        {activeTab === 'approved' && (
          <motion.div
            key="approved"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
          >
            {loadingActive ? (
              <div className={styles.loadingState}>
                <div className={styles.spinner} />
                <span>Loading approved users…</span>
              </div>
            ) : (
              <Card className={styles.tableCard}>
                <div className={styles.tableToolbar}>
                  <div className={styles.searchBox}>
                    <Search size={15} className={styles.searchIcon} />
                    <input
                      type="text"
                      placeholder="Search by name, email, institution or role…"
                      value={searchTerm}
                      onChange={e => setSearchTerm(e.target.value)}
                    />
                    {searchTerm && (
                      <button className={styles.clearSearch} onClick={() => setSearchTerm('')}>
                        <X size={13} />
                      </button>
                    )}
                  </div>
                  <span className={styles.resultCount}>
                    {filteredActive.length} of {activeUsers.length} users
                  </span>
                </div>

                {filteredActive.length === 0 ? (
                  <div className={styles.emptyState}>
                    <div className={styles.emptyIcon}><UserX size={36} /></div>
                    <h3>No users found</h3>
                    <p>Try adjusting your search term.</p>
                  </div>
                ) : (
                  <div className={styles.table}>
                    <div className={styles.tableHead}>
                      <span>User</span>
                      <span>Email</span>
                      <span>Institution</span>
                      <span>Role</span>
                      <span>Type</span>
                      <span>Status</span>
                    </div>
                    <div className={styles.tableBody}>
                      {filteredActive.map((user, i) => (
                        <motion.div
                          key={user.userId}
                          className={styles.tableRow}
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          transition={{ delay: i * 0.03 }}
                        >
                          <span className={styles.nameCell}>
                            <div className={styles.miniAvatar}>
                              {(user.userName || 'U').charAt(0).toUpperCase()}
                            </div>
                            <span>{user.userName}</span>
                          </span>
                          <span className={styles.emailCell}>{user.emailId}</span>
                          <span>{user.institution || '—'}</span>
                          <span>{user.role?.roleName || '—'}</span>
                          <span>{user.type || '—'}</span>
                          <span>
                            <span className={styles.statusBadge}>{user.userStatus || 'Active'}</span>
                          </span>
                        </motion.div>
                      ))}
                    </div>
                  </div>
                )}
              </Card>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default UserApproval;
