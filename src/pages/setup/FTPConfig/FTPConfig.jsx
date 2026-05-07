import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Plus, Pencil, Eye, Trash2, ArrowLeft, Loader2, Search, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Button, Card } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './FTPConfig.module.css';

const FTPConfig = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [ftpServers, setFtpServers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewData, setViewData] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingServer, setEditingServer] = useState(null);

  // Table controls
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Form state
  const [formData, setFormData] = useState({
    ftpServerName: '',
    serverIp: '',
    port: '',
    userName: '',
    password: '',
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (token) {
      fetchFtpServers();
    }
  }, [token]);

  const fetchFtpServers = async () => {
    setLoading(true);
    try {
      const data = await authAPI.getFtpServers(token);
      setFtpServers(data || []);
    } catch (error) {
      console.error('Failed to fetch FTP servers:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to fetch FTP servers. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  // Filter
  const filteredServers = useMemo(() => {
    if (!searchTerm) return ftpServers;
    const term = searchTerm.toLowerCase();
    return ftpServers.filter(
      (s) =>
        s.ftpServerName?.toLowerCase().includes(term) ||
        s.serverIp?.toLowerCase().includes(term)
    );
  }, [ftpServers, searchTerm]);

  // Sort
  const sortedServers = useMemo(() => {
    if (!sortConfig.key) return filteredServers;
    return [...filteredServers].sort((a, b) => {
      const aVal = a[sortConfig.key] ?? '';
      const bVal = b[sortConfig.key] ?? '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredServers, sortConfig]);

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
    setShowForm(false);
    setEditingServer(null);
  };

  const handleView = (server) => {
    setViewData(server);
  };

  const handleAdd = () => {
    setFormData({
      ftpServerName: '',
      serverIp: '',
      port: '',
      userName: '',
      password: '',
    });
    setEditingServer(null);
    setShowForm(true);
  };

  const handleEdit = (server) => {
    setEditingServer(server);
    setFormData({
      ftpServerName: server.ftpServerName || '',
      serverIp: server.serverIp || '',
      port: server.port ?? '',
      userName: server.userName || '',
      password: server.password || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (server) => {
    const serverId = server.ftpServerId || server.id;
    if (!serverId) {
      addNotification({ type: 'info', title: 'Delete', message: 'Cannot determine server ID.' });
      return;
    }
    try {
      await authAPI.deleteFtpServer(serverId, token);
      addNotification({
        type: 'success',
        title: 'Success',
        message: `FTP server "${server.ftpServerName}" deleted successfully.`,
      });
      fetchFtpServers();
    } catch (error) {
      console.error('Failed to delete FTP server:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to delete FTP server. Please try again.',
      });
    }
  };

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!formData.ftpServerName.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'FTP Server Name is required.' });
      return;
    }
    if (!formData.serverIp.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Server IP is required.' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        ftpServerName: formData.ftpServerName,
        serverIp: formData.serverIp,
        port: parseInt(formData.port) || 21,
        userName: formData.userName,
        password: formData.password,
      };

      if (editingServer) {
        const serverId = editingServer.ftpServerId || editingServer.id;
        await authAPI.updateFtpServer(serverId, payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'FTP server updated successfully.',
        });
      } else {
        await authAPI.addFtpServer(payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'FTP server added successfully.',
        });
      }
      setShowForm(false);
      setEditingServer(null);
      fetchFtpServers();
    } catch (error) {
      console.error(`Failed to ${editingServer ? 'update' : 'add'} FTP server:`, error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: `Failed to ${editingServer ? 'update' : 'add'} FTP server. Please try again.`,
      });
    } finally {
      setSubmitting(false);
    }
  };

  const getVal = (value) => {
    if (value === null || value === undefined || value === '') return '—';
    return String(value);
  };

  // ─── Add / Edit Form Page ───
  if (showForm) {
    return (
      <div className={styles.page}>
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={styles.header}
        >
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ArrowLeft size={18} />}
            onClick={handleBack}
            className={styles.backBtn}
          >
            Back to FTP Configuration
          </Button>
          <h1>{editingServer ? 'Edit FTP Server' : 'Add FTP Server'}</h1>
          <p>{editingServer ? 'Update the FTP server details' : 'Fill in the details to add a new FTP server'}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>FTP Server Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>FTP Server Name</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.ftpServerName}
                  onChange={(e) => handleFormChange('ftpServerName', e.target.value)}
                  placeholder="Enter FTP server name"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Server IP</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.serverIp}
                  onChange={(e) => handleFormChange('serverIp', e.target.value)}
                  placeholder="Enter server IP"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Port</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.port}
                  onChange={(e) => handleFormChange('port', e.target.value)}
                  placeholder="Enter port (default: 21)"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Username</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.userName}
                  onChange={(e) => handleFormChange('userName', e.target.value)}
                  placeholder="Enter username"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Password</span>
                <input
                  className={styles.formInput}
                  type="password"
                  value={formData.password}
                  onChange={(e) => handleFormChange('password', e.target.value)}
                  placeholder="Enter password"
                />
              </div>
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
                  {editingServer ? 'Updating...' : 'Adding...'}
                </>
              ) : (
                <>
                  {editingServer ? <Pencil size={16} /> : <Plus size={16} />}
                  {editingServer ? 'Update FTP Server' : 'Add FTP Server'}
                </>
              )}
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ─── View Page ───
  if (viewData) {
    return (
      <div className={styles.page}>
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={styles.header}
        >
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ArrowLeft size={18} />}
            onClick={handleBack}
            className={styles.backBtn}
          >
            Back to FTP Configuration
          </Button>
          <h1>View FTP Server</h1>
          <p>{viewData.ftpServerName}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>FTP Server Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>FTP Server Name</span>
                <span className={styles.viewValue}>{getVal(viewData.ftpServerName)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Server IP</span>
                <span className={styles.viewValue}>{getVal(viewData.serverIp)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Port</span>
                <span className={styles.viewValue}>{getVal(viewData.port)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Username</span>
                <span className={styles.viewValue}>{getVal(viewData.userName)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Password</span>
                <span className={styles.viewValue}>{'*'.repeat(viewData.password?.length || 8)}</span>
              </div>
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
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className={styles.header}
      >
        <h1>FTP Configuration</h1>
        <p>Manage FTP server connections</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Card className={styles.tableCard}>
          {/* Controls */}
          <div className={styles.tableControls}>
            <div className={styles.showEntries}>
              <span>Showing {filteredServers.length} of {ftpServers.length} entries</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search FTP servers..."
                />
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleAdd}>
                Add
              </Button>
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className={styles.loadingOverlay}>
              <Loader2 size={24} className={styles.spinner} />
              <span>Loading FTP servers...</span>
            </div>
          ) : (
            <div className={styles.tableContainer}>
              <table className={styles.mainTable}>
                <thead>
                  <tr>
                    <th className={styles.sortable} onClick={() => handleSort('ftpServerName')}>
                      <div className={styles.thContent}>
                        <span>FTP Server Name</span>
                        {getSortIcon('ftpServerName')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('serverIp')}>
                      <div className={styles.thContent}>
                        <span>Server IP</span>
                        {getSortIcon('serverIp')}
                      </div>
                    </th>
                    <th className={styles.actionCol}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedServers.length === 0 ? (
                    <tr>
                      <td colSpan={3} className={styles.emptyCell}>
                        No FTP servers found
                      </td>
                    </tr>
                  ) : (
                    sortedServers.map((server, idx) => (
                      <tr key={server.ftpServerId || server.id || idx}>
                        <td>{server.ftpServerName || '—'}</td>
                        <td>{server.serverIp || '—'}</td>
                        <td className={styles.actionCol}>
                          <div className={styles.actionBtns}>
                            <button
                              className={styles.iconBtn}
                              title="Edit"
                              onClick={() => handleEdit(server)}
                            >
                              <Pencil size={15} />
                            </button>
                            <button
                              className={`${styles.iconBtn} ${styles.iconBtnView}`}
                              title="View"
                              onClick={() => handleView(server)}
                            >
                              <Eye size={15} />
                            </button>
                            <button
                              className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                              title="Delete"
                              onClick={() => handleDelete(server)}
                            >
                              <Trash2 size={15} />
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

export default FTPConfig;
