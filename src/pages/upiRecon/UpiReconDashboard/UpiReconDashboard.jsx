import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, Settings, RefreshCw, Download, Filter,
  CheckCircle, AlertTriangle, TrendingDown, Repeat2, Clock,
  Save, X, Edit2, Bell, User, ChevronRight, Info,
} from 'lucide-react';
import { PageHeader } from '../../../components/common';
import styles from './UpiReconDashboard.module.css';

// ─── Data ────────────────────────────────────────────────────────────────────

const INITIAL_CYCLES = [
  { id: '2C',  name: 'Cycle 2C',  startTime: '00:00', endTime: '05:00', extractionTime: '06:00', reconFlag: false, active: true,  source: 'NPCI/SFTP', matchKeys: ['RRN','Amount'],           tolerance: 0.01, autoWriteOff: 500,  retryCount: 3, retryInterval: 5,  dependency: '',       sla: 60 },
  { id: '3C',  name: 'Cycle 3C',  startTime: '05:00', endTime: '07:00', extractionTime: '08:00', reconFlag: true,  active: true,  source: 'CBS',        matchKeys: ['RRN','UTR','Amount'],    tolerance: 0.02, autoWriteOff: 1000, retryCount: 3, retryInterval: 5,  dependency: '2C',     sla: 45 },
  { id: '4C',  name: 'Cycle 4C',  startTime: '07:00', endTime: '09:00', extractionTime: '10:35', reconFlag: false, active: true,  source: 'Switch',     matchKeys: ['RRN','Amount'],           tolerance: 0.01, autoWriteOff: 500,  retryCount: 2, retryInterval: 10, dependency: '',       sla: 60 },
  { id: '5C',  name: 'Cycle 5C',  startTime: '09:00', endTime: '11:00', extractionTime: '12:00', reconFlag: true,  active: true,  source: 'Kafka',      matchKeys: ['RRN','UTR','TxnID','Amount'], tolerance: 0.05, autoWriteOff: 2000, retryCount: 3, retryInterval: 5, dependency: '3C,4C', sla: 50 },
  { id: '6C',  name: 'Cycle 6C',  startTime: '11:00', endTime: '13:00', extractionTime: '14:05', reconFlag: false, active: true,  source: 'NPCI/SFTP', matchKeys: ['RRN','Amount'],           tolerance: 0.01, autoWriteOff: 500,  retryCount: 3, retryInterval: 5,  dependency: '',       sla: 60 },
  { id: '7C',  name: 'Cycle 7C',  startTime: '13:00', endTime: '15:00', extractionTime: '16:05', reconFlag: false, active: true,  source: 'CBS',        matchKeys: ['RRN','Amount','Date'],    tolerance: 0.01, autoWriteOff: 750,  retryCount: 2, retryInterval: 10, dependency: '',       sla: 55 },
  { id: '8C',  name: 'Cycle 8C',  startTime: '15:00', endTime: '17:00', extractionTime: '18:00', reconFlag: true,  active: true,  source: 'REST API',   matchKeys: ['RRN','UTR','Amount'],    tolerance: 0.02, autoWriteOff: 1500, retryCount: 3, retryInterval: 5,  dependency: '7C',     sla: 45 },
  { id: '9C',  name: 'Cycle 9C',  startTime: '17:00', endTime: '19:00', extractionTime: '20:00', reconFlag: false, active: true,  source: 'Switch',     matchKeys: ['RRN','Amount'],           tolerance: 0.01, autoWriteOff: 500,  retryCount: 3, retryInterval: 5,  dependency: '',       sla: 60 },
  { id: '10C', name: 'Cycle 10C', startTime: '19:00', endTime: '21:00', extractionTime: '22:00', reconFlag: false, active: true,  source: 'NPCI/SFTP', matchKeys: ['RRN','Amount'],           tolerance: 0.01, autoWriteOff: 500,  retryCount: 2, retryInterval: 10, dependency: '',       sla: 60 },
  { id: '1C',  name: 'Cycle 1C',  startTime: '21:00', endTime: '00:00', extractionTime: '23:10', reconFlag: false, active: true,  source: 'Kafka',      matchKeys: ['RRN','UTR','Amount'],    tolerance: 0.03, autoWriteOff: 1000, retryCount: 3, retryInterval: 5,  dependency: '10C',    sla: 50 },
];

const SEED_OFFSET = 42;
const generateData = (cycles) => cycles.map((c, i) => {
  const status = i < 3 ? 'completed' : i === 3 ? 'in_progress' : i === 4 ? 'failed' : 'not_started';
  const total   = ((i + SEED_OFFSET) * 2137 % 40000) + 10000;
  const matched = Math.floor(total * (0.92 + (i % 7) * 0.01));
  const mismatched = Math.floor((total - matched) * 0.4);
  const missing    = Math.floor((total - matched) * 0.35);
  const excess     = total - matched - mismatched - missing;
  const writeOff   = Math.floor(mismatched * 0.2);
  const debit  = ((i + 1) * 1234567) % 8000000 + 1000000;
  const credit = debit + (((i + 2) * 31337) % 200000) - 100000;
  return {
    ...c, status,
    totalRecords: status === 'not_started' ? 0 : total,
    matched:      status === 'not_started' ? 0 : matched,
    mismatched:   status === 'not_started' ? 0 : mismatched,
    missing:      status === 'not_started' ? 0 : missing,
    excess:       status === 'not_started' ? 0 : excess,
    writeOff:     status === 'not_started' ? 0 : writeOff,
    debit:        status === 'not_started' ? 0 : debit,
    credit:       status === 'not_started' ? 0 : credit,
    variance:     status === 'not_started' ? 0 : Math.abs(debit - credit),
    successRate:  status === 'not_started' ? 0 : ((matched / total) * 100).toFixed(1),
    slaStatus:    status === 'completed' ? 'met' : status === 'in_progress' ? 'at_risk' : status === 'failed' ? 'breached' : 'pending',
  };
});

const MATCH_KEY_OPTIONS = ['RRN', 'UTR', 'TxnID', 'Amount', 'Date'];
const SOURCE_OPTIONS    = ['NPCI/SFTP', 'CBS', 'Switch', 'Kafka', 'REST API'];

const fmt    = (n) => new Intl.NumberFormat('en-IN').format(n);
const fmtCur = (n) => '₹' + new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(n);

const STATUS_LABEL = { completed: 'Completed', in_progress: 'In Progress', failed: 'Failed', not_started: 'Not Started' };
const STATUS_CLS   = { completed: styles.dotGreen, in_progress: styles.dotAmber, failed: styles.dotRed, not_started: styles.dotGray };
const STATUS_CARD  = { completed: styles.cycleCardGreen, in_progress: styles.cycleCardAmber, failed: styles.cycleCardRed, not_started: '' };
const SLA_CLS      = { met: styles.pillMet, at_risk: styles.pillAtRisk, breached: styles.pillBreached, pending: styles.pillPending };
const TREND_DATA   = [94.2, 95.1, 93.8, 96.2, 95.5, 97.1, 95.9];
const EXCEPTION_CATS = [
  { label: 'Amount Mismatch',       pct: 38, cls: styles.barRed    },
  { label: 'Missing in CBS',        pct: 27, cls: styles.barAmber  },
  { label: 'Duplicate TxnID',       pct: 18, cls: styles.barOrange },
  { label: 'Timeout / No Response', pct: 12, cls: styles.barPurple },
  { label: 'Other',                 pct:  5, cls: styles.barGray   },
];

// ─── Toggle ──────────────────────────────────────────────────────────────────
const Toggle = ({ checked, onChange }) => (
  <div className={`${styles.toggle} ${checked ? styles.toggleOn : ''}`} onClick={() => onChange(!checked)}>
    <div className={styles.toggleThumb} />
  </div>
);

// ─── Mini sparkline ──────────────────────────────────────────────────────────
const Sparkline = ({ data, width = 140, height = 44 }) => {
  const max = Math.max(...data), min = Math.min(...data), range = max - min || 1;
  const pts = data.map((v, i) => `${(i / (data.length - 1)) * width},${height - ((v - min) / range) * (height - 6) - 3}`).join(' ');
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      <polyline points={pts} fill="none" stroke="var(--color-success-500)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
};

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
const DashboardScreen = ({ cycles }) => {
  const [data]          = useState(() => generateData(cycles));
  const [selectedCycle, setSelectedCycle] = useState(null);
  const [filter,        setFilter]        = useState('all');
  const [refreshing,    setRefreshing]    = useState(false);

  const totals = data.reduce((acc, d) => ({
    records: acc.records + d.totalRecords, matched: acc.matched + d.matched,
    mismatched: acc.mismatched + d.mismatched, missing: acc.missing + d.missing,
    excess: acc.excess + d.excess, debit: acc.debit + d.debit, credit: acc.credit + d.credit,
    variance: acc.variance + d.variance, writeOff: acc.writeOff + d.writeOff,
  }), { records: 0, matched: 0, mismatched: 0, missing: 0, excess: 0, debit: 0, credit: 0, variance: 0, writeOff: 0 });

  const filtered = filter === 'all' ? data : data.filter(d => d.status === filter);
  const overallRate = totals.records > 0 ? ((totals.matched / totals.records) * 100).toFixed(1) : '0.0';

  const doRefresh = () => { setRefreshing(true); setTimeout(() => setRefreshing(false), 1200); };

  const TOP_METRICS = [
    { label: 'Total Records',   value: fmt(totals.records),               sub: 'across all cycles',   cls: styles.metricAccent,  icon: <LayoutDashboard size={16} /> },
    { label: 'Match Rate',       value: overallRate + '%',                  sub: fmt(totals.matched) + ' matched', cls: styles.metricGreen, icon: <CheckCircle size={16} /> },
    { label: 'Exceptions',       value: fmt(totals.mismatched + totals.missing + totals.excess), sub: 'need attention', cls: styles.metricRed, icon: <AlertTriangle size={16} /> },
    { label: 'Net Settlement',   value: fmtCur(Math.abs(totals.debit - totals.credit)), sub: 'debit – credit', cls: styles.metricPurple, icon: <TrendingDown size={16} /> },
    { label: 'Auto Write-offs',  value: fmt(totals.writeOff),              sub: 'within threshold',    cls: styles.metricOrange,  icon: <Repeat2 size={16} /> },
  ];

  return (
    <div className={styles.screenWrap}>
      {/* Top Metrics */}
      <div className={styles.metricsGrid}>
        {TOP_METRICS.map((m, i) => (
          <motion.div key={i} className={`${styles.metricCard} ${m.cls}`} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
            <div className={styles.metricIcon}>{m.icon}</div>
            <div className={styles.metricLabel}>{m.label}</div>
            <div className={styles.metricValue}>{m.value}</div>
            <div className={styles.metricSub}>{m.sub}</div>
          </motion.div>
        ))}
      </div>

      {/* Cycle Status Strip */}
      <motion.div className={styles.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.25 }}>
        <div className={styles.cardHead}>
          <span className={styles.cardTitle}>Cycle Status Overview</span>
          <div className={styles.legend}>
            {Object.entries(STATUS_LABEL).map(([k, v]) => (
              <span key={k} className={styles.legendItem}>
                <span className={`${styles.legendDot} ${STATUS_CLS[k]}`} />{v}
              </span>
            ))}
          </div>
        </div>
        <div className={styles.cycleStrip}>
          {data.map((d) => (
            <div key={d.id}
              className={`${styles.cycleCard} ${STATUS_CARD[d.status]} ${selectedCycle === d.id ? styles.cycleCardSelected : ''}`}
              onClick={() => setSelectedCycle(selectedCycle === d.id ? null : d.id)}>
              <div className={styles.cycleId}>{d.id}</div>
              <div className={styles.cycleTime}>{d.startTime}–{d.endTime}</div>
              <span className={`${styles.cycleDot} ${STATUS_CLS[d.status]}`} />
              {d.status !== 'not_started' && <div className={styles.cycleRate}>{d.successRate}%</div>}
            </div>
          ))}
        </div>
      </motion.div>

      {/* Filter Controls */}
      <motion.div className={styles.controlsRow} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }}>
        <div className={styles.filterGroup}>
          <Filter size={13} color="var(--color-neutral-400)" />
          {['all', 'completed', 'in_progress', 'failed', 'not_started'].map(f => (
            <button key={f} className={`${styles.filterBtn} ${filter === f ? styles.filterBtnActive : ''}`} onClick={() => setFilter(f)}>
              {f === 'all' ? 'All' : STATUS_LABEL[f]}
            </button>
          ))}
        </div>
        <div className={styles.actionGroup}>
          <button className={styles.iconBtn} onClick={doRefresh}>
            <RefreshCw size={13} className={refreshing ? styles.spin : ''} /> Refresh
          </button>
          <button className={styles.iconBtn}><Download size={13} /> Export</button>
        </div>
      </motion.div>

      {/* Detail Table */}
      <motion.div className={`${styles.card} ${styles.cardNopad}`} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.35 }}>
        <div className={styles.tblWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                {['Cycle','Status','Source','Extracted','Matched','Mismatch','Missing','Excess','Write-off','Debit (₹)','Credit (₹)','Variance','SLA','Actions'].map(h => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map(d => (
                <tr key={d.id} className={selectedCycle === d.id ? styles.rowSelected : ''} onClick={() => setSelectedCycle(selectedCycle === d.id ? null : d.id)}>
                  <td className={styles.tdAccent}>{d.id}</td>
                  <td>
                    <span className={styles.statusBadge}>
                      <span className={`${styles.statusDot} ${STATUS_CLS[d.status]}`} />
                      {STATUS_LABEL[d.status]}
                    </span>
                  </td>
                  <td className={styles.tdMuted}>{d.source}</td>
                  <td className={styles.tdMono}>{fmt(d.totalRecords)}</td>
                  <td className={`${styles.tdMono} ${styles.tdGreen}`}>{fmt(d.matched)}</td>
                  <td className={`${styles.tdMono} ${d.mismatched > 0 ? styles.tdRed : styles.tdMuted}`}>{fmt(d.mismatched)}</td>
                  <td className={`${styles.tdMono} ${d.missing > 0 ? styles.tdAmber : styles.tdMuted}`}>{fmt(d.missing)}</td>
                  <td className={`${styles.tdMono} ${d.excess > 0 ? styles.tdOrange : styles.tdMuted}`}>{fmt(d.excess)}</td>
                  <td className={styles.tdMono}>{fmt(d.writeOff)}</td>
                  <td className={styles.tdMono}>{fmtCur(d.debit)}</td>
                  <td className={styles.tdMono}>{fmtCur(d.credit)}</td>
                  <td className={`${styles.tdMono} ${d.variance > 50000 ? styles.tdRed : styles.tdMuted}`}>{fmtCur(d.variance)}</td>
                  <td><span className={`${styles.slaPill} ${SLA_CLS[d.slaStatus]}`}>{d.slaStatus.replace('_',' ').toUpperCase()}</span></td>
                  <td onClick={e => e.stopPropagation()}>
                    {d.status === 'failed'    && <button className={styles.actionBtnDanger}><RefreshCw size={10} /> Retry</button>}
                    {d.status === 'completed' && <button className={styles.actionBtnGhost}><ChevronRight size={10} /> Detail</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Bottom Row */}
      <div className={styles.bottomRow}>
        {/* Financial Summary */}
        <motion.div className={styles.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}>
          <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-4)' }}>Financial Summary</div>
          {[
            { label: 'Total Debit',  value: fmtCur(totals.debit),   cls: styles.finAccent },
            { label: 'Total Credit', value: fmtCur(totals.credit),  cls: styles.finGreen  },
            { label: 'Net Variance', value: fmtCur(totals.variance), cls: styles.finRed   },
          ].map((f, i, arr) => (
            <div key={f.label} className={`${styles.finRow} ${i < arr.length - 1 ? styles.finRowBorder : ''}`}>
              <span className={styles.finLabel}>{f.label}</span>
              <span className={`${styles.finValue} ${f.cls}`}>{f.value}</span>
            </div>
          ))}
        </motion.div>

        {/* Match Rate Trend */}
        <motion.div className={styles.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.45 }}>
          <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-3)' }}>Match Rate Trend (7 days)</div>
          <div className={styles.trendRow}>
            <Sparkline data={TREND_DATA} />
            <div>
              <div className={styles.trendBigNum}>{overallRate}%</div>
              <div className={styles.trendSub}>today</div>
            </div>
          </div>
          <div className={styles.barChartRow}>
            {TREND_DATA.map((v, i) => (
              <div key={i} className={styles.barChartCol}>
                <div className={styles.barChartBar}>
                  <div className={`${styles.barChartFill} ${i === TREND_DATA.length - 1 ? styles.barChartFillActive : ''}`} style={{ height: `${(v / 100) * 32}px` }} />
                </div>
                <div className={styles.barChartDay}>{['M','T','W','T','F','S','S'][i]}</div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Exception Categories */}
        <motion.div className={styles.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }}>
          <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-4)' }}>Top Exception Categories</div>
          {EXCEPTION_CATS.map((e) => (
            <div key={e.label} className={styles.exceptionRow}>
              <div className={styles.exceptionMeta}>
                <span className={styles.exceptionLabel}>{e.label}</span>
                <span className={`${styles.exceptionPct} ${e.cls}`}>{e.pct}%</span>
              </div>
              <div className={styles.progressTrack}>
                <div className={`${styles.progressFill} ${e.cls}`} style={{ width: `${e.pct}%` }} />
              </div>
            </div>
          ))}
        </motion.div>
      </div>
    </div>
  );
};

// ─── Config Screen ────────────────────────────────────────────────────────────
const ConfigScreen = ({ cycles, setCycles }) => {
  const [tab,       setTab]       = useState('cycles');
  const [editing,   setEditing]   = useState(null);
  const [draft,     setDraft]     = useState(null);
  const [showAudit, setShowAudit] = useState(false);

  const startEdit = (c)  => { setDraft({ ...c, matchKeys: [...c.matchKeys] }); setEditing(c.id); };
  const cancelEdit = ()  => { setEditing(null); setDraft(null); };
  const saveEdit = ()    => { setCycles(p => p.map(c => c.id === editing ? { ...draft } : c)); cancelEdit(); };
  const toggleKey = (k)  => setDraft(p => ({ ...p, matchKeys: p.matchKeys.includes(k) ? p.matchKeys.filter(x => x !== k) : [...p.matchKeys, k] }));

  const CONFIG_TABS = [
    { id: 'cycles',  label: 'Cycle Configuration',      icon: <Clock size={14} /> },
    { id: 'alerts',  label: 'Alerts & Notifications',   icon: <Bell  size={14} /> },
    { id: 'access',  label: 'Access Control',            icon: <User  size={14} /> },
  ];

  const CYCLE_STATS = [
    { label: 'Total Cycles',  value: cycles.length,                        cls: styles.configStatAccent  },
    { label: 'Active Cycles', value: cycles.filter(c => c.active).length,   cls: styles.configStatGreen   },
    { label: 'Recon Enabled', value: cycles.filter(c => c.reconFlag).length, cls: styles.configStatPurple  },
    { label: 'Cross-Day',     value: cycles.filter(c => c.startTime > c.endTime).length, cls: styles.configStatOrange },
  ];

  const AUDIT_LOG = [
    { user: 'admin@bank.co',     action: 'Modified Cycle 5C extraction time', time: '13-04-2026 09:42:10', version: 'v3.2' },
    { user: 'ops_lead@bank.co',  action: 'Enabled recon for Cycle 8C',        time: '12-04-2026 14:20:33', version: 'v3.1' },
    { user: 'admin@bank.co',     action: 'Added dependency 10C → 1C',         time: '11-04-2026 11:05:17', version: 'v3.0' },
    { user: 'auditor@bank.co',   action: 'Viewed configuration snapshot',     time: '10-04-2026 16:30:00', version: '—'    },
  ];

  const ALERT_SETTINGS = [
    { label: 'Extraction Failure',         desc: 'Notify when extraction fails for any cycle',   enabled: true  },
    { label: 'Recon Mismatch > Threshold', desc: 'Alert when mismatches exceed 5% of total',     enabled: true  },
    { label: 'Cycle Delay / SLA Breach',   desc: 'Warn when cycle exceeds SLA timer',            enabled: true  },
    { label: 'Auto Write-off Triggered',   desc: 'Log when auto write-off threshold is hit',     enabled: false },
  ];

  const ACCESS_ROLES = [
    { role: 'Admin',      perms: ['Full configuration access', 'User management', 'Audit log access'],  color: 'var(--color-error-500)',   bg: 'var(--color-error-100)',   users: 2 },
    { role: 'Operations', perms: ['Dashboard view', 'Re-trigger extraction/recon', 'Download reports'], color: 'var(--color-accent-500)',  bg: 'var(--color-accent-50)',   users: 8 },
    { role: 'Auditor',    perms: ['Read-only dashboard', 'Audit log access', 'Export reports'],          color: '#7C3AED',                  bg: '#F5F3FF',                  users: 3 },
  ];

  return (
    <div className={styles.screenWrap}>
      {/* Config Tabs */}
      <div className={styles.configTabs}>
        {CONFIG_TABS.map(t => (
          <button key={t.id} className={`${styles.configTab} ${tab === t.id ? styles.configTabActive : ''}`} onClick={() => setTab(t.id)}>
            {t.icon} {t.label}
          </button>
        ))}
      </div>

      {/* ── Cycles Tab ── */}
      {tab === 'cycles' && (
        <motion.div className={styles.screenWrap} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          {/* Summary stats */}
          <div className={styles.configStatGrid}>
            {CYCLE_STATS.map((st, i) => (
              <div key={i} className={`${styles.configStatCard} ${st.cls}`}>
                <div className={styles.configStatVal}>{st.value}</div>
                <div className={styles.configStatLabel}>{st.label}</div>
              </div>
            ))}
          </div>

          <div className={`${styles.card} ${styles.cardNopad}`}>
            <div className={styles.cardHead}>
              <span className={styles.cardTitle}>Cycle Master Configuration</span>
              <span className={styles.cardBadge}>{cycles.length} cycles</span>
              <button className={styles.iconBtn} onClick={() => setShowAudit(v => !v)}>
                {showAudit ? 'Hide' : 'Show'} Audit Log
              </button>
            </div>
            <div className={styles.tblWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    {['Cycle','Start','End','Extraction','Source','Recon','Active','SLA (min)','Match Keys','Dependency','Actions'].map(h => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {cycles.map(c => {
                    const isEd = editing === c.id;
                    const d = isEd ? draft : c;
                    return (
                      <tr key={c.id} className={isEd ? styles.rowEditing : ''}>
                        <td className={styles.tdAccent}>{c.id}</td>
                        {(['startTime','endTime','extractionTime'] ).map(field => (
                          <td key={field}>
                            {isEd
                              ? <input type="time" value={d[field]} onChange={e => setDraft({ ...d, [field]: e.target.value })} className={styles.inlineInput} />
                              : <span className={styles.tdMono}>{c[field]}</span>}
                          </td>
                        ))}
                        <td>
                          {isEd
                            ? <select value={d.source} onChange={e => setDraft({ ...d, source: e.target.value })} className={styles.inlineSelect}>
                                {SOURCE_OPTIONS.map(s => <option key={s}>{s}</option>)}
                              </select>
                            : <span className={styles.sourceBadge}>{c.source}</span>}
                        </td>
                        <td><Toggle checked={d.reconFlag} onChange={isEd ? v => setDraft({ ...d, reconFlag: v }) : () => {}} /></td>
                        <td><Toggle checked={d.active}   onChange={isEd ? v => setDraft({ ...d, active: v })   : () => {}} /></td>
                        <td>
                          {isEd
                            ? <input type="number" value={d.sla} onChange={e => setDraft({ ...d, sla: parseInt(e.target.value) || 0 })} className={styles.inlineInputSm} />
                            : <span style={{ fontSize: 'var(--text-sm)' }}>{c.sla}</span>}
                        </td>
                        <td>
                          {isEd
                            ? <div className={styles.matchKeyEditor}>
                                {MATCH_KEY_OPTIONS.map(k => (
                                  <span key={k} onClick={() => toggleKey(k)}
                                    className={`${styles.matchKeyChip} ${d.matchKeys.includes(k) ? styles.matchKeyActive : ''}`}>
                                    {k}
                                  </span>
                                ))}
                              </div>
                            : <div className={styles.matchKeyEditor}>
                                {c.matchKeys.map(k => <span key={k} className={`${styles.matchKeyChip} ${styles.matchKeyActive}`}>{k}</span>)}
                              </div>}
                        </td>
                        <td>
                          {isEd
                            ? <input value={d.dependency} onChange={e => setDraft({ ...d, dependency: e.target.value })} placeholder="e.g. 2C,3C" className={styles.inlineInputSm} />
                            : <span className={`${styles.tdMono} ${c.dependency ? styles.tdAmber : styles.tdMuted}`}>{c.dependency || '—'}</span>}
                        </td>
                        <td>
                          {isEd
                            ? <div style={{ display: 'flex', gap: 6 }}>
                                <button className={styles.actionBtnSave} onClick={saveEdit}><Save size={11} /> Save</button>
                                <button className={styles.actionBtnGhost} onClick={cancelEdit}><X size={11} /></button>
                              </div>
                            : <button className={styles.actionBtnGhost} onClick={() => startEdit(c)}><Edit2 size={11} /> Edit</button>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Audit Log */}
          <AnimatePresence>
            {showAudit && (
              <motion.div className={styles.card} initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} transition={{ duration: 0.2 }}>
                <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-4)' }}>Audit &amp; Version History</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                  {AUDIT_LOG.map((log, i) => (
                    <div key={i} className={styles.auditRow}>
                      <span className={styles.auditVersion}>{log.version}</span>
                      <span className={styles.auditAction}>{log.action}</span>
                      <span className={styles.auditUser}>{log.user}</span>
                      <span className={`${styles.auditTime} ${styles.tdMono}`}>{log.time}</span>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      )}

      {/* ── Alerts Tab ── */}
      {tab === 'alerts' && (
        <motion.div className={styles.card} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-5)' }}>Alert &amp; Notification Settings</div>
          <div className={styles.alertGrid}>
            {ALERT_SETTINGS.map((a, i) => (
              <div key={i} className={styles.alertCard}>
                <div>
                  <div className={styles.alertTitle}>{a.label}</div>
                  <div className={styles.alertDesc}>{a.desc}</div>
                </div>
                <Toggle checked={a.enabled} onChange={() => {}} />
              </div>
            ))}
          </div>
          <div className={styles.alertInputRow}>
            <div className={styles.fld}>
              <label className={styles.fldLabel}>Email Recipients</label>
              <input className={styles.fldInput} defaultValue="ops@bank.co, alerts@bank.co" />
            </div>
            <div className={styles.fld}>
              <label className={styles.fldLabel}>SMS Recipients</label>
              <input className={styles.fldInput} defaultValue="+91-98XXXXXXXX" />
            </div>
          </div>
          <div className={styles.escalationWrap}>
            <div className={styles.escalationTitle}>Escalation Matrix</div>
            <div className={styles.escalationGrid}>
              {['L1: Ops Team (0–15 min)', 'L2: Ops Manager (15–30 min)', 'L3: Head of Operations (30+ min)'].map((l, i) => (
                <div key={i} className={styles.escalationCard}>{l}</div>
              ))}
            </div>
          </div>
        </motion.div>
      )}

      {/* ── Access Tab ── */}
      {tab === 'access' && (
        <motion.div className={styles.card} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <div className={styles.cardTitle} style={{ marginBottom: 'var(--space-5)' }}>Role-Based Access Control</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {ACCESS_ROLES.map((r, i) => (
              <div key={i} className={styles.accessCard}>
                <div className={styles.accessIcon} style={{ background: r.bg, color: r.color }}>
                  <User size={20} />
                </div>
                <div style={{ flex: 1 }}>
                  <div className={styles.accessRole}>{r.role}</div>
                  <div className={styles.accessPerms}>{r.perms.join('  •  ')}</div>
                </div>
                <span className={styles.accessBadge} style={{ color: r.color, background: r.bg }}>{r.users} users</span>
              </div>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
};

// ─── Main Page ────────────────────────────────────────────────────────────────
const UpiReconDashboard = () => {
  const [screen, setScreen] = useState('dashboard');
  const [cycles, setCycles] = useState(INITIAL_CYCLES);
  const now = new Date();
  const bizDate = `${String(now.getDate()).padStart(2,'0')}-${String(now.getMonth()+1).padStart(2,'0')}-${now.getFullYear()}`;

  return (
    <div className={styles.page}>
      <PageHeader title="UPI RECON (Cycle Wise)" description="Intraday reconciliation across settlement cycles">
        <div className={styles.headerMeta}>
          <span className={styles.headerDate}>{bizDate}</span>
          <span className={styles.headerBizDate}>Business Date: <strong>{bizDate}</strong></span>
        </div>
        <div className={styles.screenTabs}>
          {[
            { id: 'dashboard', label: 'Dashboard',     icon: <LayoutDashboard size={13} /> },
            { id: 'config',    label: 'Configuration', icon: <Settings        size={13} /> },
          ].map(s => (
            <button key={s.id} className={`${styles.screenTab} ${screen === s.id ? styles.screenTabActive : ''}`} onClick={() => setScreen(s.id)}>
              {s.icon} {s.label}
            </button>
          ))}
        </div>
      </PageHeader>

      <AnimatePresence mode="wait">
        <motion.div key={screen} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }}>
          {screen === 'dashboard'
            ? <DashboardScreen cycles={cycles} />
            : <ConfigScreen cycles={cycles} setCycles={setCycles} />}
        </motion.div>
      </AnimatePresence>
    </div>
  );
};

export default UpiReconDashboard;
