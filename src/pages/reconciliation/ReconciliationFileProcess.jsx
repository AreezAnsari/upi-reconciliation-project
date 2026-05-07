import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play,
  RefreshCw,
  AlertCircle,
  Database,
  CheckCircle,
  Info,
  X,
} from 'lucide-react';
import { Button, Card } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import styles from './Reconciliation.module.css';

const StatusBadge = ({ status }) => {
  const getClass = (s) => {
    if (!s) return styles.statusPending;
    const v = s.toLowerCase();
    if (v === 'running' || v === 'processing') return styles.statusProcessing;
    if (v === 'completed' || v === 'success') return styles.statusSuccess;
    if (v === 'failed' || v === 'error') return styles.statusError;
    return styles.statusPending;
  };
  return (
    <span className={`${styles.statusBadge} ${getClass(status)}`}>
      {status || 'N/A'}
    </span>
  );
};

// Modal Component for Reconciliation Status Details
const ReconStatusDetailModal = ({ data, onClose }) => {
  if (!data) return null;

  return (
    <AnimatePresence>
      <motion.div
        className={styles.modalOverlay}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      >
        <motion.div
          className={styles.modal}
          initial={{ opacity: 0, scale: 0.9, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.9, y: 20 }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className={styles.modalHeader}>
            <div className={styles.modalTitle}>
              <Info size={20} />
              <h3>Reconciliation Status Details</h3>
            </div>
            <button className={styles.modalClose} onClick={onClose}>
              <X size={20} />
            </button>
          </div>
          <div className={styles.modalContent}>
            <div className={styles.modalGrid}>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Process ID</span>
                <span className={styles.modalValue}>{data.processId ?? 'N/A'}</span>
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>File Name</span>
                <span className={styles.modalValue}>{data.fileName || 'N/A'}</span>
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Recon Status</span>
                <StatusBadge status={data.reconStatus} />
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Report Status</span>
                <StatusBadge status={data.reportStatus} />
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Start Time</span>
                <span className={styles.modalValue}>{data.startTime || 'N/A'}</span>
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>End Time</span>
                <span className={styles.modalValue}>{data.endTime || 'N/A'}</span>
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Data Count</span>
                <span className={styles.modalValue}>{data.reconDataCount ?? 'N/A'}</span>
              </div>
            </div>
          </div>
          <div className={styles.modalFooter}>
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

const ReconciliationFileProcess = () => {
  const [searchParams] = useSearchParams();
  const processId = searchParams.get('processid');

  const { token, user } = useAuthStore();
  const { addNotification } = useAppStore();

  const [menuData, setMenuData] = useState(null);
  const [menuLoading, setMenuLoading] = useState(true);
  const [isStarting, setIsStarting] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [reconData, setReconData] = useState([]);
  const [started, setStarted] = useState(false);
  const [selectedRowData, setSelectedRowData] = useState(null);

  // Reset all processing state when processId changes (e.g. menu navigation)
  useEffect(() => {
    setIsStarting(false);
    setIsRefreshing(false);
    setReconData([]);
    setStarted(false);
    setSelectedRowData(null);
  }, [processId]);

  // Fetch menu data by role to get menu name/description
  useEffect(() => {
    const fetchMenu = async () => {
      if (!token || !user?.roleId) { setMenuLoading(false); return; }
      try {
        const res = await authAPI.getMenuByRole(user.roleId, token);
        if (res.status === 'SUCCESS' && res.data) {
          const match = res.data.find(m => m.menuProcessId === Number(processId));
          setMenuData(match);
        }
      } catch (err) {
        console.error('Failed to fetch menu data:', err);
      } finally {
        setMenuLoading(false);
      }
    };
    fetchMenu();
  }, [token, user?.roleId, processId]);

  const handleStart = async () => {
    setIsStarting(true);
    try {
      const res = await authAPI.startReconciliation(processId, token);
      if (res.status === 'SUCCESS') {
        setReconData(res.data || []);
        setStarted(true);
        addNotification({ type: 'success', title: 'Reconciliation Started', message: res.statusMsg || 'Process started successfully.' });
      } else {
        throw new Error(res.statusMsg || 'Failed to start reconciliation');
      }
    } catch (err) {
      addNotification({ type: 'error', title: 'Error', message: err.message || 'Failed to start reconciliation.' });
    } finally {
      setIsStarting(false);
    }
  };

  const handleRefresh = async () => {
    if (!reconData.length) return;
    setIsRefreshing(true);
    try {
      const payload = reconData.map(item => ({
        sequenceId: String(item.sequenceId || item.sequenceNo || ' '),
        processId: String(item.processId || ' '),
      }));
      const res = await authAPI.refreshReconciliationStatus(payload, token);
      if (res.status === 'SUCCESS' && res.data) {
        setReconData(res.data);
        addNotification({ type: 'success', title: 'Status Updated', message: 'Reconciliation status refreshed.' });
      } else {
        throw new Error(res.statusMsg || 'Failed to refresh');
      }
    } catch (err) {
      addNotification({ type: 'error', title: 'Refresh Failed', message: err.message || 'Failed to refresh status.' });
    } finally {
      setIsRefreshing(false);
    }
  };

  if (menuLoading) {
    return (
      <div className={styles.pageContainer}>
        <div className={styles.loadingState}>
          <RefreshCw size={32} className={styles.spinIcon} />
          <p>Loading process details...</p>
        </div>
      </div>
    );
  }

  if (!processId) {
    return (
      <div className={styles.pageContainer}>
        <Card>
          <Card.Content>
            <div className={styles.errorState}>
              <AlertCircle size={48} />
              <h2>Missing Process ID</h2>
              <p>No process ID was provided in the URL.</p>
            </div>
          </Card.Content>
        </Card>
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        {/* Page Header */}
        <div className={styles.pageHeader}>
          <div className={styles.headerContent}>
            <div className={styles.headerIcon}>
              <RefreshCw size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>
                {menuData?.menuName || 'Reconciliation Process'}
              </h1>
              <p className={styles.pageSubtitle}>
                {menuData?.menuDescription || 'Execute and monitor reconciliation process'}
              </p>
            </div>
          </div>
        </div>

        {/* Process Info */}
        <div className={styles.processInfo}>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Process ID</div>
            <div className={styles.infoValue}>{processId}</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Process Type</div>
            <div className={styles.infoValue}>Reconciliation</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Master Menu</div>
            <div className={styles.infoValue}>{menuData?.masterMenuParent || 'N/A'}</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Status</div>
            <div className={styles.infoValue}>
              {!started ? 'Ready' : reconData.length ? 'Running' : 'Completed'}
            </div>
          </div>
        </div>

        {/* Start Card — shown before process is started */}
        {!started && (
          <Card>
            <Card.Content>
              <div className={styles.executionCard}>
                <div className={styles.executionIcon}>
                  <Database size={36} />
                </div>
                <h2 className={styles.executionTitle}>Ready to Execute</h2>
                <p className={styles.executionDescription}>
                  Click the button below to start the reconciliation process
                </p>
                <Button onClick={handleStart} size="lg" loading={isStarting}>
                  <Play size={20} />
                  Start Reconciliation
                </Button>
              </div>
            </Card.Content>
          </Card>
        )}

        {/* Results Table */}
        {started && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Card>
              <Card.Header>
                <Card.Title>
                  <CheckCircle size={18} style={{ marginRight: '0.5rem', color: 'var(--teal-600)' }} />
                  Reconciliation Results
                </Card.Title>
              </Card.Header>
              <Card.Content>
                {reconData.length > 0 ? (
                  <div className={styles.tableWrapper}>
                    <table className={styles.dataTable}>
                      <thead>
                        <tr>
                          <th>Sr No</th>
                          <th>Process Id</th>
                          <th>Process Type</th>
                          <th>Process Name</th>
                          <th>Start Time</th>
                          <th>End Time</th>
                          <th>Recon Status</th>
                          <th>Report Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reconData.map((row, index) => (
                          <tr key={row.processId || index}>
                            <td>{index + 1}</td>
                            <td>{row.processId ?? 'N/A'}</td>
                            <td>{row.processType ?? 'N/A'}</td>
                            <td>{row.processName ?? row.reconProcessName ?? 'N/A'}</td>
                            <td>{row.startTime ?? '–'}</td>
                            <td>{row.endTime ?? '–'}</td>
                            <td>
                              <button
                                className={styles.statusLink}
                                onClick={() => setSelectedRowData(row)}
                              >
                                <StatusBadge status={row.reconStatus ?? row.reConStatus} />
                              </button>
                            </td>
                            <td><StatusBadge status={row.reportStatus} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className={styles.emptyState}>
                    <div className={styles.emptyIcon}><Database size={32} /></div>
                    <h3>No Data Available</h3>
                    <p>The reconciliation process started but returned no data.</p>
                  </div>
                )}
              </Card.Content>
              <Card.Footer>
                <div className={styles.footerActions}>
                  <Button
                    variant="gold"
                    onClick={handleRefresh}
                    loading={isRefreshing}
                    disabled={!reconData.length}
                  >
                    <RefreshCw size={18} className={isRefreshing ? styles.spinIcon : ''} />
                    Refresh
                  </Button>
                </div>
              </Card.Footer>
            </Card>
          </motion.div>
        )}
        {/* Reconciliation Status Detail Modal */}
        {selectedRowData && (
          <ReconStatusDetailModal
            data={selectedRowData}
            onClose={() => setSelectedRowData(null)}
          />
        )}
      </motion.div>
    </div>
  );
};

export default ReconciliationFileProcess;
