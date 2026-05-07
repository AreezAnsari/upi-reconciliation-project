import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Database, FileText, Plus, X, Info, CheckCircle } from 'lucide-react';
import { PageHeader } from '../../../components/common';
import s from '../_shared.module.css';
import styles from './DefineReconciliation.module.css';

const STEPS = [
  { num: 1, label: 'Define Sources' },
  { num: 2, label: 'Match Rules' },
  { num: 3, label: 'Filter Conditions' },
  { num: 4, label: 'Schedule' },
  { num: 5, label: 'Review & Save' },
];

const SOURCE_OPTS = ['CBS_LEDGER', 'NPCI_ADJ', 'UPI_SWITCH', 'IMPS_NETWORK', 'NACH_FILE', 'RTGS_REPORT'];
const FIELD_OPTS_A = ['RRN', 'Amount', 'TxnDate', 'UTRNo', 'BankRef', 'AccountNo'];
const FIELD_OPTS_B = ['ReferenceNo', 'TxnAmount', 'ValueDate', 'UTR', 'ExtRef', 'AcctNo'];
const OPERATOR_OPTS = ['Equals', 'Contains', 'Starts With', 'Greater Than', 'Less Than'];
const TOLERANCE_OPTS = ['Exact Match', '±1 day', '±2 days', '±0.01 amount', '±1% amount'];

const DAYS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

const DefineReconciliation = () => {
  const [step, setStep] = useState(1);
  const [sourceA, setSourceA] = useState('CBS_LEDGER');
  const [sourceB, setSourceB] = useState('NPCI_ADJ');
  const [reconName, setReconName] = useState('');
  const [reconCode] = useState('RECON-0042');
  const [matchRules, setMatchRules] = useState([
    { id: 1, fieldA: 'RRN', operator: 'Equals', fieldB: 'ReferenceNo', tolerance: 'Exact Match' },
    { id: 2, fieldA: 'Amount', operator: 'Equals', fieldB: 'TxnAmount', tolerance: '±0.01 amount' },
  ]);
  const [filters, setFilters] = useState([
    { id: 1, col: 'TxnDate', op: 'Greater Than', val: '2026-01-01', src: 'Source A', logic: 'AND' },
  ]);
  const [activeDays, setActiveDays] = useState([0, 1, 2, 3, 4]);
  const [runTime, setRunTime] = useState('02:00');
  const [frequency, setFrequency] = useState('Daily');
  const [saved, setSaved] = useState(false);

  const addRule = () => setMatchRules(p => [...p, { id: Date.now(), fieldA: 'RRN', operator: 'Equals', fieldB: 'ReferenceNo', tolerance: 'Exact Match' }]);
  const removeRule = (id) => setMatchRules(p => p.filter(r => r.id !== id));
  const updateRule = (id, key, val) => setMatchRules(p => p.map(r => r.id === id ? { ...r, [key]: val } : r));

  const addFilter = () => setFilters(p => [...p, { id: Date.now(), col: 'TxnDate', op: 'Equals', val: '', src: 'Source A', logic: 'AND' }]);
  const removeFilter = (id) => setFilters(p => p.filter(f => f.id !== id));
  const updateFilter = (id, key, val) => setFilters(p => p.map(f => f.id === id ? { ...f, [key]: val } : f));
  const toggleDay = (i) => setActiveDays(p => p.includes(i) ? p.filter(d => d !== i) : [...p, i]);

  const canNext = step < STEPS.length;
  const canPrev = step > 1;

  const handleSave = () => setSaved(true);

  if (saved) {
    return (
      <div className={s.page}>
        <PageHeader title="Define Reconciliation" description="Configure source matching, rules, filters and schedule" />
        <motion.div className={styles.successCard} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }}>
          <div className={styles.successIcon}><CheckCircle size={40} color="var(--color-success-500)" /></div>
          <h2 className={styles.successTitle}>Reconciliation Rule Saved</h2>
          <p className={styles.successSub}>Rule <strong>{reconCode}</strong> has been created and scheduled.</p>
          <div className={styles.successMeta}>
            <div><span>Source A:</span> {sourceA}</div>
            <div><span>Source B:</span> {sourceB}</div>
            <div><span>Match Rules:</span> {matchRules.length}</div>
            <div><span>Schedule:</span> {frequency} at {runTime}</div>
          </div>
          <div style={{ display: 'flex', gap: 'var(--space-3)', justifyContent: 'center', marginTop: 'var(--space-5)' }}>
            <button className={`${s.btn} ${s.btnOutline}`} onClick={() => setSaved(false)}>Create Another</button>
            <button className={`${s.btn} ${s.btnGold}`}>View All Rules</button>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className={s.page}>
      <PageHeader title="Define Reconciliation" description="Configure source matching, rules, filters and schedule">
        <button className={`${s.btn} ${s.btnOutline}`}>Cancel</button>
        <button className={`${s.btn} ${s.btnOutline}`}>Save as Draft</button>
        {step === STEPS.length && <button className={`${s.btn} ${s.btnGold} ${s.btnLg}`} onClick={handleSave}>Save Rule →</button>}
      </PageHeader>

      {/* Stepper */}
      <div className={styles.stepper}>
        {STEPS.map((st, i) => (
          <div key={st.num} className={`${styles.step} ${step > st.num ? styles.stepDone : ''} ${step === st.num ? styles.stepActive : ''}`}>
            <div className={styles.stepDot}>{step > st.num ? '✓' : st.num}</div>
            <span className={styles.stepLabel}>{st.label}</span>
            {i < STEPS.length - 1 && <div className={styles.stepLine} />}
          </div>
        ))}
      </div>

      <AnimatePresence mode="wait">
        <motion.div key={step} initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -16 }} transition={{ duration: 0.25 }}>

          {/* Step 1: Sources */}
          {step === 1 && (
            <div className={s.card}>
              <div className={s.cardHead}><span className={s.cardHeadNum}>1</span><span className={s.cardTitle}>Define Sources</span><span className={s.cardSub}>Select two data sources to reconcile against each other</span></div>
              <div className={s.cardBody}>
                <div className={s.g2} style={{ marginBottom: 'var(--space-5)' }}>
                  <div className={s.fld}>
                    <label>Reconciliation Name <span className={s.req}>*</span></label>
                    <input className={s.fldInput} placeholder="e.g. UPI vs NPCI Daily" value={reconName} onChange={e => setReconName(e.target.value)} />
                  </div>
                  <div className={s.fld}>
                    <label>Recon Code</label>
                    <input className={`${s.fldInput} ${s.fldRo} ${s.mono}`} value={reconCode} readOnly />
                    <div className={s.hint}>Auto-generated</div>
                  </div>
                </div>
                <div className={styles.matchVisual}>
                  <div className={styles.mvSource}>
                    <div className={`${styles.srcCard} ${sourceA ? styles.srcCardFilled : ''}`}>
                      <span className={styles.srcLabel}>Source A</span>
                      <div className={styles.srcIcon} style={{ background: 'var(--color-primary-50)' }}><Database size={22} color="var(--color-primary-500)" /></div>
                      <div className={s.fld}>
                        <label>Data Source</label>
                        <select className={s.fldSelect} value={sourceA} onChange={e => setSourceA(e.target.value)}>
                          {SOURCE_OPTS.map(o => <option key={o}>{o}</option>)}
                        </select>
                      </div>
                      {sourceA && <div className={styles.srcCheck}><CheckCircle size={12} color="var(--color-success-500)" /></div>}
                    </div>
                  </div>
                  <div className={styles.mvCenter}>
                    <div className={styles.mvLine} />
                    <div className={styles.mvBadge}>Match on<br/>selected fields</div>
                  </div>
                  <div className={styles.mvSource}>
                    <div className={`${styles.srcCard} ${sourceB ? styles.srcCardFilled : ''}`}>
                      <span className={styles.srcLabel}>Source B</span>
                      <div className={styles.srcIcon} style={{ background: 'var(--color-accent-50)' }}><FileText size={22} color="var(--color-accent-500)" /></div>
                      <div className={s.fld}>
                        <label>Data Source</label>
                        <select className={s.fldSelect} value={sourceB} onChange={e => setSourceB(e.target.value)}>
                          {SOURCE_OPTS.map(o => <option key={o}>{o}</option>)}
                        </select>
                      </div>
                      {sourceB && <div className={styles.srcCheck}><CheckCircle size={12} color="var(--color-success-500)" /></div>}
                    </div>
                  </div>
                </div>
                <div className={`${s.infoBox} ${s.infoBlue}`} style={{ marginTop: 'var(--space-4)' }}>
                  <Info size={14} /><span>Both sources must have overlapping fields for matching to work. You will configure field mappings in the next step.</span>
                </div>
              </div>
            </div>
          )}

          {/* Step 2: Match Rules */}
          {step === 2 && (
            <div className={s.card}>
              <div className={s.cardHead}><span className={s.cardHeadNum}>2</span><span className={s.cardTitle}>Matching Rules</span><span className={s.cardSub}>Define how fields from Source A map to Source B</span></div>
              <div className={s.cardBody}>
                <div className={styles.ruleHeader}>
                  <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)', fontWeight: 'var(--font-semibold)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                    Source A ({sourceA}) &nbsp;↔&nbsp; Source B ({sourceB})
                  </span>
                  <button className={`${s.btn} ${s.btnSm} ${s.btnTeal}`} onClick={addRule}><Plus size={11} /> Add Rule</button>
                </div>
                {matchRules.map((r, idx) => (
                  <div key={r.id} className={styles.ruleRow}>
                    <div className={styles.ruleNum}>{idx + 1}</div>
                    <div className={styles.ruleBody}>
                      <select className={styles.ruleSelect} value={r.fieldA} onChange={e => updateRule(r.id, 'fieldA', e.target.value)}>
                        {FIELD_OPTS_A.map(f => <option key={f}>{f}</option>)}
                      </select>
                      <select className={styles.ruleSelect} value={r.operator} onChange={e => updateRule(r.id, 'operator', e.target.value)}>
                        {OPERATOR_OPTS.map(o => <option key={o}>{o}</option>)}
                      </select>
                      <select className={styles.ruleSelect} value={r.fieldB} onChange={e => updateRule(r.id, 'fieldB', e.target.value)}>
                        {FIELD_OPTS_B.map(f => <option key={f}>{f}</option>)}
                      </select>
                      <select className={styles.ruleSelect} value={r.tolerance} onChange={e => updateRule(r.id, 'tolerance', e.target.value)}>
                        {TOLERANCE_OPTS.map(t => <option key={t}>{t}</option>)}
                      </select>
                    </div>
                    <button className={styles.ruleDel} onClick={() => removeRule(r.id)}><X size={11} /></button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Step 3: Filters */}
          {step === 3 && (
            <div className={s.card}>
              <div className={s.cardHead}><span className={s.cardHeadNum}>3</span><span className={s.cardTitle}>Filter Conditions</span><span className={s.cardSub}>Narrow down records before matching is applied</span></div>
              <div className={s.cardBody}>
                <div className={styles.ruleHeader}>
                  <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)' }}>Filters are applied before matching. AND / OR logic per row.</span>
                  <button className={`${s.btn} ${s.btnSm} ${s.btnTeal}`} onClick={addFilter}><Plus size={11} /> Add Filter</button>
                </div>
                {filters.map((f, idx) => (
                  <div key={f.id} className={styles.filterRow}>
                    {idx > 0 && (
                      <div className={styles.logicChip}>
                        {['AND','OR'].map(l => (
                          <button key={l} className={f.logic === l ? styles.logicOn : ''} onClick={() => updateFilter(f.id, 'logic', l)}>{l}</button>
                        ))}
                      </div>
                    )}
                    <select className={`${styles.ruleSelect} ${styles.fCol}`} value={f.col} onChange={e => updateFilter(f.id, 'col', e.target.value)}>
                      {FIELD_OPTS_A.map(c => <option key={c}>{c}</option>)}
                    </select>
                    <select className={`${styles.ruleSelect} ${styles.fOp}`} value={f.op} onChange={e => updateFilter(f.id, 'op', e.target.value)}>
                      {OPERATOR_OPTS.map(o => <option key={o}>{o}</option>)}
                    </select>
                    <input className={`${styles.ruleSelect} ${styles.fVal}`} value={f.val} placeholder="Value…" onChange={e => updateFilter(f.id, 'val', e.target.value)} />
                    <select className={`${styles.ruleSelect} ${styles.fSrc}`} value={f.src} onChange={e => updateFilter(f.id, 'src', e.target.value)}>
                      <option>Source A</option><option>Source B</option><option>Both</option>
                    </select>
                    <button className={styles.ruleDel} onClick={() => removeFilter(f.id)}><X size={11} /></button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Step 4: Schedule */}
          {step === 4 && (
            <div className={s.card}>
              <div className={s.cardHead}><span className={s.cardHeadNum}>4</span><span className={s.cardTitle}>Schedule</span><span className={s.cardSub}>Configure when this reconciliation runs automatically</span></div>
              <div className={s.cardBody}>
                <div className={s.g3} style={{ marginBottom: 'var(--space-5)' }}>
                  <div className={s.fld}>
                    <label>Frequency</label>
                    <select className={s.fldSelect} value={frequency} onChange={e => setFrequency(e.target.value)}>
                      <option>Daily</option><option>Weekly</option><option>Monthly</option><option>Hourly</option>
                    </select>
                  </div>
                  <div className={s.fld}>
                    <label>Run Time</label>
                    <input className={s.fldInput} type="time" value={runTime} onChange={e => setRunTime(e.target.value)} />
                  </div>
                  <div className={s.fld}>
                    <label>Timezone</label>
                    <select className={s.fldSelect}><option>IST (UTC+5:30)</option><option>UTC</option></select>
                  </div>
                </div>
                <div className={s.fld}>
                  <label>Active Days</label>
                  <div className={styles.schedDays}>
                    {DAYS.map((d, i) => (
                      <button key={i} className={`${styles.sd} ${activeDays.includes(i) ? styles.sdOn : ''}`} onClick={() => toggleDay(i)}>{d}</button>
                    ))}
                  </div>
                </div>
                <div className={`${s.infoBox} ${s.infoBlue}`} style={{ marginTop: 'var(--space-4)' }}>
                  <Info size={14} /><span>The reconciliation will run automatically at <strong>{runTime} IST</strong> on selected days. Results are available in the Report module.</span>
                </div>
              </div>
            </div>
          )}

          {/* Step 5: Review */}
          {step === 5 && (
            <div className={s.card}>
              <div className={s.cardHead}><span className={s.cardHeadNum}>5</span><span className={s.cardTitle}>Review &amp; Save</span><span className={s.cardSub}>Confirm your configuration before saving</span></div>
              <div className={s.cardBody}>
                <div className={styles.summaryGrid}>
                  {[['Sources', `${sourceA} ↔ ${sourceB}`],['Match Rules', `${matchRules.length} rules defined`],['Filters', `${filters.length} conditions`],['Schedule', `${frequency} at ${runTime} IST`]].map(([label, val]) => (
                    <div key={label} className={styles.summaryCard}>
                      <div className={styles.sv}>{val.split(' ')[0]}</div>
                      <div className={styles.sl}>{label}</div>
                      <div style={{ fontSize: 'var(--text-xs)', color: 'var(--color-neutral-500)', marginTop: 4 }}>{val}</div>
                    </div>
                  ))}
                </div>
                <div className={`${s.infoBox} ${s.infoGreen}`} style={{ marginBottom: 'var(--space-4)' }}>
                  <CheckCircle size={14} /><span>Configuration looks complete. Click <strong>Save Rule</strong> to activate this reconciliation job.</span>
                </div>
                <div className={styles.jsonBox}>
                  <code>{JSON.stringify({ name: reconName || 'Untitled', code: reconCode, sourceA, sourceB, matchRules: matchRules.length, filters: filters.length, schedule: { frequency, time: runTime, days: activeDays } }, null, 2)}</code>
                </div>
              </div>
            </div>
          )}

        </motion.div>
      </AnimatePresence>

      {/* Navigation */}
      <div className={styles.navActions}>
        <button className={`${s.btn} ${s.btnOutline}`} disabled={!canPrev} onClick={() => setStep(s => s - 1)} style={{ opacity: canPrev ? 1 : 0.4 }}>← Previous</button>
        {canNext && <button className={`${s.btn} ${s.btnGold} ${s.btnLg}`} onClick={() => setStep(s => s + 1)}>Next Step →</button>}
        {!canNext && <button className={`${s.btn} ${s.btnGold} ${s.btnLg}`} onClick={handleSave}>Save Rule →</button>}
      </div>
    </div>
  );
};

export default DefineReconciliation;
