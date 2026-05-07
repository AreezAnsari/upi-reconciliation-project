import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Plus, Pencil, Eye, ArrowLeft, Loader2, Search, ArrowUpDown, ArrowUp, ArrowDown, Trash2, Save, X } from 'lucide-react';
import { Button, Card } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './TemplateConfig.module.css';

const EMPTY_FIELD = {
  columnPosition: '',
  fieldName: '',
  fieldtype: '',
  fieldFormat: 'N/A',
  fieldLength: '',
  fromPosition: '',
  toPosition: '',
  keyIdentity: 'N',
  columnOffset: '0',
  qualifier: 'NONE',
};

const INITIAL_FORM = {
  templateType: 'CSV',
  templateName: '',
  columnCount: '',
  reversalIndicator: 'N',
  dataReference: 'N',
  onlineRefund: 'N',
  fieldDetails: [{ ...EMPTY_FIELD, columnPosition: 1 }],
};

const TemplateConfig = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewData, setViewData] = useState(null);

  // Form state
  const [formMode, setFormMode] = useState(null); // null | 'add' | 'edit'
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [editTemplateId, setEditTemplateId] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Dynamic field options
  const [fieldTypes, setFieldTypes] = useState([]);
  const [fieldFormats, setFieldFormats] = useState([]);

  // Track invalid field name input attempts per row
  const [fieldNameHints, setFieldNameHints] = useState({});

  // Table controls
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Pagination metadata from API
  const [pageMetadata, setPageMetadata] = useState(null);

  useEffect(() => {
    if (token) {
      fetchTemplates();
      fetchFieldTypes();
      fetchFieldFormats();
    }
  }, [token, currentPage, pageSize]);

  const fetchFieldTypes = async () => {
    try {
      const response = await authAPI.getFieldTypes(token);
      if (response.status === 'SUCCESS') {
        setFieldTypes(response.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch field types:', error);
    }
  };

  const fetchFieldFormats = async () => {
    try {
      const response = await authAPI.getReconFieldFormats(token);
      if (response.status === 'SUCCESS') {
        setFieldFormats(response.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch field formats:', error);
    }
  };

  const fetchTemplates = async () => {
    setLoading(true);
    try {
      const response = await authAPI.viewTemplates(currentPage, pageSize, token);
      if (response.status === 'SUCCESS') {
        setTemplates(response.data || []);
        setPageMetadata(response.pageMetadata || null);
      } else {
        addNotification({
          type: 'error',
          title: 'Error',
          message: response.statusMsg || 'Failed to fetch templates.',
        });
      }
    } catch (error) {
      console.error('Failed to fetch templates:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to fetch templates. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  // Filter by search term
  const filteredTemplates = useMemo(() => {
    if (!searchTerm) return templates;
    const term = searchTerm.toLowerCase();
    return templates.filter(
      (t) =>
        t.templateName?.toLowerCase().includes(term) ||
        t.productType?.toLowerCase().includes(term)
    );
  }, [templates, searchTerm]);

  // Sort
  const sortedTemplates = useMemo(() => {
    if (!sortConfig.key) return filteredTemplates;
    return [...filteredTemplates].sort((a, b) => {
      const aVal = a[sortConfig.key] || '';
      const bVal = b[sortConfig.key] || '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredTemplates, sortConfig]);

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

  const totalPages = pageMetadata?.totalPages || 1;
  const totalElements = pageMetadata?.totalElements || templates.length;

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setCurrentPage(1);
  };

  const handleView = (template) => {
    setViewData(template);
  };

  const handleBack = () => {
    setViewData(null);
    setFormMode(null);
    setFormData(INITIAL_FORM);
    setEditTemplateId(null);
  };

  // ─── Form Handlers ───
  const handleAdd = () => {
    setFormMode('add');
    setFormData(INITIAL_FORM);
    setEditTemplateId(null);
  };

  const handleEdit = (template) => {
    setFormMode('edit');
    setEditTemplateId(template.reconTemplateId);
    // Map API response fields to form fields
    const fields = (template.fieldDetails || [])
      .sort((a, b) => a.reconColumnPosn - b.reconColumnPosn)
      .map((f) => ({
        columnPosition: f.reconColumnPosn || '',
        fieldName: f.reconShortName || '',
        fieldtype: f.fieldTypeName || 'VARCHAR2',
        fieldFormat: f.fieldFormatName || 'N/A',
        fieldLength: f.reconMaxLength || '',
        fromPosition: f.reconFromPosn || '',
        toPosition: f.reconToPosn || '',
        keyIdentity: f.reconKeyIdentifier === 1 ? 'Y' : 'N',
        columnOffset: '0',
        qualifier: 'NONE',
      }));

    setFormData({
      templateType: template.productType || 'CSV',
      templateName: template.templateName || '',
      columnCount: fields.length || '',
      reversalIndicator: template.settlementFlag || 'N',
      dataReference: template.dataTableInd || 'N',
      onlineRefund: 'N',
      fieldDetails: fields.length > 0 ? fields : [{ ...EMPTY_FIELD, columnPosition: 1 }],
    });
  };

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleFieldChange = (index, field, value) => {
    setFormData((prev) => {
      const updated = [...prev.fieldDetails];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, fieldDetails: updated };
    });
  };

  const addFieldRow = () => {
    setFormData((prev) => ({
      ...prev,
      fieldDetails: [
        ...prev.fieldDetails,
        { ...EMPTY_FIELD, columnPosition: prev.fieldDetails.length + 1 },
      ],
      columnCount: prev.fieldDetails.length + 1,
    }));
  };

  const removeFieldRow = (index) => {
    setFormData((prev) => {
      if (prev.fieldDetails.length <= 1) return prev;
      const updated = prev.fieldDetails
        .filter((_, i) => i !== index)
        .map((f, i) => ({ ...f, columnPosition: i + 1 }));
      return { ...prev, fieldDetails: updated, columnCount: updated.length };
    });
  };

  const handleSubmit = async () => {
    // Validation
    if (!formData.templateName.trim()) {
      addNotification({ type: 'error', title: 'Validation Error', message: 'Template Name is required.' });
      return;
    }
    if (!formData.fieldDetails.length) {
      addNotification({ type: 'error', title: 'Validation Error', message: 'At least one field is required.' });
      return;
    }
    for (let i = 0; i < formData.fieldDetails.length; i++) {
      if (!formData.fieldDetails[i].fieldName.trim()) {
        addNotification({ type: 'error', title: 'Validation Error', message: `Field Name is required for row ${i + 1}.` });
        return;
      }
      if (!/^[a-zA-Z0-9_]+$/.test(formData.fieldDetails[i].fieldName)) {
        addNotification({ type: 'error', title: 'Validation Error', message: `Field Name for row ${i + 1} must be alphanumeric with only underscore (_) allowed. No spaces or special characters.` });
        return;
      }
      if (!formData.fieldDetails[i].fieldLength || Number(formData.fieldDetails[i].fieldLength) < 1) {
        addNotification({ type: 'error', title: 'Validation Error', message: `Field Length must be at least 1 for row ${i + 1}.` });
        return;
      }
    }

    const payload = {
      templateType: formData.templateType,
      templateName: formData.templateName,
      columnCount: Number(formData.columnCount) || formData.fieldDetails.length,
      reversalIndicator: formData.reversalIndicator,
      dataReference: formData.dataReference,
      onlineRefund: formData.onlineRefund,
      fieldDetails: formData.fieldDetails.map((f) => ({
        columnPosition: Number(f.columnPosition),
        fieldName: f.fieldName,
        fieldtype: f.fieldtype,
        fieldFormat: f.fieldFormat,
        fieldLength: Number(f.fieldLength) || 0,
        fromPosition: Number(f.fromPosition) || 0,
        toPosition: Number(f.toPosition) || 0,
        keyIdentity: f.keyIdentity,
        columnOffset: f.columnOffset,
        qualifier: f.qualifier,
      })),
    };

    setSubmitting(true);
    try {
      let response;
      if (formMode === 'add') {
        response = await authAPI.createTemplate(payload, token);
      } else {
        response = await authAPI.updateTemplate(editTemplateId, payload, token);
      }

      if (response.status === 'SUCCESS' || response.status === 'success') {
        addNotification({
          type: 'success',
          title: 'Success',
          message: formMode === 'add' ? 'Template created successfully.' : 'Template updated successfully.',
        });
        handleBack();
        fetchTemplates();
      } else {
        addNotification({
          type: 'error',
          title: 'Error',
          message: response.statusMsg || `Failed to ${formMode === 'add' ? 'create' : 'update'} template.`,
        });
      }
    } catch (error) {
      console.error(`Failed to ${formMode} template:`, error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: `Failed to ${formMode === 'add' ? 'create' : 'update'} template. Please try again.`,
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Build page number buttons
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);
    if (end - start < maxVisible - 1) {
      start = Math.max(1, end - maxVisible + 1);
    }
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  };

  const startEntry = (currentPage - 1) * pageSize + 1;
  const endEntry = Math.min(currentPage * pageSize, totalElements);

  // ─── Form Page (Add / Edit) ───
  if (formMode) {
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
            Back to Template Config
          </Button>
          <h1>{formMode === 'add' ? 'Add New Template' : 'Edit Template'}</h1>
          <p>{formMode === 'add' ? 'Configure a new reconciliation template' : `Editing: ${formData.templateName}`}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <Card className={styles.viewCard}>
            {/* Template Metadata Fields */}
            <div className={styles.formGrid}>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Template Type</label>
                <select
                  className={styles.formSelect}
                  value={formData.templateType}
                  onChange={(e) => handleFormChange('templateType', e.target.value)}
                >
                  <option value="CSV">CSV</option>
                  <option value="FIXED">FIXED</option>
                  <option value="XML">XML</option>
                </select>
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Template Name <span className={styles.required}>*</span></label>
                <input
                  type="text"
                  className={styles.formInput}
                  value={formData.templateName}
                  onChange={(e) => handleFormChange('templateName', e.target.value)}
                  placeholder="e.g. DEBIT_CARD"
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Column Count</label>
                <input
                  type="number"
                  className={styles.formInput}
                  value={formData.columnCount}
                  onChange={(e) => handleFormChange('columnCount', e.target.value)}
                  placeholder="Auto-calculated"
                  readOnly
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Reversal Indicator</label>
                <select
                  className={styles.formSelect}
                  value={formData.reversalIndicator}
                  onChange={(e) => handleFormChange('reversalIndicator', e.target.value)}
                >
                  <option value="Y">Yes</option>
                  <option value="N">No</option>
                </select>
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Data Reference</label>
                <select
                  className={styles.formSelect}
                  value={formData.dataReference}
                  onChange={(e) => handleFormChange('dataReference', e.target.value)}
                >
                  <option value="Y">Yes</option>
                  <option value="N">No</option>
                </select>
              </div>
              <div className={styles.formGroup}>
                <label className={styles.formLabel}>Online Refund</label>
                <select
                  className={styles.formSelect}
                  value={formData.onlineRefund}
                  onChange={(e) => handleFormChange('onlineRefund', e.target.value)}
                >
                  <option value="Y">Yes</option>
                  <option value="N">No</option>
                </select>
              </div>
            </div>

            {/* Field Details Section */}
            <div className={styles.fieldSection}>
              <div className={styles.fieldSectionHeader}>
                <h3>Field Details</h3>
                <Button
                  variant="gold"
                  size="sm"
                  leftIcon={<Plus size={14} />}
                  onClick={addFieldRow}
                >
                  Add Field
                </Button>
              </div>

              <div className={styles.fieldTableWrapper}>
                <table className={styles.fieldTable}>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Field Name *</th>
                      <th>Field Type</th>
                      <th>Field Format</th>
                      <th>Field Length</th>
                      <th>From Position</th>
                      <th>To Position</th>
                      <th>Key Identity</th>
                      <th>Column Offset</th>
                      <th>Qualifier</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {formData.fieldDetails.map((field, idx) => (
                      <tr key={idx}>
                        <td className={styles.fieldIdx}>{idx + 1}</td>
                        <td>
                          <div className={styles.fieldNameWrapper}>
                            <input
                              type="text"
                              className={`${styles.fieldInput} ${fieldNameHints[idx] ? styles.fieldInputError : ''}`}
                              value={field.fieldName}
                              onChange={(e) => {
                                const val = e.target.value;
                                if (val === '' || /^[a-zA-Z0-9_]+$/.test(val)) {
                                  handleFieldChange(idx, 'fieldName', val);
                                  setFieldNameHints((prev) => ({ ...prev, [idx]: false }));
                                } else {
                                  setFieldNameHints((prev) => ({ ...prev, [idx]: true }));
                                  setTimeout(() => setFieldNameHints((prev) => ({ ...prev, [idx]: false })), 2000);
                                }
                              }}
                              placeholder="FIELD_NAME"
                              title="Only alphanumeric characters and underscore (_) allowed"
                            />
                            {fieldNameHints[idx] && (
                              <span className={styles.fieldHint}>Only A-Z, 0-9 and _ allowed</span>
                            )}
                          </div>
                        </td>
                        <td>
                          <select
                            className={styles.fieldSelect}
                            value={field.fieldtype}
                            onChange={(e) => handleFieldChange(idx, 'fieldtype', e.target.value)}
                          >
                            <option value="">Select</option>
                            {fieldTypes.map((ft) => (
                              <option key={ft.fieldTypeId} value={ft.fieldTypeDes}>
                                {ft.fieldTypeDes}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <select
                            className={styles.fieldSelect}
                            value={field.fieldFormat}
                            onChange={(e) => handleFieldChange(idx, 'fieldFormat', e.target.value)}
                          >
                            <option value="N/A">N/A</option>
                            {fieldFormats.map((ff) => (
                              <option key={ff.reconFieldFormatId} value={ff.reconFieldFormatDesc}>
                                {ff.reconFieldFormatDesc}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <input
                            type="number"
                            className={styles.fieldInputSmall}
                            value={field.fieldLength}
                            onChange={(e) => handleFieldChange(idx, 'fieldLength', e.target.value)}
                            min="1"
                            placeholder="1"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            className={styles.fieldInputSmall}
                            value={field.fromPosition}
                            onChange={(e) => handleFieldChange(idx, 'fromPosition', e.target.value)}
                            placeholder="0"
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            className={styles.fieldInputSmall}
                            value={field.toPosition}
                            onChange={(e) => handleFieldChange(idx, 'toPosition', e.target.value)}
                            placeholder="0"
                          />
                        </td>
                        <td>
                          <select
                            className={styles.fieldSelect}
                            value={field.keyIdentity}
                            onChange={(e) => handleFieldChange(idx, 'keyIdentity', e.target.value)}
                          >
                            <option value="Y">Yes</option>
                            <option value="N">No</option>
                          </select>
                        </td>
                        <td>
                          <input
                            type="text"
                            className={styles.fieldInputSmall}
                            value={field.columnOffset}
                            onChange={(e) => handleFieldChange(idx, 'columnOffset', e.target.value)}
                            placeholder="0"
                          />
                        </td>
                        <td>
                          <select
                            className={styles.fieldSelect}
                            value={field.qualifier}
                            onChange={(e) => handleFieldChange(idx, 'qualifier', e.target.value)}
                          >
                            <option value="NONE">NONE</option>
                            <option value="DOUBLE_QUOTE">DOUBLE_QUOTE</option>
                            <option value="SINGLE_QUOTE">SINGLE_QUOTE</option>
                          </select>
                        </td>
                        <td>
                          <button
                            className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                            title="Remove field"
                            onClick={() => removeFieldRow(idx)}
                            disabled={formData.fieldDetails.length <= 1}
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Form Actions */}
            <div className={styles.formActions}>
              <Button
                variant="ghost"
                size="md"
                leftIcon={<X size={16} />}
                onClick={handleBack}
              >
                Cancel
              </Button>
              <Button
                variant="gold"
                size="md"
                leftIcon={submitting ? <Loader2 size={16} className={styles.spinner} /> : <Save size={16} />}
                onClick={handleSubmit}
                disabled={submitting}
              >
                {submitting ? 'Saving...' : formMode === 'add' ? 'Create Template' : 'Update Template'}
              </Button>
            </div>
          </Card>
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
            Back to Template Config
          </Button>
          <h1>View Template</h1>
          <p>{viewData.templateName}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <Card className={styles.viewCard}>
            {/* Template Details */}
            <div className={styles.detailsGrid}>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Template Type</label>
                <div className={styles.detailValue}>
                  {viewData.productType || viewData.stageTabName || '—'}
                </div>
              </div>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Template Name</label>
                <div className={styles.detailValue}>{viewData.templateName}</div>
              </div>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Column Count</label>
                <div className={styles.detailValue}>
                  {viewData.fieldDetails?.length || 0}
                </div>
              </div>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Stage Table Name</label>
                <div className={styles.detailValue}>{viewData.stageTabName || '—'}</div>
              </div>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Data Table Indicator</label>
                <div className={styles.detailValue}>{viewData.dataTableInd || '—'}</div>
              </div>
              <div className={styles.detailItem}>
                <label className={styles.detailLabel}>Settlement Flag</label>
                <div className={styles.detailValue}>{viewData.settlementFlag || 'N'}</div>
              </div>
            </div>

            {/* Field Details Table */}
            {viewData.fieldDetails && viewData.fieldDetails.length > 0 && (
              <div className={styles.fieldTableWrapper}>
                <table className={styles.fieldTable}>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Field Name</th>
                      <th>Field Type</th>
                      <th>Field Format</th>
                      <th>Field Length</th>
                      <th>From Position</th>
                      <th>To Position</th>
                      <th>KeyIdentity</th>
                      <th>Column/Offset</th>
                      <th>Qualifier</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...viewData.fieldDetails]
                      .sort((a, b) => a.reconColumnPosn - b.reconColumnPosn)
                      .map((field, idx) => (
                        <tr key={field.reconFieldId || idx}>
                          <td className={styles.fieldIdx}>{idx + 1}</td>
                          <td>
                            <span className={styles.fieldValue}>{field.reconShortName || '—'}</span>
                          </td>
                          <td>
                            <span className={styles.fieldBadge}>{field.fieldTypeName || '—'}</span>
                          </td>
                          <td>
                            <span className={styles.fieldBadgeSecondary}>{field.fieldFormatName || 'N/A'}</span>
                          </td>
                          <td>{field.reconMaxLength || '—'}</td>
                          <td>{field.reconFromPosn || '—'}</td>
                          <td>{field.reconToPosn || '—'}</td>
                          <td>
                            <span className={styles.fieldBadgeSecondary}>
                              {field.reconKeyIdentifier === 1 ? 'Primary' :
                               field.reconKeyIdentifier === 2 ? 'Secondary' :
                               field.reconKeyIdentifier === 3 ? 'Tertiary' : 'None'}
                            </span>
                          </td>
                          <td>{field.reconColumnPosn || '—'}</td>
                          <td>
                            <span className={styles.fieldBadgeSecondary}>None</span>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
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
        <h1>Template Config</h1>
        <p>Manage reconciliation template configurations</p>
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
              <span>Show</span>
              <select value={pageSize} onChange={handlePageSizeChange}>
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
              <span>entries</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search templates..."
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
              <span>Loading templates...</span>
            </div>
          ) : (
            <>
              <div className={styles.tableContainer}>
                <table className={styles.templateTable}>
                  <thead>
                    <tr>
                      <th className={styles.sortable} onClick={() => handleSort('templateName')}>
                        <div className={styles.thContent}>
                          <span>Template Name</span>
                          {getSortIcon('templateName')}
                        </div>
                      </th>
                      <th className={styles.sortable} onClick={() => handleSort('productType')}>
                        <div className={styles.thContent}>
                          <span>Template Type</span>
                          {getSortIcon('productType')}
                        </div>
                      </th>
                      <th className={styles.actionCol}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedTemplates.length === 0 ? (
                      <tr>
                        <td colSpan={3} className={styles.emptyCell}>
                          No templates found
                        </td>
                      </tr>
                    ) : (
                      sortedTemplates.map((template) => (
                        <tr key={template.reconTemplateId}>
                          <td>{template.templateName}</td>
                          <td>{template.productType || template.stageTabName || '—'}</td>
                          <td className={styles.actionCol}>
                            <div className={styles.actionBtns}>
                              <button
                                className={styles.iconBtn}
                                title="Edit"
                                onClick={() => handleEdit(template)}
                              >
                                <Pencil size={15} />
                              </button>
                              <button
                                className={`${styles.iconBtn} ${styles.iconBtnView}`}
                                title="View"
                                onClick={() => handleView(template)}
                              >
                                <Eye size={15} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className={styles.pagination}>
                <span className={styles.pageInfo}>
                  Showing {startEntry} to {endEntry} of {totalElements} entries
                </span>
                <div className={styles.pageControls}>
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    Previous
                  </button>
                  {getPageNumbers().map((num) => (
                    <button
                      key={num}
                      className={`${styles.pageNumber} ${currentPage === num ? styles.active : ''}`}
                      onClick={() => handlePageChange(num)}
                    >
                      {num}
                    </button>
                  ))}
                  {totalPages > 5 && currentPage < totalPages - 2 && (
                    <>
                      <span className={styles.ellipsis}>...</span>
                      <button
                        className={styles.pageNumber}
                        onClick={() => handlePageChange(totalPages)}
                      >
                        {totalPages}
                      </button>
                    </>
                  )}
                  <button
                    className={styles.pageButton}
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          )}
        </Card>
      </motion.div>
    </div>
  );
};

export default TemplateConfig;
