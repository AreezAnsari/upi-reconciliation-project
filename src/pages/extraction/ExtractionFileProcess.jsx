import { useState, useEffect } from 'react';
import { useSearchParams, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FileText,
  Play,
  CheckCircle,
  Clock,
  Copy,
  Check,
  Folder,
  Database,
  RefreshCw,
  AlertCircle,
  X,
  Info
} from 'lucide-react';
import { Button, Card } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import { formatNumber } from '../../utils/helpers';
import styles from './Extraction.module.css';

// Status Badge Component
const StatusBadge = ({ status }) => {
  const getStatusClass = (status) => {
    if (!status) return styles.statusPending;
    const s = status.toLowerCase();
    if (s === 'running' || s === 'processing') return styles.statusProcessing;
    if (s === 'completed' || s === 'success') return styles.statusSuccess;
    if (s === 'failed' || s === 'error') return styles.statusError;
    return styles.statusPending;
  };

  return (
    <span className={`${styles.statusBadge} ${getStatusClass(status)}`}>
      {status || 'N/A'}
    </span>
  );
};

// Modal Component for Extraction Status Details
const StatusDetailModal = ({ data, onClose, isReconciliation }) => {
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
              <h3>{isReconciliation ? 'Reconciliation' : 'Extraction'} Status Details</h3>
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
                <span className={styles.modalLabel}>{isReconciliation ? 'Recon Status' : 'Extraction Status'}</span>
                <StatusBadge status={isReconciliation ? data.reconStatus : data.extractionStatus} />
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
                <span className={styles.modalValue}>{data.dataCount ?? 'N/A'}</span>
              </div>
              <div className={styles.modalItem}>
                <span className={styles.modalLabel}>Segregation Status</span>
                <StatusBadge status={data.segretionStatus} />
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

const ExtractionFileProcess = () => {
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const processId = searchParams.get('processid');

  // Determine if this is extraction or reconciliation based on URL path
  const isReconciliation = location.pathname.includes('/reconciliation/');
  const processType = isReconciliation ? 'Reconciliation' : 'Extraction';

  const { token, user } = useAuthStore();
  const { addNotification } = useAppStore();

  const [menuData, setMenuData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [processComplete, setProcessComplete] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState(0);
  const [results, setResults] = useState(null);
  const [logs, setLogs] = useState([]);
  const [copied, setCopied] = useState(false);
  const [apiResponse, setApiResponse] = useState(null);
  const [selectedRowData, setSelectedRowData] = useState(null);
  const [extractionData, setExtractionData] = useState([]);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const steps = [
    { title: 'Initializing', description: 'Setting up extraction environment' },
    { title: 'Reading Files', description: 'Loading raw data files' },
    { title: 'Parsing Data', description: 'Extracting transaction records' },
    { title: 'Validating', description: 'Checking data integrity' },
    { title: 'Storing', description: 'Saving to database' },
    { title: 'Completing', description: 'Finalizing process' }
  ];

  // Reset all processing state when processId changes (e.g. menu navigation)
  useEffect(() => {
    setIsProcessing(false);
    setProcessComplete(false);
    setProgress(0);
    setCurrentStep(0);
    setResults(null);
    setLogs([]);
    setApiResponse(null);
    setExtractionData([]);
    setSelectedRowData(null);
  }, [processId]);

  // Fetch menu data to get reconFilePath
  useEffect(() => {
    const fetchMenuData = async () => {
      if (!token || !user?.roleId || !processId) {
        setLoading(false);
        return;
      }

      try {
        const response = await authAPI.getMenuByRole(user.roleId, token);
        if (response.status === 'SUCCESS' && response.data) {
          // Find menu matching the processId (menuProcessId)
          const matchingMenu = response.data.find(
            menu => menu.menuProcessId === Number(processId)
          );
          setMenuData(matchingMenu);
          if (matchingMenu) {
            addNotification({
              type: 'success',
              title: 'Ready',
              message: `Loaded ${matchingMenu.menuName || 'process'} details.`,
            });
          }
        }
      } catch (error) {
        console.error('Failed to fetch menu data:', error);
        addNotification({
          type: 'error',
          title: 'Error',
          message: 'Failed to load menu data.',
        });
      } finally {
        setLoading(false);
      }
    };

    fetchMenuData();
  }, [token, user?.roleId, processId]);

  const handleCopyPath = async () => {
    if (menuData?.reconFilePath) {
      try {
        await navigator.clipboard.writeText(menuData.reconFilePath);
        setCopied(true);
        addNotification({
          type: 'success',
          title: 'Copied',
          message: 'File path copied to clipboard.',
        });
        setTimeout(() => setCopied(false), 2000);
      } catch (err) {
        console.error('Failed to copy:', err);
        addNotification({
          type: 'error',
          title: 'Copy Failed',
          message: 'Failed to copy file path to clipboard.',
        });
      }
    }
  };

  const addLog = (message, type = 'info') => {
    setLogs(prev => [...prev, { message, type, timestamp: new Date().toISOString() }]);
  };

  const handleStartProcess = async () => {
    if (!processId) {
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Process ID is missing.',
      });
      return;
    }

    setIsProcessing(true);
    setProcessComplete(false);
    setProgress(0);
    setCurrentStep(0);
    setLogs([]);
    setApiResponse(null);

    addNotification({
      type: 'info',
      title: 'Processing',
      message: `Starting ${processType.toLowerCase()} process...`,
    });

    addLog(`Starting ${processType.toLowerCase()} process...`, 'info');
    addLog(`Process ID: ${processId}`, 'info');
    addLog(`File Path: ${menuData?.reconFilePath || 'N/A'}`, 'info');

    try {
      // Call the appropriate API based on process type
      addLog(`Calling start-${processType.toLowerCase()} API...`, 'info');
      const response = isReconciliation
        ? await authAPI.startReconciliation(processId, token)
        : await authAPI.startExtraction(processId, token);
      setApiResponse(response);

      if (response.status === 'SUCCESS') {
        addLog('API call successful!', 'success');

        // Simulate progress steps
        for (let i = 0; i < steps.length; i++) {
          setCurrentStep(i);
          addLog(`${steps[i].title}: ${steps[i].description}`, 'info');

          const subSteps = 10;
          for (let j = 0; j < subSteps; j++) {
            await new Promise(resolve => setTimeout(resolve, 100));
            setProgress(((i * subSteps + j + 1) / (steps.length * subSteps)) * 100);
          }
        }

        addLog(`${processType} process completed successfully!`, 'success');

        setResults({
          status: 'SUCCESS',
          message: response.statusMsg || `${processType} completed successfully`,
          processId: processId,
          data: response.data
        });

        // Store extraction data separately for refresh updates
        setExtractionData(response.data || []);

        setProcessComplete(true);
        addNotification({
          type: 'success',
          title: 'Success',
          message: `${processType} process started successfully.`,
        });
      } else {
        throw new Error(response.statusMsg || `${processType} failed`);
      }
    } catch (error) {
      console.error(`Failed to start ${processType.toLowerCase()}:`, error);
      addLog(`Error: ${error.message}`, 'error');
      addNotification({
        type: 'error',
        title: 'Error',
        message: error.message || `Failed to start ${processType.toLowerCase()} process.`,
      });
      setProcessComplete(true);
      setResults({
        status: 'FAILED',
        message: error.message,
        processId: processId
      });
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReset = () => {
    setIsProcessing(false);
    setProcessComplete(false);
    setProgress(0);
    setCurrentStep(0);
    setResults(null);
    setLogs([]);
    setApiResponse(null);
    setExtractionData([]);
  };

  const handleRefreshStatus = async () => {
    if (!extractionData.length) return;

    setIsRefreshing(true);

    try {
      // Build request payload from current extraction data
      const requestData = extractionData.map(item => ({
        sequenceId: String(item.sequenceNo || ''),
        processId: String(item.processId || '')
      }));

      addLog(`Refreshing ${processType.toLowerCase()} status...`, 'info');

      // Call the appropriate refresh API based on process type
      const response = isReconciliation
        ? await authAPI.refreshReconciliationStatus(requestData, token)
        : await authAPI.refreshExtractionStatus(requestData, token);

      if (response.status === 'SUCCESS' && response.data) {
        setExtractionData(response.data);
        addLog('Status refreshed successfully!', 'success');
        addNotification({
          type: 'success',
          title: 'Status Updated',
          message: `${processType} status has been refreshed.`,
        });
      } else {
        throw new Error(response.statusMsg || 'Failed to refresh status');
      }
    } catch (error) {
      console.error('Failed to refresh status:', error);
      addLog(`Error refreshing status: ${error.message}`, 'error');
      addNotification({
        type: 'error',
        title: 'Refresh Failed',
        message: error.message || `Failed to refresh ${processType.toLowerCase()} status.`,
      });
    } finally {
      setIsRefreshing(false);
    }
  };

  const getStepStatus = (index) => {
    if (processComplete || index < currentStep) return 'completed';
    if (index === currentStep && isProcessing) return 'active';
    return 'pending';
  };

  const getStepIcon = (status) => {
    switch (status) {
      case 'completed': return <CheckCircle size={18} />;
      case 'active': return <Clock size={18} />;
      default: return <Clock size={18} />;
    }
  };

  if (loading) {
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
        transition={{ duration: 0.5 }}
      >
        {/* Page Header */}
        <div className={styles.pageHeader}>
          <div className={styles.headerContent}>
            <div className={styles.headerIcon}>
              <FileText size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>
                {menuData?.menuName || `${processType} File Processing`}
              </h1>
              <p className={styles.pageSubtitle}>
                {menuData?.menuDescription || `Execute file ${processType.toLowerCase()} and processing`}
              </p>
            </div>
          </div>
        </div>

        {/* File Path Display with Copy Button */}
        <Card className={styles.filePathCard}>
          <Card.Header>
            <Card.Title>File Path</Card.Title>
          </Card.Header>
          <Card.Content>
            <div className={styles.filePathWrapper}>
              <div className={styles.filePath}>
                <Folder size={20} className={styles.filePathIcon} />
                <span className={styles.filePathText}>
                  {menuData?.reconFilePath || 'No file path available'}
                </span>
              </div>
              {menuData?.reconFilePath && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopyPath}
                  className={styles.copyButton}
                >
                  {copied ? <Check size={16} /> : <Copy size={16} />}
                  {copied ? 'Copied!' : 'Copy'}
                </Button>
              )}
            </div>
          </Card.Content>
        </Card>

        {/* Process Info */}
        <div className={styles.processInfo}>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Process ID</div>
            <div className={styles.infoValue}>{processId}</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Process Type</div>
            <div className={styles.infoValue}>{processType}</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Master Menu</div>
            <div className={styles.infoValue}>{menuData?.masterMenuParent || 'N/A'}</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Status</div>
            <div className={styles.infoValue}>
              {processComplete ? (results?.status === 'SUCCESS' ? 'Completed' : 'Failed') : isProcessing ? 'Processing' : 'Ready'}
            </div>
          </div>
        </div>

        {/* Execution Card */}
        {!isProcessing && !processComplete && (
          <Card>
            <Card.Content>
              <div className={styles.executionCard}>
                <div className={styles.executionIcon}>
                  <Database size={36} />
                </div>
                <h2 className={styles.executionTitle}>Ready to Execute</h2>
                <p className={styles.executionDescription}>
                  Click the button below to start the {processType.toLowerCase()} process
                </p>
                <Button onClick={handleStartProcess} size="lg">
                  <Play size={20} />
                  Start {processType} Process
                </Button>
              </div>
            </Card.Content>
          </Card>
        )}

        {/* Processing Progress */}
        {isProcessing && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <Card>
              <Card.Header>
                <Card.Title>Processing in Progress</Card.Title>
              </Card.Header>
              <Card.Content>
                <div className={styles.progressContainer}>
                  <div className={styles.progressHeader}>
                    <span className={styles.progressLabel}>{steps[currentStep]?.title}</span>
                    <span className={styles.progressPercent}>{Math.round(progress)}%</span>
                  </div>
                  <div className={styles.progressBar}>
                    <motion.div
                      className={styles.progressFill}
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>

                <div className={styles.stepsContainer}>
                  {steps.map((step, index) => (
                    <div key={index} className={styles.step}>
                      <div className={`${styles.stepIcon} ${styles[getStepStatus(index)]}`}>
                        {getStepIcon(getStepStatus(index))}
                      </div>
                      <div className={styles.stepContent}>
                        <div className={styles.stepTitle}>{step.title}</div>
                        <div className={styles.stepDescription}>{step.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </Card.Content>
            </Card>
          </motion.div>
        )}

        {/* Results */}
        {processComplete && results && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            {/* Status Banner */}
            <Card style={{ marginBottom: '1.5rem' }}>
              <Card.Content>
                <div className={results.status === 'SUCCESS' ? styles.successState : styles.errorState}>
                  {results.status === 'SUCCESS' ? (
                    <CheckCircle size={48} className={styles.successIcon} />
                  ) : (
                    <AlertCircle size={48} className={styles.errorIcon} />
                  )}
                  <h2 className={results.status === 'SUCCESS' ? styles.successTitle : styles.errorTitle}>
                    {results.status === 'SUCCESS' ? 'Process Started!' : 'Processing Failed'}
                  </h2>
                  <p className={styles.successDescription}>
                    {results.message}
                  </p>
                </div>
              </Card.Content>
            </Card>

            {/* Extraction Data Table */}
            {results.status === 'SUCCESS' && extractionData && extractionData.length > 0 && (
              <Card>
                <Card.Header>
                  <Card.Title>{isReconciliation ? 'Reconciliation' : 'Extraction'} Results</Card.Title>
                </Card.Header>
                <Card.Content>
                  <div className={styles.tableWrapper}>
                    <table className={styles.dataTable}>
                      <thead>
                        <tr>
                          <th>Process ID</th>
                          <th>Process Type</th>
                          <th>File Name</th>
                          <th>{isReconciliation ? 'Recon Status' : 'Extraction Status'}</th>
                          <th>Segregation Status</th>
                          <th>Report Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {extractionData.map((row, index) => (
                          <tr key={row.sequenceNo || index}>
                            <td>{row.processId}</td>
                            <td>{row.processType}</td>
                            <td>{row.fileName || 'N/A'}</td>
                            <td>
                              <button
                                className={styles.statusLink}
                                onClick={() => setSelectedRowData(row)}
                              >
                                <StatusBadge status={isReconciliation ? row.reconStatus : row.extractionStatus} />
                              </button>
                            </td>
                            <td>
                              <StatusBadge status={row.segretionStatus} />
                            </td>
                            <td>
                              <StatusBadge status={row.reportStatus} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Card.Content>
                <Card.Footer>
                  <Button
                    variant="outline"
                    onClick={handleRefreshStatus}
                    loading={isRefreshing}
                  >
                    <RefreshCw size={18} className={isRefreshing ? styles.spinIcon : ''} />
                    Refresh Status
                  </Button>
                </Card.Footer>
              </Card>
            )}

            {/* No Data State */}
            {results.status === 'SUCCESS' && (!extractionData || extractionData.length === 0) && (
              <Card>
                <Card.Content>
                  <div className={styles.emptyState}>
                    <div className={styles.emptyIcon}>
                      <Database size={32} />
                    </div>
                    <h3>No Data Available</h3>
                    <p>The extraction process started but returned no data.</p>
                  </div>
                </Card.Content>
                <Card.Footer />
              </Card>
            )}

            {/* Error State */}
            {results.status !== 'SUCCESS' && (
              <Card>
                <Card.Footer>
                  <Button variant="outline" onClick={handleReset}>
                    <RefreshCw size={18} />
                    Try Again
                  </Button>
                </Card.Footer>
              </Card>
            )}
          </motion.div>
        )}

        {/* Status Detail Modal */}
        {selectedRowData && (
          <StatusDetailModal
            data={selectedRowData}
            onClose={() => setSelectedRowData(null)}
            isReconciliation={isReconciliation}
          />
        )}

        {/* Log Output */}
        {logs.length > 0 && (
          <Card style={{ marginTop: '1.5rem' }}>
            <Card.Header>
              <Card.Title>Execution Log</Card.Title>
            </Card.Header>
            <Card.Content>
              <div className={styles.logContainer}>
                {logs.map((log, index) => (
                  <div key={index} className={`${styles.logLine} ${styles[log.type]}`}>
                    [{new Date(log.timestamp).toLocaleTimeString()}] {log.message}
                  </div>
                ))}
              </div>
            </Card.Content>
          </Card>
        )}
      </motion.div>
    </div>
  );
};

export default ExtractionFileProcess;
