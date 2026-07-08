import { useState, useRef } from "react";
import { erpApi } from '@/api/erpApi';
import { getSessions } from '@/api/allocationApi';
import { toast } from "sonner";
import { useQuery } from "@tanstack/react-query";
import {
  Upload,
  FileSpreadsheet,
  Download,
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Database,
} from "lucide-react";

interface ImportResultData {
  totalRows: number;
  successCount: number;
  failedCount: number;
  skippedDuplicates: number;
  errors: string[];
}

export function ErpImportCenter() {
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [importResult, setImportResult] = useState<ImportResultData | null>(null);
  const [isImporting, setIsImporting] = useState<string | null>(null);
  const excelRef = useRef<HTMLInputElement>(null);
  const camuRef = useRef<HTMLInputElement>(null);
  const icloudRef = useRef<HTMLInputElement>(null);

  const { data: sessions = [] } = useQuery({
    queryKey: ["sessions-for-import"],
    queryFn: async () => {
      const response = await getSessions();
      return response;
    },
  });

  const handleImport = async (
    type: "excel" | "camu" | "icloudems",
    file: File
  ) => {
    if (!selectedSessionId) {
      toast.error("Please select an exam session first.");
      return;
    }
    setIsImporting(type);
    setImportResult(null);
    try {
      let response;
      switch (type) {
        case "excel":
          response = await erpApi.importExcel(file, selectedSessionId);
          break;
        case "camu":
          response = await erpApi.importCamu(file, selectedSessionId);
          break;
        case "icloudems":
          response = await erpApi.importICloudEms(file, selectedSessionId);
          break;
      }
      setImportResult(response.data);
      if (response.data.successCount > 0) {
        toast.success(
          `Imported ${response.data.successCount} students successfully!`
        );
      }
      if (response.data.failedCount > 0) {
        toast.warning(`${response.data.failedCount} rows failed.`);
      }
    } catch (error) {
      toast.error("Import failed. Please check the file format.");
    } finally {
      setIsImporting(null);
    }
  };

  const handleDownloadTemplate = async (type: string) => {
    try {
      const response = await erpApi.downloadTemplate(type);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      const ext = type === "excel" ? "xlsx" : "csv";
      link.setAttribute("download", `HallSync_${type}_template.${ext}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success("Template downloaded!");
    } catch {
      toast.error("Failed to download template.");
    }
  };

  const importCards = [
    {
      type: "excel" as const,
      title: "Excel Import",
      description: "Upload a .xlsx file with student data",
      icon: <FileSpreadsheet className="w-8 h-8 text-emerald-600" />,
      bgColor: "bg-emerald-50",
      borderColor: "border-emerald-200",
      buttonColor: "bg-emerald-600 hover:bg-emerald-700",
      accept: ".xlsx,.xls",
      ref: excelRef,
    },
    {
      type: "camu" as const,
      title: "Camu Import",
      description: "Upload a Camu-format CSV export",
      icon: <Database className="w-8 h-8 text-blue-600" />,
      bgColor: "bg-blue-50",
      borderColor: "border-blue-200",
      buttonColor: "bg-blue-600 hover:bg-blue-700",
      accept: ".csv",
      ref: camuRef,
    },
    {
      type: "icloudems" as const,
      title: "iCloudEMS Import",
      description: "Upload an iCloudEMS-format CSV export",
      icon: <Database className="w-8 h-8 text-violet-600" />,
      bgColor: "bg-violet-50",
      borderColor: "border-violet-200",
      buttonColor: "bg-violet-600 hover:bg-violet-700",
      accept: ".csv",
      ref: icloudRef,
    },
  ];

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div>
        <h1 className="text-3xl font-bold text-gray-900 tracking-tight flex items-center gap-3">
          <Upload className="text-primary" /> Import Data
        </h1>
        <p className="text-gray-500 mt-1">
          Import student data from Excel, Camu, or iCloudEMS in one click.
        </p>
      </div>

      {/* Session selector */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <label className="block text-sm font-semibold text-gray-700 mb-2">
          Target Exam Session
        </label>
        <p className="text-xs text-gray-500 mb-3">
          Imported students will be linked to this exam session.
        </p>
        <select
          className="w-full max-w-md rounded-lg border border-gray-300 py-2.5 pl-3 pr-10 shadow-sm focus:border-brand-500 focus:ring-1 focus:ring-brand-500 text-sm"
          value={selectedSessionId}
          onChange={(e) => setSelectedSessionId(e.target.value)}
        >
          <option value="">-- Select an Exam Session --</option>
          {sessions.map((session: any) => (
            <option key={session.id} value={session.id}>
              {session.name} — {session.examDate} ({session.session})
            </option>
          ))}
        </select>
      </div>

      {/* Import cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {importCards.map((card) => (
          <div
            key={card.type}
            className={`bg-white rounded-xl shadow-sm border ${card.borderColor} p-6 space-y-4`}
          >
            <div className="flex items-center gap-3">
              <div className={`p-3 ${card.bgColor} rounded-xl`}>
                {card.icon}
              </div>
              <div>
                <h3 className="font-semibold text-gray-900">{card.title}</h3>
                <p className="text-xs text-gray-500">{card.description}</p>
              </div>
            </div>

            {/* Download template button */}
            <button
              onClick={() => handleDownloadTemplate(card.type)}
              className="w-full inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
            >
              <Download className="w-4 h-4" />
              Download Template
            </button>

            {/* Hidden file input */}
            <input
              type="file"
              ref={card.ref}
              accept={card.accept}
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleImport(card.type, file);
                e.target.value = "";
              }}
            />

            {/* Upload button */}
            <button
              onClick={() => card.ref.current?.click()}
              disabled={!selectedSessionId || isImporting === card.type}
              className={`w-full inline-flex items-center justify-center gap-2 rounded-lg ${card.buttonColor} px-4 py-2.5 text-sm font-medium text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors`}
            >
              {isImporting === card.type ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Upload className="w-4 h-4" />
              )}
              Upload & Import
            </button>
          </div>
        ))}
      </div>

      {/* Import result */}
      {importResult && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-5 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">
              Import Results
            </h2>
          </div>
          <div className="p-5">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div className="bg-gray-50 rounded-lg p-4 text-center">
                <p className="text-2xl font-bold text-gray-900">
                  {importResult.totalRows}
                </p>
                <p className="text-xs text-gray-500 mt-1">Total Rows</p>
              </div>
              <div className="bg-emerald-50 rounded-lg p-4 text-center">
                <p className="text-2xl font-bold text-emerald-600">
                  {importResult.successCount}
                </p>
                <p className="text-xs text-emerald-600 mt-1 flex items-center justify-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Imported
                </p>
              </div>
              <div className="bg-amber-50 rounded-lg p-4 text-center">
                <p className="text-2xl font-bold text-amber-600">
                  {importResult.skippedDuplicates}
                </p>
                <p className="text-xs text-amber-600 mt-1 flex items-center justify-center gap-1">
                  <AlertTriangle className="w-3 h-3" /> Duplicates
                </p>
              </div>
              <div className="bg-rose-50 rounded-lg p-4 text-center">
                <p className="text-2xl font-bold text-rose-600">
                  {importResult.failedCount}
                </p>
                <p className="text-xs text-rose-600 mt-1 flex items-center justify-center gap-1">
                  <XCircle className="w-3 h-3" /> Failed
                </p>
              </div>
            </div>

            {importResult.errors.length > 0 && (
              <div className="bg-rose-50 border border-rose-200 rounded-lg p-4">
                <h4 className="text-sm font-semibold text-rose-700 mb-2">
                  Errors ({importResult.errors.length})
                </h4>
                <ul className="space-y-1 max-h-48 overflow-y-auto">
                  {importResult.errors.map((err, i) => (
                    <li
                      key={i}
                      className="text-xs text-rose-600 flex items-start gap-1.5"
                    >
                      <XCircle className="w-3 h-3 mt-0.5 shrink-0" />
                      {err}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
