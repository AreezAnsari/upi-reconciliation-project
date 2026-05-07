import { useState, useRef } from 'react';
import { motion } from 'framer-motion';
import { Upload, CheckCircle, AlertTriangle, Info, XCircle, Download } from 'lucide-react';
import { PageHeader } from '../../../components/common';
import styles from './MakerUpload.module.css';

const FILE_TYPES = [
  { id: 'npci', title: 'NPCI Adj Report', sub: 'Adjustment reports' },
  { id: 'debit', title: 'Debit Consent', sub: 'Customer consent' },
  { id: 'merchant', title: 'Merchant Response', sub: 'Partner response' },
];

const PROCESSING_MODES = [
  { id: 'auto', title: 'Auto Process', sub: 'Ingest & validate' },
  { id: 'validate', title: 'Validate Only', sub: 'Preview before push' },
];

const LOG_ENTRIES = [
  { type: 'success', icon: <CheckCircle size={12} />, msg: 'File format validated — CSV with 18 columns detected', detail: 'Header check: PASS | Encoding: UTF-8', time: '10:42:01' },
  { type: 'success', icon: <CheckCircle size={12} />, msg: '1,247 records parsed successfully', detail: 'Parse time: 2.3s | Avg row size: 412 bytes', time: '10:42:03' },
  { type: 'warn', icon: <AlertTriangle size={12} />, msg: '43 duplicate records identified — AdjRef ID + UTXN ID match', detail: 'Rows: 102, 234, 456, 512, 678... (38 more)', time: '10:42:05' },
  { type: 'info', icon: <Info size={12} />, msg: 'Merchant mapping in progress — querying Partner DB', detail: 'Mapped: 892/1204 | Pending: 312', time: '10:42:08' },
  { type: 'error', icon: <XCircle size={12} />, msg: '15 records failed validation — missing mandatory fields', detail: 'Missing: RRN (8), Amount (4), TxnDate (3)', time: '10:42:10' },
  { type: 'success', icon: <CheckCircle size={12} />, msg: 'TAT master mapping applied — stage assignment complete', detail: 'Early: 423 | Intermediary: 398 | Terminal: 368', time: '10:42:12' },
  { type: 'success', icon: <CheckCircle size={12} />, msg: 'Financial priority flags applied', detail: 'Financial: 987 | Non-Financial: 260', time: '10:42:14' },
  { type: 'info', icon: <Info size={12} />, msg: 'Evidence code mapping — checking Evidence Bank', detail: 'Matched: 234 | Pending: 955', time: '10:42:16' },
];

const SUMMARY = [
  { value: '1,247', label: 'Total Records', accent: 'blue' },
  { value: '1,189', label: 'Valid Records', accent: 'green' },
  { value: '43', label: 'Duplicates Found', accent: 'gold' },
  { value: '15', label: 'Errors', accent: 'red' },
];

const MakerUpload = () => {
  const [fileType, setFileType] = useState('npci');
  const [procMode, setProcMode] = useState('auto');
  const [dragging, setDragging] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  const fileInputRef = useRef(null);
  const [product, setProduct] = useState('UPI');
  const [cycleDate, setCycleDate] = useState('2026-04-10');

  const handleDragOver = (e) => { e.preventDefault(); setDragging(true); };
  const handleDragLeave = () => setDragging(false);
  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) setUploadedFile(file);
  };
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) setUploadedFile(file);
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="Maker Upload Portal"
        description="Upload and validate dispute files for checker approval"
      >
        <span className={styles.roleBadge}>Role: OPS MAKER</span>
      </PageHeader>

      <motion.div
        className={styles.uploadSection}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {/* Drag-and-Drop Zone */}
        <div
          className={`${styles.dropZone} ${dragging ? styles.dragover : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept=".csv,.xlsx,.txt"
            onChange={handleFileChange}
          />
          <div className={styles.dropIcon}>
            <Upload size={28} />
          </div>
          {uploadedFile ? (
            <>
              <div className={styles.dropTitle}>{uploadedFile.name}</div>
              <div className={styles.dropSub}>{(uploadedFile.size / 1024).toFixed(1)} KB — Click to change</div>
            </>
          ) : (
            <>
              <div className={styles.dropTitle}>Drag &amp; Drop Files Here</div>
              <div className={styles.dropSub}>or click to browse your computer</div>
              <button className={styles.dropBtn} onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}>
                Browse Files
              </button>
              <div className={styles.dropMeta}>Supported: .csv, .xlsx, .txt &middot; Max 50MB per file</div>
            </>
          )}
        </div>

        {/* Upload Configuration */}
        <div className={styles.fileConfig}>
          <h3 className={styles.configTitle}>Upload Configuration</h3>

          <div className={styles.configGroup}>
            <label className={styles.configLabel}>File Type</label>
            <div className={styles.radioGroup}>
              {FILE_TYPES.map((ft) => (
                <label
                  key={ft.id}
                  className={`${styles.radioCard} ${fileType === ft.id ? styles.radioSelected : ''}`}
                  onClick={() => setFileType(ft.id)}
                >
                  <input type="radio" name="fileType" checked={fileType === ft.id} readOnly style={{ display: 'none' }} />
                  <div className={styles.rcTitle}>{ft.title}</div>
                  <div className={styles.rcSub}>{ft.sub}</div>
                </label>
              ))}
            </div>
          </div>

          <div className={styles.configGroup}>
            <label className={styles.configLabel}>Product</label>
            <select className={styles.configInput} value={product} onChange={(e) => setProduct(e.target.value)}>
              <option>UPI</option><option>IMPS</option><option>AEPS</option>
            </select>
          </div>

          <div className={styles.configGroup}>
            <label className={styles.configLabel}>Settlement Cycle Date</label>
            <input className={styles.configInput} type="date" value={cycleDate} onChange={(e) => setCycleDate(e.target.value)} />
          </div>

          <div className={styles.configGroup}>
            <label className={styles.configLabel}>Processing Mode</label>
            <div className={styles.radioGroup}>
              {PROCESSING_MODES.map((pm) => (
                <label
                  key={pm.id}
                  className={`${styles.radioCard} ${procMode === pm.id ? styles.radioSelected : ''}`}
                  onClick={() => setProcMode(pm.id)}
                >
                  <input type="radio" name="procMode" checked={procMode === pm.id} readOnly style={{ display: 'none' }} />
                  <div className={styles.rcTitle}>{pm.title}</div>
                  <div className={styles.rcSub}>{pm.sub}</div>
                </label>
              ))}
            </div>
          </div>
        </div>
      </motion.div>

      {/* Validation Log */}
      <motion.div
        className={styles.validationSection}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15, duration: 0.3 }}
      >
        <h3 className={styles.validationTitle}>Real-Time Validation Log</h3>
        <p className={styles.validationSub}>File: NPCI_ADJ_UPI_20260410_SC1.csv &middot; Processing...</p>

        <div className={styles.progressBar}>
          <div className={styles.progressFill} style={{ width: '78%' }} />
        </div>

        <div className={styles.summaryRow}>
          {SUMMARY.map((s) => (
            <div key={s.label} className={`${styles.summaryCard} ${styles[`s_${s.accent}`]}`}>
              <div className={styles.summaryValue}>{s.value}</div>
              <div className={styles.summaryLabel}>{s.label}</div>
            </div>
          ))}
        </div>

        <div className={styles.logEntries}>
          {LOG_ENTRIES.map((entry, i) => (
            <div key={i} className={styles.logEntry}>
              <div className={`${styles.logIcon} ${styles[`log_${entry.type}`]}`}>
                {entry.icon}
              </div>
              <div className={styles.logContent}>
                <div className={styles.logMsg}>{entry.msg}</div>
                <div className={styles.logDetail}>{entry.detail}</div>
              </div>
              <div className={styles.logTime}>{entry.time}</div>
            </div>
          ))}
        </div>

        <div className={styles.actionBar}>
          <span className={styles.processingNote}>Processing: 78% complete &middot; ETA: ~45s</span>
          <div className={styles.actionBarBtns}>
            <button className={styles.btnOutline}><Download size={13} /> Download Error Report</button>
            <button className={styles.btnGreen}>Submit for Checker Approval →</button>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default MakerUpload;
