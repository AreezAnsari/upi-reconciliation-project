import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Filter, Download, RefreshCw, Calendar, Database, FileText, ChevronDown } from 'lucide-react';
import { Button, Input, Select, Card } from '../../components/common';
import { DUMMY_TRANSACTIONS, DUMMY_FILE_PROCESSES } from '../../services/dummyData';
import { formatDate, formatCurrency, cn } from '../../utils/helpers';
import styles from './Process.module.css';

const ExtractionSearch = () => {
  const [searchParams, setSearchParams] = useState({
    transactionId: '',
    rrn: '',
    processType: '',
    status: '',
    dateFrom: '',
    dateTo: ''
  });
  const [results, setResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
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

  const statusOptions = [
    { value: '', label: 'All Status' },
    { value: 'matched', label: 'Matched' },
    { value: 'unmatched', label: 'Unmatched' },
    { value: 'pending', label: 'Pending' },
    { value: 'error', label: 'Error' }
  ];

  const handleSearch = async () => {
    setIsSearching(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    setResults(DUMMY_TRANSACTIONS);
    setHasSearched(true);
    setIsSearching(false);
  };

  const handleReset = () => {
    setSearchParams({
      transactionId: '',
      rrn: '',
      processType: '',
      status: '',
      dateFrom: '',
      dateTo: ''
    });
    setResults([]);
    setHasSearched(false);
  };

  const getStatusBadge = (status) => {
    const statusStyles = {
      Matched: styles.statusMatched,
      Unmatched: styles.statusUnmatched,
      Pending: styles.statusPending,
      Error: styles.statusError
    };
    return (
      <span className={cn(styles.statusBadge, statusStyles[status])}>
        {status}
      </span>
    );
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
              <Search size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Extraction Transaction Search</h1>
              <p className={styles.pageSubtitle}>Search and analyze extracted transaction data</p>
            </div>
          </div>
        </div>

        {/* Search Form */}
        <Card className={styles.searchCard}>
          <Card.Header>
            <Card.Title>Search Criteria</Card.Title>
            <Card.Description>Enter parameters to search extracted transactions</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.searchGrid}>
              <Input
                label="Transaction ID"
                placeholder="Enter transaction ID"
                value={searchParams.transactionId}
                onChange={(e) => setSearchParams({...searchParams, transactionId: e.target.value})}
                leftIcon={<FileText size={18} />}
              />
              <Input
                label="RRN"
                placeholder="Enter RRN"
                value={searchParams.rrn}
                onChange={(e) => setSearchParams({...searchParams, rrn: e.target.value})}
                leftIcon={<Database size={18} />}
              />
              <Select
                label="Process Type"
                options={processTypes}
                value={searchParams.processType}
                onChange={(e) => setSearchParams({...searchParams, processType: e.target.value})}
              />
              <Select
                label="Status"
                options={statusOptions}
                value={searchParams.status}
                onChange={(e) => setSearchParams({...searchParams, status: e.target.value})}
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
          <Card.Footer className={styles.searchActions}>
            <Button variant="outline" onClick={handleReset}>
              <RefreshCw size={18} />
              Reset
            </Button>
            <Button onClick={handleSearch} loading={isSearching}>
              <Search size={18} />
              Search
            </Button>
          </Card.Footer>
        </Card>

        {/* Results Section */}
        {hasSearched && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.2 }}
          >
            <Card className={styles.resultsCard}>
              <Card.Header>
                <div className={styles.resultsHeader}>
                  <div>
                    <Card.Title>Search Results</Card.Title>
                    <Card.Description>{results.length} transactions found</Card.Description>
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
                        <th>Transaction ID</th>
                        <th>RRN</th>
                        <th>Amount</th>
                        <th>Type</th>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((txn) => (
                        <tr key={txn.id}>
                          <td className={styles.txnId}>{txn.id}</td>
                          <td>{txn.rrn}</td>
                          <td className={styles.amount}>{formatCurrency(txn.amount)}</td>
                          <td>{txn.type}</td>
                          <td>{formatDate(txn.date)}</td>
                          <td>{getStatusBadge(txn.status)}</td>
                          <td>
                            <Button variant="ghost" size="sm">View</Button>
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
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>
              <Search size={48} />
            </div>
            <h3>Search Transactions</h3>
            <p>Enter search criteria above to find extracted transactions</p>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default ExtractionSearch;
