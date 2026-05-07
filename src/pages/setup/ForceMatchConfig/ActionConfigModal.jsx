import { useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Plus, X, Loader2 } from 'lucide-react';
import { Button } from '../../../components/common';
import styles from './ForceMatchConfig.module.css';

const ActionConfigModal = ({ open, editing, form, onChange, onSubmit, onClose, submitting }) => {
  const firstInputRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const timer = setTimeout(() => firstInputRef.current?.focus(), 100);
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';
    return () => {
      clearTimeout(timer);
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <motion.div
      className={styles.modalOverlay}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="action-modal-title"
    >
      <motion.div
        className={styles.modal}
        initial={{ opacity: 0, y: 30, scale: 0.97 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 30, scale: 0.97 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.modalHeader}>
          <h2 id="action-modal-title">
            <Plus size={18} />
            {editing ? 'Edit Action Definition' : 'New Action Definition'}
          </h2>
          <button className={styles.modalClose} onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        <div className={styles.modalBody}>
          {/* Account Config */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Account Config
          </h3>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-debit">Debit Account</label>
              <input ref={firstInputRef} id="fa-debit" className={styles.formInput} type="text" value={form.debitAccount} onChange={(e) => onChange('debitAccount', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-credit">Credit Account</label>
              <input id="fa-credit" className={styles.formInput} type="text" value={form.creditAccount} onChange={(e) => onChange('creditAccount', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-dc">DC Indicator</label>
              <input id="fa-dc" className={styles.formInput} type="text" value={form.dcIndicator} onChange={(e) => onChange('dcIndicator', e.target.value)} />
            </div>
          </div>

          {/* Narration */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Narration
          </h3>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="fa-narration">
              Narration
              <span className={styles.charCount}>{form.narrationFull?.length || 0} / 4000</span>
            </label>
            <textarea
              id="fa-narration"
              className={`${styles.formInput} ${styles.formTextarea}`}
              value={form.narrationFull}
              onChange={(e) => onChange('narrationFull', e.target.value)}
              rows={3}
              maxLength={4000}
            />
          </div>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-debitNarr">Debit Narration</label>
              <input id="fa-debitNarr" className={styles.formInput} type="text" value={form.debitNarration} onChange={(e) => onChange('debitNarration', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-creditNarr">Credit Narration</label>
              <input id="fa-creditNarr" className={styles.formInput} type="text" value={form.creditNarration} onChange={(e) => onChange('creditNarration', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-delimiter">Delimiter</label>
              <input id="fa-delimiter" className={styles.formInput} type="text" value={form.delimiter} onChange={(e) => onChange('delimiter', e.target.value)} />
            </div>
          </div>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-narrConst">Narration Constant</label>
              <input id="fa-narrConst" className={styles.formInput} type="text" value={form.narrationConstant} onChange={(e) => onChange('narrationConstant', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-narrDebit">Narration Const (Debit)</label>
              <input id="fa-narrDebit" className={styles.formInput} type="text" value={form.narrationConstDebit} onChange={(e) => onChange('narrationConstDebit', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-narrCredit">Narration Const (Credit)</label>
              <input id="fa-narrCredit" className={styles.formInput} type="text" value={form.narrationConstCredit} onChange={(e) => onChange('narrationConstCredit', e.target.value)} />
            </div>
          </div>

          {/* Data Table & Meta */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Data Table & Meta
          </h3>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-dataTable">Active Data Table</label>
              <input id="fa-dataTable" className={styles.formInput} type="text" value={form.dataTable} onChange={(e) => onChange('dataTable', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-ruleId">Rule ID</label>
              <input id="fa-ruleId" className={styles.formInput} type="text" value={form.ruleId} onChange={(e) => onChange('ruleId', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-instCode">Inst Code</label>
              <input id="fa-instCode" className={styles.formInput} type="text" value={form.instCode} onChange={(e) => onChange('instCode', e.target.value)} />
            </div>
          </div>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-remarks">Remarks</label>
              <input id="fa-remarks" className={styles.formInput} type="text" value={form.remarks} onChange={(e) => onChange('remarks', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-insUser">Ins User</label>
              <input id="fa-insUser" className={styles.formInput} type="text" value={form.insUser} onChange={(e) => onChange('insUser', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fa-lupdUser">LUPD User</label>
              <input id="fa-lupdUser" className={styles.formInput} type="text" value={form.lupdUser} onChange={(e) => onChange('lupdUser', e.target.value)} />
            </div>
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.cancelBtn} onClick={onClose}>Cancel</button>
          <Button variant="gold" onClick={onSubmit} disabled={submitting} leftIcon={submitting ? <Loader2 size={16} className={styles.spinner} /> : <Plus size={16} />}>
            {submitting ? 'Saving...' : (editing ? 'Update Action' : 'Create Action')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default ActionConfigModal;
