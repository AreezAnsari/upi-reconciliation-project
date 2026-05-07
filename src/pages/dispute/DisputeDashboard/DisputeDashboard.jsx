import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { AlertTriangle, X, TrendingUp, TrendingDown, ChevronRight } from 'lucide-react';
import { PageHeader } from '../../../components/common';
import styles from './DisputeDashboard.module.css';

const trendData = [
  { month: 'Jan', Disputes: 118, Resolved: 90 },
  { month: 'Feb', Disputes: 128, Resolved: 98 },
  { month: 'Mar', Disputes: 123, Resolved: 94 },
  { month: 'Apr', Disputes: 144, Resolved: 114 },
  { month: 'May', Disputes: 164, Resolved: 134 },
  { month: 'Jun', Disputes: 154, Resolved: 124 },
  { month: 'Jul', Disputes: 184, Resolved: 154 },
];

const recentDisputes = [
  { rrn: '412908765432', type: 'P2M CB', amount: '₹4,500', merchant: 'FlipMart Pvt Ltd', stage: 'early', tat: '2 days', updated: '10 Apr 2026' },
  { rrn: '412907654321', type: 'P2P Adj', amount: '₹12,000', merchant: '—', stage: 'intermediary', tat: '8 days', updated: '09 Apr 2026' },
  { rrn: '412906543210', type: 'P2M Disp', amount: '₹850', merchant: 'QuickPay Services', stage: 'terminal', tat: '28 days', updated: '08 Apr 2026' },
  { rrn: '412905432109', type: 'P2P CB', amount: '₹25,000', merchant: '—', stage: 'resolved', tat: '5 days', updated: '07 Apr 2026' },
  { rrn: '412904321098', type: 'P2M Adj', amount: '₹3,200', merchant: 'Zap Payments', stage: 'early', tat: '1 day', updated: '10 Apr 2026' },
];

const kpis = [
  { label: 'Total Disputes', value: '2,847', sub: '↑ 12% vs last month', trend: 'up', accent: 'blue' },
  { label: 'Aging (> 30 Days)', value: '342', sub: '↑ 5% needs attention', trend: 'down', accent: 'red' },
  { label: 'Amount at Risk', value: '₹2.41 Cr', sub: '↑ ₹18L from yesterday', trend: 'down', accent: 'gold' },
  { label: 'Resolution Rate', value: '94.7%', sub: '↑ 2.1% SLA compliance', trend: 'up', accent: 'green' },
];

const DONUT_SEGMENTS = [
  { label: 'Early', pct: 33, color: '#234b73', dasharray: 113, offset: 0 },
  { label: 'Intermediary', pct: 23, color: '#f59e0b', dasharray: 78, offset: -113 },
  { label: 'Terminal', pct: 15, color: '#ef4444', dasharray: 50, offset: -191 },
  { label: 'Resolved', pct: 29, color: '#10b981', dasharray: 99, offset: -241 },
];

const stageBadgeClass = (stage) => ({
  early: styles.badgeEarly,
  intermediary: styles.badgeIntermediary,
  terminal: styles.badgeTerminal,
  resolved: styles.badgeResolved,
}[stage] || styles.badgeEarly);

const stageLabel = (stage) => stage.charAt(0).toUpperCase() + stage.slice(1);

const cardVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: (i) => ({ opacity: 1, y: 0, transition: { delay: i * 0.06, duration: 0.35 } }),
};

const DisputeDashboard = () => {
  const [alertVisible, setAlertVisible] = useState(true);

  return (
    <div className={styles.page}>
      <PageHeader
        title="Dispute Management — Dashboard"
        description="Settlement Cycle: SC-20260410"
      >
        <button className={styles.btnOutline}>Export Report</button>
        <Link to="/dispute/action-center" className={styles.btnPrimary}>+ New Dispute</Link>
      </PageHeader>

      {alertVisible && (
        <motion.div
          className={styles.alertBanner}
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.25 }}
        >
          <div className={styles.alertIcon}><AlertTriangle size={18} /></div>
          <div className={styles.alertBody}>
            <strong>SLA Alert: 18 disputes approaching TAT breach (&gt;25 days)</strong>
            <p>Evidence pending for 12 chargebacks with financial impact ₹8.2L.</p>
          </div>
          <button className={styles.alertClose} onClick={() => setAlertVisible(false)} aria-label="Dismiss">
            <X size={16} />
          </button>
        </motion.div>
      )}

      {/* KPI Row */}
      <div className={styles.kpiRow}>
        {kpis.map((kpi, i) => (
          <motion.div
            key={kpi.label}
            className={`${styles.kpiCard} ${styles[`kpi_${kpi.accent}`]}`}
            custom={i}
            variants={cardVariants}
            initial="hidden"
            animate="visible"
          >
            <div className={styles.kpiLabel}>{kpi.label}</div>
            <div className={styles.kpiValue}>{kpi.value}</div>
            <div className={`${styles.kpiSub} ${kpi.trend === 'up' ? styles.kpiUp : styles.kpiDown}`}>
              {kpi.trend === 'up' ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
              {kpi.sub}
            </div>
          </motion.div>
        ))}
      </div>

      {/* Charts Row */}
      <motion.div
        className={styles.chartsRow}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.28, duration: 0.35 }}
      >
        {/* Trend Chart */}
        <div className={styles.chartCard}>
          <h3 className={styles.chartTitle}>Dispute Count &amp; Resolution Trend</h3>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={trendData} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="colorDisputes" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#234b73" stopOpacity={0.18} />
                  <stop offset="95%" stopColor="#234b73" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="colorResolved" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.06)" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '12px' }}
                cursor={{ stroke: 'rgba(0,0,0,0.08)', strokeWidth: 1 }}
              />
              <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
              <Area type="monotone" dataKey="Disputes" stroke="#234b73" strokeWidth={2.5} fill="url(#colorDisputes)" dot={false} activeDot={{ r: 4 }} />
              <Area type="monotone" dataKey="Resolved" stroke="#10b981" strokeWidth={2.5} fill="url(#colorResolved)" dot={false} activeDot={{ r: 4 }} />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Donut Chart */}
        <div className={styles.chartCard}>
          <h3 className={styles.chartTitle}>Dispute Stage Distribution</h3>
          <div className={styles.donutWrap}>
            <svg className={styles.donutChart} viewBox="0 0 140 140">
              <circle cx="70" cy="70" r="54" fill="none" stroke="#e5e7eb" strokeWidth="14" />
              {DONUT_SEGMENTS.map((seg) => (
                <circle
                  key={seg.label}
                  cx="70" cy="70" r="54"
                  fill="none"
                  stroke={seg.color}
                  strokeWidth="14"
                  strokeDasharray={`${seg.dasharray} ${340 - seg.dasharray}`}
                  strokeDashoffset={seg.offset}
                  transform="rotate(-90 70 70)"
                />
              ))}
              <text x="70" y="66" textAnchor="middle" fontSize="20" fontWeight="700" fill="#111827">2,847</text>
              <text x="70" y="82" textAnchor="middle" fontSize="9" fill="#9ca3af">TOTAL</text>
            </svg>
            <ul className={styles.donutLegend}>
              {DONUT_SEGMENTS.map((seg) => (
                <li key={seg.label} className={styles.legendItem}>
                  <span className={styles.legendDot} style={{ background: seg.color }} />
                  {seg.label} ({seg.pct}%)
                </li>
              ))}
            </ul>
          </div>
        </div>
      </motion.div>

      {/* Recent Disputes Table */}
      <motion.div
        className={styles.recentSection}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.38, duration: 0.35 }}
      >
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>Recent Disputes</h3>
          <Link to="/dispute/action-center" className={styles.viewAllBtn}>
            View All <ChevronRight size={14} />
          </Link>
        </div>
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>RRN / UTR</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Merchant</th>
                <th>Stage</th>
                <th>TAT</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {recentDisputes.map((row) => (
                <tr key={row.rrn}>
                  <td className={styles.mono}>{row.rrn}</td>
                  <td>{row.type}</td>
                  <td className={styles.amountCell}>{row.amount}</td>
                  <td>{row.merchant}</td>
                  <td><span className={`${styles.badge} ${stageBadgeClass(row.stage)}`}>{stageLabel(row.stage)}</span></td>
                  <td className={row.stage === 'terminal' ? styles.tatWarn : styles.tatOk}>{row.tat}</td>
                  <td className={styles.mutedText}>{row.updated}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>
    </div>
  );
};

export default DisputeDashboard;
