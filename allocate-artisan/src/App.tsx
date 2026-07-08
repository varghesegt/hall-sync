import React, { Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";

// ── Core (loaded eagerly — needed on first render) ─────────────────────
import ProtectedRoute from "./components/ProtectedRoute.tsx";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";
import PublicLayout from "./components/layout/PublicLayout.tsx";
import { NetworkOfflineOverlay } from "@/components/ui/NetworkOfflineOverlay";

// ── Lazy-loaded Pages (code-split for 5x faster initial load) ──────────
// Auth & Core
const Index = React.lazy(() => import("./features/core/pages/Index.tsx"));
const Login = React.lazy(() => import("./features/core/pages/Login.tsx"));
const AdminLogin = React.lazy(() => import("./features/core/pages/AdminLogin.tsx"));
const ForgotPassword = React.lazy(() => import("./features/core/pages/ForgotPassword.tsx"));
const ResetPassword = React.lazy(() => import("./features/core/pages/ResetPassword.tsx"));
const Dashboard = React.lazy(() => import("./features/core/pages/Dashboard.tsx"));
const Settings = React.lazy(() => import("./features/core/pages/Settings.tsx"));
const NotFound = React.lazy(() => import("./features/core/pages/NotFound.tsx"));

// Allocations Domain
const InternalDashboard = React.lazy(() => import("./features/allocations/pages/InternalDashboard.tsx"));
const FacultyManagement = React.lazy(() => import("./features/allocations/pages/FacultyManagement.tsx"));
const DutyAllocation = React.lazy(() => import("./features/allocations/pages/DutyAllocation.tsx"));
const FloorPlanEditor = React.lazy(() => import("./features/allocations/pages/FloorPlanEditor.tsx"));
const FloorPlanSelect = React.lazy(() => import("./features/allocations/pages/FloorPlanSelect.tsx"));
const AppointmentOrders = React.lazy(() => import("./features/allocations/pages/AppointmentOrders.tsx"));

// Claims Domain
const ClaimsDashboard = React.lazy(() => import("./features/claims/pages/ClaimsDashboard.tsx"));
const ClaimsList = React.lazy(() => import("./features/claims/pages/ClaimsList.tsx"));
const CollegeDistanceDB = React.lazy(() => import("./features/claims/pages/CollegeDistanceDB.tsx"));

// Analytics & Admin Domain
const AnalyticsDashboard = React.lazy(() => import("./features/analytics/pages/AnalyticsDashboard.tsx"));
const ExamHistory = React.lazy(() => import("./features/analytics/pages/ExamHistory.tsx"));
const MalpracticeTracker = React.lazy(() => import("./features/analytics/pages/MalpracticeTracker.tsx"));
const AccreditationReports = React.lazy(() => import("./features/analytics/pages/AccreditationReports.tsx"));
const AdminDashboard = React.lazy(() => import("./features/analytics/pages/AdminDashboard.tsx"));
const CommandCenter = React.lazy(() => import("./features/analytics/pages/CommandCenter.tsx").then(m => ({ default: m.CommandCenter })));
const AuditCenter = React.lazy(() => import("./features/analytics/pages/AuditCenter.tsx").then(m => ({ default: m.AuditCenter })));
const QuestionPaperSecurity = React.lazy(() => import("./features/analytics/pages/QuestionPaperSecurity.tsx").then(m => ({ default: m.QuestionPaperSecurity })));
const CommunicationCenter = React.lazy(() => import("./features/analytics/pages/CommunicationCenter.tsx").then(m => ({ default: m.CommunicationCenter })));
const ErpImportCenter = React.lazy(() => import("./features/analytics/pages/ErpImportCenter.tsx").then(m => ({ default: m.ErpImportCenter })));
const ExamLifecycle = React.lazy(() => import("./features/analytics/pages/ExamLifecycle"));
const ManagementDashboard = React.lazy(() => import("./features/analytics/pages/ManagementDashboard"));
const ComplianceVault = React.lazy(() => import("./features/analytics/pages/ComplianceVault"));
const HallManagement = React.lazy(() => import("./features/analytics/pages/HallManagement"));

// Public SaaS Pages
const Home = React.lazy(() => import("./pages/public/Home.tsx"));
const About = React.lazy(() => import("./pages/public/About.tsx"));
const Contact = React.lazy(() => import("./pages/public/Contact.tsx"));
const Pricing = React.lazy(() => import("./pages/public/Pricing.tsx"));
const Payment = React.lazy(() => import("./pages/public/Payment.tsx"));
const PublicRemunerationForm = React.lazy(() => import("./pages/public/PublicRemunerationForm.tsx"));

// ── Loading Spinner for Suspense boundaries ────────────────────────────
const PageLoader = () => (
  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100vh", background: "#0a0a0a" }}>
    <div style={{ width: 36, height: 36, border: "3px solid #333", borderTopColor: "#fff", borderRadius: "50%", animation: "spin 0.8s linear infinite" }} />
    <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
  </div>
);


const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <NetworkOfflineOverlay />
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Public SaaS Routes */}
          <Route element={<PublicLayout />}>
            <Route path="/" element={<Home />} />
            <Route path="/about" element={<About />} />
            <Route path="/contact" element={<Contact />} />
            <Route path="/pricing" element={<Pricing />} />
            <Route path="/payment" element={<Payment />} />
          </Route>
          
          <Route path="/public/remuneration/:batchId" element={<PublicRemunerationForm />} />

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
            <Route path="appointments" element={<AppointmentOrders />} />
            <Route path="reports" element={<AccreditationReports />} />
            <Route path="settings" element={<Settings />} />
            <Route path="claims" element={<ClaimsDashboard />} />
            <Route path="claims/:id" element={<ClaimsList />} />
            <Route path="colleges" element={<CollegeDistanceDB />} />
            
            <Route path="command-center" element={<CommandCenter />} />
            <Route path="audit-center" element={<AuditCenter />} />
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
        </Suspense>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;

