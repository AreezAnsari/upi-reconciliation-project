import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Filter, Download, Eye, Link2, Calendar, Hash, CreditCard } from 'lucide-react';
import { Button, Input, Select, Card } from '../../components/common';
import { DUMMY_TRANSACTIONS } from '../../services/dummyData';
import { formatDate, formatCurrency, cn } from '../../utils/helpers';
import styles from './Process.module.css';

const ReconSearch = () => {
  const [searchParams, setSearchParams] = useState({
    transactionId: '',
    rrn: '',
    cardNumber: '',
    reconStatus: '',
    dateFrom: '',
    dateTo: ''
  });
  const [results, setResults] = useState([]);
  const [selectedTxn, setSelectedTxn] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const reconStatusOptions = [
    { value: '', label: 'All Status' },
    { value: 'reconciled', label: 'Reconciled' },
    { value: 'partially_matched', label: 'Partially Matched' },
    { value: 'unreconciled', label: 'Unreconciled' },
    { value: 'disputed', label: 'Disputed' }
  ];

  const handleSearch = async () => {
    setIsSearching(true);
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    const extendedResults = DUMMY_TRANSACTIONS.map(txn => ({
      ...txn,
      reconStatus: ['Reconciled', 'Partially Matched', 'Unreconciled', 'Disputed'][Math.floor(Math.random() * 4)],
      reconDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      matchedWith: `CBS-${Math.random().toString().slice(2, 10)}`,
      variance: Math.random() > 0.7 ? (Math.random() * 100).toFixed(2) : '0.00'
    }));
    
    setResults(extendedResults);
    setHasSearched(true);
    setIsSearching(false);
  };

  const handleReset = () => {
    setSearchParams({
      transactionId: '',
      rrn: '',
      cardNumber: '',
      reconStatus: '',
      dateFrom: '',
      dateTo: ''
    });
    setResults([]);
    setHasSearched(false);
    setSelectedTxn(null);
  };

  const getReconStatusBadge = (status) => {
    const statusStyles = {
      'Reconciled': styles.statusMatched,
      'Partially Matched': styles.statusPending,
      'Unreconciled': styles.statusUnmatched,
      'Disputed': styles.statusError
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
              <Link2 size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Recon Transaction Search</h1>
              <p className={styles.pageSubtitle}>Search reconciliation status of transactions</p>
            </div>
          </div>
        </div>

        {/* Search Form */}
        <Card className={styles.searchCard}>
          <Card.Header>
            <Card.Title>Search Criteria</Card.Title>
            <Card.Description>Enter parameters to search reconciliation records</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.searchGrid}>
              <Input
                label="Transaction ID"
                placeholder="Enter transaction ID"
                value={searchParams.transactionId}
                onChange={(e) => setSearchParams({...searchParams, transactionId: e.target.value})}
                leftIcon={<Hash size={18} />}
              />
              <Input
                label="RRN"
                placeholder="Enter RRN"
                value={searchParams.rrn}
                onChange={(e) => setSearchParams({...searchParams, rrn: e.target.value})}
              />
              <Input
                label="Card Number (Last 4)"
                placeholder="Enter last 4 digits"
                value={searchParams.cardNumber}
                onChange={(e) => setSearchParams({...searchParams, cardNumber: e.target.value})}
                leftIcon={<CreditCard size={18} />}
                maxLength={4}
              />
              <Select
                label="Recon Status"
                options={reconStatusOptions}
                value={searchParams.reconStatus}
                onChange={(e) => setSearchParams({...searchParams, reconStatus: e.target.value})}
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
                    <Card.Title>Reconciliation Results</Card.Title>
                    <Card.Description>{results.length} records found</Card.Description>
                  </div>
                  <div className={styles.resultsActions}>
                    <Button variant="outline" size="sm">
                      <Filter size={16} />
                      Filter
                    </Button>
                    <Button variant="outline" size="sm">
                      <Download size={16} />
                      Export
                    </Button>
                  </div>
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
                        <th>Recon Status</th>
                        <th>Matched With</th>
                        <th>Variance</th>
                        <th>Recon Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((txn) => (
                        <tr 
                          key={txn.id}
                          className={selectedTxn?.id === txn.id ? styles.selectedRow : ''}
                        >
                          <td className={styles.txnId}>{txn.id}</td>
                          <td>{txn.rrn}</td>
                          <td className={styles.amount}>{formatCurrency(txn.amount)}</td>
                          <td>{getReconStatusBadge(txn.reconStatus)}</td>
                          <td className={styles.matchedRef}>{txn.matchedWith}</td>
                          <td className={parseFloat(txn.variance) > 0 ? styles.varianceWarning : ''}>
                            {parseFloat(txn.variance) > 0 ? `₹${txn.variance}` : '-'}
                          </td>
                          <td>{formatDate(txn.reconDate)}</td>
                          <td>
                            <Button 
                              variant="ghost" 
                              size="sm"
                              onClick={() => setSelectedTxn(txn)}
                            >
                              <Eye size={16} />
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

        {/* Transaction Detail Modal/Panel */}
        {selectedTxn && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Card className={styles.detailCard}>
              <Card.Header>
                <div className={styles.detailHeader}>
                  <Card.Title>Transaction Details</Card.Title>
                  <Button variant="ghost" size="sm" onClick={() => setSelectedTxn(null)}>
                    Close
                  </Button>
                </div>
              </Card.Header>
              <Card.Content>
                <div className={styles.detailGrid}>
                  <div className={styles.detailSection}>
                    <h4>Original Transaction</h4>
                    <div className={styles.detailItems}>
                      <div className={styles.detailItem}>
                        <span>Transaction ID</span>
                        <strong>{selectedTxn.id}</strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>RRN</span>
                        <strong>{selectedTxn.rrn}</strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>Amount</span>
                        <strong>{formatCurrency(selectedTxn.amount)}</strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>Type</span>
                        <strong>{selectedTxn.type}</strong>
                      </div>
                    </div>
                  </div>
                  <div className={styles.detailSection}>
                    <h4>Reconciliation Info</h4>
                    <div className={styles.detailItems}>
                      <div className={styles.detailItem}>
                        <span>Status</span>
                        <strong>{getReconStatusBadge(selectedTxn.reconStatus)}</strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>Matched With</span>
                        <strong>{selectedTxn.matchedWith}</strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>Variance</span>
                        <strong className={parseFloat(selectedTxn.variance) > 0 ? styles.varianceWarning : ''}>
                          {parseFloat(selectedTxn.variance) > 0 ? `₹${selectedTxn.variance}` : 'None'}
                        </strong>
                      </div>
                      <div className={styles.detailItem}>
                        <span>Recon Date</span>
                        <strong>{formatDate(selectedTxn.reconDate)}</strong>
                      </div>
                    </div>
                  </div>
                </div>
              </Card.Content>
            </Card>
          </motion.div>
        )}

        {/* Empty State */}
        {!hasSearched && (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>
              <Link2 size={48} />
            </div>
            <h3>Search Reconciliation Records</h3>
            <p>Enter search criteria above to find reconciliation status</p>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default ReconSearch;
