import { useState, useMemo, useRef } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowLeft, ArrowRight, Check, Loader2, Plus, Eye, Pencil, Trash2,
  Search, ArrowUpDown, ArrowUp, ArrowDown, Building2, Upload, RefreshCw,
} from 'lucide-react';
import { Button, Card, Input, Select, Stepper } from '../../../components/common';
import { useAppStore } from '../../../store';
import { formatDate } from '../../../utils/helpers';
import styles from './InstitutionOnboarding.module.css';

// ─── Products list (from HTML) ───
const PRODUCTS = [
  { name: 'UPI', icon: '📲' },
  { name: 'NEFT', icon: '🏦' },
  { name: 'RTGS', icon: '⚡' },
  { name: 'AEPS', icon: '👆' },
  { name: 'BBPS', icon: '💡' },
  { name: 'SWIFT', icon: '🌐' },
  { name: 'Nostro-Vostro', icon: '🔄' },
  { name: 'Credit Cards', icon: '💳', variants: ['Master', 'Visa', 'Diners', 'RuPay', 'JCB'] },
  { name: 'Debit Card', icon: '🃏' },
  { name: 'Prepaid', icon: '📦' },
  { name: 'Forex Card', icon: '💱' },
  { name: 'NACH', icon: '🔁' },
  { name: 'CBDC', icon: '🪙' },
  { name: 'GL Tally', icon: '📊' },
  { name: 'Cash Tally', icon: '💰' },
  { name: 'Dispute & Chargeback', icon: '⚖️' },
];

const COUNTRY_CODES = [
  { value: '+91', label: '+91' },
  { value: '+1', label: '+1' },
  { value: '+44', label: '+44' },
  { value: '+971', label: '+971' },
];

const CITY_DATA = {
  Mumbai: { state: 'Maharashtra', country: 'India' },
  Delhi: { state: 'Delhi', country: 'India' },
  Bangalore: { state: 'Karnataka', country: 'India' },
  Chennai: { state: 'Tamil Nadu', country: 'India' },
  Kolkata: { state: 'West Bengal', country: 'India' },
  Hyderabad: { state: 'Telangana', country: 'India' },
};

const CITY_OPTIONS = [
  { value: '', label: 'Select city...' },
  ...Object.keys(CITY_DATA).map((c) => ({ value: c, label: c })),
];

const BANK_TYPE_OPTIONS = ['Issuer', 'Acquirer', 'Settlement Bank'];

// ─── Steps (5 matching HTML sections) ───
const STEPS = [
  { id: 'institution', label: 'Institution Details' },
  { id: 'address', label: 'Address' },
  { id: 'contact', label: 'Contact Info' },
  { id: 'products', label: 'Product Features' },
  { id: 'security', label: 'Security & Compliance' },
  { id: 'review', label: 'Review & Submit' },
];

const INITIAL_FORM = {
  // Step 1: Institution Details
  institutionNameFull: '',
  institutionNameShort: '',
  bankType: [],
  bankLogo: null,
  bankLogoName: '',

  // Step 2: Address — Registered
  regAddressLine1: '',
  regAddressLine2: '',
  regAddressLine3: '',
  regCity: '',
  regState: '',
  regCountry: '',
  regPhoneCode: '+91',
  regCityCode: '',
  regPhone: '',

  // Step 2: Address — Communication
  sameAsRegistered: false,
  commAddressLine1: '',
  commAddressLine2: '',
  commAddressLine3: '',
  commCity: '',
  commState: '',
  commCountry: '',
  commPhoneCode: '+91',
  commCityCode: '',
  commPhone: '',

  // Step 3: Contact — Primary
  primaryFullName: '',
  primaryEmail: '',
  primaryMobileCode: '+91',
  primaryMobile: '',
  primaryAltMobileCode: '+91',
  primaryAltMobile: '',

  // Step 3: Contact — Secondary
  secondaryFullName: '',
  secondaryEmail: '',
  secondaryMobileCode: '+91',
  secondaryMobile: '',
  secondaryAltMobileCode: '+91',
  secondaryAltMobile: '',

  // Step 4: Product Features
  selectedProducts: [],
  selectedVariants: {},

  // Step 5: Security
  enableMFA: false,
  enableHRMS: false,
  enableOTP: true,
};

// ─── Dummy Data ───
const DUMMY_INSTITUTIONS = [
  {
    id: 1,
    institutionId: 'INST-2024-00001',
    institutionNameFull: 'State Bank of India',
    institutionNameShort: 'SBI',
    bankType: ['Issuer', 'Acquirer'],
    regAddressLine1: 'SBI Bhavan, Madame Cama Road',
    regAddressLine2: 'Nariman Point',
    regAddressLine3: 'Near Mantralaya',
    regCity: 'Mumbai',
    regState: 'Maharashtra',
    regCountry: 'India',
    regPhoneCode: '+91',
    regCityCode: '022',
    regPhone: '22740012',
    sameAsRegistered: true,
    commAddressLine1: '',
    commAddressLine2: '',
    commAddressLine3: '',
    commCity: '',
    commState: '',
    commCountry: '',
    commPhoneCode: '+91',
    commCityCode: '',
    commPhone: '',
    primaryFullName: 'Rajesh Kumar',
    primaryEmail: 'rajesh.kumar@sbi.co.in',
    primaryMobileCode: '+91',
    primaryMobile: '9876543210',
    primaryAltMobileCode: '+91',
    primaryAltMobile: '9876543211',
    secondaryFullName: 'Amit Patel',
    secondaryEmail: 'amit.patel@sbi.co.in',
    secondaryMobileCode: '+91',
    secondaryMobile: '9876543212',
    secondaryAltMobileCode: '+91',
    secondaryAltMobile: '9876543213',
    selectedProducts: ['UPI', 'NEFT', 'RTGS', 'Credit Cards', 'Debit Card'],
    selectedVariants: { 'Credit Cards': ['Master', 'Visa', 'RuPay'] },
    enableMFA: true,
    enableHRMS: false,
    enableOTP: true,
    status: 'ACTIVE',
    createdDate: '2025-11-15',
  },
  {
    id: 2,
    institutionId: 'INST-2024-00002',
    institutionNameFull: 'HDFC Bank',
    institutionNameShort: 'HDFC',
    bankType: ['Issuer', 'Acquirer', 'Settlement Bank'],
    regAddressLine1: 'HDFC Bank House, Senapati Bapat Marg',
    regAddressLine2: 'Lower Parel',
    regAddressLine3: '',
    regCity: 'Mumbai',
    regState: 'Maharashtra',
    regCountry: 'India',
    regPhoneCode: '+91',
    regCityCode: '022',
    regPhone: '66521000',
    sameAsRegistered: false,
    commAddressLine1: 'HDFC Bank Ltd, Ramon House',
    commAddressLine2: 'H.T. Parekh Marg, Churchgate',
    commAddressLine3: '',
    commCity: 'Mumbai',
    commState: 'Maharashtra',
    commCountry: 'India',
    commPhoneCode: '+91',
    commCityCode: '022',
    commPhone: '66316000',
    primaryFullName: 'Priya Sharma',
    primaryEmail: 'priya.sharma@hdfcbank.com',
    primaryMobileCode: '+91',
    primaryMobile: '9123456780',
    primaryAltMobileCode: '+91',
    primaryAltMobile: '9123456781',
    secondaryFullName: 'Suresh Nair',
    secondaryEmail: 'suresh.nair@hdfcbank.com',
    secondaryMobileCode: '+91',
    secondaryMobile: '9123456782',
    secondaryAltMobileCode: '+91',
    secondaryAltMobile: '9123456783',
    selectedProducts: ['UPI', 'NEFT', 'RTGS', 'AEPS', 'BBPS', 'Credit Cards', 'Debit Card', 'NACH'],
    selectedVariants: { 'Credit Cards': ['Master', 'Visa', 'Diners', 'RuPay', 'JCB'] },
    enableMFA: true,
    enableHRMS: true,
    enableOTP: true,
    status: 'ACTIVE',
    createdDate: '2025-12-02',
  },
  {
    id: 3,
    institutionId: 'INST-2025-00003',
    institutionNameFull: 'ICICI Bank',
    institutionNameShort: 'ICICI',
    bankType: ['Issuer'],
    regAddressLine1: 'ICICI Bank Towers, Bandra-Kurla Complex',
    regAddressLine2: 'Bandra East',
    regAddressLine3: '',
    regCity: 'Mumbai',
    regState: 'Maharashtra',
    regCountry: 'India',
    regPhoneCode: '+91',
    regCityCode: '022',
    regPhone: '26531414',
    sameAsRegistered: true,
    commAddressLine1: '',
    commAddressLine2: '',
    commAddressLine3: '',
    commCity: '',
    commState: '',
    commCountry: '',
    commPhoneCode: '+91',
    commCityCode: '',
    commPhone: '',
    primaryFullName: 'Vikram Singh',
    primaryEmail: 'vikram.singh@icicibank.com',
    primaryMobileCode: '+91',
    primaryMobile: '9988776655',
    primaryAltMobileCode: '+91',
    primaryAltMobile: '9988776656',
    secondaryFullName: 'Deepak Verma',
    secondaryEmail: 'deepak.verma@icicibank.com',
    secondaryMobileCode: '+91',
    secondaryMobile: '9988776657',
    secondaryAltMobileCode: '+91',
    secondaryAltMobile: '9988776658',
    selectedProducts: ['UPI', 'NEFT', 'RTGS', 'Debit Card'],
    selectedVariants: {},
    enableMFA: true,
    enableHRMS: false,
    enableOTP: true,
    status: 'ACTIVE',
    createdDate: '2026-01-10',
  },
  {
    id: 4,
    institutionId: 'INST-2025-00004',
    institutionNameFull: 'Punjab National Bank',
    institutionNameShort: 'PNB',
    bankType: ['Issuer', 'Settlement Bank'],
    regAddressLine1: 'PNB Head Office, 7 Bhikhaiji Cama Place',
    regAddressLine2: 'R.K. Puram',
    regAddressLine3: '',
    regCity: 'Delhi',
    regState: 'Delhi',
    regCountry: 'India',
    regPhoneCode: '+91',
    regCityCode: '011',
    regPhone: '26198126',
    sameAsRegistered: true,
    commAddressLine1: '',
    commAddressLine2: '',
    commAddressLine3: '',
    commCity: '',
    commState: '',
    commCountry: '',
    commPhoneCode: '+91',
    commCityCode: '',
    commPhone: '',
    primaryFullName: 'Harpreet Kaur',
    primaryEmail: 'harpreet.kaur@pnb.co.in',
    primaryMobileCode: '+91',
    primaryMobile: '9871234560',
    primaryAltMobileCode: '+91',
    primaryAltMobile: '9871234561',
    secondaryFullName: 'Mohit Gupta',
    secondaryEmail: 'mohit.gupta@pnb.co.in',
    secondaryMobileCode: '+91',
    secondaryMobile: '9871234562',
    secondaryAltMobileCode: '+91',
    secondaryAltMobile: '9871234563',
    selectedProducts: ['NEFT', 'RTGS', 'NACH'],
    selectedVariants: {},
    enableMFA: false,
    enableHRMS: false,
    enableOTP: true,
    status: 'INACTIVE',
    createdDate: '2026-02-20',
  },
  {
    id: 5,
    institutionId: 'INST-2025-00005',
    institutionNameFull: 'Axis Bank',
    institutionNameShort: 'AXIS',
    bankType: ['Acquirer'],
    regAddressLine1: 'Axis House, Bombay Dyeing Mills Compound',
    regAddressLine2: 'Pandurang Budhkar Marg, Worli',
    regAddressLine3: '',
    regCity: 'Mumbai',
    regState: 'Maharashtra',
    regCountry: 'India',
    regPhoneCode: '+91',
    regCityCode: '022',
    regPhone: '24252525',
    sameAsRegistered: false,
    commAddressLine1: 'Axis Bank, Wadia International Centre',
    commAddressLine2: 'Pandurang Budhkar Marg, Worli',
    commAddressLine3: '',
    commCity: 'Mumbai',
    commState: 'Maharashtra',
    commCountry: 'India',
    commPhoneCode: '+91',
    commCityCode: '022',
    commPhone: '24254545',
    primaryFullName: 'Sneha Deshmukh',
    primaryEmail: 'sneha.deshmukh@axisbank.com',
    primaryMobileCode: '+91',
    primaryMobile: '9765432100',
    primaryAltMobileCode: '+91',
    primaryAltMobile: '9765432101',
    secondaryFullName: 'Ravi Iyer',
    secondaryEmail: 'ravi.iyer@axisbank.com',
    secondaryMobileCode: '+91',
    secondaryMobile: '9765432102',
    secondaryAltMobileCode: '+91',
    secondaryAltMobile: '9765432103',
    selectedProducts: ['UPI', 'NEFT', 'RTGS', 'BBPS', 'Credit Cards', 'Debit Card', 'Prepaid', 'Forex Card'],
    selectedVariants: { 'Credit Cards': ['Master', 'Visa', 'RuPay'] },
    enableMFA: true,
    enableHRMS: true,
    enableOTP: true,
    status: 'PENDING',
    createdDate: '2026-03-05',
  },
];

// ─── Summary config for review / detail view ───
const SUMMARY_SECTIONS = [
  {
    title: 'Institution Details',
    fields: [
      { key: 'institutionNameFull', label: 'Institution Name (Full)' },
      { key: 'institutionNameShort', label: 'Institution Name (Short)' },
      { key: 'bankType', label: 'Bank Type', type: 'array' },
    ],
  },
  {
    title: 'Registered Office Address',
    fields: [
      { key: 'regAddressLine1', label: 'Address Line 1' },
      { key: 'regAddressLine2', label: 'Address Line 2' },
      { key: 'regAddressLine3', label: 'Address Line 3' },
      { key: 'regCity', label: 'City' },
      { key: 'regState', label: 'State' },
      { key: 'regCountry', label: 'Country' },
      { key: '_regLandline', label: 'Landline Phone', type: 'computed', compute: (d) => d.regPhone ? `${d.regPhoneCode} ${d.regCityCode} ${d.regPhone}` : '—' },
    ],
  },
  {
    title: 'Communication Address',
    fields: [
      { key: 'sameAsRegistered', label: 'Same as Registered', type: 'bool' },
      { key: 'commAddressLine1', label: 'Address Line 1' },
      { key: 'commAddressLine2', label: 'Address Line 2' },
      { key: 'commAddressLine3', label: 'Address Line 3' },
      { key: 'commCity', label: 'City' },
      { key: 'commState', label: 'State' },
      { key: 'commCountry', label: 'Country' },
      { key: '_commLandline', label: 'Landline Phone', type: 'computed', compute: (d) => d.commPhone ? `${d.commPhoneCode} ${d.commCityCode} ${d.commPhone}` : '—' },
    ],
  },
  {
    title: 'Primary Contact',
    fields: [
      { key: 'primaryFullName', label: 'Full Name' },
      { key: 'primaryEmail', label: 'Official Email' },
      { key: '_primaryMobile', label: 'Mobile Number', type: 'computed', compute: (d) => d.primaryMobile ? `${d.primaryMobileCode} ${d.primaryMobile}` : '—' },
      { key: '_primaryAlt', label: 'Alternate Mobile', type: 'computed', compute: (d) => d.primaryAltMobile ? `${d.primaryAltMobileCode} ${d.primaryAltMobile}` : '—' },
    ],
  },
  {
    title: 'Secondary Contact',
    fields: [
      { key: 'secondaryFullName', label: 'Full Name' },
      { key: 'secondaryEmail', label: 'Official Email' },
      { key: '_secondaryMobile', label: 'Mobile Number', type: 'computed', compute: (d) => d.secondaryMobile ? `${d.secondaryMobileCode} ${d.secondaryMobile}` : '—' },
      { key: '_secondaryAlt', label: 'Alternate Mobile', type: 'computed', compute: (d) => d.secondaryAltMobile ? `${d.secondaryAltMobileCode} ${d.secondaryAltMobile}` : '—' },
    ],
  },
  {
    title: 'Product Features',
    fields: [
      { key: 'selectedProducts', label: 'Selected Products', type: 'array' },
    ],
  },
  {
    title: 'Security & Compliance',
    fields: [
      { key: 'enableMFA', label: 'Multi-Factor Authentication (MFA)', type: 'bool' },
      { key: 'enableHRMS', label: 'IDAM Integration', type: 'bool' },
      { key: 'enableOTP', label: 'OTP Verification', type: 'bool' },
    ],
  },
];

// ─── Toggle Switch ───
const SecurityToggle = ({ checked, onChange, label, description }) => (
  <div className={styles.securityItem}>
    <div className={styles.securityLeft}>
      <div className={styles.securityName}>{label}</div>
      <div className={styles.securityDesc}>{description}</div>
    </div>
    <label className={styles.toggleSwitch}>
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <span className={styles.toggleSlider} />
    </label>
  </div>
);

// ─── Main Component ───
const InstitutionOnboarding = () => {
  const { addNotification } = useAppStore();
  const fileInputRef = useRef(null);

  // View: 'list' | 'add' | 'edit' | 'detail'
  const [view, setView] = useState('list');
  const [institutions, setInstitutions] = useState(DUMMY_INSTITUTIONS);
  const [selectedInstitution, setSelectedInstitution] = useState(null);

  // Table controls
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Form state
  const [currentStep, setCurrentStep] = useState('institution');
  const [formData, setFormData] = useState({ ...INITIAL_FORM });
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  const currentStepIndex = STEPS.findIndex((s) => s.id === currentStep);

  // ─── Table Filter & Sort ───
  const filteredInstitutions = useMemo(() => {
    if (!searchTerm) return institutions;
    const term = searchTerm.toLowerCase();
    return institutions.filter(
      (inst) =>
        inst.institutionNameFull?.toLowerCase().includes(term) ||
        inst.institutionNameShort?.toLowerCase().includes(term) ||
        inst.regCity?.toLowerCase().includes(term) ||
        inst.status?.toLowerCase().includes(term) ||
        inst.primaryFullName?.toLowerCase().includes(term)
    );
  }, [institutions, searchTerm]);

  const sortedInstitutions = useMemo(() => {
    if (!sortConfig.key) return filteredInstitutions;
    return [...filteredInstitutions].sort((a, b) => {
      const aVal = a[sortConfig.key] ?? '';
      const bVal = b[sortConfig.key] ?? '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredInstitutions, sortConfig]);

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

  // ─── Navigation ───
  const handleBackToList = () => {
    setView('list');
    setSelectedInstitution(null);
    setFormData({ ...INITIAL_FORM });
    setCurrentStep('institution');
    setErrors({});
  };

  const handleAddNew = () => {
    setFormData({ ...INITIAL_FORM });
    setCurrentStep('institution');
    setErrors({});
    setView('add');
  };

  const handleViewDetail = (inst) => {
    setSelectedInstitution(inst);
    setView('detail');
  };

  const handleEdit = (inst) => {
    setSelectedInstitution(inst);
    setFormData({ ...INITIAL_FORM, ...inst });
    setCurrentStep('institution');
    setErrors({});
    setView('edit');
  };

  const handleDelete = (inst) => {
    setInstitutions((prev) => prev.filter((i) => i.id !== inst.id));
    addNotification({
      type: 'success',
      title: 'Deleted',
      message: `"${inst.institutionNameFull}" has been removed.`,
    });
  };

  // ─── Form Logic ───
  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: null }));
  };

  const handleMobileChange = (field, rawValue) => {
    if (/[^\d]/.test(rawValue)) {
      setErrors((prev) => ({ ...prev, [field]: 'Only numbers are allowed' }));
      return;
    }
    if (rawValue.length > 0 && !/^[6-9]/.test(rawValue)) {
      setErrors((prev) => ({ ...prev, [field]: 'Must start with 6, 7, 8, or 9' }));
      return;
    }
    if (rawValue.length > 10) return;
    setFormData((prev) => ({ ...prev, [field]: rawValue }));
    if (rawValue.length === 10) {
      setErrors((prev) => ({ ...prev, [field]: null }));
    } else if (rawValue.length > 0) {
      setErrors((prev) => ({ ...prev, [field]: 'Must be 10 digits' }));
    } else {
      setErrors((prev) => ({ ...prev, [field]: null }));
    }
  };

  const handleCityChange = (prefix, city) => {
    const data = CITY_DATA[city];
    setFormData((prev) => ({
      ...prev,
      [`${prefix}City`]: city,
      [`${prefix}State`]: data?.state || '',
      [`${prefix}Country`]: data?.country || '',
    }));
  };

  const toggleBankType = (type) => {
    setFormData((prev) => {
      const types = prev.bankType || [];
      return {
        ...prev,
        bankType: types.includes(type) ? types.filter((t) => t !== type) : [...types, type],
      };
    });
  };

  const toggleProduct = (productName) => {
    setFormData((prev) => {
      const selected = prev.selectedProducts || [];
      const isSelected = selected.includes(productName);
      const newSelected = isSelected
        ? selected.filter((p) => p !== productName)
        : [...selected, productName];
      const newVariants = { ...prev.selectedVariants };
      if (isSelected) delete newVariants[productName];
      return { ...prev, selectedProducts: newSelected, selectedVariants: newVariants };
    });
  };

  const toggleVariant = (productName, variant) => {
    setFormData((prev) => {
      const variants = { ...prev.selectedVariants };
      const current = variants[productName] || [];
      variants[productName] = current.includes(variant)
        ? current.filter((v) => v !== variant)
        : [...current, variant];
      return { ...prev, selectedVariants: variants };
    });
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        addNotification({ type: 'error', title: 'File too large', message: 'Max file size is 2 MB.' });
        return;
      }
      handleFieldChange('bankLogo', file);
      handleFieldChange('bankLogoName', file.name);
    }
  };

  const handleNext = () => {
    const stepErrors = validateStep(currentStep);
    if (Object.keys(stepErrors).length > 0) {
      setErrors(stepErrors);
      addNotification({ type: 'error', title: 'Validation', message: 'Please fix the highlighted errors.' });
      return;
    }
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
    if (targetIndex <= currentStepIndex) {
      setCurrentStep(stepId);
    }
  };

  const validateStep = (step) => {
    const e = {};
    if (step === 'institution') {
      if (!formData.institutionNameFull || formData.institutionNameFull.length < 2) e.institutionNameFull = 'Required (min 2 chars)';
      if (!formData.bankType || formData.bankType.length === 0) e.bankType = 'Select at least one bank type';
    } else if (step === 'address') {
      if (!formData.regAddressLine1) e.regAddressLine1 = 'Required';
      if (!formData.regCity) e.regCity = 'Required';
      if (!formData.regState) e.regState = 'Required';
      if (!formData.regCountry) e.regCountry = 'Required';
      if (!formData.regPhone) e.regPhone = 'Required';
      if (!formData.sameAsRegistered) {
        if (!formData.commAddressLine1) e.commAddressLine1 = 'Required';
        if (!formData.commCity) e.commCity = 'Required';
        if (!formData.commPhone) e.commPhone = 'Required';
      }
    } else if (step === 'contact') {
      if (!formData.primaryFullName) e.primaryFullName = 'Required';
      if (!formData.primaryEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.primaryEmail)) e.primaryEmail = 'Valid email required';
      if (!formData.primaryMobile || !/^[6-9]\d{9}$/.test(formData.primaryMobile)) e.primaryMobile = 'Enter a valid 10-digit mobile number starting with 6-9';
      if (!formData.primaryAltMobile || !/^[6-9]\d{9}$/.test(formData.primaryAltMobile)) e.primaryAltMobile = 'Enter a valid 10-digit mobile number starting with 6-9';
      if (!formData.secondaryFullName) e.secondaryFullName = 'Required';
      if (!formData.secondaryEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.secondaryEmail)) e.secondaryEmail = 'Valid email required';
      if (!formData.secondaryMobile || !/^[6-9]\d{9}$/.test(formData.secondaryMobile)) e.secondaryMobile = 'Enter a valid 10-digit mobile number starting with 6-9';
      if (!formData.secondaryAltMobile || !/^[6-9]\d{9}$/.test(formData.secondaryAltMobile)) e.secondaryAltMobile = 'Enter a valid 10-digit mobile number starting with 6-9';
    }
    return e;
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      if (view === 'edit' && selectedInstitution) {
        setInstitutions((prev) =>
          prev.map((inst) => (inst.id === selectedInstitution.id ? { ...inst, ...formData } : inst))
        );
        addNotification({
          type: 'success',
          title: 'Updated',
          message: `"${formData.institutionNameFull}" updated successfully.`,
        });
      } else {
        const newInst = {
          ...formData,
          id: Date.now(),
          institutionId: `INST-${new Date().getFullYear()}-${String(institutions.length + 1).padStart(5, '0')}`,
          status: 'PENDING',
          createdDate: new Date().toISOString().split('T')[0],
        };
        setInstitutions((prev) => [...prev, newInst]);
        addNotification({
          type: 'success',
          title: 'Success',
          message: `"${formData.institutionNameFull}" onboarded successfully.`,
        });
      }
      handleBackToList();
    } catch (error) {
      console.error('Failed:', error);
      addNotification({ type: 'error', title: 'Error', message: 'Operation failed. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  const getDisplayValue = (value, field, data) => {
    if (field?.type === 'bool') return null;
    if (field?.type === 'computed') return field.compute(data);
    if (field?.type === 'array') return Array.isArray(value) && value.length > 0 ? value.join(', ') : '—';
    if (value === null || value === undefined || value === '') return '—';
    return String(value);
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'ACTIVE': return styles.statusActive;
      case 'INACTIVE': return styles.statusInactive;
      case 'PENDING': return styles.statusPending;
      default: return '';
    }
  };

  // ═══════════════════════════════════════
  //  STEP RENDERERS
  // ═══════════════════════════════════════

  // ─── Step 1: Institution Details ───
  const renderInstitutionDetails = () => (
    <div className={styles.formGrid}>
      <Input
        label="Institution Name (Full)"
        value={formData.institutionNameFull}
        onChange={(e) => handleFieldChange('institutionNameFull', e.target.value)}
        placeholder="e.g. State Bank of India"
        required
        error={errors.institutionNameFull}
      />
      <Input
        label="Institution Name (Short)"
        value={formData.institutionNameShort}
        onChange={(e) => handleFieldChange('institutionNameShort', e.target.value)}
        placeholder="e.g. SBI"
        hint="Optional"
      />
      <div className={styles.fullWidth}>
        <div className={styles.fieldLabel}>Institution ID</div>
        <div className={styles.autoTag}>
          <RefreshCw size={14} />
          <span>Auto-populated by system</span>
          <span className={styles.autoTagValue}>
            {view === 'edit' && selectedInstitution?.institutionId
              ? selectedInstitution.institutionId
              : 'INST-2024-XXXXX'}
          </span>
        </div>
      </div>
      <div className={styles.fullWidth}>
        <div className={styles.fieldLabel}>
          Bank Type <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.chipGroup}>
          {BANK_TYPE_OPTIONS.map((type) => (
            <button
              key={type}
              type="button"
              className={`${styles.chip} ${(formData.bankType || []).includes(type) ? styles.chipSelected : ''}`}
              onClick={() => toggleBankType(type)}
            >
              <span className={styles.chipDot} />
              {type}
            </button>
          ))}
        </div>
        {errors.bankType && <div className={styles.errorText}>{errors.bankType}</div>}
        <div className={styles.hint}>Multiple selections allowed — contact Salim for guidance on bank type classification</div>
      </div>
      <div className={styles.fullWidth}>
        <div className={styles.fieldLabel}>Bank Logo</div>
        <div
          className={styles.uploadZone}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".jpg,.jpeg,.tif,.tiff"
            hidden
            onChange={handleFileChange}
          />
          {formData.bankLogoName ? (
            <>
              <Check size={28} className={styles.uploadIconDone} />
              <div className={styles.uploadText}>{formData.bankLogoName}</div>
              <div className={styles.uploadHint}>Click to change file</div>
            </>
          ) : (
            <>
              <Upload size={28} className={styles.uploadIcon} />
              <div className={styles.uploadText}><strong>Click to upload</strong> or drag & drop</div>
              <div className={styles.uploadHint}>Accepted: JPG / TIF — Max 2 MB</div>
            </>
          )}
        </div>
      </div>
    </div>
  );

  // ─── Step 2: Address ───
  const renderAddress = () => (
    <div className={styles.formGrid}>
      {/* Registered Office */}
      <div className={`${styles.sectionDividerLabel} ${styles.fullWidth}`}>Registered Office Address</div>
      <div className={styles.fullWidth}>
        <Input
          label="Address Line 1"
          value={formData.regAddressLine1}
          onChange={(e) => handleFieldChange('regAddressLine1', e.target.value)}
          placeholder="Building / Street"
          required
          error={errors.regAddressLine1}
        />
      </div>
      <Input
        label="Address Line 2"
        value={formData.regAddressLine2}
        onChange={(e) => handleFieldChange('regAddressLine2', e.target.value)}
        placeholder="Area / Locality"
      />
      <Input
        label="Address Line 3"
        value={formData.regAddressLine3}
        onChange={(e) => handleFieldChange('regAddressLine3', e.target.value)}
        placeholder="Landmark (optional)"
      />
      <div className={styles.cols3Row}>
        <Select
          label="City"
          value={formData.regCity}
          onChange={(e) => handleCityChange('reg', e.target.value)}
          options={CITY_OPTIONS}
          required
          error={errors.regCity}
        />
        <Input
          label="State"
          value={formData.regState}
          onChange={(e) => handleFieldChange('regState', e.target.value)}
          placeholder="Auto-populated..."
          required
          error={errors.regState}
        />
        <Input
          label="Country"
          value={formData.regCountry}
          onChange={(e) => handleFieldChange('regCountry', e.target.value)}
          placeholder="Auto-populated..."
          required
          error={errors.regCountry}
        />
      </div>
      <div className={styles.fullWidth}>
        <div className={styles.fieldLabel}>
          Landline Phone <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.phoneGroup}>
          <select
            className={styles.phoneSelect}
            value={formData.regPhoneCode}
            onChange={(e) => handleFieldChange('regPhoneCode', e.target.value)}
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
          <input
            className={styles.phoneInput}
            type="text"
            value={formData.regCityCode}
            onChange={(e) => handleFieldChange('regCityCode', e.target.value)}
            placeholder="City code"
            style={{ width: 100 }}
          />
          <input
            className={styles.phoneInputMain}
            type="tel"
            value={formData.regPhone}
            onChange={(e) => handleFieldChange('regPhone', e.target.value)}
            placeholder="Phone number"
          />
        </div>
        {errors.regPhone && <div className={styles.errorText}>{errors.regPhone}</div>}
      </div>

      {/* Divider */}
      <div className={`${styles.fullWidth} ${styles.addrDivider}`} />

      {/* Same as Registered Toggle */}
      <div className={styles.fullWidth}>
        <label
          className={`${styles.sameAddrToggle} ${formData.sameAsRegistered ? styles.sameAddrActive : ''}`}
          onClick={() => handleFieldChange('sameAsRegistered', !formData.sameAsRegistered)}
        >
          <div className={styles.toggleBox}>
            {formData.sameAsRegistered && <Check size={14} className={styles.toggleCheck} />}
          </div>
          Communication address same as Registered Office Address
        </label>
      </div>

      {/* Communication Address */}
      {!formData.sameAsRegistered && (
        <>
          <div className={`${styles.sectionDividerLabel} ${styles.fullWidth}`}>Communication Address</div>
          <div className={styles.fullWidth}>
            <Input
              label="Address Line 1"
              value={formData.commAddressLine1}
              onChange={(e) => handleFieldChange('commAddressLine1', e.target.value)}
              placeholder="Building / Street"
              required
              error={errors.commAddressLine1}
            />
          </div>
          <Input
            label="Address Line 2"
            value={formData.commAddressLine2}
            onChange={(e) => handleFieldChange('commAddressLine2', e.target.value)}
            placeholder="Area / Locality"
          />
          <Input
            label="Address Line 3"
            value={formData.commAddressLine3}
            onChange={(e) => handleFieldChange('commAddressLine3', e.target.value)}
            placeholder="Landmark (optional)"
          />
          <div className={styles.cols3Row}>
            <Select
              label="City"
              value={formData.commCity}
              onChange={(e) => handleCityChange('comm', e.target.value)}
              options={CITY_OPTIONS}
              required
              error={errors.commCity}
            />
            <Input
              label="State"
              value={formData.commState}
              onChange={(e) => handleFieldChange('commState', e.target.value)}
              placeholder="Auto-populated..."
              required
            />
            <Input
              label="Country"
              value={formData.commCountry}
              onChange={(e) => handleFieldChange('commCountry', e.target.value)}
              placeholder="Auto-populated..."
              required
            />
          </div>
          <div className={styles.fullWidth}>
            <div className={styles.fieldLabel}>
              Landline Phone <span className={styles.reqMark}>*</span>
            </div>
            <div className={styles.phoneGroup}>
              <select
                className={styles.phoneSelect}
                value={formData.commPhoneCode}
                onChange={(e) => handleFieldChange('commPhoneCode', e.target.value)}
              >
                {COUNTRY_CODES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
              <input
                className={styles.phoneInput}
                type="text"
                value={formData.commCityCode}
                onChange={(e) => handleFieldChange('commCityCode', e.target.value)}
                placeholder="City code"
                style={{ width: 100 }}
              />
              <input
                className={styles.phoneInputMain}
                type="tel"
                value={formData.commPhone}
                onChange={(e) => handleFieldChange('commPhone', e.target.value)}
                placeholder="Phone number"
              />
            </div>
            {errors.commPhone && <div className={styles.errorText}>{errors.commPhone}</div>}
          </div>
        </>
      )}
    </div>
  );

  // ─── Step 3: Contact Info ───
  const renderContact = () => (
    <div className={styles.formGrid}>
      {/* Primary Contact */}
      <div className={`${styles.sectionDividerLabel} ${styles.fullWidth}`}>Primary Contact</div>
      <Input
        label="Full Name"
        value={formData.primaryFullName}
        onChange={(e) => handleFieldChange('primaryFullName', e.target.value)}
        placeholder="Contact person name"
        required
        error={errors.primaryFullName}
      />
      <Input
        label="Official Email"
        type="email"
        value={formData.primaryEmail}
        onChange={(e) => handleFieldChange('primaryEmail', e.target.value)}
        placeholder="name@institution.com"
        required
        error={errors.primaryEmail}
      />
      <div>
        <div className={styles.fieldLabel}>
          Mobile Number <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.phoneGroup}>
          <select
            className={styles.phoneSelect}
            value={formData.primaryMobileCode}
            onChange={(e) => handleFieldChange('primaryMobileCode', e.target.value)}
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
          <input
            className={styles.phoneInputMain}
            type="tel"
            value={formData.primaryMobile}
            onChange={(e) => handleMobileChange('primaryMobile', e.target.value)}
            maxLength={10}
            placeholder="Mobile number"
          />
        </div>
        {errors.primaryMobile && <div className={styles.errorText}>{errors.primaryMobile}</div>}
      </div>
      <div>
        <div className={styles.fieldLabel}>
          Alternate Mobile <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.phoneGroup}>
          <select
            className={styles.phoneSelect}
            value={formData.primaryAltMobileCode}
            onChange={(e) => handleFieldChange('primaryAltMobileCode', e.target.value)}
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
          <input
            className={styles.phoneInputMain}
            type="tel"
            value={formData.primaryAltMobile}
            onChange={(e) => handleMobileChange('primaryAltMobile', e.target.value)}
            maxLength={10}
            placeholder="Alternate number"
          />
        </div>
        {errors.primaryAltMobile && <div className={styles.errorText}>{errors.primaryAltMobile}</div>}
      </div>

      {/* Divider */}
      <div className={`${styles.fullWidth} ${styles.addrDivider}`} />

      {/* Secondary Contact */}
      <div className={`${styles.sectionDividerLabel} ${styles.fullWidth}`}>Secondary Contact</div>
      <Input
        label="Full Name"
        value={formData.secondaryFullName}
        onChange={(e) => handleFieldChange('secondaryFullName', e.target.value)}
        placeholder="Contact person name"
        required
        error={errors.secondaryFullName}
      />
      <Input
        label="Official Email"
        type="email"
        value={formData.secondaryEmail}
        onChange={(e) => handleFieldChange('secondaryEmail', e.target.value)}
        placeholder="name@institution.com"
        required
        error={errors.secondaryEmail}
      />
      <div>
        <div className={styles.fieldLabel}>
          Mobile Number <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.phoneGroup}>
          <select
            className={styles.phoneSelect}
            value={formData.secondaryMobileCode}
            onChange={(e) => handleFieldChange('secondaryMobileCode', e.target.value)}
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
          <input
            className={styles.phoneInputMain}
            type="tel"
            value={formData.secondaryMobile}
            onChange={(e) => handleMobileChange('secondaryMobile', e.target.value)}
            maxLength={10}
            placeholder="Mobile number"
          />
        </div>
        {errors.secondaryMobile && <div className={styles.errorText}>{errors.secondaryMobile}</div>}
      </div>
      <div>
        <div className={styles.fieldLabel}>
          Alternate Mobile <span className={styles.reqMark}>*</span>
        </div>
        <div className={styles.phoneGroup}>
          <select
            className={styles.phoneSelect}
            value={formData.secondaryAltMobileCode}
            onChange={(e) => handleFieldChange('secondaryAltMobileCode', e.target.value)}
          >
            {COUNTRY_CODES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
          <input
            className={styles.phoneInputMain}
            type="tel"
            value={formData.secondaryAltMobile}
            onChange={(e) => handleMobileChange('secondaryAltMobile', e.target.value)}
            maxLength={10}
            placeholder="Alternate number"
          />
        </div>
        {errors.secondaryAltMobile && <div className={styles.errorText}>{errors.secondaryAltMobile}</div>}
      </div>
    </div>
  );

  // ─── Step 4: Product Features ───
  const renderProducts = () => (
    <div>
      <div className={styles.productGrid}>
        {PRODUCTS.map((product) => {
          const isSelected = (formData.selectedProducts || []).includes(product.name);
          return (
            <div
              key={product.name}
              className={`${styles.productCard} ${isSelected ? styles.productCardSelected : ''}`}
              onClick={() => toggleProduct(product.name)}
            >
              <div className={styles.productTop}>
                <div className={styles.productIcon}>{product.icon}</div>
                <div className={`${styles.productCheck} ${isSelected ? styles.productCheckSelected : ''}`}>
                  {isSelected && <Check size={12} />}
                </div>
              </div>
              <div className={styles.productName}>{product.name}</div>
              {product.variants && isSelected && (
                <div className={styles.variantChips} onClick={(e) => e.stopPropagation()}>
                  {product.variants.map((v) => {
                    const isVarOn = (formData.selectedVariants?.[product.name] || []).includes(v);
                    return (
                      <button
                        key={v}
                        type="button"
                        className={`${styles.variantChip} ${isVarOn ? styles.variantChipOn : ''}`}
                        onClick={() => toggleVariant(product.name, v)}
                      >
                        {v}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );

  // ─── Step 5: Security & Compliance ───
  const renderSecurity = () => (
    <div className={styles.securityList}>
      <SecurityToggle
        label="Multi-Factor Authentication (MFA)"
        checked={formData.enableMFA}
        onChange={(v) => handleFieldChange('enableMFA', v)}
      />
      <SecurityToggle
        label="IDAM Integration"
        checked={formData.enableHRMS}
        onChange={(v) => handleFieldChange('enableHRMS', v)}
      />
      <SecurityToggle
        label="OTP Verification"
        checked={formData.enableOTP}
        onChange={(v) => handleFieldChange('enableOTP', v)}
      />
    </div>
  );

  // ─── Review (shown on last step) ───
  const renderReview = () => {
    const data = formData;
    return (
      <div className={styles.summaryContainer}>
        {SUMMARY_SECTIONS.map((section) => (
          <div key={section.title} className={styles.summarySection}>
            <h3 className={styles.summarySectionTitle}>{section.title}</h3>
            <div className={styles.summaryGrid}>
              {section.fields.map((field) => (
                <div key={field.key} className={styles.summaryItem}>
                  <span className={styles.summaryLabel}>{field.label}</span>
                  {field.type === 'bool' ? (
                    <span className={`${styles.badge} ${data[field.key] ? styles.badgeEnabled : styles.badgeDisabled}`}>
                      {data[field.key] ? 'Enabled' : 'Disabled'}
                    </span>
                  ) : (
                    <span className={styles.summaryValue}>
                      {getDisplayValue(data[field.key], field, data)}
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderStepContent = () => {
    switch (currentStep) {
      case 'institution': return renderInstitutionDetails();
      case 'address': return renderAddress();
      case 'contact': return renderContact();
      case 'products': return renderProducts();
      case 'security': return renderSecurity();
      case 'review': return renderReview();
      default: return null;
    }
  };

  const isLastStep = currentStepIndex === STEPS.length - 1;
  const isFirstStep = currentStepIndex === 0;

  // ═══════════════════════════════════════
  //  VIEW: Detail
  // ═══════════════════════════════════════
  if (view === 'detail' && selectedInstitution) {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBackToList} className={styles.backBtn}>
            Back to Institutions
          </Button>
          <h1>{selectedInstitution.institutionNameFull}</h1>
          <p>{selectedInstitution.institutionNameShort ? `(${selectedInstitution.institutionNameShort})` : 'Institution Details'}</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          {SUMMARY_SECTIONS.map((section) => (
            <div key={section.title} className={styles.viewSection}>
              <div className={styles.viewSectionHeader}>{section.title}</div>
              <div className={styles.viewGrid}>
                {section.fields.map((field) => (
                  <div key={field.key} className={styles.viewItem}>
                    <span className={styles.viewLabel}>{field.label}</span>
                    {field.type === 'bool' ? (
                      <span className={`${styles.badge} ${selectedInstitution[field.key] ? styles.badgeEnabled : styles.badgeDisabled}`}>
                        {selectedInstitution[field.key] ? 'Enabled' : 'Disabled'}
                      </span>
                    ) : (
                      <span className={styles.viewValue}>
                        {getDisplayValue(selectedInstitution[field.key], field, selectedInstitution)}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}

          <div className={styles.viewActions}>
            <Button variant="secondary" onClick={handleBackToList}>Back</Button>
            <Button variant="gold" leftIcon={<Pencil size={16} />} onClick={() => handleEdit(selectedInstitution)}>
              Edit Institution
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ═══════════════════════════════════════
  //  VIEW: Add / Edit Form
  // ═══════════════════════════════════════
  if (view === 'add' || view === 'edit') {
    return (
      <div className={styles.page}>
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
          <Button variant="ghost" size="sm" leftIcon={<ArrowLeft size={18} />} onClick={handleBackToList} className={styles.backBtn}>
            Back to Institutions
          </Button>
          <h1>{view === 'edit' ? 'Update Institution' : 'Add New Institution'}</h1>
          <p>{view === 'edit' ? `Editing: ${selectedInstitution?.institutionNameFull}` : 'Set up a new institution for the reconciliation platform'}</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
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
                <Button variant="secondary" onClick={handlePrev} leftIcon={<ArrowLeft size={16} />}>
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
                  {submitting ? 'Submitting...' : view === 'edit' ? 'Update Institution' : 'Submit Onboarding Form'}
                </Button>
              ) : (
                <Button variant="primary" onClick={handleNext} rightIcon={<ArrowRight size={16} />}>
                  Next
                </Button>
              )}
            </div>
          </Card>
        </motion.div>
      </div>
    );
  }

  // ═══════════════════════════════════════
  //  VIEW: List (default)
  // ═══════════════════════════════════════
  return (
    <div className={styles.page}>
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className={styles.header}>
        <h1>Institution Onboarding</h1>
        <p>Manage onboarded institutions</p>
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <Card className={styles.tableCard}>
          <div className={styles.tableControls}>
            <div className={styles.showEntries}>
              <span>Showing {filteredInstitutions.length} of {institutions.length} institutions</span>
            </div>
            <div className={styles.controlsRight}>
              <div className={styles.searchBox}>
                <Search size={16} className={styles.searchIcon} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search institutions..."
                />
              </div>
              <Button variant="gold" size="sm" leftIcon={<Plus size={16} />} onClick={handleAddNew}>
                Add Institution
              </Button>
            </div>
          </div>

          <div className={styles.tableContainer}>
            <table className={styles.mainTable}>
              <thead>
                <tr>
                  <th className={styles.sortable} onClick={() => handleSort('institutionNameFull')}>
                    <div className={styles.thContent}>
                      <span>Institution Name</span>
                      {getSortIcon('institutionNameFull')}
                    </div>
                  </th>
                  <th className={styles.sortable} onClick={() => handleSort('regCity')}>
                    <div className={styles.thContent}>
                      <span>City</span>
                      {getSortIcon('regCity')}
                    </div>
                  </th>
                  <th className={styles.sortable} onClick={() => handleSort('primaryFullName')}>
                    <div className={styles.thContent}>
                      <span>Contact Person</span>
                      {getSortIcon('primaryFullName')}
                    </div>
                  </th>
                  <th className={styles.sortable} onClick={() => handleSort('status')}>
                    <div className={styles.thContent}>
                      <span>Status</span>
                      {getSortIcon('status')}
                    </div>
                  </th>
                  <th className={styles.sortable} onClick={() => handleSort('createdDate')}>
                    <div className={styles.thContent}>
                      <span>Onboarded</span>
                      {getSortIcon('createdDate')}
                    </div>
                  </th>
                  <th className={styles.actionCol}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sortedInstitutions.length === 0 ? (
                  <tr>
                    <td colSpan={6} className={styles.emptyCell}>
                      <div className={styles.emptyState}>
                        <Building2 size={40} className={styles.emptyIcon} />
                        <span>No institutions found</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  sortedInstitutions.map((inst) => (
                    <tr key={inst.id} onClick={() => handleViewDetail(inst)}>
                      <td>
                        <div className={styles.instNameCell}>
                          <span className={styles.instName}>{inst.institutionNameFull}</span>
                          {inst.institutionNameShort && (
                            <span className={styles.instShortName}>{inst.institutionNameShort}</span>
                          )}
                        </div>
                      </td>
                      <td>{inst.regCity || '—'}</td>
                      <td>
                        <div className={styles.contactCell}>
                          <span>{inst.primaryFullName || '—'}</span>
                          <span className={styles.contactEmail}>{inst.primaryEmail || ''}</span>
                        </div>
                      </td>
                      <td>
                        <span className={`${styles.statusBadge} ${getStatusClass(inst.status)}`}>
                          {inst.status}
                        </span>
                      </td>
                      <td>{inst.createdDate ? formatDate(inst.createdDate) : '—'}</td>
                      <td className={styles.actionCol} onClick={(e) => e.stopPropagation()}>
                        <div className={styles.actionBtns}>
                          <button className={`${styles.iconBtn} ${styles.iconBtnView}`} title="View" onClick={() => handleViewDetail(inst)}>
                            <Eye size={15} />
                          </button>
                          <button className={`${styles.iconBtn} ${styles.iconBtnEdit}`} title="Edit" onClick={() => handleEdit(inst)}>
                            <Pencil size={15} />
                          </button>
                          <button className={`${styles.iconBtn} ${styles.iconBtnDelete}`} title="Delete" onClick={() => handleDelete(inst)}>
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
        </Card>
      </motion.div>
    </div>
  );
};

export default InstitutionOnboarding;
