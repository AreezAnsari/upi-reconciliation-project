// Dummy data for development and demo purposes

export const DUMMY_USERS = [
  {
    id: '1',
    username: 'admin',
    name: 'Akshay Ramani',
    email: 'akshay.ramani@fluxrecon.com',
    role: 'bankadmin',
    status: 'active',
    avatar: null,
    lastLogin: '2025-12-25T09:47:48Z',
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    username: 'superadmin',
    name: 'Priya Sharma',
    email: 'priya.sharma@fluxrecon.com',
    role: 'superadmin',
    status: 'active',
    avatar: null,
    lastLogin: '2025-12-24T14:30:00Z',
    createdAt: '2023-06-01T09:00:00Z',
  },
  {
    id: '3',
    username: 'operator1',
    name: 'Rahul Verma',
    email: 'rahul.verma@fluxrecon.com',
    role: 'operator',
    status: 'active',
    avatar: null,
    lastLogin: '2025-12-25T08:15:00Z',
    createdAt: '2024-03-20T11:00:00Z',
  },
  {
    id: '4',
    username: 'viewer1',
    name: 'Sneha Patel',
    email: 'sneha.patel@fluxrecon.com',
    role: 'viewer',
    status: 'pending',
    avatar: null,
    lastLogin: null,
    createdAt: '2025-12-20T15:00:00Z',
  },
];

export const DUMMY_MENUS = [
  {
    id: '1',
    name: 'Dashboard',
    type: 'master',
    description: 'Main dashboard menu',
    role: 'bankadmin',
    parentId: null,
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: '2',
    name: 'Reports',
    type: 'master',
    description: 'Reports management',
    role: 'bankadmin',
    parentId: null,
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: '3',
    name: 'Daily Reports',
    type: 'main',
    description: 'Daily transaction reports',
    role: 'operator',
    parentId: '2',
    createdAt: '2024-01-02T00:00:00Z',
  },
  {
    id: '4',
    name: 'TTUM Report',
    type: 'sub',
    description: 'Transaction Type Unit Mapping reports',
    role: 'operator',
    parentId: '3',
    processType: 'debitcard',
    fileName: 'POS_RAW',
    createdAt: '2024-01-03T00:00:00Z',
  },
];

export const DUMMY_TTUM_DATA = [
  {
    id: '1',
    description: 'UPIINW_TCC_CBS_TTUM',
    type: '1',
    reconModel: 'N/A',
  },
  {
    id: '2',
    description: 'NEFT_INWARD_CBS_TTUM',
    type: '2',
    reconModel: 'Standard',
  },
  {
    id: '3',
    description: 'RTGS_OUTWARD_CBS_TTUM',
    type: '3',
    reconModel: 'Premium',
  },
  {
    id: '4',
    description: 'IMPS_P2P_CBS_TTUM',
    type: '4',
    reconModel: 'Standard',
  },
  {
    id: '5',
    description: 'POS_DEBIT_SWITCH_TTUM',
    type: '5',
    reconModel: 'Premium',
  },
];

export const DUMMY_TRANSACTIONS = [
  {
    id: 'TXN001',
    reference: 'REF2025122501',
    amount: 15000.00,
    type: 'Credit',
    status: 'Matched',
    source: 'CBS',
    destination: 'Switch',
    date: '2025-12-25T10:30:00Z',
  },
  {
    id: 'TXN002',
    reference: 'REF2025122502',
    amount: 8500.50,
    type: 'Debit',
    status: 'Unmatched',
    source: 'Switch',
    destination: 'Network',
    date: '2025-12-25T11:15:00Z',
  },
  {
    id: 'TXN003',
    reference: 'REF2025122503',
    amount: 250000.00,
    type: 'Credit',
    status: 'Pending',
    source: 'CBS',
    destination: 'NPCI',
    date: '2025-12-25T09:45:00Z',
  },
  {
    id: 'TXN004',
    reference: 'REF2025122504',
    amount: 5000.00,
    type: 'Debit',
    status: 'Matched',
    source: 'POS',
    destination: 'Switch',
    date: '2025-12-24T18:20:00Z',
  },
  {
    id: 'TXN005',
    reference: 'REF2025122505',
    amount: 125000.00,
    type: 'Credit',
    status: 'Error',
    source: 'NEFT',
    destination: 'CBS',
    date: '2025-12-24T14:00:00Z',
  },
];

export const DUMMY_RECONCILIATION_SUMMARY = {
  totalTransactions: 15847,
  matchedTransactions: 14523,
  unmatchedTransactions: 892,
  pendingTransactions: 432,
  matchRate: 91.6,
  totalAmount: 2456789012.50,
  matchedAmount: 2234567890.00,
  unmatchedAmount: 222221122.50,
};

export const DUMMY_FILE_PROCESSES = [
  {
    id: 'FP001',
    fileName: 'CBS_DAILY_20251225.txt',
    filePath: '/app/fluxrecon/DCRSFILES/DEBITCARD/CBS_RAW',
    status: 'Completed',
    recordCount: 15000,
    processedAt: '2025-12-25T06:00:00Z',
  },
  {
    id: 'FP002',
    fileName: 'SWITCH_DAILY_20251225.txt',
    filePath: '/app/fluxrecon/DCRSFILES/DEBITCARD/SWITCH_RAW',
    status: 'Processing',
    recordCount: 12500,
    processedAt: '2025-12-25T06:30:00Z',
  },
  {
    id: 'FP003',
    fileName: 'POS_DAILY_20251225.txt',
    filePath: '/app/fluxrecon/DCRSFILES/DEBITCARD/POS_RAW',
    status: 'Pending',
    recordCount: null,
    processedAt: null,
  },
];

export const DUMMY_DASHBOARD_STATS = {
  dailyTransactions: 24567,
  dailyVolume: 125678943.50,
  matchRate: 94.2,
  pendingRecon: 1423,
  alerts: 12,
  processingQueue: 856,
};

export const DUMMY_RECENT_ACTIVITY = [
  {
    id: '1',
    action: 'File Uploaded',
    description: 'CBS_DAILY_20251225.txt uploaded successfully',
    user: 'Akshay Ramani',
    timestamp: '2025-12-25T10:30:00Z',
    type: 'success',
  },
  {
    id: '2',
    action: 'Reconciliation Started',
    description: 'Auto reconciliation initiated for Debit Card transactions',
    user: 'System',
    timestamp: '2025-12-25T10:15:00Z',
    type: 'info',
  },
  {
    id: '3',
    action: 'Report Generated',
    description: 'Daily TTUM report generated',
    user: 'Priya Sharma',
    timestamp: '2025-12-25T09:45:00Z',
    type: 'success',
  },
  {
    id: '4',
    action: 'Alert',
    description: 'High unmatched transaction count detected',
    user: 'System',
    timestamp: '2025-12-25T09:30:00Z',
    type: 'warning',
  },
  {
    id: '5',
    action: 'User Login',
    description: 'Admin logged in from 192.168.1.100',
    user: 'Akshay Ramani',
    timestamp: '2025-12-25T09:47:48Z',
    type: 'info',
  },
];

// Helper function to simulate API delay
export const simulateApiDelay = (data, delay = 500) => {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delay);
  });
};
