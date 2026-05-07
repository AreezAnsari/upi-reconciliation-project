import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertCircle, Info, X } from 'lucide-react';
import { useAppStore } from '../../../store';
import styles from './Toast.module.css';

const iconMap = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertCircle,
  info: Info,
};

const ToastItem = ({ notification, onClose }) => {
  const Icon = iconMap[notification.type] || Info;

  return (
    <motion.div
      className={`${styles.toast} ${styles[notification.type]}`}
      initial={{ opacity: 0, x: 100, scale: 0.9 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={{ opacity: 0, x: 100, scale: 0.9 }}
      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
      layout
    >
      <div className={styles.iconWrapper}>
        <Icon size={20} />
      </div>
      <div className={styles.content}>
        {notification.title && (
          <div className={styles.title}>{notification.title}</div>
        )}
        {notification.message && (
          <div className={styles.message}>{notification.message}</div>
        )}
      </div>
      <button
        className={styles.closeButton}
        onClick={() => onClose(notification.id)}
        aria-label="Close notification"
      >
        <X size={16} />
      </button>
    </motion.div>
  );
};

const Toast = () => {
  const { notifications, removeNotification } = useAppStore();

  return (
    <div className={styles.container}>
      <AnimatePresence mode="popLayout">
        {notifications.map((notification) => (
          <ToastItem
            key={notification.id}
            notification={notification}
            onClose={removeNotification}
          />
        ))}
      </AnimatePresence>
    </div>
  );
};

export default Toast;
