import { motion } from 'framer-motion';
import { CheckCircle, Clock, Lock, Info } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import s from '../_shared.module.css';
import styles from './RoleApprovalSent.module.css';

const TIMELINE = [
  { icon: <CheckCircle size={12} />, iconColor: 'var(--color-success-500)', dotBg: 'var(--color-success-100)', title: 'Role form submitted', desc: 'Role details, permissions and module mapping completed', time: 'Today, 14:32', done: true },
  { icon: <CheckCircle size={12} />, iconColor: 'var(--color-success-500)', dotBg: 'var(--color-success-100)', title: 'Approval email dispatched', desc: "Secure link sent to approver's official email address", time: 'Today, 14:32', done: true },
  { icon: <Clock size={12} />, iconColor: 'var(--color-info-500)', dotBg: 'var(--color-info-100)', title: 'Awaiting approver decision', desc: 'Approver will approve or reject via the email link — or from the Role list screen', time: null, active: true },
  { icon: <Lock size={12} />, iconColor: 'var(--color-neutral-400)', dotBg: 'var(--color-neutral-100)', title: 'Role activated in system', desc: 'Role becomes available for user assignment upon approval. Can be inactivated or disapproved later.', time: null },
];

const RoleApprovalSent = () => {
  const navigate = useNavigate();

  return (
    <div className={styles.outerPage}>
      <motion.div className={styles.card} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.35 }}>
        {/* Banner */}
        <div className={styles.cardBanner}>
          <div className={styles.successRing}>
            <svg viewBox="0 0 24 24" fill="none" strokeWidth="2.5" stroke="var(--color-success-500)" width="28" height="28"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <h1 className={styles.bannerTitle}>Role submitted for approval</h1>
          <p className={styles.bannerSub}>
            The new role has been sent to the approver.<br />
            A secure approval link has been dispatched to their official email.
          </p>
          <div className={styles.refWrap}>
            <div className={styles.refBox}>
              <span className={styles.refLbl}>Reference</span>
              <span className={`${styles.refCode} ${s.mono}`}>ROLE-REQ-2025-0041</span>
            </div>
          </div>
        </div>

        {/* Body */}
        <div className={styles.cardBody}>
          <div className={styles.timeline}>
            {TIMELINE.map((step, i) => (
              <div key={i} className={`${styles.tlItem} ${i < TIMELINE.length - 1 ? styles.tlItemLine : ''}`}>
                <div className={styles.tlDot} style={{ background: step.dotBg, color: step.iconColor }}>
                  {step.icon}
                </div>
                <div className={styles.tlContent}>
                  <div className={styles.tlTitle}>{step.title}</div>
                  <div className={styles.tlDesc}>{step.desc}</div>
                  {step.time && <div className={`${styles.tlTime} ${s.mono}`}>{step.time}</div>}
                </div>
              </div>
            ))}
          </div>

          <div className={`${s.infoBox} ${s.infoGold}`} style={{ marginBottom: 'var(--space-5)' }}>
            <Info size={15} style={{ flexShrink: 0, marginTop: 1 }} />
            <span>All role changes are audit-logged. Track status from the <strong>Role &amp; Permissions</strong> list. An approved role can be inactivated or disapproved at any time from the role's detail page.</span>
          </div>

          <div className={styles.actions}>
            <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/add-role')}>Add another role</button>
            <button className={`${s.btn} ${s.btnOutline}`} onClick={() => navigate('/new-config/view-role')}>View this role</button>
            <button className={`${s.btn} ${s.btnGold}`} onClick={() => navigate('/new-config/role-list')}>
              Back to roles →
            </button>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default RoleApprovalSent;
