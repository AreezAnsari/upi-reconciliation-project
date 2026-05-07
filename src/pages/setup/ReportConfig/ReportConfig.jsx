import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import {
  Plus, Pencil, Eye, Trash2, ArrowLeft, Loader2, Search,
  ArrowUpDown, ArrowUp, ArrowDown, X, Check, ChevronDown,
  FileText, Code, PlusCircle, Save,
} from 'lucide-react';
import { Button, Card, Input, Select, RadioGroup, Checkbox } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './ReportConfig.module.css';

const OPERATORS = [
  { value: '=', label: '=' },
  { value: '!=', label: '!=' },
  { value: '>', label: '>' },
  { value: '<', label: '<' },
  { value: '>=', label: '>=' },
  { value: '<=', label: '<=' },
  { value: 'LIKE', label: 'LIKE' },
  { value: 'IN', label: 'IN' },
  { value: 'BETWEEN', label: 'BETWEEN' },
  { value: 'IS NULL', label: 'IS NULL' },
  { value: 'IS NOT NULL', label: 'IS NOT NULL' },
];

const LOGICAL_OPS = [
  { value: 'AND', label: 'AND' },
  { value: 'OR', label: 'OR' },
];

const OUTPUT_FORMATS = [
  { value: 'CSV', label: 'CSV' },
  { value: 'EXCEL', label: 'Excel' },
];

const PROCESS_TYPES = [
  { value: 'EXTRACTION', label: 'Extraction' },
  { value: 'RECONCILIATION', label: 'Reconciliation' },
];

// ─── Helper: deduplicate fieldDetails by field_id ───
const deduplicateFields = (fields) => {
  if (!fields || !fields.length) return [];
  const seen = new Map();
  fields.forEach((f) => {
    if (!seen.has(f.field_id)) {
      seen.set(f.field_id, f);
    }
  });
  return Array.from(seen.values());
};

// ─── Helper: build NVL query ───
const buildQuery = (columns, stageTableName, conditions) => {
  if (!columns || columns.length === 0) return '';
  const tableName = stageTableName || '<TABLE_NAME>';
  const colExpr = columns.map((c) => `NVL(${c}, 0)`).join(" || '~' || ");
  let query = `SELECT ${colExpr} AS DATA FROM ${tableName}`;

  const validConditions = (conditions || []).filter(
    (c) => c.column && c.column.trim() !== '' && c.operator && c.operator.trim() !== ''
  );
  if (validConditions.length > 0) {
    const whereParts = validConditions.map((c, idx) => {
      let part = '';
      if (idx > 0 && c.logicalOp) {
        part += ` ${c.logicalOp} `;
      }
      if (c.operator === 'IS NULL' || c.operator === 'IS NOT NULL') {
        part += `${c.column} ${c.operator}`;
      } else {
        part += `${c.column} ${c.operator} '${c.value || ''}'`;
      }
      return part;
    });
    query += ` WHERE ${whereParts.join('')}`;
  }
  return query + ';';
};

const EMPTY_CONDITION = { column: '', operator: '=', value: '', logicalOp: null };

const ReportConfig = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  // ─── List state ───
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // ─── View state ───
  const [page, setPage] = useState('list'); // list | view | add | edit
  const [viewData, setViewData] = useState(null);
  const [editId, setEditId] = useState(null);

  // ─── Form state ───
  const [formData, setFormData] = useState({
    file_name: '',
    report_key: '',
    report_date: '',
    report_type: 'CSV',
    process_type: 'EXTRACTION',
    process_id: null,
    selected_file: '',
    selected_columns: [],
    where_conditions: [{ ...EMPTY_CONDITION }],
    column_headers: [],
  });

  // ─── Dropdown data ───
  const [extractionData, setExtractionData] = useState([]);
  const [reconData, setReconData] = useState([]);
  const [loadingDropdowns, setLoadingDropdowns] = useState(false);

  // ─── Extraction flow selections ───
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [selectedFileId, setSelectedFileId] = useState('');

  // ─── Recon flow selections ───
  const [selectedProcessId, setSelectedProcessId] = useState('');
  const [selectedFileSlot, setSelectedFileSlot] = useState('');

  // ─── Submitting ───
  const [submitting, setSubmitting] = useState(false);

  // ─── Column header input ───
  const [headerInput, setHeaderInput] = useState('');

  const queryRef = useRef(null);

  // ─── Fetch list ───
  useEffect(() => {
    if (token) fetchConfigs();
  }, [token]);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const res = await authAPI.getReportConfigs(token);
      setConfigs(res?.data || []);
    } catch (err) {
      console.error('Failed to fetch report configs:', err);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to fetch report configurations.' });
    } finally {
      setLoading(false);
    }
  };

  // ─── Fetch dropdown data when entering add/edit ───
  const fetchDropdownData = useCallback(async (processType) => {
    setLoadingDropdowns(true);
    try {
      if (processType === 'EXTRACTION') {
        const res = await authAPI.getExtractionTemplates(token);
        setExtractionData(res?.data || []);
      } else {
        const res = await authAPI.getReconProcesses(token);
        setReconData(res?.data || []);
      }
    } catch (err) {
      console.error('Failed to fetch dropdown data:', err);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to load process data.' });
    } finally {
      setLoadingDropdowns(false);
    }
  }, [token]);

  // ─── Filter & sort ───
  const filteredConfigs = useMemo(() => {
    if (!searchTerm) return configs;
    const term = searchTerm.toLowerCase();
    return configs.filter(
      (c) =>
        c.file_name?.toLowerCase().includes(term) ||
        c.report_key?.toLowerCase().includes(term) ||
        c.process_type?.toLowerCase().includes(term)
    );
  }, [configs, searchTerm]);

  const sortedConfigs = useMemo(() => {
    if (!sortConfig.key) return filteredConfigs;
    return [...filteredConfigs].sort((a, b) => {
      const aVal = a[sortConfig.key] ?? '';
      const bVal = b[sortConfig.key] ?? '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredConfigs, sortConfig]);

  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  const getSortIcon = (key) => {
    if (sortConfig.key !== key) return <ArrowUpDown size={14} className={styles.sortIconInactive} />;
    return sortConfig.direction === 'asc'
      ? <ArrowUp size={14} className={styles.sortIconActive} />
      : <ArrowDown size={14} className={styles.sortIconActive} />;
  };

  // ─── Extraction computed ───
  const uniqueTemplates = useMemo(() => {
    const map = new Map();
    extractionData.forEach((item) => {
      const tid = item.templateDetails?.template_id;
      if (tid && !map.has(String(tid))) {
        map.set(String(tid), {
          value: String(tid),
          label: item.templateDetails.template_name || String(tid),
        });
      }
    });
    return Array.from(map.values());
  }, [extractionData]);

  const filesForTemplate = useMemo(() => {
    if (!selectedTemplateId) return [];
    return extractionData
      .filter((item) => String(item.templateDetails?.template_id) === selectedTemplateId)
      .map((item) => ({
        value: String(item.fileDetails?.file_id),
        label: item.fileDetails?.file_name || String(item.fileDetails?.file_id),
        _entry: item,
      }));
  }, [extractionData, selectedTemplateId]);

  const currentFieldDetails = useMemo(() => {
    if (formData.process_type === 'EXTRACTION') {
      if (!selectedFileId) return [];
      const entry = extractionData.find(
        (item) =>
          String(item.templateDetails?.template_id) === selectedTemplateId &&
          String(item.fileDetails?.file_id) === selectedFileId
      );
      return deduplicateFields(entry?.fieldDetails);
    } else {
      if (!selectedProcessId || !selectedFileSlot) return [];
      const process = reconData.find((p) => String(p.processId) === selectedProcessId);
      if (!process) return [];
      const fileData = process[selectedFileSlot];
      return deduplicateFields(fileData?.fieldDetails);
    }
  }, [formData.process_type, extractionData, reconData, selectedTemplateId, selectedFileId, selectedProcessId, selectedFileSlot]);

  const currentStageTableName = useMemo(() => {
    if (formData.process_type === 'EXTRACTION') {
      if (!selectedFileId) return '';
      const entry = extractionData.find(
        (item) =>
          String(item.templateDetails?.template_id) === selectedTemplateId &&
          String(item.fileDetails?.file_id) === selectedFileId
      );
      return entry?.templateDetails?.stageTableName || '';
    } else {
      if (!selectedProcessId || !selectedFileSlot) return '';
      const process = reconData.find((p) => String(p.processId) === selectedProcessId);
      if (!process) return '';
      const fileData = process[selectedFileSlot];
      return fileData?.templateDetails?.stageTableName || '';
    }
  }, [formData.process_type, extractionData, reconData, selectedTemplateId, selectedFileId, selectedProcessId, selectedFileSlot]);

  // ─── Recon computed ───
  const reconProcessOptions = useMemo(() => {
    return reconData.map((p) => ({
      value: String(p.processId),
      label: p.processName || String(p.processId),
    }));
  }, [reconData]);

  const reconFileSlots = useMemo(() => {
    if (!selectedProcessId) return [];
    const process = reconData.find((p) => String(p.processId) === selectedProcessId);
    if (!process) return [];
    const slots = [];
    ['file1', 'file2', 'file3', 'file4'].forEach((key, idx) => {
      if (process[key]) {
        slots.push({
          key,
          label: process[key].fileDetails?.file_name || `File ${idx + 1}`,
        });
      }
    });
    return slots;
  }, [reconData, selectedProcessId]);

  // ─── Query preview ───
  const queryPreview = useMemo(() => {
    return buildQuery(formData.selected_columns, currentStageTableName, formData.where_conditions);
  }, [formData.selected_columns, currentStageTableName, formData.where_conditions]);

  // ─── Handlers ───
  const handleAdd = () => {
    setFormData({
      file_name: '',
      report_key: '',
      report_date: '',
      report_type: 'CSV',
      process_type: 'EXTRACTION',
      process_id: null,
      selected_file: '',
      selected_columns: [],
      where_conditions: [{ ...EMPTY_CONDITION }],
      column_headers: [],
    });
    setSelectedTemplateId('');
    setSelectedFileId('');
    setSelectedProcessId('');
    setSelectedFileSlot('');
    setEditId(null);
    setPage('add');
    fetchDropdownData('EXTRACTION');
  };

  const handleEdit = (config) => {
    setFormData({
      file_name: config.file_name || '',
      report_key: config.report_key || '',
      report_date: config.report_date || '',
      report_type: config.report_type || 'CSV',
      process_type: config.process_type || 'EXTRACTION',
      process_id: config.process_id || null,
      selected_file: config.selected_file || '',
      selected_columns: config.selected_columns || [],
      where_conditions: config.where_conditions?.length > 0
        ? config.where_conditions
        : [{ ...EMPTY_CONDITION }],
      column_headers: config.column_headers || [],
    });
    setSelectedTemplateId('');
    setSelectedFileId('');
    setSelectedProcessId('');
    setSelectedFileSlot('');
    setEditId(config.id);
    setPage('edit');
    fetchDropdownData(config.process_type || 'EXTRACTION');
  };

  const handleView = (config) => {
    setViewData(config);
    setPage('view');
  };

  const handleDelete = async (config) => {
    if (!window.confirm(`Delete report "${config.file_name}"?`)) return;
    try {
      await authAPI.deleteReportConfig(config.id, token);
      addNotification({ type: 'success', title: 'Deleted', message: `Report "${config.file_name}" deleted.` });
      fetchConfigs();
    } catch (err) {
      addNotification({ type: 'error', title: 'Error', message: 'Failed to delete report config.' });
    }
  };

  const handleBack = () => {
    setPage('list');
    setViewData(null);
    setEditId(null);
  };

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleProcessTypeChange = (value) => {
    handleFormChange('process_type', value);
    handleFormChange('selected_columns', []);
    handleFormChange('selected_file', '');
    handleFormChange('process_id', null);
    setSelectedTemplateId('');
    setSelectedFileId('');
    setSelectedProcessId('');
    setSelectedFileSlot('');
    fetchDropdownData(value);
  };

  const handleTemplateSelect = (e) => {
    const tid = e.target.value;
    setSelectedTemplateId(tid);
    setSelectedFileId('');
    handleFormChange('selected_columns', []);
    handleFormChange('selected_file', '');
  };

  const handleFileSelect = (e) => {
    const fid = e.target.value;
    setSelectedFileId(fid);
    handleFormChange('selected_columns', []);
    // Set selected_file name
    const entry = extractionData.find(
      (item) =>
        String(item.templateDetails?.template_id) === selectedTemplateId &&
        String(item.fileDetails?.file_id) === fid
    );
    handleFormChange('selected_file', entry?.fileDetails?.file_name || '');
  };

  const handleReconProcessSelect = (e) => {
    const pid = e.target.value;
    setSelectedProcessId(pid);
    setSelectedFileSlot('');
    handleFormChange('selected_columns', []);
    handleFormChange('selected_file', '');
    handleFormChange('process_id', parseInt(pid) || null);
  };

  const handleFileSlotSelect = (slotKey) => {
    setSelectedFileSlot(slotKey);
    handleFormChange('selected_columns', []);
    const process = reconData.find((p) => String(p.processId) === selectedProcessId);
    if (process && process[slotKey]) {
      handleFormChange('selected_file', process[slotKey].fileDetails?.file_name || '');
    }
  };

  const handleColumnToggle = (fieldName) => {
    setFormData((prev) => {
      const cols = prev.selected_columns;
      if (cols.includes(fieldName)) {
        return { ...prev, selected_columns: cols.filter((c) => c !== fieldName) };
      }
      return { ...prev, selected_columns: [...cols, fieldName] };
    });
  };

  const handleSelectAllColumns = () => {
    const allNames = currentFieldDetails.map((f) => f.field_name);
    const allSelected = allNames.every((n) => formData.selected_columns.includes(n));
    if (allSelected) {
      handleFormChange('selected_columns', []);
    } else {
      handleFormChange('selected_columns', allNames);
    }
  };

  // ─── WHERE conditions ───
  const handleConditionChange = (idx, field, value) => {
    setFormData((prev) => {
      const updated = [...prev.where_conditions];
      updated[idx] = { ...updated[idx], [field]: value };
      return { ...prev, where_conditions: updated };
    });
  };

  const addCondition = () => {
    setFormData((prev) => ({
      ...prev,
      where_conditions: [...prev.where_conditions, { column: '', operator: '=', value: '', logicalOp: 'AND' }],
    }));
  };

  const removeCondition = (idx) => {
    setFormData((prev) => {
      const updated = prev.where_conditions.filter((_, i) => i !== idx);
      if (updated.length === 0) return { ...prev, where_conditions: [{ ...EMPTY_CONDITION }] };
      // First condition should not have logicalOp
      if (updated[0]) updated[0] = { ...updated[0], logicalOp: null };
      return { ...prev, where_conditions: updated };
    });
  };

  // ─── Column headers (tag builder) ───
  const handleHeaderKeyDown = (e) => {
    if (e.key === 'Enter' && headerInput.trim()) {
      e.preventDefault();
      setFormData((prev) => ({
        ...prev,
        column_headers: [...prev.column_headers, headerInput.trim()],
      }));
      setHeaderInput('');
    }
  };

  const removeHeader = (idx) => {
    setFormData((prev) => ({
      ...prev,
      column_headers: prev.column_headers.filter((_, i) => i !== idx),
    }));
  };

  // ─── Submit ───
  const handleSubmit = async () => {
    if (!formData.file_name.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Report Name is required.' });
      return;
    }
    if (formData.selected_columns.length === 0) {
      addNotification({ type: 'error', title: 'Validation', message: 'Select at least one column.' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        file_name: formData.file_name,
        process_id: formData.process_id,
        report_key: formData.report_key,
        process_type: formData.process_type,
        selected_file: formData.selected_file,
        report_type: formData.report_type,
        selected_columns: formData.selected_columns,
        where_conditions: formData.where_conditions.filter(
          (c) => c.column && c.column.trim() !== ''
        ),
      };

      if (editId) {
        await authAPI.updateReportConfig(editId, payload, token);
        addNotification({ type: 'success', title: 'Updated', message: 'Report config updated successfully.' });
      } else {
        await authAPI.createReportConfig(payload, token);
        addNotification({ type: 'success', title: 'Created', message: 'Report config created successfully.' });
      }
      setPage('list');
      fetchConfigs();
    } catch (err) {
      console.error('Submit failed:', err);
      addNotification({ type: 'error', title: 'Error', message: 'Failed to save report config.' });
    } finally {
      setSubmitting(false);
    }
  };

  const handlePreviewQuery = () => {
    if (queryRef.current) {
      queryRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
      queryRef.current.classList.add(styles.queryHighlight);
      setTimeout(() => queryRef.current?.classList.remove(styles.queryHighlight), 1500);
    }
  };

  const getVal = (value) => {
    if (value === null || value === undefined || value === '') return '—';
    return String(value);
  };

  // ═══════════════════════════════════════
  // VIEW PAGE
  // ═══════════════════════════════════════
  if (page === 'view' && viewData) {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBack} className={styles.backBtn}>
            Back to Report Configs
          </Button>
          <h1>View Report Configuration</h1>
          <p>{viewData.file_name}</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          {/* General Details */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>General Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Report Name</span>
                <span className={styles.viewValue}>{getVal(viewData.file_name)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Report Key</span>
                <span className={styles.viewValue}>{getVal(viewData.report_key)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Process Type</span>
                <span className={styles.viewValue}>{getVal(viewData.process_type)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Output Format</span>
                <span className={styles.viewValue}>{getVal(viewData.report_type)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Selected File</span>
                <span className={styles.viewValue}>{getVal(viewData.selected_file)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Process ID</span>
                <span className={styles.viewValue}>{getVal(viewData.process_id)}</span>
              </div>
            </div>
          </div>

          {/* Selected Columns */}
          {viewData.selected_columns && viewData.selected_columns.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>Selected Columns</div>
              <div className={styles.tagsViewContainer}>
                {viewData.selected_columns.map((col, idx) => (
                  <span key={idx} className={styles.fieldTag}>{col}</span>
                ))}
              </div>
            </div>
          )}

          {/* WHERE Conditions */}
          {viewData.where_conditions && viewData.where_conditions.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>WHERE Conditions</div>
              <div className={styles.conditionsViewList}>
                {viewData.where_conditions.map((cond, idx) => (
                  <div key={idx} className={styles.conditionViewRow}>
                    {idx > 0 && cond.logicalOp && (
                      <span className={styles.logicalOpTag}>{cond.logicalOp}</span>
                    )}
                    <span className={styles.conditionText}>
                      {cond.column} {cond.operator}{' '}
                      {cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL' ? `'${cond.value}'` : ''}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className={styles.viewActions}>
            <Button variant="secondary" onClick={handleBack}>Back</Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ═══════════════════════════════════════
  // ADD / EDIT FORM
  // ═══════════════════════════════════════
  if (page === 'add' || page === 'edit') {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBack} className={styles.backBtn}>
            Back to Report Configs
          </Button>
          <h1>{page === 'edit' ? 'Edit Report Configuration' : 'Add Report Configuration'}</h1>
          <p>Fill in the details to {page === 'edit' ? 'update' : 'create'} a report configuration</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          {/* ── Section 1: Basic Information ── */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>
              <span className={styles.sectionNumber}>1</span>
              Basic Information
            </div>
            <div className={styles.formSectionBody}>
              <div className={styles.formGrid}>
                <Input
                  label="Report Name"
                  required
                  value={formData.file_name}
                  onChange={(e) => handleFormChange('file_name', e.target.value)}
                  placeholder="e.g. SWITCH_RECON"
                />
                <Input
                  label="Report Key"
                  value={formData.report_key}
                  onChange={(e) => handleFormChange('report_key', e.target.value)}
                  placeholder="e.g. SWITCH_RECON_2025"
                />
                <Input
                  label="Report Date"
                  type="date"
                  value={formData.report_date}
                  onChange={(e) => handleFormChange('report_date', e.target.value)}
                />
                <RadioGroup
                  label="Output Format"
                  name="report_type"
                  options={OUTPUT_FORMATS}
                  value={formData.report_type}
                  onChange={(val) => handleFormChange('report_type', val)}
                />
              </div>
            </div>
          </div>

          {/* ── Section 2: Process Selection ── */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>
              <span className={styles.sectionNumber}>2</span>
              Process Selection
            </div>
            <div className={styles.formSectionBody}>
              <div className={styles.processTypeRow}>
                <RadioGroup
                  label="Process Type"
                  name="process_type"
                  options={PROCESS_TYPES}
                  value={formData.process_type}
                  onChange={handleProcessTypeChange}
                />
              </div>

              {loadingDropdowns ? (
                <div className={styles.loadingInline}>
                  <Loader2 size={18} className={styles.spinner} />
                  <span>Loading data...</span>
                </div>
              ) : formData.process_type === 'EXTRACTION' ? (
                /* ── Extraction Flow ── */
                <div className={styles.selectionFlow}>
                  <div className={styles.formGrid}>
                    <Select
                      label="Select Template"
                      options={uniqueTemplates}
                      value={selectedTemplateId}
                      onChange={handleTemplateSelect}
                      placeholder="Choose a template"
                    />
                    <Select
                      label="Select File"
                      options={filesForTemplate}
                      value={selectedFileId}
                      onChange={handleFileSelect}
                      placeholder={selectedTemplateId ? 'Choose a file' : 'Select template first'}
                      disabled={!selectedTemplateId}
                    />
                  </div>

                  {/* Column Picker */}
                  {currentFieldDetails.length > 0 && (
                    <ColumnPicker
                      fields={currentFieldDetails}
                      selectedColumns={formData.selected_columns}
                      onToggle={handleColumnToggle}
                      onSelectAll={handleSelectAllColumns}
                    />
                  )}
                </div>
              ) : (
                /* ── Reconciliation Flow ── */
                <div className={styles.selectionFlow}>
                  <div className={styles.formGrid}>
                    <Select
                      label="Select Process"
                      options={reconProcessOptions}
                      value={selectedProcessId}
                      onChange={handleReconProcessSelect}
                      placeholder="Choose a process"
                    />
                    <div />
                  </div>

                  {/* File Tabs */}
                  {reconFileSlots.length > 0 && (
                    <div className={styles.fileTabs}>
                      <span className={styles.fileTabsLabel}>Select File:</span>
                      <div className={styles.fileTabsRow}>
                        {reconFileSlots.map((slot) => (
                          <button
                            key={slot.key}
                            className={`${styles.fileTab} ${selectedFileSlot === slot.key ? styles.fileTabActive : ''}`}
                            onClick={() => handleFileSlotSelect(slot.key)}
                          >
                            {slot.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Column Picker */}
                  {currentFieldDetails.length > 0 && (
                    <ColumnPicker
                      fields={currentFieldDetails}
                      selectedColumns={formData.selected_columns}
                      onToggle={handleColumnToggle}
                      onSelectAll={handleSelectAllColumns}
                    />
                  )}
                </div>
              )}
            </div>
          </div>

          {/* ── Section 3: WHERE Conditions ── */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>
              <span className={styles.sectionNumber}>3</span>
              WHERE Conditions
            </div>
            <div className={styles.formSectionBody}>
              <div className={styles.conditionsList}>
                {formData.where_conditions.map((cond, idx) => (
                  <div key={idx} className={styles.conditionRow}>
                    {idx > 0 && (
                      <div className={styles.conditionLogical}>
                        <select
                          className={styles.conditionSmallSelect}
                          value={cond.logicalOp || 'AND'}
                          onChange={(e) => handleConditionChange(idx, 'logicalOp', e.target.value)}
                        >
                          <option value="AND">AND</option>
                          <option value="OR">OR</option>
                        </select>
                      </div>
                    )}
                    <div className={styles.conditionFields}>
                      <input
                        className={styles.conditionInput}
                        type="text"
                        placeholder="Column name"
                        value={cond.column}
                        onChange={(e) => handleConditionChange(idx, 'column', e.target.value)}
                      />
                      <select
                        className={styles.conditionSelect}
                        value={cond.operator}
                        onChange={(e) => handleConditionChange(idx, 'operator', e.target.value)}
                      >
                        {OPERATORS.map((op) => (
                          <option key={op.value} value={op.value}>{op.label}</option>
                        ))}
                      </select>
                      {cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL' && (
                        <input
                          className={styles.conditionInput}
                          type="text"
                          placeholder="Value"
                          value={cond.value}
                          onChange={(e) => handleConditionChange(idx, 'value', e.target.value)}
                        />
                      )}
                      <button
                        className={styles.conditionRemove}
                        onClick={() => removeCondition(idx)}
                        title="Remove condition"
                      >
                        <X size={16} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              <button className={styles.addConditionBtn} onClick={addCondition}>
                <PlusCircle size={16} />
                Add Condition
              </button>
            </div>
          </div>

          {/* ── Section 4: Report Header & Query Preview ── */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>
              <span className={styles.sectionNumber}>4</span>
              Report Header & Query Preview
            </div>
            <div className={styles.formSectionBody}>
              {/* Column Header Tags */}
              <div className={styles.headerBuilderGroup}>
                <label className={styles.fieldLabel}>Column Headers</label>
                <div className={styles.headerTagsContainer}>
                  {formData.column_headers.map((h, idx) => (
                    <span key={idx} className={styles.headerTag}>
                      {h}
                      <button className={styles.headerTagRemove} onClick={() => removeHeader(idx)}>
                        <X size={12} />
                      </button>
                    </span>
                  ))}
                </div>
                <input
                  className={styles.headerTagInput}
                  type="text"
                  value={headerInput}
                  onChange={(e) => setHeaderInput(e.target.value)}
                  onKeyDown={handleHeaderKeyDown}
                  placeholder="Type column header and press Enter..."
                />
              </div>

              {/* Query Preview */}
              <div className={styles.queryGroup}>
                <label className={styles.fieldLabel}>Generated Report Query</label>
                <div className={styles.queryPreview} ref={queryRef}>
                  <code className={styles.queryCode}>
                    {queryPreview || 'Select columns to generate query preview...'}
                  </code>
                </div>
              </div>
            </div>
          </div>

          {/* ── Form Actions ── */}
          <div className={styles.formActions}>
            <Button variant="secondary" onClick={handleBack}>Cancel</Button>
            <Button variant="outline" onClick={handlePreviewQuery} leftIcon={<Code size={16} />}>
              Preview Query
            </Button>
            <Button variant="gold" onClick={handleSubmit} disabled={submitting} leftIcon={submitting ? <Loader2 size={16} className={styles.spinner} /> : <Save size={16} />}>
              {submitting ? 'Saving...' : (page === 'edit' ? 'Update Report Config' : 'Save Report Config')}
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ═══════════════════════════════════════
  // LIST PAGE
  // ═══════════════════════════════════════
  return (
    <div className={styles.page}>
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
        <h1>Report Configuration</h1>
        <p>Manage report configurations for extraction and reconciliation</p>
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <Card className={styles.tableCard}>
          {/* Controls */}
          <div className={styles.tableControls}>
            <div className={styles.showEntries}>
              <span>Showing {filteredConfigs.length} of {configs.length} entries</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search reports..."
                />
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleAdd}>
                Add
              </Button>
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className={styles.loadingOverlay}>
              <Loader2 size={24} className={styles.spinner} />
              <span>Loading report configurations...</span>
            </div>
          ) : (
            <div className={styles.tableContainer}>
              <table className={styles.mainTable}>
                <thead>
                  <tr>
                    <th className={styles.sortable} onClick={() => handleSort('file_name')}>
                      <div className={styles.thContent}>
                        <span>Report Name</span>
                        {getSortIcon('file_name')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('report_type')}>
                      <div className={styles.thContent}>
                        <span>Output Format</span>
                        {getSortIcon('report_type')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('selected_file')}>
                      <div className={styles.thContent}>
                        <span>Selected File</span>
                        {getSortIcon('selected_file')}
                      </div>
                    </th>
                    <th className={styles.actionCol}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedConfigs.length === 0 ? (
                    <tr>
                      <td colSpan={4} className={styles.emptyCell}>
                        No report configurations found
                      </td>
                    </tr>
                  ) : (
                    sortedConfigs.map((config) => (
                      <tr key={config.id}>
                        <td>{config.file_name || '—'}</td>
                        <td>{config.report_type || '—'}</td>
                        <td>{config.selected_file || '—'}</td>
                        <td className={styles.actionCol}>
                          <div className={styles.actionBtns}>
                            <button className={styles.iconBtn} title="Edit" onClick={() => handleEdit(config)}>
                              <Pencil size={15} />
                            </button>
                            <button className={`${styles.iconBtn} ${styles.iconBtnView}`} title="View" onClick={() => handleView(config)}>
                              <Eye size={15} />
                            </button>
                            <button className={`${styles.iconBtn} ${styles.iconBtnDanger}`} title="Delete" onClick={() => handleDelete(config)}>
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </motion.div>
    </div>
  );
};

// ═══════════════════════════════════════
// COLUMN PICKER COMPONENT
// ═══════════════════════════════════════
const ColumnPicker = ({ fields, selectedColumns, onToggle, onSelectAll }) => {
  const allSelected = fields.length > 0 && fields.every((f) => selectedColumns.includes(f.field_name));

  return (
    <div className={styles.columnPicker}>
      <div className={styles.columnPickerHeader}>
        <div className={styles.columnPickerTitle}>
          <FileText size={16} />
          <span>Available Columns</span>
        </div>
        <div className={styles.columnPickerMeta}>
          <span className={styles.selectedBadge}>{selectedColumns.length} selected</span>
          <button className={styles.selectAllBtn} onClick={onSelectAll}>
            {allSelected ? 'Deselect All' : 'Select All'}
          </button>
        </div>
      </div>
      <div className={styles.columnPickerList}>
        {fields.map((f) => (
          <label
            key={f.field_id}
            className={`${styles.columnItem} ${selectedColumns.includes(f.field_name) ? styles.columnItemSelected : ''}`}
          >
            <Checkbox
              checked={selectedColumns.includes(f.field_name)}
              onChange={() => onToggle(f.field_name)}
            />
            <span className={styles.columnName}>{f.field_name}</span>
          </label>
        ))}
      </div>
    </div>
  );
};

export default ReportConfig;
