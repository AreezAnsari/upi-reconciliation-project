import { useState } from 'react';
import { motion } from 'framer-motion';
import { Activity, CheckCircle, XCircle, Clock, AlertTriangle, RefreshCw, Download, Filter, ChevronLeft, ChevronRight } from 'lucide-react';
import { Button, Select, Card } from '../../components/common';
import { DUMMY_RECONCILIATION_SUMMARY, DUMMY_TRANSACTIONS, DUMMY_RECENT_ACTIVITY } from '../../services/dummyData';
import { formatDate, formatDateTime, formatNumber, formatCurrency, cn, getRelativeTime } from '../../utils/helpers';
import styles from './Reconciliation.module.css';

const ReconciliationStatus = () => {
  const [selectedFilter, setSelectedFilter] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const summary = DUMMY_RECONCILIATION_SUMMARY;
  const activities = DUMMY_RECENT_ACTIVITY;

  const filterOptions = [
    { value: 'all', label: 'All Status' },
    { value: 'matched', label: 'Matched' },
    { value: 'unmatched', label: 'Unmatched' },
    { value: 'pending', label: 'Pending' },
    { value: 'error', label: 'Error' }
  ];

  // Extended transaction data with recon details
  const reconTransactions = DUMMY_TRANSACTIONS.flatMap(t => [
    {
      ...t,
      reconId: `RECON-${Math.random().toString(36).slice(2, 10).toUpperCase()}`,
      reconStatus: t.status,
      reconDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      matchType: t.status === 'Matched' ? 'Auto' : (t.status === 'Pending' ? 'Manual' : 'N/A'),
      processedBy: 'System'
    },
    {
      id: `TXN-${Math.random().toString(36).slice(2, 10).toUpperCase()}`,
      rrn: `RRN${Math.random().toString().slice(2, 14)}`,
      amount: Math.random() * 50000 + 1000,
      type: ['Debit Card', 'Credit Card', 'UPI'][Math.floor(Math.random() * 3)],
      date: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      status: ['Matched', 'Unmatched', 'Pending', 'Error'][Math.floor(Math.random() * 4)],
      reconId: `RECON-${Math.random().toString(36).slice(2, 10).toUpperCase()}`,
      reconStatus: ['Matched', 'Unmatched', 'Pending', 'Error'][Math.floor(Math.random() * 4)],
      reconDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      matchType: Math.random() > 0.5 ? 'Auto' : 'Manual',
      processedBy: Math.random() > 0.7 ? 'Admin User' : 'System'
    }
  ]);

  const filteredTransactions = selectedFilter === 'all' 
    ? reconTransactions 
    : reconTransactions.filter(t => t.reconStatus.toLowerCase() === selectedFilter);

  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const paginatedTransactions = filteredTransactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

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

  const getActivityIcon = (type) => {
    switch (type) {
      case 'success': return <CheckCircle size={16} />;
      case 'warning': return <AlertTriangle size={16} />;
      case 'error': return <XCircle size={16} />;
      default: return <Activity size={16} />;
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
              <Activity size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Reconciliation Status</h1>
              <p className={styles.pageSubtitle}>Monitor reconciliation progress and status</p>
            </div>
          </div>
        </div>

        {/* Stats Overview */}
        <div className={styles.statsGrid}>
          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Match Rate</span>
              <div className={`${styles.statIcon} ${styles.success}`}>
                <CheckCircle size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{summary.matchRate}%</div>
            <div className={`${styles.statChange} ${styles.positive}`}>
              ↑ 2.3% from last week
            </div>
          </motion.div>

          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Matched Today</span>
              <div className={`${styles.statIcon} ${styles.info}`}>
                <CheckCircle size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.matchedTransactions)}</div>
          </motion.div>

          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Pending Review</span>
              <div className={`${styles.statIcon} ${styles.warning}`}>
                <Clock size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.pendingTransactions)}</div>
          </motion.div>

          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Exceptions</span>
              <div className={`${styles.statIcon} ${styles.error}`}>
                <AlertTriangle size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.unmatchedTransactions)}</div>
          </motion.div>
        </div>

        {/* Progress Card */}
        <div className={styles.progressSection}>
          <div className={styles.progressCard}>
            <div className={styles.progressHeader}>
              <h3 className={styles.progressTitle}>Today's Reconciliation Progress</h3>
              <span className={styles.progressPercentage}>{summary.matchRate}%</span>
            </div>
            <div className={styles.progressBarContainer}>
              <motion.div 
                className={styles.progressBarFill}
                initial={{ width: 0 }}
                animate={{ width: `${summary.matchRate}%` }}
                transition={{ duration: 1, delay: 0.5 }}
              />
            </div>
            <div className={styles.progressStats}>
              <div className={styles.progressStat}>
                <div className={styles.progressStatValue}>{formatNumber(summary.totalTransactions)}</div>
                <div className={styles.progressStatLabel}>Total</div>
              </div>
              <div className={styles.progressStat}>
                <div className={styles.progressStatValue}>{formatNumber(summary.matchedTransactions)}</div>
                <div className={styles.progressStatLabel}>Matched</div>
              </div>
              <div className={styles.progressStat}>
                <div className={styles.progressStatValue}>{formatNumber(summary.unmatchedTransactions)}</div>
                <div className={styles.progressStatLabel}>Unmatched</div>
              </div>
              <div className={styles.progressStat}>
                <div className={styles.progressStatValue}>{formatNumber(summary.pendingTransactions)}</div>
                <div className={styles.progressStatLabel}>Pending</div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
          {/* Transactions Table */}
          <Card className={styles.tableCard}>
            <Card.Header>
              <div className={styles.tableHeader}>
                <div>
                  <Card.Title>Reconciliation Records</Card.Title>
                  <Card.Description>{filteredTransactions.length} records</Card.Description>
                </div>
                <div className={styles.tableActions}>
                  <Select
                    options={filterOptions}
                    value={selectedFilter}
                    onChange={(e) => {
                      setSelectedFilter(e.target.value);
                      setCurrentPage(1);
                    }}
                  />
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
                      <th>Recon ID</th>
                      <th>Transaction</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Match Type</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedTransactions.map((txn, idx) => (
                      <tr key={`${txn.reconId}-${idx}`}>
                        <td className={styles.txnId}>{txn.reconId}</td>
                        <td>{txn.id}</td>
                        <td className={styles.amount}>{formatCurrency(txn.amount)}</td>
                        <td>{getStatusBadge(txn.reconStatus)}</td>
                        <td>{txn.matchType}</td>
                        <td>{formatDate(txn.reconDate)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className={styles.pagination}>
                <span className={styles.paginationInfo}>
                  Showing {((currentPage - 1) * itemsPerPage) + 1} to {Math.min(currentPage * itemsPerPage, filteredTransactions.length)} of {filteredTransactions.length}
                </span>
                <div className={styles.paginationButtons}>
                  <button 
                    className={styles.pageBtn}
                    onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                  >
                    <ChevronLeft size={16} />
                  </button>
                  {[...Array(Math.min(5, totalPages))].map((_, i) => {
                    const pageNum = i + 1;
                    return (
                      <button
                        key={pageNum}
                        className={cn(styles.pageBtn, currentPage === pageNum && styles.active)}
                        onClick={() => setCurrentPage(pageNum)}
                      >
                        {pageNum}
                      </button>
                    );
                  })}
                  <button 
                    className={styles.pageBtn}
                    onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                    disabled={currentPage === totalPages}
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            </Card.Content>
          </Card>

          {/* Activity Timeline */}
          <Card>
            <Card.Header>
              <Card.Title>Recent Activity</Card.Title>
              <Card.Description>Latest reconciliation events</Card.Description>
            </Card.Header>
            <Card.Content>
              <div className={styles.timeline}>
                {activities.map((activity) => (
                  <div key={activity.id} className={styles.timelineItem}>
                    <div className={cn(styles.timelineDot, styles[activity.type])}>
                      {getActivityIcon(activity.type)}
                    </div>
                    <div className={styles.timelineContent}>
                      <p className={styles.timelineTitle}>{activity.action}</p>
                      <p className={styles.timelineDescription}>{activity.details}</p>
                      <span className={styles.timelineTime}>{getRelativeTime(activity.timestamp)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </Card.Content>
            <Card.Footer>
              <Button variant="ghost" style={{ width: '100%' }}>
                View All Activity
              </Button>
            </Card.Footer>
          </Card>
        </div>
      </motion.div>
    </div>
  );
};

export default ReconciliationStatus;
