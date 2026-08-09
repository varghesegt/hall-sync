import React, { useEffect, useState, useMemo } from "react";
import { labClaimsApi, LabClaimRecord } from "@/api/labClaimsApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import {
  FileSpreadsheet,
  Plus,
  Trash2,
  FileText,
  UploadCloud,
  Search,
  Filter,
  IndianRupee,
  Users,
  Building2,
  Calendar,
  CalendarClock,
  Layers,
  Sparkles,
  Download,
  AlertTriangle,
  Briefcase,
  RefreshCw,
  Clock,
  CheckCircle2,
  UserCheck,
  Award,
  BookOpen,
  ClipboardList,
  Phone,
  X,
  ChevronRight,
  TrendingUp
} from "lucide-react";

// Safe string converter that handles raw objects (e.g. Jackson serialized LocalDate objects) cleanly
const safeString = (val: any, fallback = ""): string => {
  if (val === null || val === undefined) return fallback;
  if (typeof val === "string") return val;
  if (typeof val === "number" || typeof val === "boolean") return String(val);
  try {
    if (typeof val === "object") {
      if (val.year && (val.monthValue || val.month) && (val.dayOfMonth || val.day)) {
        const m = val.monthValue || val.month;
        const d = val.dayOfMonth || val.day;
        return `${val.year}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
      }
      return JSON.stringify(val);
    }
    return String(val);
  } catch {
    return fallback;
  }
};

function LabClaimsDashboardContent() {
  const [claims, setClaims] = useState<LabClaimRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeView, setActiveView] = useState<"SESSIONS" | "LEDGER" | "ALL" | "EXTERNAL" | "INTERNAL" | "TECH">("SESSIONS");

  // Ledger Filter States
  const [ledgerSeason, setLedgerSeason] = useState<string>("ALL");
  const [ledgerDate, setLedgerDate] = useState<string>("ALL");

  // Upload Modal State
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [examSeason, setExamSeason] = useState("APR/MAY 2026");
  const [examDate, setExamDate] = useState("2026-03-30");
  const [subjectCode, setSubjectCode] = useState("UCS1811");
  const [uploading, setUploading] = useState(false);
  const [warnings, setWarnings] = useState<string[]>([]);

  const getErrorMessage = (err: any): string => {
    if (!err) return "An error occurred";
    if (typeof err === "string") return err;
    if (err.response?.data) {
      const data = err.response.data;
      if (typeof data === "string") return data;
      if (typeof data.message === "string") return data.message;
      if (typeof data.error === "string") return data.error;
    }
    if (typeof err.message === "string") return err.message;
    return "Operation failed";
  };

  const fetchClaims = async () => {
    setLoading(true);
    try {
      const data = await labClaimsApi.getClaims();
      setClaims(Array.isArray(data) ? data : []);
    } catch (err: any) {
      toast.error("Failed to fetch lab claims: " + getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClaims();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUploadExcel = async () => {
    if (!examSeason || !examSeason.trim()) {
      toast.error("Exam Season is required before uploading Excel.");
      return;
    }
    if (!examDate || !examDate.trim()) {
      toast.error("Exam Date is required before uploading Excel.");
      return;
    }
    if (!subjectCode || !subjectCode.trim()) {
      toast.error("Lab / Subject Code is required before uploading Excel.");
      return;
    }
    if (!selectedFile) {
      toast.error("Please select an Excel file to upload.");
      return;
    }
    setUploading(true);
    setWarnings([]);
    try {
      const res = await labClaimsApi.uploadExcel(selectedFile, examSeason.trim(), examDate.trim(), subjectCode.trim());
      toast.success(res.message || "Lab claims imported successfully!");
      if (res.warnings && Array.isArray(res.warnings) && res.warnings.length > 0) {
        setWarnings(res.warnings.map((w) => safeString(w)));
      } else {
        setIsUploadOpen(false);
        setSelectedFile(null);
      }
      fetchClaims();
    } catch (err: any) {
      toast.error("Upload failed: " + getErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this lab claim record?")) return;
    try {
      await labClaimsApi.deleteClaim(id);
      toast.success("Lab claim record deleted.");
      fetchClaims();
    } catch (err: any) {
      toast.error("Failed to delete claim: " + getErrorMessage(err));
    }
  };

  const handleDeleteAllClaims = async () => {
    if (!confirm("Are you sure you want to DELETE ALL lab claims records? This action cannot be undone.")) return;
    try {
      const res = await labClaimsApi.deleteAllClaims();
      toast.success(res.message || "All lab claims records deleted successfully.");
      fetchClaims();
    } catch (err: any) {
      toast.error("Failed to delete all claims: " + getErrorMessage(err));
    }
  };

  const handleDownloadWordBySession = async (examDateStr?: string, codeStr?: string, claimId?: number) => {
    try {
      let blob: Blob;
      if (examDateStr && codeStr) {
        blob = await labClaimsApi.downloadSessionWord(examDateStr, codeStr);
      } else if (claimId) {
        blob = await labClaimsApi.downloadWord(claimId);
      } else {
        return;
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `Lab_Claim_Form_${codeStr || "Session"}.docx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Downloaded official 4-column Word (.docx) Claim Form!");
    } catch (err: any) {
      toast.error("Failed to download Word doc: " + getErrorMessage(err));
    }
  };

  const handleDownloadSampleTemplate = async () => {
    try {
      const blob = await labClaimsApi.downloadSampleTemplate();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "Comprehensive_Lab_Claims_Form_Responses.xlsx";
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Downloaded sample Google Form lab claims Excel template!");
    } catch (err: any) {
      toast.error("Failed to download template: " + getErrorMessage(err));
    }
  };

  // Group Claims by Examination Sessions (Exam Date + Subject Code + Department)
  interface LabSessionGroup {
    key: string;
    examSeason: string;
    examDate: string;
    subjectCode: string;
    subjectName: string;
    department: string;
    semester: string;
    session: string;
    registeredCount: number;
    presentCount: number;
    external?: LabClaimRecord;
    internal?: LabClaimRecord;
    skilled?: LabClaimRecord;
    tech?: LabClaimRecord;
    allRecords: LabClaimRecord[];
    totalSessionAmount: number;
  }

  const sessionGroups: LabSessionGroup[] = useMemo(() => {
    if (!Array.isArray(claims)) return [];
    const map = new Map<string, LabSessionGroup>();

    claims.forEach((c) => {
      if (!c) return;
      const dateStr = safeString(c.examDate, "NO_DATE");
      const codeStr = safeString(c.subjectCode, "NO_CODE").toUpperCase().trim();
      const deptStr = safeString(c.department, "NO_DEPT").toUpperCase().trim();
      const key = `${dateStr}_${codeStr}_${deptStr}`;

      if (!map.has(key)) {
        map.set(key, {
          key,
          examSeason: safeString(c.examSeason, "APR/MAY 2026"),
          examDate: safeString(c.examDate),
          subjectCode: safeString(c.subjectCode),
          subjectName: safeString(c.subjectName),
          department: safeString(c.department),
          semester: safeString(c.semester),
          session: safeString(c.session, "FN / AN"),
          registeredCount: Number(c.registeredCount || 0),
          presentCount: Number(c.presentCount || 0),
          allRecords: [],
          totalSessionAmount: 0
        });
      }

      const grp = map.get(key)!;
      grp.allRecords.push(c);

      const role = safeString(c.staffRole).toUpperCase().trim();
      if (!grp.external && role === "EXTERNAL_EXAMINER") grp.external = c;
      else if (!grp.internal && role === "INTERNAL_EXAMINER") grp.internal = c;
      else if (!grp.skilled && role === "SKILLED_ASSISTANT") grp.skilled = c;
      else if (!grp.tech && (role.includes("TECH") || role.includes("ATTENDER"))) grp.tech = c;
    });

    map.forEach((grp) => {
      const extAmt = Number(grp.external?.totalAmount || 0);
      const intAmt = Number(grp.internal?.totalAmount || 0);
      const skilledAmt = Number(grp.skilled?.totalAmount || 0);
      const techAmt = Number(grp.tech?.totalAmount || 0);
      grp.totalSessionAmount = extAmt + intAmt + skilledAmt + techAmt;
    });

    return Array.from(map.values());
  }, [claims]);

  // Extract Unique Seasons & Dates for Ledger Filtering
  const uniqueSeasons = useMemo(() => {
    const set = new Set<string>();
    sessionGroups.forEach((s) => {
      if (s.examSeason) set.add(s.examSeason);
    });
    return Array.from(set);
  }, [sessionGroups]);

  const uniqueDates = useMemo(() => {
    const set = new Set<string>();
    sessionGroups.forEach((s) => {
      if (ledgerSeason === "ALL" || s.examSeason === ledgerSeason) {
        if (s.examDate) set.add(s.examDate);
      }
    });
    return Array.from(set).sort();
  }, [sessionGroups, ledgerSeason]);

  // Filtered Session Groups by Search
  const filteredSessions = useMemo(() => {
    return sessionGroups.filter((s) => {
      const q = searchTerm.toLowerCase();
      return (
        s.subjectCode.toLowerCase().includes(q) ||
        s.subjectName.toLowerCase().includes(q) ||
        s.department.toLowerCase().includes(q) ||
        s.examDate.includes(q) ||
        (s.external && safeString(s.external.staffName).toLowerCase().includes(q)) ||
        (s.internal && safeString(s.internal.staffName).toLowerCase().includes(q))
      );
    });
  }, [sessionGroups, searchTerm]);

  // Ledger Groups (Grouped by Date)
  interface DateLedgerGroup {
    date: string;
    seasons: string[];
    sessions: LabSessionGroup[];
    totalSessionsCount: number;
    totalExamined: number;
    totalPayout: number;
  }

  const ledgerDateGroups: DateLedgerGroup[] = useMemo(() => {
    const map = new Map<string, DateLedgerGroup>();

    sessionGroups.forEach((s) => {
      if (ledgerSeason !== "ALL" && s.examSeason !== ledgerSeason) return;
      if (ledgerDate !== "ALL" && s.examDate !== ledgerDate) return;

      const q = searchTerm.toLowerCase();
      if (
        q &&
        !s.subjectCode.toLowerCase().includes(q) &&
        !s.subjectName.toLowerCase().includes(q) &&
        !s.department.toLowerCase().includes(q) &&
        !s.examDate.includes(q) &&
        (!s.external || !safeString(s.external.staffName).toLowerCase().includes(q)) &&
        (!s.internal || !safeString(s.internal.staffName).toLowerCase().includes(q))
      ) {
        return;
      }

      const d = s.examDate || "UNSCHEDULED";
      if (!map.has(d)) {
        map.set(d, {
          date: d,
          seasons: [],
          sessions: [],
          totalSessionsCount: 0,
          totalExamined: 0,
          totalPayout: 0
        });
      }

      const grp = map.get(d)!;
      grp.sessions.push(s);
      if (s.examSeason && !grp.seasons.includes(s.examSeason)) grp.seasons.push(s.examSeason);
      grp.totalSessionsCount += 1;
      grp.totalExamined += s.presentCount;
      grp.totalPayout += s.totalSessionAmount;
    });

    return Array.from(map.values()).sort((a, b) => a.date.localeCompare(b.date));
  }, [sessionGroups, ledgerSeason, ledgerDate, searchTerm]);

  // Filtered Individual Claims for Table View
  const filteredClaims = useMemo(() => {
    return (Array.isArray(claims) ? claims : []).filter((c) => {
      if (!c) return false;
      const q = searchTerm.toLowerCase();
      const matchesSearch =
        safeString(c.staffName).toLowerCase().includes(q) ||
        safeString(c.subjectCode).toLowerCase().includes(q) ||
        safeString(c.department).toLowerCase().includes(q) ||
        safeString(c.examDate).includes(q);

      if (!matchesSearch) return false;
      const role = safeString(c.staffRole);
      if (activeView === "INTERNAL") return role === "INTERNAL_EXAMINER";
      if (activeView === "EXTERNAL") return role === "EXTERNAL_EXAMINER";
      if (activeView === "TECH") return role === "SKILLED_ASSISTANT" || role === "LAB_ATTENDER";
      return true;
    });
  }, [claims, searchTerm, activeView]);

  // Calculate Summary Totals based on Active Session Examiner Claims
  const totalGrandAmount = sessionGroups.reduce((acc, s) => acc + s.totalSessionAmount, 0);
  const totalExaminersAmount = sessionGroups.reduce(
    (acc, s) => acc + Number(s.external?.totalAmount || 0) + Number(s.internal?.totalAmount || 0),
    0
  );
  const totalTechStaffAmount = sessionGroups.reduce(
    (acc, s) => acc + Number(s.skilled?.totalAmount || 0) + Number(s.tech?.totalAmount || 0),
    0
  );
  const totalClaimsCount = sessionGroups.reduce((acc, s) => {
    let count = 0;
    if (s.external) count++;
    if (s.internal) count++;
    if (s.skilled) count++;
    if (s.tech) count++;
    return acc + count;
  }, 0);

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto bg-slate-50/50 min-h-screen">
      {/* Top Title & Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
              <Briefcase className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Lab Claims & Remuneration</h1>
              <p className="text-sm text-slate-500 font-medium mt-0.5">
                Practical examination bills, session claim ledgers, external TA/DA, skilled assistants & lab attenders
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant="outline"
            onClick={fetchClaims}
            className="border-slate-200 bg-white text-slate-700 hover:bg-slate-50 shadow-sm font-semibold"
          >
            <RefreshCw className={`h-4 w-4 mr-2 text-slate-500 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>

          <Button
            variant="outline"
            onClick={handleDownloadSampleTemplate}
            className="border-indigo-200 bg-indigo-50/50 text-indigo-700 hover:bg-indigo-100 shadow-sm font-semibold text-xs"
          >
            <Download className="h-4 w-4 mr-2 text-indigo-600" />
            Excel Template
          </Button>

          <Button
            onClick={() => setIsUploadOpen(true)}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-sm shadow-indigo-200"
          >
            <UploadCloud className="h-4 w-4 mr-2" />
            Upload Excel
          </Button>

          <Button
            onClick={handleDeleteAllClaims}
            className="bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs shadow-sm shadow-rose-200"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Delete All Claims
          </Button>
        </div>
      </div>

      {/* Analytics Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Claims</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">{totalClaimsCount} Records</h3>
            <p className="text-xs text-indigo-600 font-semibold mt-1 flex items-center">
              <BookOpen className="h-3.5 w-3.5 mr-1" /> {sessionGroups.length} Lab Sessions
            </p>
          </div>
          <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl">
            <Briefcase className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Grand Remuneration</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">₹{totalGrandAmount.toLocaleString("en-IN")}</h3>
            <p className="text-xs text-emerald-600 font-semibold mt-1">Includes Remuneration + TA/DA</p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
            <IndianRupee className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Examiners (Ext / Int)</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">₹{totalExaminersAmount.toLocaleString("en-IN")}</h3>
            <p className="text-xs text-amber-600 font-semibold mt-1">External TA/DA included</p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
            <Award className="h-6 w-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Skilled Asst / Lab Attenders</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">₹{totalTechStaffAmount.toLocaleString("en-IN")}</h3>
            <p className="text-xs text-purple-600 font-semibold mt-1">Technical supporting staff</p>
          </div>
          <div className="p-3 bg-purple-50 text-purple-600 rounded-xl">
            <Users className="h-6 w-6" />
          </div>
        </div>
      </div>

      {/* Main View Mode Selector & Search Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
          <button
            onClick={() => setActiveView("SESSIONS")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 ${
              activeView === "SESSIONS"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            Lab Sessions ({sessionGroups.length})
          </button>

          <button
            onClick={() => setActiveView("LEDGER")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 ${
              activeView === "LEDGER"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <CalendarClock className="h-3.5 w-3.5" />
            Season & Date Ledger
          </button>

          <button
            onClick={() => setActiveView("ALL")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 ${
              activeView === "ALL"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <ClipboardList className="h-3.5 w-3.5" />
            All Records ({claims.length})
          </button>

          <button
            onClick={() => setActiveView("EXTERNAL")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all ${
              activeView === "EXTERNAL"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            External
          </button>

          <button
            onClick={() => setActiveView("INTERNAL")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all ${
              activeView === "INTERNAL"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            Internal
          </button>

          <button
            onClick={() => setActiveView("TECH")}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition-all ${
              activeView === "TECH"
                ? "bg-white text-indigo-600 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            Skilled Asst
          </button>
        </div>

        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Search Subject, Date, Staff..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 bg-slate-50 border-slate-200 text-xs rounded-xl"
          />
        </div>
      </div>

      {/* VIEW 1: PRACTICAL LAB SESSIONS CARDS */}
      {activeView === "SESSIONS" && (
        <div className="space-y-6">
          {loading ? (
            <div className="bg-white p-12 text-center rounded-2xl border border-slate-200">
              <RefreshCw className="h-8 w-8 text-indigo-600 animate-spin mx-auto mb-3" />
              <p className="text-sm font-semibold text-slate-600">Loading lab sessions...</p>
            </div>
          ) : filteredSessions.length === 0 ? (
            <div className="bg-white p-12 text-center rounded-2xl border border-slate-200">
              <FileSpreadsheet className="h-12 w-12 text-slate-300 mx-auto mb-3" />
              <h3 className="text-base font-bold text-slate-800">No Practical Examination Sessions Found</h3>
              <p className="text-xs text-slate-500 mt-1">Upload an Excel sheet to generate practical claim bills.</p>
            </div>
          ) : (
            filteredSessions.map((session) => (
              <div
                key={session.key}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all overflow-hidden"
              >
                {/* Session Card Header */}
                <div className="bg-slate-900 text-white p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="bg-indigo-500 text-white text-[11px] font-black px-2.5 py-0.5 rounded-full uppercase tracking-wider">
                        {safeString(session.subjectCode, "LAB CODE")}
                      </span>
                      <span className="bg-slate-800 text-slate-300 text-[11px] font-semibold px-2.5 py-0.5 rounded-full">
                        {safeString(session.department)} (Sem {safeString(session.semester, "I")})
                      </span>
                      <span className="bg-emerald-500/20 text-emerald-300 text-[11px] font-semibold px-2.5 py-0.5 rounded-full border border-emerald-500/30">
                        {safeString(session.session, "Session")}
                      </span>
                    </div>
                    <h2 className="text-lg font-black text-white mt-1">
                      {safeString(session.subjectName, "Practical Examination")}
                    </h2>
                    <div className="text-xs text-slate-400 flex items-center gap-4 mt-1 font-medium">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-indigo-400" /> Date: <strong className="text-white">{safeString(session.examDate)}</strong>
                      </span>
                      <span className="flex items-center gap-1">
                        <Users className="h-3.5 w-3.5 text-indigo-400" /> Candidates: <strong className="text-white">{session.presentCount} Examined</strong> ({session.registeredCount} Registered)
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Session Total</p>
                      <p className="text-xl font-black text-emerald-400">₹{Number(session.totalSessionAmount || 0).toLocaleString("en-IN")}</p>
                    </div>
                    <Button
                      onClick={() => handleDownloadWordBySession(session.examDate, session.subjectCode, session.allRecords[0]?.id)}
                      className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-md"
                    >
                      <Download className="h-4 w-4 mr-2" />
                      Download Word Form (.docx)
                    </Button>
                  </div>
                </div>

                {/* Session 4-Staff Examiners Grid (Matching Image 1 Table Structure) */}
                <div className="p-5 grid grid-cols-1 md:grid-cols-4 gap-4 bg-slate-50/50">
                  {/* External Examiner Card */}
                  <div className="bg-white p-4 rounded-xl border border-indigo-100 shadow-2xs space-y-2">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                      <span className="text-xs font-black text-indigo-600 uppercase tracking-wider">1. External Examiner</span>
                      <Award className="h-4 w-4 text-indigo-500" />
                    </div>
                    {session.external ? (
                      <div className="space-y-1 text-xs">
                        <p className="font-bold text-slate-900">{safeString(session.external.staffName)}</p>
                        <p className="text-[11px] text-slate-500">{safeString(session.external.designation, "Assistant Professor")}</p>
                        <p className="text-[11px] text-slate-600 font-medium truncate">{safeString(session.external.institutionName, "-NA-")}</p>
                        <p className="text-[11px] text-slate-500 flex items-center gap-1">
                          <Phone className="h-3 w-3 text-slate-400" /> {safeString(session.external.mobileNo, "-NA-")}
                        </p>
                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                          <span className="text-slate-500">Claim + TA/DA:</span>
                          <span className="font-black text-indigo-700">₹{Number(session.external.totalAmount || 0)}</span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center font-medium">-NA-</p>
                    )}
                  </div>

                  {/* Internal Examiner Card */}
                  <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs space-y-2">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                      <span className="text-xs font-black text-slate-700 uppercase tracking-wider">2. Internal Examiner</span>
                      <UserCheck className="h-4 w-4 text-slate-500" />
                    </div>
                    {session.internal ? (
                      <div className="space-y-1 text-xs">
                        <p className="font-bold text-slate-900">{safeString(session.internal.staffName)}</p>
                        <p className="text-[11px] text-slate-500">{safeString(session.internal.designation, "Assistant Professor")}</p>
                        <p className="text-[11px] text-slate-600 font-medium">KRCE ({safeString(session.internal.department)})</p>
                        <p className="text-[11px] text-slate-500 flex items-center gap-1">
                          <Phone className="h-3 w-3 text-slate-400" /> {safeString(session.internal.mobileNo, "-NA-")}
                        </p>
                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                          <span className="text-slate-500">Claim Amount:</span>
                          <span className="font-black text-slate-900">₹{Number(session.internal.totalAmount || 0)}</span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center font-medium">-NA-</p>
                    )}
                  </div>

                  {/* Skilled Assistant Card */}
                  <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs space-y-2">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                      <span className="text-xs font-black text-slate-700 uppercase tracking-wider">3. Skilled Asst.</span>
                      <Users className="h-4 w-4 text-purple-500" />
                    </div>
                    {session.skilled ? (
                      <div className="space-y-1 text-xs">
                        <p className="font-bold text-slate-900">{safeString(session.skilled.staffName)}</p>
                        <p className="text-[11px] text-slate-500">{safeString(session.skilled.designation, "Assistant Professor")}</p>
                        <p className="text-[11px] text-slate-600 font-medium">KRCE ({safeString(session.skilled.department)})</p>
                        <p className="text-[11px] text-slate-500 flex items-center gap-1">
                          <Phone className="h-3 w-3 text-slate-400" /> {safeString(session.skilled.mobileNo, "-NA-")}
                        </p>
                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                          <span className="text-slate-500">Claim Amount:</span>
                          <span className="font-black text-slate-900">₹{Number(session.skilled.totalAmount || 0)}</span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center font-medium">-NA-</p>
                    )}
                  </div>

                  {/* Lab Technician Card */}
                  <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs space-y-2">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                      <span className="text-xs font-black text-slate-700 uppercase tracking-wider">4. Lab Technician</span>
                      <Briefcase className="h-4 w-4 text-slate-400" />
                    </div>
                    {session.tech ? (
                      <div className="space-y-1 text-xs">
                        <p className="font-bold text-slate-900">{safeString(session.tech.staffName)}</p>
                        <p className="text-[11px] text-slate-500">{safeString(session.tech.designation, "Technician")}</p>
                        <p className="text-[11px] text-slate-600 font-medium">KRCE ({safeString(session.tech.department)})</p>
                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                          <span className="text-slate-500">Claim Amount:</span>
                          <span className="font-black text-slate-900">₹{Number(session.tech.totalAmount || 0)}</span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center font-medium">-NA-</p>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* VIEW 2: SEASON & DATE PRACTICAL EXAM LEDGER */}
      {activeView === "LEDGER" && (
        <div className="space-y-6">
          {/* Season & Date Selection Filters */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-4 w-full md:w-auto">
              <div>
                <Label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Exam Season</Label>
                <select
                  value={ledgerSeason}
                  onChange={(e) => setLedgerSeason(e.target.value)}
                  className="mt-1 block w-48 text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-slate-800 focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="ALL">All Seasons</option>
                  {uniqueSeasons.map((season) => (
                    <option key={season} value={season}>
                      {season}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <Label className="text-xs font-bold text-slate-600 uppercase tracking-wider">Exam Date Filter</Label>
                <select
                  value={ledgerDate}
                  onChange={(e) => setLedgerDate(e.target.value)}
                  className="mt-1 block w-48 text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-slate-800 focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="ALL">All Exam Dates</option>
                  {uniqueDates.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="text-right">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Filtered Ledger Total</span>
              <p className="text-2xl font-black text-indigo-600">
                ₹{ledgerDateGroups.reduce((acc, g) => acc + g.totalPayout, 0).toLocaleString("en-IN")}
              </p>
            </div>
          </div>

          {/* Date-wise Ledger Cards */}
          {ledgerDateGroups.length === 0 ? (
            <div className="bg-white p-12 text-center rounded-2xl border border-slate-200">
              <CalendarClock className="h-12 w-12 text-slate-300 mx-auto mb-3" />
              <h3 className="text-base font-bold text-slate-800">No Ledger Entries Found</h3>
              <p className="text-xs text-slate-500 mt-1">Select a different season or date filter.</p>
            </div>
          ) : (
            ledgerDateGroups.map((group) => (
              <div key={group.date} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden space-y-0">
                {/* Ledger Date Header Banner */}
                <div className="bg-slate-800 text-white p-4 px-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-500/20 border border-indigo-400/30 rounded-xl text-indigo-300">
                      <Calendar className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="text-base font-black text-white">Date: {group.date}</h3>
                      <p className="text-xs text-slate-300 font-medium">
                        {group.seasons.join(", ") || "Practical Examinations"} &bull; {group.totalSessionsCount} Practical Labs &bull; {group.totalExamined} Candidates Examined
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <span className="text-[10px] font-bold text-slate-400 uppercase">Day Total</span>
                      <p className="text-lg font-black text-emerald-400">₹{group.totalPayout.toLocaleString("en-IN")}</p>
                    </div>
                  </div>
                </div>

                {/* Ledger Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200">
                      <tr>
                        <th className="p-3.5">Lab / Subject Code</th>
                        <th className="p-3.5">Department</th>
                        <th className="p-3.5">External Examiner</th>
                        <th className="p-3.5">Internal Examiner</th>
                        <th className="p-3.5">Skilled Asst & Tech</th>
                        <th className="p-3.5 text-center">Examined</th>
                        <th className="p-3.5 text-right">Lab Total</th>
                        <th className="p-3.5 text-center">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {group.sessions.map((session) => (
                        <tr key={session.key} className="hover:bg-slate-50/80 transition-colors">
                          <td className="p-3.5">
                            <div className="font-bold text-slate-900">{session.subjectCode}</div>
                            <div className="text-[11px] text-slate-500 truncate max-w-[160px]">{session.subjectName}</div>
                          </td>

                          <td className="p-3.5">
                            <span className="font-bold text-slate-800">{session.department}</span>
                            <div className="text-[10px] text-slate-400">Sem {session.semester || "I"}</div>
                          </td>

                          <td className="p-3.5">
                            {session.external ? (
                              <div>
                                <div className="font-bold text-indigo-700">{session.external.staffName}</div>
                                <div className="text-[10px] text-slate-500 truncate max-w-[140px]">
                                  {session.external.institutionName || "-NA-"}
                                </div>
                                <div className="text-[10px] font-semibold text-slate-600">₹{session.external.totalAmount}</div>
                              </div>
                            ) : (
                              <span className="text-slate-400 font-medium">-NA-</span>
                            )}
                          </td>

                          <td className="p-3.5">
                            {session.internal ? (
                              <div>
                                <div className="font-bold text-slate-900">{session.internal.staffName}</div>
                                <div className="text-[10px] text-slate-500">KRCE ({session.internal.department})</div>
                                <div className="text-[10px] font-semibold text-slate-600">₹{session.internal.totalAmount}</div>
                              </div>
                            ) : (
                              <span className="text-slate-400 font-medium">-NA-</span>
                            )}
                          </td>

                          <td className="p-3.5">
                            <div>
                              <div className="font-semibold text-slate-800">
                                {session.skilled ? session.skilled.staffName : "-NA-"}
                              </div>
                              <div className="text-[10px] text-slate-500">
                                Tech: {session.tech ? session.tech.staffName : "-NA-"}
                              </div>
                            </div>
                          </td>

                          <td className="p-3.5 text-center">
                            <span className="bg-slate-100 text-slate-700 font-bold px-2.5 py-0.5 rounded-full text-[11px]">
                              {session.presentCount} / {session.registeredCount}
                            </span>
                          </td>

                          <td className="p-3.5 text-right font-black text-emerald-600 text-sm">
                            ₹{session.totalSessionAmount.toLocaleString("en-IN")}
                          </td>

                          <td className="p-3.5 text-center">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleDownloadWordBySession(session.examDate, session.subjectCode, session.allRecords[0]?.id)}
                              className="h-8 border-slate-200 text-slate-700 hover:bg-slate-100 text-[11px] font-semibold"
                            >
                              <Download className="h-3.5 w-3.5 mr-1 text-indigo-600" />
                              Word
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* VIEW 3: ALL INDIVIDUAL RECORDS TABLE */}
      {activeView !== "SESSIONS" && activeView !== "LEDGER" && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900 text-white font-bold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="p-3.5">Staff Name & Role</th>
                  <th className="p-3.5">Subject & Dept</th>
                  <th className="p-3.5">Exam Date & Session</th>
                  <th className="p-3.5 text-center">Appeared</th>
                  <th className="p-3.5 text-right">Claim</th>
                  <th className="p-3.5 text-right">TA & DA</th>
                  <th className="p-3.5 text-right">Total Amount</th>
                  <th className="p-3.5 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredClaims.map((claim) => (
                  <tr key={claim.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="p-3.5">
                      <div className="font-bold text-slate-900">{safeString(claim.staffName)}</div>
                      <div className="text-[10px] text-indigo-600 font-semibold mt-0.5">
                        {safeString(claim.staffRole)}
                      </div>
                      <div className="text-[10px] text-slate-400">{safeString(claim.institutionName, "KRCE")}</div>
                    </td>

                    <td className="p-3.5">
                      <div className="font-bold text-slate-800">{safeString(claim.subjectCode)}</div>
                      <div className="text-[10px] text-slate-500 truncate max-w-[140px]">{safeString(claim.subjectName)}</div>
                      <div className="text-[10px] text-slate-400">{safeString(claim.department)} (Sem {safeString(claim.semester)})</div>
                    </td>

                    <td className="p-3.5">
                      <div className="font-semibold text-slate-700">{safeString(claim.examDate, "N/A")}</div>
                      <div className="text-[10px] text-slate-400">{safeString(claim.session)}</div>
                    </td>

                    <td className="p-3.5 text-center">
                      <span className="bg-slate-100 text-slate-700 font-bold px-2 py-0.5 rounded-full">
                        {Number(claim.presentCount || 0)} / {Number(claim.registeredCount || 0)}
                      </span>
                    </td>

                    <td className="p-3.5 text-right font-semibold text-slate-800">
                      ₹{Number(claim.remunerationAmount || 0)}
                    </td>

                    <td className="p-3.5 text-right text-slate-600 font-medium">
                      {claim.staffRole === "EXTERNAL_EXAMINER" ? (
                        <div>
                          <div>TA: ₹{Number(claim.taAmount || 0)}</div>
                          <div>DA: ₹{Number(claim.daAmount || 0)}</div>
                        </div>
                      ) : (
                        "---"
                      )}
                    </td>

                    <td className="p-3.5 text-right font-black text-emerald-600 text-sm">
                      ₹{Number(claim.totalAmount || 0)}
                    </td>

                    <td className="p-3.5 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDownloadWordBySession(claim.examDate, claim.subjectCode, claim.id)}
                          className="h-8 border-slate-200 text-slate-700 hover:bg-slate-100 text-[11px] font-semibold"
                          title="Download Official Session Claim Form (.docx)"
                        >
                          <Download className="h-3.5 w-3.5 mr-1 text-indigo-600" />
                          Word
                        </Button>

                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => claim.id && handleDelete(claim.id)}
                          className="h-8 text-rose-500 hover:text-rose-700 hover:bg-rose-50 p-1.5"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* UPLOAD EXCEL MODAL */}
      {isUploadOpen && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                  <UploadCloud className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900">Upload Lab Claims Excel Sheet</h3>
                  <p className="text-xs text-slate-500">Google Form Response export / Lab claims sheet</p>
                </div>
              </div>
              <button onClick={() => setIsUploadOpen(false)} className="text-slate-400 hover:text-slate-600 font-bold">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <Label className="text-xs font-bold text-slate-700">
                    Exam Season <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    value={examSeason}
                    onChange={(e) => setExamSeason(e.target.value)}
                    placeholder="APR/MAY 2026"
                    className="text-xs mt-1 border-slate-300 font-medium"
                    required
                  />
                </div>
                <div>
                  <Label className="text-xs font-bold text-slate-700">
                    Exam Date <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    type="date"
                    value={examDate}
                    onChange={(e) => setExamDate(e.target.value)}
                    className="text-xs mt-1 border-slate-300 font-medium"
                    required
                  />
                </div>
                <div>
                  <Label className="text-xs font-bold text-slate-700">
                    Subject Code <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    value={subjectCode}
                    onChange={(e) => setSubjectCode(e.target.value)}
                    placeholder="UCS1811"
                    className="text-xs mt-1 border-slate-300 uppercase font-bold text-indigo-700"
                    required
                  />
                </div>
              </div>

              <div>
                <Label className="text-xs font-bold text-slate-700">Select Excel File (.xlsx)</Label>
                <Input
                  type="file"
                  accept=".xlsx, .xls"
                  onChange={handleFileChange}
                  className="text-xs mt-1 cursor-pointer"
                />
              </div>

              {warnings.length > 0 && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 space-y-1">
                  <div className="font-bold flex items-center gap-1.5 text-amber-900">
                    <AlertTriangle className="h-4 w-4 text-amber-600" /> Warnings ({warnings.length}):
                  </div>
                  <ul className="list-disc pl-4 space-y-0.5 max-h-24 overflow-y-auto text-[11px]">
                    {warnings.map((w, idx) => (
                      <li key={idx}>{safeString(w)}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setIsUploadOpen(false)} className="text-xs font-semibold">
                Cancel
              </Button>
              <Button
                onClick={handleUploadExcel}
                disabled={uploading}
                className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold"
              >
                {uploading ? <RefreshCw className="h-4 w-4 mr-2 animate-spin" /> : <UploadCloud className="h-4 w-4 mr-2" />}
                {uploading ? "Processing Excel..." : "Upload & Filter Claims"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export function LabClaimsDashboard() {
  return (
    <ErrorBoundary>
      <LabClaimsDashboardContent />
    </ErrorBoundary>
  );
}

export default LabClaimsDashboard;
