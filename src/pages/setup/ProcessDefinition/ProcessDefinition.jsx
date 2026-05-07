import { useState, useEffect, useMemo, useRef } from 'react';
import { motion } from 'framer-motion';
import { Plus, Pencil, Eye, Trash2, ArrowLeft, Loader2, Search, ArrowUpDown, ArrowUp, ArrowDown, ChevronDown, Check, X } from 'lucide-react';
import { Button, Card } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './ProcessDefinition.module.css';

const MATCHING_TYPE_MAP = {
  '1': 'One To One',
  '2': 'One To Many',
  '3': 'Many To Many',
};

const MATCHING_FIELD_OPTIONS = ['TRAN_AMOUNT', 'TRAN_DATE', 'TRAN_SEQ_NUM'];

const ProcessDefinition = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [processes, setProcesses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewData, setViewData] = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingProcess, setEditingProcess] = useState(null);

  // Table controls
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Add form state
  const [formData, setFormData] = useState({
    processName: '',
    tranChannel: '',
    retentionPeriod: '',
    retentionVolume: '',
    matchingType: '1',
    inputCount: 2,
    reconType: '',
    jpsRpsl: '',
    identicalMatching: 'N',
    instCode: '',
    insUser: '',
    processMastId: '',
  });
  const [fileTypeMappings, setFileTypeMappings] = useState([]);
  const [templateMappings, setTemplateMappings] = useState([]);
  const [matchingFields, setMatchingFields] = useState([]);
  const [extractionTemplates, setExtractionTemplates] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (token) {
      fetchProcesses();
    }
  }, [token]);

  // Initialize dynamic arrays when inputCount changes
  useEffect(() => {
    const count = parseInt(formData.inputCount) || 0;
    setFileTypeMappings((prev) => {
      const arr = [];
      for (let i = 0; i < count; i++) {
        arr.push(prev[i] || { fileTypeNumber: i + 1, fileTypeId: '' });
      }
      return arr;
    });
    setTemplateMappings((prev) => {
      const arr = [];
      for (let i = 0; i < count; i++) {
        arr.push(prev[i] || { templateNumber: i + 1, templateId: '', templateName: '' });
      }
      return arr;
    });
    setMatchingFields((prev) => {
      const arr = [];
      for (let i = 0; i < count; i++) {
        arr.push(prev[i] || { fieldNumber: i + 1, selectedFields: [] });
      }
      return arr;
    });
  }, [formData.inputCount]);

  const fetchProcesses = async () => {
    setLoading(true);
    try {
      const data = await authAPI.getProcessDefinitions(token);
      setProcesses(data || []);
    } catch (error) {
      console.error('Failed to fetch process definitions:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to fetch process definitions. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchDropdownData = async () => {
    try {
      const res = await authAPI.getExtractionTemplates(token);
      const items = res?.data || res || [];
      const data = items.filter((item) => item.fileDetails && !Array.isArray(item.fileDetails));
      setExtractionTemplates(data);
    } catch (error) {
      console.error('Failed to fetch dropdown data:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to load dropdown data.',
      });
    }
  };

  // Filter
  const filteredProcesses = useMemo(() => {
    if (!searchTerm) return processes;
    const term = searchTerm.toLowerCase();
    return processes.filter(
      (p) =>
        p.processName?.toLowerCase().includes(term) ||
        p.reconType?.toLowerCase().includes(term)
    );
  }, [processes, searchTerm]);

  // Sort
  const sortedProcesses = useMemo(() => {
    if (!sortConfig.key) return filteredProcesses;
    return [...filteredProcesses].sort((a, b) => {
      const aVal = a[sortConfig.key] ?? '';
      const bVal = b[sortConfig.key] ?? '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredProcesses, sortConfig]);

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

  const handleView = (process) => {
    setViewData(process);
  };

  const handleBack = () => {
    setViewData(null);
    setShowAddForm(false);
    setEditingProcess(null);
  };

  const handleDelete = (process) => {
    addNotification({
      type: 'info',
      title: 'Delete',
      message: `Delete action for "${process.processName}" is not yet implemented.`,
    });
  };

  const handleEdit = (process) => {
    setEditingProcess(process);
    const count = process.inputCount || 2;
    setFormData({
      processName: process.processName || '',
      tranChannel: process.tranChannel || '',
      retentionPeriod: process.retentionPeriod ?? '',
      retentionVolume: process.retentionVolume ?? '',
      matchingType: process.matchingType || '1',
      inputCount: count,
      reconType: process.reconType || '',
      jpsRpsl: process.jpsRpsl || '',
      identicalMatching: process.identicalMatching || 'N',
      instCode: process.instCode ?? '',
      insUser: process.insUser ?? '',
      processMastId: process.processMastId ?? '',
    });
    const ftMappings = (process.fileTypeMappings || process.fileTypes || []).map((ft, i) => ({
      fileTypeNumber: ft.fileTypeNumber || i + 1,
      fileTypeId: ft.fileTypeId ?? '',
      fileTypeName: ft.fileTypeName || '',
    }));
    const tplMappings = (process.templateMappings || process.templates || []).map((tpl, i) => ({
      templateNumber: tpl.templateNumber || i + 1,
      templateId: tpl.templateId ?? '',
      templateName: tpl.templateName || '',
    }));
    const mfMappings = (process.matchingFields || []).map((mf, i) => ({
      fieldNumber: mf.fieldNumber || i + 1,
      selectedFields: mf.selectedFields || [],
    }));
    // Ensure arrays match inputCount
    while (ftMappings.length < count) ftMappings.push({ fileTypeNumber: ftMappings.length + 1, fileTypeId: '' });
    while (tplMappings.length < count) tplMappings.push({ templateNumber: tplMappings.length + 1, templateId: '', templateName: '' });
    while (mfMappings.length < count) mfMappings.push({ fieldNumber: mfMappings.length + 1, selectedFields: [] });
    setFileTypeMappings(ftMappings);
    setTemplateMappings(tplMappings);
    setMatchingFields(mfMappings);
    setShowAddForm(true);
    fetchDropdownData();
  };

  const handleAdd = () => {
    setFormData({
      processName: '',
      tranChannel: '',
      retentionPeriod: '',
      retentionVolume: '',
      matchingType: '1',
      inputCount: 2,
      reconType: '',
      jpsRpsl: '',
      identicalMatching: 'N',
      instCode: '',
      insUser: '',
      processMastId: '',
    });
    setFileTypeMappings([
      { fileTypeNumber: 1, fileTypeId: '' },
      { fileTypeNumber: 2, fileTypeId: '' },
    ]);
    setTemplateMappings([
      { templateNumber: 1, templateId: '', templateName: '' },
      { templateNumber: 2, templateId: '', templateName: '' },
    ]);
    setMatchingFields([
      { fieldNumber: 1, selectedFields: [] },
      { fieldNumber: 2, selectedFields: [] },
    ]);
    setShowAddForm(true);
    fetchDropdownData();
  };

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleFileTypeChange = (index, value) => {
    const selected = extractionTemplates.find((item) => String(item.fileDetails.file_id) === String(value));
    setFileTypeMappings((prev) => {
      const updated = [...prev];
      updated[index] = {
        ...updated[index],
        fileTypeId: value,
        fileTypeName: selected ? selected.fileDetails.file_name : '',
      };
      return updated;
    });
    // Auto-populate template from the same object
    if (selected) {
      setTemplateMappings((prev) => {
        const updated = [...prev];
        updated[index] = {
          ...updated[index],
          templateId: selected.templateDetails.template_id,
          templateName: selected.templateDetails.template_name,
        };
        return updated;
      });
    }
  };

  const handleMatchingFieldToggle = (index, fieldName) => {
    setMatchingFields((prev) => {
      const updated = [...prev];
      const current = updated[index].selectedFields || [];
      if (current.includes(fieldName)) {
        updated[index] = { ...updated[index], selectedFields: current.filter((f) => f !== fieldName) };
      } else {
        updated[index] = { ...updated[index], selectedFields: [...current, fieldName] };
      }
      return updated;
    });
  };

  const handleSubmit = async () => {
    if (!formData.processName.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Process Name is required.' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        processName: formData.processName,
        tranChannel: formData.tranChannel,
        retentionPeriod: parseInt(formData.retentionPeriod) || 0,
        retentionVolume: parseInt(formData.retentionVolume) || 0,
        matchingType: formData.matchingType,
        inputCount: parseInt(formData.inputCount) || 0,
        reconType: formData.reconType,
        jpsRpsl: formData.jpsRpsl,
        identicalMatching: formData.identicalMatching,
        instCode: parseInt(formData.instCode) || 0,
        insUser: parseInt(formData.insUser) || 0,
        processMastId: parseInt(formData.processMastId) || 0,
        fileTypeMappings: fileTypeMappings.map((ft) => ({
          fileTypeNumber: ft.fileTypeNumber,
          fileTypeId: parseInt(ft.fileTypeId) || 0,
        })),
        templateMappings: templateMappings.map((tpl) => ({
          templateNumber: tpl.templateNumber,
          templateId: parseInt(tpl.templateId) || 0,
          templateName: tpl.templateName,
        })),
        matchingFields: matchingFields.map((mf) => ({
          fieldNumber: mf.fieldNumber,
          selectedFields: mf.selectedFields,
        })),
      };

      if (editingProcess) {
        const processId = editingProcess.processId || editingProcess.id;
        await authAPI.updateProcessDefinition(processId, payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'Process definition updated successfully.',
        });
      } else {
        await authAPI.addProcessDefinition(payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'Process definition added successfully.',
        });
      }
      setShowAddForm(false);
      setEditingProcess(null);
      fetchProcesses();
    } catch (error) {
      console.error(`Failed to ${editingProcess ? 'update' : 'add'} process definition:`, error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: `Failed to ${editingProcess ? 'update' : 'add'} process definition. Please try again.`,
      });
    } finally {
      setSubmitting(false);
    }
  };

  const getVal = (value) => {
    if (value === null || value === undefined || value === '') return '—';
    if (value === 'Y') return 'YES';
    if (value === 'N') return 'NO';
    return String(value);
  };

  // ─── Add Form Page ───
  if (showAddForm) {
    return (
      <div className={styles.page}>
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={styles.header}
        >
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ArrowLeft size={18} />}
            onClick={handleBack}
            className={styles.backBtn}
          >
            Back to Process Definition
          </Button>
          <h1>{editingProcess ? 'Edit Process Definition' : 'Add Process Definition'}</h1>
          <p>{editingProcess ? 'Update the details of the process definition' : 'Fill in the details to create a new process definition'}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          {/* General Details */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>General Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Process Name</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.processName}
                  onChange={(e) => handleFormChange('processName', e.target.value)}
                  placeholder="Enter process name"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Input Count</span>
                <input
                  className={styles.formInput}
                  type="number"
                  min="1"
                  value={formData.inputCount}
                  onChange={(e) => handleFormChange('inputCount', e.target.value)}
                  placeholder="Enter input count"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Tran Channel</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.tranChannel}
                  onChange={(e) => handleFormChange('tranChannel', e.target.value)}
                  placeholder="Enter tran channel"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Recon Type</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.reconType}
                  onChange={(e) => handleFormChange('reconType', e.target.value)}
                  placeholder="Enter recon type"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Retention Period (in days)</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.retentionPeriod}
                  onChange={(e) => handleFormChange('retentionPeriod', e.target.value)}
                  placeholder="Enter retention period"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Retention Volume</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.retentionVolume}
                  onChange={(e) => handleFormChange('retentionVolume', e.target.value)}
                  placeholder="Enter retention volume"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Matching Type</span>
                <select
                  className={styles.formSelect}
                  value={formData.matchingType}
                  onChange={(e) => handleFormChange('matchingType', e.target.value)}
                >
                  <option value="1">One To One</option>
                  <option value="2">One To Many</option>
                  <option value="3">Many To Many</option>
                </select>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Identical Matching</span>
                <select
                  className={styles.formSelect}
                  value={formData.identicalMatching}
                  onChange={(e) => handleFormChange('identicalMatching', e.target.value)}
                >
                  <option value="Y">YES</option>
                  <option value="N">NO</option>
                </select>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>JPB and RPSL</span>
                <input
                  className={styles.formInput}
                  type="text"
                  value={formData.jpsRpsl}
                  onChange={(e) => handleFormChange('jpsRpsl', e.target.value)}
                  placeholder="Enter JPB/RPSL"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Inst Code</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.instCode}
                  onChange={(e) => handleFormChange('instCode', e.target.value)}
                  placeholder="Enter inst code"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Ins User</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.insUser}
                  onChange={(e) => handleFormChange('insUser', e.target.value)}
                  placeholder="Enter ins user"
                />
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Process Mast ID</span>
                <input
                  className={styles.formInput}
                  type="number"
                  value={formData.processMastId}
                  onChange={(e) => handleFormChange('processMastId', e.target.value)}
                  placeholder="Enter process mast ID"
                />
              </div>
            </div>
          </div>

          {/* File Types & Templates */}
          {fileTypeMappings.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>File Types & Templates</div>
              <div className={styles.viewGrid}>
                {fileTypeMappings.map((ft, idx) => (
                  <div key={idx} className={styles.viewItem}>
                    <span className={styles.viewLabel}>File Type {ft.fileTypeNumber}</span>
                    <select
                      className={styles.formSelect}
                      value={ft.fileTypeId}
                      onChange={(e) => handleFileTypeChange(idx, e.target.value)}
                    >
                      <option value="">Select file type</option>
                      {extractionTemplates.map((item) => (
                        <option key={item.fileDetails.file_id} value={item.fileDetails.file_id}>
                          {item.fileDetails.file_name}
                        </option>
                      ))}
                    </select>
                    <div style={{ marginTop: '6px' }}>
                      <span className={styles.viewLabel}>Template {ft.fileTypeNumber}</span>
                      <input
                        className={styles.formInput}
                        type="text"
                        value={templateMappings[idx]?.templateName || ''}
                        readOnly
                        placeholder="Auto-populated from file type"
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Matching Fields */}
          {matchingFields.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>Matching Fields</div>
              <div className={styles.viewGrid}>
                {matchingFields.map((mf, idx) => (
                  <div key={idx} className={styles.viewItem}>
                    <span className={styles.viewLabel}>Field {mf.fieldNumber}</span>
                    <MultiSelectDropdown
                      options={MATCHING_FIELD_OPTIONS}
                      selected={mf.selectedFields || []}
                      onToggle={(fieldName) => handleMatchingFieldToggle(idx, fieldName)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className={styles.formActions}>
            <Button variant="secondary" onClick={handleBack}>
              Cancel
            </Button>
            <Button variant="gold" onClick={handleSubmit} disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 size={16} className={styles.spinner} />
                  {editingProcess ? 'Updating...' : 'Adding...'}
                </>
              ) : (
                <>
                  {editingProcess ? <Pencil size={16} /> : <Plus size={16} />}
                  {editingProcess ? 'Update Process Definition' : 'Add Process Definition'}
                </>
              )}
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ─── View Page ───
  if (viewData) {
    return (
      <div className={styles.page}>
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={styles.header}
        >
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ArrowLeft size={18} />}
            onClick={handleBack}
            className={styles.backBtn}
          >
            Back to Process Definition
          </Button>
          <h1>View Process Definition</h1>
          <p>{viewData.processName}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          {/* General Details */}
          <div className={styles.viewSection}>
            <div className={styles.viewSectionHeader}>General Details</div>
            <div className={styles.viewGrid}>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Process Name</span>
                <span className={styles.viewValue}>{getVal(viewData.processName)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Input Count</span>
                <span className={styles.viewValue}>{getVal(viewData.inputCount)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Tran Channel</span>
                <span className={styles.viewValue}>{getVal(viewData.tranChannel)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Recon Type</span>
                <span className={styles.viewValue}>{getVal(viewData.reconType)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Retention Period (in days)</span>
                <span className={styles.viewValue}>{getVal(viewData.retentionPeriod)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Retention Volume</span>
                <span className={styles.viewValue}>{getVal(viewData.retentionVolume)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Matching Type</span>
                <span className={styles.viewValue}>{MATCHING_TYPE_MAP[viewData.matchingType] || getVal(viewData.matchingType)}</span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>Identical Matching</span>
                <span className={styles.viewValue}>
                  {viewData.identicalMatching === 'Y' ? 'User has enabled same matching fields!' : getVal(viewData.identicalMatching)}
                </span>
              </div>
              <div className={styles.viewItem}>
                <span className={styles.viewLabel}>JPB and RPSL</span>
                <span className={styles.viewValue}>{getVal(viewData.jpsRpsl)}</span>
              </div>
            </div>
          </div>

          {/* File Types */}
          {viewData.fileTypes && viewData.fileTypes.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>File Types</div>
              <div className={styles.viewGrid}>
                {viewData.fileTypes.map((ft, idx) => (
                  <div key={idx} className={styles.viewItem}>
                    <span className={styles.viewLabel}>File Type {ft.fileTypeNumber}</span>
                    <span className={styles.viewValue}>{ft.fileTypeName || ft.dataTableName || '—'}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Templates */}
          {viewData.templates && viewData.templates.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>Templates</div>
              <div className={styles.templateViewGrid}>
                {viewData.templates.map((tpl, idx) => (
                  <div key={idx} className={styles.templateCard}>
                    <div className={styles.templateCardTitle}>Template {tpl.templateNumber}</div>
                    <div className={styles.templateCardRow}>
                      <span className={styles.templateCardLabel}>{tpl.templateName || '—'}</span>
                    </div>
                    <div className={styles.templateCardRow}>
                      <span className={styles.templateCardSub}>{tpl.stagingTableName || '—'}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Matching Fields */}
          {viewData.matchingFields && viewData.matchingFields.length > 0 && (
            <div className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>Matching Fields</div>
              <div className={styles.viewGrid}>
                {viewData.matchingFields.map((mf, idx) => (
                  <div key={idx} className={styles.viewItem}>
                    <span className={styles.viewLabel}>Field {mf.fieldNumber}</span>
                    <div className={styles.fieldTagsContainer}>
                      {(mf.selectedFields || []).map((f, fIdx) => (
                        <span key={fIdx} className={styles.fieldTag}>{f}</span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Back Button */}
          <div className={styles.viewActions}>
            <Button variant="secondary" onClick={handleBack}>
              Back
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ─── List Page ───
  return (
    <div className={styles.page}>
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className={styles.header}
      >
        <h1>Process Definition</h1>
        <p>Manage process definitions for reconciliation</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Card className={styles.tableCard}>
          {/* Controls */}
          <div className={styles.tableControls}>
            <div className={styles.showEntries}>
              <span>Showing {filteredProcesses.length} of {processes.length} entries</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search processes..."
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
              <span>Loading process definitions...</span>
            </div>
          ) : (
            <div className={styles.tableContainer}>
              <table className={styles.mainTable}>
                <thead>
                  <tr>
                    <th className={styles.sortable} onClick={() => handleSort('processName')}>
                      <div className={styles.thContent}>
                        <span>Process Name</span>
                        {getSortIcon('processName')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('inputCount')}>
                      <div className={styles.thContent}>
                        <span>Input Count</span>
                        {getSortIcon('inputCount')}
                      </div>
                    </th>
                    <th className={styles.sortable} onClick={() => handleSort('reconType')}>
                      <div className={styles.thContent}>
                        <span>Recon Type</span>
                        {getSortIcon('reconType')}
                      </div>
                    </th>
                    <th className={styles.actionCol}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedProcesses.length === 0 ? (
                    <tr>
                      <td colSpan={4} className={styles.emptyCell}>
                        No process definitions found
                      </td>
                    </tr>
                  ) : (
                    sortedProcesses.map((process) => (
                      <tr key={process.processId}>
                        <td>{process.processName || '—'}</td>
                        <td>{process.inputCount ?? '—'}</td>
                        <td>{process.reconType || '—'}</td>
                        <td className={styles.actionCol}>
                          <div className={styles.actionBtns}>
                            <button
                              className={styles.iconBtn}
                              title="Edit"
                              onClick={() => handleEdit(process)}
                            >
                              <Pencil size={15} />
                            </button>
                            <button
                              className={`${styles.iconBtn} ${styles.iconBtnView}`}
                              title="View"
                              onClick={() => handleView(process)}
                            >
                              <Eye size={15} />
                            </button>
                            <button
                              className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                              title="Delete"
                              onClick={() => handleDelete(process)}
                            >
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

// ─── Multi-Select Dropdown with Checkboxes ───
const MultiSelectDropdown = ({ options, selected, onToggle }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className={styles.multiSelect} ref={ref}>
      <div className={styles.multiSelectTrigger} onClick={() => setOpen(!open)}>
        <div className={styles.multiSelectValue}>
          {selected.length === 0 ? (
            <span className={styles.multiSelectPlaceholder}>Select fields</span>
          ) : (
            <div className={styles.multiSelectTags}>
              {selected.map((s) => (
                <span key={s} className={styles.multiSelectTag}>
                  {s}
                  <X
                    size={12}
                    className={styles.multiSelectTagRemove}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggle(s);
                    }}
                  />
                </span>
              ))}
            </div>
          )}
        </div>
        <ChevronDown size={16} className={`${styles.multiSelectArrow} ${open ? styles.multiSelectArrowOpen : ''}`} />
      </div>
      {open && (
        <div className={styles.multiSelectDropdown}>
          {options.map((opt) => (
            <label key={opt} className={styles.multiSelectOption}>
              <input
                type="checkbox"
                checked={selected.includes(opt)}
                onChange={() => onToggle(opt)}
                className={styles.multiSelectCheckbox}
              />
              <span>{opt}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProcessDefinition;
