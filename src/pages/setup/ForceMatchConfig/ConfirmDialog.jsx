import { useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle } from 'lucide-react';
import { Button } from '../../../components/common';
import styles from './ForceMatchConfig.module.css';

const ConfirmDialog = ({ open, title, message, confirmLabel = 'Delete', onConfirm, onCancel }) => {
  const confirmRef = useRef(null);
  const dialogRef = useRef(null);

  // Focus trap + Escape key
  useEffect(() => {
    if (!open) return;
    confirmRef.current?.focus();
    const handleKey = (e) => {
      if (e.key === 'Escape') onCancel();
    };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
    };
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <motion.div
      className={styles.modalOverlay}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
    >
      <motion.div
        className={styles.confirmDialog}
        ref={dialogRef}
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.confirmIcon}>
          <AlertTriangle size={24} />
        </div>
        <h3 id="confirm-title" className={styles.confirmTitle}>{title}</h3>
        <p className={styles.confirmMessage}>{message}</p>
        <div className={styles.confirmActions}>
          <button className={styles.cancelBtn} onClick={onCancel}>
            Cancel
          </button>
          <Button variant="danger" size="sm" ref={confirmRef} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default ConfirmDialog;
