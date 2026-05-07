import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Lock, Users, Clock, CheckCircle, XCircle, BarChart2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../../components/common';
import s from '../_shared.module.css';
import styles from './CheckerQueue.module.css';

const PENDING_ROLES = [
  { id: 1, name: 'Ops Manager',    code: 'ROLE-0039', type: 'Internal', modules: 3, submittedBy: 'Akshay R.', date: '14-04-2026', status: 'Pending' },
  { id: 2, name: 'CBS Reconciler', code: 'ROLE-0041', type: 'Internal', modules: 2, submittedBy: 'Akshay R.', date: '14-04-2026', status: 'Pending' },
  { id: 3, name: 'Branch Auditor', code: 'ROLE-0042', type: 'External', modules: 1, submittedBy: 'Akshay R.', date: '13-04-2026', status: 'Pending' },
  { id: 4, name: 'NACH Maker',     code: 'ROLE-0043', type: 'Internal', modules: 2, submittedBy: 'Akshay R.', date: '12-04-2026', status: 'Pending' },
];

const PENDING_USERS = [
  { id: 1, name: 'Priya Patel',  email: 'priya.patel@bank.in',  role: 'UPI Maker',   submittedBy: 'Akshay R.', date: '14-04-2026', status: 'Pending' },
  { id: 2, name: 'Raj Kumar',    email: 'raj.kumar@bank.in',    role: 'IMPS Maker',  submittedBy: 'Akshay R.', date: '13-04-2026', status: 'Pending' },
  { id: 3, name: 'Sonia Singh',  email: 'sonia.singh@bank.in',  role: 'Ops Manager', submittedBy: 'Akshay R.', date: '12-04-2026', status: 'Pending' },
];

const HISTORY = [
  { ref: 'ROLE-REQ-2025-0040', name: 'Report Viewer', type: 'Role', decision: 'Approved', by: 'Rohit S.', date: '10-04-2026' },
  { ref: 'USR-REQ-2025-0021',  name: 'Amit Verma',   type: 'User', decision: 'Approved', by: 'Rohit S.', date: '09-04-2026' },
  { ref: 'ROLE-REQ-2025-0038', name: 'RTGS Viewer',   type: 'Role', decision: 'Disapproved', by: 'Rohit S.', date: '08-04-2026' },
  { ref: 'USR-REQ-2025-0019',  name: 'Deepa Nair',   type: 'User', decision: 'Approved', by: 'Rohit S.', date: '07-04-2026' },
  { ref: 'ROLE-REQ-2025-0037', name: 'NACH Checker',  type: 'Role', decision: 'Approved', by: 'Rohit S.', date: '06-04-2026' },
  { ref: 'USR-REQ-2025-0018',  name: 'Ravi Iyer',    type: 'User', decision: 'Disapproved', by: 'Rohit S.', date: '05-04-2026' },
  { ref: 'ROLE-REQ-2025-0036', name: 'Branch Viewer', type: 'Role', decision: 'Approved', by: 'Rohit S.', date: '04-04-2026' },
];

const STATS = [
  { label: 'Roles Pending', value: '4', sub: 'Awaiting decision', subColor: '#92400e', iconBg: 'var(--color-warning-100)', iconColor: '#92400e', icon: <Clock size={18} /> },
  { label: 'Users Pending', value: '3', sub: 'Awaiting decision', subColor: '#92400e', iconBg: 'var(--color-warning-100)', iconColor: '#92400e', icon: <Users size={18} /> },
  { label: 'Approved Today', value: '2', sub: 'Items processed', subColor: 'var(--color-success-500)', iconBg: 'var(--color-success-100)', iconColor: 'var(--color-success-500)', icon: <CheckCircle size={18} /> },
  { label: 'History Records', value: '7', sub: 'Total decisions', subColor: 'var(--color-info-500)', iconBg: 'var(--color-info-100)', iconColor: 'var(--color-info-500)', icon: <BarChart2 size={18} /> },
];

const CheckerQueue = () => {
  const navigate = useNavigate();
  const [tab, setTab] = useState('roles');
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [roleStatuses, setRoleStatuses] = useState({});
  const [userStatuses, setUserStatuses] = useState({});

  const toggleRole = (id) => setSelectedRoles(p => p.includes(id) ? p.filter(x => x !== id) : [...p, id]);
  const toggleUser = (id) => setSelectedUsers(p => p.includes(id) ? p.filter(x => x !== id) : [...p, id]);

  const decideRole = (id, decision) => setRoleStatuses(prev => ({ ...prev, [id]: decision }));
  const decideUser = (id, decision) => setUserStatuses(prev => ({ ...prev, [id]: decision }));

  const getDecisionCls = (dec) => dec === 'Approved' ? s.pillActive : dec === 'Disapproved' ? s.pillDisapproved : s.pillPending;

  return (
    <div className={s.page}>
      <PageHeader title="Checker Queue" description="Review, approve or disapprove roles & users submitted by makers — individually or in bulk">
        <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/maker-queue')}>← Maker Queue</button>
        <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/role-list')}>All Roles</button>
      </PageHeader>

      {/* Tabs */}
      <div className={s.tabs}>
        <button className={`${s.tab} ${tab === 'roles' ? s.tabActive : ''}`} onClick={() => setTab('roles')}>
          <Lock size={14} /> Roles for Approval <span className={s.tabCnt}>4</span>
        </button>
        <button className={`${s.tab} ${tab === 'users' ? s.tabActive : ''}`} onClick={() => setTab('users')}>
          <Users size={14} /> Users for Approval <span className={s.tabCnt}>3</span>
        </button>
        <button className={`${s.tab} ${tab === 'history' ? s.tabActive : ''}`} onClick={() => setTab('history')}>
          <BarChart2 size={14} /> Decision History
          <span className={s.tabCnt} style={{ background: tab === 'history' ? 'rgba(255,255,255,0.2)' : 'var(--color-success-100)', color: tab === 'history' ? '#fff' : 'var(--color-success-500)' }}>7</span>
        </button>
      </div>

      {/* Stats */}
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

      {/* Content */}
      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1, duration: 0.3 }}>
        <div className={s.cardHead}>
          {tab === 'history' ? <BarChart2 size={14} color="rgba(255,255,255,0.7)" /> : tab === 'roles' ? <Lock size={14} color="rgba(255,255,255,0.7)" /> : <Users size={14} color="rgba(255,255,255,0.7)" />}
          <span className={s.cardTitle}>{tab === 'roles' ? 'Roles for Approval' : tab === 'users' ? 'Users for Approval' : 'Decision History'}</span>
          <span className={s.cardSub}>{tab === 'history' ? '7 records' : tab === 'roles' ? `${selectedRoles.length} selected` : `${selectedUsers.length} selected`}</span>
        </div>
        <div className={s.cardBody}>
          {/* Bulk approve/disapprove bar */}
          {(tab === 'roles' && selectedRoles.length > 0) && (
            <div className={`${styles.approveBar} ${styles.approveBarShow}`}>
              <div className={styles.abLeft}>
                <div className={styles.abIcon} style={{ background: 'var(--color-success-100)' }}><CheckCircle size={18} color="var(--color-success-500)" /></div>
                <div>
                  <div className={styles.abTxt}>{selectedRoles.length} roles selected for bulk action</div>
                  <div className={styles.abSub}>Choose an action to apply to all selected items</div>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                <button className={`${s.btn} ${s.btnSm} ${s.btnSuccess}`}><CheckCircle size={11} /> Approve All</button>
                <button className={`${s.btn} ${s.btnSm} ${s.btnDanger}`}><XCircle size={11} /> Disapprove All</button>
                <button className={`${s.btn} ${s.btnSm} ${s.btnOutline}`} onClick={() => setSelectedRoles([])}>Clear</button>
              </div>
            </div>
          )}

          {tab !== 'history' && (
            <div className={s.toolbar}>
              <div className={s.toolbarLeft}>
                <div className={s.searchWrap}>
                  <Search size={13} className={s.searchIcon} />
                  <input className={s.searchInput} placeholder="Search…" value={search} onChange={e => setSearch(e.target.value)} />
                </div>
              </div>
            </div>
          )}

          {tab === 'roles' && (
            <div className={s.tblWrap}>
              <table className={s.table}>
                <thead><tr>
                  <th style={{ width: 34 }}><input type="checkbox" onChange={e => setSelectedRoles(e.target.checked ? PENDING_ROLES.map(r => r.id) : [])} style={{ accentColor: 'var(--color-primary-900)' }} /></th>
                  <th>Role Name</th><th>Code</th><th>Type</th><th>Modules</th><th>Submitted By</th><th>Date</th><th>Status</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {PENDING_ROLES.map(r => {
                    const dec = roleStatuses[r.id];
                    return (
                      <tr key={r.id} className={`${selectedRoles.includes(r.id) ? s.selected : ''} ${dec ? styles.decidedRow : ''}`}>
                        <td><input type="checkbox" checked={selectedRoles.includes(r.id)} onChange={() => toggleRole(r.id)} style={{ accentColor: 'var(--color-primary-900)' }} /></td>
                        <td style={{ fontWeight: 'var(--font-semibold)' }}>{r.name}</td>
                        <td><span className={s.mono}>{r.code}</span></td>
                        <td><span className={`${s.typeBadge} ${r.type === 'External' ? s.typeBadgeExt : ''}`}>{r.type}</span></td>
                        <td><span className={s.modCnt}>{r.modules}</span></td>
                        <td>{r.submittedBy}</td>
                        <td><span className={s.mono} style={{ fontSize: 11 }}>{r.date}</span></td>
                        <td><span className={`${s.pill} ${getDecisionCls(dec || r.status)}`}>{dec || r.status}</span></td>
                        <td>
                          <span className={s.al} onClick={() => navigate('/new-config/view-role')}>View</span>
                          {!dec && <><span className={`${s.al} ${s.alGreen}`} onClick={() => decideRole(r.id, 'Approved')}>Approve</span><span className={`${s.al} ${s.alRed}`} onClick={() => decideRole(r.id, 'Disapproved')}>Disapprove</span></>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {tab === 'users' && (
            <div className={s.tblWrap}>
              <table className={s.table}>
                <thead><tr>
                  <th style={{ width: 34 }}><input type="checkbox" onChange={e => setSelectedUsers(e.target.checked ? PENDING_USERS.map(u => u.id) : [])} style={{ accentColor: 'var(--color-primary-900)' }} /></th>
                  <th>Name</th><th>Email</th><th>Role</th><th>Submitted By</th><th>Date</th><th>Status</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {PENDING_USERS.map(u => {
                    const dec = userStatuses[u.id];
                    return (
                      <tr key={u.id} className={`${selectedUsers.includes(u.id) ? s.selected : ''} ${dec ? styles.decidedRow : ''}`}>
                        <td><input type="checkbox" checked={selectedUsers.includes(u.id)} onChange={() => toggleUser(u.id)} style={{ accentColor: 'var(--color-primary-900)' }} /></td>
                        <td style={{ fontWeight: 'var(--font-semibold)' }}>{u.name}</td>
                        <td>{u.email}</td>
                        <td><span className={s.typeBadge}>{u.role}</span></td>
                        <td>{u.submittedBy}</td>
                        <td><span className={s.mono} style={{ fontSize: 11 }}>{u.date}</span></td>
                        <td><span className={`${s.pill} ${getDecisionCls(dec || u.status)}`}>{dec || u.status}</span></td>
                        <td>
                          <span className={s.al}>View</span>
                          {!dec && <><span className={`${s.al} ${s.alGreen}`} onClick={() => decideUser(u.id, 'Approved')}>Approve</span><span className={`${s.al} ${s.alRed}`} onClick={() => decideUser(u.id, 'Disapproved')}>Disapprove</span></>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {tab === 'history' && (
            <div className={s.tblWrap}>
              <table className={s.table}>
                <thead><tr>
                  <th>Reference</th><th>Name</th><th>Type</th><th>Decision</th><th>Decided By</th><th>Date</th>
                </tr></thead>
                <tbody>
                  {HISTORY.map((h, i) => (
                    <tr key={i}>
                      <td><span className={s.mono}>{h.ref}</span></td>
                      <td style={{ fontWeight: 'var(--font-semibold)' }}>{h.name}</td>
                      <td><span className={`${s.typeBadge}`}>{h.type}</span></td>
                      <td><span className={`${s.pill} ${h.decision === 'Approved' ? s.pillApproved : s.pillDisapproved}`}>{h.decision}</span></td>
                      <td>{h.by}</td>
                      <td><span className={s.mono} style={{ fontSize: 11 }}>{h.date}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
};

export default CheckerQueue;
