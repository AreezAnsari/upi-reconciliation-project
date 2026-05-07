import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { FileText, Play, CheckCircle, Clock, AlertCircle, Folder, Database, RefreshCw } from 'lucide-react';
import { Button, Card } from '../../components/common';
import { formatNumber } from '../../utils/helpers';
import styles from './Extraction.module.css';

const FileProcessing = () => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [processComplete, setProcessComplete] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState(0);
  const [results, setResults] = useState(null);
  const [logs, setLogs] = useState([]);

  // Get process ID from URL params (simulated)
  const processId = '22950354274';
  const filePath = '/app/jpbrecon/JPB_RECON/DCRSFILES/DEBITCARD/POS_RAW';

  const steps = [
    { title: 'Initializing', description: 'Setting up extraction environment' },
    { title: 'Reading Files', description: 'Loading raw data files' },
    { title: 'Parsing Data', description: 'Extracting transaction records' },
    { title: 'Validating', description: 'Checking data integrity' },
    { title: 'Storing', description: 'Saving to database' },
    { title: 'Completing', description: 'Finalizing process' }
  ];

  const addLog = (message, type = 'info') => {
    setLogs(prev => [...prev, { message, type, timestamp: new Date().toISOString() }]);
  };

  const handleStartProcess = async () => {
    setIsProcessing(true);
    setProcessComplete(false);
    setProgress(0);
    setCurrentStep(0);
    setLogs([]);
    
    addLog('Starting file processing execution...', 'info');
    addLog(`Process ID: ${processId}`, 'info');
    addLog(`File Path: ${filePath}`, 'info');

    const stepDuration = 1500;

    for (let i = 0; i < steps.length; i++) {
      setCurrentStep(i);
      addLog(`${steps[i].title}: ${steps[i].description}`, 'info');

      const stepProgress = ((i + 1) / steps.length) * 100;
      const subSteps = 10;
      
      for (let j = 0; j < subSteps; j++) {
        await new Promise(resolve => setTimeout(resolve, stepDuration / subSteps));
        setProgress(((i * subSteps + j + 1) / (steps.length * subSteps)) * 100);
      }

      if (i === 1) {
        addLog('Found 3 data files in directory', 'success');
      }
      if (i === 2) {
        addLog('Parsed 15,847 transaction records', 'success');
      }
      if (i === 3) {
        addLog('Validation complete - 2 warnings found', 'warning');
      }
      if (i === 4) {
        addLog('Database insertion successful', 'success');
      }
    }

    addLog('File processing completed successfully!', 'success');
    
    setResults({
      totalRecords: 15847,
      successfulRecords: 15823,
      failedRecords: 24,
      processingTime: '2m 18s',
      filesProcessed: 3
    });
    
    setProcessComplete(true);
    setIsProcessing(false);
  };

  const handleReset = () => {
    setIsProcessing(false);
    setProcessComplete(false);
    setProgress(0);
    setCurrentStep(0);
    setResults(null);
    setLogs([]);
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
              <h1 className={styles.pageTitle}>File Processing</h1>
              <p className={styles.pageSubtitle}>Execute file extraction and processing</p>
            </div>
          </div>
        </div>

        {/* File Path Display */}
        <Card className={styles.filePathCard}>
          <Card.Header>
            <Card.Title>File Path</Card.Title>
          </Card.Header>
          <Card.Content>
            <div className={styles.filePath}>
              <Folder size={20} className={styles.filePathIcon} />
              <span className={styles.filePathText}>{filePath}</span>
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
            <div className={styles.infoValue}>Debit Card</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>File Type</div>
            <div className={styles.infoValue}>POS Raw</div>
          </div>
          <div className={styles.infoItem}>
            <div className={styles.infoLabel}>Status</div>
            <div className={styles.infoValue}>
              {processComplete ? 'Completed' : isProcessing ? 'Processing' : 'Ready'}
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
                  Click the button below to start the file extraction process
                </p>
                <Button onClick={handleStartProcess} size="lg">
                  <Play size={20} />
                  Start Execution Process
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
            <Card>
              <Card.Content>
                <div className={styles.successState}>
                  <CheckCircle size={64} className={styles.successIcon} />
                  <h2 className={styles.successTitle}>Processing Complete!</h2>
                  <p className={styles.successDescription}>
                    Successfully processed {formatNumber(results.successfulRecords)} records
                  </p>
                </div>
              </Card.Content>
            </Card>

            <Card className={styles.resultsCard}>
              <Card.Header>
                <Card.Title>Processing Results</Card.Title>
              </Card.Header>
              <Card.Content>
                <div className={styles.resultsGrid}>
                  <div className={styles.resultItem}>
                    <div className={styles.resultValue}>{formatNumber(results.totalRecords)}</div>
                    <div className={styles.resultLabel}>Total Records</div>
                  </div>
                  <div className={styles.resultItem}>
                    <div className={`${styles.resultValue} ${styles.success}`}>
                      {formatNumber(results.successfulRecords)}
                    </div>
                    <div className={styles.resultLabel}>Successful</div>
                  </div>
                  <div className={styles.resultItem}>
                    <div className={`${styles.resultValue} ${styles.error}`}>
                      {formatNumber(results.failedRecords)}
                    </div>
                    <div className={styles.resultLabel}>Failed</div>
                  </div>
                  <div className={styles.resultItem}>
                    <div className={styles.resultValue}>{results.processingTime}</div>
                    <div className={styles.resultLabel}>Processing Time</div>
                  </div>
                </div>
              </Card.Content>
              <Card.Footer>
                <Button variant="outline" onClick={handleReset}>
                  <RefreshCw size={18} />
                  Process Another File
                </Button>
                <Button>
                  View Detailed Report
                </Button>
              </Card.Footer>
            </Card>
          </motion.div>
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

export default FileProcessing;
