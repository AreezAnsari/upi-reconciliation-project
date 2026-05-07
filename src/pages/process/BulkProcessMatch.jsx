import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Layers, Play, CheckCircle, AlertCircle, X } from 'lucide-react';
import { Button, Select, Card } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import styles from './Process.module.css';

// Result Modal Component
const ResultModal = ({ result, onClose }) => {
  const isSuccess = result?.status === 'SUCCESS';

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
              {isSuccess ? <CheckCircle size={20} /> : <AlertCircle size={20} />}
              <h3>{isSuccess ? 'Process Completed' : 'Process Failed'}</h3>
            </div>
            <button className={styles.modalClose} onClick={onClose}>
              <X size={20} />
            </button>
          </div>

          <div className={styles.modalContent}>
            <div className={styles.modalMessage}>
              {isSuccess ? (
                <CheckCircle size={48} className={styles.successIcon} />
              ) : (
                <AlertCircle size={48} className={styles.errorIcon} />
              )}
              <p>{result?.statusMsg || 'Process completed.'}</p>
            </div>
          </div>

          <div className={styles.modalFooter}>
            <Button variant="gold" onClick={onClose}>
              Close
            </Button>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

const BulkProcessMatch = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [bulkforceList, setBulkforceList] = useState([]);
  const [selectedProcessId, setSelectedProcessId] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [loadingList, setLoadingList] = useState(true);

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [processResult, setProcessResult] = useState(null);

  // Fetch bulkforce list on mount
  useEffect(() => {
    const fetchBulkforceList = async () => {
      if (!token) {
        setLoadingList(false);
        return;
      }

      try {
        const response = await authAPI.getBulkforceList(token);
        if (response.status === 'SUCCESS' && response.data) {
          setBulkforceList(response.data);
        } else {
          throw new Error(response.statusMsg || 'Failed to fetch bulkforce list');
        }
      } catch (error) {
        console.error('Failed to fetch bulkforce list:', error);
        addNotification({
          type: 'error',
          title: 'Error',
          message: 'Failed to load bulk process list.',
        });
      } finally {
        setLoadingList(false);
      }
    };

    fetchBulkforceList();
  }, [token]);

  // Map bulkforce list to dropdown options
  const processOptions = bulkforceList.map(item => ({
    id: String(item.reconProcessId),
    label: item.reconProcessName,
  }));

  const handleStartBulkMatch = async () => {
    if (!selectedProcessId) {
      addNotification({
        type: 'warning',
        title: 'Validation',
        message: 'Please select a process type.',
      });
      return;
    }

    setIsProcessing(true);

    try {
      const response = await authAPI.processBulkforce(selectedProcessId, token);
      setProcessResult(response);
      setShowModal(true);

      if (response.status === 'SUCCESS') {
        addNotification({
          type: 'success',
          title: 'Success',
          message: response.statusMsg || 'Bulk force process completed.',
        });
      } else {
        addNotification({
          type: 'error',
          title: 'Error',
          message: response.statusMsg || 'Bulk force process failed.',
        });
      }
    } catch (error) {
      console.error('Failed to process bulkforce:', error);
      setProcessResult({
        status: 'FAILURE',
        statusMsg: error.message || 'Failed to process bulk force match.',
        data: [],
      });
      setShowModal(true);
      addNotification({
        type: 'error',
        title: 'Error',
        message: error.message || 'Failed to process bulk force match.',
      });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className={styles.pageContainer}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        {/* Page Header */}
        <div className={styles.pageHeader}>
          <div className={styles.headerContent}>
            <div className={styles.headerIcon}>
              <Layers size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Bulk Process Match</h1>
              <p className={styles.pageSubtitle}>Process multiple files for reconciliation matching</p>
            </div>
          </div>
        </div>

        {/* Configuration Card */}
        <Card className={styles.configCard}>
          <Card.Header>
            <Card.Title>Process Configuration</Card.Title>
            <Card.Description>Select a process type and start bulk matching</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.configGrid}>
              <Select
                label="Process Type"
                placeholder={loadingList ? 'Loading...' : 'Select Process Type'}
                options={processOptions}
                value={selectedProcessId}
                onChange={(e) => setSelectedProcessId(e.target.value)}
                disabled={loadingList}
              />
            </div>

            <div className={styles.buttonGroup}>
              <Button
                variant="gold"
                onClick={handleStartBulkMatch}
                loading={isProcessing}
                disabled={!selectedProcessId}
                leftIcon={<Play size={18} />}
              >
                Start Bulk Match
              </Button>
            </div>
          </Card.Content>
        </Card>
      </motion.div>

      {/* Result Modal */}
      {showModal && (
        <ResultModal
          result={processResult}
          onClose={() => setShowModal(false)}
        />
      )}
    </div>
  );
};

export default BulkProcessMatch;
