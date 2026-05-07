import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Plus, Pencil, Eye, Trash2, ArrowLeft, ArrowRight, Loader2, Search, ArrowUpDown, ArrowUp, ArrowDown, Check } from 'lucide-react';
import { Button, Card, Input, Select, Checkbox, Stepper } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './FileConfig.module.css';

const STEPS = [
  { id: 'file-details', label: 'File Details' },
  { id: 'header-record', label: 'Header Record' },
  { id: 'data-record', label: 'Data Record' },
  { id: 'footer-record', label: 'Footer Record' },
  { id: 'summary', label: 'Summary' },
];

const INITIAL_FORM = {
  // File Details
  rtdTemplateId: '',
  rfdFileName: '',
  rfdFileDescription: '',
  rfdNameConvFormat: '',
  rfdFileDestPath: '',
  rfdFileDupChkFlag: 'N',
  rfdGlFlag: 'N',
  rfdDependentFileId: false,
  rfdSettleFlg: false,
  rfdFileType: '',
  rfdFileDelimiter: '',
  rfdFilenameLength: '',
  // Header Record
  rfdHdrAvlFlag: 'N',
  rfdHdrBlockSize: '',
  rfdHdrId: '',
  rfdHdrKeyCount: '',
  rfdHdrWithDr: 'N',
  // Data Record
  rfdDrBlockSize: '',
  rfdDrBlockSizeFlag: 'N',
  rfdDrFormat: '',
  rfdDridentifierFlag: 'N',
  rfdMultiDrCheck: 'N',
  rfdMultiDrCount: '',
  // Footer Record
  rfdFtrAvailFlag: 'N',
  rfdFtrBeginConstVal: '',
  rfdFtrLength: '',
  rfdFtrType: '',
  rfdFtrCtrlTagCnt: '',
};

const FILE_TYPE_OPTIONS = [
  { value: 'DELIMITER', label: 'DELIMITER' },
  { value: 'FIXED', label: 'FIXED' },
  { value: 'CSV', label: 'CSV' },
];

const DELIMITER_OPTIONS = [
  { value: '|', label: 'Pipe ( | )' },
  { value: ',', label: 'Comma ( , )' },
  { value: '\t', label: 'Tab' },
];

const YES_NO_OPTIONS = [
  { value: 'Y', label: 'YES' },
  { value: 'N', label: 'NO' },
];

const DR_FORMAT_OPTIONS = [
  { value: 'DELIMITED', label: 'DELIMITED' },
  { value: 'FIXED', label: 'FIXED' },
  { value: 'XML', label: 'XML' },
];

const FTR_TYPE_OPTIONS = [
  { value: 'FIXED', label: 'FIXED' },
  { value: 'DELIMITED', label: 'DELIMITED' },
];

const SUMMARY_SECTIONS = [
  {
    title: 'File Details',
    fields: [
      { key: 'rtdTemplateId', label: 'Template Name', type: 'template' },
      { key: 'rfdFileName', label: 'File Name' },
      { key: 'rfdFileDescription', label: 'File Description' },
      { key: 'rfdNameConvFormat', label: 'File Name Pattern' },
      { key: 'rfdFileDestPath', label: 'File Path' },
      { key: 'rfdFileDupChkFlag', label: 'Duplicate File Name' },
      { key: 'rfdGlFlag', label: 'GL Flag' },
      { key: 'rfdDependentFileId', label: 'Dependency', type: 'bool' },
      { key: 'rfdSettleFlg', label: 'Settlement Required', type: 'bool' },
      { key: 'rfdFileType', label: 'File Type' },
      { key: 'rfdFileDelimiter', label: 'Delimiter' },
      { key: 'rfdFilenameLength', label: 'File Name Max. Length' },
    ],
  },
  {
    title: 'Header Record',
    fields: [
      { key: 'rfdHdrAvlFlag', label: 'Header Available' },
      { key: 'rfdHdrBlockSize', label: 'Header Block Size' },
      { key: 'rfdHdrId', label: 'Header ID' },
      { key: 'rfdHdrKeyCount', label: 'Header Key Count' },
      { key: 'rfdHdrWithDr', label: 'Header With Data Record' },
    ],
  },
  {
    title: 'Data Record',
    fields: [
      { key: 'rfdDrBlockSize', label: 'DR Block Size' },
      { key: 'rfdDrBlockSizeFlag', label: 'DR Block Size Flag' },
      { key: 'rfdDrFormat', label: 'DR Format' },
      { key: 'rfdDridentifierFlag', label: 'DR Identifier Flag' },
      { key: 'rfdMultiDrCheck', label: 'Multi DR Check' },
      { key: 'rfdMultiDrCount', label: 'Multi DR Count' },
    ],
  },
  {
    title: 'Footer Record',
    fields: [
      { key: 'rfdFtrAvailFlag', label: 'Footer Available' },
      { key: 'rfdFtrBeginConstVal', label: 'Footer Begin Const Value' },
      { key: 'rfdFtrLength', label: 'Footer Length' },
      { key: 'rfdFtrType', label: 'Footer Type' },
      { key: 'rfdFtrCtrlTagCnt', label: 'Footer Ctrl Tag Count' },
    ],
  },
];

const VIEW_SECTIONS = [
  {
    title: 'File Details',
    fields: [
      { key: 'templateName', label: 'Template Name' },
      { key: 'rfdFileName', label: 'File Name' },
      { key: 'rfdFileDescription', label: 'File Description' },
      { key: 'rfdNameConvFormat', label: 'File Name Pattern' },
      { key: 'rfdFileLocation', label: 'File Location' },
      { key: 'rfdFileDestPath', label: 'File Destination Path' },
      { key: 'rfdFileDupChkFlag', label: 'Duplicate File Name' },
      { key: 'rfdGlFlag', label: 'GL Flag' },
      { key: 'rfdDependentFileId', label: 'Dependency' },
      { key: 'rfdSettleFlg', label: 'Settlement Required' },
      { key: 'rfdFileType', label: 'File Type' },
      { key: 'rfdFileDelimiter', label: 'Delimiter' },
      { key: 'rfdFilenameLength', label: 'File Name Max. Length' },
      { key: 'rfdFileDefineConst', label: 'File Define Constant' },
      { key: 'rfdShortName', label: 'Short Name' },
      { key: 'rfdJpslRpsl', label: 'JPSL / RPSL' },
    ],
  },
  {
    title: 'Header Record',
    fields: [
      { key: 'rfdHdrAvlFlag', label: 'Header Available' },
      { key: 'rfdHdrBlockSize', label: 'Header Block Size' },
      { key: 'rfdHdrId', label: 'Header ID' },
      { key: 'rfdHdrKeyCount', label: 'Header Key Count' },
      { key: 'rfdHdrWithDr', label: 'Header With Data Record' },
    ],
  },
  {
    title: 'Data Record',
    fields: [
      { key: 'rfdDrBlockSize', label: 'DR Block Size' },
      { key: 'rfdDrBlockSizeFlag', label: 'DR Block Size Flag' },
      { key: 'rfdDrFormat', label: 'DR Format' },
      { key: 'rfdDridentifierFlag', label: 'DR Identifier Flag' },
      { key: 'rfdMultiDrCheck', label: 'Multi DR Check' },
      { key: 'rfdMultiDrCount', label: 'Multi DR Count' },
    ],
  },
  {
    title: 'Footer Record',
    fields: [
      { key: 'rfdFtrAvailFlag', label: 'Footer Available' },
      { key: 'rfdFtrBeginConstVal', label: 'Footer Begin Const Value' },
      { key: 'rfdFtrLength', label: 'Footer Length' },
      { key: 'rfdFtrType', label: 'Footer Type' },
      { key: 'rfdFtrCtrlTagCnt', label: 'Footer Ctrl Tag Count' },
    ],
  },
];

const FileConfig = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();

  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewData, setViewData] = useState(null);
  const [mode, setMode] = useState('list'); // list | add | edit | view
  const [editFileId, setEditFileId] = useState(null);

  // Stepper state
  const [currentStep, setCurrentStep] = useState('file-details');
  const [formData, setFormData] = useState({ ...INITIAL_FORM });
  const [submitting, setSubmitting] = useState(false);
  const [templates, setTemplates] = useState([]);

  // Table controls
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Pagination metadata from API
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    if (token) {
      fetchFiles();
    }
  }, [token, currentPage, pageSize]);

  const fetchFiles = async () => {
    setLoading(true);
    try {
      const response = await authAPI.getFileConfigurations(currentPage, pageSize, token);
      setFiles(response.content || []);
      setTotalPages(response.totalPages || 1);
      setTotalElements(response.totalElements || 0);
    } catch (error) {
      console.error('Failed to fetch file configurations:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to fetch file configurations. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  // Filter by search term
  const filteredFiles = useMemo(() => {
    if (!searchTerm) return files;
    const term = searchTerm.toLowerCase();
    return files.filter(
      (f) =>
        f.rfdFileName?.toLowerCase().includes(term) ||
        f.rfdFileLocation?.toLowerCase().includes(term) ||
        f.templateName?.toLowerCase().includes(term)
    );
  }, [files, searchTerm]);

  // Sort
  const sortedFiles = useMemo(() => {
    if (!sortConfig.key) return filteredFiles;
    return [...filteredFiles].sort((a, b) => {
      const aVal = a[sortConfig.key] || '';
      const bVal = b[sortConfig.key] || '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredFiles, sortConfig]);

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

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setCurrentPage(1);
  };

  const handleView = (file) => {
    setViewData(file);
    setMode('view');
  };

  const handleBack = () => {
    setViewData(null);
    setEditFileId(null);
    setMode('list');
    setCurrentStep('file-details');
    setFormData({ ...INITIAL_FORM });
  };

  const handleDelete = (file) => {
    addNotification({
      type: 'info',
      title: 'Delete',
      message: `Delete action for "${file.rfdFileName}" is not yet implemented.`,
    });
  };

  const handleAddClick = async () => {
    setMode('add');
    setCurrentStep('file-details');
    setFormData({ ...INITIAL_FORM });
    try {
      const data = await authAPI.getFileTemplates(token);
      setTemplates(data || []);
    } catch (error) {
      console.error('Failed to fetch templates:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to fetch templates.',
      });
    }
  };

  const handleEditClick = async (file) => {
    setMode('edit');
    setEditFileId(file.rfdFileId);
    setCurrentStep('file-details');
    setFormData({
      rtdTemplateId: file.rtdTemplateId ? String(file.rtdTemplateId) : '',
      rfdFileName: file.rfdFileName || '',
      rfdFileDescription: file.rfdFileDescription || '',
      rfdNameConvFormat: file.rfdNameConvFormat || '',
      rfdFileDestPath: file.rfdFileDestPath || '',
      rfdFileDupChkFlag: file.rfdFileDupChkFlag || 'N',
      rfdGlFlag: file.rfdGlFlag || 'N',
      rfdDependentFileId: !!file.rfdDependentFileId,
      rfdSettleFlg: file.rfdSettleFlg === 'Y',
      rfdFileType: file.rfdFileType || '',
      rfdFileDelimiter: file.rfdFileDelimiter || '',
      rfdFilenameLength: file.rfdFilenameLength != null ? String(file.rfdFilenameLength) : '',
      rfdHdrAvlFlag: file.rfdHdrAvlFlag || 'N',
      rfdHdrBlockSize: file.rfdHdrBlockSize != null ? String(file.rfdHdrBlockSize) : '',
      rfdHdrId: file.rfdHdrId != null ? String(file.rfdHdrId) : '',
      rfdHdrKeyCount: file.rfdHdrKeyCount != null ? String(file.rfdHdrKeyCount) : '',
      rfdHdrWithDr: file.rfdHdrWithDr || 'N',
      rfdDrBlockSize: file.rfdDrBlockSize != null ? String(file.rfdDrBlockSize) : '',
      rfdDrBlockSizeFlag: file.rfdDrBlockSizeFlag || 'N',
      rfdDrFormat: file.rfdDrFormat || '',
      rfdDridentifierFlag: file.rfdDridentifierFlag || 'N',
      rfdMultiDrCheck: file.rfdMultiDrCheck || 'N',
      rfdMultiDrCount: file.rfdMultiDrCount != null ? String(file.rfdMultiDrCount) : '',
      rfdFtrAvailFlag: file.rfdFtrAvailFlag || 'N',
      rfdFtrBeginConstVal: file.rfdFtrBeginConstVal || '',
      rfdFtrLength: file.rfdFtrLength != null ? String(file.rfdFtrLength) : '',
      rfdFtrType: file.rfdFtrType || '',
      rfdFtrCtrlTagCnt: file.rfdFtrCtrlTagCnt != null ? String(file.rfdFtrCtrlTagCnt) : '',
    });
    try {
      const data = await authAPI.getFileTemplates(token);
      setTemplates(data || []);
    } catch (error) {
      console.error('Failed to fetch templates:', error);
    }
  };

  // Form handlers
  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const currentStepIndex = STEPS.findIndex((s) => s.id === currentStep);

  const handleNext = () => {
    if (currentStepIndex < STEPS.length - 1) {
      setCurrentStep(STEPS[currentStepIndex + 1].id);
    }
  };

  const handlePrev = () => {
    if (currentStepIndex > 0) {
      setCurrentStep(STEPS[currentStepIndex - 1].id);
    }
  };

  const handleStepClick = (stepId) => {
    const targetIndex = STEPS.findIndex((s) => s.id === stepId);
    if (mode === 'edit' || targetIndex <= currentStepIndex) {
      setCurrentStep(stepId);
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const payload = {
        rtdTemplateId: formData.rtdTemplateId ? Number(formData.rtdTemplateId) : null,
        processMastId: 1,
        rfdFileName: formData.rfdFileName,
        rfdShortName: '',
        rfdFileDescription: formData.rfdFileDescription,
        rfdFileType: formData.rfdFileType,
        rfdFileLocation: '',
        rfdFileDelimiter: formData.rfdFileDelimiter,
        rfdFileDestPath: formData.rfdFileDestPath,
        rfdFileDupChkFlag: formData.rfdFileDupChkFlag,
        rfdGlFlag: formData.rfdGlFlag,
        rfdFileDefineConst: '',
        rfdFilenameLength: formData.rfdFilenameLength ? Number(formData.rfdFilenameLength) : null,
        rfdNameConvFormat: formData.rfdNameConvFormat,
        rfdDependentFileId: formData.rfdDependentFileId ? 1 : null,
        fileUpdateFlag: 'N',
        rfdHdrAvlFlag: formData.rfdHdrAvlFlag,
        rfdHdrBlockSize: formData.rfdHdrBlockSize ? Number(formData.rfdHdrBlockSize) : null,
        rfdHdrId: formData.rfdHdrId ? Number(formData.rfdHdrId) : null,
        rfdHdrKeyCount: formData.rfdHdrKeyCount ? Number(formData.rfdHdrKeyCount) : null,
        rfdHdrWithDr: formData.rfdHdrWithDr,
        rfdFtrAvailFlag: formData.rfdFtrAvailFlag,
        rfdFtrBeginConstVal: formData.rfdFtrBeginConstVal,
        rfdFtrLength: formData.rfdFtrLength ? Number(formData.rfdFtrLength) : null,
        rfdFtrType: formData.rfdFtrType,
        rfdFtrCtrlTagCnt: formData.rfdFtrCtrlTagCnt ? Number(formData.rfdFtrCtrlTagCnt) : null,
        rfdDrBlockSize: formData.rfdDrBlockSize ? Number(formData.rfdDrBlockSize) : null,
        rfdDrBlockSizeFlag: formData.rfdDrBlockSizeFlag,
        rfdDrFormat: formData.rfdDrFormat,
        rfdDridentifierFlag: formData.rfdDridentifierFlag,
        rfdMultiDrCheck: formData.rfdMultiDrCheck,
        rfdMultiDrCount: formData.rfdMultiDrCount ? Number(formData.rfdMultiDrCount) : null,
        rfdSettleFlg: formData.rfdSettleFlg ? 'Y' : 'N',
        rfdInstCode: 1,
      };

      if (mode === 'edit' && editFileId) {
        await authAPI.updateFileConfiguration(editFileId, payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'File configuration updated successfully.',
        });
      } else {
        await authAPI.createFileConfiguration(payload, token);
        addNotification({
          type: 'success',
          title: 'Success',
          message: 'File configuration created successfully.',
        });
      }
      handleBack();
      fetchFiles();
    } catch (error) {
      console.error('Failed to save file configuration:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: `Failed to ${mode === 'edit' ? 'update' : 'create'} file configuration. Please try again.`,
      });
    } finally {
      setSubmitting(false);
    }
  };

  const getDisplayValue = (key, value, field) => {
    if (field?.type === 'template') {
      const tpl = templates.find((t) => String(t.reconTemplateId) === String(value));
      return tpl ? tpl.templateName : '—';
    }
    if (value === null || value === undefined || value === '') return '—';
    if (value === true) return 'Yes';
    if (value === false) return 'No';
    if (value === 'Y') return 'YES';
    if (value === 'N') return 'NO';
    if (value === '\t') return 'Tab';
    if (value === '|') return 'Pipe ( | )';
    if (value === ',') return 'Comma ( , )';
    return String(value);
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

  // ─── Step Content Renderers ───

  const templateOptions = templates.map((t) => ({
    value: String(t.reconTemplateId),
    label: t.templateName,
  }));

  const renderFileDetails = () => (
    <div className={styles.formGrid}>
      <Select
        label="Template Name"
        value={formData.rtdTemplateId}
        onChange={(e) => handleFieldChange('rtdTemplateId', e.target.value)}
        options={templateOptions}
        placeholder="Select template"
      />
      <Input
        label="File Name"
        value={formData.rfdFileName}
        onChange={(e) => handleFieldChange('rfdFileName', e.target.value)}
        placeholder="e.g. DAILY_TRANSACTION_FILE.csv"
      />
      <Input
        label="File Description"
        value={formData.rfdFileDescription}
        onChange={(e) => handleFieldChange('rfdFileDescription', e.target.value)}
        placeholder="Enter file description"
      />
      <Input
        label="File Name Pattern"
        value={formData.rfdNameConvFormat}
        onChange={(e) => handleFieldChange('rfdNameConvFormat', e.target.value)}
        placeholder="e.g. YYYYMMDD_TXN"
      />
      <Input
        label="File Path"
        value={formData.rfdFileDestPath}
        onChange={(e) => handleFieldChange('rfdFileDestPath', e.target.value)}
        placeholder="e.g. /data/processed/transactions"
      />
      <Select
        label="Duplicate File Name"
        value={formData.rfdFileDupChkFlag}
        onChange={(e) => handleFieldChange('rfdFileDupChkFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Select
        label="GL Flag"
        value={formData.rfdGlFlag}
        onChange={(e) => handleFieldChange('rfdGlFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Select
        label="File Type"
        value={formData.rfdFileType}
        onChange={(e) => handleFieldChange('rfdFileType', e.target.value)}
        options={FILE_TYPE_OPTIONS}
        placeholder="Select file type"
      />
      <Select
        label="Delimiter"
        value={formData.rfdFileDelimiter}
        onChange={(e) => handleFieldChange('rfdFileDelimiter', e.target.value)}
        options={DELIMITER_OPTIONS}
        placeholder="Select delimiter"
      />
      <Input
        label="File Name Max. Length"
        type="number"
        value={formData.rfdFilenameLength}
        onChange={(e) => handleFieldChange('rfdFilenameLength', e.target.value)}
        placeholder="e.g. 50"
      />
      <div className={styles.checkboxRow}>
        <Checkbox
          label="Dependency"
          checked={formData.rfdDependentFileId}
          onChange={(e) => handleFieldChange('rfdDependentFileId', e.target.checked)}
        />
        <Checkbox
          label="Settlement Required"
          checked={formData.rfdSettleFlg}
          onChange={(e) => handleFieldChange('rfdSettleFlg', e.target.checked)}
        />
      </div>
    </div>
  );

  const renderHeaderRecord = () => (
    <div className={styles.formGrid}>
      <Select
        label="Header Available"
        value={formData.rfdHdrAvlFlag}
        onChange={(e) => handleFieldChange('rfdHdrAvlFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Input
        label="Header Block Size"
        type="number"
        value={formData.rfdHdrBlockSize}
        onChange={(e) => handleFieldChange('rfdHdrBlockSize', e.target.value)}
        placeholder="e.g. 1"
      />
      <Input
        label="Header ID"
        value={formData.rfdHdrId}
        onChange={(e) => handleFieldChange('rfdHdrId', e.target.value)}
        placeholder="Enter header ID"
      />
      <Input
        label="Header Key Count"
        type="number"
        value={formData.rfdHdrKeyCount}
        onChange={(e) => handleFieldChange('rfdHdrKeyCount', e.target.value)}
        placeholder="e.g. 5"
      />
      <Select
        label="Header With Data Record"
        value={formData.rfdHdrWithDr}
        onChange={(e) => handleFieldChange('rfdHdrWithDr', e.target.value)}
        options={YES_NO_OPTIONS}
      />
    </div>
  );

  const renderDataRecord = () => (
    <div className={styles.formGrid}>
      <Input
        label="DR Block Size"
        type="number"
        value={formData.rfdDrBlockSize}
        onChange={(e) => handleFieldChange('rfdDrBlockSize', e.target.value)}
        placeholder="e.g. 500"
      />
      <Select
        label="DR Block Size Flag"
        value={formData.rfdDrBlockSizeFlag}
        onChange={(e) => handleFieldChange('rfdDrBlockSizeFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Select
        label="DR Format"
        value={formData.rfdDrFormat}
        onChange={(e) => handleFieldChange('rfdDrFormat', e.target.value)}
        options={DR_FORMAT_OPTIONS}
        placeholder="Select DR format"
      />
      <Select
        label="DR Identifier Flag"
        value={formData.rfdDridentifierFlag}
        onChange={(e) => handleFieldChange('rfdDridentifierFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Select
        label="Multi DR Check"
        value={formData.rfdMultiDrCheck}
        onChange={(e) => handleFieldChange('rfdMultiDrCheck', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Input
        label="Multi DR Count"
        type="number"
        value={formData.rfdMultiDrCount}
        onChange={(e) => handleFieldChange('rfdMultiDrCount', e.target.value)}
        placeholder="e.g. 10"
      />
    </div>
  );

  const renderFooterRecord = () => (
    <div className={styles.formGrid}>
      <Select
        label="Footer Available"
        value={formData.rfdFtrAvailFlag}
        onChange={(e) => handleFieldChange('rfdFtrAvailFlag', e.target.value)}
        options={YES_NO_OPTIONS}
      />
      <Input
        label="Footer Begin Const Value"
        value={formData.rfdFtrBeginConstVal}
        onChange={(e) => handleFieldChange('rfdFtrBeginConstVal', e.target.value)}
        placeholder="e.g. FOOTER"
      />
      <Input
        label="Footer Length"
        type="number"
        value={formData.rfdFtrLength}
        onChange={(e) => handleFieldChange('rfdFtrLength', e.target.value)}
        placeholder="e.g. 100"
      />
      <Select
        label="Footer Type"
        value={formData.rfdFtrType}
        onChange={(e) => handleFieldChange('rfdFtrType', e.target.value)}
        options={FTR_TYPE_OPTIONS}
        placeholder="Select footer type"
      />
      <Input
        label="Footer Ctrl Tag Count"
        type="number"
        value={formData.rfdFtrCtrlTagCnt}
        onChange={(e) => handleFieldChange('rfdFtrCtrlTagCnt', e.target.value)}
        placeholder="e.g. 3"
      />
    </div>
  );

  const renderSummary = () => (
    <div className={styles.summaryContainer}>
      {SUMMARY_SECTIONS.map((section) => (
        <div key={section.title} className={styles.summarySection}>
          <h3 className={styles.summarySectionTitle}>{section.title}</h3>
          <div className={styles.summaryGrid}>
            {section.fields.map((field) => (
              <div key={field.key} className={styles.summaryItem}>
                <span className={styles.summaryLabel}>{field.label}</span>
                <span className={styles.summaryValue}>
                  {getDisplayValue(field.key, formData[field.key], field)}
                </span>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );

  const renderStepContent = () => {
    switch (currentStep) {
      case 'file-details': return renderFileDetails();
      case 'header-record': return renderHeaderRecord();
      case 'data-record': return renderDataRecord();
      case 'footer-record': return renderFooterRecord();
      case 'summary': return renderSummary();
      default: return null;
    }
  };

  // ─── Add / Edit Form View ───
  if (mode === 'add' || mode === 'edit') {
    const isLastStep = currentStep === 'summary';
    const isFirstStep = currentStepIndex === 0;
    const isEdit = mode === 'edit';

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
            Back to File Configuration
          </Button>
          <h1>{isEdit ? 'Edit File Configuration' : 'Add File Configuration'}</h1>
          <p>{isEdit ? 'Update the details step by step' : 'Fill in the details step by step'}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <Stepper steps={STEPS} currentStep={currentStep} onStepClick={handleStepClick} />

          <Card className={styles.formCard}>
            <div className={styles.stepTitle}>
              {STEPS[currentStepIndex].label}
            </div>

            <motion.div
              key={currentStep}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.2 }}
            >
              {renderStepContent()}
            </motion.div>

            <div className={styles.formActions}>
              {!isFirstStep && (
                <Button
                  variant="secondary"
                  onClick={handlePrev}
                  leftIcon={<ArrowLeft size={16} />}
                >
                  Previous
                </Button>
              )}
              <div className={styles.formActionsSpacer} />
              {isLastStep ? (
                <Button
                  variant="gold"
                  onClick={handleSubmit}
                  disabled={submitting}
                  leftIcon={submitting ? <Loader2 size={16} className={styles.spinner} /> : <Check size={16} />}
                >
                  {submitting ? 'Submitting...' : isEdit ? 'Update File Configuration' : 'Add File Configuration'}
                </Button>
              ) : (
                <Button
                  variant="primary"
                  onClick={handleNext}
                  rightIcon={<ArrowRight size={16} />}
                >
                  Next
                </Button>
              )}
            </div>
          </Card>
        </motion.div>
      </div>
    );
  }

  // ─── View Page ───
  if (mode === 'view' && viewData) {
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
            Back to File Configuration
          </Button>
          <h1>View File Configuration</h1>
          <p>{viewData.rfdFileName}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className={styles.summaryContainer}>
            {VIEW_SECTIONS.map((section) => (
              <div key={section.title} className={styles.summarySection}>
                <h3 className={styles.summarySectionTitle}>{section.title}</h3>
                <div className={styles.summaryGrid}>
                  {section.fields.map((field) => (
                    <div key={field.key} className={styles.summaryItem}>
                      <span className={styles.summaryLabel}>{field.label}</span>
                      <span className={styles.summaryValue}>
                        {getDisplayValue(field.key, viewData[field.key], field)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
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
        <h1>File Configuration</h1>
        <p>Manage file configurations for reconciliation</p>
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
                  placeholder="Search files..."
                />
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleAddClick}>
                Add
              </Button>
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className={styles.loadingOverlay}>
              <Loader2 size={24} className={styles.spinner} />
              <span>Loading file configurations...</span>
            </div>
          ) : (
            <>
              <div className={styles.tableContainer}>
                <table className={styles.mainTable}>
                  <thead>
                    <tr>
                      <th className={styles.sortable} onClick={() => handleSort('rfdFileName')}>
                        <div className={styles.thContent}>
                          <span>File Name</span>
                          {getSortIcon('rfdFileName')}
                        </div>
                      </th>
                      <th className={styles.sortable} onClick={() => handleSort('rfdFileLocation')}>
                        <div className={styles.thContent}>
                          <span>File Location</span>
                          {getSortIcon('rfdFileLocation')}
                        </div>
                      </th>
                      <th className={styles.sortable} onClick={() => handleSort('templateName')}>
                        <div className={styles.thContent}>
                          <span>Template Name</span>
                          {getSortIcon('templateName')}
                        </div>
                      </th>
                      <th className={styles.actionCol}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedFiles.length === 0 ? (
                      <tr>
                        <td colSpan={4} className={styles.emptyCell}>
                          No file configurations found
                        </td>
                      </tr>
                    ) : (
                      sortedFiles.map((file) => (
                        <tr key={file.rfdFileId}>
                          <td>{file.rfdFileName || '—'}</td>
                          <td>{file.rfdFileLocation || '—'}</td>
                          <td>{file.templateName || '—'}</td>
                          <td className={styles.actionCol}>
                            <div className={styles.actionBtns}>
                              <button
                                className={styles.iconBtn}
                                title="Edit"
                                onClick={() => handleEditClick(file)}
                              >
                                <Pencil size={15} />
                              </button>
                              <button
                                className={`${styles.iconBtn} ${styles.iconBtnView}`}
                                title="View"
                                onClick={() => handleView(file)}
                              >
                                <Eye size={15} />
                              </button>
                              <button
                                className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                                title="Delete"
                                onClick={() => handleDelete(file)}
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

              {/* Pagination */}
              <div className={styles.pagination}>
                <span className={styles.pageInfo}>
                  Showing {totalElements > 0 ? startEntry : 0} to {endEntry} of {totalElements} entries
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

export default FileConfig;
