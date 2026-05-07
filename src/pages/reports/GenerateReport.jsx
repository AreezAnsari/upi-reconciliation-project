import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FileText, Calendar, Download, X, CheckSquare, Square, FileDown, Upload, Search } from 'lucide-react';
import { Card, Button, Select, PageHeader, Checkbox } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import styles from './Reports.module.css';

const TABS = [
  { id: 'generate', label: 'Generate', icon: Upload },
  { id: 'retrieve', label: 'Retrieve', icon: Search },
];

// Report Selection Modal Component
const ReportModal = ({ reports, onClose, onDownload, isDownloading }) => {
  const [selectedReports, setSelectedReports] = useState([]);

  const toggleReport = (reportId) => {
    setSelectedReports(prev =>
      prev.includes(reportId)
        ? prev.filter(id => id !== reportId)
        : [...prev, reportId]
    );
  };

  const toggleAll = () => {
    if (selectedReports.length === reports.length) {
      setSelectedReports([]);
    } else {
      setSelectedReports(reports.map(r => r.reportId));
    }
  };

  const handleDownload = () => {
    const selected = reports.filter(r => selectedReports.includes(r.reportId));
    onDownload(selected);
  };

  const allSelected = selectedReports.length === reports.length && reports.length > 0;

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
              <FileText size={20} />
              <h3>Available Reports</h3>
            </div>
            <button className={styles.modalClose} onClick={onClose}>
              <X size={20} />
            </button>
          </div>

          <div className={styles.modalContent}>
            {reports.length > 0 ? (
              <>
                {/* Select All Header */}
                <div className={styles.selectAllRow}>
                  <button
                    className={styles.selectAllButton}
                    onClick={toggleAll}
                  >
                    {allSelected ? <CheckSquare size={18} /> : <Square size={18} />}
                    <span>Select All ({reports.length} reports)</span>
                  </button>
                </div>

                {/* Report List */}
                <div className={styles.reportList}>
                  {reports.map((report) => (
                    <div
                      key={report.reportId}
                      className={`${styles.reportItem} ${selectedReports.includes(report.reportId) ? styles.selected : ''}`}
                      onClick={() => toggleReport(report.reportId)}
                    >
                      <div className={styles.reportCheckbox}>
                        {selectedReports.includes(report.reportId) ? (
                          <CheckSquare size={18} className={styles.checked} />
                        ) : (
                          <Square size={18} />
                        )}
                      </div>
                      <div className={styles.reportInfo}>
                        <div className={styles.reportName}>{report.reportName}</div>
                        <div className={styles.reportMeta}>
                          <span>File: {report.fileName}</span>
                          <span>Date: {report.reportDate}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className={styles.noReports}>
                <FileText size={48} />
                <p>No reports found for the selected criteria.</p>
              </div>
            )}
          </div>

          <div className={styles.modalFooter}>
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            {reports.length > 0 && (
              <Button
                variant="gold"
                onClick={handleDownload}
                disabled={selectedReports.length === 0}
                loading={isDownloading}
                leftIcon={<FileDown size={18} />}
              >
                Download {selectedReports.length > 0 ? `(${selectedReports.length})` : ''}
              </Button>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

const GenerateReport = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();
  const [searchParams, setSearchParams] = useSearchParams();

  const [activeTab, setActiveTab] = useState(searchParams.get('tab') || 'generate');
  const [genForm, setGenForm] = useState({ reportType: '', fileProcess: '', date: '' });
  const [retForm, setRetForm] = useState({ reportType: searchParams.get('reportType') || '', fileProcess: '', date: '' });
  const [loading, setLoading] = useState(false);
  const [reportTypes, setReportTypes] = useState([]);
  const [processData, setProcessData] = useState([]);
  const [loadingReportTypes, setLoadingReportTypes] = useState(true);

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [retrievedReports, setRetrievedReports] = useState([]);
  const [isDownloading, setIsDownloading] = useState(false);

  const FORCEMATCH_ID = 'FORCEMATCH';

  // Clear URL search params after consuming them
  useEffect(() => {
    if (searchParams.has('tab') || searchParams.has('reportType')) {
      setSearchParams({}, { replace: true });
    }
  }, []);

  // Fetch report types from API
  useEffect(() => {
    const fetchReportTypes = async () => {
      if (!token) {
        setLoadingReportTypes(false);
        return;
      }

      try {
        const response = await authAPI.getProcess(token, 'Y');
        if (response.status === 'SUCCESS' && response.data) {
          setProcessData(response.data);
          const options = response.data.map(item => ({
            id: item.processMastId,
            label: item.longName
          }));
          // Add hardcoded FORCEMATCH option
          options.push({ id: FORCEMATCH_ID, label: 'FORCEMATCH' });
          setReportTypes(options);
        }
      } catch (error) {
        console.error('Failed to fetch report types:', error);
        addNotification({
          type: 'error',
          title: 'Error',
          message: 'Failed to load report types.',
        });
      } finally {
        setLoadingReportTypes(false);
      }
    };

    fetchReportTypes();
  }, [token]);

  // Helper to check if a reportType is FORCEMATCH
  const isForceMatch = (reportType) => reportType === FORCEMATCH_ID;

  // Helper to get file/process options for a given reportType
  const getFileProcessOptions = (reportType) => {
    if (!reportType) return [];

    // FORCEMATCH uses same processList as RECONCILIATION
    if (isForceMatch(reportType)) {
      const reconProcess = processData.find(p => p.longName === 'RECONCILIATION');
      if (reconProcess?.processList) {
        return reconProcess.processList.map(p => ({ id: p.reconProcessId, label: p.reconProcessName }));
      }
      return [];
    }

    const selectedProcess = processData.find(
      p => p.processMastId === Number(reportType)
    );
    if (!selectedProcess) return [];
    if (selectedProcess.longName === 'EXTRACTION' && selectedProcess.fileList) {
      return selectedProcess.fileList.map(f => ({ id: f.reconFileId, label: f.reconFileName }));
    } else if (selectedProcess.longName === 'RECONCILIATION' && selectedProcess.processList) {
      return selectedProcess.processList.map(p => ({ id: p.reconProcessId, label: p.reconProcessName }));
    }
    return [];
  };

  const genFileProcessOptions = getFileProcessOptions(genForm.reportType);
  const retFileProcessOptions = getFileProcessOptions(retForm.reportType);

  const handleGenerate = async () => {
    const isFM = isForceMatch(genForm.reportType);
    if (!genForm.reportType || !genForm.fileProcess || (!isFM && !genForm.date)) {
      addNotification({ type: 'warning', title: 'Validation', message: 'Please fill all required fields.' });
      return;
    }

    setLoading(true);
    try {
      const selectedFile = genFileProcessOptions.find(
        opt => String(opt.id) === String(genForm.fileProcess)
      );

      const payload = {
        reportId: ' ',
        processId: String(genForm.fileProcess),
        reportKey: ' ',
        reportQuery: ' ',
        reportName: isFM ? 'FORCE_MATCH' : ' ',
        reportFileName: selectedFile?.label || ' ',
        reportSeprator: ' ',
        reportLocation: ' ',
        reportDate: isFM ? ' ' : genForm.date,
        reportType: ' '
      };

      const response = await authAPI.generateNtslReport(payload, token);

      if (response.status === 'SUCCESS') {
        addNotification({ type: 'success', title: 'Success', message: 'Report generated successfully!' });
      } else {
        throw new Error(response.statusMsg || 'Failed to generate report');
      }
    } catch (error) {
      console.error('Failed to generate report:', error);
      addNotification({ type: 'error', title: 'Error', message: error.message || 'Failed to generate report.' });
    } finally {
      setLoading(false);
    }
  };

  const handleRetrieve = async () => {
    const isFM = isForceMatch(retForm.reportType);
    if (!retForm.reportType || !retForm.fileProcess || !retForm.date) {
      addNotification({ type: 'warning', title: 'Validation', message: 'Please fill all required fields.' });
      return;
    }

    setLoading(true);
    try {
      const selectedFile = retFileProcessOptions.find(
        opt => String(opt.id) === String(retForm.fileProcess)
      );

      const payload = {
        reportId: ' ',
        processId: String(retForm.fileProcess),
        reportKey: ' ',
        reportQuery: ' ',
        reportName: isFM ? 'FORCE_MATCH' : ' ',
        reportFileName: selectedFile?.label || ' ',
        reportSeprator: ' ',
        reportLocation: ' ',
        reportDate: retForm.date,
        reportType: ' '
      };

      const response = await authAPI.retrieveReport(payload, token);

      if (response.status === 'SUCCESS') {
        setRetrievedReports(response.data || []);
        setShowModal(true);
        addNotification({ type: 'success', title: 'Success', message: `Found ${response.data?.length || 0} reports.` });
      } else {
        throw new Error(response.statusMsg || 'Failed to retrieve reports');
      }
    } catch (error) {
      console.error('Failed to retrieve reports:', error);
      addNotification({ type: 'error', title: 'Error', message: error.message || 'Failed to retrieve reports.' });
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (selectedReports) => {
    if (selectedReports.length === 0) return;

    setIsDownloading(true);

    try {
      const payload = selectedReports.map(report => ({
        processId: String(report.processId),
        reportName: report.reportName,
        reportLocation: report.reportLocation
      }));

      const blob = await authAPI.downloadReportZip(payload, token);

      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;

      // Generate filename
      const fileName = selectedReports.length === 1
        ? selectedReports[0].reportName
        : `reports_${new Date().toISOString().split('T')[0]}.zip`;

      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      addNotification({
        type: 'success',
        title: 'Downloaded',
        message: `Successfully downloaded ${selectedReports.length} report(s).`,
      });

      setShowModal(false);
    } catch (error) {
      console.error('Failed to download reports:', error);
      addNotification({
        type: 'error',
        title: 'Download Failed',
        message: error.message || 'Failed to download reports.',
      });
    } finally {
      setIsDownloading(false);
    }
  };

  const renderForm = (formData, setFormData, options, { hideDate = false } = {}) => (
    <div className={styles.formGrid}>
      <div className={styles.formGroup}>
        <label className={styles.label}>
          <span className={styles.required}>*</span> Report Type
        </label>
        <Select
          placeholder={loadingReportTypes ? "Loading..." : "Select Report Type"}
          options={reportTypes}
          value={formData.reportType}
          onChange={(e) => setFormData({
            ...formData,
            reportType: e.target.value,
            fileProcess: ''
          })}
          disabled={loadingReportTypes}
        />
      </div>

      <div className={styles.formGroup}>
        <label className={styles.label}>
          <span className={styles.required}>*</span> File Process
        </label>
        <Select
          placeholder="Select File/Process"
          options={options}
          value={formData.fileProcess}
          onChange={(e) => setFormData({ ...formData, fileProcess: e.target.value })}
          disabled={!formData.reportType}
        />
      </div>

      {!hideDate && (
        <div className={styles.formGroup}>
          <label className={styles.label}>
            <span className={styles.required}>*</span> Select Date
          </label>
          <div className={styles.dateInput}>
            <input
              type="date"
              className={styles.input}
              value={formData.date}
              max={new Date().toISOString().split('T')[0]}
              onChange={(e) => setFormData({ ...formData, date: e.target.value })}
            />
            <Calendar size={18} className={styles.dateIcon} />
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div className={styles.page}>
      <PageHeader
        title="Generate Report"
        description="Create and retrieve reconciliation reports"
      />

      {/* Segmented Tab Control */}
      <div className={styles.tabContainer}>
        <div className={styles.tabBar} role="tablist">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                role="tab"
                aria-selected={isActive}
                className={`${styles.tab} ${isActive ? styles.tabActive : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                <span className={styles.tabContent}>
                  <Icon size={16} />
                  <span className={styles.tabLabel}>{tab.label}</span>
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div>
          <Card className={styles.formCard}>
            <Card.Content>
              {activeTab === 'generate' ? (
                <>
                  {renderForm(genForm, setGenForm, genFileProcessOptions, { hideDate: isForceMatch(genForm.reportType) })}
                  <div className={styles.buttonGroup}>
                    <Button
                      variant="gold"
                      onClick={handleGenerate}
                      loading={loading}
                      leftIcon={<Upload size={18} />}
                    >
                      Generate Report
                    </Button>
                  </div>
                </>
              ) : (
                <>
                  {renderForm(retForm, setRetForm, retFileProcessOptions)}
                  <div className={styles.buttonGroup}>
                    <Button
                      variant="gold"
                      onClick={handleRetrieve}
                      loading={loading}
                      leftIcon={<Search size={18} />}
                    >
                      Retrieve Report
                    </Button>
                  </div>
                </>
              )}
            </Card.Content>
          </Card>
      </div>

      {/* Report Selection Modal */}
      {showModal && (
        <ReportModal
          reports={retrievedReports}
          onClose={() => setShowModal(false)}
          onDownload={handleDownload}
          isDownloading={isDownloading}
        />
      )}
    </div>
  );
};

export default GenerateReport;
