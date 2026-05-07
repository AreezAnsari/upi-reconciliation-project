import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Shield, CheckCircle, Clock, Users, Plus, Upload, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../../components/common';
import s from '../_shared.module.css';
import styles from './RoleList.module.css';

const ROLES_DATA = [
  { id: 1, name: 'UPI Maker', code: 'ROLE-0001', type: 'Internal', modules: ['UPI','RTGS','NEFT','NACH'], users: 5, valid: '31-03-2026', status: 'Active' },
  { id: 2, name: 'UPI Checker', code: 'ROLE-0002', type: 'Internal', modules: ['UPI','RTGS'], users: 2, valid: '31-03-2026', status: 'Active' },
  { id: 3, name: 'IMPS Maker', code: 'ROLE-0003', type: 'Internal', modules: ['IMPS','NEFT'], users: 3, valid: '31-12-2025', status: 'Active' },
  { id: 4, name: 'Report Viewer', code: 'ROLE-0004', type: 'External', modules: ['Report'], users: 8, valid: '31-03-2026', status: 'Active' },
  { id: 5, name: 'Admin Super', code: 'ROLE-0005', type: 'Internal', modules: ['Admin','Setup','Report','Process'], users: 1, valid: '31-03-2026', status: 'Approved' },
  { id: 6, name: 'Branch Viewer', code: 'ROLE-0006', type: 'External', modules: ['Report'], users: 5, valid: '30-06-2026', status: 'Inactive' },
  { id: 7, name: 'Ops Manager', code: 'ROLE-0039', type: 'Internal', modules: ['UPI','IMPS','NACH'], users: 0, valid: '31-03-2026', status: 'Pending' },
  { id: 8, name: 'CBS Reconciler', code: 'ROLE-0041', type: 'Internal', modules: ['Process','Report'], users: 0, valid: '—', status: 'Draft' },
];

const STATS = [
  { label: 'Total Roles', value: '8', sub: '↑ 2 this month', subColor: 'var(--color-success-500)', icon: <Shield size={18} />, iconBg: 'var(--color-info-100)', iconColor: 'var(--color-info-500)' },
  { label: 'Active Roles', value: '6', sub: '75% of total', subColor: 'var(--color-success-500)', icon: <CheckCircle size={18} />, iconBg: 'var(--color-success-100)', iconColor: 'var(--color-success-500)' },
  { label: 'Pending Approval', value: '1', sub: 'Awaiting action', subColor: '#92400e', icon: <Clock size={18} />, iconBg: 'var(--color-warning-100)', iconColor: '#92400e' },
  { label: 'Users Assigned', value: '24', sub: 'across all roles', subColor: 'var(--color-neutral-500)', icon: <Users size={18} />, iconBg: 'var(--color-gold-100)', iconColor: 'var(--color-gold-600)' },
];

const statusClass = { Active: s.pillActive, Approved: s.pillApproved, Inactive: s.pillInactive, Pending: s.pillPending, Draft: s.pillDraft, Disapproved: s.pillDisapproved };

const RoleList = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [selected, setSelected] = useState([]);
  const [showBulkModal, setShowBulkModal] = useState(false);

  const filtered = ROLES_DATA.filter(r =>
    (r.name.toLowerCase().includes(search.toLowerCase()) || r.code.toLowerCase().includes(search.toLowerCase())) &&
    (!statusFilter || r.status === statusFilter) &&
    (!typeFilter || r.type === typeFilter)
  );

  const toggleRow = (id) => setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  const toggleAll = (checked) => setSelected(checked ? filtered.map(r => r.id) : []);

  return (
    <div className={s.page}>
      <PageHeader title="Role & Permissions" description="Define roles, assign module-level permissions and map users">
        <button className={`${s.btn} ${s.btnOutline}`} onClick={() => setShowBulkModal(true)}>
          <Upload size={13} /> Bulk Upload
        </button>
        <button className={`${s.btn} ${s.btnGold} ${s.btnLg}`} onClick={() => navigate('/new-config/add-role')}>
          <Plus size={13} /> Add New Role
        </button>
      </PageHeader>

      <motion.div className={s.stats} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
        {STATS.map((st, i) => (
          <div key={i} className={s.stat}>
            <div className={s.statIcon} style={{ background: st.iconBg, color: st.iconColor }}>{st.icon}</div>
            <div>
              <div className={s.statLabel}>{st.label}</div>
              <div className={s.statVal}>{st.value}</div>
              <div className={s.statSub} style={{ color: st.subColor }}>{st.sub}</div>
            </div>
          </div>
        ))}
      </motion.div>

      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1, duration: 0.3 }}>
        <div className={s.cardHead}>
          <Shield size={14} color="rgba(255,255,255,0.7)" />
          <span className={s.cardTitle}>All Roles</span>
          <span className={s.cardSub}>{filtered.length} roles</span>
        </div>
        <div className={s.cardBody}>
          {selected.length > 0 && (
            <div className={`${s.bbar} ${s.bbarShow} ${s.bbarNavy}`} style={{ marginBottom: 'var(--space-4)' }}>
              <span className={s.bbarTxt}>{selected.length} selected</span>
              <button className={`${s.btn} ${s.btnSm} ${s.btnSuccess}`}>Approve</button>
              <button className={`${s.btn} ${s.btnSm} ${s.btnWarn}`}>Inactivate</button>
              <button className={`${s.btn} ${s.btnSm} ${s.btnDanger}`}>Disapprove</button>
              <button className={`${s.btn} ${s.btnSm} ${s.btnOutline}`} style={{ color: 'var(--color-white)', borderColor: 'rgba(255,255,255,0.3)' }} onClick={() => setSelected([])}>Clear</button>
            </div>
          )}
          <div className={s.toolbar}>
            <div className={s.toolbarLeft}>
              <div className={s.searchWrap}>
                <Search size={13} className={s.searchIcon} />
                <input className={s.searchInput} placeholder="Search roles…" value={search} onChange={e => setSearch(e.target.value)} />
              </div>
              <select className={s.filterSelect} value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                <option value="">All statuses</option>
                {['Active','Inactive','Pending','Draft','Approved','Disapproved'].map(st => <option key={st}>{st}</option>)}
              </select>
              <select className={s.filterSelect} value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
                <option value="">All types</option>
                <option>Internal</option><option>External</option>
              </select>
            </div>
          </div>
          <div className={s.tblWrap}>
            <table className={s.table}>
              <thead><tr>
                <th style={{ width: 34 }}><input type="checkbox" checked={selected.length === filtered.length && filtered.length > 0} onChange={e => toggleAll(e.target.checked)} style={{ accentColor: 'var(--color-primary-900)' }} /></th>
                <th>Role Name</th><th>Role Code</th><th>Type</th><th>Modules</th><th>Users</th><th>Valid Until</th><th>Status</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {filtered.map(r => (
                  <tr key={r.id} className={selected.includes(r.id) ? s.selected : ''}>
                    <td><input type="checkbox" checked={selected.includes(r.id)} onChange={() => toggleRow(r.id)} style={{ accentColor: 'var(--color-primary-900)' }} /></td>
                    <td style={{ fontWeight: 'var(--font-semibold)' }}>{r.name}</td>
                    <td><span className={s.mono}>{r.code}</span></td>
                    <td><span className={`${s.typeBadge} ${r.type === 'External' ? s.typeBadgeExt : ''}`}>{r.type}</span></td>
                    <td><span className={s.modCnt}>{r.modules.length}</span> <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)', marginLeft: 4 }}>{r.modules.slice(0,2).join(', ')}{r.modules.length > 2 ? '…' : ''}</span></td>
                    <td>{r.users}</td>
                    <td><span className={s.mono} style={{ fontSize: 11 }}>{r.valid}</span></td>
                    <td><span className={`${s.pill} ${statusClass[r.status] || ''}`}>{r.status}</span></td>
                    <td>
                      <span className={s.al} onClick={() => navigate('/new-config/view-role')}>View</span>
                      <span className={s.al}>Edit</span>
                      <span className={`${s.al} ${s.alAmber}`}>Inactivate</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </motion.div>

      {/* Bulk Upload Modal */}
      {showBulkModal && (
        <div className={`${s.overlay} ${s.overlayShow}`} onClick={() => setShowBulkModal(false)}>
          <div className={s.modal} onClick={e => e.stopPropagation()}>
            <div className={s.modalHead}>
              <h3><Upload size={14} color="rgba(255,255,255,0.8)" /> Bulk Upload Roles</h3>
              <button className={s.modalClose} onClick={() => setShowBulkModal(false)}>×</button>
            </div>
            <div className={s.modalBody}>
              <div className={s.tmplStrip}>
                <span>Download template to ensure correct column format</span>
                <a>Download Template (CSV)</a>
              </div>
              <div className={s.dropZone}>
                <Upload size={36} color="var(--color-neutral-400)" style={{ margin: '0 auto 12px' }} />
                <h4 style={{ fontSize: 'var(--text-sm)', fontWeight: 'var(--font-semibold)', marginBottom: 4 }}>Drop file here or click to browse</h4>
                <p style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)' }}>Supported formats — max 5 MB</p>
                <div className={styles.fchips}>
                  <span className={styles.fchipCsv}>CSV</span>
                  <span className={styles.fchipXl}>XLSX</span>
                  <span className={styles.fchipXml}>XML</span>
                </div>
              </div>
            </div>
            <div className={s.modalFooter}>
              <button className={`${s.btn} ${s.btnOutline}`} onClick={() => setShowBulkModal(false)}>Cancel</button>
              <button className={`${s.btn} ${s.btnNavy}`} disabled><Upload size={13} /> Upload &amp; Process</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RoleList;
