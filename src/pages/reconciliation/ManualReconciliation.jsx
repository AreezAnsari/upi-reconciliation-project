import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link2, Search, CheckCircle, XCircle, ArrowRight, Filter, Download } from 'lucide-react';
import { Button, Input, Select, Card, Checkbox } from '../../components/common';
import { DUMMY_TRANSACTIONS } from '../../services/dummyData';
import { formatDate, formatCurrency, cn } from '../../utils/helpers';
import styles from './Reconciliation.module.css';

const ManualReconciliation = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSource, setSelectedSource] = useState(null);
  const [selectedTarget, setSelectedTarget] = useState(null);
  const [showMatchPanel, setShowMatchPanel] = useState(false);
  const [matchedPairs, setMatchedPairs] = useState([]);

  // Simulate source and target transactions
  const sourceTransactions = DUMMY_TRANSACTIONS.map(t => ({
    ...t,
    source: 'POS',
    sourceId: `POS-${t.id}`
  }));

  const targetTransactions = DUMMY_TRANSACTIONS.map(t => ({
    ...t,
    id: `CBS-${t.id.split('-')[1]}`,
    source: 'CBS',
    sourceId: `CBS-${t.id.split('-')[1]}`,
    amount: t.amount + (Math.random() > 0.8 ? Math.random() * 10 : 0)
  }));

  const handleSelectSource = (txn) => {
    setSelectedSource(txn);
    if (selectedTarget) {
      setShowMatchPanel(true);
    }
  };

  const handleSelectTarget = (txn) => {
    setSelectedTarget(txn);
    if (selectedSource) {
      setShowMatchPanel(true);
    }
  };

  const handleConfirmMatch = () => {
    if (selectedSource && selectedTarget) {
      setMatchedPairs(prev => [...prev, {
        source: selectedSource,
        target: selectedTarget,
        matchedAt: new Date().toISOString()
      }]);
      setSelectedSource(null);
      setSelectedTarget(null);
      setShowMatchPanel(false);
    }
  };

  const handleRejectMatch = () => {
    setShowMatchPanel(false);
  };

  const getVariance = () => {
    if (!selectedSource || !selectedTarget) return 0;
    return Math.abs(selectedSource.amount - selectedTarget.amount).toFixed(2);
  };

  const getStatusBadge = (status) => {
    const statusStyles = {
      Matched: styles.statusMatched,
      Unmatched: styles.statusUnmatched,
      Pending: styles.statusPending
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
              <h1 className={styles.pageTitle}>Manual Reconciliation</h1>
              <p className={styles.pageSubtitle}>Manually match and reconcile transactions</p>
            </div>
          </div>
        </div>

        {/* Match Summary */}
        {matchedPairs.length > 0 && (
          <div className={styles.batchActions}>
            <CheckCircle size={20} className={styles.successIcon} />
            <span className={styles.batchInfo}>
              <strong>{matchedPairs.length}</strong> transaction pair(s) matched in this session
            </span>
            <Button variant="outline" size="sm">
              <Download size={16} />
              Export Matches
            </Button>
          </div>
        )}

        {/* Two Column Layout */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
          {/* Source Transactions */}
          <Card className={styles.tableCard}>
            <Card.Header>
              <div className={styles.tableHeader}>
                <div>
                  <Card.Title>Source Transactions (POS)</Card.Title>
                  <Card.Description>Select a transaction to match</Card.Description>
                </div>
              </div>
            </Card.Header>
            <Card.Content>
              <div className={styles.filterPanel}>
                <Input
                  placeholder="Search by ID or RRN"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  leftIcon={<Search size={16} />}
                />
              </div>
              <div className={styles.tableWrapper}>
                <table className={styles.dataTable}>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>RRN</th>
                      <th>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sourceTransactions.map((txn) => (
                      <tr 
                        key={txn.sourceId}
                        className={cn(
                          selectedSource?.sourceId === txn.sourceId && styles.selectedRow,
                          matchedPairs.some(p => p.source.sourceId === txn.sourceId) && styles.matchedRow
                        )}
                        onClick={() => !matchedPairs.some(p => p.source.sourceId === txn.sourceId) && handleSelectSource(txn)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td className={styles.txnId}>{txn.sourceId}</td>
                        <td>{txn.rrn}</td>
                        <td className={styles.amount}>{formatCurrency(txn.amount)}</td>
                        <td>
                          {matchedPairs.some(p => p.source.sourceId === txn.sourceId) 
                            ? getStatusBadge('Matched')
                            : getStatusBadge(txn.status)
                          }
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card.Content>
          </Card>

          {/* Target Transactions */}
          <Card className={styles.tableCard}>
            <Card.Header>
              <div className={styles.tableHeader}>
                <div>
                  <Card.Title>Target Transactions (CBS)</Card.Title>
                  <Card.Description>Select matching transaction</Card.Description>
                </div>
              </div>
            </Card.Header>
            <Card.Content>
              <div className={styles.filterPanel}>
                <Input
                  placeholder="Search by ID or RRN"
                  leftIcon={<Search size={16} />}
                />
              </div>
              <div className={styles.tableWrapper}>
                <table className={styles.dataTable}>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>RRN</th>
                      <th>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {targetTransactions.map((txn) => (
                      <tr 
                        key={txn.sourceId}
                        className={cn(
                          selectedTarget?.sourceId === txn.sourceId && styles.selectedRow,
                          matchedPairs.some(p => p.target.sourceId === txn.sourceId) && styles.matchedRow
                        )}
                        onClick={() => !matchedPairs.some(p => p.target.sourceId === txn.sourceId) && handleSelectTarget(txn)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td className={styles.txnId}>{txn.sourceId}</td>
                        <td>{txn.rrn}</td>
                        <td className={styles.amount}>{formatCurrency(txn.amount)}</td>
                        <td>
                          {matchedPairs.some(p => p.target.sourceId === txn.sourceId) 
                            ? getStatusBadge('Matched')
                            : getStatusBadge(txn.status)
                          }
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card.Content>
          </Card>
        </div>

        {/* Match Confirmation Panel */}
        {showMatchPanel && selectedSource && selectedTarget && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className={styles.manualMatchPanel}
          >
            <Card>
              <Card.Header>
                <Card.Title>Confirm Match</Card.Title>
                <Card.Description>Review and confirm the transaction match</Card.Description>
              </Card.Header>
              <Card.Content>
                <div className={styles.matchPanelGrid}>
                  <div className={styles.matchSide}>
                    <div className={styles.matchSideTitle}>Source (POS)</div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Transaction ID</span>
                      <span className={styles.matchItemValue}>{selectedSource.sourceId}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>RRN</span>
                      <span className={styles.matchItemValue}>{selectedSource.rrn}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Amount</span>
                      <span className={styles.matchItemValue}>{formatCurrency(selectedSource.amount)}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Date</span>
                      <span className={styles.matchItemValue}>{formatDate(selectedSource.date)}</span>
                    </div>
                  </div>

                  <div className={styles.matchConnector}>
                    <div className={styles.connectorIcon}>
                      <ArrowRight size={24} />
                    </div>
                    {parseFloat(getVariance()) > 0 && (
                      <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                        <div style={{ color: 'var(--gold-600)', fontWeight: 600 }}>
                          Variance: ₹{getVariance()}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className={styles.matchSide}>
                    <div className={styles.matchSideTitle}>Target (CBS)</div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Transaction ID</span>
                      <span className={styles.matchItemValue}>{selectedTarget.sourceId}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>RRN</span>
                      <span className={styles.matchItemValue}>{selectedTarget.rrn}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Amount</span>
                      <span className={styles.matchItemValue}>{formatCurrency(selectedTarget.amount)}</span>
                    </div>
                    <div className={styles.matchItem}>
                      <span className={styles.matchItemLabel}>Date</span>
                      <span className={styles.matchItemValue}>{formatDate(selectedTarget.date)}</span>
                    </div>
                  </div>
                </div>
              </Card.Content>
              <Card.Footer>
                <Button variant="outline" onClick={handleRejectMatch}>
                  <XCircle size={18} />
                  Cancel
                </Button>
                <Button onClick={handleConfirmMatch}>
                  <CheckCircle size={18} />
                  Confirm Match
                </Button>
              </Card.Footer>
            </Card>
          </motion.div>
        )}

        {/* Instructions */}
        {!showMatchPanel && !selectedSource && !selectedTarget && (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>
              <Link2 size={48} />
            </div>
            <h3>Select Transactions to Match</h3>
            <p>Click on a source transaction (left) and a target transaction (right) to create a match</p>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default ManualReconciliation;
