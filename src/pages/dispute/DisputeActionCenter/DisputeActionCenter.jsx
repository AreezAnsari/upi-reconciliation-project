import { useState } from 'react';
import { motion } from 'framer-motion';
import { Download, Upload, SlidersHorizontal, Info, FileUp } from 'lucide-react';
import { PageHeader } from '../../../components/common';
import styles from './DisputeActionCenter.module.css';

const STAGE_CHIPS = ['All', 'Early', 'Intermediary', 'Terminal', 'Resolved'];

const RECORDS = [
  { rrn: '412908765432', adjRef: 'ADJ20260410001', type: 'P2M CB', amount: '₹4,500', merchant: 'FlipMart Pvt Ltd', stage: 'early', tat: '2d', tatWarn: false, financial: true },
  { rrn: '412907654321', adjRef: 'ADJ20260409015', type: 'P2P Adj', amount: '₹12,000', merchant: '—', stage: 'intermediary', tat: '8d', tatWarn: false, financial: true },
  { rrn: '412906543210', adjRef: 'ADJ20260405042', type: 'P2M Disp', amount: '₹850', merchant: 'QuickPay Services', stage: 'terminal', tat: '28d ⚠', tatWarn: true, financial: false },
  { rrn: '412905432109', adjRef: 'ADJ20260403078', type: 'P2P CB', amount: '₹25,000', merchant: '—', stage: 'resolved', tat: '5d', tatWarn: false, financial: true },
  { rrn: '412904321098', adjRef: 'ADJ20260410003', type: 'P2M Adj', amount: '₹3,200', merchant: 'Zap Payments', stage: 'early', tat: '1d', tatWarn: false, financial: true },
  { rrn: '412903210987', adjRef: 'ADJ20260408022', type: 'P2M CB', amount: '₹78,500', merchant: 'MegaStore Online', stage: 'intermediary', tat: '22d ⚠', tatWarn: true, financial: true },
];

const stageBadgeClass = (stage) => ({
  early: styles.badgeEarly,
  intermediary: styles.badgeIntermediary,
  terminal: styles.badgeTerminal,
  resolved: styles.badgeResolved,
}[stage] || styles.badgeEarly);

const stageLabel = (s) => s.charAt(0).toUpperCase() + s.slice(1);

const DisputeActionCenter = () => {
  const [filterOpen, setFilterOpen] = useState(true);
  const [activeChip, setActiveChip] = useState('All');
  const [selected, setSelected] = useState([]);
  const [filters, setFilters] = useState({
    rrn: '',
    product: '',
    txnType: '',
    tat: '',
    dateFrom: '',
    dateTo: '',
    financial: '',
    bankRole: '',
  });

  const toggleRow = (rrn) => setSelected((prev) =>
    prev.includes(rrn) ? prev.filter((r) => r !== rrn) : [...prev, rrn]
  );
  const toggleAll = () =>
    setSelected((prev) => (prev.length === RECORDS.length ? [] : RECORDS.map((r) => r.rrn)));

  const filtered = RECORDS.filter((r) => {
    if (activeChip !== 'All' && r.stage !== activeChip.toLowerCase()) return false;
    if (filters.rrn && !r.rrn.includes(filters.rrn)) return false;
    return true;
  });

  const handleFilterChange = (e) =>
    setFilters((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const resetFilters = () => {
    setFilters({ rrn: '', product: '', txnType: '', tat: '', dateFrom: '', dateTo: '', financial: '', bankRole: '' });
    setActiveChip('All');
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="Dispute Action Center"
        description="Search, filter and act on dispute records"
      >
        <button className={styles.btnIconOutline} onClick={() => setFilterOpen((p) => !p)}>
          <SlidersHorizontal size={15} /> Filters
        </button>
      </PageHeader>

      <motion.div
        className={styles.layout}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {/* Filter Panel */}
        {filterOpen && (
          <aside className={styles.filterPanel}>
            <h3 className={styles.filterTitle}>Search &amp; Filters</h3>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>RRN / UTXN ID</label>
              <input
                className={styles.filterInput}
                type="text"
                name="rrn"
                placeholder="Enter RRN or UTXN..."
                value={filters.rrn}
                onChange={handleFilterChange}
                style={{ fontFamily: 'var(--font-mono)' }}
              />
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Product</label>
              <select className={styles.filterInput} name="product" value={filters.product} onChange={handleFilterChange}>
                <option value="">All Products</option>
                <option>UPI</option><option>IMPS</option><option>AEPS</option>
              </select>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Status</label>
              <div className={styles.chipRow}>
                {STAGE_CHIPS.map((chip) => (
                  <button
                    key={chip}
                    className={`${styles.chip} ${activeChip === chip ? styles.chipActive : ''}`}
                    onClick={() => setActiveChip(chip)}
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Transaction Type</label>
              <select className={styles.filterInput} name="txnType" value={filters.txnType} onChange={handleFilterChange}>
                <option value="">All Types</option>
                <option>P2P - Adjustment</option>
                <option>P2P - Dispute</option>
                <option>P2M - Adjustment</option>
                <option>P2M - Dispute</option>
              </select>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>TAT Stage</label>
              <select className={styles.filterInput} name="tat" value={filters.tat} onChange={handleFilterChange}>
                <option value="">All</option>
                <option>Within SLA</option>
                <option>SLA Warning (&gt;20 days)</option>
                <option>SLA Breached (&gt;30 days)</option>
              </select>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Date Range</label>
              <div className={styles.dateRow}>
                <input className={styles.filterInput} type="date" name="dateFrom" value={filters.dateFrom} onChange={handleFilterChange} />
                <input className={styles.filterInput} type="date" name="dateTo" value={filters.dateTo} onChange={handleFilterChange} />
              </div>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Financial Priority</label>
              <select className={styles.filterInput} name="financial" value={filters.financial} onChange={handleFilterChange}>
                <option value="">All</option>
                <option>Financial</option>
                <option>Non-Financial</option>
              </select>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>Bank Role</label>
              <select className={styles.filterInput} name="bankRole" value={filters.bankRole} onChange={handleFilterChange}>
                <option value="">All Roles</option>
                <option>Remitter</option>
                <option>Beneficiary</option>
              </select>
            </div>

            <div className={styles.filterActions}>
              <button className={styles.btnPrimary}>Apply Filters</button>
              <button className={styles.btnOutline} onClick={resetFilters}>Reset</button>
            </div>
          </aside>
        )}

        {/* Main Table Area */}
        <div className={styles.content}>
          <div className={styles.contentHeader}>
            <div>
              <h2 className={styles.contentTitle}>Dispute Records</h2>
              <p className={styles.resultCount}>Showing 1–{filtered.length} of 2,847 disputes</p>
            </div>
            <div className={styles.toolbar}>
              <button className={styles.toolbarBtn}><Download size={13} /> Export CSV</button>
              <button className={styles.toolbarBtn}><Download size={13} /> Download Action Files</button>
              <button className={`${styles.toolbarBtn} ${styles.toolbarBtnGreen}`}><Upload size={13} /> Bulk Upload</button>
            </div>
          </div>

          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th><input type="checkbox" onChange={toggleAll} checked={selected.length === RECORDS.length} /></th>
                  <th>RRN / UTR</th>
                  <th>Adj Ref ID</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Merchant / Partner</th>
                  <th>Stage</th>
                  <th>TAT</th>
                  <th>Financial</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((row) => (
                  <tr key={row.rrn} className={selected.includes(row.rrn) ? styles.rowSelected : ''}>
                    <td><input type="checkbox" checked={selected.includes(row.rrn)} onChange={() => toggleRow(row.rrn)} /></td>
                    <td className={styles.mono}>{row.rrn}</td>
                    <td className={styles.mono}>{row.adjRef}</td>
                    <td>{row.type}</td>
                    <td className={styles.amountCell}>{row.amount}</td>
                    <td>{row.merchant}</td>
                    <td><span className={`${styles.badge} ${stageBadgeClass(row.stage)}`}>{stageLabel(row.stage)}</span></td>
                    <td className={row.tatWarn ? styles.tatWarn : styles.tatOk}>{row.tat}</td>
                    <td>
                      {row.financial
                        ? <span className={`${styles.badge} ${styles.badgeFinancial}`}>Financial</span>
                        : <span className={styles.nonFin}>Non-Fin</span>}
                    </td>
                    <td>
                      <div className={styles.actionCell}>
                        {row.stage !== 'resolved' && (
                          <button className={`${styles.actionBtn} ${styles.actionEvidence}`}>
                            <FileUp size={11} /> Evidence
                          </button>
                        )}
                        <button className={styles.actionBtn}>
                          <Info size={11} /> {row.stage === 'resolved' ? 'View' : 'Action'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className={styles.pagination}>
              <span className={styles.pageInfo}>Page 1 of 114</span>
              <div className={styles.pageBtns}>
                {['‹', '1', '2', '3', '...', '114', '›'].map((p, i) => (
                  <button key={i} className={`${styles.pageBtn} ${p === '1' ? styles.pageBtnActive : ''}`}>{p}</button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default DisputeActionCenter;
