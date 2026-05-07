import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, Plus, Trash2, Info, AlertTriangle, Shield, Copy, FileText } from 'lucide-react';
import { Button, Card, Input, Select, Stepper } from '../../../components/common';
import styles from './ReconConfig.module.css';

const STEPS = [
  { id: 'what', label: 'What to Reconcile' },
  { id: 'sources', label: 'Pick Source Files' },
  { id: 'match', label: 'How to Match' },
  { id: 'filter', label: 'Filter Data' },
  { id: 'schedule', label: 'Schedule & Run' },
  { id: 'review', label: 'Review' },
];

const RECON_TYPE_OPTIONS = [
  { value: '2WAY', label: '2-Way (File A \u2194 File B)' },
  { value: '3WAY', label: '3-Way (A \u2194 B \u2194 C)' },
];

const CHANNEL_OPTIONS = [
  { value: 'UPI', label: 'UPI' },
  { value: 'AEPS', label: 'AEPS' },
  { value: 'IMPS', label: 'IMPS' },
  { value: 'NEFT', label: 'NEFT' },
  { value: 'RTGS', label: 'RTGS' },
];

const FREQUENCY_OPTIONS = [
  { value: 'Daily', label: 'Daily' },
  { value: 'Intraday', label: 'Intraday' },
  { value: 'Cycle-based', label: 'Cycle-based' },
  { value: 'Weekly', label: 'Weekly' },
  { value: 'On-demand', label: 'On-demand' },
];

const DATE_LOGIC_OPTIONS = [
  { value: 'T0', label: 'Same Day (T+0)' },
  { value: 'T1', label: 'Next Day (T+1)' },
  { value: 'T2', label: 'T+2' },
];

const MATCH_TYPE_OPTIONS = [
  { value: '1:1', label: 'One-to-One' },
  { value: '1:N', label: 'One-to-Many' },
  { value: 'N:N', label: 'Many-to-Many' },
];

const FILE_TYPE_OPTIONS = [
  { value: 'SWITCH', label: 'Switch File' },
  { value: 'CBS', label: 'CBS File' },
  { value: 'NPCI', label: 'NPCI Settlement' },
  { value: 'BANK', label: 'Bank Statement' },
  { value: 'MERCHANT', label: 'Merchant File' },
];

const MATCH_MODE_OPTIONS = [
  { value: 'EXACT', label: 'EXACT' },
  { value: 'TOLERANCE', label: 'TOLERANCE' },
  { value: 'CONDITIONAL', label: 'CONDITIONAL' },
  { value: 'CONTAINS', label: 'CONTAINS' },
];

const OPERATOR_OPTIONS = [
  { value: '=', label: '=' },
  { value: '!=', label: '!=' },
  { value: '>', label: '>' },
  { value: '<', label: '<' },
  { value: '>=', label: '>=' },
  { value: '<=', label: '<=' },
  { value: 'IN', label: 'IN' },
  { value: 'NOT IN', label: 'NOT IN' },
];

const APPLY_TO_OPTIONS = [
  { value: 'BOTH', label: 'Both' },
  { value: 'Source A', label: 'Source A' },
  { value: 'Source B', label: 'Source B' },
];

const RERUN_OPTIONS = [
  { value: 'INCREMENTAL', label: 'Incremental (new records only)' },
  { value: 'FULL', label: 'Full re-run' },
  { value: 'DELTA', label: 'Delta (changed records)' },
];

const DUP_OPTIONS = [
  { value: 'SKIP', label: 'Skip duplicates' },
  { value: 'OVERWRITE', label: 'Overwrite previous' },
  { value: 'FLAG', label: 'Flag for review' },
];

const TEMPLATES = {
  SWITCH: ['TMPL-0001 \u2014 AEPS Switch Daily', 'TMPL-0005 \u2014 UPI Switch Intraday', 'TMPL-0012 \u2014 IMPS Switch File'],
  CBS: ['TMPL-0002 \u2014 CBS AEPS Posting', 'TMPL-0006 \u2014 CBS UPI Settlement', 'TMPL-0009 \u2014 CBS NEFT Posting'],
  NPCI: ['TMPL-0003 \u2014 NPCI AEPS Settlement', 'TMPL-0007 \u2014 NPCI UPI Settlement'],
  BANK: ['TMPL-0004 \u2014 Bank Statement Daily', 'TMPL-0010 \u2014 Bank Reconciliation'],
  MERCHANT: ['TMPL-0008 \u2014 Merchant Summary', 'TMPL-0011 \u2014 Merchant Payout File'],
};

const STAGING_TABLES = { SWITCH: 'STG_SWITCH_TXN', CBS: 'STG_CBS_TXN', NPCI: 'STG_NPCI_SETTLEMENT', BANK: 'STG_BANK_STMT', MERCHANT: 'STG_MERCHANT_TXN' };

const SOURCE_FIELDS = {
  SWITCH: ['RRN', 'UTR_NO', 'TXN_DATE', 'TXN_AMOUNT', 'STATUS', 'DR_CR_IND', 'CHANNEL', 'CYCLE_ID', 'PAYEE_NAME', 'RESP_CODE', 'REC_FLG'],
  CBS: ['RRN', 'UTR_NO', 'VALUE_DATE', 'AMOUNT', 'CB_STATUS', 'ACCOUNT_NO', 'BRANCH_CODE', 'NARRATION', 'REC_FLG'],
  NPCI: ['RRN', 'UTR_NO', 'TXN_DATE', 'TXN_AMOUNT', 'SETTLEMENT_AMOUNT', 'NPCI_STATUS', 'CYCLE_ID', 'MCC', 'REC_FLG'],
  BANK: ['REF_NO', 'TXN_DATE', 'DEBIT', 'CREDIT', 'BALANCE', 'NARRATION', 'REC_FLG'],
  MERCHANT: ['MERCHANT_ID', 'TXN_DATE', 'TXN_AMOUNT', 'FEE', 'NET_AMOUNT', 'STATUS', 'REC_FLG'],
};

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const INITIAL_FORM = {
  reconName: '',
  reconCode: 'RCN-00042',
  reconType: '',
  channel: '',
  frequency: 'Daily',
  dateLogic: 'T0',
  matchType: '1:1',
  description: '',
  srcAFileType: '',
  srcATemplate: '',
  srcBFileType: '',
  srcBTemplate: '',
  srcCFileType: '',
  srcCTemplate: '',
  autoRun: true,
  cronExpr: '0 6 * * *',
  rerunMode: 'INCREMENTAL',
  dupHandling: 'SKIP',
  activeDays: [true, true, true, true, true, false, false],
};

const ReconConfig = () => {
  const [currentStep, setCurrentStep] = useState('what');
  const [formData, setFormData] = useState({ ...INITIAL_FORM });
  const [rules, setRules] = useState([]);
  const [filters, setFilters] = useState([]);
  const [filterLogic, setFilterLogic] = useState('AND');

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const srcAFields = SOURCE_FIELDS[formData.srcAFileType] || [];
  const srcBFields = SOURCE_FIELDS[formData.srcBFileType] || [];

  const srcATemplateOptions = useMemo(() =>
    (TEMPLATES[formData.srcAFileType] || []).map((t) => ({ value: t.split(' \u2014 ')[0], label: t })),
    [formData.srcAFileType]
  );
  const srcBTemplateOptions = useMemo(() =>
    (TEMPLATES[formData.srcBFileType] || []).map((t) => ({ value: t.split(' \u2014 ')[0], label: t })),
    [formData.srcBFileType]
  );
  const srcCTemplateOptions = useMemo(() =>
    (TEMPLATES[formData.srcCFileType] || []).map((t) => ({ value: t.split(' \u2014 ')[0], label: t })),
    [formData.srcCFileType]
  );

  // Rules
  const addRule = () => {
    setRules((prev) => [...prev, { srcAField: '', matchMode: 'EXACT', srcBField: '', tolerance: '0', required: true }]);
  };
  const updateRule = (idx, field, value) => {
    setRules((prev) => prev.map((r, i) => i === idx ? { ...r, [field]: value } : r));
  };
  const removeRule = (idx) => {
    setRules((prev) => prev.filter((_, i) => i !== idx));
  };
  const addPresetRule = (srcA, mode, srcB, tol, req) => {
    setRules((prev) => [...prev, { srcAField: srcA, matchMode: mode, srcBField: srcB, tolerance: tol, required: req === 'Y' }]);
  };

  // Filters
  const addFilter = () => {
    setFilters((prev) => [...prev, { applyTo: 'BOTH', column: '', operator: '=', value: '' }]);
  };
  const updateFilter = (idx, field, value) => {
    setFilters((prev) => prev.map((f, i) => i === idx ? { ...f, [field]: value } : f));
  };
  const removeFilter = (idx) => {
    setFilters((prev) => prev.filter((_, i) => i !== idx));
  };
  const addQuickFilter = (applyTo, column, operator, value) => {
    setFilters((prev) => [...prev, { applyTo, column, operator, value }]);
  };

  // Navigation
  const stepIdx = STEPS.findIndex((s) => s.id === currentStep);
  const goNext = () => { if (stepIdx < STEPS.length - 1) setCurrentStep(STEPS[stepIdx + 1].id); };
  const goPrev = () => { if (stepIdx > 0) setCurrentStep(STEPS[stepIdx - 1].id); };

  const toggleDay = (idx) => {
    setFormData((prev) => {
      const days = [...prev.activeDays];
      days[idx] = !days[idx];
      return { ...prev, activeDays: days };
    });
  };

  const buildJson = () => ({
    reconName: formData.reconName,
    reconType: formData.reconType,
    channel: formData.channel,
    frequency: formData.frequency,
    dateLogic: formData.dateLogic,
    matchType: formData.matchType,
    sources: {
      A: { fileType: formData.srcAFileType, template: formData.srcATemplate },
      B: { fileType: formData.srcBFileType, template: formData.srcBTemplate },
      ...(formData.reconType === '3WAY' ? { C: { fileType: formData.srcCFileType, template: formData.srcCTemplate } } : {}),
    },
    rules,
    filters: { logic: filterLogic, conditions: filters },
    schedule: {
      autoRun: formData.autoRun,
      cron: formData.cronExpr,
      rerunMode: formData.rerunMode,
      dupHandling: formData.dupHandling,
      activeDays: DAYS.filter((_, i) => formData.activeDays[i]),
    },
  });

  // ── Renderers ──

  const renderStep1 = () => (
    <div className={styles.sectionCard}>
      <div className={styles.sectionHead}>
        <div className={styles.sectionNum}>1</div>
        <span className={styles.sectionTitle}>What are you reconciling?</span>
        <span className={styles.sectionSub}>Give it a name and tell us the basics</span>
      </div>
      <div className={styles.sectionBody}>
        <div className={styles.formGrid4}>
          <Input
            label="Recon Name"
            placeholder="e.g. AEPS Daily Recon"
            value={formData.reconName}
            onChange={(e) => handleFieldChange('reconName', e.target.value)}
            required
          />
          <Input
            label="Recon Code"
            value={formData.reconCode}
            readOnly
            disabled
          />
          <Select
            label="Recon Type"
            placeholder="Select..."
            value={formData.reconType}
            onChange={(e) => handleFieldChange('reconType', e.target.value)}
            options={RECON_TYPE_OPTIONS}
            required
          />
          <Select
            label="Channel"
            placeholder="Select..."
            value={formData.channel}
            onChange={(e) => handleFieldChange('channel', e.target.value)}
            options={CHANNEL_OPTIONS}
            required
          />
        </div>
        <div className={styles.formGrid4} style={{ marginTop: 'var(--space-4)' }}>
          <Select
            label="Frequency"
            value={formData.frequency}
            onChange={(e) => handleFieldChange('frequency', e.target.value)}
            options={FREQUENCY_OPTIONS}
          />
          <Select
            label="Date Logic"
            value={formData.dateLogic}
            onChange={(e) => handleFieldChange('dateLogic', e.target.value)}
            options={DATE_LOGIC_OPTIONS}
          />
          <Select
            label="Matching Type"
            value={formData.matchType}
            onChange={(e) => handleFieldChange('matchType', e.target.value)}
            options={MATCH_TYPE_OPTIONS}
          />
          <Input
            label="Description"
            placeholder="Optional notes..."
            value={formData.description}
            onChange={(e) => handleFieldChange('description', e.target.value)}
          />
        </div>
      </div>
    </div>
  );

  const renderSourceCard = (label, fileTypeKey, templateKey, iconBg, iconColor, templateOptions) => (
    <div className={`${styles.sourceCard} ${formData[fileTypeKey] ? styles.filled : ''}`}>
      <div className={styles.sourceLabel}>{label}</div>
      <div className={styles.sourceIcon} style={{ background: iconBg }}>
        <FileText size={22} color={iconColor} />
      </div>
      <div className={styles.sourceFields}>
        <Select
          label="File Type"
          placeholder="Select..."
          value={formData[fileTypeKey]}
          onChange={(e) => {
            handleFieldChange(fileTypeKey, e.target.value);
            handleFieldChange(templateKey, '');
          }}
          options={FILE_TYPE_OPTIONS}
          required
        />
        <Select
          label="Template"
          placeholder={formData[fileTypeKey] ? 'Select template...' : '\u2190 Pick file type'}
          value={formData[templateKey]}
          onChange={(e) => handleFieldChange(templateKey, e.target.value)}
          options={templateOptions}
          required
        />
      </div>
      {formData[fileTypeKey] && (
        <div className={styles.stagingInfo}>
          Staging: <span>{STAGING_TABLES[formData[fileTypeKey]]}</span>
        </div>
      )}
    </div>
  );

  const renderStep2 = () => (
    <div className={styles.sectionCard}>
      <div className={styles.sectionHead}>
        <div className={styles.sectionNum}>2</div>
        <span className={styles.sectionTitle}>Pick your source files</span>
        <span className={styles.sectionSub}>Select the templates you created in Template Config</span>
      </div>
      <div className={styles.sectionBody}>
        <div className={styles.sourceLayout}>
          {renderSourceCard('Source A', 'srcAFileType', 'srcATemplate', 'rgba(29,95,163,0.1)', '#1D5FA3', srcATemplateOptions)}
          <div className={styles.matchConnector}>
            <div className={styles.matchLine} />
            <div className={styles.matchBadge}>{'\u27F7'} MATCH</div>
          </div>
          {renderSourceCard('Source B', 'srcBFileType', 'srcBTemplate', 'rgba(184,110,0,0.1)', '#B86E00', srcBTemplateOptions)}
        </div>

        {formData.reconType === '3WAY' && (
          <div className={styles.sourceCRow}>
            {renderSourceCard('Source C (3-Way)', 'srcCFileType', 'srcCTemplate', 'rgba(108,92,231,0.1)', '#6C5CE7', srcCTemplateOptions)}
          </div>
        )}

        <div className={styles.infoBoxBlue}>
          <Info size={16} />
          <div>Templates are created in <strong>Setup \u2192 Template Config</strong>. Each template defines the file structure (columns, types, delimiters). Select the correct template so the system knows which fields are available for matching.</div>
        </div>
      </div>
    </div>
  );

  const renderStep3 = () => (
    <div className={styles.sectionCard}>
      <div className={styles.sectionHead}>
        <div className={styles.sectionNum}>3</div>
        <span className={styles.sectionTitle}>How should records be matched?</span>
        <span className={styles.sectionSub}>Map fields from Source A to Source B</span>
      </div>
      <div className={styles.sectionBody}>
        <div className={styles.rulesHeader}>
          <div className={styles.rulesTitle}>
            <span>Matching Rules</span>
            <span className={styles.rulesCount}>{rules.length} rule{rules.length !== 1 ? 's' : ''}</span>
          </div>
          <Button variant="primary" size="small" onClick={addRule}>
            <Plus size={14} /> Add Rule
          </Button>
        </div>

        {rules.length > 0 && (
          <div className={styles.ruleLabels}>
            <div>#</div>
            <div className={styles.ruleLabelGrid}>
              <div>Source A Field</div>
              <div style={{ textAlign: 'center' }}>Match Type</div>
              <div>Source B Field</div>
              <div style={{ textAlign: 'center' }}>Tolerance</div>
              <div style={{ textAlign: 'center' }}>Required</div>
            </div>
            <div style={{ width: 28 }} />
          </div>
        )}

        {rules.map((rule, idx) => (
          <div key={idx} className={styles.ruleRow}>
            <div className={styles.ruleNum}>{idx + 1}</div>
            <div className={styles.ruleBody}>
              <select value={rule.srcAField} onChange={(e) => updateRule(idx, 'srcAField', e.target.value)}>
                <option value="">Select...</option>
                {srcAFields.map((f) => <option key={f} value={f}>{f}</option>)}
              </select>
              <select value={rule.matchMode} onChange={(e) => updateRule(idx, 'matchMode', e.target.value)}>
                {MATCH_MODE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <select value={rule.srcBField} onChange={(e) => updateRule(idx, 'srcBField', e.target.value)}>
                <option value="">Select...</option>
                {srcBFields.map((f) => <option key={f} value={f}>{f}</option>)}
              </select>
              <input
                type="text"
                value={rule.tolerance}
                onChange={(e) => updateRule(idx, 'tolerance', e.target.value)}
                placeholder="0"
              />
              <div className={styles.ruleRequired}>
                <input
                  type="checkbox"
                  checked={rule.required}
                  onChange={(e) => updateRule(idx, 'required', e.target.checked)}
                />
              </div>
            </div>
            <button className={styles.ruleDel} onClick={() => removeRule(idx)}>
              <Trash2 size={12} />
            </button>
          </div>
        ))}

        <div className={styles.infoBoxGold}>
          <AlertTriangle size={16} />
          <div><strong>How it works:</strong> Rules run top-to-bottom. The first rule (e.g. exact RRN match) is the strictest. If it doesn&apos;t match, the system tries Rule 2, then Rule 3, and so on. Mark a rule as <strong>Required</strong> if both fields <em>must</em> match.</div>
        </div>

        <div className={styles.presetSection}>
          <div className={styles.presetLabel}>Quick Add Common Rules:</div>
          <div className={styles.presetBtns}>
            <Button variant="outline" size="small" onClick={() => addPresetRule('RRN', 'EXACT', 'RRN', '0', 'Y')}>+ RRN Exact Match</Button>
            <Button variant="outline" size="small" onClick={() => addPresetRule('TXN_AMOUNT', 'EXACT', 'TXN_AMOUNT', '0', 'Y')}>+ Amount Exact</Button>
            <Button variant="outline" size="small" onClick={() => addPresetRule('TXN_AMOUNT', 'TOLERANCE', 'AMOUNT', '1.00', 'Y')}>+ Amount \u00b11</Button>
            <Button variant="outline" size="small" onClick={() => addPresetRule('UTR_NO', 'EXACT', 'UTR_NO', '0', 'N')}>+ UTR Match</Button>
            <Button variant="outline" size="small" onClick={() => addPresetRule('TXN_DATE', 'EXACT', 'VALUE_DATE', '0', 'N')}>+ Date Match</Button>
          </div>
        </div>
      </div>
    </div>
  );

  const renderStep4 = () => (
    <div className={styles.sectionCard}>
      <div className={styles.sectionHead}>
        <div className={styles.sectionNum}>4</div>
        <span className={styles.sectionTitle}>Filter which records to include</span>
        <span className={styles.sectionSub}>Optional \u2014 only process records that match these conditions</span>
      </div>
      <div className={styles.sectionBody}>
        <div className={styles.rulesHeader}>
          <div className={styles.rulesTitle}>
            <span>Conditions</span>
            <span className={styles.rulesCount}>{filters.length} filter{filters.length !== 1 ? 's' : ''}</span>
            <div className={styles.logicChip}>
              <button className={`${styles.logicBtn} ${filterLogic === 'AND' ? styles.active : ''}`} onClick={() => setFilterLogic('AND')}>AND</button>
              <button className={`${styles.logicBtn} ${filterLogic === 'OR' ? styles.active : ''}`} onClick={() => setFilterLogic('OR')}>OR</button>
            </div>
          </div>
          <Button variant="primary" size="small" onClick={addFilter}>
            <Plus size={14} /> Add Filter
          </Button>
        </div>

        {filters.length > 0 && (
          <div className={styles.filterLabels}>
            <div className={styles.fSrc}>Apply To</div>
            <div className={styles.fCol}>Column</div>
            <div className={styles.fOp}>Operator</div>
            <div className={styles.fVal}>Value(s)</div>
            <div style={{ width: 26 }} />
          </div>
        )}

        {filters.map((filter, idx) => (
          <div key={idx} className={styles.filterRow}>
            <select className={styles.fSrc} value={filter.applyTo} onChange={(e) => updateFilter(idx, 'applyTo', e.target.value)}>
              {APPLY_TO_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <select className={styles.fCol} value={filter.column} onChange={(e) => updateFilter(idx, 'column', e.target.value)}>
              <option value="">Select column...</option>
              {[...new Set([...srcAFields, ...srcBFields])].map((f) => <option key={f} value={f}>{f}</option>)}
            </select>
            <select className={styles.fOp} value={filter.operator} onChange={(e) => updateFilter(idx, 'operator', e.target.value)}>
              {OPERATOR_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <input className={styles.fVal} value={filter.value} onChange={(e) => updateFilter(idx, 'value', e.target.value)} placeholder="Value" />
            <button className={styles.ruleDel} onClick={() => removeFilter(idx)}>
              <Trash2 size={11} />
            </button>
          </div>
        ))}

        <div className={styles.presetSection}>
          <div className={styles.presetLabel}>Quick Add:</div>
          <div className={styles.presetBtns}>
            <Button variant="outline" size="small" onClick={() => addQuickFilter('BOTH', 'STATUS', '=', 'SUCCESS')}>+ Only SUCCESS</Button>
            <Button variant="outline" size="small" onClick={() => addQuickFilter('BOTH', 'REC_FLG', '!=', 'Y')}>+ Exclude Reconciled</Button>
            <Button variant="outline" size="small" onClick={() => addQuickFilter('BOTH', 'TXN_AMOUNT', '>', '0')}>+ Amount {'>'} 0</Button>
            <Button variant="outline" size="small" onClick={() => addQuickFilter('Source A', 'CHANNEL', 'IN', 'UPI,AEPS')}>+ Channel Filter</Button>
          </div>
        </div>

        <div className={styles.infoBoxBlue}>
          <Info size={16} />
          <div>Filters narrow down which records get processed. For example, &quot;STATUS = SUCCESS&quot; means only successful transactions will be picked up for matching. If no filters are added, <em>all</em> records from the source are processed.</div>
        </div>
      </div>
    </div>
  );

  const renderStep5 = () => (
    <div className={styles.sectionCard}>
      <div className={styles.sectionHead}>
        <div className={styles.sectionNum}>5</div>
        <span className={styles.sectionTitle}>When should this run?</span>
        <span className={styles.sectionSub}>Set up automatic scheduling or run manually</span>
      </div>
      <div className={styles.sectionBody}>
        <div className={styles.formGrid4}>
          <div>
            <label style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-semibold)', color: 'var(--color-neutral-500)', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'block', marginBottom: 'var(--space-1)' }}>Auto-Run</label>
            <div className={styles.toggle}>
              <label className={styles.toggleSwitch}>
                <input type="checkbox" checked={formData.autoRun} onChange={(e) => handleFieldChange('autoRun', e.target.checked)} />
                <span className={styles.toggleSlider} />
              </label>
              <span className={styles.toggleLabel}>Run automatically on schedule</span>
            </div>
          </div>
          {formData.autoRun && (
            <Input
              label="Cron / Time"
              value={formData.cronExpr}
              onChange={(e) => handleFieldChange('cronExpr', e.target.value)}
              placeholder="0 6 * * *"
            />
          )}
          <Select
            label="Re-Run Mode"
            value={formData.rerunMode}
            onChange={(e) => handleFieldChange('rerunMode', e.target.value)}
            options={RERUN_OPTIONS}
          />
          <Select
            label="Duplicate Handling"
            value={formData.dupHandling}
            onChange={(e) => handleFieldChange('dupHandling', e.target.value)}
            options={DUP_OPTIONS}
          />
        </div>
        {formData.autoRun && (
          <div style={{ marginTop: 'var(--space-4)' }}>
            <label style={{ fontSize: 'var(--text-xs)', fontWeight: 'var(--font-semibold)', color: 'var(--color-neutral-500)', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'block', marginBottom: 'var(--space-2)' }}>Active Days</label>
            <div className={styles.schedDays}>
              {DAYS.map((day, idx) => (
                <div
                  key={day}
                  className={`${styles.dayBtn} ${formData.activeDays[idx] ? styles.active : ''}`}
                  onClick={() => toggleDay(idx)}
                >
                  {day}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );

  const renderStep6 = () => {
    const json = buildJson();
    return (
      <div className={styles.sectionCard}>
        <div className={styles.sectionHead} style={{ background: 'linear-gradient(135deg, var(--color-primary-900), var(--color-primary-800))' }}>
          <div className={styles.sectionNum} style={{ background: 'var(--color-gold-500)', color: 'var(--color-primary-900)' }}>{'\u2713'}</div>
          <span className={styles.sectionTitle}>Review your configuration</span>
          <span className={styles.sectionSub}>Verify everything before submitting</span>
        </div>
        <div className={styles.sectionBody}>
          <div className={styles.summaryGrid}>
            <div className={styles.summaryCard}>
              <div className={styles.summaryValue} style={{ color: 'var(--color-primary-900)' }}>{formData.reconName || '\u2014'}</div>
              <div className={styles.summaryLabel}>Recon Name</div>
            </div>
            <div className={styles.summaryCard}>
              <div className={styles.summaryValue} style={{ color: 'var(--color-accent-600)' }}>{formData.reconType || '\u2014'}</div>
              <div className={styles.summaryLabel}>Recon Type</div>
            </div>
            <div className={styles.summaryCard}>
              <div className={styles.summaryValue} style={{ color: 'var(--color-gold-500)' }}>{rules.length}</div>
              <div className={styles.summaryLabel}>Matching Rules</div>
            </div>
            <div className={styles.summaryCard}>
              <div className={styles.summaryValue} style={{ color: 'var(--color-success-500, #1A7F4B)' }}>{filters.length}</div>
              <div className={styles.summaryLabel}>Filters</div>
            </div>
          </div>

          <div className={styles.reviewFlow}>
            <div className={styles.reviewFlowTitle}>MATCHING FLOW</div>
            <div className={styles.reviewFlowText}>
              {formData.srcAFileType && formData.srcBFileType ? (
                <>
                  <strong>{formData.srcAFileType}</strong> ({formData.srcATemplate || 'no template'})
                  {' \u27F7 '}
                  <strong>{formData.srcBFileType}</strong> ({formData.srcBTemplate || 'no template'})
                  {formData.reconType === '3WAY' && formData.srcCFileType && (
                    <>
                      {' \u27F7 '}
                      <strong>{formData.srcCFileType}</strong> ({formData.srcCTemplate || 'no template'})
                    </>
                  )}
                  <br />
                  {rules.length > 0 && (
                    <span>Match by: {rules.map((r, i) => `${r.srcAField} ${r.matchMode} ${r.srcBField}`).join(', ')}</span>
                  )}
                </>
              ) : (
                <span style={{ color: 'var(--color-neutral-400)' }}>Select source files to see matching flow</span>
              )}
            </div>
          </div>

          <div className={styles.jsonHeader}>
            <span className={styles.jsonLabel}>Config JSON</span>
            <Button variant="outline" size="small" onClick={() => navigator.clipboard.writeText(JSON.stringify(json, null, 2))}>
              <Copy size={12} /> Copy
            </Button>
          </div>
          <div className={styles.jsonBox}>
            <code>{JSON.stringify(json, null, 2)}</code>
          </div>

          <div className={styles.infoBoxGreen}>
            <Shield size={16} />
            <div><strong>Maker-Checker:</strong> After you submit, a Checker will review and approve this configuration. The recon will only run after approval.</div>
          </div>
        </div>
      </div>
    );
  };

  const stepRenderers = { what: renderStep1, sources: renderStep2, match: renderStep3, filter: renderStep4, schedule: renderStep5, review: renderStep6 };

  return (
    <motion.div className={styles.page} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
      <div className={styles.header}>
        <h1>Define Reconciliation</h1>
        <p>Pick your source files, tell us which fields to match, and you&apos;re done</p>
      </div>

      <Stepper steps={STEPS} currentStep={currentStep} onStepClick={setCurrentStep} />

      <motion.div key={currentStep} initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.3 }}>
        {stepRenderers[currentStep]()}
      </motion.div>

      <div className={styles.stepActions}>
        <div>
          {stepIdx > 0 && (
            <Button variant="outline" onClick={goPrev}>
              <ArrowLeft size={16} /> Previous
            </Button>
          )}
        </div>
        <div className={styles.stepActionsRight}>
          {currentStep === 'review' ? (
            <Button variant="primary" size="large" onClick={() => alert('Submitted for approval (dummy)')}>
              Submit for Approval <ArrowRight size={16} />
            </Button>
          ) : (
            <Button variant="primary" onClick={goNext}>
              Next <ArrowRight size={16} />
            </Button>
          )}
        </div>
      </div>
    </motion.div>
  );
};

export default ReconConfig;
