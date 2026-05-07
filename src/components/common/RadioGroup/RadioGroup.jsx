import { forwardRef } from 'react';
import styles from './RadioGroup.module.css';
import { cn } from '../../../utils';

const RadioGroup = forwardRef(({
  label,
  name,
  options = [],
  value,
  onChange,
  direction = 'horizontal',
  className,
  ...props
}, ref) => {
  return (
    <div className={cn(styles.container, className)}>
      {label && <span className={styles.groupLabel}>{label}</span>}
      <div className={cn(styles.options, styles[direction])}>
        {options.map((option) => (
          <label key={option.id || option.value} className={styles.option}>
            <input
              ref={ref}
              type="radio"
              name={name}
              value={option.id || option.value}
              checked={value === (option.id || option.value)}
              onChange={(e) => onChange?.(e.target.value)}
              className={styles.input}
              {...props}
            />
            <span className={cn(styles.radio, value === (option.id || option.value) && styles.checked)}>
              <span className={styles.dot} />
            </span>
            <span className={styles.label}>{option.label || option.name}</span>
          </label>
        ))}
      </div>
    </div>
  );
});

RadioGroup.displayName = 'RadioGroup';

export default RadioGroup;
