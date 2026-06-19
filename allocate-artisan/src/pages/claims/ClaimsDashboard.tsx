import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  claimsApi,
  UploadResponse,
  BatchSummary,
} from '../../api/claimsApi';
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
  Trash2 
} from 'lucide-react';
import { Button } from '../../components/ui/button';

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
      setError('Please fill in Exam Season and Valuation Date before uploading.');
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
      setError('Please upload a valid Excel file (.xlsx or .xls)');
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
    if (!confirm('Are you sure you want to delete this batch?')) return;
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
    <div className="w-full max-w-7xl mx-auto py-8 px-4 animate-in fade-in duration-500 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex flex-col gap-2">
          <div className="inline-flex items-center w-max px-3 py-1 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs font-bold uppercase tracking-widest">
            <Zap size={14} className="mr-1.5" /> Office of the Controller of Examinations
          </div>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-slate-900 mt-2">
            Claims Processing Engine
          </h1>
          <p className="text-slate-500 max-w-3xl text-sm sm:text-base leading-relaxed">
            Upload valuation claim data from Excel, automatically calculate TA & DA,
            and generate production-ready Word and Excel claim forms.
          </p>
        </div>
        {batches.length > 0 && (
          <Button 
            onClick={() => navigate(`/dashboard/claims/${batches[0].batchId}`)}
            className="shrink-0 bg-slate-900 hover:bg-slate-800 text-white gap-2"
          >
            <Eye size={16} /> View All Claims
          </Button>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center group hover:border-purple-200 hover:shadow-md transition-all">
          <div className="w-10 h-10 rounded-full bg-purple-50 text-purple-600 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
            <ClipboardList size={20} />
          </div>
          <div className="text-2xl font-black text-slate-800">{totalClaims}</div>
          <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mt-1">Total Claims</div>
        </div>
        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center group hover:border-blue-200 hover:shadow-md transition-all">
          <div className="w-10 h-10 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
            <Package size={20} />
          </div>
          <div className="text-2xl font-black text-slate-800">{totalBatchesCount}</div>
          <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mt-1">Batches</div>
        </div>
        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center group hover:border-emerald-200 hover:shadow-md transition-all">
          <div className="w-10 h-10 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
            <IndianRupee size={20} />
          </div>
          <div className="text-2xl font-black text-slate-800">₹{totalAmount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</div>
          <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mt-1">Total Amount</div>
        </div>
        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col items-center justify-center text-center group hover:border-cyan-200 hover:shadow-md transition-all">
          <div className="w-10 h-10 rounded-full bg-cyan-50 text-cyan-600 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
            <CalendarClock size={20} />
          </div>
          <div className="text-lg font-black text-slate-800 truncate w-full px-2">{examPeriodLabel}</div>
          <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mt-1">Exam Period</div>
        </div>
      </div>

      {/* Upload Zone */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center gap-2">
          <UploadCloud size={18} className="text-indigo-600" />
          <h2 className="text-base font-bold text-slate-800">Upload Claims Data</h2>
        </div>
        <div className="p-6">
          {/* Configuration Form */}
          <div className="bg-slate-50 rounded-xl p-5 border border-slate-200 mb-6">
            <h3 className="text-sm font-bold text-slate-800 mb-4">Pre-Upload Configuration</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Exam Season</label>
                <input 
                  type="text" 
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm" 
                  placeholder="e.g. MAY 2026" 
                  value={examSeason}
                  onChange={(e) => setExamSeason(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Valuation Date</label>
                <input 
                  type="date" 
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm" 
                  value={valuationDate}
                  onChange={(e) => {
                    const dateVal = e.target.value;
                    setValuationDate(dateVal);
                    if (dateVal) {
                      const date = new Date(dateVal);
                      if (date.getDay() === 0) { // Sunday
                        setGovernmentHoliday(true);
                      }
                    }
                  }}
                />
              </div>
              <div className="flex flex-col justify-center">
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Government Holiday / Sunday?</label>
                <div className="flex gap-4 items-center h-10">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="radio" 
                      name="govHoliday"
                      className="text-indigo-600 focus:ring-indigo-500 w-4 h-4"
                      checked={governmentHoliday === true}
                      onChange={() => setGovernmentHoliday(true)}
                    />
                    <span className={`text-sm font-medium ${governmentHoliday ? 'text-emerald-600' : 'text-slate-500'}`}>
                      Yes (Full DA for Home Inst)
                    </span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="radio" 
                      name="govHoliday"
                      className="text-indigo-600 focus:ring-indigo-500 w-4 h-4"
                      checked={governmentHoliday === false}
                      onChange={() => setGovernmentHoliday(false)}
                    />
                    <span className={`text-sm font-medium ${!governmentHoliday ? 'text-slate-800' : 'text-slate-500'}`}>
                      No (No DA for Home Inst)
                    </span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <div
            className={`border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center transition-all ${
              (!examSeason || !valuationDate) 
                ? 'opacity-60 bg-slate-50 cursor-not-allowed border-slate-200' 
                : isDragging
                  ? 'border-indigo-500 bg-indigo-50/50 cursor-copy'
                  : 'border-slate-300 hover:border-indigo-400 hover:bg-slate-50 cursor-pointer'
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => {
              if (examSeason && valuationDate) {
                fileInputRef.current?.click();
              } else {
                setError('Please fill in Exam Season and Valuation Date before uploading.');
              }
            }}
          >
            <div className={`w-16 h-16 rounded-full flex items-center justify-center mb-4 ${isUploading ? 'bg-indigo-100 text-indigo-600 animate-pulse' : 'bg-slate-100 text-slate-400'}`}>
              <FileSpreadsheet size={32} />
            </div>
            <div className="text-lg font-bold text-slate-800 mb-1">
              {isUploading ? 'Processing...' : 'Drop your Excel file here'}
            </div>
            <div className="text-sm text-slate-500 mb-6">
              or click to browse • Supports .xlsx, .xls files
            </div>
            {!isUploading && (
              <Button type="button" variant="outline" className="gap-2 pointer-events-none">
                <FolderUp size={16} /> Choose File
              </Button>
            )}

            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls,.csv"
              onChange={handleFileSelect}
              className="hidden"
            />

            {isUploading && (
              <div className="w-full max-w-md mt-6">
                <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-indigo-600 transition-all duration-300 ease-out"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
                <div className="text-center text-xs font-bold text-slate-500 uppercase tracking-widest mt-2">
                  {uploadProgress < 100 ? `Processing... ${uploadProgress}%` : 'Upload complete!'}
                </div>
              </div>
            )}
          </div>

          {/* Error */}
          {error && (
            <div className="mt-6 p-4 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 flex flex-col gap-2">
              <div className="flex items-center gap-2 font-bold">
                <XCircle size={18} /> Error
              </div>
              <div className="text-sm">{error}</div>
            </div>
          )}

          {/* Upload Result */}
          {uploadResult && (
            <div className="mt-8 pt-8 border-t border-slate-100 animate-in slide-in-from-bottom-4 duration-500">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 text-center">
                  <div className="text-xl font-black text-slate-800">{uploadResult.totalRecords}</div>
                  <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Records Processed</div>
                </div>
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 text-center">
                  <div className="text-xl font-black text-slate-800">{uploadResult.examiners}</div>
                  <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Examiners</div>
                </div>
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 text-center">
                  <div className="text-xl font-black text-slate-800">{uploadResult.chiefExaminers}</div>
                  <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Chief Examiners</div>
                </div>
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 text-center">
                  <div className="text-xl font-black text-slate-800">{uploadResult.assistantExaminers}</div>
                  <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Asst. Examiners</div>
                </div>
              </div>

              {/* Download buttons */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <a
                  href={claimsApi.getDownloadWordUrl(uploadResult.batchId)}
                  className="flex items-start gap-3 p-4 rounded-xl border border-indigo-200 bg-indigo-50 hover:bg-indigo-100 hover:border-indigo-300 transition-all group"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <div className="w-10 h-10 rounded-lg bg-indigo-600 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                    <FileSignature size={20} />
                  </div>
                  <div>
                    <div className="font-bold text-indigo-900 text-sm">Download Word</div>
                    <div className="text-xs text-indigo-700/70 mt-0.5">Editable .docx format</div>
                  </div>
                </a>
                <a
                  href={claimsApi.getDownloadExcel1Url(uploadResult.batchId)}
                  className="flex items-start gap-3 p-4 rounded-xl border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 hover:border-emerald-300 transition-all group"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <div className="w-10 h-10 rounded-lg bg-emerald-600 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                    <FileSpreadsheet size={20} />
                  </div>
                  <div>
                    <div className="font-bold text-emerald-900 text-sm">Download Excel 1</div>
                    <div className="text-xs text-emerald-700/70 mt-0.5">Detailed Claim Details</div>
                  </div>
                </a>
                <a
                  href={claimsApi.getDownloadExcel2Url(uploadResult.batchId)}
                  className="flex items-start gap-3 p-4 rounded-xl border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 hover:border-emerald-300 transition-all group"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <div className="w-10 h-10 rounded-lg bg-emerald-600 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                    <FileSpreadsheet size={20} />
                  </div>
                  <div>
                    <div className="font-bold text-emerald-900 text-sm">Download Excel 2</div>
                    <div className="text-xs text-emerald-700/70 mt-0.5">Reduced End Sem Claim</div>
                  </div>
                </a>
                <button
                  onClick={() => navigate(`/dashboard/claims/${uploadResult.batchId}`)}
                  className="flex items-start gap-3 p-4 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 hover:border-slate-300 transition-all group text-left"
                >
                  <div className="w-10 h-10 rounded-lg bg-slate-800 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                    <Eye size={20} />
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 text-sm">View Claims</div>
                    <div className="text-xs text-slate-500 mt-0.5">See all {uploadResult.totalRecords} claims</div>
                  </div>
                </button>
              </div>

              {/* Warnings */}
              {uploadResult.warnings && uploadResult.warnings.length > 0 && (
                <div className="mt-6 p-4 rounded-xl border border-amber-200 bg-amber-50 text-amber-800">
                  <div className="flex items-center gap-2 font-bold mb-3">
                    <AlertTriangle size={18} /> {uploadResult.warnings.length} Warning(s)
                  </div>
                  <div className="space-y-1 text-sm">
                    {uploadResult.warnings.slice(0, 10).map((w, i) => (
                      <div key={i} className="flex gap-2">
                        <span className="text-amber-500">•</span>
                        <span>{w}</span>
                      </div>
                    ))}
                    {uploadResult.warnings.length > 10 && (
                      <div className="italic text-amber-600/70 mt-2">
                        ...and {uploadResult.warnings.length - 10} more
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Recent Batches */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mt-12 mb-6">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Recent Batches</h2>
          <p className="text-sm text-slate-500">Previously uploaded claim batches</p>
        </div>
        <div className="flex items-center gap-3">
          {uniqueSeasons.length > 0 && (
            <div className="flex items-center gap-2">
              <label htmlFor="season-filter" className="text-xs font-bold text-slate-500 uppercase">
                Season
              </label>
              <select
                id="season-filter"
                className="h-9 px-3 rounded-md border border-slate-300 text-sm font-medium text-slate-700 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
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
          <Button variant="outline" size="sm" onClick={loadBatches} className="gap-2">
            <RefreshCw size={14} /> Refresh
          </Button>
        </div>
      </div>

      {batches.length === 0 ? (
        <div className="bg-slate-50 border border-slate-200 border-dashed rounded-2xl p-12 flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 rounded-full bg-white shadow-sm flex items-center justify-center text-slate-400 mb-4">
            <ArchiveX size={32} />
          </div>
          <h3 className="text-lg font-bold text-slate-800">No batches yet</h3>
          <p className="text-slate-500 max-w-sm mt-1">Upload an Excel file above to generate your first batch of claims.</p>
        </div>
      ) : filteredBatches.length === 0 ? (
        <div className="bg-slate-50 border border-slate-200 border-dashed rounded-2xl p-12 flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 rounded-full bg-white shadow-sm flex items-center justify-center text-slate-400 mb-4">
            <ArchiveX size={32} />
          </div>
          <h3 className="text-lg font-bold text-slate-800">No matches found</h3>
          <p className="text-slate-500 mt-1">No batches found for the season "{selectedSeasonFilter}"</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pb-12">
          {filteredBatches.map((batch) => (
            <div key={batch.batchId} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 hover:shadow-md transition-shadow flex flex-col">
              <div className="flex justify-between items-start mb-4">
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-sm font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
                      #{batch.batchId.substring(0, 8)}
                    </span>
                    {batch.examSeason && (
                      <span className="text-[10px] font-bold uppercase tracking-wider bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full border border-indigo-100">
                        {batch.examSeason}
                      </span>
                    )}
                  </div>
                  <span className="text-sm font-bold text-slate-900">
                    {batch.valuationDate || batch.createdAt.split('T')[0]}
                  </span>
                </div>
                <div className="text-[10px] text-slate-400 font-medium bg-slate-50 px-2 py-1 rounded">
                  {batch.createdAt.split('T')[0]}
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2 mb-5">
                <div className="bg-slate-50 rounded-lg p-2 text-center">
                  <div className="text-lg font-black text-slate-700">{batch.examiners}</div>
                  <div className="text-[9px] font-bold text-slate-500 uppercase tracking-wider mt-0.5">Examiners</div>
                </div>
                <div className="bg-slate-50 rounded-lg p-2 text-center">
                  <div className="text-lg font-black text-slate-700">{batch.chiefExaminers}</div>
                  <div className="text-[9px] font-bold text-slate-500 uppercase tracking-wider mt-0.5">Chief</div>
                </div>
                <div className="bg-slate-50 rounded-lg p-2 text-center">
                  <div className="text-lg font-black text-slate-700">{batch.assistantExaminers}</div>
                  <div className="text-[9px] font-bold text-slate-500 uppercase tracking-wider mt-0.5">Asst</div>
                </div>
              </div>

              <div className="flex justify-between items-end mb-6 pb-5 border-b border-slate-100">
                <span className="text-sm font-bold text-slate-500">{batch.totalRecords} claims</span>
                <span className="text-xl font-black text-emerald-600">
                  ₹{(batch.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                </span>
              </div>

              <div className="mt-auto flex flex-col gap-2">
                <div className="grid grid-cols-2 gap-2">
                  <Button 
                    variant="outline" 
                    className="w-full gap-2"
                    onClick={() => navigate(`/dashboard/claims/${batch.batchId}`)}
                  >
                    <Eye size={14} /> View
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full gap-2 border-indigo-200 text-indigo-700 hover:bg-indigo-50"
                    onClick={() => window.open(claimsApi.getDownloadWordUrl(batch.batchId), '_blank')}
                  >
                    <FileSignature size={14} /> Word
                  </Button>
                </div>
                <div className="flex gap-2">
                  <Button 
                    variant="outline" 
                    className="flex-1 gap-2 border-emerald-200 text-emerald-700 hover:bg-emerald-50 px-2"
                    title="Consolidated Sheet 1 (Detailed)"
                    onClick={() => window.open(claimsApi.getDownloadExcel1Url(batch.batchId), '_blank')}
                  >
                    <FileSpreadsheet size={14} /> Excel 1
                  </Button>
                  <Button 
                    variant="outline" 
                    className="flex-1 gap-2 border-emerald-200 text-emerald-700 hover:bg-emerald-50 px-2"
                    title="Consolidated Sheet 2 (Reduced)"
                    onClick={() => window.open(claimsApi.getDownloadExcel2Url(batch.batchId), '_blank')}
                  >
                    <FileSpreadsheet size={14} /> Excel 2
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-10 px-0 shrink-0 border-rose-200 text-rose-600 hover:bg-rose-50 hover:text-rose-700 hover:border-rose-300"
                    onClick={() => handleDeleteBatch(batch.batchId)}
                    title="Delete Batch"
                  >
                    <Trash2 size={16} />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
