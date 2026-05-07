import { useState } from 'react';
import { motion } from 'framer-motion';
import { Split, Search, Plus, Trash2, CheckCircle, AlertTriangle } from 'lucide-react';
import { Button, Input, Select, Card } from '../../components/common';
import { formatCurrency } from '../../utils/helpers';
import styles from './Process.module.css';

const SplitTransaction = () => {
  const [transactionId, setTransactionId] = useState('');
  const [originalTransaction, setOriginalTransaction] = useState(null);
  const [splits, setSplits] = useState([
    { id: 1, amount: '', description: '', account: '' }
  ]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSplitting, setIsSplitting] = useState(false);
  const [splitSuccess, setSplitSuccess] = useState(false);

  const accountOptions = [
    { value: '', label: 'Select Account' },
    { value: 'pos_account', label: 'POS Account' },
    { value: 'cbs_account', label: 'CBS Account' },
    { value: 'settlement', label: 'Settlement Account' },
    { value: 'suspense', label: 'Suspense Account' }
  ];

  const handleSearch = async () => {
    if (!transactionId) return;
    
    setIsSearching(true);
    setSplitSuccess(false);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    setOriginalTransaction({
      id: transactionId,
      rrn: 'RRN' + Math.random().toString().slice(2, 14),
      amount: 125000.50,
      type: 'Debit Card',
      merchant: 'ABC Electronics',
      date: new Date().toISOString(),
      status: 'Pending'
    });
    setIsSearching(false);
  };

  const addSplit = () => {
    setSplits(prev => [...prev, { 
      id: prev.length + 1, 
      amount: '', 
      description: '', 
      account: '' 
    }]);
  };

  const removeSplit = (id) => {
    if (splits.length > 1) {
      setSplits(prev => prev.filter(s => s.id !== id));
    }
  };

  const updateSplit = (id, field, value) => {
    setSplits(prev => prev.map(s => 
      s.id === id ? { ...s, [field]: value } : s
    ));
  };

  const getTotalSplitAmount = () => {
    return splits.reduce((sum, split) => sum + (parseFloat(split.amount) || 0), 0);
  };

  const getRemainingAmount = () => {
    if (!originalTransaction) return 0;
    return originalTransaction.amount - getTotalSplitAmount();
  };

  const isValidSplit = () => {
    if (!originalTransaction) return false;
    const remaining = getRemainingAmount();
    return remaining === 0 && splits.every(s => s.amount && s.account);
  };

  const handleSplit = async () => {
    if (!isValidSplit()) return;
    
    setIsSplitting(true);
    await new Promise(resolve => setTimeout(resolve, 1500));
    setSplitSuccess(true);
    setIsSplitting(false);
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
              <Split size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>Split Transaction</h1>
              <p className={styles.pageSubtitle}>Split a single transaction into multiple entries</p>
            </div>
          </div>
        </div>

        {/* Search Transaction */}
        <Card className={styles.searchCard}>
          <Card.Header>
            <Card.Title>Find Transaction</Card.Title>
            <Card.Description>Enter transaction ID to split</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.searchRow}>
              <Input
                placeholder="Enter Transaction ID"
                value={transactionId}
                onChange={(e) => setTransactionId(e.target.value)}
                leftIcon={<Search size={18} />}
              />
              <Button onClick={handleSearch} loading={isSearching}>
                <Search size={18} />
                Find
              </Button>
            </div>
          </Card.Content>
        </Card>

        {/* Original Transaction Details */}
        {originalTransaction && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Card className={styles.originalTxnCard}>
              <Card.Header>
                <Card.Title>Original Transaction</Card.Title>
              </Card.Header>
              <Card.Content>
                <div className={styles.txnDetailsGrid}>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>Transaction ID</span>
                    <span className={styles.detailValue}>{originalTransaction.id}</span>
                  </div>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>RRN</span>
                    <span className={styles.detailValue}>{originalTransaction.rrn}</span>
                  </div>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>Amount</span>
                    <span className={`${styles.detailValue} ${styles.amountHighlight}`}>
                      {formatCurrency(originalTransaction.amount)}
                    </span>
                  </div>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>Type</span>
                    <span className={styles.detailValue}>{originalTransaction.type}</span>
                  </div>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>Merchant</span>
                    <span className={styles.detailValue}>{originalTransaction.merchant}</span>
                  </div>
                  <div className={styles.txnDetailItem}>
                    <span className={styles.detailLabel}>Status</span>
                    <span className={styles.detailValue}>{originalTransaction.status}</span>
                  </div>
                </div>
              </Card.Content>
            </Card>
          </motion.div>
        )}

        {/* Split Configuration */}
        {originalTransaction && !splitSuccess && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.1 }}
          >
            <Card className={styles.splitCard}>
              <Card.Header>
                <div className={styles.splitHeader}>
                  <div>
                    <Card.Title>Split Configuration</Card.Title>
                    <Card.Description>Define how to split the transaction</Card.Description>
                  </div>
                  <Button variant="outline" size="sm" onClick={addSplit}>
                    <Plus size={16} />
                    Add Split
                  </Button>
                </div>
              </Card.Header>
              <Card.Content>
                <div className={styles.splitsContainer}>
                  {splits.map((split, index) => (
                    <motion.div 
                      key={split.id}
                      className={styles.splitRow}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.1 }}
                    >
                      <div className={styles.splitNumber}>{index + 1}</div>
                      <div className={styles.splitInputs}>
                        <Input
                          label="Amount"
                          type="number"
                          placeholder="0.00"
                          value={split.amount}
                          onChange={(e) => updateSplit(split.id, 'amount', e.target.value)}
                        />
                        <Input
                          label="Description"
                          placeholder="Enter description"
                          value={split.description}
                          onChange={(e) => updateSplit(split.id, 'description', e.target.value)}
                        />
                        <Select
                          label="Target Account"
                          options={accountOptions}
                          value={split.account}
                          onChange={(e) => updateSplit(split.id, 'account', e.target.value)}
                        />
                      </div>
                      {splits.length > 1 && (
                        <button 
                          className={styles.removeSplitBtn}
                          onClick={() => removeSplit(split.id)}
                        >
                          <Trash2 size={18} />
                        </button>
                      )}
                    </motion.div>
                  ))}
                </div>

                {/* Split Summary */}
                <div className={styles.splitSummary}>
                  <div className={styles.summaryRow}>
                    <span>Original Amount:</span>
                    <span>{formatCurrency(originalTransaction.amount)}</span>
                  </div>
                  <div className={styles.summaryRow}>
                    <span>Total Split Amount:</span>
                    <span>{formatCurrency(getTotalSplitAmount())}</span>
                  </div>
                  <div className={`${styles.summaryRow} ${styles.remainingRow}`}>
                    <span>Remaining:</span>
                    <span className={getRemainingAmount() !== 0 ? styles.errorText : styles.successText}>
                      {formatCurrency(getRemainingAmount())}
                    </span>
                  </div>
                  {getRemainingAmount() !== 0 && (
                    <div className={styles.warningMessage}>
                      <AlertTriangle size={16} />
                      <span>Split amounts must equal original transaction amount</span>
                    </div>
                  )}
                </div>
              </Card.Content>
              <Card.Footer className={styles.splitActions}>
                <Button 
                  variant="outline" 
                  onClick={() => setSplits([{ id: 1, amount: '', description: '', account: '' }])}
                >
                  Reset
                </Button>
                <Button 
                  onClick={handleSplit}
                  loading={isSplitting}
                  disabled={!isValidSplit()}
                >
                  <Split size={18} />
                  Execute Split
                </Button>
              </Card.Footer>
            </Card>
          </motion.div>
        )}

        {/* Success Message */}
        {splitSuccess && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.3 }}
          >
            <Card className={styles.successCard}>
              <Card.Content>
                <div className={styles.successContent}>
                  <CheckCircle size={64} className={styles.successIcon} />
                  <h2>Transaction Split Successfully!</h2>
                  <p>The transaction has been split into {splits.length} entries</p>
                  <Button onClick={() => {
                    setOriginalTransaction(null);
                    setTransactionId('');
                    setSplits([{ id: 1, amount: '', description: '', account: '' }]);
                    setSplitSuccess(false);
                  }}>
                    Split Another Transaction
                  </Button>
                </div>
              </Card.Content>
            </Card>
          </motion.div>
        )}
      </motion.div>
    </div>
  );
};

export default SplitTransaction;
