import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Info, Plus, Upload, Copy, X, Trash2, GripVertical, CheckCircle,
  AlertCircle, ChevronDown, Save, ArrowRight, Server, FolderOpen,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../../components/common';
import styles from './AddTemplate.module.css';

// ─── Constants ────────────────────────────────────────────────────────────────
const STEPS = [
  { num: 1, label: 'Template Header',  sub: 'File type & settings' },
  { num: 2, label: 'Field Details',    sub: 'Define columns' },
  { num: 3, label: 'Recon Mapping',    sub: 'Optional key mapping' },
  { num: 4, label: 'File Delivery',    sub: 'SFTP or manual' },
  { num: 5, label: 'Encryption',       sub: 'Security settings' },
  { num: 6, label: 'Schedule & Validate', sub: 'Automate & test' },
];

const FIELD_TYPES = ['String', 'Number', 'Date', 'Decimal', 'Boolean', 'Amount'];
const SFTP_SERVERS = [
  { value: 'SFTP_NPCI_PROD',    label: 'SFTP_NPCI_PROD',    ip: '10.20.30.40', env: 'Production' },
  { value: 'SFTP_CBS_PRIMARY',  label: 'SFTP_CBS_PRIMARY',  ip: '10.20.30.41', env: 'Production' },
  { value: 'SFTP_MERCHANT_GW',  label: 'SFTP_MERCHANT_GW',  ip: '10.20.30.42', env: 'Production' },
  { value: 'SFTP_SWITCH_DR',    label: 'SFTP_SWITCH_DR',    ip: '10.20.30.43', env: 'DR' },
  { value: 'SFTP_GATEWAY_01',   label: 'SFTP_GATEWAY_01',   ip: '10.20.30.44', env: 'Production' },
];

const RECON_MAP_FIELDS = [
  { id: 'reconTxnDate',  label: 'Transaction Date Field',    required: true  },
  { id: 'reconTxnAmt',   label: 'Transaction Amount Field',  required: true  },
  { id: 'reconTxnId',    label: 'Transaction ID / UTR Field',required: true  },
  { id: 'reconDrCr',     label: 'Dr / Cr Indicator Field',   required: false },
  { id: 'reconRRN',      label: 'RRN / Reference No Field',  required: false },
  { id: 'reconAcctNo',   label: 'Account Number Field',      required: false },
  { id: 'reconChannel',  label: 'Channel / Source Field',    required: false },
  { id: 'reconStatus',   label: 'Status Field',              required: false },
];

const DRY_RUN_LOG = [
  { type: 'info', msg: '[10:42:01] Initialising dry-run validation for template TMPL-0015…' },
  { type: 'ok',   msg: '[10:42:01] File pattern check: PASS — TXN_*.csv matched 3 files in path.' },
  { type: 'ok',   msg: '[10:42:02] Parsing sample file: TXN_20260414_001.csv (1,247 records)' },
  { type: 'ok',   msg: '[10:42:03] Header row found at line 1. 14 columns detected.' },
  { type: 'warn', msg: '[10:42:03] WARN: 43 records have missing TXN_AMT — will use default 0.00' },
  { type: 'ok',   msg: '[10:42:04] Recon key mapping validated — RRN, TXN_AMT, TXN_DATE fields resolved.' },
  { type: 'ok',   msg: '[10:42:05] SFTP connectivity test: PASS — connected to SFTP_NPCI_PROD.' },
  { type: 'err',  msg: '[10:42:06] ERROR: 15 records failed mandatory field check on TXN_ID.' },
  { type: 'ok',   msg: '[10:42:07] Encryption: AES-256 key loaded successfully.' },
  { type: 'info', msg: '[10:42:08] Dry-run complete — 1,189 records valid, 58 flagged for review.' },
];

let fieldIdCounter = 3;

// ─── Component ────────────────────────────────────────────────────────────────
const AddTemplate = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  // Section refs for scroll-to
  const sectionRefs = {
    sec1: useRef(null),
    sec2: useRef(null),
    sec3: useRef(null),
    sec4: useRef(null),
    sec5: useRef(null),
    sec6: useRef(null),
  };

  // Section collapse state
  const [collapsed, setCollapsed] = useState({ sec3: true, sec5: true });
  // Active step (visual tracker)
  const [activeStep, setActiveStep] = useState(1);
  // Clone banner
  const [showClone, setShowClone] = useState(true);
  const [showCloneModal, setShowCloneModal] = useState(false);
  // Validation errors
  const [errors, setErrors] = useState({});
  const [showValSummary, setShowValSummary] = useState(false);

  // ── Section 1: Template Header ──
  const [tmplType,     setTmplType]     = useState('');
  const [tmplName,     setTmplName]     = useState('');
  const [description,  setDescription]  = useState('');
  const [sourceSystem, setSourceSystem] = useState('');
  const [filePath,     setFilePath]     = useState('');
  const [delimiter,    setDelimiter]    = useState(',');
  const [customDelim,  setCustomDelim]  = useState('');
  const [textQualifier,setTextQualifier]= useState('');
  const [xmlRoot,      setXmlRoot]      = useState('');
  const [xmlRow,       setXmlRow]       = useState('');
  const [xmlNs,        setXmlNs]        = useState('');
  const [recordLen,    setRecordLen]    = useState('');
  const [padChar,      setPadChar]      = useState('space');
  const [encoding,     setEncoding]     = useState('UTF-8');
  const [headerRow,    setHeaderRow]    = useState('Y');
  const [skipHeader,   setSkipHeader]   = useState(0);
  const [skipFooter,   setSkipFooter]   = useState(0);
  const [freq,         setFreq]         = useState('');
  const [filePattern,  setFilePattern]  = useState('');
  const [dateFormat,   setDateFormat]   = useState('DD/MM/YYYY');
  const [amtFormat,    setAmtFormat]    = useState('decimal');
  const [revInd,       setRevInd]       = useState('N');
  const [dupCheck,     setDupCheck]     = useState('Y');
  const [revField,     setRevField]     = useState('');
  const [revValue,     setRevValue]     = useState('');
  const [revAmt,       setRevAmt]       = useState('negate');

  // ── Section 2: Fields ──
  const [fields, setFields] = useState([
    { id: 1, name: 'TXN_DATE',   type: 'Date',    format: 'DD/MM/YYYY', len: 10, from: 1,  to: 10,  pk: false, reconKey: true,  mandatory: true,  trim: true,  def: '' },
    { id: 2, name: 'TXN_AMT',    type: 'Decimal', format: '',           len: 15, from: 11, to: 25,  pk: false, reconKey: true,  mandatory: true,  trim: false, def: '0.00' },
    { id: 3, name: 'RRN',        type: 'String',  format: '',           len: 12, from: 26, to: 37,  pk: true,  reconKey: true,  mandatory: true,  trim: true,  def: '' },
  ]);
  const [uploadPreview, setUploadPreview] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadPct, setUploadPct] = useState(0);

  // ── Section 3: Recon Mapping ──
  const [reconMap, setReconMap] = useState({});

  // ── Section 4: Delivery ──
  const [deliveryMode,     setDeliveryMode]     = useState('sftp');
  const [sftpServer,       setSftpServer]       = useState('');
  const [sftpProtocol,     setSftpProtocol]     = useState('SFTP');
  const [sftpRemotePath,   setSftpRemotePath]   = useState('');
  const [showCustomSftp,   setShowCustomSftp]   = useState(false);
  const [sftpIp,           setSftpIp]           = useState('');
  const [sftpPort,         setSftpPort]         = useState(22);
  const [sftpUser,         setSftpUser]         = useState('');
  const [sftpAuthType,     setSftpAuthType]     = useState('password');
  const [sftpPass,         setSftpPass]         = useState('');
  const [sftpPattern,      setSftpPattern]      = useState('');
  const [sftpArchive,      setSftpArchive]      = useState('');
  const [showTestModal,    setShowTestModal]    = useState(false);
  const [testResult,       setTestResult]       = useState(null);
  const [testing,          setTesting]          = useState(false);

  // ── Section 5: Encryption ──
  const [encEnabled,  setEncEnabled]  = useState(false);
  const [encType,     setEncType]     = useState('AES-256');
  const [encKeyMode,  setEncKeyMode]  = useState('system');

  // ── Section 6: Schedule & Validate ──
  const [schedEnabled,  setSchedEnabled]  = useState(true);
  const [schedTime,     setSchedTime]     = useState('06:00');
  const [schedDays,     setSchedDays]     = useState([0,1,2,3,4]);
  const [showDryRun,    setShowDryRun]    = useState(false);

  // ─── Helpers ──────────────────────────────────────────────────────────────
  const toggleSection = (id) => setCollapsed(p => ({ ...p, [id]: !p[id] }));

  const goToStep = (num) => {
    setActiveStep(num);
    const secId = `sec${num}`;
    // Expand section if collapsed
    setCollapsed(p => ({ ...p, [secId]: false }));
    // Wait one tick for AnimatePresence to start opening, then scroll
    setTimeout(() => {
      sectionRefs[secId]?.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 60);
  };

  const addField = () => {
    setFields(p => [...p, { id: ++fieldIdCounter, name: '', type: 'String', format: '', len: '', from: '', to: '', pk: false, reconKey: false, mandatory: false, trim: false, def: '' }]);
  };

  const removeField = (id) => setFields(p => p.filter(f => f.id !== id));

  const cloneField  = (f) => {
    const newId = ++fieldIdCounter;
    setFields(p => [...p, { ...f, id: newId, name: f.name + '_copy' }]);
  };

  const updateField = (id, key, val) => setFields(p => p.map(f => f.id === id ? { ...f, [key]: val } : f));

  const clearFields = () => setFields([]);

  const fieldOptions = () => ['— Select field —', ...fields.map(f => f.name).filter(Boolean)];

  const selectedSftp = SFTP_SERVERS.find(s => s.value === sftpServer);

  const onSftpSelect = (val) => {
    setSftpServer(val);
    setShowCustomSftp(val === 'custom');
    setTestResult(null);
  };

  const handleTestConnection = () => {
    setTesting(true);
    setTestResult(null);
    setTimeout(() => {
      setTesting(false);
      setTestResult('ok');
    }, 1600);
  };

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    setUploadPct(0);
    const interval = setInterval(() => {
      setUploadPct(p => {
        if (p >= 100) { clearInterval(interval); setUploading(false); return 100; }
        return p + 20;
      });
    }, 200);
    setTimeout(() => {
      setUploadPreview({ name: file.name, cols: ['TXN_DATE','TXN_AMT','RRN','MERCHANT_ID','STATUS'], rows: [['15-04-2026','4500.00','412908765432','MER001','SUCCESS'],['15-04-2026','1200.00','412908765433','MER002','FAILED']] });
    }, 1100);
  };

  const confirmUpload = () => {
    if (!uploadPreview) return;
    setFields(uploadPreview.cols.map((col, i) => ({ id: ++fieldIdCounter, name: col, type: 'String', format: '', len: '', from: '', to: '', pk: false, reconKey: false, mandatory: true, trim: true, def: '' })));
    setUploadPreview(null);
  };

  const validate = () => {
    const errs = {};
    if (!tmplType)     errs.tmplType     = 'Template type is required';
    if (!tmplName)     errs.tmplName     = 'Template name is required';
    if (!sourceSystem) errs.sourceSystem = 'Source system is required';
    if (!filePath)     errs.filePath     = 'File path is required';
    if (!freq)         errs.freq         = 'Frequency is required';
    if (fields.filter(f => f.name.trim()).length === 0) errs.fields = 'At least one field must be defined';
    setErrors(errs);
    setShowValSummary(Object.keys(errs).length > 0);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = () => { if (validate()) navigate('/new-config/role-approval-sent'); };
  const handleDraft  = () => {};

  const DAYS_LABELS = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const toggleDay   = (i) => setSchedDays(p => p.includes(i) ? p.filter(d => d !== i) : [...p, i]);

  // ─── Render ───────────────────────────────────────────────────────────────
  return (
    <div className={styles.page}>
      <PageHeader title="Add New Template" description="Configure a reconciliation file template end-to-end. Define file structure, map fields, set delivery mode, and validate before submission.">
        <button className={styles.btnOutline} onClick={() => navigate('/new-config/role-list')}>Cancel</button>
        <button className={styles.btnOutline} onClick={handleDraft}><Save size={11} /> Save Draft</button>
        <button className={styles.btnGold} onClick={handleSubmit}>Submit for Approval <ArrowRight size={12} /></button>
      </PageHeader>

      {/* Validation Summary */}
      <AnimatePresence>
        {showValSummary && (
          <motion.div className={styles.valSummary} initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <div className={styles.valTitle}><AlertCircle size={13} /> Please fix the following errors</div>
            <ul>{Object.values(errors).map((e, i) => <li key={i}>{e}</li>)}</ul>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Clone Banner */}
      <AnimatePresence>
        {showClone && (
          <motion.div className={styles.cloneBanner} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, height: 0, marginBottom: 0 }}>
            <div className={styles.cloneBannerIcon}><Copy size={18} /></div>
            <div className={styles.cloneBannerText}>
              <strong>Quick start — Clone from existing template</strong>
              <p>Save time by copying field definitions and settings from a previously configured template, then modify as needed.</p>
            </div>
            <div className={styles.cloneBannerAct}>
              <button className={styles.btnWarn} onClick={() => setShowCloneModal(true)}><Copy size={11} /> Clone Template</button>
              <button className={`${styles.btnOutline} ${styles.btnSm}`} style={{ marginLeft: 8 }} onClick={() => setShowClone(false)}>Dismiss</button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Progress Tracker */}
      <div className={styles.progressTrack}>
        {STEPS.map((st, i) => (
          <div key={st.num} className={styles.stepRow}>
            <div className={`${styles.pstep} ${activeStep === st.num ? styles.pstepActive : ''} ${activeStep > st.num ? styles.pstepDone : ''}`}
              onClick={() => goToStep(st.num)}>
              <div className={styles.pstepNum}>
                {activeStep > st.num ? <CheckCircle size={12} /> : st.num}
              </div>
              <div>
                <div className={styles.pstepLabel}>{st.label}</div>
                <div className={styles.pstepSub}>{st.sub}</div>
              </div>
            </div>
            {i < STEPS.length - 1 && <span className={styles.pstepArrow}>›</span>}
          </div>
        ))}
      </div>

      {/* ─── Section 1: Template Header ─── */}
      <Section id="sec1" num={1} title="Template Header" badge="required" sub="File type, source, and basic configuration"
        collapsed={collapsed.sec1} onToggle={() => toggleSection('sec1')} sectionRef={sectionRefs.sec1}>
        <div className={`${styles.g3} ${styles.mb3}`}>
          <Field label="Template Type" required error={errors.tmplType}>
            <select className={`${styles.fldInput} ${errors.tmplType ? styles.errInput : ''}`} value={tmplType} onChange={e => { setTmplType(e.target.value); setErrors(p => ({ ...p, tmplType: '' })); }}>
              <option value="">Select type…</option>
              <option value="CSV">CSV (with Delimiter)</option>
              <option value="XML">XML</option>
              <option value="Excel">Excel (.xlsx / .xls)</option>
              <option value="Fixed Width">Fixed Width</option>
            </select>
          </Field>
          <Field label="Template Name" required error={errors.tmplName}>
            <input className={`${styles.fldInput} ${errors.tmplName ? styles.errInput : ''}`} placeholder="e.g. UPI Settlement Daily" value={tmplName} onChange={e => { setTmplName(e.target.value); setErrors(p => ({ ...p, tmplName: '' })); }} />
          </Field>
          <Field label="Template Code" hint="Auto-generated by system">
            <input className={`${styles.fldInput} ${styles.fldRo} ${styles.mono}`} value="TMPL-0015" readOnly />
          </Field>
        </div>

        <div className={`${styles.g3} ${styles.mb3}`}>
          <Field label="Description / Remarks">
            <textarea className={styles.fldTextarea} placeholder="Brief description — purpose, source system, frequency…" value={description} onChange={e => setDescription(e.target.value)} />
          </Field>
          <Field label="Source System" required error={errors.sourceSystem}>
            <select className={`${styles.fldInput} ${errors.sourceSystem ? styles.errInput : ''}`} value={sourceSystem} onChange={e => { setSourceSystem(e.target.value); setErrors(p => ({ ...p, sourceSystem: '' })); }}>
              <option value="">Select…</option>
              <option value="SWITCH">Switch (NPCI)</option>
              <option value="CBS">CBS (Core Banking)</option>
              <option value="MERCHANT">Merchant Portal</option>
              <option value="GATEWAY">Payment Gateway</option>
              <option value="OTHER">Other</option>
            </select>
          </Field>
          <Field label="File Path" required error={errors.filePath} hint="Local or network path for this template's files">
            <input className={`${styles.fldInput} ${errors.filePath ? styles.errInput : ''}`} placeholder="e.g. /data/recon/inward/upi/" value={filePath} onChange={e => { setFilePath(e.target.value); setErrors(p => ({ ...p, filePath: '' })); }} />
          </Field>
        </div>

        {/* CSV Settings */}
        <AnimatePresence>
          {tmplType === 'CSV' && (
            <motion.div className={styles.condPanel} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}>
              <div className={styles.condTitle}>CSV Settings</div>
              <div className={styles.g4}>
                <Field label="Delimiter" required>
                  <select className={styles.fldInput} value={delimiter} onChange={e => setDelimiter(e.target.value)}>
                    <option value=",">Comma ( , )</option>
                    <option value="|">Pipe ( | )</option>
                    <option value="tab">Tab</option>
                    <option value=";">Semicolon ( ; )</option>
                    <option value="custom">Custom…</option>
                  </select>
                </Field>
                {delimiter === 'custom' && (
                  <Field label="Custom Delimiter">
                    <input className={styles.fldInput} placeholder="Enter character" maxLength={3} value={customDelim} onChange={e => setCustomDelim(e.target.value)} />
                  </Field>
                )}
                <Field label="Text Qualifier">
                  <select className={styles.fldInput} value={textQualifier} onChange={e => setTextQualifier(e.target.value)}>
                    <option value="">None</option>
                    <option value='"'>Double Quote ( " )</option>
                    <option value="'">Single Quote ( ' )</option>
                  </select>
                </Field>
              </div>
            </motion.div>
          )}
          {tmplType === 'Fixed Width' && (
            <motion.div className={styles.condPanel} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}>
              <div className={styles.condTitle}>Fixed Width Settings</div>
              <div className={styles.g3}>
                <Field label="Record Length" required hint="Total bytes per record line">
                  <input className={styles.fldInput} type="number" placeholder="e.g. 256" min={1} value={recordLen} onChange={e => setRecordLen(e.target.value)} />
                </Field>
                <Field label="Padding Character">
                  <select className={styles.fldInput} value={padChar} onChange={e => setPadChar(e.target.value)}>
                    <option value="space">Space</option>
                    <option value="zero">Zero (0)</option>
                  </select>
                </Field>
              </div>
            </motion.div>
          )}
          {tmplType === 'XML' && (
            <motion.div className={styles.condPanel} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}>
              <div className={styles.condTitle}>XML Settings</div>
              <div className={styles.g3}>
                <Field label="Root Element Tag" required>
                  <input className={styles.fldInput} placeholder="e.g. Transactions" value={xmlRoot} onChange={e => setXmlRoot(e.target.value)} />
                </Field>
                <Field label="Row Element Tag" required>
                  <input className={styles.fldInput} placeholder="e.g. Transaction" value={xmlRow} onChange={e => setXmlRow(e.target.value)} />
                </Field>
                <Field label="Namespace">
                  <input className={styles.fldInput} placeholder="Optional namespace URI" value={xmlNs} onChange={e => setXmlNs(e.target.value)} />
                </Field>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className={`${styles.g5} ${styles.mt3}`}>
          <Field label="File Encoding"><select className={styles.fldInput} value={encoding} onChange={e => setEncoding(e.target.value)}><option>UTF-8</option><option>ISO-8859-1 (Latin)</option><option>Windows-1252</option><option>ASCII</option><option>UTF-16</option></select></Field>
          <Field label="Header Row Present" required><select className={styles.fldInput} value={headerRow} onChange={e => setHeaderRow(e.target.value)}><option value="Y">Yes</option><option value="N">No</option></select></Field>
          <Field label="Skip Header Rows" hint="Top rows to skip"><input className={styles.fldInput} type="number" min={0} value={skipHeader} onChange={e => setSkipHeader(e.target.value)} /></Field>
          <Field label="Skip Footer Rows" hint="Bottom trailer rows"><input className={styles.fldInput} type="number" min={0} value={skipFooter} onChange={e => setSkipFooter(e.target.value)} /></Field>
          <Field label="Column Count" hint="Auto-calculated"><input className={`${styles.fldInput} ${styles.fldRo} ${styles.mono}`} value={fields.length} readOnly /></Field>
        </div>

        <div className={`${styles.g4} ${styles.mt3}`}>
          <Field label="File Frequency" required error={errors.freq}>
            <select className={`${styles.fldInput} ${errors.freq ? styles.errInput : ''}`} value={freq} onChange={e => { setFreq(e.target.value); setErrors(p => ({ ...p, freq: '' })); }}>
              <option value="">Select…</option>
              {['Daily','Weekly','Monthly','On-demand','Hourly','T+1','T+5','T+15'].map(o => <option key={o}>{o}</option>)}
            </select>
          </Field>
          <Field label="File Naming Pattern" hint="Tokens: {YYYYMMDD}, {SEQ}, {HHmmss}">
            <input className={styles.fldInput} placeholder="e.g. TXN_{YYYYMMDD}_*.csv" value={filePattern} onChange={e => setFilePattern(e.target.value)} />
          </Field>
          <Field label="Date Format" required>
            <select className={styles.fldInput} value={dateFormat} onChange={e => setDateFormat(e.target.value)}>
              {['DD/MM/YYYY','MM/DD/YYYY','YYYYMMDD','DD-MM-YYYY','YYYY-MM-DD','DD-MON-YYYY'].map(o => <option key={o}>{o}</option>)}
            </select>
          </Field>
          <Field label="Amount Format" required>
            <select className={styles.fldInput} value={amtFormat} onChange={e => setAmtFormat(e.target.value)}>
              <option value="decimal">Decimal (e.g. 100.50)</option>
              <option value="implied2">Implied 2 dec (10050 = 100.50)</option>
              <option value="implied3">Implied 3 dec (100500 = 100.500)</option>
            </select>
          </Field>
        </div>

        <div className={`${styles.g3} ${styles.mt3}`}>
          <Field label="Reversal Indicator">
            <select className={styles.fldInput} value={revInd} onChange={e => setRevInd(e.target.value)}>
              <option value="N">No</option><option value="Y">Yes</option>
            </select>
          </Field>
          <Field label="Duplicate Check">
            <select className={styles.fldInput} value={dupCheck} onChange={e => setDupCheck(e.target.value)}>
              <option value="Y">Yes (Reject duplicates)</option>
              <option value="N">No (Allow duplicates)</option>
            </select>
          </Field>
        </div>

        <AnimatePresence>
          {revInd === 'Y' && (
            <motion.div className={`${styles.condPanel} ${styles.mt2}`} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}>
              <div className={styles.condTitle}>Reversal Configuration</div>
              <div className={styles.g3}>
                <Field label="Reversal Field Name" required><input className={styles.fldInput} placeholder="e.g. TXN_TYPE" value={revField} onChange={e => setRevField(e.target.value)} /></Field>
                <Field label="Reversal Value"><input className={styles.fldInput} placeholder="e.g. REV or CR" value={revValue} onChange={e => setRevValue(e.target.value)} /></Field>
                <Field label="Reversal Amount Handling"><select className={styles.fldInput} value={revAmt} onChange={e => setRevAmt(e.target.value)}><option value="negate">Negate amount</option><option value="asis">Keep as-is</option></select></Field>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </Section>

      {/* ─── Section 2: Field Details ─── */}
      <Section id="sec2" num={2} title="Field Details" badge="required" sub="Define all columns — or upload a file to auto-detect"
        collapsed={collapsed.sec2} onToggle={() => toggleSection('sec2')} sectionRef={sectionRefs.sec2}>
        <div className={styles.hintCard}><Info size={14} /><span>Define each column in the source file. You can type manually, or upload a sample file to auto-detect fields. Field names auto-populate the Recon Mapping dropdowns in Section 3.</span></div>
        {errors.fields && <div className={styles.inlineError}><AlertCircle size={12} /> {errors.fields}</div>}

        <div className={styles.fgToolbar}>
          <span className={styles.fgCount}>{fields.length} field{fields.length !== 1 ? 's' : ''} defined</span>
          <div className={styles.fgActions}>
            <button className={`${styles.btnOutline} ${styles.btnSm}`} onClick={clearFields}><Trash2 size={10} /> Clear All</button>
            <label className={`${styles.btnInfo} ${styles.btnSm}`} style={{ cursor: 'pointer' }}>
              <Upload size={11} /> Upload File (Auto-detect)
              <input ref={fileInputRef} type="file" accept=".csv,.xlsx,.xls,.xml,.txt" style={{ display: 'none' }} onChange={handleFileUpload} />
            </label>
            <button className={`${styles.btnGold} ${styles.btnSm}`} onClick={addField}><Plus size={11} /> Add Field</button>
          </div>
        </div>

        {/* Upload progress */}
        {uploading && (
          <div className={styles.uploadProgress}>
            <div className={styles.progLabel}>Parsing file… detecting columns…</div>
            <div className={styles.progBar}><div className={styles.progFill} style={{ width: `${uploadPct}%` }} /></div>
          </div>
        )}

        {/* Upload preview */}
        <AnimatePresence>
          {uploadPreview && (
            <motion.div className={styles.uploadPreview} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
              <div className={styles.upTitle}><CheckCircle size={13} /> File parsed — <strong>{uploadPreview.name}</strong> — {uploadPreview.cols.length} columns detected</div>
              <div style={{ fontSize: 'var(--text-xs)', color: 'var(--color-success-500)', marginBottom: 'var(--space-2)' }}>Preview (first 2 rows):</div>
              <div className={styles.previewTableWrap}>
                <table className={styles.previewTable}>
                  <thead><tr>{uploadPreview.cols.map(c => <th key={c}>{c}</th>)}</tr></thead>
                  <tbody>{uploadPreview.rows.map((r, i) => <tr key={i}>{r.map((cell, j) => <td key={j}>{cell}</td>)}</tr>)}</tbody>
                </table>
              </div>
              <div style={{ display: 'flex', gap: 'var(--space-2)', marginTop: 'var(--space-3)' }}>
                <button className={`${styles.btnSuccess} ${styles.btnSm}`} onClick={confirmUpload}><CheckCircle size={10} /> Confirm &amp; Populate</button>
                <button className={`${styles.btnOutline} ${styles.btnSm}`} onClick={() => setUploadPreview(null)}>Cancel</button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Field grid */}
        <div className={styles.fgWrap}>
          <table className={styles.fgTable}>
            <thead>
              <tr>
                <th style={{ width: 24 }}>⋮</th>
                <th style={{ width: 28 }}>#</th>
                <th style={{ textAlign: 'left', minWidth: 130 }}>Field Name *</th>
                <th>Type</th>
                <th>Format</th>
                <th>Len</th>
                <th>From</th>
                <th>To</th>
                <th>PK</th>
                <th>Recon</th>
                <th>Mand.</th>
                <th>Trim</th>
                <th>Default</th>
                <th style={{ width: 52 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {fields.map((f, i) => (
                <tr key={f.id}>
                  <td><GripVertical size={12} className={styles.dragHandle} /></td>
                  <td><span className={styles.rowNum}>{i + 1}</span></td>
                  <td><input className={`${styles.fgInput} ${!f.name ? styles.fieldErr : ''}`} value={f.name} onChange={e => updateField(f.id, 'name', e.target.value)} placeholder="FIELD_NAME" /></td>
                  <td><select className={styles.fgSelect} value={f.type} onChange={e => updateField(f.id, 'type', e.target.value)}>{FIELD_TYPES.map(t => <option key={t}>{t}</option>)}</select></td>
                  <td><input className={styles.fgInput} value={f.format} onChange={e => updateField(f.id, 'format', e.target.value)} placeholder="DD/MM/YYYY" style={{ width: 90 }} /></td>
                  <td><input className={`${styles.fgInput} ${styles.fgInputNum}`} type="number" value={f.len} onChange={e => updateField(f.id, 'len', e.target.value)} style={{ width: 52 }} /></td>
                  <td><input className={`${styles.fgInput} ${styles.fgInputNum}`} type="number" value={f.from} onChange={e => updateField(f.id, 'from', e.target.value)} style={{ width: 48 }} /></td>
                  <td><input className={`${styles.fgInput} ${styles.fgInputNum}`} type="number" value={f.to} onChange={e => updateField(f.id, 'to', e.target.value)} style={{ width: 48 }} /></td>
                  <td><input type="checkbox" checked={f.pk} onChange={e => updateField(f.id, 'pk', e.target.checked)} className={styles.fgChk} /></td>
                  <td><input type="checkbox" checked={f.reconKey} onChange={e => updateField(f.id, 'reconKey', e.target.checked)} className={styles.fgChk} /></td>
                  <td><input type="checkbox" checked={f.mandatory} onChange={e => updateField(f.id, 'mandatory', e.target.checked)} className={styles.fgChk} /></td>
                  <td><input type="checkbox" checked={f.trim} onChange={e => updateField(f.id, 'trim', e.target.checked)} className={styles.fgChk} /></td>
                  <td><input className={styles.fgInput} value={f.def} onChange={e => updateField(f.id, 'def', e.target.value)} placeholder="—" style={{ width: 60 }} /></td>
                  <td>
                    <button className={styles.cloneRow} onClick={() => cloneField(f)} title="Clone row"><Copy size={10} /></button>
                    <button className={styles.delRow} onClick={() => removeField(f.id)} title="Delete row"><Trash2 size={10} /></button>
                  </td>
                </tr>
              ))}
              {fields.length === 0 && (
                <tr><td colSpan={14} className={styles.emptyRow}>No fields defined yet — click "Add Field" or upload a file</td></tr>
              )}
            </tbody>
          </table>
        </div>
        <div className={styles.fgHint}>Drag ⋮ to reorder &nbsp;•&nbsp; PK = dedup key &nbsp;•&nbsp; Recon = matching key &nbsp;•&nbsp; Field names feed Section 3 dropdowns</div>
      </Section>

      {/* ─── Section 3: Recon Mapping (Optional) ─── */}
      <Section id="sec3" num={3} title="Reconciliation Field Mapping" badge="optional" sub="Map file columns to recon key fields — configure after reconciliation setup"
        collapsed={collapsed.sec3 !== false} onToggle={() => setCollapsed(p => ({ ...p, sec3: p.sec3 === false ? true : false }))} sectionRef={sectionRefs.sec3}>
        <div className={styles.hintCard}><Info size={14} /><span>This section is optional at template creation time. Reconciliation mapping can be configured later after the reconciliation process is set up. Dropdowns auto-populate from field names defined in Section 2.</span></div>
        <div className={styles.g4}>
          {RECON_MAP_FIELDS.slice(0, 4).map(rf => (
            <Field key={rf.id} label={rf.label} required={rf.required}>
              <select className={styles.fldInput} value={reconMap[rf.id] || ''} onChange={e => setReconMap(p => ({ ...p, [rf.id]: e.target.value }))}>
                {fieldOptions().map(o => <option key={o} value={o === '— Select field —' ? '' : o}>{o}</option>)}
              </select>
            </Field>
          ))}
        </div>
        <div className={`${styles.g4} ${styles.mt3}`}>
          {RECON_MAP_FIELDS.slice(4).map(rf => (
            <Field key={rf.id} label={rf.label}>
              <select className={styles.fldInput} value={reconMap[rf.id] || ''} onChange={e => setReconMap(p => ({ ...p, [rf.id]: e.target.value }))}>
                {fieldOptions().map(o => <option key={o} value={o === '— Select field —' ? '' : o}>{o}</option>)}
              </select>
            </Field>
          ))}
        </div>
      </Section>

      {/* ─── Section 4: File Delivery ─── */}
      <Section id="sec4" num={4} title="File Delivery" badge="required" sub="How this file reaches the system — SFTP auto-pickup or manual upload"
        collapsed={collapsed.sec4} onToggle={() => toggleSection('sec4')} sectionRef={sectionRefs.sec4}>
        <div className={styles.deliveryToggle}>
          {[{ id: 'sftp', icon: '📡', label: 'SFTP Auto-Pickup', sub: 'File fetched automatically from server' },
            { id: 'manual', icon: '📁', label: 'Manual Upload', sub: 'User browses and uploads the file' }].map(d => (
            <div key={d.id} className={`${styles.dtog} ${deliveryMode === d.id ? styles.dtogActive : ''}`} onClick={() => setDeliveryMode(d.id)}>
              <div className={styles.dtogIcon}>{d.icon}</div>
              <div className={styles.dtogLabel}>{d.label}</div>
              <div className={styles.dtogSub}>{d.sub}</div>
            </div>
          ))}
        </div>

        {deliveryMode === 'sftp' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            <div className={`${styles.g3} ${styles.mb3}`}>
              <Field label="SFTP Server" required hint="Select a pre-configured SFTP server from master list">
                <select className={styles.fldInput} value={sftpServer} onChange={e => onSftpSelect(e.target.value)}>
                  <option value="">Select SFTP server…</option>
                  {SFTP_SERVERS.map(s => <option key={s.value} value={s.value}>{s.label} — {s.ip}</option>)}
                  <option value="custom">+ Add Custom SFTP…</option>
                </select>
              </Field>
              <Field label="Protocol">
                <select className={styles.fldInput} value={sftpProtocol} onChange={e => setSftpProtocol(e.target.value)}>
                  {['SFTP','FTP','FTPS','SCP'].map(p => <option key={p}>{p}</option>)}
                </select>
              </Field>
              <Field label="Remote Directory Path" required>
                <input className={styles.fldInput} placeholder="e.g. /data/recon/inward/" value={sftpRemotePath} onChange={e => setSftpRemotePath(e.target.value)} />
              </Field>
            </div>

            {selectedSftp && (
              <motion.div className={styles.sftpInfoCard} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <span className={styles.sftpDot} />
                <div>
                  <div className={styles.sftpName}>{selectedSftp.label}</div>
                  <div className={styles.sftpMeta}>{selectedSftp.ip} &nbsp;·&nbsp; {selectedSftp.env}</div>
                </div>
                <button className={`${styles.btnNavy} ${styles.btnSm}`} style={{ marginLeft: 'auto' }} onClick={() => { setShowTestModal(true); setTestResult(null); }}>
                  <CheckCircle size={10} /> Test Connection
                </button>
              </motion.div>
            )}

            <AnimatePresence>
              {showCustomSftp && (
                <motion.div className={styles.condPanel} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                  <div className={styles.condTitle}>Custom SFTP Server Details</div>
                  <div className={`${styles.g4} ${styles.mb3}`}>
                    <Field label="IP / Hostname" required><input className={styles.fldInput} placeholder="192.168.1.100" value={sftpIp} onChange={e => setSftpIp(e.target.value)} /></Field>
                    <Field label="Port"><input className={styles.fldInput} type="number" value={sftpPort} onChange={e => setSftpPort(e.target.value)} /></Field>
                    <Field label="Username" required><input className={styles.fldInput} placeholder="SFTP username" value={sftpUser} onChange={e => setSftpUser(e.target.value)} /></Field>
                    <Field label="Auth Type">
                      <select className={styles.fldInput} value={sftpAuthType} onChange={e => setSftpAuthType(e.target.value)}>
                        <option value="password">Password</option><option value="key">SSH Key</option>
                      </select>
                    </Field>
                  </div>
                  <div className={styles.g4}>
                    {sftpAuthType === 'password'
                      ? <Field label="Password" required><input className={styles.fldInput} type="password" placeholder="••••••••" value={sftpPass} onChange={e => setSftpPass(e.target.value)} /></Field>
                      : <Field label="SSH Key File"><input className={styles.fldInput} type="file" style={{ height: 'auto', padding: '6px 10px', fontSize: 'var(--text-xs)' }} /></Field>}
                    <Field label="File Pattern"><input className={styles.fldInput} placeholder="e.g. *.csv or TXN_*.txt" value={sftpPattern} onChange={e => setSftpPattern(e.target.value)} /></Field>
                    <Field label="Archive Path" hint="Files moved here after extraction"><input className={styles.fldInput} placeholder="/data/recon/archive/" value={sftpArchive} onChange={e => setSftpArchive(e.target.value)} /></Field>
                  </div>
                  <div style={{ marginTop: 'var(--space-3)', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                    <button className={`${styles.btnNavy} ${styles.btnSm}`} onClick={() => { setShowTestModal(true); setTestResult(null); }}><CheckCircle size={10} /> Test Connection</button>
                    <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)' }}>Verify before saving</span>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )}

        {deliveryMode === 'manual' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            <div className={styles.hintCard}><Info size={14} /><span>In Manual Upload mode, operators will be prompted to browse and upload the file during extraction. No SFTP configuration is needed.</span></div>
            <div className={styles.g2}>
              <Field label="Allowed File Extensions" hint="e.g. .csv, .xlsx, .txt">
                <input className={styles.fldInput} placeholder=".csv, .xlsx" defaultValue=".csv" />
              </Field>
              <Field label="Max File Size (MB)">
                <input className={styles.fldInput} type="number" defaultValue={50} />
              </Field>
            </div>
          </motion.div>
        )}
      </Section>

      {/* ─── Section 5: Encryption ─── */}
      <Section id="sec5" num={5} title="Encryption" badge="optional" sub="Security settings for file encryption/decryption"
        collapsed={collapsed.sec5 !== false} onToggle={() => setCollapsed(p => ({ ...p, sec5: p.sec5 === false ? true : false }))} sectionRef={sectionRefs.sec5}>
        <div className={styles.g3}>
          <Field label="Enable Encryption">
            <div className={styles.toggleRow}>
              <div className={`${styles.toggle} ${encEnabled ? styles.toggleOn : ''}`} onClick={() => setEncEnabled(v => !v)}>
                <div className={styles.toggleThumb} />
              </div>
              <span style={{ fontSize: 'var(--text-sm)', color: 'var(--color-neutral-600)' }}>{encEnabled ? 'Enabled' : 'Disabled'}</span>
            </div>
          </Field>
          {encEnabled && <>
            <Field label="Encryption Type">
              <select className={styles.fldInput} value={encType} onChange={e => setEncType(e.target.value)}>
                <option>AES-256</option><option>AES-128</option><option>PGP/GPG</option><option>3DES</option>
              </select>
            </Field>
            <Field label="Key Management">
              <select className={styles.fldInput} value={encKeyMode} onChange={e => setEncKeyMode(e.target.value)}>
                <option value="system">System-managed key</option>
                <option value="upload">Upload key file</option>
                <option value="hsm">HSM (Hardware Security Module)</option>
              </select>
            </Field>
          </>}
        </div>
      </Section>

      {/* ─── Section 6: Schedule & Validate ─── */}
      <Section id="sec6" num={6} title="Schedule & Validate" badge="optional" sub="Automate extraction schedule and test before submission"
        collapsed={collapsed.sec6} onToggle={() => toggleSection('sec6')} sectionRef={sectionRefs.sec6}>
        <div className={`${styles.g3} ${styles.mb3}`}>
          <Field label="Enable Auto-Schedule">
            <div className={styles.toggleRow}>
              <div className={`${styles.toggle} ${schedEnabled ? styles.toggleOn : ''}`} onClick={() => setSchedEnabled(v => !v)}>
                <div className={styles.toggleThumb} />
              </div>
              <span style={{ fontSize: 'var(--text-sm)', color: 'var(--color-neutral-600)' }}>{schedEnabled ? 'Enabled' : 'Disabled'}</span>
            </div>
          </Field>
          {schedEnabled && <>
            <Field label="Extraction Time">
              <input className={styles.fldInput} type="time" value={schedTime} onChange={e => setSchedTime(e.target.value)} />
            </Field>
            <Field label="Active Days">
              <div className={styles.schedDays}>
                {DAYS_LABELS.map((d, i) => (
                  <button key={i} className={`${styles.sdBtn} ${schedDays.includes(i) ? styles.sdBtnOn : ''}`} onClick={() => toggleDay(i)}>{d}</button>
                ))}
              </div>
            </Field>
          </>}
        </div>

        {/* Dry Run */}
        <div className={styles.extractionPanel}>
          <div className={styles.extPanelTop}>
            <div>
              <h3>Dry Run Validation</h3>
              <p>Run a test extraction against a sample file to verify field mapping and delivery configuration before submitting.</p>
            </div>
            <button className={styles.btnGoldSolid} onClick={() => setShowDryRun(v => !v)}>
              {showDryRun ? 'Hide Log' : '▶ Run Dry Test'}
            </button>
          </div>
          <div className={styles.extStats}>
            {[['1,247','Total Records'],['1,189','Valid Records'],['43','Warnings'],['15','Errors']].map(([val, lbl]) => (
              <div key={lbl} className={styles.extStat}><div className={styles.extStatVal}>{val}</div><div className={styles.extStatLbl}>{lbl}</div></div>
            ))}
          </div>
          <AnimatePresence>
            {showDryRun && (
              <motion.div className={styles.dryRunLog} initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}>
                {DRY_RUN_LOG.map((log, i) => (
                  <div key={i} className={styles[`log${log.type.charAt(0).toUpperCase() + log.type.slice(1)}`]}>{log.msg}</div>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </Section>

      {/* Form actions */}
      <div className={styles.formActions}>
        <button className={styles.btnOutline} onClick={() => navigate('/new-config/role-list')}>← Cancel</button>
        <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
          <button className={styles.btnOutline} onClick={handleDraft}><Save size={11} /> Save Draft</button>
          <button className={styles.btnGold} onClick={handleSubmit}>Submit for Approval <ArrowRight size={12} /></button>
        </div>
      </div>

      {/* ─── SFTP Test Modal ─── */}
      {showTestModal && (
        <div className={styles.overlay} onClick={() => setShowTestModal(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <div className={styles.modalHead}>
              <h3><Server size={14} color="rgba(255,255,255,0.8)" /> Test SFTP Connection</h3>
              <button className={styles.modalClose} onClick={() => setShowTestModal(false)}>×</button>
            </div>
            <div className={styles.modalBody}>
              <p style={{ fontSize: 'var(--text-sm)', color: 'var(--color-neutral-600)', marginBottom: 'var(--space-4)' }}>
                Testing connection to <strong>{selectedSftp?.label || 'custom SFTP'}</strong> ({selectedSftp?.ip || sftpIp})
              </p>
              {testResult === null && !testing && (
                <button className={`${styles.btnNavy} ${styles.btnLg}`} onClick={handleTestConnection} style={{ width: '100%' }}>
                  <CheckCircle size={14} /> Start Connection Test
                </button>
              )}
              {testing && <div className={styles.testingSpinner}><div className={styles.spinner} /> Testing connection…</div>}
              {testResult === 'ok' && (
                <div className={styles.testResultOk}><CheckCircle size={14} /> Connection successful — server reachable, credentials verified, directory accessible.</div>
              )}
              {testResult === 'fail' && (
                <div className={styles.testResultFail}><AlertCircle size={14} /> Connection failed — please check IP address, credentials, and firewall rules.</div>
              )}
            </div>
            <div className={styles.modalFooter}>
              <button className={styles.btnOutline} onClick={() => setShowTestModal(false)}>Close</button>
              {testResult === 'ok' && <button className={`${styles.btnSuccess}`} onClick={() => setShowTestModal(false)}>✓ Confirmed</button>}
            </div>
          </div>
        </div>
      )}

      {/* ─── Clone Template Modal ─── */}
      {showCloneModal && (
        <div className={styles.overlay} onClick={() => setShowCloneModal(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <div className={styles.modalHead}>
              <h3><Copy size={14} color="rgba(255,255,255,0.8)" /> Clone from Existing Template</h3>
              <button className={styles.modalClose} onClick={() => setShowCloneModal(false)}>×</button>
            </div>
            <div className={styles.modalBody}>
              <Field label="Select Template to Clone">
                <select className={styles.fldInput}>
                  <option value="">Select a template…</option>
                  {['UPI_Settlement_Daily (TMPL-0001)','IMPS_Report_Daily (TMPL-0003)','CBS_Ledger_Weekly (TMPL-0007)','NACH_Batch_File (TMPL-0012)'].map(t => <option key={t}>{t}</option>)}
                </select>
              </Field>
              <div className={`${styles.hintCard} ${styles.mt3}`}><Info size={14} /><span>Fields, recon mapping, and delivery settings will be copied. You can modify any values after cloning.</span></div>
            </div>
            <div className={styles.modalFooter}>
              <button className={styles.btnOutline} onClick={() => setShowCloneModal(false)}>Cancel</button>
              <button className={styles.btnGold} onClick={() => setShowCloneModal(false)}><Copy size={12} /> Clone &amp; Populate</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ─── Section wrapper ──────────────────────────────────────────────────────────
const Section = ({ num, title, badge, sub, collapsed, onToggle, children, sectionRef }) => (
  <motion.div ref={sectionRef} className={`${styles.sc} ${collapsed ? styles.scCollapsed : ''}`} layout>
    <div className={styles.scHead} onClick={onToggle}>
      <span className={styles.scNum}>{num}</span>
      <span className={styles.scTitle}>{title}</span>
      <span className={`${styles.scBadge} ${badge === 'required' ? styles.scBadgeReq : styles.scBadgeOpt}`}>
        {badge}
      </span>
      <span className={styles.scSub}>{sub}</span>
      <ChevronDown size={15} className={`${styles.scToggle} ${collapsed ? styles.scToggleCollapsed : ''}`} color="rgba(255,255,255,0.45)" />
    </div>
    <AnimatePresence>
      {!collapsed && (
        <motion.div className={styles.scBody} initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} transition={{ duration: 0.2 }}>
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  </motion.div>
);

// ─── Field wrapper ────────────────────────────────────────────────────────────
const Field = ({ label, required, hint, error, children }) => (
  <div className={styles.fld}>
    <label className={styles.fldLabel}>
      {label} {required && <span className={styles.req}>*</span>}
    </label>
    {children}
    {hint  && <div className={styles.hint}>{hint}</div>}
    {error && <div className={styles.errorMsg}><AlertCircle size={10} /> {error}</div>}
  </div>
);

export default AddTemplate;
