import { forwardRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import styles from './Input.module.css';
import { cn } from '../../../utils';

const Input = forwardRef(({
  label,
  error,
  success,
  hint,
  type = 'text',
  leftIcon,
  rightIcon,
  fullWidth = true,
  className,
  containerClassName,
  ...props
}, ref) => {
  const [showPassword, setShowPassword] = useState(false);
  const isPassword = type === 'password';
  const inputType = isPassword ? (showPassword ? 'text' : 'password') : type;

  return (
    <div className={cn(styles.container, fullWidth && styles.fullWidth, containerClassName)}>
      {label && (
        <label className={styles.label}>
          {label}
          {props.required && <span className={styles.required}>*</span>}
        </label>
      )}
      <div className={cn(styles.inputWrapper, error && styles.hasError, !error && success && styles.hasSuccess)}>
        {leftIcon && <span className={styles.leftIcon}>{leftIcon}</span>}
        <input
          ref={ref}
          type={inputType}
          className={cn(
            styles.input,
            leftIcon && styles.hasLeftIcon,
            (rightIcon || isPassword) && styles.hasRightIcon,
            className
          )}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            className={styles.passwordToggle}
            onClick={() => setShowPassword(!showPassword)}
            tabIndex={-1}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        )}
        {rightIcon && !isPassword && (
          <span className={styles.rightIcon}>{rightIcon}</span>
        )}
      </div>
      {error && <span className={styles.error}>{error}</span>}
      {!error && success && <span className={styles.success}>{success}</span>}
      {hint && !error && !success && <span className={styles.hint}>{hint}</span>}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
