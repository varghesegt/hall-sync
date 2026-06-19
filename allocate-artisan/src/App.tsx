import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";

// Pages
import Index from "./pages/Index.tsx";
import Dashboard from "./pages/Dashboard.tsx";
import Login from "./pages/Login.tsx";
import AdminLogin from "./pages/AdminLogin.tsx";
import ForgotPassword from "./pages/ForgotPassword.tsx";
import ResetPassword from "./pages/ResetPassword.tsx";
import AdminDashboard from "./pages/AdminDashboard.tsx";
import InternalDashboard from "./pages/InternalDashboard.tsx";
import NotFound from "./pages/NotFound.tsx";
import ProtectedRoute from "./components/ProtectedRoute.tsx";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";

// Enterprise Modules
import AnalyticsDashboard from "./pages/AnalyticsDashboard.tsx";
import ExamHistory from "./pages/ExamHistory.tsx";
import FacultyManagement from "./pages/FacultyManagement.tsx";
import MalpracticeTracker from "./pages/MalpracticeTracker.tsx";
import DutyAllocation from "./pages/DutyAllocation.tsx";
import AccreditationReports from "./pages/AccreditationReports.tsx";
import Settings from "./pages/Settings.tsx";
import ClaimsDashboard from "./pages/claims/ClaimsDashboard.tsx";
import ClaimsList from "./pages/claims/ClaimsList.tsx";
import CollegeDistanceDB from "./pages/claims/CollegeDistanceDB.tsx";

// New Expansion Modules
import { CommandCenter } from "./pages/admin/CommandCenter.tsx";
import { AuditCenter } from "./pages/admin/AuditCenter.tsx";
import { FacultyWorkload } from "./pages/admin/FacultyWorkload.tsx";
import { QuestionPaperSecurity } from "./pages/admin/QuestionPaperSecurity.tsx";
import { CommunicationCenter } from "./pages/admin/CommunicationCenter.tsx";
import { ErpImportCenter } from "./pages/admin/ErpImportCenter.tsx";
import ExamLifecycle from "./pages/admin/ExamLifecycle";
import ManagementDashboard from "./pages/admin/ManagementDashboard";
import ComplianceVault from "./pages/admin/ComplianceVault";
import HallManagement from "./pages/admin/HallManagement";
import FloorPlanEditor from "./pages/FloorPlanEditor.tsx";
import FloorPlanSelect from "./pages/FloorPlanSelect.tsx";

// Public SaaS Pages
import PublicLayout from "./components/layout/PublicLayout.tsx";
import Home from "./pages/public/Home.tsx";
import About from "./pages/public/About.tsx";
import Contact from "./pages/public/Contact.tsx";
import Pricing from "./pages/public/Pricing.tsx";
import Payment from "./pages/public/Payment.tsx";

import { NetworkOfflineOverlay } from "@/components/ui/NetworkOfflineOverlay";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <NetworkOfflineOverlay />
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          {/* Public SaaS Routes */}
          <Route element={<PublicLayout />}>
            <Route path="/" element={<Home />} />
            <Route path="/about" element={<About />} />
            <Route path="/contact" element={<Contact />} />
            <Route path="/pricing" element={<Pricing />} />
            <Route path="/payment" element={<Payment />} />
          </Route>

          {/* Authentication Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/admin-login" element={<AdminLogin />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Admin Protected Routes */}
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute allowedRole="ROLE_SUPER_ADMIN">
                <AdminDashboard />
              </ProtectedRoute>
            } 
          />

          {/* COE Protected Routes */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute allowedRole="ROLE_COLLEGE_ADMIN">
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<AnalyticsDashboard />} />
            <Route path="semester" element={<Dashboard />} />
            <Route path="internal" element={<InternalDashboard />} />
            <Route path="history" element={<ExamHistory />} />
            <Route path="faculty" element={<FacultyManagement />} />
            <Route path="malpractice" element={<MalpracticeTracker />} />
            <Route path="duty" element={<DutyAllocation />} />
            <Route path="reports" element={<AccreditationReports />} />
            <Route path="settings" element={<Settings />} />
            <Route path="claims" element={<ClaimsDashboard />} />
            <Route path="claims/:id" element={<ClaimsList />} />
            <Route path="colleges" element={<CollegeDistanceDB />} />
            
            <Route path="command-center" element={<CommandCenter />} />
            <Route path="audit-center" element={<AuditCenter />} />
            <Route path="workload" element={<FacultyWorkload />} />
            <Route path="qp-security" element={<QuestionPaperSecurity />} />
            <Route path="communications" element={<CommunicationCenter />} />
            <Route path="import" element={<ErpImportCenter />} />
            <Route path="dashboard" element={<ManagementDashboard />} />
            <Route path="lifecycle" element={<ExamLifecycle />} />
            <Route path="compliance-vault" element={<ComplianceVault />} />
            <Route path="halls" element={<HallManagement />} />
            <Route path="floor-plan-select" element={<FloorPlanSelect />} />
            <Route path="floor-plan/:batchId" element={<FloorPlanEditor />} />
          </Route>

          {/* Legacy Redirects */}
          <Route path="/analytics" element={<Navigate to="/dashboard" replace />} />
          <Route path="/semester" element={<Navigate to="/dashboard/semester" replace />} />
          <Route path="/internal" element={<Navigate to="/dashboard/internal" replace />} />
          <Route path="/history" element={<Navigate to="/dashboard/history" replace />} />
          <Route path="/faculty" element={<Navigate to="/dashboard/faculty" replace />} />
          <Route path="/malpractice" element={<Navigate to="/dashboard/malpractice" replace />} />
          <Route path="/duties" element={<Navigate to="/dashboard/duty" replace />} />
          <Route path="/reports" element={<Navigate to="/dashboard/reports" replace />} />
          <Route path="/settings" element={<Navigate to="/dashboard/settings" replace />} />

          {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;

