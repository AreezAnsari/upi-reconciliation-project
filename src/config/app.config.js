// Application Configuration
export const APP_CONFIG = {
  name: 'Kal Infotech',
  tagline: 'Next-Generation Reconciliation Platform powered by AI',
  description: 'Enterprise-grade reconciliation platform for seamless data accuracy',
  version: '1.0.0',
  company: 'Kal Infotech',
};

// API Configuration
export const API_CONFIG = {
  baseUrl: import.meta.env.VITE_API_URL || 'http://13.48.46.135:8081',
    // baseUrl: import.meta.env.VITE_API_URL || 'http://192.168.1.104:8081',

  timeout: 30000,
};

// Menu Configuration
export const MENU_CONFIG = {
  admin: {
    label: 'Admin',
    icon: 'Shield',
    items: [
      { id: 'institution-onboarding', label: 'Institution Onboarding', path: '/admin/institution-onboarding' },
      { id: 'add-roles', label: 'Add Roles', path: '/admin/add-roles' },
      { id: 'add-user', label: 'Add User', path: '/admin/add-user' },
      { id: 'user-approval', label: 'User Approval', path: '/admin/user-approval' },
      { id: 'add-menu', label: 'Add Menu', path: '/admin/add-menu' },
      { id: "sub-institute", label: "Sub Institute", path: "/admin/sub-institute" }
      
      
      
    ],
  },
  setup: {
    label: 'Configuration',
    icon: 'Settings',
    items: [
      { id: 'file-config', label: 'File Configuration', path: '/setup/file-config' },
            { id: 'recon-config', label: 'Recon Config', path: '/setup/recon-config' },
      { id: 'report-config', label: 'Report Configuration', path: '/setup/report-config' },
       { id: 'ftp-config', label: 'FTP Configuration', path: '/setup/ftp-config' },
      { id: 'force-match', label: 'Force Match Config', path: '/setup/force-match' },

      { id: 'template-config', label: 'Template Config', path: '/setup/template-config' },
      
      { id: 'process-definition', label: 'Process Definition', path: '/setup/process-definition' },
     
    ],
  },
  report: {
    label: 'Report',
    icon: 'FileText',
    items: [
      { id: 'generate-report', label: 'Generate Report', path: '/reports/generate' },
      { id: 'ttum-report', label: 'TTUM Report', path: '/reports/ttum' },
      { id: 'reconciliation-report', label: 'Reconciliation Report', path: '/reports/reconciliation' },
      { id: 'extraction-details', label: 'Job Details', path: '/reports/extraction-details' },
    ],
  },
  process: {
    label: 'Process',
    icon: 'Cog',
    items: [
      { id: 'extraction-search', label: 'Extraction Transaction Search', path: '/process/extraction-search' },
      { id: 'bulk-match', label: 'Bulk Process Match', path: '/process/bulk-match' },
      { id: 'file-upload', label: 'File Upload', path: '/process/file-upload' },
      { id: 'split-transaction', label: 'Split Transaction', path: '/process/split-transaction' },
      { id: 'recon-search', label: 'Recon Transaction Search', path: '/process/recon-search' },
    ],
  },
  dispute: {
    label: 'Dispute Management',
    icon: 'Scale',
    items: [
      { id: 'dispute-dashboard', label: 'Dashboard', path: '/dispute/dashboard' },
      { id: 'dispute-action-center', label: 'Dispute Action Center', path: '/dispute/action-center' },
      { id: 'maker-upload', label: 'Maker Upload', path: '/dispute/maker-upload' },
      { id: 'checker-approval', label: 'Checker Approval', path: '/dispute/checker-approval' },
    ],
  },
  upi_recon: {
    label: 'UPI RECON Dashboard',
    icon: 'BarChart2',
    items: [
      { id: 'upi-recon-dashboard', label: 'UPI Recon Dashboard', path: '/upi-recon/dashboard' },
    ],
  },
  new_configuration: {
    label: 'New Configuration',
    icon: 'Layers',
    items: [
      { id: 'role-list', label: 'Role & Permissions', path: '/new-config/role-list' },
      { id: 'add-role', label: 'Add Role', path: '/new-config/add-role' },
      { id: 'view-role', label: 'View Role', path: '/new-config/view-role' },
      { id: 'role-approval-sent', label: 'Role Approval Sent', path: '/new-config/role-approval-sent' },
      { id: 'maker-queue', label: 'Maker Queue', path: '/new-config/maker-queue' },
      { id: 'checker-queue', label: 'Checker Queue', path: '/new-config/checker-queue' },
      { id: 'add-template', label: 'Add New Template', path: '/new-config/add-template' },
      { id: 'reconciliation', label: 'Define Reconciliation', path: '/new-config/reconciliation' },
    ],
  },
};

// Role Configuration
export const ROLES = [
  { id: 'superadmin', label: 'Super Admin' },
  { id: 'bankadmin', label: 'Bank Admin' },
  { id: 'operator', label: 'Operator' },
  { id: 'viewer', label: 'Viewer' },
];

// Report Types
export const REPORT_TYPES = [
  { id: 'daily', label: 'Daily Report' },
  { id: 'weekly', label: 'Weekly Report' },
  { id: 'monthly', label: 'Monthly Report' },
  { id: 'custom', label: 'Custom Report' },
  { id: 'ttum', label: 'TTUM Report' },
  { id: 'reconciliation', label: 'Reconciliation Report' },
];

// Process Types
export const PROCESS_TYPES = [
  { id: 'debitcard', label: 'Debit Card' },
  { id: 'creditcard', label: 'Credit Card' },
  { id: 'upi', label: 'UPI' },
  { id: 'neft', label: 'NEFT' },
  { id: 'rtgs', label: 'RTGS' },
  { id: 'imps', label: 'IMPS' },
];

// File Types
export const FILE_TYPES = [
  { id: 'pos_raw', label: 'POS Raw' },
  { id: 'cbs_raw', label: 'CBS Raw' },
  { id: 'switch_raw', label: 'Switch Raw' },
  { id: 'network_raw', label: 'Network Raw' },
];

// Dummy Menus for parent menu selection
export const DUMMY_MENUS = {
  master: [
    { id: 'admin', label: 'Admin' },
    { id: 'report', label: 'Report' },
    { id: 'process', label: 'Process' },
    { id: 'reconciliation', label: 'Reconciliation' },
    { id: 'extraction', label: 'Extraction' },
  ],
  main: [
    { id: 'add-menu', label: 'Add Menu', parent: 'admin' },
    { id: 'add-user', label: 'Add User', parent: 'admin' },
    { id: 'generate-report', label: 'Generate Report', parent: 'report' },
    { id: 'ttum-report', label: 'TTUM Report', parent: 'report' },
    { id: 'extraction-search', label: 'Extraction Search', parent: 'process' },
    { id: 'bulk-match', label: 'Bulk Process Match', parent: 'process' },
    { id: 'file-upload', label: 'File Upload', parent: 'process' },
    { id: 'auto-recon', label: 'Auto Reconciliation', parent: 'reconciliation' },
    { id: 'manual-recon', label: 'Manual Reconciliation', parent: 'reconciliation' },
    { id: 'file-processing', label: 'File Processing', parent: 'extraction' },
    { id: 'data-extraction', label: 'Data Extraction', parent: 'extraction' },
  ],
};
