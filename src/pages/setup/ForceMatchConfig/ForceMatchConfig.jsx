import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, Search, Loader2, Play, X,
  ArrowUpDown, ArrowUp, ArrowDown,
  CheckCircle, FileText,
} from 'lucide-react';
import { Button, Card } from '../../../components/common';
import { useAuthStore, useAppStore } from '../../../store';
import { authAPI } from '../../../services';
import useTableControls from './useTableControls';
import ProcessConfigModal from './ProcessConfigModal';
import ActionConfigModal from './ActionConfigModal';
import ConfirmDialog from './ConfirmDialog';
import { formatDateTime } from '../../../utils/helpers';
import {
  CATEGORY_OPTIONS, CATEGORY_COLORS,
  INITIAL_PROCESS_FORM, INITIAL_ACTION_FORM,
} from './constants';
import styles from './ForceMatchConfig.module.css';

// ── Shared small components ──

const FlagBadge = ({ label, value }) => (
  <span className={`${styles.flagBadge} ${value === 'Y' ? styles.flagY : styles.flagN}`}>
    {label}:{value}
  </span>
);

const SortHeader = ({ label, sortKey, sortConfig, onSort }) => (
  <th className={styles.sortable} onClick={() => onSort(sortKey)}>
    <div className={styles.thContent}>
      {label}
      {sortConfig.key !== sortKey
        ? <ArrowUpDown size={12} className={styles.sortIconInactive} />
        : sortConfig.direction === 'asc'
          ? <ArrowUp size={12} className={styles.sortIconActive} />
          : <ArrowDown size={12} className={styles.sortIconActive} />}
    </div>
  </th>
);

const SearchInput = ({ value, onChange, onClear, placeholder }) => (
  <div className={styles.searchBox}>
    <Search size={15} className={styles.searchIcon} />
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      aria-label={placeholder}
    />
    {value && (
      <button className={styles.searchClear} onClick={onClear} aria-label="Clear search">
        <X size={14} />
      </button>
    )}
  </div>
);

const EmptyState = ({ icon: Icon, message }) => (
  <div className={styles.emptyState}>
    {Icon && <Icon size={40} className={styles.emptyIcon} />}
    <p>{message}</p>
  </div>
);

// ── Process search keys ──
const PROCESS_SEARCH_KEYS = ['processId', 'remarks', 'categoryName', 'description'];
const ACTION_SEARCH_KEYS = ['id', 'remarks', 'dataTable', 'debitAccount'];

const ForceMatchConfig = () => {
  const { token } = useAuthStore();
  const { addNotification } = useAppStore();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('process');
  const [loading, setLoading] = useState(false);

  // Data
  const [processConfigs, setProcessConfigs] = useState([]);
  const [actionDefinitions, setActionDefinitions] = useState([]);

  // Process Config state
  const [processCategory, setProcessCategory] = useState('');
  const [processStatus, setProcessStatus] = useState('');
  const [showProcessModal, setShowProcessModal] = useState(false);
  const [editingProcess, setEditingProcess] = useState(null);
  const [processForm, setProcessForm] = useState({ ...INITIAL_PROCESS_FORM });
  const [processSubmitting, setProcessSubmitting] = useState(false);

  // Action Config state
  const [showActionModal, setShowActionModal] = useState(false);
  const [editingAction, setEditingAction] = useState(null);
  const [actionForm, setActionForm] = useState({ ...INITIAL_ACTION_FORM });
  const [actionSubmitting, setActionSubmitting] = useState(false);

  // Execute Force Match state
  const [spProcessId, setSpProcessId] = useState('');
  const [executing, setExecuting] = useState(false);
  const [bulkforceList, setBulkforceList] = useState([]);
  const [showExecuteSuccess, setShowExecuteSuccess] = useState(false);

  // Delete confirmation
  const [confirmDelete, setConfirmDelete] = useState(null);

  // ── Table controls (shared hook for sort + debounced search) ──
  const filteredByCategory = useMemo(() => {
    return processConfigs.filter((p) => {
      const matchesCat = !processCategory || p.categoryId === processCategory;
      const matchesStatus = !processStatus || p.status === processStatus;
      return matchesCat && matchesStatus;
    });
  }, [processConfigs, processCategory, processStatus]);

  const processTable = useTableControls(filteredByCategory, PROCESS_SEARCH_KEYS);
  const actionTable = useTableControls(actionDefinitions, ACTION_SEARCH_KEYS);

  // ── Fetch process configs from API ──
  const fetchProcessConfigs = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const response = await authAPI.getForceMatchProcessConfigs(token);
      if (response.status === 'SUCCESS' && response.data) {
        const mapped = response.data.map((item) => ({
          id: item.rmpActionId,
          processId: String(item.rmpProcessId),
          categoryId: String(item.rmpActionCatgId),
          categoryName: item.categoryDesc || '',
          description: item.rmpManrecDescription || '',
          execOrder: item.rmpOrderOfExecution || '',
          mappingLevel: item.rmpMappingLevel || '',
          transactionDay: item.rmpTransactionDay || '',
          tempFlag1: item.rmpTemp1 || 'N',
          tempFlag2: item.rmpTemp2 || 'N',
          tempFlag3: item.rmpTemp3 || 'N',
          tempFlag4: item.rmpTemp4 || 'N',
          tempId1: item.rmpTempId1 || '',
          tempId2: item.rmpTempId2 || '',
          tempId3: item.rmpTempId3 || '',
          tempId4: item.rmpTempId4 || '',
          field1: item.rpmField1 || '',
          field2: item.rpmField2 || '',
          field3: item.rpmField3 || '',
          field4: item.rpmField4 || '',
          ejErrorCheck: item.rpmEjErrchkFlg || 'N',
          approvalRequired: item.rpmApprReqFlg || 'Y',
          diffFlag: item.rmpDiffFlag || 'N',
          diffAmountExpression: item.rmpDiffAmtExpr || '',
          status: item.rmpActionConfigStatus === 'Y' ? 'ACTIVE' : 'INACTIVE',
          instCode: item.rmpInstCode ? String(item.rmpInstCode) : '',
          insUser: item.rmpInsUser ? String(item.rmpInsUser) : '',
          lupdUser: item.rmpLupdUser ? String(item.rmpLupdUser) : '',
          insDate: item.rmpInsDate || '',
          lupdDate: item.rmpLupdDate || '',
          remarks: item.rmtRemarks || '',
          dataTable: item.rmtActDataTbl || '',
          debitAccount: item.rmtDebitAcct || '',
          creditAccount: item.rmtCreditAcct || '',
        }));
        setProcessConfigs(mapped);
      }
    } catch (error) {
      addNotification({ type: 'error', title: 'Error', message: error?.message || 'Failed to load process configurations.' });
    } finally {
      setLoading(false);
    }
  }, [token, addNotification]);

  // ── Fetch action configs from API ──
  const fetchActionConfigs = useCallback(async () => {
    if (!token) return;
    try {
      const response = await authAPI.getForceMatchActionConfigs(token);
      if (response.status === 'SUCCESS' && response.data) {
        const mapped = response.data.map((item) => ({
          id: item.rmtActionId,
          debitAccount: item.rmtDebitAcct || '',
          creditAccount: item.rmtCreditAcct || '',
          dataTable: item.rmtActDataTbl || '',
          delimiter: item.rmtDelimiter || '/',
          narration: item.rmtNarration || '',
          narrationFull: item.rmtNarration || '',
          debitNarration: item.rmtDebitNarration || '',
          creditNarration: item.rmtCreditNarration || '',
          narrationConstant: item.rmtNarrationCnst || '',
          narrationConstDebit: item.rmtNarrationCnstDebit || '',
          narrationConstCredit: item.rmtNarrationCnstCredit || '',
          dcIndicator: item.rmtDcInd || '',
          remarks: item.rmtRemarks || '',
          ruleId: item.rmtRuleId ? String(item.rmtRuleId) : '',
          instCode: item.rmtInstCode ? String(item.rmtInstCode) : '',
          insUser: item.rmtInsUser ? String(item.rmtInsUser) : '',
          lupdUser: item.rmtLupdUser ? String(item.rmtLupdUser) : '',
          insDate: item.rmtInsDate || '',
          lupdDate: item.rmtLupdDate || '',
        }));
        setActionDefinitions(mapped);
      }
    } catch (error) {
      addNotification({ type: 'error', title: 'Error', message: error?.message || 'Failed to load action definitions.' });
    }
  }, [token, addNotification]);

  useEffect(() => {
    fetchProcessConfigs();
    fetchActionConfigs();
  }, [fetchProcessConfigs, fetchActionConfigs]);

  // ── Fetch bulkforce list for Execute SP dropdown ──
  useEffect(() => {
    if (!token || activeTab !== 'execute') return;
    const fetchBulkforceList = async () => {
      try {
        const response = await authAPI.getBulkforceList(token);
        if (response.status === 'SUCCESS' && response.data) {
          setBulkforceList(response.data);
        }
      } catch {
        addNotification({ type: 'error', title: 'Error', message: 'Failed to load process list.' });
      }
    };
    fetchBulkforceList();
  }, [token, activeTab, addNotification]);

  // ── Stats ──
  const stats = useMemo(() => {
    const active = processConfigs.filter((p) => p.status === 'ACTIVE').length;
    return {
      total: processConfigs.length,
      active,
      inactive: processConfigs.length - active,
      actionDefs: actionDefinitions.length,
    };
  }, [processConfigs, actionDefinitions]);

  // ── Process CRUD ──
  const handleNewProcessConfig = useCallback(() => {
    setEditingProcess(null);
    setProcessForm({ ...INITIAL_PROCESS_FORM });
    setShowProcessModal(true);
  }, []);

  const handleEditProcess = useCallback((config) => {
    setEditingProcess(config);
    setProcessForm({
      processId: config.processId || '',
      categoryId: config.categoryId || '',
      categoryName: config.categoryName || '',
      tempFlag1: config.tempFlag1 || 'N',
      tempFlag2: config.tempFlag2 || 'N',
      tempFlag3: config.tempFlag3 || 'N',
      tempFlag4: config.tempFlag4 || 'N',
      tempId1: config.tempId1 || '',
      tempId2: config.tempId2 || '',
      tempId3: config.tempId3 || '',
      tempId4: config.tempId4 || '',
      mappingLevel: config.mappingLevel || '',
      transactionDay: config.transactionDay || '',
      instCode: config.instCode || '',
      approvalRequired: config.approvalRequired || 'Y',
      ejErrorCheck: config.ejErrorCheck || 'N',
      diffFlag: config.diffFlag || 'N',
      diffAmountExpression: config.diffAmountExpression || '',
      field1: config.field1 || '',
      field2: config.field2 || '',
      field3: config.field3 || '',
      field4: config.field4 || '',
      configStatus: config.status === 'ACTIVE' ? 'Y' : 'N',
      insUser: config.insUser || '',
      lupdUser: config.lupdUser || '',
      description: config.description || '',
    });
    setShowProcessModal(true);
  }, []);

  const handleProcessFormChange = useCallback((field, value) => {
    setProcessForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleProcessSubmit = useCallback(async () => {
    if (!processForm.processId.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Process ID is required.' });
      return;
    }
    if (!processForm.categoryId) {
      addNotification({ type: 'error', title: 'Validation', message: 'Category is required.' });
      return;
    }
    setProcessSubmitting(true);
    try {
      const payload = {
        rmpProcessId: parseInt(processForm.processId, 10),
        rmpActionCatgId: parseInt(processForm.categoryId, 10),
        rmpManrecDescription: processForm.description || null,
        rmpOrderOfExecution: processForm.execOrder ? String(processForm.execOrder) : null,
        rmpMappingLevel: processForm.mappingLevel || null,
        rmpTransactionDay: processForm.transactionDay || null,
        rmpTemp1: processForm.tempFlag1,
        rmpTemp2: processForm.tempFlag2,
        rmpTemp3: processForm.tempFlag3,
        rmpTemp4: processForm.tempFlag4,
        rpmEjErrchkFlg: processForm.ejErrorCheck,
        rpmApprReqFlg: processForm.approvalRequired,
        rmpDiffFlag: processForm.diffFlag,
        rmpDiffAmtExpr: processForm.diffAmountExpression || null,
        rmpActionConfigStatus: processForm.configStatus,
        rmpInstCode: processForm.instCode ? parseInt(processForm.instCode, 10) : null,
        rmpInsUser: processForm.insUser ? parseInt(processForm.insUser, 10) : null,
        rmpLupdUser: processForm.lupdUser ? parseInt(processForm.lupdUser, 10) : null,
      };
      if (editingProcess) {
        await authAPI.updateForceMatchProcessConfig(editingProcess.id, payload, token);
        addNotification({ type: 'success', title: 'Success', message: 'Process config updated successfully.' });
      } else {
        await authAPI.createForceMatchProcessConfig(payload, token);
        addNotification({ type: 'success', title: 'Success', message: 'Process config created successfully.' });
      }
      setShowProcessModal(false);
      setEditingProcess(null);
      fetchProcessConfigs();
    } catch (error) {
      addNotification({
        type: 'error', title: 'Error',
        message: error?.message || 'Failed to save process config.',
      });
    } finally {
      setProcessSubmitting(false);
    }
  }, [processForm, editingProcess, addNotification, token, fetchProcessConfigs]);

  const handleCloseProcessModal = useCallback(() => {
    setShowProcessModal(false);
    setEditingProcess(null);
  }, []);

  const handleToggleProcessStatus = useCallback((config) => {
    const newStatus = config.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    // TODO: Replace with actual API call
    setProcessConfigs((prev) =>
      prev.map((p) => (p.id === config.id ? { ...p, status: newStatus } : p))
    );
    addNotification({
      type: 'success', title: 'Status Updated',
      message: `Config #${config.id} is now ${newStatus}.`,
    });
  }, [addNotification]);

  const handleDeleteProcess = useCallback((config) => {
    setConfirmDelete({
      type: 'process',
      item: config,
      title: 'Delete Process Config',
      message: `Are you sure you want to delete config #${config.id} (Process ${config.processId})?`,
    });
  }, []);

  // ── Action CRUD ──
  const handleNewAction = useCallback(() => {
    setEditingAction(null);
    setActionForm({ ...INITIAL_ACTION_FORM });
    setShowActionModal(true);
  }, []);

  const handleEditAction = useCallback((action) => {
    setEditingAction(action);
    setActionForm({
      debitAccount: action.debitAccount || '',
      creditAccount: action.creditAccount || '',
      dcIndicator: action.dcIndicator || '',
      narrationFull: action.narrationFull || '',
      debitNarration: action.debitNarration || '',
      creditNarration: action.creditNarration || '',
      delimiter: action.delimiter || '/',
      narrationConstant: action.narrationConstant || '',
      narrationConstDebit: action.narrationConstDebit || '',
      narrationConstCredit: action.narrationConstCredit || '',
      dataTable: action.dataTable || '',
      ruleId: action.ruleId || '',
      instCode: action.instCode || '',
      remarks: action.remarks || '',
      insUser: action.insUser || '',
      lupdUser: action.lupdUser || '',
    });
    setShowActionModal(true);
  }, []);

  const handleActionFormChange = useCallback((field, value) => {
    setActionForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleActionSubmit = useCallback(async () => {
    if (!actionForm.debitAccount.trim()) {
      addNotification({ type: 'error', title: 'Validation', message: 'Debit Account is required.' });
      return;
    }
    setActionSubmitting(true);
    try {
      const payload = {
        rmtDebitAcct: actionForm.debitAccount || null,
        rmtCreditAcct: actionForm.creditAccount || null,
        rmtDcInd: actionForm.dcIndicator || null,
        rmtNarration: actionForm.narrationFull || null,
        rmtDebitNarration: actionForm.debitNarration || null,
        rmtCreditNarration: actionForm.creditNarration || null,
        rmtDelimiter: actionForm.delimiter || null,
        rmtNarrationCnst: actionForm.narrationConstant || null,
        rmtNarrationCnstDebit: actionForm.narrationConstDebit || null,
        rmtNarrationCnstCredit: actionForm.narrationConstCredit || null,
        rmtActDataTbl: actionForm.dataTable || null,
        rmtRuleId: actionForm.ruleId ? parseInt(actionForm.ruleId, 10) : null,
        rmtInstCode: actionForm.instCode ? parseInt(actionForm.instCode, 10) : null,
        rmtRemarks: actionForm.remarks || null,
        rmtInsUser: actionForm.insUser ? parseInt(actionForm.insUser, 10) : null,
        rmtLupdUser: actionForm.lupdUser ? parseInt(actionForm.lupdUser, 10) : null,
      };
      if (editingAction) {
        await authAPI.updateForceMatchActionConfig(editingAction.id, payload, token);
        addNotification({ type: 'success', title: 'Success', message: 'Action definition updated successfully.' });
      } else {
        await authAPI.createForceMatchActionConfig(payload, token);
        addNotification({ type: 'success', title: 'Success', message: 'Action definition created successfully.' });
      }
      setShowActionModal(false);
      setEditingAction(null);
      fetchActionConfigs();
    } catch (error) {
      addNotification({
        type: 'error', title: 'Error',
        message: error?.message || 'Failed to save action definition.',
      });
    } finally {
      setActionSubmitting(false);
    }
  }, [actionForm, editingAction, addNotification, token, fetchActionConfigs]);

  const handleCloseActionModal = useCallback(() => {
    setShowActionModal(false);
    setEditingAction(null);
  }, []);

  const handleDeleteAction = useCallback((action) => {
    setConfirmDelete({
      type: 'action',
      item: action,
      title: 'Delete Action Definition',
      message: `Are you sure you want to delete action #${action.id}?`,
    });
  }, []);

  // ── Confirm delete handler ──
  const handleConfirmDelete = useCallback(async () => {
    if (!confirmDelete) return;
    try {
      if (confirmDelete.type === 'process') {
        await authAPI.deleteForceMatchProcessConfig(confirmDelete.item.id, token);
        addNotification({
          type: 'success', title: 'Deleted',
          message: `Process config #${confirmDelete.item.id} deleted successfully.`,
        });
        fetchProcessConfigs();
      }
    } catch (err) {
      addNotification({
        type: 'error', title: 'Delete Failed',
        message: err.message || 'Failed to delete config.',
      });
    }
    setConfirmDelete(null);
  }, [confirmDelete, token, addNotification, fetchProcessConfigs]);

  // ── Execute Force Match ──
  const handleExecuteSP = useCallback(async () => {
    if (!spProcessId) {
      addNotification({ type: 'error', title: 'Validation', message: 'Please select a Process ID.' });
      return;
    }
    setExecuting(true);
    try {
      const payload = { processId: String(spProcessId) };
      const response = await authAPI.executeForceMatch(payload, token);
      if (response.status?.toUpperCase() === 'SUCCESS') {
        setShowExecuteSuccess(true);
      } else {
        addNotification({
          type: 'error', title: 'Error',
          message: response.statusMsg || 'Failed to execute force match.',
        });
      }
    } catch (error) {
      addNotification({
        type: 'error', title: 'Error',
        message: error?.message || 'Failed to execute force match.',
      });
    } finally {
      setExecuting(false);
    }
  }, [spProcessId, addNotification, token]);

  // ── Category badge color helper ──
  const getCategoryStyle = useCallback((categoryId) => {
    const color = CATEGORY_COLORS[categoryId] || '#6b7280';
    return `${styles.categoryBadge} ${styles[`cat${categoryId}`] || ''}`;
  }, []);

  return (
    <div className={styles.page}>
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className={styles.pageHeader}
      >
        <div className={styles.headerLeft}>
          <div className={styles.headerIcon}>FM</div>
          <div>
            <h1 className={styles.headerTitle}>Force Match Config</h1>
            <p className={styles.headerSubtitle}>Reconciliation Management System</p>
          </div>
        </div>
        <nav className={styles.tabBar} aria-label="Force Match Config tabs">
          {[
            { id: 'process', label: 'Process Config' },
            { id: 'action', label: 'Action Config' },
            { id: 'execute', label: 'Execute Force Match', icon: <Play size={14} /> },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`${styles.tab} ${activeTab === tab.id ? styles.tabActive : ''}`}
              onClick={() => setActiveTab(tab.id)}
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`panel-${tab.id}`}
            >
              {tab.icon || <span className={styles.tabDot} />}
              {tab.label}
            </button>
          ))}
        </nav>
      </motion.div>

      {/* Stats Cards */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
        className={styles.statsRow}
      >
        {[
          { value: stats.total, label: 'Total Process Configs', variant: 'statOrange' },
          { value: stats.active, label: 'Active', variant: 'statGreen' },
          { value: stats.inactive, label: 'Inactive', variant: 'statGray' },
          { value: stats.actionDefs, label: 'Action Definitions', variant: 'statBlue' },
        ].map((stat) => (
          <div key={stat.variant} className={`${styles.statCard} ${styles[stat.variant]}`}>
            <span className={styles.statValue}>{stat.value}</span>
            <span className={styles.statLabel}>{stat.label}</span>
          </div>
        ))}
      </motion.div>

      {/* Tab Content */}
      <motion.div
        key={activeTab}
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        id={`panel-${activeTab}`}
        role="tabpanel"
      >
        {/* ── Process Config Tab ── */}
        {activeTab === 'process' && (
          <Card className={styles.sectionCard}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>Process Configurations</h2>
                <p className={styles.sectionCount}>
                  {processTable.filteredCount} of {processConfigs.length} records
                </p>
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleNewProcessConfig}>
                New Config
              </Button>
            </div>

            <div className={styles.filters}>
              <SearchInput
                value={processTable.searchTerm}
                onChange={processTable.handleSearch}
                onClear={processTable.clearSearch}
                placeholder="search process id / description"
              />
              <select
                className={styles.filterSelect}
                value={processCategory}
                onChange={(e) => setProcessCategory(e.target.value)}
                aria-label="Filter by category"
              >
                <option value="">All Categories</option>
                {CATEGORY_OPTIONS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
              <select
                className={styles.filterSelect}
                value={processStatus}
                onChange={(e) => setProcessStatus(e.target.value)}
                aria-label="Filter by status"
              >
                <option value="">All Status</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>

            {loading ? (
              <div className={styles.loadingOverlay}>
                <Loader2 size={24} className={styles.spinner} />
                <span>Loading process configurations...</span>
              </div>
            ) : processTable.sorted.length === 0 ? (
              <EmptyState message="No process configurations found" />
            ) : (
              <div className={styles.tableContainer}>
                <table className={styles.dataTable}>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <SortHeader label="Process ID" sortKey="processId" sortConfig={processTable.sortConfig} onSort={processTable.handleSort} />
                      <th>Category</th>
                      <th>Flags</th>
                      <SortHeader label="Order" sortKey="execOrder" sortConfig={processTable.sortConfig} onSort={processTable.handleSort} />
                      <th>Mapping</th>
                      <th>Appr</th>
                      <SortHeader label="Status" sortKey="status" sortConfig={processTable.sortConfig} onSort={processTable.handleSort} />
                      <th>Description</th>
                      <th>Remarks</th>
                      <th>Last Updated</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {processTable.sorted.map((config) => (
                      <tr key={config.id}>
                        <td className={styles.idCol}>#{config.id}</td>
                        <td className={styles.processIdCol}>{config.processId}</td>
                        <td>
                          <span
                            className={styles.categoryBadge}
                            style={{
                              backgroundColor: `${CATEGORY_COLORS[config.categoryId] || '#6b7280'}18`,
                              color: CATEGORY_COLORS[config.categoryId] || '#6b7280',
                              borderColor: `${CATEGORY_COLORS[config.categoryId] || '#6b7280'}40`,
                            }}
                          >
                            {config.categoryId} - {config.categoryName}
                          </span>
                        </td>
                        <td>
                          <div className={styles.flagGroup}>
                            <FlagBadge label="NPCI" value={config.tempFlag1} />
                            <FlagBadge label="CBS" value={config.tempFlag2} />
                            <FlagBadge label="SWITCH" value={config.tempFlag3} />
                          </div>
                        </td>
                        <td className={styles.centerCol}>{config.execOrder}</td>
                        <td>{config.mappingLevel}</td>
                        <td>
                          <span className={`${styles.apprDot} ${config.approvalRequired === 'Y' ? styles.apprY : styles.apprN}`} />
                          {config.approvalRequired}
                        </td>
                        <td>
                          <span className={`${styles.statusBadge} ${config.status === 'ACTIVE' ? styles.statusActive : styles.statusInactive}`}>
                            {config.status}
                          </span>
                        </td>
                        <td className={styles.remarksCol} title={config.description}>{config.description}</td>
                        <td className={styles.remarksCol}>{config.remarks}</td>
                        <td className={styles.dateCol}>{formatDateTime(config.lupdDate)}</td>
                        <td>
                          <div className={styles.actionBtns}>
                            <button className={`${styles.actionBtn} ${styles.actionEdit}`} onClick={() => handleEditProcess(config)}>
                              Edit
                            </button>
                            <button
                              className={`${styles.actionBtn} ${config.status === 'ACTIVE' ? styles.actionDeact : styles.actionActivate}`}
                              onClick={() => handleToggleProcessStatus(config)}
                            >
                              {config.status === 'ACTIVE' ? 'Deact' : 'Activ'}
                            </button>
                            <button className={`${styles.actionBtn} ${styles.actionDel}`} onClick={() => handleDeleteProcess(config)}>
                              Del
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        )}

        {/* ── Action Config Tab ── */}
        {activeTab === 'action' && (
          <Card className={styles.sectionCard}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>Action Definitions</h2>
                <p className={styles.sectionCount}>
                  {actionTable.filteredCount} of {actionDefinitions.length} records
                </p>
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleNewAction}>
                New Action
              </Button>
            </div>

            <div className={styles.filters}>
              <SearchInput
                value={actionTable.searchTerm}
                onChange={actionTable.handleSearch}
                onClear={actionTable.clearSearch}
                placeholder="search action id / remarks / table"
              />
            </div>

            {loading ? (
              <div className={styles.loadingOverlay}>
                <Loader2 size={24} className={styles.spinner} />
                <span>Loading action definitions...</span>
              </div>
            ) : actionTable.sorted.length === 0 ? (
              <EmptyState message="No action definitions found" />
            ) : (
              <div className={styles.tableContainer}>
                <table className={styles.dataTable}>
                  <thead>
                    <tr>
                      <SortHeader label="Action ID" sortKey="id" sortConfig={actionTable.sortConfig} onSort={actionTable.handleSort} />
                      <th>Debit Acct</th>
                      <th>Credit Acct</th>
                      <th>Data Table</th>
                      <th>Delimiter</th>
                      <th>Narration</th>
                      <th>Remarks</th>
                      <SortHeader label="Rule ID" sortKey="ruleId" sortConfig={actionTable.sortConfig} onSort={actionTable.handleSort} />
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {actionTable.sorted.map((action) => (
                      <tr key={action.id}>
                        <td className={styles.idCol}>#{action.id}</td>
                        <td className={styles.acctCol}>{action.debitAccount}</td>
                        <td className={styles.acctCol}>{action.creditAccount}</td>
                        <td><span className={styles.tableBadge}>{action.dataTable}</span></td>
                        <td className={styles.delimiterCol}>
                          <span className={styles.delimiterBadge}>{action.delimiter}</span>
                        </td>
                        <td className={styles.narrationCol} title={action.narration}>{action.narration}</td>
                        <td>{action.remarks}</td>
                        <td>{action.ruleId}</td>
                        <td>
                          <div className={styles.actionBtns}>
                            <button className={`${styles.actionBtn} ${styles.actionEdit}`} onClick={() => handleEditAction(action)}>
                              Edit
                            </button>
                            <button className={`${styles.actionBtn} ${styles.actionDel}`} onClick={() => handleDeleteAction(action)}>
                              Del
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        )}

        {/* ── Execute Force Match Tab ── */}
        {activeTab === 'execute' && (
          <Card className={styles.sectionCard}>
            <div className={styles.executeCard}>
              <h3 className={styles.executeSectionTitle}>
                <span className={styles.executeDot} />
                Execute Force Match
              </h3>
              <div className={styles.executeGrid}>
                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="sp-processId">Process ID</label>
                  <select
                    id="sp-processId"
                    className={styles.formInput}
                    value={spProcessId}
                    onChange={(e) => setSpProcessId(e.target.value)}
                  >
                    <option value="">-- select process --</option>
                    {bulkforceList.map((item) => (
                      <option key={item.reconProcessId} value={item.reconProcessId}>{item.reconProcessName}</option>
                    ))}
                  </select>
                </div>
              </div>
              <Button
                variant="gold"
                leftIcon={executing ? <Loader2 size={16} className={styles.spinner} /> : <Play size={16} />}
                onClick={handleExecuteSP}
                disabled={executing}
              >
                {executing ? 'Executing...' : 'Execute Force Match'}
              </Button>
            </div>
          </Card>
        )}
      </motion.div>

      {/* ── Modals ── */}
      <AnimatePresence>
        {showProcessModal && (
          <ProcessConfigModal
            open={showProcessModal}
            editing={!!editingProcess}
            form={processForm}
            onChange={handleProcessFormChange}
            onSubmit={handleProcessSubmit}
            onClose={handleCloseProcessModal}
            submitting={processSubmitting}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showActionModal && (
          <ActionConfigModal
            open={showActionModal}
            editing={!!editingAction}
            form={actionForm}
            onChange={handleActionFormChange}
            onSubmit={handleActionSubmit}
            onClose={handleCloseActionModal}
            submitting={actionSubmitting}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {confirmDelete && (
          <ConfirmDialog
            open={!!confirmDelete}
            title={confirmDelete.title}
            message={confirmDelete.message}
            onConfirm={handleConfirmDelete}
            onCancel={() => setConfirmDelete(null)}
          />
        )}
      </AnimatePresence>

      {/* ── Execute Force Match Success Modal ── */}
      <AnimatePresence>
        {showExecuteSuccess && (
          <motion.div
            className={styles.modalOverlay}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowExecuteSuccess(false)}
          >
            <motion.div
              className={styles.successModal}
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className={styles.successModalIcon}>
                <CheckCircle size={48} />
              </div>
              <h3 className={styles.successModalTitle}>Force Match Executed Successfully</h3>
              <p className={styles.successModalText}>
                Would you like to retrieve the report?
              </p>
              <div className={styles.successModalActions}>
                <Button
                  variant="gold"
                  leftIcon={<FileText size={16} />}
                  onClick={() => {
                    setShowExecuteSuccess(false);
                    navigate('/reports/generate?tab=retrieve&reportType=FORCEMATCH');
                  }}
                >
                  Retrieve Report
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowExecuteSuccess(false)}
                >
                  Close
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default ForceMatchConfig;
