import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Lock, Users, Plus, Send, Clock, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../../components/common';
import s from '../_shared.module.css';
import styles from './MakerQueue.module.css';

const ROLES = [
  { id: 1, name: 'Ops Manager',    code: 'ROLE-0039', type: 'Internal', modules: 3, valid: '31-03-2026', status: 'Draft',   createdBy: 'Akshay R.', date: '14-04-2026' },
  { id: 2, name: 'CBS Reconciler', code: 'ROLE-0041', type: 'Internal', modules: 2, valid: '31-03-2026', status: 'Draft',   createdBy: 'Akshay R.', date: '14-04-2026' },
  { id: 3, name: 'Branch Auditor', code: 'ROLE-0042', type: 'External', modules: 1, valid: '30-06-2026', status: 'Draft',   createdBy: 'Akshay R.', date: '13-04-2026' },
  { id: 4, name: 'NACH Maker',     code: 'ROLE-0043', type: 'Internal', modules: 2, valid: '31-12-2026', status: 'Pending', createdBy: 'Akshay R.', date: '12-04-2026' },
  { id: 5, name: 'RTGS Viewer',    code: 'ROLE-0044', type: 'External', modules: 1, valid: '30-06-2026', status: 'Pending', createdBy: 'Akshay R.', date: '11-04-2026' },
];

const USERS = [
  { id: 1, name: 'Priya Patel',  email: 'priya.patel@bank.in',  role: 'UPI Maker',  status: 'Draft',   date: '14-04-2026' },
  { id: 2, name: 'Raj Kumar',    email: 'raj.kumar@bank.in',    role: 'IMPS Maker', status: 'Draft',   date: '13-04-2026' },
  { id: 3, name: 'Sonia Singh',  email: 'sonia.singh@bank.in',  role: 'Ops Manager',status: 'Pending', date: '12-04-2026' },
];

const STATS_ROLES = [
  { label: 'Roles in Queue', value: '5', sub: '2 ready to submit', subColor: 'var(--color-info-500)', iconBg: 'var(--color-info-100)', iconColor: 'var(--color-info-500)', icon: <Lock size={18} /> },
  { label: 'Draft Roles', value: '3', sub: 'Not yet submitted', subColor: 'var(--color-neutral-500)', iconBg: 'var(--color-neutral-100)', iconColor: 'var(--color-neutral-500)', icon: <FileText size={18} /> },
  { label: 'Pending Review', value: '2', sub: 'Awaiting checker', subColor: '#92400e', iconBg: 'var(--color-warning-100)', iconColor: '#92400e', icon: <Clock size={18} /> },
  { label: 'Users in Queue', value: '3', sub: 'Across 2 roles', subColor: 'var(--color-success-500)', iconBg: 'var(--color-success-100)', iconColor: 'var(--color-success-500)', icon: <Users size={18} /> },
];

const STATS_USERS = STATS_ROLES.slice().reverse();

const statusCls = { Draft: s.pillDraft, Pending: s.pillPending, Active: s.pillActive };

const MakerQueue = () => {
  const navigate = useNavigate();
  const [tab, setTab] = useState('roles');
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [search, setSearch] = useState('');

  const toggleRole = (id) => setSelectedRoles(p => p.includes(id) ? p.filter(x => x !== id) : [...p, id]);
  const toggleUser = (id) => setSelectedUsers(p => p.includes(id) ? p.filter(x => x !== id) : [...p, id]);

  const stats = tab === 'roles' ? STATS_ROLES : STATS_USERS;

  return (
    <div className={s.page}>
      <PageHeader title="Maker Queue" description="Create roles & users, select items and submit to checker for approval in bulk">
        <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/role-list')}>← All Roles</button>
        <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/add-role')}><Plus size={13} /> New Role</button>
        <button className={`${s.btn} ${s.btnGold} ${s.btnLg}`}><Plus size={13} /> New User</button>
      </PageHeader>

      {/* Tabs */}
      <div className={s.tabs}>
        <button className={`${s.tab} ${tab === 'roles' ? s.tabActive : ''}`} onClick={() => setTab('roles')}>
          <Lock size={14} /> Roles Queue <span className={s.tabCnt}>5</span>
        </button>
        <button className={`${s.tab} ${tab === 'users' ? s.tabActive : ''}`} onClick={() => setTab('users')}>
          <Users size={14} /> Users Queue <span className={s.tabCnt}>3</span>
        </button>
      </div>

      {/* Stats */}
      <motion.div className={s.stats} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
        {stats.map((st, i) => (
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

      {/* Table */}
      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1, duration: 0.3 }}>
        <div className={s.cardHead}>
          {tab === 'roles' ? <Lock size={14} color="rgba(255,255,255,0.7)" /> : <Users size={14} color="rgba(255,255,255,0.7)" />}
          <span className={s.cardTitle}>{tab === 'roles' ? 'Roles Queue' : 'Users Queue'}</span>
          <div className={s.cardSub}>
            {(tab === 'roles' ? selectedRoles : selectedUsers).length > 0 && (
              <button className={`${s.btn} ${s.btnSm}`} style={{ background: 'var(--color-gold-500)', color: 'var(--color-primary-900)', border: 'none' }}>
                <Send size={11} /> Submit {(tab === 'roles' ? selectedRoles : selectedUsers).length} to Checker
              </button>
            )}
          </div>
        </div>
        <div className={s.cardBody}>
          {/* Bulk bar */}
          {(tab === 'roles' ? selectedRoles : selectedUsers).length > 0 && (
            <div className={`${s.bbar} ${s.bbarShow} ${s.bbarNavy}`} style={{ marginBottom: 'var(--space-4)' }}>
              <div className={styles.bbarIcon}><Send size={16} color="var(--color-gold-400)" /></div>
              <div>
                <div className={s.bbarTxt}>{(tab === 'roles' ? selectedRoles : selectedUsers).length} items selected</div>
                <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 'var(--text-xs)' }}>Ready to submit to checker for review</div>
              </div>
              <div style={{ display: 'flex', gap: 'var(--space-2)', marginLeft: 'auto' }}>
                <button className={`${s.btn} ${s.btnSm}`} style={{ background: 'var(--color-gold-500)', color: 'var(--color-primary-900)', border: 'none' }}><Send size={11} /> Submit to Checker</button>
                <button className={`${s.btn} ${s.btnSm} ${s.btnOutline}`} style={{ color: 'var(--color-white)', borderColor: 'rgba(255,255,255,0.3)' }} onClick={() => tab === 'roles' ? setSelectedRoles([]) : setSelectedUsers([])}>Clear</button>
              </div>
            </div>
          )}

          <div className={s.toolbar}>
            <div className={s.toolbarLeft}>
              <div className={s.searchWrap}>
                <Search size={13} className={s.searchIcon} />
                <input className={s.searchInput} placeholder={tab === 'roles' ? 'Search roles…' : 'Search users…'} value={search} onChange={e => setSearch(e.target.value)} />
              </div>
            </div>
          </div>

          {tab === 'roles' ? (
            <div className={s.tblWrap}>
              <table className={s.table}>
                <thead><tr>
                  <th style={{ width: 34 }}><input type="checkbox" onChange={e => setSelectedRoles(e.target.checked ? ROLES.map(r => r.id) : [])} style={{ accentColor: 'var(--color-primary-900)' }} /></th>
                  <th>Role Name</th><th>Role Code</th><th>Type</th><th>Modules</th><th>Valid Until</th><th>Status</th><th>Created</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {ROLES.map(r => (
                    <tr key={r.id} className={selectedRoles.includes(r.id) ? s.selected : ''}>
                      <td><input type="checkbox" checked={selectedRoles.includes(r.id)} onChange={() => toggleRole(r.id)} style={{ accentColor: 'var(--color-primary-900)' }} /></td>
                      <td style={{ fontWeight: 'var(--font-semibold)' }}>{r.name}</td>
                      <td><span className={s.mono}>{r.code}</span></td>
                      <td><span className={`${s.typeBadge} ${r.type === 'External' ? s.typeBadgeExt : ''}`}>{r.type}</span></td>
                      <td><span className={s.modCnt}>{r.modules}</span></td>
                      <td><span className={s.mono} style={{ fontSize: 11 }}>{r.valid}</span></td>
                      <td><span className={`${s.pill} ${statusCls[r.status]}`}>{r.status}</span></td>
                      <td><span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)' }}>{r.date}</span></td>
                      <td>
                        <span className={s.al} onClick={() => navigate('/new-config/view-role')}>View</span>
                        <span className={s.al}>Edit</span>
                        <span className={`${s.al} ${s.alRed}`}>Delete</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className={s.tblWrap}>
              <table className={s.table}>
                <thead><tr>
                  <th style={{ width: 34 }}><input type="checkbox" onChange={e => setSelectedUsers(e.target.checked ? USERS.map(u => u.id) : [])} style={{ accentColor: 'var(--color-primary-900)' }} /></th>
                  <th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Date</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {USERS.map(u => (
                    <tr key={u.id} className={selectedUsers.includes(u.id) ? s.selected : ''}>
                      <td><input type="checkbox" checked={selectedUsers.includes(u.id)} onChange={() => toggleUser(u.id)} style={{ accentColor: 'var(--color-primary-900)' }} /></td>
                      <td style={{ fontWeight: 'var(--font-semibold)' }}>{u.name}</td>
                      <td>{u.email}</td>
                      <td><span className={`${s.typeBadge}`}>{u.role}</span></td>
                      <td><span className={`${s.pill} ${statusCls[u.status]}`}>{u.status}</span></td>
                      <td><span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)' }}>{u.date}</span></td>
                      <td>
                        <span className={s.al}>View</span>
                        <span className={s.al}>Edit</span>
                        <span className={`${s.al} ${s.alRed}`}>Delete</span>
                      </td>
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

export default MakerQueue;
