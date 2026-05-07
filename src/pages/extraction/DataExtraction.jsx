import { useState } from 'react';
import { motion } from 'framer-motion';
import { Database, Search, Download, Filter, RefreshCw, Calendar, FileText } from 'lucide-react';
import { Button, Input, Select, Card } from '../../components/common';
import { DUMMY_TRANSACTIONS, DUMMY_FILE_PROCESSES } from '../../services/dummyData';
import { formatDate, formatNumber, formatCurrency, cn } from '../../utils/helpers';
import styles from './Extraction.module.css';

const DataExtraction = () => {
  const [searchParams, setSearchParams] = useState({
    processType: '',
    fileType: '',
    dateFrom: '',
    dateTo: ''
  });
  const [extractedData, setExtractedData] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const processTypes = [
    { value: '', label: 'All Process Types' },
    { value: 'debitcard', label: 'Debit Card' },
    { value: 'creditcard', label: 'Credit Card' },
    { value: 'upi', label: 'UPI' },
    { value: 'neft', label: 'NEFT' },
    { value: 'rtgs', label: 'RTGS' },
    { value: 'imps', label: 'IMPS' }
  ];

  const fileTypes = [
    { value: '', label: 'All File Types' },
    { value: 'pos_raw', label: 'POS Raw' },
    { value: 'cbs_raw', label: 'CBS Raw' },
    { value: 'switch_raw', label: 'Switch Raw' },
    { value: 'network_raw', label: 'Network Raw' }
  ];

  const handleSearch = async () => {
    setIsLoading(true);
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Generate extraction data
    const data = DUMMY_FILE_PROCESSES.map(file => ({
      ...file,
      extractionId: `EXT-${Math.random().toString(36).slice(2, 10).toUpperCase()}`,
      extractedRecords: Math.floor(file.totalRecords * 0.98),
      errorRecords: Math.floor(file.totalRecords * 0.02),
      extractionDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      extractionTime: `${Math.floor(Math.random() * 5)}m ${Math.floor(Math.random() * 60)}s`
    }));
    
    setExtractedData(data);
    setHasSearched(true);
    setIsLoading(false);
  };

  const handleReset = () => {
    setSearchParams({
      processType: '',
      fileType: '',
      dateFrom: '',
      dateTo: ''
    });
    setExtractedData([]);
    setHasSearched(false);
  };

  const getStatusBadge = (status) => {
    const statusStyles = {
      completed: styles.statusSuccess,
      processing: styles.statusProcessing,
      pending: styles.statusPending,
      error: styles.statusError
    };
    return (
      <span className={cn(styles.statusBadge, statusStyles[status])}>
        {status}
      </span>
    );
  };

  const getTotalStats = () => {
    if (extractedData.length === 0) return { total: 0, extracted: 0, errors: 0 };
    return {
      total: extractedData.reduce((sum, d) => sum + d.totalRecords, 0),
      extracted: extractedData.reduce((sum, d) => sum + d.extractedRecords, 0),
      errors: extractedData.reduce((sum, d) => sum + d.errorRecords, 0)
    };
  };

  const stats = getTotalStats();

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
              <Database size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Data Extraction</h1>
              <p className={styles.pageSubtitle}>View and manage extracted data records</p>
            </div>
          </div>
        </div>

        {/* Search Form */}
        <Card>
          <Card.Header>
            <Card.Title>Search Extracted Data</Card.Title>
            <Card.Description>Filter extraction records by type and date</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.configGrid}>
              <Select
                label="Process Type"
                options={processTypes}
                value={searchParams.processType}
                onChange={(e) => setSearchParams({...searchParams, processType: e.target.value})}
              />
              <Select
                label="File Type"
                options={fileTypes}
                value={searchParams.fileType}
                onChange={(e) => setSearchParams({...searchParams, fileType: e.target.value})}
              />
              <Input
                label="Date From"
                type="date"
                value={searchParams.dateFrom}
                onChange={(e) => setSearchParams({...searchParams, dateFrom: e.target.value})}
                leftIcon={<Calendar size={18} />}
              />
              <Input
                label="Date To"
                type="date"
                value={searchParams.dateTo}
                onChange={(e) => setSearchParams({...searchParams, dateTo: e.target.value})}
                leftIcon={<Calendar size={18} />}
              />
            </div>
          </Card.Content>
          <Card.Footer>
            <Button variant="outline" onClick={handleReset}>
              <RefreshCw size={18} />
              Reset
            </Button>
            <Button onClick={handleSearch} loading={isLoading}>
              <Search size={18} />
              Search
            </Button>
          </Card.Footer>
        </Card>

        {/* Stats Summary */}
        {hasSearched && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className={styles.processInfo} style={{ marginTop: '1.5rem' }}>
              <div className={styles.infoItem}>
                <div className={styles.infoLabel}>Total Records</div>
                <div className={styles.infoValue}>{formatNumber(stats.total)}</div>
              </div>
              <div className={styles.infoItem}>
                <div className={styles.infoLabel}>Extracted</div>
                <div className={styles.infoValue} style={{ color: 'var(--success-600)' }}>
                  {formatNumber(stats.extracted)}
                </div>
              </div>
              <div className={styles.infoItem}>
                <div className={styles.infoLabel}>Errors</div>
                <div className={styles.infoValue} style={{ color: 'var(--error-500)' }}>
                  {formatNumber(stats.errors)}
                </div>
              </div>
              <div className={styles.infoItem}>
                <div className={styles.infoLabel}>Success Rate</div>
                <div className={styles.infoValue}>
                  {stats.total > 0 ? ((stats.extracted / stats.total) * 100).toFixed(1) : 0}%
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Results Table */}
        {hasSearched && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <Card style={{ marginTop: '1.5rem' }}>
              <Card.Header>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Card.Title>Extraction Records</Card.Title>
                    <Card.Description>{extractedData.length} files found</Card.Description>
                  </div>
                  <Button variant="outline" size="sm">
                    <Download size={16} />
                    Export
                  </Button>
                </div>
              </Card.Header>
              <Card.Content>
                <div className={styles.tableWrapper}>
                  <table className={styles.dataTable}>
                    <thead>
                      <tr>
                        <th>Extraction ID</th>
                        <th>File Name</th>
                        <th>File Type</th>
                        <th>Total Records</th>
                        <th>Extracted</th>
                        <th>Errors</th>
                        <th>Status</th>
                        <th>Extraction Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {extractedData.map((item) => (
                        <tr key={item.extractionId}>
                          <td style={{ fontFamily: 'var(--font-mono)', color: 'var(--teal-600)' }}>
                            {item.extractionId}
                          </td>
                          <td>{item.fileName}</td>
                          <td>{item.fileType}</td>
                          <td>{formatNumber(item.totalRecords)}</td>
                          <td style={{ color: 'var(--success-600)', fontWeight: 600 }}>
                            {formatNumber(item.extractedRecords)}
                          </td>
                          <td style={{ color: item.errorRecords > 0 ? 'var(--error-500)' : 'inherit' }}>
                            {formatNumber(item.errorRecords)}
                          </td>
                          <td>{getStatusBadge(item.status)}</td>
                          <td>{formatDate(item.extractionDate)}</td>
                          <td>
                            <Button variant="ghost" size="sm">
                              <FileText size={16} />
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card.Content>
            </Card>
          </motion.div>
        )}

        {/* Empty State */}
        {!hasSearched && (
          <div className={styles.emptyState} style={{ marginTop: '2rem' }}>
            <div className={styles.emptyIcon}>
              <Database size={48} />
            </div>
            <h3>Search Extracted Data</h3>
            <p>Use the filters above to search for extraction records</p>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default DataExtraction;
