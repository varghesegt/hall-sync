import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  claimsApi,
  ClaimRecord,
  BatchSummary,
} from '../../api/claimsApi';
import { 
  FileSignature, 
  FileSpreadsheet,
  Eye, 
  X, 
  ClipboardList,
  Search,
  ArrowLeft,
  Filter,
  Download
} from 'lucide-react';
import { Button } from '../../components/ui/button';

export default function ClaimsList() {
  const { id: batchIdParam } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [claims, setClaims] = useState<ClaimRecord[]>([]);
  const [batches, setBatches] = useState<BatchSummary[]>([]);
  const [selectedBatch, setSelectedBatch] = useState<string | null>(batchIdParam || null);
  const [selectedClaim, setSelectedClaim] = useState<ClaimRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSeasonFilter, setSelectedSeasonFilter] = useState('ALL');

  useEffect(() => {
    loadBatches();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedBatch) {
      loadClaims(selectedBatch);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedBatch]);

  const loadBatches = async () => {
    try {
      const data = await claimsApi.getAllBatches();
      setBatches(data);
      if (!selectedBatch && data.length > 0) {
        setSelectedBatch(data[0].batchId);
      }
    } catch {
      console.error('Failed to load batches');
    }
    setLoading(false);
  };

  const loadClaims = async (batchId: string) => {
    setLoading(true);
    try {
      const data = await claimsApi.getBatchClaims(batchId);
      setClaims(data);
    } catch {
      console.error('Failed to load claims');
    }
    setLoading(false);
  };

  const filteredClaims = claims.filter((c) => {
    let matchesFilter = true;
    if (filter === 'examiner') matchesFilter = c.postHeld?.toUpperCase() === 'EXAMINER';
    if (filter === 'chief') matchesFilter = c.postHeld?.toUpperCase().includes('CHIEF');
    if (filter === 'assistant') matchesFilter = c.postHeld?.toUpperCase().includes('ASSISTANT');

    let matchesSearch = true;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      matchesSearch = (
        (c.staffName?.toLowerCase().includes(q)) ||
        (c.institutionName?.toLowerCase().includes(q)) ||
        (c.boardName?.toLowerCase().includes(q)) ||
        (c.postHeld?.toLowerCase().includes(q))
      ) as boolean;
    }

    return matchesFilter && matchesSearch;
  });

  const getPostBadgeClass = (postHeld: string) => {
    if (!postHeld) return 'bg-slate-100 text-slate-700 border-slate-200';
    const p = postHeld.toUpperCase();
    if (p.includes('CHIEF')) return 'bg-purple-50 text-purple-700 border-purple-200';
    if (p.includes('ASSISTANT')) return 'bg-amber-50 text-amber-700 border-amber-200';
    return 'bg-indigo-50 text-indigo-700 border-indigo-200';
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

  return (
    <div className="w-full max-w-7xl mx-auto py-8 px-4 animate-in fade-in duration-500 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4 mb-2">
        <div className="flex flex-col gap-2">
          <button 
            onClick={() => navigate('/dashboard/claims')}
            className="flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-indigo-600 transition-colors w-max"
          >
            <ArrowLeft size={16} /> Back to Dashboard
          </button>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 mt-1">
            Batch Claims
          </h1>
          <div className="text-slate-500 text-sm font-medium">
            {selectedBatch
              ? `Batch #${selectedBatch.substring(0, 8)} • ${claims.length} claims found`
              : 'Select a batch to view claims'}
          </div>
        </div>
        
        {selectedBatch && (
          <div className="flex flex-wrap items-center gap-2">
            <a
              href={claimsApi.getDownloadWordUrl(selectedBatch)}
              className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSignature size={16} className="text-indigo-600" /> Word
            </a>
            <a
              href={claimsApi.getDownloadExcel1Url(selectedBatch)}
              className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet size={16} className="text-emerald-600" /> Excel 1
            </a>
            <a
              href={claimsApi.getDownloadExcel2Url(selectedBatch)}
              className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet size={16} className="text-emerald-600" /> Excel 2
            </a>
          </div>
        )}
      </div>

      {/* Batch selector */}
      {batches.length > 0 && (
        <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
          <div className="flex justify-between items-center flex-wrap gap-4 mb-4">
            <div className="text-sm font-bold text-slate-700 uppercase tracking-wider">
              Select Batch
            </div>
            {uniqueSeasons.length > 0 && (
              <div className="flex items-center gap-2">
                <Filter size={14} className="text-slate-400" />
                <select
                  className="h-8 px-2 rounded bg-white border border-slate-200 text-xs font-bold text-slate-700 outline-none focus:ring-2 focus:ring-indigo-500"
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
          </div>
          
          {filteredBatches.length === 0 ? (
            <div className="text-sm text-slate-500 italic">
              No batches found for the season "{selectedSeasonFilter}"
            </div>
          ) : (
            <div className="flex flex-wrap gap-2">
              {filteredBatches.map((b) => (
                <button
                  key={b.batchId}
                  className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm transition-all ${
                    selectedBatch === b.batchId 
                      ? 'bg-indigo-600 border-indigo-700 text-white shadow-sm' 
                      : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300 hover:bg-slate-50'
                  }`}
                  onClick={() => {
                    setSelectedBatch(b.batchId);
                    navigate(`/dashboard/claims/${b.batchId}`);
                  }}
                >
                  <span className="font-mono font-bold">#{b.batchId.substring(0, 8)}</span>
                  {b.examSeason && (
                    <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                      selectedBatch === b.batchId ? 'bg-indigo-500/50 text-indigo-50' : 'bg-slate-100 text-slate-500'
                    }`}>
                      {b.examSeason}
                    </span>
                  )}
                  <span className="opacity-80">({b.totalRecords})</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Filters and Search */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex bg-slate-100 p-1 rounded-lg border border-slate-200">
          {[
            { id: 'all', label: `All (${claims.length})` },
            { id: 'examiner', label: 'Examiners' },
            { id: 'chief', label: 'Chief' },
            { id: 'assistant', label: 'Assistant' }
          ].map(t => (
            <button
              key={t.id}
              className={`px-4 py-1.5 text-sm font-bold rounded-md transition-all ${
                filter === t.id 
                  ? 'bg-white text-slate-900 shadow-sm' 
                  : 'text-slate-500 hover:text-slate-700'
              }`}
              onClick={() => setFilter(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>
        
        <div className="relative w-full sm:w-72">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search name, institution..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full h-10 pl-9 pr-4 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none text-sm bg-white"
          />
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200">
          <div className="w-8 h-8 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin mb-4" />
          <div className="text-slate-500 font-bold">Loading claims...</div>
        </div>
      ) : filteredClaims.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200 border-dashed">
          <ClipboardList size={48} className="text-slate-300 mb-4" />
          <div className="text-lg font-bold text-slate-800">No claims found</div>
          <div className="text-slate-500 text-sm mt-1">
            {claims.length === 0
              ? 'Select a batch or upload one from the dashboard.'
              : 'Try adjusting your filters or search.'}
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="px-4 py-3">S.No</th>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Post Held</th>
                  <th className="px-4 py-3">Institution</th>
                  <th className="px-4 py-3">Sessions</th>
                  <th className="px-4 py-3">Scripts</th>
                  <th className="px-4 py-3 text-right">TA / DA</th>
                  <th className="px-4 py-3 text-right">Total Amount</th>
                  <th className="px-4 py-3 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredClaims.map((claim, index) => (
                  <tr key={claim.id} className="hover:bg-slate-50 transition-colors group">
                    <td className="px-4 py-3 text-slate-500 font-medium">
                      {claim.serialNumber || index + 1}
                    </td>
                    <td className="px-4 py-3 font-bold text-slate-800">
                      {claim.nameTitle}{claim.staffName}
                      <div className="text-[10px] text-slate-400 font-normal uppercase tracking-wider">{claim.boardName}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-[10px] font-bold border ${getPostBadgeClass(claim.postHeld)}`}>
                        {claim.postHeld}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="max-w-[200px] truncate text-slate-600" title={claim.institutionName}>
                        {claim.institutionName}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {claim.sessionsAttended}
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-700">
                      {claim.totalScripts}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-600 font-mono text-xs">
                      ₹{claim.travellingAllowance || 0} / ₹{claim.dearnessAllowance || 0}
                    </td>
                    <td className="px-4 py-3 text-right font-black text-emerald-600">
                      ₹{(claim.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          className="w-8 h-8 rounded flex items-center justify-center text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
                          title="View Details"
                          onClick={() => setSelectedClaim(claim)}
                        >
                          <Eye size={16} />
                        </button>
                        <a
                          href={claimsApi.getSingleWordUrl(claim.id)}
                          className="w-8 h-8 rounded flex items-center justify-center text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                          title="Download Word"
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <FileSignature size={16} />
                        </a>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Claim Detail Modal */}
      {selectedClaim && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200" onClick={() => setSelectedClaim(null)}>
          <div 
            className="bg-white rounded-2xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto border border-slate-200 animate-in zoom-in-95 duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="sticky top-0 bg-white/80 backdrop-blur border-b border-slate-100 p-6 flex justify-between items-start z-10">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                  <ClipboardList size={24} />
                </div>
                <div>
                  <h3 className="text-xl font-black text-slate-900">
                    {selectedClaim.nameTitle}{selectedClaim.staffName}
                  </h3>
                  <div className="text-sm font-bold text-slate-500 uppercase tracking-wider mt-0.5">
                    Claim Details Breakdown
                  </div>
                </div>
              </div>
              <button
                className="w-8 h-8 flex items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
                onClick={() => setSelectedClaim(null)}
              >
                <X size={18} />
              </button>
            </div>
            
            {/* Modal Body */}
            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="space-y-4">
                  <DetailRow label="Post Held" value={<span className={`px-2 py-1 rounded text-[10px] font-bold border ${getPostBadgeClass(selectedClaim.postHeld)}`}>{selectedClaim.postHeld}</span>} />
                  <DetailRow label="Mobile" value={selectedClaim.mobileNo} />
                  <DetailRow label="Designation" value={selectedClaim.designation} />
                  <DetailRow label="Institution" value={selectedClaim.institutionName} />
                  <DetailRow label="Faculty Type" value={selectedClaim.facultyType} />
                  <DetailRow label="Distance (One-way)" value={`${selectedClaim.distanceKm} Km`} />
                  <DetailRow label="Sessions" value={selectedClaim.sessionsAttended} />
                </div>
                
                <div className="bg-slate-50 rounded-xl p-5 border border-slate-200 space-y-4">
                  <DetailRow label="Total Scripts" value={`${selectedClaim.totalScripts} (FN: ${selectedClaim.fnScripts}, AN: ${selectedClaim.anScripts})`} />
                  <DetailRow label={selectedClaim.postHeld?.toUpperCase().includes('ASSISTANT') ? 'Base Remuneration' : 'Script Amount'} value={`₹${(selectedClaim.scriptAmount || 0).toLocaleString('en-IN')}`} />
                  <DetailRow label="Travelling Allowance" value={`₹${(selectedClaim.travellingAllowance || 0).toLocaleString('en-IN')}`} />
                  <DetailRow label="Dearness Allowance" value={`₹${(selectedClaim.dearnessAllowance || 0).toLocaleString('en-IN')}`} />
                  
                  {selectedClaim.maxScriptsValued && (
                    <>
                      <DetailRow label="10% Bonus" value={`₹${(selectedClaim.tenPercentAmount || 0).toLocaleString('en-IN')}`} />
                      <DetailRow label="Overall Script Amount" value={`₹${(selectedClaim.overallScriptAmount || 0).toLocaleString('en-IN')}`} />
                    </>
                  )}
                  
                  <div className="pt-4 mt-2 border-t border-slate-200">
                    <div className="flex justify-between items-end mb-2">
                      <span className="font-bold text-slate-500 uppercase tracking-wider text-xs">Total Amount</span>
                      <span className="text-3xl font-black text-emerald-600">
                        ₹{(selectedClaim.totalAmount || 0).toLocaleString('en-IN')}
                      </span>
                    </div>
                    <div className="text-xs text-slate-500 font-medium text-center italic bg-white p-2 rounded border border-slate-100">
                      {selectedClaim.amountInWords}
                    </div>
                  </div>
                </div>
              </div>

              {/* Bank Details */}
              <div className="mt-8 pt-6 border-t border-slate-100">
                <h4 className="text-sm font-bold text-slate-900 mb-4 uppercase tracking-wider flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-indigo-600" /> Bank Details
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <BankField label="Account Number" value={selectedClaim.bankAccountNumber} isMono />
                  <BankField label="IFSC Code" value={selectedClaim.ifscCode} isMono />
                  <BankField label="Bank Name" value={selectedClaim.bankName} />
                  <BankField label="Branch" value={selectedClaim.branch} />
                </div>
              </div>

              {/* Actions */}
              <div className="mt-8 flex justify-end gap-3">
                <Button variant="outline" onClick={() => setSelectedClaim(null)}>
                  Close
                </Button>
                <a
                  href={claimsApi.getSingleWordUrl(selectedClaim.id)}
                  className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white font-bold text-sm rounded-lg hover:bg-indigo-700 transition-colors shadow-sm"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Download size={16} /> Download Claim Form (Word)
                </a>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function DetailRow({ label, value }: { label: string, value: React.ReactNode }) {
  return (
    <div className="flex justify-between items-start gap-4">
      <span className="text-xs font-bold text-slate-500 uppercase tracking-wider mt-0.5">{label}</span>
      <span className="text-sm font-medium text-slate-900 text-right">{value || '-'}</span>
    </div>
  );
}

function BankField({ label, value, isMono }: { label: string, value: React.ReactNode, isMono?: boolean }) {
  return (
    <div className="bg-slate-50 rounded-lg p-3 border border-slate-100">
      <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">{label}</div>
      <div className={`text-sm text-slate-800 ${isMono ? 'font-mono font-bold' : 'font-medium'}`}>{value || '-'}</div>
    </div>
  );
}
