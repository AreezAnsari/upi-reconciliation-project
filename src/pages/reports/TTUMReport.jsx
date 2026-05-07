import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FileText, Download, X, CheckSquare, Square, FileDown } from 'lucide-react';
import { Card, Button, Table, PageHeader } from '../../components/common';
import { useAuthStore, useAppStore } from '../../store';
import { authAPI } from '../../services';
import styles from './Reports.module.css';

// Report Selection Modal Component (same pattern as GenerateReport)
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
              <h3>Generated TTUM Reports</h3>
            </div>
            <button className={styles.modalClose} onClick={onClose}>
              <X size={20} />
            </button>
          </div>

          <div className={styles.modalContent}>
            {reports.length > 0 ? (
              <>
                <div className={styles.selectAllRow}>
                  <button
                    className={styles.selectAllButton}
                    onClick={toggleAll}
                  >
                    {allSelected ? <CheckSquare size={18} /> : <Square size={18} />}
                    <span>Select All ({reports.length} reports)</span>
                  </button>
                </div>

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
                      </div>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className={styles.noReports}>
                <FileText size={48} />
                <p>No reports found.</p>
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

const TTUMReport = () => {
  const { token, user } = useAuthStore();
  const { addNotification } = useAppStore();

  const [ttumData, setTtumData] = useState([]);
  const [selectedRows, setSelectedRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);
  const [generating, setGenerating] = useState(false);

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [generatedReports, setGeneratedReports] = useState([]);
  const [isDownloading, setIsDownloading] = useState(false);

  // Fetch TTUM report data on mount
  useEffect(() => {
    const fetchTtumData = async () => {
      if (!token) {
        setLoadingData(false);
        return;
      }

      try {
        const response = await authAPI.getTtumReportData(token);
        if (response.status === 'SUCCESS' && response.data) {
          const mapped = response.data.map((item, index) => ({
            id: String(item.ttumConfigId || index),
            description: item.ttumDescription,
            type: item.ttumType,
            reconModel: item.ttumTypeDescription,
            // Keep full data for generate request
            _raw: item,
          }));
          setTtumData(mapped);
        } else {
          throw new Error(response.statusMsg || 'Failed to fetch TTUM data');
        }
      } catch (error) {
        console.error('Failed to fetch TTUM report data:', error);
        addNotification({
          type: 'error',
          title: 'Error',
          message: 'Failed to load TTUM report data.',
        });
      } finally {
        setLoadingData(false);
      }
    };

    fetchTtumData();
  }, [token]);

  const columns = [
    {
      key: 'description',
      label: 'TTUM Description',
      sortable: true,
    },
    {
      key: 'type',
      label: 'TTUM Type',
      sortable: true,
      width: '120px',
    },
    {
      key: 'reconModel',
      label: 'Recon Model',
      sortable: true,
      width: '250px',
    },
  ];

  const handleGenerateReport = async () => {
    if (selectedRows.length === 0) {
      addNotification({
        type: 'warning',
        title: 'Validation',
        message: 'Please select at least one TTUM entry.',
      });
      return;
    }

    setGenerating(true);

    try {
      // Get the selected TTUM row (use first selected for single generate)
      const selectedItem = ttumData.find(d => d.id === String(selectedRows[0]));
      if (!selectedItem || !selectedItem._raw) {
        throw new Error('Selected TTUM entry not found');
      }

      const raw = selectedItem._raw;
      const payload = {
        ttumConfigId: raw.ttumConfigId,
        ttumDescription: raw.ttumDescription,
        ttumEntityId: raw.ttumEntityId,
        ttumProcessId: raw.ttumProcessId,
        ttumTypeDescription: raw.ttumTypeDescription,
        ttumType: raw.ttumType,
        jrxmlId: raw.jrxmlId,
        outputFormat: raw.outputFormat,
        insertCode: raw.insertCode,
        ttumCatType: raw.ttumCatType,
        settleFileId: raw.settleFileId,
        isCBSTTUM: raw.isCBSTTUM,
        reconProcessName: raw.reconProcessName,
        userId: user?.userId || null,
      };

      const response = await authAPI.generateTtumReport(payload, token);

      if (response.status === 'SUCCESS') {
        const mapped = (response.data || []).map((item, idx) => ({
          reportId: item.ttumConfigId || idx,
          reportName: item.ttumDescription,
          reportLocation: item.reportFileLocation,
          processId: item.ttumProcessId,
        }));
        setGeneratedReports(mapped);
        setShowModal(true);
        addNotification({
          type: 'success',
          title: 'Success',
          message: `Generated ${mapped.length} report(s).`,
        });
      } else {
        throw new Error(response.statusMsg || 'Failed to generate TTUM report');
      }
    } catch (error) {
      console.error('Failed to generate TTUM report:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: error.message || 'Failed to generate TTUM report.',
      });
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = async (selectedReports) => {
    if (selectedReports.length === 0) return;

    setIsDownloading(true);

    try {
      const payload = selectedReports.map(report => ({
        processId: String(report.processId),
        reportName: report.reportName,
        reportLocation: report.reportLocation,
      }));

      const blob = await authAPI.downloadReportZip(payload, token);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;

      const fileName = selectedReports.length === 1
        ? selectedReports[0].reportName
        : `ttum_reports_${new Date().toISOString().split('T')[0]}.zip`;

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

  return (
    <div className={styles.page}>
      <PageHeader
        title="TTUM Report"
        description="Transaction Type Unit Mapping reports"
      />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <Card>
          <Card.Content>
            <Table
              columns={columns}
              data={ttumData}
              selectable
              onSelectionChange={setSelectedRows}
              pageSize={10}
              loading={loadingData}
            />

            <div className={styles.buttonGroup}>
              <Button
                variant="gold"
                onClick={handleGenerateReport}
                loading={generating}
                leftIcon={<FileText size={18} />}
              >
                Generate Report
              </Button>
            </div>
          </Card.Content>
        </Card>
      </motion.div>

      {/* Report Selection Modal */}
      {showModal && (
        <ReportModal
          reports={generatedReports}
          onClose={() => setShowModal(false)}
          onDownload={handleDownload}
          isDownloading={isDownloading}
        />
      )}
    </div>
  );
};

export default TTUMReport;
