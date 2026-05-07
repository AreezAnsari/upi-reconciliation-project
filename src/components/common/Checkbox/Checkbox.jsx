import { forwardRef } from 'react';
import { Check } from 'lucide-react';
import styles from './Checkbox.module.css';
import { cn } from '../../../utils';

const Checkbox = forwardRef(({
  label,
  checked,
  onChange,
  className,
  ...props
}, ref) => {
  return (
    <label className={cn(styles.container, className)}>
      <input
        ref={ref}
        type="checkbox"
        checked={checked}
        onChange={onChange}
        className={styles.input}
        {...props}
      />
      <span className={cn(styles.checkbox, checked && styles.checked)}>
        {checked && <Check size={14} strokeWidth={3} />}
      </span>
      {label && <span className={styles.label}>{label}</span>}
    </label>
  );
});

Checkbox.displayName = 'Checkbox';

export default Checkbox;
