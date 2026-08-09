import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { claimsApi, ClaimRecord, BatchSummary } from '@/api/claimsApi';
import {
  FileSignature,
  FileSpreadsheet,
  Eye,
  X,
  ClipboardList,
  Search,
  ArrowLeft,
  Briefcase,
  Building2,
  Coins,
  RefreshCw,
  Award
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
      const list = Array.isArray(data) ? data : [];
      setBatches(list);
      if (!selectedBatch && list.length > 0) {
        setSelectedBatch(list[0].batchId);
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
      setClaims(Array.isArray(data) ? data : []);
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
        c.staffName?.toLowerCase().includes(q) ||
        c.institutionName?.toLowerCase().includes(q) ||
        c.boardName?.toLowerCase().includes(q) ||
        c.postHeld?.toLowerCase().includes(q)
      ) as boolean;
    }

    return matchesFilter && matchesSearch;
  });

  const getPostBadgeColor = (postHeld: string) => {
    if (!postHeld) return 'bg-slate-100 text-slate-600 border-slate-200';
    const p = postHeld.toUpperCase();
    if (p.includes('CHIEF')) return 'bg-indigo-50 text-indigo-700 border-indigo-200 font-bold';
    if (p.includes('ASSISTANT')) return 'bg-amber-50 text-amber-700 border-amber-200 font-bold';
    return 'bg-slate-100 text-slate-700 border-slate-200 font-bold';
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

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto bg-slate-50/50 min-h-screen">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <button
            onClick={() => navigate('/dashboard/claims')}
            className="flex items-center gap-2 text-xs font-bold text-indigo-600 hover:text-indigo-800 transition-colors uppercase tracking-wider mb-2"
          >
            <ArrowLeft className="h-4 w-4" /> Theory Remuneration Directory
          </button>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
              <ClipboardList className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Theory Remuneration Master Ledger</h1>
              <p className="text-sm text-slate-500 font-medium mt-0.5">
                {selectedBatch
                  ? `Batch ID: ${selectedBatch.substring(0, 8).toUpperCase()} • ${claims.length} Records Processed`
                  : 'Select a batch to inspect claims'}
              </p>
            </div>
          </div>
        </div>

        {selectedBatch && (
          <div className="flex flex-wrap items-center gap-2 shrink-0">
            <a
              href={claimsApi.getDownloadWordUrl(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-3.5 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSignature className="h-4 w-4 text-indigo-600" /> Export Word
            </a>
            <a
              href={claimsApi.getDownloadExcel1Url(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-3.5 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet className="h-4 w-4 text-emerald-600" /> XLS Detailed
            </a>
            <a
              href={claimsApi.getDownloadExcel2Url(selectedBatch)}
              className="inline-flex items-center justify-center gap-2 h-9 px-3.5 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 transition-colors shadow-sm"
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileSpreadsheet className="h-4 w-4 text-amber-600" /> XLS Summary
            </a>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Column: Batch Selection Directory */}
        <div className="lg:col-span-1 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider">Active Ledgers</h3>
            {uniqueSeasons.length > 0 && (
              <select
                className="h-8 px-2 rounded-xl bg-white border border-slate-200 text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
                value={selectedSeasonFilter}
                onChange={(e) => setSelectedSeasonFilter(e.target.value)}
              >
                <option value="ALL">All Seasons</option>
                {uniqueSeasons.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="flex flex-col gap-2 max-h-[600px] overflow-y-auto pr-1">
            {filteredBatches.length === 0 ? (
              <div className="text-xs text-slate-500 font-medium p-4 text-center bg-white rounded-2xl border border-slate-200">
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
                    className={`flex flex-col p-4 rounded-2xl border text-left transition-all ${
                      isSelected
                        ? 'bg-slate-900 border-slate-900 text-white shadow-md'
                        : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300 hover:bg-slate-50 shadow-sm'
                    }`}
                  >
                    <div className="flex justify-between items-center mb-1">
                      <div className="font-bold text-sm">{displayDate}</div>
                      <span
                        className={`text-xs font-extrabold px-2 py-0.5 rounded-lg ${
                          isSelected ? 'bg-slate-800 text-indigo-300' : 'bg-indigo-50 text-indigo-700 border border-indigo-100'
                        }`}
                      >
                        {b.totalRecords}
                      </span>
                    </div>
                    <div className="flex justify-between items-center w-full mt-2">
                      <span className={`font-mono text-xs ${isSelected ? 'text-slate-400' : 'text-slate-500'}`}>
                        {b.batchId.substring(0, 8).toUpperCase()}
                      </span>
                      {b.examSeason && (
                        <span className={`text-[10px] font-bold uppercase tracking-wider ${isSelected ? 'text-slate-300' : 'text-slate-600'}`}>
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
          {/* Filters and Search Bar */}
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
            <div className="flex bg-slate-100 p-1 rounded-xl">
              {[
                { id: 'all', label: `All (${claims.length})` },
                { id: 'examiner', label: 'Examiners' },
                { id: 'chief', label: 'Chief' },
                { id: 'assistant', label: 'Assistant' },
              ].map((t) => (
                <button
                  key={t.id}
                  className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-all ${
                    filter === t.id
                      ? 'bg-white text-indigo-600 shadow-sm border border-slate-200'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                  onClick={() => setFilter(t.id)}
                >
                  {t.label}
                </button>
              ))}
            </div>

            <div className="relative w-full max-w-sm flex items-center">
              <Search className="absolute left-3.5 h-4 w-4 text-slate-400" />
              <Input
                type="text"
                placeholder="Search staff, institution, board..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 h-10 text-xs font-semibold bg-slate-50 border-slate-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Claims Table */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-32 bg-white rounded-2xl border border-slate-200 shadow-sm">
              <RefreshCw className="h-6 w-6 text-indigo-600 animate-spin mb-3" />
              <div className="text-slate-500 font-semibold text-sm">Querying theory ledger records...</div>
            </div>
          ) : filteredClaims.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-24 bg-white rounded-2xl border border-slate-200 shadow-sm text-center">
              <ClipboardList className="h-10 w-10 text-slate-300 mb-3" />
              <div className="text-sm font-bold text-slate-900">No records matched</div>
              <div className="text-slate-500 text-xs font-medium mt-1">Adjust search parameters or select a different ledger.</div>
            </div>
          ) : (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader className="bg-slate-50 border-b border-slate-200">
                    <TableRow>
                      <TableHead className="font-bold text-xs text-slate-600 uppercase tracking-wider h-11 w-16">Seq</TableHead>
                      <TableHead className="font-bold text-xs text-slate-600 uppercase tracking-wider h-11">Entity Name</TableHead>
                      <TableHead className="font-bold text-xs text-slate-600 uppercase tracking-wider h-11">Role</TableHead>
                      <TableHead className="font-bold text-xs text-slate-600 uppercase tracking-wider h-11">Institution</TableHead>
                      <TableHead className="font-bold text-xs text-slate-600 uppercase tracking-wider h-11 text-right">Scripts</TableHead>
                      <TableHead className="text-right font-bold text-xs text-slate-600 uppercase tracking-wider h-11">TA / DA (₹)</TableHead>
                      <TableHead className="text-right font-bold text-xs text-slate-600 uppercase tracking-wider h-11">Net Payable</TableHead>
                      <TableHead className="text-right font-bold text-xs text-slate-600 uppercase tracking-wider h-11 w-24">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody className="divide-y divide-slate-100 font-medium">
                    {filteredClaims.map((claim, index) => (
                      <TableRow key={claim.id} className="hover:bg-slate-50/70 transition-colors group">
                        <TableCell className="text-slate-400 font-mono text-xs font-bold">
                          {(claim.serialNumber || index + 1).toString().padStart(3, '0')}
                        </TableCell>
                        <TableCell>
                          <div className="font-bold text-slate-900 text-sm">
                            {claim.nameTitle}
                            {claim.staffName}
                          </div>
                          <div className="text-[10px] text-indigo-600 font-bold uppercase tracking-wider mt-0.5">
                            {claim.boardName}
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-lg text-[10px] tracking-wider uppercase whitespace-nowrap border ${getPostBadgeColor(claim.postHeld)}`}>
                            {claim.postHeld}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="max-w-[200px] truncate text-slate-700 text-xs font-semibold" title={claim.institutionName}>
                            {claim.institutionName}
                          </div>
                        </TableCell>
                        <TableCell className="text-right font-mono text-xs text-slate-800 font-bold">
                          {claim.totalScripts}
                        </TableCell>
                        <TableCell className="text-right text-slate-600 font-mono text-xs">
                          {claim.travellingAllowance || 0} / {claim.dearnessAllowance || 0}
                        </TableCell>
                        <TableCell className="text-right font-black text-emerald-600 text-sm">
                          ₹{(claim.totalAmount || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              variant="outline"
                              size="sm"
                              className="h-8 w-8 p-0 border-slate-200 text-slate-600 hover:text-indigo-600 hover:bg-indigo-50"
                              title="Inspect Record"
                              onClick={() => setSelectedClaim(claim)}
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                            <a
                              href={claimsApi.getSingleWordUrl(claim.id)}
                              className="h-8 w-8 rounded-lg border border-slate-200 flex items-center justify-center text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
                              title="Download Word Document"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <FileSignature className="h-4 w-4" />
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

      {/* Claim Detail Modal */}
      {selectedClaim && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200" onClick={() => setSelectedClaim(null)}>
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto border border-slate-200 animate-in zoom-in-95 duration-200 flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="sticky top-0 bg-white border-b border-slate-200 p-6 flex justify-between items-center z-10 shrink-0">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
                  <ClipboardList className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="text-xl font-black text-slate-900 tracking-tight">
                    {selectedClaim.nameTitle}
                    {selectedClaim.staffName}
                  </h3>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mt-0.5">
                    Record ID: {String(selectedClaim.id).padStart(4, '0')}
                  </p>
                </div>
              </div>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0 text-slate-400 hover:bg-slate-100" onClick={() => setSelectedClaim(null)}>
                <X className="h-5 w-5" />
              </Button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-6 flex-1">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Entity Profile */}
                <div>
                  <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                    <Briefcase className="h-4 w-4 text-indigo-600" /> Entity Profile
                  </h4>
                  <div className="space-y-3">
                    <DetailRow label="Designation" value={selectedClaim.designation} />
                    <DetailRow
                      label="Role Assigned"
                      value={
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-lg text-[10px] tracking-wider uppercase border ${getPostBadgeColor(selectedClaim.postHeld)}`}>
                          {selectedClaim.postHeld}
                        </span>
                      }
                    />
                    <DetailRow label="Faculty Type" value={selectedClaim.facultyType} />
                    <DetailRow label="Institution" value={selectedClaim.institutionName} />
                    <DetailRow label="Distance" value={`${selectedClaim.distanceKm} Km (One-way)`} />
                    <DetailRow label="Mobile Contact" value={selectedClaim.mobileNo} />
                  </div>
                </div>

                {/* Financial Breakdown */}
                <div>
                  <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                    <Coins className="h-4 w-4 text-emerald-600" /> Financial Breakdown
                  </h4>
                  <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200 space-y-3">
                    <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                      <span className="text-slate-600">Sessions Fulfilled</span>
                      <span className="text-slate-900 font-bold">{selectedClaim.sessionsAttended}</span>
                    </div>
                    <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                      <span className="text-slate-600">Scripts Evaluated</span>
                      <span className="text-slate-900 font-bold">
                        {selectedClaim.totalScripts} <span className="text-slate-400 font-normal">(FN: {selectedClaim.fnScripts}, AN: {selectedClaim.anScripts})</span>
                      </span>
                    </div>
                    <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                      <span className="text-slate-600">Evaluation Remuneration</span>
                      <span className="font-mono font-bold text-slate-900">₹{(selectedClaim.scriptAmount || 0).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                      <span className="text-slate-600">Travelling Allowance (TA)</span>
                      <span className="font-mono font-bold text-slate-900">₹{(selectedClaim.travellingAllowance || 0).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                      <span className="text-slate-600">Dearness Allowance (DA)</span>
                      <span className="font-mono font-bold text-slate-900">₹{(selectedClaim.dearnessAllowance || 0).toLocaleString('en-IN')}</span>
                    </div>

                    {selectedClaim.maxScriptsValued && (
                      <div className="flex justify-between text-xs pb-2 border-b border-slate-200/60 font-semibold">
                        <span className="text-slate-600">10% Bonus</span>
                        <span className="font-mono font-bold text-slate-900">₹{(selectedClaim.tenPercentAmount || 0).toLocaleString('en-IN')}</span>
                      </div>
                    )}

                    <div className="pt-2">
                      <div className="flex justify-between items-end mb-1">
                        <span className="font-bold text-slate-500 uppercase tracking-wider text-[10px]">Net Payable</span>
                        <span className="text-2xl font-black text-emerald-600 tracking-tight">
                          ₹{(selectedClaim.totalAmount || 0).toLocaleString('en-IN')}
                        </span>
                      </div>
                      <div className="text-xs text-slate-500 font-bold text-right uppercase tracking-wider">
                        {selectedClaim.amountInWords}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Bank Details */}
              <div className="pt-4 border-t border-slate-100">
                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-indigo-600" /> Bank Routing Information
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div className="border border-slate-200 rounded-xl p-3 bg-white">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-0.5">Account Number</div>
                    <div className="text-xs font-mono font-bold text-slate-900">{selectedClaim.bankAccountNumber || '-'}</div>
                  </div>
                  <div className="border border-slate-200 rounded-xl p-3 bg-white">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-0.5">IFSC Code</div>
                    <div className="text-xs font-mono font-bold text-slate-900">{selectedClaim.ifscCode || '-'}</div>
                  </div>
                  <div className="border border-slate-200 rounded-xl p-3 bg-white">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-0.5">Bank Name</div>
                    <div className="text-xs font-bold text-slate-900 truncate" title={selectedClaim.bankName}>
                      {selectedClaim.bankName || '-'}
                    </div>
                  </div>
                  <div className="border border-slate-200 rounded-xl p-3 bg-white">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-0.5">Branch</div>
                    <div className="text-xs font-bold text-slate-900 truncate" title={selectedClaim.branch}>
                      {selectedClaim.branch || '-'}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end gap-3 rounded-b-2xl shrink-0">
              <Button variant="outline" onClick={() => setSelectedClaim(null)} className="h-9 px-4 text-xs font-bold bg-white">
                Close
              </Button>
              <a
                href={claimsApi.getSingleWordUrl(selectedClaim.id)}
                className="inline-flex items-center justify-center gap-2 px-4 h-9 bg-indigo-600 text-white font-bold text-xs rounded-xl hover:bg-indigo-700 transition-colors shadow-sm shadow-indigo-200"
                target="_blank"
                rel="noopener noreferrer"
              >
                <FileSignature className="h-4 w-4" /> Export Document
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between items-start gap-4">
      <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">{label}</span>
      <span className="text-xs font-bold text-slate-900 text-right">{value || '-'}</span>
    </div>
  );
}
