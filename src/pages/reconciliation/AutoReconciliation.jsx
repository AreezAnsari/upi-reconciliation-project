import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Zap, Play, CheckCircle, XCircle, Clock, AlertTriangle, Settings, RefreshCw, TrendingUp } from 'lucide-react';
import { Button, Select, Card, Checkbox } from '../../components/common';
import { DUMMY_RECONCILIATION_SUMMARY, DUMMY_TRANSACTIONS } from '../../services/dummyData';
import { formatNumber, formatCurrency } from '../../utils/helpers';
import styles from './Reconciliation.module.css';

const AutoReconciliation = () => {
  const [selectedProcessType, setSelectedProcessType] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [reconProgress, setReconProgress] = useState(null);
  const [reconComplete, setReconComplete] = useState(false);
  const [reconResults, setReconResults] = useState(null);

  const processTypes = [
    { value: '', label: 'Select Process Type' },
    { value: 'debitcard', label: 'Debit Card' },
    { value: 'creditcard', label: 'Credit Card' },
    { value: 'upi', label: 'UPI' },
    { value: 'neft', label: 'NEFT' },
    { value: 'rtgs', label: 'RTGS' },
    { value: 'imps', label: 'IMPS' }
  ];

  const summary = DUMMY_RECONCILIATION_SUMMARY;

  const handleStartReconciliation = async () => {
    if (!selectedProcessType) return;
    
    setIsRunning(true);
    setReconComplete(false);
    setReconProgress({ 
      phase: 'Initializing',
      progress: 0,
      processed: 0,
      matched: 0,
      unmatched: 0,
      total: summary.totalTransactions
    });

    // Simulate reconciliation phases
    const phases = [
      { name: 'Loading transactions', duration: 1000 },
      { name: 'Validating data', duration: 800 },
      { name: 'Running matching rules', duration: 2000 },
      { name: 'Processing exceptions', duration: 1500 },
      { name: 'Finalizing results', duration: 1000 }
    ];

    let currentProgress = 0;
    const progressPerPhase = 100 / phases.length;

    for (const phase of phases) {
      setReconProgress(prev => ({ ...prev, phase: phase.name }));
      
      const steps = 10;
      const stepDuration = phase.duration / steps;
      
      for (let i = 0; i < steps; i++) {
        await new Promise(resolve => setTimeout(resolve, stepDuration));
        currentProgress += progressPerPhase / steps;
        
        const processed = Math.floor((currentProgress / 100) * summary.totalTransactions);
        const matched = Math.floor(processed * 0.916);
        
        setReconProgress(prev => ({
          ...prev,
          progress: Math.round(currentProgress),
          processed,
          matched,
          unmatched: processed - matched
        }));
      }
    }

    // Set final results
    setReconResults({
      totalProcessed: summary.totalTransactions,
      matched: summary.matchedTransactions,
      unmatched: summary.unmatchedTransactions,
      pending: summary.pendingTransactions,
      matchRate: summary.matchRate,
      processingTime: '2m 34s',
      rulesApplied: 12
    });
    
    setReconComplete(true);
    setIsRunning(false);
  };

  const handleReset = () => {
    setReconProgress(null);
    setReconComplete(false);
    setReconResults(null);
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
              <Zap size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Auto Reconciliation</h1>
              <p className={styles.pageSubtitle}>Automated transaction matching and reconciliation</p>
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
              <span className={styles.statLabel}>Total Transactions</span>
              <div className={`${styles.statIcon} ${styles.info}`}>
                <TrendingUp size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.totalTransactions)}</div>
          </motion.div>

          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Matched</span>
              <div className={`${styles.statIcon} ${styles.success}`}>
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
              <span className={styles.statLabel}>Unmatched</span>
              <div className={`${styles.statIcon} ${styles.error}`}>
                <XCircle size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.unmatchedTransactions)}</div>
          </motion.div>

          <motion.div 
            className={styles.statCard}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
          >
            <div className={styles.statHeader}>
              <span className={styles.statLabel}>Pending</span>
              <div className={`${styles.statIcon} ${styles.warning}`}>
                <Clock size={18} />
              </div>
            </div>
            <div className={styles.statValue}>{formatNumber(summary.pendingTransactions)}</div>
          </motion.div>
        </div>

        {/* Configuration Card */}
        {!isRunning && !reconComplete && (
          <Card className={styles.configCard}>
            <Card.Header>
              <Card.Title>Reconciliation Configuration</Card.Title>
              <Card.Description>Select process type and configure reconciliation parameters</Card.Description>
            </Card.Header>
            <Card.Content>
              <div className={styles.configGrid}>
                <Select
                  label="Process Type"
                  options={processTypes}
                  value={selectedProcessType}
                  onChange={(e) => setSelectedProcessType(e.target.value)}
                />
              </div>
            </Card.Content>
            <Card.Footer>
              <Button variant="outline">
                <Settings size={18} />
                Advanced Settings
              </Button>
              <Button 
                onClick={handleStartReconciliation}
                disabled={!selectedProcessType}
              >
                <Play size={18} />
                Start Auto Reconciliation
              </Button>
            </Card.Footer>
          </Card>
        )}

        {/* Progress Section */}
        {isRunning && reconProgress && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className={styles.progressSection}
          >
            <div className={styles.progressCard}>
              <div className={styles.progressHeader}>
                <h3 className={styles.progressTitle}>{reconProgress.phase}</h3>
                <span className={styles.progressPercentage}>{reconProgress.progress}%</span>
              </div>
              <div className={styles.progressBarContainer}>
                <motion.div 
                  className={styles.progressBarFill}
                  initial={{ width: 0 }}
                  animate={{ width: `${reconProgress.progress}%` }}
                  transition={{ duration: 0.3 }}
                />
              </div>
              <div className={styles.progressStats}>
                <div className={styles.progressStat}>
                  <div className={styles.progressStatValue}>{formatNumber(reconProgress.processed)}</div>
                  <div className={styles.progressStatLabel}>Processed</div>
                </div>
                <div className={styles.progressStat}>
                  <div className={styles.progressStatValue}>{formatNumber(reconProgress.matched)}</div>
                  <div className={styles.progressStatLabel}>Matched</div>
                </div>
                <div className={styles.progressStat}>
                  <div className={styles.progressStatValue}>{formatNumber(reconProgress.unmatched)}</div>
                  <div className={styles.progressStatLabel}>Unmatched</div>
                </div>
                <div className={styles.progressStat}>
                  <div className={styles.progressStatValue}>{formatNumber(reconProgress.total)}</div>
                  <div className={styles.progressStatLabel}>Total</div>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Results Section */}
        {reconComplete && reconResults && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          >
            <Card>
              <Card.Content>
                <div className={styles.successAnimation}>
                  <CheckCircle size={64} className={styles.successIcon} />
                  <h2 className={styles.successTitle}>Reconciliation Complete!</h2>
                  <p className={styles.successDescription}>
                    Successfully processed {formatNumber(reconResults.totalProcessed)} transactions
                  </p>
                </div>
              </Card.Content>
            </Card>

            <div className={styles.statsGrid} style={{ marginTop: '1.5rem' }}>
              <div className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statLabel}>Match Rate</span>
                  <div className={`${styles.statIcon} ${styles.success}`}>
                    <TrendingUp size={18} />
                  </div>
                </div>
                <div className={styles.statValue}>{reconResults.matchRate}%</div>
              </div>
              <div className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statLabel}>Processing Time</span>
                  <div className={`${styles.statIcon} ${styles.info}`}>
                    <Clock size={18} />
                  </div>
                </div>
                <div className={styles.statValue}>{reconResults.processingTime}</div>
              </div>
              <div className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statLabel}>Rules Applied</span>
                  <div className={`${styles.statIcon} ${styles.warning}`}>
                    <Settings size={18} />
                  </div>
                </div>
                <div className={styles.statValue}>{reconResults.rulesApplied}</div>
              </div>
              <div className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statLabel}>Exceptions</span>
                  <div className={`${styles.statIcon} ${styles.error}`}>
                    <AlertTriangle size={18} />
                  </div>
                </div>
                <div className={styles.statValue}>{formatNumber(reconResults.unmatched)}</div>
              </div>
            </div>

            <Card style={{ marginTop: '1.5rem' }}>
              <Card.Footer>
                <Button variant="outline" onClick={handleReset}>
                  <RefreshCw size={18} />
                  Run Another Reconciliation
                </Button>
                <Button>
                  View Detailed Report
                </Button>
              </Card.Footer>
            </Card>
          </motion.div>
        )}
      </motion.div>
    </div>
  );
};

export default AutoReconciliation;
