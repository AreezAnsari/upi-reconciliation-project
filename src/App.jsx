import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from './components/layout';
import ProtectedRoute from './components/auth/ProtectedRoute';

// Auth Pages
import { Login } from './pages/auth';

// Main Pages
import Dashboard from './pages/Dashboard';

// Admin Pages
import AddMenu from './pages/admin/AddMenu';
import AddUser from './pages/admin/AddUser';
import UserApproval from './pages/admin/UserApproval';
import TemplateConfig from './pages/admin/TemplateConfig';
import RoleManagement from './pages/admin/RoleManagement';
import InstitutionOnboarding from './pages/admin/InstitutionOnboarding';

// Setup Pages
import FileConfig from './pages/setup/FileConfig';
import ProcessDefinition from './pages/setup/ProcessDefinition';
import ReportConfig from './pages/setup/ReportConfig';
import FTPConfig from './pages/setup/FTPConfig';
import ForceMatchConfig from './pages/setup/ForceMatchConfig';
import ReconConfig from './pages/setup/ReconConfig';

// Report Pages
import GenerateReport from './pages/reports/GenerateReport';
import TTUMReport from './pages/reports/TTUMReport';
import ExtractionDetails from './pages/reports/ExtractionDetails';

// Process Pages
import ExtractionSearch from './pages/process/ExtractionSearch';
import BulkProcessMatch from './pages/process/BulkProcessMatch';
import FileUpload from './pages/process/FileUpload';
import SplitTransaction from './pages/process/SplitTransaction';
import ReconSearch from './pages/process/ReconSearch';

// Reconciliation Pages
import AutoReconciliation from './pages/reconciliation/AutoReconciliation';
import ManualReconciliation from './pages/reconciliation/ManualReconciliation';
import ReconciliationStatus from './pages/reconciliation/ReconciliationStatus';
import ReconciliationFileProcess from './pages/reconciliation/ReconciliationFileProcess';

// Extraction Pages
import FileProcessing from './pages/extraction/FileProcessing';
import DataExtraction from './pages/extraction/DataExtraction';
import ExtractionFileProcess from './pages/extraction/ExtractionFileProcess';

// Dispute Management Pages
import DisputeDashboard from './pages/dispute/DisputeDashboard';
import DisputeActionCenter from './pages/dispute/DisputeActionCenter';
import MakerUpload from './pages/dispute/MakerUpload';
import CheckerApproval from './pages/dispute/CheckerApproval';

// UPI Recon Dashboard
import UpiReconDashboard from './pages/upiRecon/UpiReconDashboard';

// New Configuration Pages
import RoleList from './pages/newconfig/RoleList';
import AddRole from './pages/newconfig/AddRole';
import ViewRole from './pages/newconfig/ViewRole';
import RoleApprovalSent from './pages/newconfig/RoleApprovalSent';
import MakerQueue from './pages/newconfig/MakerQueue';
import CheckerQueue from './pages/newconfig/CheckerQueue';
import AddTemplate from './pages/newconfig/AddTemplate';
import DefineReconciliation from './pages/newconfig/DefineReconciliation';
import AddSubInstitute from './pages/SubInstitute/AddSubInstitute';
function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />
        
        {/* Protected Routes with Layout */}
        <Route path="/" element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }>
          {/* Dashboard */}
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="home" element={<Dashboard />} />
          
          {/* Admin Routes */}
          <Route path="admin/add-menu" element={<AddMenu />} />
          <Route path="admin/add-user" element={<AddUser />} />
          <Route path="admin/user-approval" element={<UserApproval />} />
          <Route path="admin/add-roles" element={<RoleManagement />} />
          {/* SubInstitute Route */}
<Route path="admin/sub-institute" element={<AddSubInstitute />} />
          <Route path="admin/institution-onboarding" element={<InstitutionOnboarding />} />
          {/* Setup Routes */}
          <Route path="setup/template-config" element={<TemplateConfig />} />
          <Route path="setup/file-config" element={<FileConfig />} />
          <Route path="setup/process-definition" element={<ProcessDefinition />} />
          <Route path="setup/report-config" element={<ReportConfig />} />
          <Route path="setup/ftp-config" element={<FTPConfig />} />
          <Route path="setup/force-match" element={<ForceMatchConfig />} />
          <Route path="setup/recon-config" element={<ReconConfig />} />
          
          {/* Report Routes */}
          <Route path="reports/generate" element={<GenerateReport />} />
          <Route path="reports/ttum" element={<TTUMReport />} />
          <Route path="reports/reconciliation" element={<GenerateReport />} />
          <Route path="reports/extraction-details" element={<ExtractionDetails />} />
          
          {/* Process Routes */}
          <Route path="process/extraction-search" element={<ExtractionSearch />} />
          <Route path="process/bulk-match" element={<BulkProcessMatch />} />
          <Route path="process/file-upload" element={<FileUpload />} />
          <Route path="process/split-transaction" element={<SplitTransaction />} />
          <Route path="process/recon-search" element={<ReconSearch />} />
          
          {/* Reconciliation Routes */}
          <Route path="reconciliation/auto" element={<AutoReconciliation />} />
          <Route path="reconciliation/manual" element={<ManualReconciliation />} />
          <Route path="reconciliation/status" element={<ReconciliationStatus />} />
          <Route path="reconciliation/fileProcessing.extr" element={<ReconciliationFileProcess />} />
          
          {/* Extraction Routes */}
          <Route path="extraction/file-processing" element={<FileProcessing />} />
          <Route path="extraction/data" element={<DataExtraction />} />
          <Route path="extraction/fileProcessing.extr" element={<ExtractionFileProcess />} />

          {/* Dispute Management Routes */}
          <Route path="dispute/dashboard" element={<DisputeDashboard />} />
          <Route path="dispute/action-center" element={<DisputeActionCenter />} />
          <Route path="dispute/maker-upload" element={<MakerUpload />} />
          <Route path="dispute/checker-approval" element={<CheckerApproval />} />

          {/* UPI Recon Routes */}
          <Route path="upi-recon/dashboard" element={<UpiReconDashboard />} />

          {/* New Configuration Routes */}
          <Route path="new-config/role-list" element={<RoleList />} />
          <Route path="new-config/add-role" element={<AddRole />} />
          <Route path="new-config/view-role" element={<ViewRole />} />
          <Route path="new-config/role-approval-sent" element={<RoleApprovalSent />} />
          <Route path="new-config/maker-queue" element={<MakerQueue />} />
          <Route path="new-config/checker-queue" element={<CheckerQueue />} />
          <Route path="new-config/add-template" element={<AddTemplate />} />
          <Route path="new-config/reconciliation" element={<DefineReconciliation />} />
        </Route>
        
        {/* Catch all - redirect to dashboard */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
