import { motion } from 'framer-motion';
import styles from './PageHeader.module.css';

const PageHeader = ({ title, description, children }) => {
  return (
    <motion.div 
      className={styles.header}
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className={styles.titleSection}>
        <h1 className={styles.title}>{title}</h1>
        {description && <p className={styles.description}>{description}</p>}
      </div>
      {children && <div className={styles.actions}>{children}</div>}
    </motion.div>
  );
};

export default PageHeader;
