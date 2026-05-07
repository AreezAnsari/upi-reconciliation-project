import { useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import { Plus, X, Loader2 } from 'lucide-react';
import { Button } from '../../../components/common';
import { CATEGORY_OPTIONS } from './constants';
import styles from './ForceMatchConfig.module.css';

const YNToggle = ({ field, value, onChange, ariaLabel }) => (
  <div className={styles.ynToggle} role="radiogroup" aria-label={ariaLabel}>
    <button
      type="button"
      role="radio"
      aria-checked={value === 'Y'}
      className={`${styles.ynBtn} ${value === 'Y' ? styles.ynActive : ''}`}
      onClick={() => onChange(field, 'Y')}
    >
      Y
    </button>
    <button
      type="button"
      role="radio"
      aria-checked={value === 'N'}
      className={`${styles.ynBtn} ${value === 'N' ? styles.ynActiveN : ''}`}
      onClick={() => onChange(field, 'N')}
    >
      N
    </button>
  </div>
);

const ProcessConfigModal = ({ open, editing, form, onChange, onSubmit, onClose, submitting }) => {
  const firstInputRef = useRef(null);

  // Focus first input on open, lock body scroll, Escape to close
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

  const handleCategoryChange = useCallback((e) => {
    const opt = CATEGORY_OPTIONS.find((c) => c.value === e.target.value);
    onChange('categoryId', e.target.value);
    onChange('categoryName', opt ? opt.label.split(' - ')[1] : '');
  }, [onChange]);

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
      aria-labelledby="process-modal-title"
    >
      <motion.div
        className={styles.modal}
        initial={{ opacity: 0, y: 30, scale: 0.97 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 30, scale: 0.97 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.modalHeader}>
          <h2 id="process-modal-title">
            <Plus size={18} />
            {editing ? 'Edit Process Configuration' : 'New Process Configuration'}
          </h2>
          <button className={styles.modalClose} onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        <div className={styles.modalBody}>
          {/* Top fields */}
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-processId">Process ID</label>
              <input ref={firstInputRef} id="fm-processId" className={styles.formInput} type="text" value={form.processId} onChange={(e) => onChange('processId', e.target.value)} placeholder="Enter process ID" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-category">Category</label>
              <select id="fm-category" className={styles.formInput} value={form.categoryId} onChange={handleCategoryChange}>
                <option value="">-- select --</option>
                {CATEGORY_OPTIONS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-execOrder">Exec Order</label>
              <input id="fm-execOrder" className={styles.formInput} type="number" value={form.execOrder || ''} onChange={(e) => onChange('execOrder', e.target.value)} placeholder="Order" />
            </div>
          </div>

          {/* Source Flags */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Source Flags (Temp 1-4)
          </h3>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Temp Flag 1</label>
              <YNToggle field="tempFlag1" value={form.tempFlag1} onChange={onChange} ariaLabel="Temp Flag 1" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Temp Flag 2</label>
              <YNToggle field="tempFlag2" value={form.tempFlag2} onChange={onChange} ariaLabel="Temp Flag 2" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Temp Flag 3</label>
              <YNToggle field="tempFlag3" value={form.tempFlag3} onChange={onChange} ariaLabel="Temp Flag 3" />
            </div>
          </div>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Temp Flag 4</label>
              <YNToggle field="tempFlag4" value={form.tempFlag4} onChange={onChange} ariaLabel="Temp Flag 4" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-tempId1">Temp ID 1</label>
              <input id="fm-tempId1" className={styles.formInput} type="text" value={form.tempId1} onChange={(e) => onChange('tempId1', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-tempId2">Temp ID 2</label>
              <input id="fm-tempId2" className={styles.formInput} type="text" value={form.tempId2} onChange={(e) => onChange('tempId2', e.target.value)} />
            </div>
          </div>
          <div className={styles.modalGrid2}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-tempId3">Temp ID 3</label>
              <input id="fm-tempId3" className={styles.formInput} type="text" value={form.tempId3} onChange={(e) => onChange('tempId3', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-tempId4">Temp ID 4</label>
              <input id="fm-tempId4" className={styles.formInput} type="text" value={form.tempId4} onChange={(e) => onChange('tempId4', e.target.value)} />
            </div>
          </div>

          {/* Configuration */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Configuration
          </h3>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-mappingLevel">Mapping Level</label>
              <input id="fm-mappingLevel" className={styles.formInput} type="text" value={form.mappingLevel} onChange={(e) => onChange('mappingLevel', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-transactionDay">Transaction Day</label>
              <input id="fm-transactionDay" className={styles.formInput} type="text" value={form.transactionDay} onChange={(e) => onChange('transactionDay', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-instCode">Inst Code</label>
              <input id="fm-instCode" className={styles.formInput} type="text" value={form.instCode} onChange={(e) => onChange('instCode', e.target.value)} />
            </div>
          </div>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Approval Required</label>
              <YNToggle field="approvalRequired" value={form.approvalRequired} onChange={onChange} ariaLabel="Approval Required" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>EJ Error Check</label>
              <YNToggle field="ejErrorCheck" value={form.ejErrorCheck} onChange={onChange} ariaLabel="EJ Error Check" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Diff Flag</label>
              <YNToggle field="diffFlag" value={form.diffFlag} onChange={onChange} ariaLabel="Diff Flag" />
            </div>
          </div>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="fm-diffExpr">Diff Amount Expression</label>
            <input id="fm-diffExpr" className={styles.formInput} type="text" value={form.diffAmountExpression} onChange={(e) => onChange('diffAmountExpression', e.target.value)} />
          </div>

          {/* Extended Fields */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Extended Fields
          </h3>
          <div className={styles.modalGrid3}>
            {[1, 2, 3].map((n) => (
              <div key={n} className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor={`fm-field${n}`}>Field {n}</label>
                <input id={`fm-field${n}`} className={styles.formInput} type="text" value={form[`field${n}`]} onChange={(e) => onChange(`field${n}`, e.target.value)} />
              </div>
            ))}
          </div>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="fm-field4">Field 4</label>
            <input id="fm-field4" className={styles.formInput} type="text" value={form.field4} onChange={(e) => onChange('field4', e.target.value)} />
          </div>

          {/* Status & Notes */}
          <h3 className={styles.modalSectionTitle}>
            <span className={styles.sectionDot} />
            Status & Notes
          </h3>
          <div className={styles.modalGrid3}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Config Status</label>
              <YNToggle field="configStatus" value={form.configStatus} onChange={onChange} ariaLabel="Config Status" />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-insUser">Ins User</label>
              <input id="fm-insUser" className={styles.formInput} type="text" value={form.insUser} onChange={(e) => onChange('insUser', e.target.value)} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="fm-lupdUser">LUPD User</label>
              <input id="fm-lupdUser" className={styles.formInput} type="text" value={form.lupdUser} onChange={(e) => onChange('lupdUser', e.target.value)} />
            </div>
          </div>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="fm-description">
              Description
              <span className={styles.charCount}>{form.description?.length || 0} / 500</span>
            </label>
            <textarea
              id="fm-description"
              className={`${styles.formInput} ${styles.formTextarea}`}
              value={form.description}
              onChange={(e) => onChange('description', e.target.value)}
              rows={3}
              maxLength={500}
            />
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.cancelBtn} onClick={onClose}>Cancel</button>
          <Button variant="gold" onClick={onSubmit} disabled={submitting} leftIcon={submitting ? <Loader2 size={16} className={styles.spinner} /> : <Plus size={16} />}>
            {submitting ? 'Saving...' : (editing ? 'Update Config' : 'Create Config')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default ProcessConfigModal;
