import { motion } from 'framer-motion';
import { Check } from 'lucide-react';
import styles from './Stepper.module.css';

const Stepper = ({ steps, currentStep, onStepClick }) => {
  return (
    <div className={styles.stepper}>
      {steps.map((step, index) => {
        const isActive = currentStep === step.id;
        const isCompleted = steps.findIndex(s => s.id === currentStep) > index;
        
        return (
          <div key={step.id} className={styles.stepWrapper}>
            {/* Connector Line */}
            {index > 0 && (
              <div className={styles.connectorWrapper}>
                <div className={`${styles.connector} ${isCompleted || isActive ? styles.connectorActive : ''}`} />
              </div>
            )}
            
            {/* Step Item */}
            <button
              type="button"
              className={`${styles.step} ${isActive ? styles.stepActive : ''} ${isCompleted ? styles.stepCompleted : ''}`}
              onClick={() => onStepClick(step.id)}
            >
              <motion.div 
                className={styles.stepCircle}
                initial={false}
                animate={{
                  scale: isActive ? 1.1 : 1,
                  backgroundColor: isActive ? 'var(--color-gold-500)' : isCompleted ? 'var(--color-teal-500)' : 'var(--color-neutral-200)',
                }}
                transition={{ duration: 0.2 }}
              >
                {isCompleted ? (
                  <Check size={16} className={styles.checkIcon} />
                ) : (
                  <span className={styles.stepNumber}>{index + 1}</span>
                )}
              </motion.div>
              
              <div className={styles.stepContent}>
                <span className={styles.stepLabel}>{step.label}</span>
                {step.description && (
                  <span className={styles.stepDescription}>{step.description}</span>
                )}
              </div>
            </button>
          </div>
        );
      })}
    </div>
  );
};

export default Stepper;