import { useState } from "react";
import { Button } from "@/components/ui/button";
import type { IntegritySummary } from "@/api/allocationApi";
import { ShieldCheck, AlertTriangle, ChevronDown, ChevronUp, Sparkles, ArrowRight, RefreshCw, SlidersHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";

interface IntegrityAuditBannerProps {
  integrity: IntegritySummary | null;
  onDownloadCertificate: () => void;
  isDownloadingCert: boolean;
}

export function IntegrityAuditBanner({ integrity, onDownloadCertificate, isDownloadingCert }: IntegrityAuditBannerProps) {
  const [showDetails, setShowDetails] = useState(false);

  if (!integrity) return null;

  const isVerified = integrity.certified && integrity.totalViolations === 0;

  return (
    <div className={cn(
      "rounded-xl border p-4 shadow-xs transition-all duration-300 space-y-3",
      isVerified
        ? "border-emerald-200 bg-gradient-to-br from-emerald-50/90 via-teal-50/70 to-emerald-50/90"
        : "border-amber-300 bg-gradient-to-br from-amber-50/90 via-orange-50/70 to-amber-50/90"
    )}>
      {/* Header Line */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className={cn(
            "p-2 rounded-full border",
            isVerified ? "bg-emerald-100/80 border-emerald-200 text-emerald-600" : "bg-amber-100/80 border-amber-300 text-amber-600"
          )}>
            {isVerified ? <ShieldCheck className="h-5 w-5 shrink-0" /> : <AlertTriangle className="h-5 w-5 shrink-0" />}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="font-bold text-slate-900 text-sm">Seating Integrity Audit</h4>
              <span className={cn(
                "px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-wide uppercase shadow-2xs",
                isVerified
                  ? "bg-emerald-100 text-emerald-800 border border-emerald-300"
                  : "bg-amber-100 text-amber-900 border border-amber-300"
              )}>
                {isVerified ? "✓ VERIFIED 100%" : `${integrity.totalViolations} ADJACENCY WARNINGS`}
              </span>
            </div>
            <p className="text-xs text-slate-600 mt-0.5">
              {isVerified
                ? "Mathematical verification confirmed zero subject & department adjacency across all halls"
                : "Subject adjacency warnings detected due to high subject concentration or room limits"}
            </p>
          </div>
        </div>
        <div className="text-right">
          <span className={cn("text-2xl font-black tracking-tight", isVerified ? "text-emerald-700" : "text-amber-700")}>
            {integrity.integrityScore}%
          </span>
          <div className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Integrity Score</div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-4 gap-2 text-center bg-white/80 backdrop-blur-xs rounded-lg p-2.5 border border-slate-200/60 shadow-2xs">
        <div>
          <div className="text-xs font-extrabold text-slate-800">{integrity.totalStudents}</div>
          <div className="text-[10px] text-slate-500 font-medium">Students</div>
        </div>
        <div>
          <div className="text-xs font-extrabold text-slate-800">{integrity.totalHalls}</div>
          <div className="text-[10px] text-slate-500 font-medium">Halls</div>
        </div>
        <div>
          <div className="text-xs font-extrabold text-slate-800">{integrity.totalAdjacencyChecks}</div>
          <div className="text-[10px] text-slate-500 font-medium">Adjacency Checks</div>
        </div>
        <div>
          <div className={cn("text-xs font-extrabold", integrity.totalViolations === 0 ? "text-emerald-600" : "text-red-600")}>
            {integrity.totalViolations}
          </div>
          <div className="text-[10px] text-slate-500 font-medium font-medium">Violations</div>
        </div>
      </div>

      {/* Actionable Solution & Details Toggle when Warnings Exist */}
      {!isVerified && (
        <div className="space-y-2 pt-1">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowDetails(!showDetails)}
            className="w-full justify-between text-xs font-bold text-amber-900 bg-amber-100/60 hover:bg-amber-100 border border-amber-300/60 h-8"
          >
            <span className="flex items-center gap-1.5">
              <Sparkles className="h-3.5 w-3.5 text-amber-700" />
              View Violation Details & Solutions
            </span>
            {showDetails ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </Button>

          {showDetails && (
            <div className="p-3 bg-white rounded-lg border border-amber-200 space-y-3 text-xs shadow-xs animate-in fade-in slide-in-from-top-1 duration-200">
              {/* Violation Details List */}
              {integrity.hallViolations && integrity.hallViolations.length > 0 && (
                <div className="space-y-1.5">
                  <h5 className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
                    <AlertTriangle className="h-3.5 w-3.5 text-red-500" />
                    Detected Adjacency Violations by Hall:
                  </h5>
                  <div className="max-h-36 overflow-y-auto space-y-1 pr-1">
                    {integrity.hallViolations.map((hv, idx) => (
                      <div key={idx} className="p-2 rounded bg-slate-50 border border-slate-200/80">
                        <div className="flex items-center justify-between font-semibold text-slate-800">
                          <span>{hv.hallName}</span>
                          <span className="text-red-600 text-[11px] font-bold">{hv.violations} violation(s)</span>
                        </div>
                        {hv.details && hv.details.length > 0 && (
                          <ul className="mt-1 space-y-0.5 text-[11px] font-mono text-slate-600 list-disc list-inside">
                            {hv.details.map((detail, dIdx) => (
                              <li key={dIdx}>{detail}</li>
                            ))}
                          </ul>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Actionable Solutions */}
              <div className="space-y-1.5 border-t border-slate-100 pt-2">
                <h5 className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
                  <Sparkles className="h-3.5 w-3.5 text-emerald-600" />
                  Recommended Solutions:
                </h5>
                <div className="space-y-1.5 text-[11px] text-slate-700">
                  <div className="flex items-start gap-2 p-1.5 rounded bg-emerald-50/60 border border-emerald-100">
                    <SlidersHorizontal className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold text-slate-800">1. Interactive Drag & Swap:</span> Use the <span className="font-semibold text-emerald-700">Preview Panel</span> below to drag and swap seats between columns to eliminate adjacency instantly.
                    </div>
                  </div>
                  <div className="flex items-start gap-2 p-1.5 rounded bg-blue-50/60 border border-blue-100">
                    <ArrowRight className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold text-slate-800">2. Select Additional Halls:</span> Add 1–2 extra halls during session creation to lower student density per hall.
                    </div>
                  </div>
                  <div className="flex items-start gap-2 p-1.5 rounded bg-purple-50/60 border border-purple-100">
                    <RefreshCw className="h-4 w-4 text-purple-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold text-slate-800">3. Re-Run Allocation:</span> Click <span className="font-semibold text-purple-700">Run Allocation</span> to let the constraint engine calculate an alternate non-adjacent seating pattern.
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Download Integrity Certificate (PDF) Button */}
      <Button
        variant="outline"
        size="sm"
        onClick={onDownloadCertificate}
        disabled={isDownloadingCert}
        className={cn(
          "w-full gap-2 font-semibold shadow-xs transition-all",
          isVerified
            ? "text-emerald-800 border-emerald-300 bg-white hover:bg-emerald-50"
            : "text-amber-900 border-amber-300 bg-white hover:bg-amber-50"
        )}
      >
        {isDownloadingCert ? (
          <><span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-emerald-600/30 border-t-emerald-600" /> Generating Certificate…</>
        ) : (
          <><ShieldCheck className={cn("h-4 w-4", isVerified ? "text-emerald-600" : "text-amber-600")} /> Download Official Integrity Certificate (PDF)</>
        )}
      </Button>
    </div>
  );
}
