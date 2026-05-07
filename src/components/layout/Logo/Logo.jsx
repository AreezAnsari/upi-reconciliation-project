import { motion } from 'framer-motion';
import styles from './Logo.module.css';
import { cn } from '../../../utils';

const Logo = ({ size = 'md', showText = true, className }) => {
  return (
    <div className={cn(styles.logo, styles[size], className)}>
      <motion.div 
        className={styles.icon}
        whileHover={{ rotate: 180 }}
        transition={{ duration: 0.5 }}
      >
        <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="40" height="40" rx="10" fill="url(#logo-gradient)" />
          <path
            d="M12 20C12 15.5817 15.5817 12 20 12V12C24.4183 12 28 15.5817 28 20V20C28 24.4183 24.4183 28 20 28V28"
            stroke="white"
            strokeWidth="3"
            strokeLinecap="round"
          />
          <path
            d="M20 16L24 20L20 24"
            stroke="white"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="16" cy="20" r="2" fill="white" />
          <defs>
            <linearGradient id="logo-gradient" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
              <stop stopColor="#17a398" />
              <stop offset="1" stopColor="#0d6963" />
            </linearGradient>
          </defs>
        </svg>
      </motion.div>
      {showText && (
        <div className={styles.text}>
          <span className={styles.name}>Kal<span className={styles.highlight}> Infotech</span></span>
        </div>
      )}
    </div>
  );
};

export default Logo;
