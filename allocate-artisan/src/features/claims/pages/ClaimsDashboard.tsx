import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  claimsApi,
  UploadResponse,
  BatchSummary,
} from '@/api/claimsApi';
import { 
  Zap, 
  ClipboardList, 
  Package, 
  IndianRupee, 
  CalendarClock, 
  UploadCloud, 
  FileSpreadsheet, 
  FolderUp, 
  XCircle, 
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
  MoreVertical
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
      date: valuationDate
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
      setBatches(data);
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
      setError('Exam Season and Valuation Date are required for upload.');
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
      'text/csv'
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
      }, 500);

      const result = await claimsApi.uploadExcel(file, examSeason, valuationDate, governmentHoliday);

      clearInterval(progressInterval);
      setUploadProgress(100);
      setUploadResult(result);

      await loadBatches();

      setTimeout(() => {
        setIsUploading(false);
        setUploadProgress(0);
      }, 1500);
    } catch (err: any) {
      setIsUploading(false);
      setUploadProgress(0);
      setError(err.message || 'Upload failed.');
    }
  };

  const handleDeleteBatch = async (batchId: string) => {
    if (!confirm('Permanently delete this batch? This action cannot be undone.')) return;
    try {
      await claimsApi.deleteBatch(batchId);
      await loadBatches();
    } catch {
      setError('Failed to delete batch');
    }
  };

  const uniqueSeasons = Array.from(
    new Set(
      batches
        .map((b) => b.examSeason)
        .filter((s): s is string => !!s && s.trim() !== "")
    )
  );

  const filteredBatches = selectedSeasonFilter === 'ALL'
    ? batches
    : batches.filter((b) => b.examSeason === selectedSeasonFilter);

  const totalClaims = filteredBatches.reduce((sum, b) => sum + b.totalRecords, 0);
  const totalAmount = filteredBatches.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  const totalBatchesCount = filteredBatches.length;
  const examPeriodLabel = selectedSeasonFilter === 'ALL'
    ? (batches.length > 0 ? (batches[0].examSeason || 'ALL SEASONS') : 'ALL SEASONS')
    : selectedSeasonFilter;

  return (
    <div className="container mx-auto py-8 px-4 lg:px-8 max-w-7xl animate-in fade-in slide-in-from-bottom-4 duration-700 font-sans">
      
      {/* Enterprise Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10 pb-6 border-b border-slate-200/60">
        <div>
          <h1 className="text-2xl lg:text-3xl font-bold tracking-tight text-slate-900 mb-1">
            Claims Processing Engine
          </h1>
          <p className="text-slate-500 text-sm font-medium">
            Enterprise TA/DA calculation and financial document generation.
          </p>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          {batches.length > 0 && (
            <Button 
              onClick={() => navigate(`/dashboard/claims/${batches[0].batchId}`)}
              className="bg-slate-900 hover:bg-slate-800 text-white gap-2 h-9 px-4 rounded-md shadow-sm transition-all text-sm font-medium"
            >
              Master Ledger
              <ChevronRight size={16} className="opacity-50" />
            </Button>
          )}
        </div>
      </div>

      {/* Professional KPI Metrics */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <div className="bg-white border border-slate-200/80 rounded-xl p-5 shadow-[0_1px_2px_rgba(0,0,0,0.02)] flex flex-col">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-medium text-slate-500">Total Claims Processed</h3>
            <ClipboardList size={16} className="text-slate-400" />
          </div>
          <div className="text-3xl font-semibold text-slate-900 tracking-tight">{totalClaims.toLocaleString()}</div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-xl p-5 shadow-[0_1px_2px_rgba(0,0,0,0.02)] flex flex-col">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-medium text-slate-500">Active Batches</h3>
            <Package size={16} className="text-slate-400" />
          </div>
          <div className="text-3xl font-semibold text-slate-900 tracking-tight">{totalBatchesCount}</div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-xl p-5 shadow-[0_1px_2px_rgba(0,0,0,0.02)] flex flex-col">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-medium text-slate-500">Total Capital Disbursed</h3>
            <IndianRupee size={16} className="text-slate-400" />
          </div>
          <div className="text-3xl font-semibold text-slate-900 tracking-tight">
            ₹{totalAmount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
          </div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-xl p-5 shadow-[0_1px_2px_rgba(0,0,0,0.02)] flex flex-col">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-medium text-slate-500">Exam Period</h3>
            <CalendarClock size={16} className="text-slate-400" />
          </div>
          <div className="text-2xl font-semibold text-slate-800 tracking-tight truncate" title={examPeriodLabel}>
            {examPeriodLabel}
          </div>
        </div>
      </div>

      {/* Advanced Data Ingestion Section */}
      <div className="mb-12">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">Ingestion & Configuration</h2>
        <div className="bg-white border border-slate-200/80 rounded-xl shadow-[0_1px_2px_rgba(0,0,0,0.02)] overflow-hidden">
          
          <div className="p-6 border-b border-slate-100">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-600">Exam Season Identifier</label>
                <input 
                  type="text" 
                  className="w-full h-9 px-3 rounded-md border border-slate-300 bg-white focus:ring-1 focus:ring-slate-400 focus:border-slate-400 transition-colors text-sm text-slate-900" 
                  placeholder="e.g. MAY 2026" 
                  value={examSeason}
                  onChange={(e) => setExamSeason(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-600">Valuation Date</label>
                <input 
                  type="date" 
                  className="w-full h-9 px-3 rounded-md border border-slate-300 bg-white focus:ring-1 focus:ring-slate-400 focus:border-slate-400 transition-colors text-sm text-slate-900" 
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
                <label className="text-xs font-semibold text-slate-600">Apply Holiday Rates (Home Inst.)</label>
                <div className="flex h-9 bg-slate-100/50 p-0.5 rounded-md border border-slate-200/50">
                  <button
                    className={`flex-1 rounded-sm text-xs font-medium transition-all ${governmentHoliday ? 'bg-white text-slate-900 shadow-sm border border-slate-200' : 'text-slate-500 hover:text-slate-700'}`}
                    onClick={() => setGovernmentHoliday(true)}
                  >
                    Enabled
                  </button>
                  <button
                    className={`flex-1 rounded-sm text-xs font-medium transition-all ${!governmentHoliday ? 'bg-white text-slate-900 shadow-sm border border-slate-200' : 'text-slate-500 hover:text-slate-700'}`}
                    onClick={() => setGovernmentHoliday(false)}
                  >
                    Disabled
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
            
            {/* Upload Method */}
            <div className="p-6 bg-slate-50/30">
              <div className="flex items-center gap-2 mb-4">
                <FileSpreadsheet size={16} className="text-slate-500" />
                <h3 className="text-sm font-semibold text-slate-900">Batch Upload (Excel)</h3>
              </div>
              
              <div
                className={`border border-dashed rounded-lg p-6 flex flex-col items-center justify-center transition-all ${
                  (!examSeason || !valuationDate) 
                    ? 'opacity-50 bg-slate-50/50 cursor-not-allowed border-slate-200' 
                    : isDragging
                      ? 'border-indigo-500 bg-indigo-50/30'
                      : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50 cursor-pointer'
                }`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => {
                  if (examSeason && valuationDate) fileInputRef.current?.click();
                  else setError('Configuration required before upload.');
                }}
              >
                {isUploading ? (
                  <div className="flex items-center gap-3 text-sm font-medium text-slate-700">
                    <div className="w-4 h-4 border-2 border-slate-400 border-t-transparent rounded-full animate-spin" />
                    Processing {uploadProgress}%
                  </div>
                ) : (
                  <>
                    <div className="text-sm font-medium text-slate-700 mb-1">
                      Drag & drop your file here
                    </div>
                    <div className="text-xs text-slate-500 mb-4">Supports .xlsx, .xls, .csv</div>
                    
                    <Button type="button" variant="outline" size="sm" className="h-8 text-xs font-medium bg-white">
                      Browse Files
                    </Button>
                  </>
                )}
                
                <input ref={fileInputRef} type="file" accept=".xlsx,.xls,.csv" onChange={handleFileSelect} className="hidden" />
              </div>
            </div>

            {/* Link Method */}
            <div className="p-6 bg-slate-50/30">
              <div className="flex items-center gap-2 mb-4">
                <LinkIcon size={16} className="text-slate-500" />
                <h3 className="text-sm font-semibold text-slate-900">Distributed Collection (Link)</h3>
              </div>

              <div className="border border-slate-200 bg-white rounded-lg p-6 flex flex-col items-center justify-center h-[178px]">
                <p className="text-sm text-slate-600 text-center max-w-[250px] mb-6">
                  Generate a secure URL to distribute to faculty for direct claim entry.
                </p>
                <Button 
                  onClick={handleGenerateLink}
                  className="w-full max-w-[200px] h-9 bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium shadow-sm transition-all rounded-md"
                >
                  <Copy size={14} className="mr-2" /> Copy Link
                </Button>
              </div>
            </div>

          </div>
        </div>
      </div>

      {error && (
        <div className="mb-8 p-3 rounded-md border border-red-200 bg-red-50 text-red-800 flex items-center gap-2">
          <AlertTriangle size={16} className="shrink-0" />
          <span className="font-medium text-sm">{error}</span>
        </div>
      )}

      {/* Upload Result */}
      {uploadResult && (
        <div className="mb-8 bg-emerald-50/50 border border-emerald-200 rounded-xl p-5 animate-in slide-in-from-bottom-2 duration-300">
          <div className="flex items-center gap-2 mb-4">
            <CheckCircle2 size={16} className="text-emerald-600" />
            <h3 className="text-sm font-semibold text-emerald-900">Batch successfully processed</h3>
          </div>
          <div className="flex gap-8 text-sm">
            <div>
              <span className="text-emerald-700 font-medium mr-2">Total Records:</span>
              <span className="font-semibold text-emerald-900">{uploadResult.totalRecords}</span>
            </div>
            <div>
              <span className="text-emerald-700 font-medium mr-2">Examiners:</span>
              <span className="font-semibold text-emerald-900">{uploadResult.examiners}</span>
            </div>
            <div>
              <span className="text-emerald-700 font-medium mr-2">Chief:</span>
              <span className="font-semibold text-emerald-900">{uploadResult.chiefExaminers}</span>
            </div>
            <div>
              <span className="text-emerald-700 font-medium mr-2">Assistant:</span>
              <span className="font-semibold text-emerald-900">{uploadResult.assistantExaminers}</span>
            </div>
          </div>
          
          {uploadResult.warnings && uploadResult.warnings.length > 0 && (
            <div className="mt-4 pt-4 border-t border-emerald-200/50">
              <div className="text-xs font-semibold text-amber-700 mb-2">{uploadResult.warnings.length} Validation Notes:</div>
              <ul className="text-xs text-amber-700/80 space-y-1 list-disc list-inside">
                {uploadResult.warnings.slice(0, 3).map((w, i) => (
                  <li key={i}>{w}</li>
                ))}
                {uploadResult.warnings.length > 3 && <li>...and {uploadResult.warnings.length - 3} more</li>}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Ledger Directory */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Ledger Directory</h2>
        </div>
        <div className="flex items-center gap-3">
          {uniqueSeasons.length > 0 && (
            <div className="flex items-center">
              <select
                className="h-8 px-2 rounded-md border border-slate-300 text-xs font-medium text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-1 focus:ring-slate-400"
                value={selectedSeasonFilter}
                onChange={(e) => setSelectedSeasonFilter(e.target.value)}
              >
                <option value="ALL">All Seasons</option>
                {uniqueSeasons.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
          )}
          <Button variant="outline" size="sm" onClick={loadBatches} className="h-8 gap-2 px-3 text-xs font-medium border-slate-300">
            <RefreshCw size={12} /> Sync Directory
          </Button>
        </div>
      </div>

      {batches.length === 0 ? (
        <div className="border border-slate-200 rounded-xl p-12 flex flex-col items-center justify-center text-center bg-slate-50/50">
          <ArchiveX size={24} className="text-slate-400 mb-3" />
          <h3 className="text-sm font-semibold text-slate-900">Directory Empty</h3>
          <p className="text-xs text-slate-500 mt-1">Ingest data above to create the first ledger entry.</p>
        </div>
      ) : filteredBatches.length === 0 ? (
        <div className="border border-slate-200 rounded-xl p-12 flex flex-col items-center justify-center text-center bg-slate-50/50">
          <ArchiveX size={24} className="text-slate-400 mb-3" />
          <h3 className="text-sm font-semibold text-slate-900">No Entries Found</h3>
          <p className="text-xs text-slate-500 mt-1">Adjust your season filter.</p>
        </div>
      ) : (
        <div className="bg-white border border-slate-200/80 rounded-xl shadow-[0_1px_2px_rgba(0,0,0,0.02)] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Batch ID / Season</th>
                  <th className="px-6 py-4">Valuation Date</th>
                  <th className="px-6 py-4 text-right">Records</th>
                  <th className="px-6 py-4 text-right">Disbursed (₹)</th>
                  <th className="px-6 py-4 text-center">Export Artifacts</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredBatches.map((batch) => (
                  <tr key={batch.batchId} className="hover:bg-slate-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="font-mono text-slate-900 font-medium">{batch.batchId.substring(0, 8)}</div>
                      <div className="text-xs text-slate-500 mt-0.5">{batch.examSeason || 'N/A'}</div>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-700">
                      {batch.valuationDate || batch.createdAt.split('T')[0]}
                    </td>
                    <td className="px-6 py-4 text-right text-slate-600 font-medium">
                      {batch.totalRecords}
                    </td>
                    <td className="px-6 py-4 text-right font-semibold text-slate-900">
                      {(batch.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="h-7 px-2.5 text-xs font-medium border-slate-200 text-slate-600 hover:text-slate-900 hover:border-slate-300"
                          onClick={() => window.open(claimsApi.getDownloadWordUrl(batch.batchId), '_blank')}
                          title="Download Word Document"
                        >
                          <FileSignature size={14} className="mr-1.5" /> Word
                        </Button>
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="h-7 px-2.5 text-xs font-medium border-slate-200 text-slate-600 hover:text-slate-900 hover:border-slate-300"
                          onClick={() => window.open(claimsApi.getDownloadExcel1Url(batch.batchId), '_blank')}
                          title="Download Detailed Excel"
                        >
                          <FileSpreadsheet size={14} className="mr-1.5" /> XLS-1
                        </Button>
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="h-7 px-2.5 text-xs font-medium border-slate-200 text-slate-600 hover:text-slate-900 hover:border-slate-300"
                          onClick={() => window.open(claimsApi.getDownloadExcel2Url(batch.batchId), '_blank')}
                          title="Download Reduced Excel"
                        >
                          <FileSpreadsheet size={14} className="mr-1.5" /> XLS-2
                        </Button>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
                        <Button 
                          variant="ghost" 
                          size="sm" 
                          className="h-7 w-7 p-0 text-slate-500 hover:text-slate-900 hover:bg-slate-100"
                          onClick={() => navigate(`/dashboard/claims/${batch.batchId}`)}
                          title="View Ledger Details"
                        >
                          <Eye size={14} />
                        </Button>
                        <Button 
                          variant="ghost" 
                          size="sm" 
                          className="h-7 w-7 p-0 text-slate-400 hover:text-red-600 hover:bg-red-50"
                          onClick={() => handleDeleteBatch(batch.batchId)}
                          title="Delete Batch"
                        >
                          <Trash2 size={14} />
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
