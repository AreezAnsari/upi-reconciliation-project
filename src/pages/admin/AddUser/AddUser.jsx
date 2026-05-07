import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { UserPlus, Mail, User, Lock, Building, Phone, Search, Loader, ArrowLeft, Briefcase } from 'lucide-react';
import { Button, Input, Select, Card } from '../../../components/common';
import { useAppStore, useAuthStore } from '../../../store';
import { authAPI } from '../../../services/api';
import { formatDate, formatDateTime } from '../../../utils';
import styles from './AddUser.module.css';

const AddUser = () => {
  const { addNotification } = useAppStore();
  const { token } = useAuthStore();

  // View state: 'table' or 'form'
  const [view, setView] = useState('table');

  // Table state
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [tableLoading, setTableLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Form state
  const [formData, setFormData] = useState({
    institution: '',
    designation: '',
    emailId: '',
    type: 'INTERNAL',
    userStatus: 'INACTIVE',
    roleId: '',
    roleName: '',
    userName: '',
    userPassword: '',
    confirmPassword: '',
    mobileNumber: '',
    createdBy: 'system',
    approvedYn: 'N',
  });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  // Fetch users and roles on mount
  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const fetchUsers = async () => {
    setTableLoading(true);
    try {
      const res = await authAPI.getAllUsers(token);
      if (res.status === 'SUCCESS') {
        setUsers(res.data || []);
      }
    } catch (err) {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch users.' });
    } finally {
      setTableLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const res = await authAPI.getAllRoles(token);
      if (res.status === 'SUCCESS') {
        setRoles(res.data || []);
      }
    } catch (err) {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch roles.' });
    }
  };

  const roleOptions = roles.map(r => ({ label: r.roleName, value: String(r.roleId) }));
  const typeOptions = [
    { label: 'INTERNAL', value: 'INTERNAL' },
    { label: 'EXTERNAL', value: 'EXTERNAL' },
  ];

  const validateForm = () => {
    const newErrors = {};
    if (!formData.userName.trim()) newErrors.userName = 'Username is required';
    if (!formData.emailId.trim()) newErrors.emailId = 'Email is required';
    if (!formData.userPassword) newErrors.userPassword = 'Password is required';
    if (formData.userPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    if (!formData.roleId) newErrors.roleId = 'Role is required';
    if (formData.mobileNumber && !/^[6-9]\d{9}$/.test(formData.mobileNumber)) {
      newErrors.mobileNumber = 'Enter a valid 10-digit mobile number starting with 6-9';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setLoading(true);
    try {
      const { confirmPassword, ...payload } = formData;
      payload.roleId = Number(payload.roleId);
      payload.mobileNumber = payload.mobileNumber ? Number(payload.mobileNumber) : null;

      const res = await authAPI.createUser(payload, token);
      if (res.status === 'SUCCESS') {
        addNotification({ type: 'success', title: 'User Created', message: `${formData.userName} has been created successfully.` });
        resetForm();
        setView('table');
        fetchUsers();
      } else {
        addNotification({ type: 'error', title: 'Error', message: res.statusMsg || 'Failed to create user.' });
      }
    } catch (err) {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to create user.' });
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({
      institution: '',
      designation: '',
      emailId: '',
      type: 'INTERNAL',
      userStatus: 'INACTIVE',
      roleId: '',
      roleName: '',
      userName: '',
      userPassword: '',
      confirmPassword: '',
      mobileNumber: '',
      createdBy: 'system',
      approvedYn: 'N',
    });
    setErrors({});
  };

  const handleRoleChange = (e) => {
    const selectedRoleId = e.target.value;
    const selectedRole = roles.find(r => String(r.roleId) === selectedRoleId);
    setFormData({
      ...formData,
      roleId: selectedRoleId,
      roleName: selectedRole ? selectedRole.roleName : '',
    });
  };

  const filteredUsers = users.filter(u => {
    const term = searchTerm.toLowerCase();
    return (
      (u.userName || '').toLowerCase().includes(term) ||
      (u.emailId || '').toLowerCase().includes(term) ||
      (u.institution || '').toLowerCase().includes(term)
    );
  });

  // ─── Table View ───
  if (view === 'table') {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <h1>User Management</h1>
          <p>Manage system users</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <Card className={styles.tableCard}>
            <div className={styles.tableControls}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  placeholder="Search users..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button variant="gold" leftIcon={<UserPlus size={18} />} onClick={() => { resetForm(); setView('form'); }}>
                Add User
              </Button>
            </div>

            {tableLoading ? (
              <div className={styles.loadingOverlay}>
                <Loader size={24} className={styles.spinner} />
                Loading users...
              </div>
            ) : (
              <div className={styles.tableContainer}>
                <table className={styles.mainTable}>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Username</th>
                      <th>Email</th>
                      <th>Institution</th>
                      <th>Designation</th>
                      <th>Mobile</th>
                      <th>Type</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>Created At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.length === 0 ? (
                      <tr>
                        <td colSpan="10" className={styles.emptyCell}>No users found</td>
                      </tr>
                    ) : (
                      filteredUsers.map((user, idx) => (
                        <tr key={user.userId}>
                          <td>{idx + 1}</td>
                          <td>{user.userName}</td>
                          <td>{user.emailId}</td>
                          <td>{user.institution || '-'}</td>
                          <td>{user.designation || '-'}</td>
                          <td>{user.mobileNumber || '-'}</td>
                          <td>{user.type}</td>
                          <td>
                            <span className={styles.roleBadge}>{user.role?.roleName || '-'}</span>
                          </td>
                          <td>
                            <span className={`${styles.statusBadge} ${user.userStatus === 'ACTIVE' ? styles.statusActive : styles.statusInactive}`}>
                              {user.userStatus}
                            </span>
                          </td>
                          <td style={{ whiteSpace: 'nowrap' }}>
                            {user.createdAt ? (
                              <div className={styles.dateTimeCell}>
                                <span className={styles.dateText}>{formatDate(user.createdAt)}</span>
                                <span className={styles.timeText}>{formatDate(user.createdAt, 'HH:mm:ss')}</span>
                              </div>
                            ) : '-'}
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
  }

  // ─── Form View ───
  return (
    <div className={styles.page}>
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
        <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={() => setView('table')} className={styles.backBtn}>
          Back to Users
        </Button>
        <h1>Add New User</h1>
        <p>Create a new user account</p>
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <Card className={styles.tableCard}>
          <form onSubmit={handleSubmit}>
            <div className={styles.viewSection}>
              <h3 className={styles.viewSectionHeader}>User Details</h3>
              <div className={styles.formGrid}>
                <Input
                  label="Username"
                  placeholder="Enter username"
                  leftIcon={<User size={18} />}
                  value={formData.userName}
                  onChange={(e) => setFormData({ ...formData, userName: e.target.value })}
                  error={errors.userName}
                  required
                />
                <Input
                  label="Email"
                  type="email"
                  placeholder="Enter email address"
                  leftIcon={<Mail size={18} />}
                  value={formData.emailId}
                  onChange={(e) => setFormData({ ...formData, emailId: e.target.value })}
                  error={errors.emailId}
                  required
                />
                <Input
                  label="Institution"
                  placeholder="Enter institution"
                  leftIcon={<Building size={18} />}
                  value={formData.institution}
                  onChange={(e) => setFormData({ ...formData, institution: e.target.value })}
                />
                <Input
                  label="Designation"
                  placeholder="Enter designation"
                  leftIcon={<Briefcase size={18} />}
                  value={formData.designation}
                  onChange={(e) => setFormData({ ...formData, designation: e.target.value })}
                />
                <Input
                  label="Mobile Number"
                  placeholder="Enter mobile number"
                  leftIcon={<Phone size={18} />}
                  value={formData.mobileNumber}
                  maxLength={10}
                  onChange={(e) => {
                    const raw = e.target.value;
                    if (/[^\d]/.test(raw)) {
                      setErrors((prev) => ({ ...prev, mobileNumber: 'Only numbers are allowed' }));
                      return;
                    }
                    const val = raw;
                    if (val.length > 0 && !/^[6-9]/.test(val)) {
                      setErrors((prev) => ({ ...prev, mobileNumber: 'Must start with 6, 7, 8, or 9' }));
                      return;
                    }
                    setFormData({ ...formData, mobileNumber: val });
                    if (val.length === 10) {
                      setErrors((prev) => ({ ...prev, mobileNumber: undefined }));
                    } else if (val.length > 0) {
                      setErrors((prev) => ({ ...prev, mobileNumber: 'Must be 10 digits' }));
                    } else {
                      setErrors((prev) => ({ ...prev, mobileNumber: undefined }));
                    }
                  }}
                  error={errors.mobileNumber}
                />
                <Select
                  label="Role"
                  placeholder="Select a role"
                  options={roleOptions}
                  value={formData.roleId}
                  onChange={handleRoleChange}
                  error={errors.roleId}
                  required
                />
                <Select
                  label="User Type"
                  options={typeOptions}
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                />
              </div>
            </div>

            <div className={styles.viewSection}>
              <h3 className={styles.viewSectionHeader}>Security</h3>
              <div className={styles.formGrid}>
                <Input
                  label="Password"
                  type="password"
                  placeholder="Enter password"
                  leftIcon={<Lock size={18} />}
                  value={formData.userPassword}
                  onChange={(e) => {
                    const val = e.target.value;
                    setFormData({ ...formData, userPassword: val });
                    if (formData.confirmPassword) {
                      if (val !== formData.confirmPassword) {
                        setErrors((prev) => ({ ...prev, confirmPassword: 'Passwords do not match' }));
                      } else {
                        setErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                      }
                    }
                  }}
                  error={errors.userPassword}
                  required
                />
                <Input
                  label="Confirm Password"
                  type="password"
                  placeholder="Confirm password"
                  leftIcon={<Lock size={18} />}
                  value={formData.confirmPassword}
                  onChange={(e) => {
                    const val = e.target.value;
                    setFormData({ ...formData, confirmPassword: val });
                    if (val && val !== formData.userPassword) {
                      setErrors((prev) => ({ ...prev, confirmPassword: 'Passwords do not match' }));
                    } else if (val && val === formData.userPassword) {
                      setErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                    } else {
                      setErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                    }
                  }}
                  error={errors.confirmPassword}
                  success={formData.confirmPassword && formData.confirmPassword === formData.userPassword ? 'Passwords match' : undefined}
                  required
                />
              </div>
            </div>

            <div className={styles.formActions}>
              <Button variant="ghost" type="button" onClick={() => setView('table')}>
                Cancel
              </Button>
              <Button type="submit" variant="gold" loading={loading} leftIcon={<UserPlus size={18} />}>
                Create User
              </Button>
            </div>
          </form>
        </Card>
      </motion.div>
    </div>
  );
};

export default AddUser;
