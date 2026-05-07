import { Outlet } from 'react-router-dom';
import { motion } from 'framer-motion';
import Sidebar from '../Sidebar';
import { Toast } from '../../common';
import styles from './MainLayout.module.css';

const MainLayout = () => {
  return (
    <div className={styles.layout}>
      <Sidebar />
      <div className={styles.body}>
        <main className={styles.main}>
          <motion.div
            className={styles.content}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Outlet />
          </motion.div>
        </main>
        <footer className={styles.footer}>
          <p>&copy; {new Date().getFullYear()} Kal Infotech. All rights reserved.</p>
        </footer>
      </div>
      <Toast />
    </div>
  );
};

export default MainLayout;
