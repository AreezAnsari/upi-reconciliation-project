// ── Dummy Data (replace with API calls later) ──

export const DUMMY_PROCESS_CONFIGS = [
  {
    id: 1, processId: '1001', categoryId: '3', categoryName: 'Adjustment Entry',
    tempFlag1: 'Y', tempFlag2: 'N', tempFlag3: 'Y', tempFlag4: 'N',
    execOrder: 1, mappingLevel: 'L1', approvalRequired: 'Y', status: 'ACTIVE',
    remarks: 'PLAIN KNOCK OFF', tempId1: '', tempId2: '', tempId3: '', tempId4: '',
    transactionDay: '', instCode: '', ejErrorCheck: 'N', diffFlag: 'N',
    diffAmountExpression: '', field1: '', field2: '', field3: '', field4: '',
    insUser: '', lupdUser: '', description: '',
  },
  {
    id: 2, processId: '1001', categoryId: '4', categoryName: 'Late Presentment',
    tempFlag1: 'Y', tempFlag2: 'Y', tempFlag3: 'N', tempFlag4: 'N',
    execOrder: 2, mappingLevel: 'L2', approvalRequired: 'N', status: 'ACTIVE',
    remarks: 'LPR ENTRY', tempId1: '', tempId2: '', tempId3: '', tempId4: '',
    transactionDay: '', instCode: '', ejErrorCheck: 'N', diffFlag: 'N',
    diffAmountExpression: '', field1: '', field2: '', field3: '', field4: '',
    insUser: '', lupdUser: '', description: '',
  },
  {
    id: 3, processId: '1002', categoryId: '1', categoryName: 'Plain Knock Off',
    tempFlag1: 'N', tempFlag2: 'Y', tempFlag3: 'N', tempFlag4: 'Y',
    execOrder: 1, mappingLevel: 'L1', approvalRequired: 'Y', status: 'INACTIVE',
    remarks: 'PLAIN KNOCK OFF', tempId1: '', tempId2: '', tempId3: '', tempId4: '',
    transactionDay: '', instCode: '', ejErrorCheck: 'Y', diffFlag: 'N',
    diffAmountExpression: '', field1: '', field2: '', field3: '', field4: '',
    insUser: '', lupdUser: '', description: '',
  },
  {
    id: 4, processId: '1003', categoryId: '5', categoryName: 'LPR Reversal',
    tempFlag1: 'Y', tempFlag2: 'Y', tempFlag3: 'Y', tempFlag4: 'N',
    execOrder: 1, mappingLevel: 'L3', approvalRequired: 'N', status: 'ACTIVE',
    remarks: 'LPR REVERSAL', tempId1: '', tempId2: '', tempId3: '', tempId4: '',
    transactionDay: '', instCode: '', ejErrorCheck: 'N', diffFlag: 'N',
    diffAmountExpression: '', field1: '', field2: '', field3: '', field4: '',
    insUser: '', lupdUser: '', description: '',
  },
];

export const DUMMY_ACTION_DEFINITIONS = [
  {
    id: 101, debitAccount: 'customer_Acct', creditAccount: 'RPHUB03563506900',
    dataTable: 'REC_UPI_DATA', delimiter: '/', narration: 'TRAN_SEQ_NUM/TRAN_DATE/CO...',
    remarks: 'PLAIN KNOCK OFF', ruleId: '1001', dcIndicator: '', narrationFull: '',
    debitNarration: '', creditNarration: '', narrationConstant: '',
    narrationConstDebit: '', narrationConstCredit: '', instCode: '',
    insUser: '', lupdUser: '',
  },
  {
    id: 102, debitAccount: 'channel_Acct', creditAccount: 'customer_Acct',
    dataTable: 'REC_AEPS_AEOF_DATA', delimiter: '|', narration: 'TRAN_SEQ_NUM|TERM_ID|CONS...',
    remarks: 'LPR ENTRY', ruleId: '1002', dcIndicator: '', narrationFull: '',
    debitNarration: '', creditNarration: '', narrationConstant: '',
    narrationConstDebit: '', narrationConstCredit: '', instCode: '',
    insUser: '', lupdUser: '',
  },
  {
    id: 103, debitAccount: 'RPHUB035635069', creditAccount: 'customer_Acct',
    dataTable: 'REC_IMPS_DATA', delimiter: '/', narration: 'CARD_NUM/TRAN_AMOUNT/CONS...',
    remarks: 'ADJUSTMENT', ruleId: '1003', dcIndicator: '', narrationFull: '',
    debitNarration: '', creditNarration: '', narrationConstant: '',
    narrationConstDebit: '', narrationConstCredit: '', instCode: '',
    insUser: '', lupdUser: '',
  },
];

export const CATEGORY_OPTIONS = [
  { value: '1', label: '1 - Plain Knock Off' },
  { value: '2', label: '2 - Adjustment Reversal' },
  { value: '3', label: '3 - Adjustment Entry' },
  { value: '4', label: '4 - Late Presentment' },
  { value: '5', label: '5 - LPR Reversal' },
];

export const CATEGORY_COLORS = {
  '1': '#10b981', '2': '#3b82f6', '3': '#f59e0b',
  '4': '#a855f7', '5': '#ef4444',
};

export const INITIAL_PROCESS_FORM = {
  processId: '', categoryId: '', categoryName: '',
  tempFlag1: 'N', tempFlag2: 'N', tempFlag3: 'N', tempFlag4: 'N',
  tempId1: '', tempId2: '', tempId3: '', tempId4: '',
  mappingLevel: '', transactionDay: '', instCode: '',
  approvalRequired: 'Y', ejErrorCheck: 'N', diffFlag: 'N',
  diffAmountExpression: '',
  field1: '', field2: '', field3: '', field4: '',
  configStatus: 'Y', insUser: '', lupdUser: '', description: '',
};

export const INITIAL_ACTION_FORM = {
  debitAccount: '', creditAccount: '', dcIndicator: '',
  narrationFull: '', debitNarration: '', creditNarration: '', delimiter: '/',
  narrationConstant: '', narrationConstDebit: '', narrationConstCredit: '',
  dataTable: '', ruleId: '', instCode: '',
  remarks: '', insUser: '', lupdUser: '',
};
