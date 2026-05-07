import { useState } from 'react';
import { motion } from 'framer-motion';
import { Lock, FileText, Users } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../../components/common';
import s from '../_shared.module.css';
import styles from './ViewRole.module.css';

const PERM_DATA = [
  { module: 'UPI',    access: true,  view: true,  create: true,  edit: true,  approve: false, download: false },
  { module: 'IMPS',   access: true,  view: true,  create: false, edit: false, approve: false, download: false },
  { module: 'NEFT',   access: true,  view: true,  create: true,  edit: false, approve: false, download: true  },
  { module: 'RTGS',   access: false, view: false, create: false, edit: false, approve: false, download: false },
  { module: 'Report', access: true,  view: true,  create: false, edit: false, approve: false, download: true  },
  { module: 'Admin',  access: false, view: false, create: false, edit: false, approve: false, download: false },
];

const USERS_DATA = [
  { initials: 'RS', name: 'Rohit Sharma',   email: 'rohit.sharma@bank.in',   phone: '+91 98765 43210', status: 'Active' },
  { initials: 'PP', name: 'Priya Patel',    email: 'priya.patel@bank.in',    phone: '+91 87654 32109', status: 'Active' },
  { initials: 'AK', name: 'Ajay Kumar',     email: 'ajay.kumar@bank.in',     phone: '+91 76543 21098', status: 'Inactive' },
  { initials: 'SM', name: 'Sunita Mehta',   email: 'sunita.mehta@bank.in',   phone: '+91 65432 10987', status: 'Active' },
  { initials: 'VR', name: 'Vikram Reddy',   email: 'vikram.reddy@bank.in',   phone: '+91 54321 09876', status: 'Active' },
];

const PERM_COLS = ['access', 'view', 'create', 'edit', 'approve', 'download'];
const STATUS_COLORS = { Active: 'var(--color-success-500)', Approved: 'var(--color-success-500)', Inactive: 'var(--color-neutral-500)', Disapproved: 'var(--color-error-500)' };

const CheckMark = ({ yes }) => yes
  ? <span className={styles.chkYes}><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--color-success-500)" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
  : <span className={styles.chkNo}><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--color-neutral-400)" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></span>;

const ViewRole = () => {
  const navigate = useNavigate();
  const [roleStatus, setRoleStatus] = useState('Active');

  const handleStatus = (ns) => setRoleStatus(ns);

  const statusBadgeCls = { Active: styles.rbadgeActive, Approved: styles.rbadgeApproved, Inactive: styles.rbadgeInactive, Disapproved: styles.rbadgeDisapproved, Pending: styles.rbadgePending };

  return (
    <div className={s.page}>
      <PageHeader title="UPI Maker" description="Role details, permissions and assigned users">
        {(roleStatus === 'Active' || roleStatus === 'Inactive' || roleStatus === 'Disapproved') && (
          <button className={`${s.btn} ${s.btnSuccess}`} onClick={() => handleStatus('Approved')}>✓ Approve</button>
        )}
        {(roleStatus === 'Active' || roleStatus === 'Approved') && (
          <button className={`${s.btn} ${s.btnWarn}`} onClick={() => handleStatus('Inactive')}>— Inactivate</button>
        )}
        {(roleStatus === 'Active' || roleStatus === 'Approved' || roleStatus === 'Inactive') && (
          <button className={`${s.btn} ${s.btnDanger}`} onClick={() => handleStatus('Disapproved')}>✕ Disapprove</button>
        )}
        <button className={`${s.btn} ${s.btnGold}`} onClick={() => navigate('/new-config/add-role')}>✎ Edit Role</button>
      </PageHeader>

      {/* Hero */}
      <motion.div className={styles.roleHero} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
        <div className={styles.heroTop}>
          <div>
            <div className={styles.roleIcon}><Lock size={22} /></div>
            <div className={styles.roleName}>UPI Maker</div>
            <div className={styles.roleCode}>ROLE-0001</div>
            <div className={styles.rbadges}>
              <span className={`${styles.rbadge} ${statusBadgeCls[roleStatus]}`}>{roleStatus}</span>
              <span className={`${styles.rbadge} ${styles.rbadgeInternal}`}>Internal</span>
            </div>
          </div>
        </div>
        <div className={styles.heroMeta}>
          {[['Users Assigned','5'],['Modules Mapped','4'],['Valid Until','31-03-2026'],['Created By','Ajay Sharma']].map(([label, val]) => (
            <div key={label} className={styles.metaItem}>
              <div className={styles.metaLabel}>{label}</div>
              <div className={styles.metaVal}>{val}</div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* Status panel */}
      <div className={styles.statusPanel}>
        <div>
          <div className={styles.statusPanelLabel}>Current Status: <span style={{ color: STATUS_COLORS[roleStatus] || 'var(--color-neutral-500)' }}>{roleStatus}</span></div>
          <div className={styles.statusPanelSub}>Use the buttons above to approve, inactivate, or disapprove this role. Re-activate an inactivated role at any time.</div>
        </div>
        {(roleStatus === 'Inactive' || roleStatus === 'Disapproved') && (
          <button className={`${s.btn} ${s.btnSuccess}`} onClick={() => handleStatus('Active')}>✓ Re-activate</button>
        )}
      </div>

      {/* Role Details */}
      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1, duration: 0.3 }}>
        <div className={s.cardHead}><FileText size={14} color="rgba(255,255,255,0.7)" /><span className={s.cardTitle}>Role Details</span></div>
        <div style={{ padding: 0 }}>
          <div className={styles.detailsGrid}>
            <div className={styles.detailCol}>
              {[['Role Name','UPI Maker',true,false],['Role Code','ROLE-0001',false,true],['Role Type','Internal',false,false],['Status',null,false,false]].map(([label, val, bold, mono]) => (
                <div key={label} className={styles.detailRow}>
                  <div className={styles.detailLabel}>{label}</div>
                  <div className={`${styles.detailValue} ${mono ? s.mono : ''}`} style={{ fontWeight: bold ? 'var(--font-semibold)' : undefined }}>
                    {label === 'Status' ? <span className={`${s.pill} ${s.pillActive}`}>{roleStatus}</span> : val}
                  </div>
                </div>
              ))}
            </div>
            <div className={`${styles.detailCol} ${styles.detailColRight}`}>
              {[['Description','Initiates and submits UPI transactions for checker review'],['Valid From','01-04-2025'],['Valid To','31-03-2026'],['Created On','12-01-2025']].map(([label, val]) => (
                <div key={label} className={styles.detailRow}>
                  <div className={styles.detailLabel}>{label}</div>
                  <div className={styles.detailValue}>{val}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </motion.div>

      {/* Module Permissions */}
      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15, duration: 0.3 }}>
        <div className={s.cardHead}><Lock size={14} color="rgba(255,255,255,0.7)" /><span className={s.cardTitle}>Module Permissions</span><span className={s.cardSub}>4 of 12 modules enabled</span></div>
        <div style={{ padding: 0, overflowX: 'auto' }}>
          <table className={styles.permView}>
            <thead><tr>
              <th style={{ textAlign: 'left' }}>Module</th>
              {PERM_COLS.map(col => <th key={col}>{col.charAt(0).toUpperCase() + col.slice(1)}</th>)}
            </tr></thead>
            <tbody>
              {PERM_DATA.map(p => (
                <tr key={p.module} className={!p.access ? styles.rowDis : ''}>
                  <td style={{ textAlign: 'left', fontWeight: 'var(--font-medium)' }}>{p.module}</td>
                  {PERM_COLS.map(col => <td key={col}><CheckMark yes={p[col]} /></td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Assigned Users */}
      <motion.div className={s.card} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2, duration: 0.3 }}>
        <div className={s.cardHead}>
          <Users size={14} color="rgba(255,255,255,0.7)" />
          <span className={s.cardTitle}>Users Assigned to this Role</span>
          <span className={s.cardSub}><span style={{ color: 'var(--color-gold-400)', fontWeight: 'var(--font-semibold)', cursor: 'pointer' }}>+ Assign new user</span></span>
        </div>
        <div style={{ padding: 0 }}>
          <table className={styles.usersTbl}>
            <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {USERS_DATA.map(u => (
                <tr key={u.email}>
                  <td>
                    <div className={styles.nmCell}>
                      <span className={styles.ua}>{u.initials}</span>
                      {u.name}
                    </div>
                  </td>
                  <td>{u.email}</td>
                  <td><span className={s.mono}>{u.phone}</span></td>
                  <td><span className={`${s.pill} ${u.status === 'Active' ? s.pillActive : s.pillInactive}`}>{u.status}</span></td>
                  <td>
                    <span className={s.al}>View</span>
                    <span className={`${s.al} ${s.alRed}`}>Remove</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>
    </div>
  );
};

export default ViewRole;
