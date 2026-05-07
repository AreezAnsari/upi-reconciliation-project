import { forwardRef } from 'react';
import { ChevronDown } from 'lucide-react';
import styles from './Select.module.css';
import { cn } from '../../../utils';

const Select = forwardRef(({
  label,
  error,
  hint,
  options = [],
  placeholder = 'Select an option',
  fullWidth = true,
  className,
  containerClassName,
  ...props
}, ref) => {
  return (
    <div className={cn(styles.container, fullWidth && styles.fullWidth, containerClassName)}>
      {label && (
        <label className={styles.label}>
          {label}
          {props.required && <span className={styles.required}>*</span>}
        </label>
      )}
      <div className={cn(styles.selectWrapper, error && styles.hasError)}>
        <select
          ref={ref}
          className={cn(styles.select, !props.value && styles.placeholder, className)}
          {...props}
        >
          <option value="" disabled>
            {placeholder}
          </option>
          {options.map((option) => (
            <option key={option.id || option.value} value={option.id || option.value}>
              {option.label || option.name}
            </option>
          ))}
        </select>
        <ChevronDown className={styles.chevron} size={18} />
      </div>
      {error && <span className={styles.error}>{error}</span>}
      {hint && !error && <span className={styles.hint}>{hint}</span>}
    </div>
  );
});

Select.displayName = 'Select';

export default Select;
