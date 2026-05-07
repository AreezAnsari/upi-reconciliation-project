import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { User, Lock, ArrowRight } from 'lucide-react';
import { Button, Input, Checkbox } from '../../components/common';
import { Logo } from '../../components/layout';
import { useAuthStore } from '../../store';
import { DUMMY_USERS } from '../../services';
import { APP_CONFIG } from '../../config';
import styles from './Login.module.css';
import { authAPI } from '../../services';


const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    rememberMe: false,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');



  // const handleSubmit = async (e) => {
  //   e.preventDefault();
  //   setError('');
  //   setLoading(true);

  //   try {
  //     const response = await authAPI.login({
  //       emailId: formData.username.includes('@') ? formData.username : `${formData.username}@gmail.com`,
  //       userName: formData.username,
  //       userPassword: formData.password,
  //     });

  //     // Adjust based on your API response structure
  //     login(
  //       { username: formData.username, name: formData.username },
  //       response.accessToken,
  //       response.refreshToken
  //     );
  //     navigate('/dashboard');
  //   } catch (err) {
  //     setError('Invalid username or password');
  //   } finally {
  //     setLoading(false);
  //   }
  // };

  const handleSubmit = async (e) => {
  e.preventDefault();
  setError('');
  setLoading(true);

  try {
    const response = await authAPI.login({
      emailId: formData.username.includes('@') ? formData.username : `${formData.username}@gmail.com`,
      userName: formData.username,
      userPassword: formData.password,
    });

    // Fetch user details
    const userResponse = await authAPI.getUser(response.accessToken);
    const userData = userResponse.data[0];

    // Fetch user role
    const roleResponse = await authAPI.getRoleLoginUser(userData.role.roleId, response.accessToken);

    login(
      {
        userId: userData.userId,
        username: userData.userName,
        name: userData.userName,
        email: userData.emailId,
        role: userData.role.roleName,
        roleId: userData.role.roleId,
        roleCode: roleResponse?.data?.[0]?.roleCode || '',
        designation: userData.designation,
        institution: userData.institution,
        mobileNumber: userData.mobileNumber,
        roleDetails: roleResponse,
      },
      response.accessToken,
      response.refreshToken
    );
    navigate('/dashboard');
  } catch (err) {
    setError('Invalid username or password');
  } finally {
    setLoading(false);
  }
};

  const handleGoogleSignIn = () => {
    // Simulate Google sign-in
    const user = DUMMY_USERS[0];
    login(user);
    navigate('/dashboard');
  };

  return (
    <div className={styles.page}>
      <div className={styles.left}>
        <div className={styles.leftContent}>
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5 }}
          >
            <Logo size="lg" />
          </motion.div>

          <motion.div
            className={styles.hero}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <h1 className={styles.title}>
              Next-Generation Reconciliation Platform <br />
              <span className={styles.highlight}>powered by AI</span>
            </h1>
            <p className={styles.subtitle}>
              Simplify. Automate. Trust.
            </p>
            <p className={styles.description}>
              Take the first step toward a smarter reconciliation process.
              Let Kal Infotech transform how you manage data consistency and accuracy.
            </p>
          </motion.div>

          <div className={styles.features}>
            <motion.div
              className={styles.feature}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.4 }}
            >
              <div className={styles.featureIcon}>✓</div>
              <span>99.9% Accuracy Rate</span>
            </motion.div>
            <motion.div
              className={styles.feature}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.5 }}
            >
              <div className={styles.featureIcon}>⚡</div>
              <span>Real-time Processing</span>
            </motion.div>
            <motion.div
              className={styles.feature}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.6 }}
            >
              <div className={styles.featureIcon}>🔒</div>
              <span>Bank-grade Security</span>
            </motion.div>
          </div>
        </div>

        <div className={styles.decoration}>
          <div className={styles.circle1} />
          <div className={styles.circle2} />
          <div className={styles.circle3} />
        </div>
      </div>

      <div className={styles.right}>
        <motion.div
          className={styles.formContainer}
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
        >
          <div className={styles.formHeader}>
            <h2>Login</h2>
            <p>Welcome, please log in to your account.</p>
          </div>

          <form onSubmit={handleSubmit} className={styles.form}>
            {error && (
              <motion.div
                className={styles.error}
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
              >
                {error}
              </motion.div>
            )}

            <Input
              label="Username"
              placeholder="Enter your username"
              leftIcon={<User size={18} />}
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              required
            />

            <Input
              label="Password"
              type="password"
              placeholder="Enter your password"
              leftIcon={<Lock size={18} />}
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
            />

            <div className={styles.options}>
              <Checkbox
                label="Remember me"
                checked={formData.rememberMe}
                onChange={(e) => setFormData({ ...formData, rememberMe: e.target.checked })}
              />
              <a href="#" className={styles.forgotPassword}>Forgot password</a>
            </div>

            <Button
              type="submit"
              variant="gold"
              fullWidth
              loading={loading}
              rightIcon={<ArrowRight size={18} />}
            >
              Log in
            </Button>
          </form>

          <div className={styles.divider}>
            <span>or</span>
          </div>

          <Button
            variant="outline"
            fullWidth
            onClick={handleGoogleSignIn}
            leftIcon={
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
            }
          >
            Sign in with Google
          </Button>

          <p className={styles.hint}>
            Demo: Use <strong>admin</strong> / <strong>password</strong>
          </p>
        </motion.div>
      </div>
    </div>
  );
};

export default Login;
