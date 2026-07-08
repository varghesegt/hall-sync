import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  claimsApi,
  ClaimRecord,
  BatchSummary,
} from '@/api/claimsApi';
import { 
  FileSignature, 
  FileSpreadsheet,
  Eye, 
  X, 
  ClipboardList,
  Search,
  ArrowLeft,
  Filter,
  Download,
  Calendar,
  Building2,
  Phone,
  Briefcase,
  Coins
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

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

  const getPostBadgeColor = (postHeld: string) => {
    if (!postHeld) return 'bg-slate-50 text-slate-500 border-slate-200';
    const p = postHeld.toUpperCase();
    if (p.includes('CHIEF')) return 'bg-slate-900 text-slate-50 border-slate-900 shadow-sm';
    if (p.includes('ASSISTANT')) return 'bg-white text-slate-700 border-slate-300 shadow-sm';
    return 'bg-slate-50 text-slate-600 border-slate-200';
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
    <div className="container mx-auto py-8 px-4 lg:px-8 max-w-[1400px] animate-in fade-in duration-500 font-sans">
      
      {/* Enterprise Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8 pb-6 border-b border-slate-200/60">
        <div className="flex flex-col gap-2">
          <button 
            onClick={() => navigate('/dashboard/claims')}
            className="flex items-center gap-2 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors w-max uppercase tracking-widest"
          >
            <ArrowLeft size={14} /> Directory Hub
          </button>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900 mt-1">
            Master Ledger
          </h1>
          <div className="text-slate-500 text-sm font-medium">
            {selectedBatch
              ? `Ledger ID: ${selectedBatch.substring(0, 8).toUpperCase()} • ${claims.length} Records Processed`
              : 'Select a ledger to view claims'}
          </div>
        </div>
        
        {selectedBatch && (
          <div className="flex flex-wrap items-center gap-2">
            <a
              href={claimsApi.getDownloadWordUrl(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-4 rounded-md bg-white border border-slate-200 text-slate-700 font-medium text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSignature size={16} className="text-slate-500" /> Export Word
            </a>
            <a
              href={claimsApi.getDownloadExcel1Url(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-4 rounded-md bg-white border border-slate-200 text-slate-700 font-medium text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet size={16} className="text-slate-500" /> XLS Detailed
            </a>
            <a
              href={claimsApi.getDownloadExcel2Url(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-4 rounded-md bg-white border border-slate-200 text-slate-700 font-medium text-sm hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet size={16} className="text-slate-500" /> XLS Reduced
            </a>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        {/* Left Column: Batch Selection Directory */}
        <div className="lg:col-span-1 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-widest">Active Ledgers</h3>
            {uniqueSeasons.length > 0 && (
              <select
                className="h-7 px-2 rounded-md bg-white border border-slate-200 text-xs font-medium text-slate-700 outline-none hover:bg-slate-50 focus:ring-1 focus:ring-slate-300"
                value={selectedSeasonFilter}
                onChange={(e) => setSelectedSeasonFilter(e.target.value)}
              >
                <option value="ALL">All Seasons</option>
                {uniqueSeasons.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            )}
          </div>
          
          <div className="flex flex-col gap-2 max-h-[600px] overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-slate-200">
            {filteredBatches.length === 0 ? (
              <div className="text-xs text-slate-500 italic p-4 text-center bg-slate-50 rounded-md">
                No ledgers found.
              </div>
            ) : (
              filteredBatches.map((b) => {
                const isSelected = selectedBatch === b.batchId;
                const displayDate = b.valuationDate || b.createdAt.split('T')[0];
                return (
                  <button
                    key={b.batchId}
                    onClick={() => {
                      setSelectedBatch(b.batchId);
                      navigate(`/dashboard/claims/${b.batchId}`);
                    }}
                    className={`flex flex-col p-3 rounded-lg border text-left transition-all ${
                      isSelected 
                        ? 'bg-slate-900 border-slate-900 text-white shadow-md' 
                        : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div className="font-semibold text-sm">
                        {displayDate}
                      </div>
                      <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${isSelected ? 'bg-slate-800 text-slate-300' : 'bg-slate-100 text-slate-500'}`}>
                        {b.totalRecords}
                      </span>
                    </div>
                    <div className="flex justify-between items-center w-full mt-auto">
                      <span className={`font-mono text-[10px] ${isSelected ? 'text-slate-400' : 'text-slate-400'}`}>
                        {b.batchId.substring(0, 8).toUpperCase()}
                      </span>
                      {b.examSeason && (
                        <span className={`text-[10px] font-semibold uppercase tracking-wider ${isSelected ? 'text-slate-300' : 'text-slate-500'}`}>
                          {b.examSeason}
                        </span>
                      )}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Right Column: Data Table */}
        <div className="lg:col-span-3 flex flex-col gap-4">
          
          {/* Filters and Search */}
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between bg-slate-50/50 p-3 rounded-xl border border-slate-200/60">
            <div className="flex bg-slate-200/50 p-1 rounded-md">
              {[
                { id: 'all', label: `All Records (${claims.length})` },
                { id: 'examiner', label: 'Examiners' },
                { id: 'chief', label: 'Chief' },
                { id: 'assistant', label: 'Assistant' }
              ].map(t => (
                <button
                  key={t.id}
                  className={`px-3 py-1.5 text-xs font-semibold rounded transition-all ${
                    filter === t.id 
                      ? 'bg-white text-slate-900 shadow-sm' 
                      : 'text-slate-500 hover:text-slate-700 hover:bg-slate-200/50'
                  }`}
                  onClick={() => setFilter(t.id)}
                >
                  {t.label}
                </button>
              ))}
            </div>
            
            <div className="relative w-full max-w-sm flex items-center">
              <Search size={14} className="absolute left-3 text-slate-400" />
              <Input
                type="text"
                placeholder="Search staff, institution, board..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 h-9 text-xs font-medium bg-white border-slate-200 focus:ring-1 focus:ring-slate-300 focus:border-slate-300"
              />
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-32 bg-white rounded-xl border border-slate-200">
              <div className="w-5 h-5 border-2 border-slate-200 border-t-slate-800 rounded-full animate-spin mb-4" />
              <div className="text-slate-500 font-medium text-sm">Querying ledger records...</div>
            </div>
          ) : filteredClaims.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-32 bg-slate-50/50 rounded-xl border border-slate-200 border-dashed">
              <ClipboardList size={32} className="text-slate-300 mb-3" />
              <div className="text-sm font-semibold text-slate-900">No records matched</div>
              <div className="text-slate-500 text-xs mt-1">Adjust filters or select a different ledger.</div>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-slate-200/80 shadow-[0_1px_2px_rgba(0,0,0,0.02)] overflow-hidden">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader className="bg-slate-50/80 border-b border-slate-200">
                    <TableRow>
                      <TableHead className="font-semibold text-xs text-slate-500 uppercase tracking-wider h-10 w-16">Seq</TableHead>
                      <TableHead className="font-semibold text-xs text-slate-500 uppercase tracking-wider h-10">Entity Name</TableHead>
                      <TableHead className="font-semibold text-xs text-slate-500 uppercase tracking-wider h-10">Role</TableHead>
                      <TableHead className="font-semibold text-xs text-slate-500 uppercase tracking-wider h-10">Institution</TableHead>
                      <TableHead className="font-semibold text-xs text-slate-500 uppercase tracking-wider h-10 text-right">Scripts</TableHead>
                      <TableHead className="text-right font-semibold text-xs text-slate-500 uppercase tracking-wider h-10">Allowance (TA/DA)</TableHead>
                      <TableHead className="text-right font-semibold text-xs text-slate-500 uppercase tracking-wider h-10">Net Payable</TableHead>
                      <TableHead className="text-right font-semibold text-xs text-slate-500 uppercase tracking-wider h-10 w-24">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody className="divide-y divide-slate-100">
                    {filteredClaims.map((claim, index) => (
                      <TableRow key={claim.id} className="hover:bg-slate-50/50 transition-colors group h-14">
                        <TableCell className="text-slate-400 font-mono text-xs">
                          {(claim.serialNumber || index + 1).toString().padStart(3, '0')}
                        </TableCell>
                        <TableCell>
                          <div className="font-semibold text-slate-900 text-sm">
                            {claim.nameTitle}{claim.staffName}
                          </div>
                          <div className="text-[10px] text-slate-500 font-medium uppercase tracking-widest mt-0.5">
                            {claim.boardName}
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-bold tracking-widest uppercase whitespace-nowrap border ${getPostBadgeColor(claim.postHeld)}`}>
                            {claim.postHeld}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="max-w-[200px] truncate text-slate-600 text-sm font-medium" title={claim.institutionName}>
                            {claim.institutionName}
                          </div>
                        </TableCell>
                        <TableCell className="text-right font-mono text-xs text-slate-700 font-medium">
                          {claim.totalScripts}
                        </TableCell>
                        <TableCell className="text-right text-slate-500 font-mono text-xs">
                          {claim.travellingAllowance || 0} / {claim.dearnessAllowance || 0}
                        </TableCell>
                        <TableCell className="text-right font-semibold text-slate-900 text-sm">
                          ₹{(claim.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-1 opacity-60 group-hover:opacity-100 transition-opacity">
                            <button
                              className="w-7 h-7 rounded flex items-center justify-center text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
                              title="Inspect Record"
                              onClick={() => setSelectedClaim(claim)}
                            >
                              <Eye size={14} />
                            </button>
                            <a
                              href={claimsApi.getSingleWordUrl(claim.id)}
                              className="w-7 h-7 rounded flex items-center justify-center text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
                              title="Download Word Document"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <FileSignature size={14} />
                            </a>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Claim Detail Modal - Enterprise Style */}
      {selectedClaim && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200" onClick={() => setSelectedClaim(null)}>
          <div 
            className="bg-white rounded-xl shadow-[0_20px_40px_rgba(0,0,0,0.1)] w-full max-w-3xl max-h-[90vh] overflow-y-auto border border-slate-200 animate-in zoom-in-95 duration-200 flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="sticky top-0 bg-white border-b border-slate-200 p-6 flex justify-between items-start z-10 shrink-0">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-md bg-slate-100 text-slate-600 flex items-center justify-center shrink-0 border border-slate-200">
                  <ClipboardList size={20} />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-900 tracking-tight">
                    {selectedClaim.nameTitle}{selectedClaim.staffName}
                  </h3>
                  <div className="text-xs font-semibold text-slate-500 uppercase tracking-widest mt-1">
                    Record ID: {String(selectedClaim.id).padStart(4, '0')}
                  </div>
                </div>
              </div>
              <button
                className="w-8 h-8 flex items-center justify-center rounded-md text-slate-400 hover:bg-slate-100 hover:text-slate-900 transition-colors"
                onClick={() => setSelectedClaim(null)}
              >
                <X size={18} />
              </button>
            </div>
            
            {/* Modal Body */}
            <div className="p-8 flex-1">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
                
                {/* Entity Profile */}
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
                    <Briefcase size={14} /> Entity Profile
                  </h4>
                  <div className="space-y-4">
                    <DetailRow label="Designation" value={selectedClaim.designation} />
                    <DetailRow label="Role Assigned" value={
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-bold tracking-widest uppercase whitespace-nowrap border ${getPostBadgeColor(selectedClaim.postHeld)}`}>
                        {selectedClaim.postHeld}
                      </span>
                    } />
                    <DetailRow label="Faculty Type" value={selectedClaim.facultyType} />
                    <DetailRow label="Institution" value={selectedClaim.institutionName} />
                    <DetailRow label="Distance" value={`${selectedClaim.distanceKm} Km (One-way)`} />
                    <DetailRow label="Mobile Contact" value={selectedClaim.mobileNo} />
                  </div>
                </div>
                
                {/* Financial Breakdown */}
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
                    <Coins size={14} /> Financial Breakdown
                  </h4>
                  <div className="bg-slate-50 rounded-lg p-5 border border-slate-200 space-y-4">
                    <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                      <span className="text-slate-600 font-medium">Sessions Fulfilled</span>
                      <span className="font-semibold text-slate-900">{selectedClaim.sessionsAttended}</span>
                    </div>
                    <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                      <span className="text-slate-600 font-medium">Scripts Evaluated</span>
                      <span className="font-semibold text-slate-900">{selectedClaim.totalScripts} <span className="text-slate-400 font-normal text-xs">(FN: {selectedClaim.fnScripts}, AN: {selectedClaim.anScripts})</span></span>
                    </div>
                    <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                      <span className="text-slate-600 font-medium">Evaluation Remuneration</span>
                      <span className="font-mono font-medium text-slate-900">₹{(selectedClaim.scriptAmount || 0).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                      <span className="text-slate-600 font-medium">Travelling Allowance (TA)</span>
                      <span className="font-mono font-medium text-slate-900">₹{(selectedClaim.travellingAllowance || 0).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                      <span className="text-slate-600 font-medium">Dearness Allowance (DA)</span>
                      <span className="font-mono font-medium text-slate-900">₹{(selectedClaim.dearnessAllowance || 0).toLocaleString('en-IN')}</span>
                    </div>
                    
                    {selectedClaim.maxScriptsValued && (
                      <div className="flex justify-between text-sm pb-3 border-b border-slate-200/60">
                        <span className="text-slate-600 font-medium">10% Performance Bonus</span>
                        <span className="font-mono font-medium text-slate-900">₹{(selectedClaim.tenPercentAmount || 0).toLocaleString('en-IN')}</span>
                      </div>
                    )}
                    
                    <div className="pt-2">
                      <div className="flex justify-between items-end mb-2">
                        <span className="font-semibold text-slate-500 uppercase tracking-widest text-[10px]">Net Payable</span>
                        <span className="text-2xl font-bold text-slate-900 tracking-tight">
                          ₹{(selectedClaim.totalAmount || 0).toLocaleString('en-IN')}
                        </span>
                      </div>
                      <div className="text-xs text-slate-500 font-medium text-right uppercase tracking-wider">
                        {selectedClaim.amountInWords}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Bank Details */}
              <div className="mt-8 pt-8 border-t border-slate-100">
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
                  <Building2 size={14} /> Bank Routing Information
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="border border-slate-200 rounded-md p-3 bg-white">
                    <div className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest mb-1">Account Number</div>
                    <div className="text-sm font-mono text-slate-900 font-medium">{selectedClaim.bankAccountNumber || '-'}</div>
                  </div>
                  <div className="border border-slate-200 rounded-md p-3 bg-white">
                    <div className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest mb-1">IFSC Code</div>
                    <div className="text-sm font-mono text-slate-900 font-medium">{selectedClaim.ifscCode || '-'}</div>
                  </div>
                  <div className="border border-slate-200 rounded-md p-3 bg-white">
                    <div className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest mb-1">Bank Name</div>
                    <div className="text-sm text-slate-900 font-medium truncate" title={selectedClaim.bankName}>{selectedClaim.bankName || '-'}</div>
                  </div>
                  <div className="border border-slate-200 rounded-md p-3 bg-white">
                    <div className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest mb-1">Branch</div>
                    <div className="text-sm text-slate-900 font-medium truncate" title={selectedClaim.branch}>{selectedClaim.branch || '-'}</div>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Modal Footer */}
            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end gap-3 rounded-b-xl shrink-0">
              <Button variant="outline" onClick={() => setSelectedClaim(null)} className="h-9 px-4 text-xs font-medium">
                Close
              </Button>
              <a
                href={claimsApi.getSingleWordUrl(selectedClaim.id)}
                className="inline-flex items-center justify-center gap-2 px-4 h-9 bg-slate-900 text-white font-medium text-xs rounded-md hover:bg-slate-800 transition-colors shadow-sm"
                target="_blank"
                rel="noopener noreferrer"
              >
                <FileSignature size={14} /> Export Document
              </a>
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
      <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest">{label}</span>
      <span className="text-sm font-medium text-slate-900 text-right">{value || '-'}</span>
    </div>
  );
}
