import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { claimsApi, UploadResponse, BatchSummary } from '@/api/claimsApi';
import {
  ClipboardList,
  Package,
  IndianRupee,
  CalendarClock,
  UploadCloud,
  FileSpreadsheet,
  FileSignature,
  Eye,
  AlertTriangle,
  RefreshCw,
  ArchiveX,
  Trash2,
  Link as LinkIcon,
  Copy,
  ChevronRight,
  Download,
  CheckCircle2,
  Layers,
  Award,
  BookOpen
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';

export default function ClaimsDashboard() {
  const navigate = useNavigate();
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadResult, setUploadResult] = useState<UploadResponse | null>(null);
  const [batches, setBatches] = useState<BatchSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [examSeason, setExamSeason] = useState('');
  const [valuationDate, setValuationDate] = useState('');
  const [governmentHoliday, setGovernmentHoliday] = useState(false);
  const [isRevaluation, setIsRevaluation] = useState(false);
  const [selectedSeasonFilter, setSelectedSeasonFilter] = useState('ALL');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleGenerateLink = () => {
    if (!examSeason || !valuationDate) {
      toast.error('Please configure Exam Season and Valuation Date first.');
      return;
    }
    const newBatchId = crypto.randomUUID();
    const queryParams = new URLSearchParams({
      season: examSeason,
      date: valuationDate,
      isRevaluation: isRevaluation.toString(),
    });
    const link = `${window.location.origin}/public/remuneration/${newBatchId}?${queryParams.toString()}`;
    navigator.clipboard.writeText(link);
    toast.success('Secure collection link copied to clipboard!');
  };

  useEffect(() => {
    loadBatches();
  }, []);

  const loadBatches = async () => {
    try {
      const data = await claimsApi.getAllBatches();
      setBatches(Array.isArray(data) ? data : []);
    } catch (err) {
      console.log('Failed to load batches:', err);
    }
  };

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (!examSeason || !valuationDate) {
      setError('Exam Season and Valuation Date are required before uploading Excel.');
      return;
    }
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleFileUpload = async (file: File) => {
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel',
      'text/csv',
    ];
    if (!validTypes.includes(file.type) && !file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
      setError('Invalid format. Please upload a valid Excel file (.xlsx or .xls)');
      return;
    }

    setIsUploading(true);
    setError(null);
    setUploadResult(null);
    setUploadProgress(10);

    try {
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => Math.min(prev + 15, 85));
      }, 400);

      const result = await claimsApi.uploadExcel(file, examSeason, valuationDate, governmentHoliday, isRevaluation);

      clearInterval(progressInterval);
      setUploadProgress(100);
      setUploadResult(result);
      toast.success('Theory remuneration batch uploaded successfully!');

      await loadBatches();

      setTimeout(() => {
        setIsUploading(false);
        setUploadProgress(0);
      }, 1200);
    } catch (err: any) {
      setIsUploading(false);
      setUploadProgress(0);
      setError(err.message || 'Upload failed.');
      toast.error('Upload failed: ' + (err.message || 'Error occurred'));
    }
  };

  const handleDeleteBatch = async (batchId: string) => {
    if (!confirm('Permanently delete this batch? This action cannot be undone.')) return;
    try {
      await claimsApi.deleteBatch(batchId);
      toast.success('Batch deleted successfully.');
      await loadBatches();
    } catch {
      setError('Failed to delete batch');
      toast.error('Failed to delete batch');
    }
  };

  const uniqueSeasons = Array.from(
    new Set(
      batches
        .map((b) => b.examSeason)
        .filter((s): s is string => !!s && s.trim() !== '')
    )
  );

  const filteredBatches =
    selectedSeasonFilter === 'ALL'
      ? batches
      : batches.filter((b) => b.examSeason === selectedSeasonFilter);

  const totalClaims = filteredBatches.reduce((sum, b) => sum + b.totalRecords, 0);
  const totalAmount = filteredBatches.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  const totalBatchesCount = filteredBatches.length;
  const examPeriodLabel =
    selectedSeasonFilter === 'ALL'
      ? batches.length > 0
        ? batches[0].examSeason || 'ALL SEASONS'
        : 'ALL SEASONS'
      : selectedSeasonFilter;

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto bg-slate-50/50 min-h-screen">
      {/* Header Banner aligned with HallSync Design System */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
              <IndianRupee className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                Theory Claims & Remuneration Processing Engine
              </h1>
              <p className="text-sm text-slate-500 font-medium mt-0.5">
                Centralized evaluation remuneration, TA/DA calculation, and financial voucher generation
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <Button
            variant="outline"
            onClick={loadBatches}
            className="border-slate-200 bg-white text-slate-700 hover:bg-slate-50 shadow-sm font-semibold"
          >
            <RefreshCw className="h-4 w-4 mr-2 text-slate-500" />
            Sync Directory
          </Button>

          {batches.length > 0 && (
            <Button
              onClick={() => navigate(`/dashboard/claims/${batches[0].batchId}`)}
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold shadow-sm shadow-indigo-200 gap-2"
            >
              Master Ledger
              <ChevronRight className="h-4 w-4 opacity-80" />
            </Button>
          )}
        </div>
      </div>

      {/* KPI Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Claims */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Claims Processed</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">{totalClaims.toLocaleString()} Records</h3>
            <p className="text-xs text-indigo-600 font-semibold mt-1 flex items-center">
              <ClipboardList className="h-3.5 w-3.5 mr-1" /> Checked & Verified
            </p>
          </div>
          <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl">
            <ClipboardList className="h-6 w-6" />
          </div>
        </div>

        {/* Active Batches */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Active Batches</p>
            <h3 className="text-2xl font-black text-slate-900 mt-1">{totalBatchesCount} Batches</h3>
            <p className="text-xs text-emerald-600 font-semibold mt-1 flex items-center">
              <Package className="h-3.5 w-3.5 mr-1" /> Active Collections
            </p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
            <Package className="h-6 w-6" />
          </div>
        </div>

        {/* Total Capital Disbursed */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Capital Disbursed</p>
            <h3 className="text-2xl font-black text-emerald-600 mt-1">
              ₹{totalAmount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
            </h3>
            <p className="text-xs text-emerald-600 font-semibold mt-1 flex items-center">
              <Award className="h-3.5 w-3.5 mr-1" /> Includes Remuneration + TA/DA
            </p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
            <IndianRupee className="h-6 w-6" />
          </div>
        </div>

        {/* Exam Period */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Exam Period</p>
            <h3 className="text-xl font-black text-slate-900 mt-1 truncate max-w-[150px]" title={examPeriodLabel}>
              {examPeriodLabel}
            </h3>
            <p className="text-xs text-amber-600 font-semibold mt-1 flex items-center">
              <CalendarClock className="h-3.5 w-3.5 mr-1" /> Valuation Season
            </p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
            <CalendarClock className="h-6 w-6" />
          </div>
        </div>
      </div>

      {/* Ingestion & Configuration Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2">
            <Layers className="h-5 w-5 text-indigo-600" />
            <h2 className="text-base font-black text-slate-900">Data Ingestion & Session Configuration</h2>
          </div>
          <span className="text-xs font-semibold text-slate-500">Configure parameters before uploading</span>
        </div>

        <div className="p-6 space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Exam Season Identifier <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                className="w-full h-10 px-3 rounded-xl border border-slate-200 bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm font-semibold text-slate-900"
                placeholder="e.g. APR/MAY 2026"
                value={examSeason}
                onChange={(e) => setExamSeason(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Valuation Date <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                className="w-full h-10 px-3 rounded-xl border border-slate-200 bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm font-semibold text-slate-900"
                value={valuationDate}
                onChange={(e) => {
                  const dateVal = e.target.value;
                  setValuationDate(dateVal);
                  if (dateVal) {
                    const date = new Date(dateVal);
                    if (date.getDay() === 0) setGovernmentHoliday(true);
                  }
                }}
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Apply Holiday Rates (Home Inst.)
              </label>
              <div className="flex h-10 bg-slate-100 p-1 rounded-xl border border-slate-200">
                <button
                  type="button"
                  className={`flex-1 rounded-lg text-xs font-bold transition-all ${
                    governmentHoliday
                      ? 'bg-white text-indigo-700 shadow-sm border border-slate-200'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                  onClick={() => setGovernmentHoliday(true)}
                >
                  Enabled
                </button>
                <button
                  type="button"
                  className={`flex-1 rounded-lg text-xs font-bold transition-all ${
                    !governmentHoliday
                      ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                  onClick={() => setGovernmentHoliday(false)}
                >
                  Disabled
                </button>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Revaluation Mode
              </label>
              <div className="flex h-10 bg-slate-100 p-1 rounded-xl border border-slate-200">
                <button
                  type="button"
                  className={`flex-1 rounded-lg text-xs font-bold transition-all ${
                    isRevaluation
                      ? 'bg-white text-amber-700 shadow-sm border border-amber-200 font-black'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                  onClick={() => setIsRevaluation(true)}
                >
                  Yes
                </button>
                <button
                  type="button"
                  className={`flex-1 rounded-lg text-xs font-bold transition-all ${
                    !isRevaluation
                      ? 'bg-white text-slate-900 shadow-sm border border-slate-200 font-black'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                  onClick={() => setIsRevaluation(false)}
                >
                  No
                </button>
              </div>
              <p className="text-[10px] text-slate-400 font-medium">ESE Re-Valuation format (15 rows)</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-slate-100">
            {/* Batch Upload Option */}
            <div className="bg-slate-50/50 p-5 rounded-2xl border border-slate-200 space-y-4">
              <div className="flex items-center gap-2">
                <FileSpreadsheet className="h-5 w-5 text-indigo-600" />
                <h3 className="text-sm font-black text-slate-900">Batch Upload (Excel File)</h3>
              </div>

              <div
                className={`border-2 border-dashed rounded-2xl p-6 flex flex-col items-center justify-center transition-all ${
                  !examSeason || !valuationDate
                    ? 'opacity-50 bg-slate-100/50 cursor-not-allowed border-slate-300'
                    : isDragging
                    ? 'border-indigo-500 bg-indigo-50/50'
                    : 'border-indigo-200 bg-indigo-50/20 hover:border-indigo-400 hover:bg-indigo-50/40 cursor-pointer'
                }`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => {
                  if (examSeason && valuationDate) fileInputRef.current?.click();
                  else {
                    setError('Exam Season & Valuation Date configuration required before upload.');
                    toast.error('Configure Exam Season and Valuation Date first!');
                  }
                }}
              >
                {isUploading ? (
                  <div className="flex items-center gap-3 text-sm font-bold text-indigo-700">
                    <RefreshCw className="h-5 w-5 animate-spin text-indigo-600" />
                    Processing Excel Records... {uploadProgress}%
                  </div>
                ) : (
                  <>
                    <UploadCloud className="h-8 w-8 text-indigo-500 mb-2" />
                    <div className="text-sm font-bold text-slate-800 mb-0.5">
                      Drag & drop your Excel file here
                    </div>
                    <div className="text-xs text-slate-500 mb-4 font-medium">Supports .xlsx, .xls, .csv</div>

                    <Button type="button" variant="outline" size="sm" className="h-9 px-4 text-xs font-bold bg-white border-slate-300 text-slate-700">
                      Browse Files
                    </Button>
                  </>
                )}

                <input ref={fileInputRef} type="file" accept=".xlsx,.xls,.csv" onChange={handleFileSelect} className="hidden" />
              </div>
            </div>

            {/* Link Generation Option */}
            <div className="bg-slate-50/50 p-5 rounded-2xl border border-slate-200 space-y-4">
              <div className="flex items-center gap-2">
                <LinkIcon className="h-5 w-5 text-indigo-600" />
                <h3 className="text-sm font-black text-slate-900">Distributed Faculty Collection (URL)</h3>
              </div>

              <div className="border border-slate-200 bg-white rounded-2xl p-6 flex flex-col items-center justify-center h-[178px]">
                <p className="text-xs font-medium text-slate-600 text-center max-w-xs mb-4">
                  Generate a secure portal link for external and internal faculty members to fill remuneration claims directly.
                </p>
                <Button
                  onClick={handleGenerateLink}
                  className="w-full max-w-[220px] h-10 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-sm shadow-indigo-200 rounded-xl"
                >
                  <Copy className="h-4 w-4 mr-2" /> Copy Collection Link
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-2xl border border-rose-200 bg-rose-50 text-rose-800 flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-rose-600 shrink-0" />
          <span className="font-semibold text-sm">{error}</span>
        </div>
      )}

      {/* Upload Result Alert */}
      {uploadResult && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-5 shadow-sm space-y-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-emerald-600" />
            <h3 className="text-sm font-black text-emerald-900">Theory Remuneration Batch Processed Successfully</h3>
          </div>
          <div className="flex flex-wrap gap-6 text-xs font-semibold">
            <div>
              <span className="text-emerald-700">Total Records:</span>{' '}
              <span className="font-bold text-emerald-900">{uploadResult.totalRecords}</span>
            </div>
            <div>
              <span className="text-emerald-700">Examiners:</span>{' '}
              <span className="font-bold text-emerald-900">{uploadResult.examiners}</span>
            </div>
            <div>
              <span className="text-emerald-700">Chief Examiners:</span>{' '}
              <span className="font-bold text-emerald-900">{uploadResult.chiefExaminers}</span>
            </div>
            <div>
              <span className="text-emerald-700">Assistant Examiners:</span>{' '}
              <span className="font-bold text-emerald-900">{uploadResult.assistantExaminers}</span>
            </div>
          </div>
        </div>
      )}

      {/* Ledger Directory */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pt-4">
        <div>
          <h2 className="text-lg font-black text-slate-900 tracking-tight">Theory Remuneration Ledger Directory</h2>
          <p className="text-xs text-slate-500 font-medium">Uploaded valuation batches, disbursed amounts, and export files</p>
        </div>
        <div className="flex items-center gap-3">
          {uniqueSeasons.length > 0 && (
            <select
              className="h-9 px-3 rounded-xl border border-slate-200 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
              value={selectedSeasonFilter}
              onChange={(e) => setSelectedSeasonFilter(e.target.value)}
            >
              <option value="ALL">All Valuation Seasons</option>
              {uniqueSeasons.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          )}
        </div>
      </div>

      {batches.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 flex flex-col items-center justify-center text-center shadow-sm">
          <ArchiveX className="h-10 w-10 text-slate-300 mb-3" />
          <h3 className="text-sm font-bold text-slate-900">Directory Empty</h3>
          <p className="text-xs text-slate-500 font-medium mt-1">Configure and upload an Excel file above to create your first theory claim ledger.</p>
        </div>
      ) : filteredBatches.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 flex flex-col items-center justify-center text-center shadow-sm">
          <ArchiveX className="h-10 w-10 text-slate-300 mb-3" />
          <h3 className="text-sm font-bold text-slate-900">No Batches Match Filter</h3>
          <p className="text-xs text-slate-500 font-medium mt-1">Adjust your season filter above to view existing claim ledgers.</p>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Batch ID / Season</th>
                  <th className="px-6 py-4">Valuation Date</th>
                  <th className="px-6 py-4 text-right">Records</th>
                  <th className="px-6 py-4 text-right">Disbursed (₹)</th>
                  <th className="px-6 py-4 text-center">Export Artifacts</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredBatches.map((batch) => (
                  <tr key={batch.batchId} className="hover:bg-slate-50/70 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="font-mono text-slate-900 font-bold">{batch.batchId.substring(0, 8)}</div>
                      <div className="text-xs text-slate-500 font-semibold mt-0.5">{batch.examSeason || 'N/A'}</div>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-700">
                      {batch.valuationDate || batch.createdAt.split('T')[0]}
                    </td>
                    <td className="px-6 py-4 text-right text-slate-700 font-bold">
                      {batch.totalRecords}
                    </td>
                    <td className="px-6 py-4 text-right font-black text-emerald-600 text-base">
                      ₹{(batch.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-2.5 text-xs font-bold border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm"
                          onClick={() => window.open(claimsApi.getDownloadWordUrl(batch.batchId), '_blank')}
                          title="Download Official Word Claim Form"
                        >
                          <FileSignature className="h-3.5 w-3.5 mr-1 text-indigo-600" /> Word
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-2.5 text-xs font-bold border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm"
                          onClick={() => window.open(claimsApi.getDownloadExcel1Url(batch.batchId), '_blank')}
                          title="Download Detailed Excel Remuneration Sheet"
                        >
                          <FileSpreadsheet className="h-3.5 w-3.5 mr-1 text-emerald-600" /> XLS-1
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-2.5 text-xs font-bold border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm"
                          onClick={() => window.open(claimsApi.getDownloadExcel2Url(batch.batchId), '_blank')}
                          title="Download Reduced Summary Excel"
                        >
                          <FileSpreadsheet className="h-3.5 w-3.5 mr-1 text-amber-600" /> XLS-2
                        </Button>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-3 text-xs font-bold border-indigo-200 text-indigo-700 bg-indigo-50/50 hover:bg-indigo-100 shadow-sm"
                          onClick={() => navigate(`/dashboard/claims/${batch.batchId}`)}
                          title="View Ledger Details"
                        >
                          <Eye className="h-3.5 w-3.5 mr-1 text-indigo-600" /> View
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 w-8 p-0 text-slate-400 hover:text-rose-600 hover:bg-rose-50 border-slate-200"
                          onClick={() => handleDeleteBatch(batch.batchId)}
                          title="Delete Batch"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
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
    </div>
  );
}
