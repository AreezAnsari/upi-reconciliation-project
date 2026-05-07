import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Calendar, Search, Database } from 'lucide-react';
import { Card, Button, Select, PageHeader } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import { formatDate } from '../../utils/helpers';
import styles from './Reports.module.css';

const ExtractionDetails = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [processData, setProcessData] = useState([]);
  const [loadingProcess, setLoadingProcess] = useState(true);
  const [loading, setLoading] = useState(false);

  const [form, setForm] = useState({
    processType: '',
    fileProcess: '',
    date: '',
  });

  const [results, setResults] = useState(null);

  // Process Type options from API
  const processTypeOptions = processData.map(item => ({
    id: item.processMastId,
    label: item.longName,
  }));

  // File Process options based on selected process type
  const fileProcessOptions = (() => {
    if (!form.processType) return [];
    const selected = processData.find(p => p.processMastId === Number(form.processType));
    if (!selected) return [];
    if (selected.fileList?.length) {
      return selected.fileList.map(f => ({ id: f.reconFileId, label: f.reconFileName }));
    }
    if (selected.processList?.length) {
      return selected.processList.map(p => ({ id: p.reconProcessId, label: p.reconProcessName }));
    }
    return [];
  })();

  // Get the selected file/process label for API payload
  const getSelectedFileName = () => {
    if (!form.fileProcess) return '';
    const option = fileProcessOptions.find(o => String(o.id) === String(form.fileProcess));
    return option?.label || '';
  };

  // Get the selected process type longName for API payload
  const getSelectedProcessTypeName = () => {
    if (!form.processType) return '';
    const selected = processData.find(p => p.processMastId === Number(form.processType));
    return selected?.longName || '';
  };

  useEffect(() => {
    const fetchProcessTypes = async () => {
      if (!token) { setLoadingProcess(false); return; }
      try {
        const response = await authAPI.getProcess(token, 'Y');
        if (response.status === 'SUCCESS' && response.data) {
          setProcessData(response.data);
        }
      } catch (error) {
        console.error('Failed to fetch process types:', error);
        addNotification({ type: 'error', title: 'Error', message: 'Failed to load process types.' });
      } finally {
        setLoadingProcess(false);
      }
    };
    fetchProcessTypes();
  }, [token]);

  const handleView = async () => {
    if (!form.processType) {
      addNotification({ type: 'error', title: 'Validation', message: 'Please select a process type.' });
      return;
    }
    if (!form.fileProcess) {
      addNotification({ type: 'error', title: 'Validation', message: 'Please select a file/process.' });
      return;
    }
    if (!form.date) {
      addNotification({ type: 'error', title: 'Validation', message: 'Please select a date.' });
      return;
    }

    setLoading(true);
    setResults(null);

    try {
      const payload = {
        processId: Number(form.fileProcess),
        reportType: getSelectedProcessTypeName(),
        reportFileName: getSelectedFileName(),
        reportDate: form.date,
      };

      const response = await authAPI.viewExtractionDetails(payload, token);
      setResults(response.data || []);

      if (!response.data?.length) {
        addNotification({ type: 'info', title: 'No Data', message: 'No extraction details found for the selected criteria.' });
      }
    } catch (error) {
      addNotification({ type: 'error', title: 'Error', message: error.message || 'Failed to fetch extraction details.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="Job Details"
        description="View extraction details by process type and date"
      />

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <Card className={styles.formCard}>
          <Card.Content>
            <div className={styles.formGrid}>
              <div className={styles.formGroup}>
                <label className={styles.label}>
                  <span className={styles.required}>*</span> Process Type
                </label>
                <Select
                  placeholder={loadingProcess ? 'Loading...' : 'Select Process Type'}
                  options={processTypeOptions}
                  value={form.processType}
                  onChange={(e) => setForm({ ...form, processType: e.target.value, fileProcess: '' })}
                  disabled={loadingProcess}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>
                  <span className={styles.required}>*</span> File Process
                </label>
                <Select
                  placeholder="Select File/Process"
                  options={fileProcessOptions}
                  value={form.fileProcess}
                  onChange={(e) => setForm({ ...form, fileProcess: e.target.value })}
                  disabled={!form.processType}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>
                  <span className={styles.required}>*</span> Select Date
                </label>
                <div className={styles.dateInput}>
                  <input
                    type="date"
                    className={styles.input}
                    value={form.date}
                    onChange={(e) => setForm({ ...form, date: e.target.value })}
                  />
                  <Calendar size={18} className={styles.dateIcon} />
                </div>
              </div>
            </div>

            <div className={styles.buttonGroup}>
              <Button
                variant="gold"
                onClick={handleView}
                loading={loading}
                leftIcon={<Search size={18} />}
              >
                View
              </Button>
            </div>
          </Card.Content>
        </Card>
      </motion.div>

      {/* Results Table */}
      {results && (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          style={{ marginTop: '1.5rem' }}
        >
          <Card>
            <Card.Header>
              <Card.Title>
                <Database size={18} style={{ marginRight: '0.5rem' }} />
                Job Details ({results.length} record{results.length !== 1 ? 's' : ''})
              </Card.Title>
            </Card.Header>
            <Card.Content>
              {results.length > 0 ? (
                <div className={styles.tableWrapper}>
                  <table className={styles.dataTable}>
                    <thead>
                      <tr>
                        <th>Sr No</th>
                        <th>Process ID</th>
                        <th>Process Type</th>
                        <th>Status</th>
                        <th>File Name</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((row, index) => (
                        <tr key={index}>
                          <td>{index + 1}</td>
                          <td>{row.processId ?? 'N/A'}</td>
                          <td>{row.processType ?? 'N/A'}</td>
                          <td>{row.status ?? 'N/A'}</td>
                          <td>{row.fileName ?? 'N/A'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className={styles.emptyState}>
                  <Database size={32} />
                  <h3>No Data Found</h3>
                  <p>No extraction details found for the selected criteria.</p>
                </div>
              )}
            </Card.Content>
          </Card>
        </motion.div>
      )}
    </div>
  );
};

export default ExtractionDetails;
