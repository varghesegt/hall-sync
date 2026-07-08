import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { getBatchPreview, swapSeats, updateRegisterNumber, downloadBatchPdf, downloadBatchExcel, type PreviewSeat } from "@/api/allocationApi";
import {
  ArrowLeft, ArrowLeftRight, Building2, Check, ChevronLeft, ChevronRight,
  GripVertical, Pencil, Search, Users, X, AlertTriangle, Undo2,
  ZoomIn, ZoomOut, Eye, EyeOff, LayoutGrid, Maximize2, FileText, FileSpreadsheet
} from "lucide-react";
import { toast } from "sonner";

// ==================== CONSTANTS & HELPERS ====================

const ALL_COLS = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII"];
const DEPT_COLORS: Record<string, string> = {};
const PALETTE = [
  "hsl(221, 83%, 90%)", "hsl(150, 60%, 88%)", "hsl(35, 80%, 88%)",
  "hsl(280, 50%, 90%)", "hsl(0, 55%, 90%)",  "hsl(190, 60%, 88%)",
  "hsl(55, 60%, 88%)",  "hsl(320, 50%, 90%)", "hsl(100, 50%, 88%)",
  "hsl(250, 50%, 90%)", "hsl(170, 50%, 88%)", "hsl(10, 70%, 90%)"
];
let colorIdx = 0;
function getDeptColor(dept: string): string {
  if (!DEPT_COLORS[dept]) {
    DEPT_COLORS[dept] = PALETTE[colorIdx % PALETTE.length];
    colorIdx++;
  }
  return DEPT_COLORS[dept];
}

interface DragData {
  hallId: string;
  seatRow: number;
  seatCol: string;
  registerNumber: string;
}

interface EditingCell {
  hallId: string;
  seatRow: number;
  seatCol: string;
  value: string;
}

interface UndoAction {
  type: "swap" | "edit";
  description: string;
  timestamp: number;
}

// ==================== COMPONENT ====================

export default function FloorPlanEditor() {
  const { batchId } = useParams<{ batchId: string }>();
  const navigate = useNavigate();

  // Data
  const [seats, setSeats] = useState<PreviewSeat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Navigation
  const [currentHallIdx, setCurrentHallIdx] = useState(0);

  // Edit state
  const [editMode, setEditMode] = useState(true);
  const [dragSource, setDragSource] = useState<DragData | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const [editingCell, setEditingCell] = useState<EditingCell | null>(null);
  const [saving, setSaving] = useState(false);
  const editInputRef = useRef<HTMLInputElement>(null);

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState("");
  const [showRiskOnly, setShowRiskOnly] = useState(false);

  // Zoom
  const [zoom, setZoom] = useState(100);

  // History
  const [history, setHistory] = useState<UndoAction[]>([]);

  // ==================== FETCH DATA ====================

  useEffect(() => {
    if (!batchId) return;
    setLoading(true);
    setError(null);
    getBatchPreview(batchId)
      .then(setSeats)
      .catch((e) => setError(e?.message || "Failed to load allocation data"))
      .finally(() => setLoading(false));
  }, [batchId]);

  // ==================== DOWNLOAD HANDLERS ====================

  const handleDownloadPdf = async () => {
    if (!batchId) return;
    try {
      toast.info("Generating PDF...");
      const { blob, filename } = await downloadBatchPdf(batchId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success("PDF Downloaded");
    } catch (err: any) {
      toast.error("Failed to download PDF");
    }
  };

  const handleDownloadExcel = async () => {
    if (!batchId) return;
    try {
      toast.info("Generating Excel...");
      const { blob, filename } = await downloadBatchExcel(batchId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success("Excel Downloaded");
    } catch (err: any) {
      toast.error("Failed to download Excel");
    }
  };

  // ==================== COMPUTED ====================

  const halls = useMemo(() => {
    const map = new Map<string, { hallId: string; hallName: string; seats: PreviewSeat[] }>();
    for (const s of seats) {
      if (!map.has(s.hallId)) {
        map.set(s.hallId, { hallId: s.hallId, hallName: s.hallName || s.hallId, seats: [] });
      }
      map.get(s.hallId)!.seats.push(s);
    }
    return Array.from(map.values()).sort((a, b) => a.hallName.localeCompare(b.hallName, undefined, { numeric: true }));
  }, [seats]);

  const currentHall = halls[currentHallIdx] ?? null;

  const { grid, currentCols, rowNumbers } = useMemo(() => {
    if (!currentHall || currentHall.seats.length === 0) {
      return { grid: null, currentCols: ["I", "II", "III", "IV", "V"], rowNumbers: [1, 2, 3, 4, 5] };
    }
    const g: Record<string, Record<string, PreviewSeat>> = {};
    let maxR = 0;
    const colsSet = new Set<string>();
    for (const s of currentHall.seats) {
      if (!g[s.seatRow]) g[s.seatRow] = {};
      g[s.seatRow][s.seatCol] = s;
      if (s.seatRow > maxR) maxR = s.seatRow;
      colsSet.add(s.seatCol);
    }
    const mR = Math.max(5, maxR);
    const cCols = ALL_COLS.filter(c => colsSet.has(c));
    return {
      grid: { data: g, maxRow: mR },
      currentCols: cCols.length > 0 ? cCols : ["I", "II", "III", "IV", "V"],
      rowNumbers: Array.from({ length: mR }, (_, i) => i + 1)
    };
  }, [currentHall]);

  const colDepts = useMemo(() => {
    if (!currentHall) return {};
    const map: Record<string, Set<string>> = {};
    for (const s of currentHall.seats) {
      if (!map[s.seatCol]) map[s.seatCol] = new Set();
      map[s.seatCol].add(s.department);
    }
    return Object.fromEntries(Object.entries(map).map(([k, v]) => [k, Array.from(v)]));
  }, [currentHall]);

  const allDepts = useMemo(() => {
    const set = new Set<string>();
    for (const s of seats) set.add(s.department);
    return Array.from(set).sort();
  }, [seats]);

  // Search match
  const searchMatches = useMemo(() => {
    if (!searchQuery.trim()) return new Set<string>();
    const q = searchQuery.toLowerCase().trim();
    const matched = new Set<string>();
    for (const s of seats) {
      if (
        s.registerNumber.toLowerCase().includes(q) ||
        s.studentName?.toLowerCase().includes(q) ||
        s.department.toLowerCase().includes(q) ||
        s.subjectCode?.toLowerCase().includes(q)
      ) {
        matched.add(`${s.hallId}:${s.seatRow}:${s.seatCol}`);
      }
    }
    return matched;
  }, [searchQuery, seats]);

  // Risk counts
  const riskCount = useMemo(() => {
    return currentHall?.seats.filter(s => (s.riskScore ?? 0) > 0).length ?? 0;
  }, [currentHall]);

  // Stats
  const totalStudents = seats.length;
  const totalHalls = halls.length;

  // ==================== DRAG HANDLERS ====================

  const handleDragStart = useCallback((e: React.DragEvent, seat: PreviewSeat) => {
    if (!editMode) return;
    const d: DragData = { hallId: seat.hallId, seatRow: seat.seatRow, seatCol: seat.seatCol, registerNumber: seat.registerNumber };
    setDragSource(d);
    e.dataTransfer.setData("text/plain", JSON.stringify(d));
    e.dataTransfer.effectAllowed = "move";
  }, [editMode]);

  const handleDragOver = useCallback((e: React.DragEvent, row: number, col: string) => {
    if (!editMode) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDropTarget(`${row}:${col}`);
  }, [editMode]);

  const handleDragLeave = useCallback(() => setDropTarget(null), []);

  const handleDrop = useCallback(async (e: React.DragEvent, targetRow: number, targetCol: string) => {
    e.preventDefault();
    setDropTarget(null);
    if (!batchId || !dragSource || !currentHall) return;
    if (dragSource.hallId === currentHall.hallId && dragSource.seatRow === targetRow && dragSource.seatCol === targetCol) {
      setDragSource(null);
      return;
    }

    setSaving(true);
    try {
      const refreshed = await swapSeats(batchId, {
        hallIdA: dragSource.hallId, seatRowA: dragSource.seatRow, seatColA: dragSource.seatCol,
        hallIdB: currentHall.hallId, seatRowB: targetRow, seatColB: targetCol,
      });
      setSeats(refreshed);

      const targetSeat = grid?.data?.[targetRow]?.[targetCol];
      const desc = targetSeat
        ? `Swapped ${dragSource.registerNumber} ↔ ${targetSeat.registerNumber}`
        : `Moved ${dragSource.registerNumber} → R${targetRow}:${targetCol}`;
      setHistory(h => [{ type: "swap", description: desc, timestamp: Date.now() }, ...h]);
      toast.success(targetSeat ? "Seats Swapped" : "Student Moved", { description: desc });
    } catch (err: any) {
      toast.error("Swap Failed", { description: err?.message || "Could not swap seats." });
    } finally {
      setSaving(false);
      setDragSource(null);
    }
  }, [batchId, dragSource, currentHall, grid]);

  const handleDragEnd = useCallback(() => { setDragSource(null); setDropTarget(null); }, []);

  // ==================== INLINE EDIT ====================

  const startEditing = useCallback((seat: PreviewSeat) => {
    if (!editMode) return;
    setEditingCell({ hallId: seat.hallId, seatRow: seat.seatRow, seatCol: seat.seatCol, value: seat.registerNumber });
    setTimeout(() => editInputRef.current?.focus(), 50);
  }, [editMode]);

  const cancelEditing = useCallback(() => setEditingCell(null), []);

  const confirmEdit = useCallback(async () => {
    if (!batchId || !editingCell) return;
    const original = grid?.data?.[editingCell.seatRow]?.[editingCell.seatCol];
    if (!original || original.registerNumber === editingCell.value.trim()) { setEditingCell(null); return; }

    setSaving(true);
    try {
      const refreshed = await updateRegisterNumber(batchId, {
        hallId: editingCell.hallId, seatRow: editingCell.seatRow, seatCol: editingCell.seatCol,
        newRegisterNumber: editingCell.value.trim(),
      });
      setSeats(refreshed);
      const desc = `Edited ${original.registerNumber} → ${editingCell.value.trim()}`;
      setHistory(h => [{ type: "edit", description: desc, timestamp: Date.now() }, ...h]);
      toast.success("Register Number Updated", { description: desc });
    } catch (err: any) {
      toast.error("Update Failed", { description: err?.message || "Could not update register number." });
    } finally {
      setSaving(false);
      setEditingCell(null);
    }
  }, [batchId, editingCell, grid]);

  const handleEditKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter") confirmEdit();
    if (e.key === "Escape") cancelEditing();
  }, [confirmEdit, cancelEditing]);

  // ==================== RENDER ====================

  if (!batchId) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <p className="text-muted-foreground">No batch selected.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] gap-3">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary/30 border-t-primary" />
        <p className="text-sm text-muted-foreground">Loading floor plan data…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] gap-4">
        <AlertTriangle className="h-10 w-10 text-destructive" />
        <p className="text-sm text-destructive">{error}</p>
        <Button variant="outline" onClick={() => { if (window.history.length <= 2) window.close(); else navigate(-1); }}>Go Back</Button>
      </div>
    );
  }

  // rowNumbers handles rows array

  return (
    <div className="w-full space-y-4 animate-in fade-in duration-500">
      {/* ==================== TOP BAR ==================== */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => { if (window.history.length <= 2) window.close(); else navigate(-1); }} className="shrink-0">
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-lg font-bold tracking-tight flex items-center gap-2">
              <LayoutGrid className="h-5 w-5 text-primary" />
              Visual Floor Plan Editor
            </h1>
            <p className="text-xs text-muted-foreground">
              Drag students to swap seats · Double-click to edit register numbers · Changes are saved instantly
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <Badge variant="outline" className="gap-1 text-xs">
            <Users className="h-3 w-3" /> {totalStudents}
          </Badge>
          <Badge variant="outline" className="gap-1 text-xs">
            <Building2 className="h-3 w-3" /> {totalHalls} halls
          </Badge>
          {riskCount > 0 && (
            <Badge variant="destructive" className="gap-1 text-xs">
              <AlertTriangle className="h-3 w-3" /> {riskCount} risk
            </Badge>
          )}
          <div className="flex items-center gap-1.5 border-l border-slate-200 pl-2 ml-1">
            <Button variant="outline" size="sm" onClick={handleDownloadPdf} className="h-8 gap-1.5 text-xs font-semibold">
              <FileText className="h-3.5 w-3.5 text-red-500" /> PDF
            </Button>
            <Button variant="outline" size="sm" onClick={handleDownloadExcel} className="h-8 gap-1.5 text-xs font-semibold">
              <FileSpreadsheet className="h-3.5 w-3.5 text-green-600" /> Excel
            </Button>
          </div>
          <Button
            variant={editMode ? "default" : "outline"}
            size="sm"
            onClick={() => { setEditMode(!editMode); setEditingCell(null); }}
            className={`gap-1.5 text-xs h-8 ${editMode ? "bg-primary hover:bg-primary/90 text-white" : ""}`}
          >
            {editMode ? <><Check className="h-3.5 w-3.5" /> Done Editing</> : <><Pencil className="h-3.5 w-3.5" /> Edit</>}
          </Button>
        </div>
      </div>

      {/* ==================== SEARCH & TOOLS ==================== */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search student, register no., dept, subject…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-9 text-sm"
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery("")} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-1.5">
          <Button
            variant={showRiskOnly ? "destructive" : "outline"}
            size="sm" className="gap-1 text-xs h-8"
            onClick={() => setShowRiskOnly(!showRiskOnly)}
          >
            {showRiskOnly ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
            {showRiskOnly ? "Show All" : "Risk Only"}
          </Button>

          <div className="flex items-center gap-0.5 border rounded-md px-1">
            <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setZoom(z => Math.max(60, z - 10))}>
              <ZoomOut className="h-3.5 w-3.5" />
            </Button>
            <span className="text-xs font-medium w-10 text-center">{zoom}%</span>
            <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setZoom(z => Math.min(150, z + 10))}>
              <ZoomIn className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        {/* ==================== MAIN GRID ==================== */}
        <div className="flex-1 min-w-0">
          {saving && (
            <div className="flex items-center gap-2 text-sm text-primary p-2 border border-primary/20 rounded-md bg-primary/5 mb-3">
              <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-primary/30 border-t-primary" />
              Saving changes…
            </div>
          )}

          {currentHall && grid && (
            <Card className="border-slate-200/70 shadow-sm overflow-hidden">
              {/* Hall Navigation */}
              <CardHeader className="py-3 px-4 bg-slate-50/80 border-b">
                <div className="flex items-center justify-between">
                  <Button variant="ghost" size="icon" disabled={currentHallIdx === 0} onClick={() => setCurrentHallIdx(i => i - 1)}>
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <div className="text-center">
                    <CardTitle className="text-sm">{currentHall.hallName}</CardTitle>
                    <p className="text-xs text-muted-foreground">
                      Hall {currentHallIdx + 1} of {halls.length} · {currentHall.seats.length} students
                    </p>
                  </div>
                  <Button variant="ghost" size="icon" disabled={currentHallIdx === halls.length - 1} onClick={() => setCurrentHallIdx(i => i + 1)}>
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </CardHeader>

              <CardContent className="p-3 overflow-x-auto">
                <div style={{ transform: `scale(${zoom / 100})`, transformOrigin: "top left", transition: "transform 0.2s ease" }}>
                  <table className="w-full text-xs border-collapse" style={{ minWidth: "550px" }}>
                    <thead>
                      <tr className="bg-slate-100/80">
                        <th className="border border-slate-200 p-2 w-12 text-center font-semibold text-slate-500 uppercase text-[10px] tracking-wider">Bench</th>
                        {currentCols.map(col => {
                          const depts = colDepts[col];
                          return (
                            <th key={col} className="border border-slate-200 p-2 text-center min-w-[110px]">
                              <div className="font-semibold text-slate-700">Col {col}</div>
                              {depts && depts.length > 0 && (
                                <div className="flex flex-wrap gap-0.5 justify-center mt-1">
                                  {depts.map(d => (
                                    <span key={d} className="text-[9px] font-semibold px-1.5 py-0.5 rounded-full"
                                      style={{ backgroundColor: getDeptColor(d), color: "#333" }}>{d}</span>
                                  ))}
                                </div>
                              )}
                            </th>
                          );
                        })}
                      </tr>
                    </thead>
                    <tbody>
                      {rowNumbers.map(row => (
                        <tr key={row} className="group">
                          <td className="border border-slate-200 p-2 text-center font-semibold text-slate-400 bg-slate-50/50">{row}</td>
                          {currentCols.map(col => {
                            const seat = grid.data[row]?.[col];
                            const cellKey = `${row}:${col}`;
                            const isDropZone = dropTarget === cellKey;
                            const isDragSrc = dragSource?.seatRow === row && dragSource?.seatCol === col && dragSource?.hallId === currentHall.hallId;
                            const isEditing = editingCell?.seatRow === row && editingCell?.seatCol === col && editingCell?.hallId === currentHall.hallId;
                            const isSearchMatch = searchQuery.trim() && seat && searchMatches.has(`${seat.hallId}:${seat.seatRow}:${seat.seatCol}`);
                            const hasRisk = seat && (seat.riskScore ?? 0) > 0;

                            // Filter: risk only
                            if (showRiskOnly && seat && !hasRisk) {
                              return (
                                <td key={col} className="border border-slate-200 p-2 text-center text-slate-300 bg-slate-50/30">
                                  <span className="text-[10px]">—</span>
                                </td>
                              );
                            }

                            // Empty cell
                            if (!seat) {
                              return (
                                <td key={col}
                                  className={`border border-slate-200 p-2 text-center transition-all duration-150
                                    ${editMode ? "cursor-pointer hover:bg-blue-50/50" : ""}
                                    ${isDropZone ? "bg-blue-100 border-blue-400 border-2 border-dashed ring-2 ring-blue-300/50" : "text-slate-300"}`}
                                  onDragOver={editMode ? (e) => handleDragOver(e, row, col) : undefined}
                                  onDragLeave={editMode ? handleDragLeave : undefined}
                                  onDrop={editMode ? (e) => handleDrop(e, row, col) : undefined}
                                >
                                  {isDropZone ? (
                                    <span className="text-blue-500 font-semibold text-[10px] animate-pulse">Drop here</span>
                                  ) : (
                                    <span className="text-[10px]">Empty</span>
                                  )}
                                </td>
                              );
                            }

                            // Occupied cell
                            return (
                              <td key={col}
                                className={`border border-slate-200 p-1.5 transition-all duration-150 relative
                                  ${editMode ? "cursor-grab active:cursor-grabbing" : ""}
                                  ${isDragSrc ? "opacity-30 ring-2 ring-primary scale-95" : ""}
                                  ${isDropZone ? "ring-2 ring-blue-500 bg-blue-50 scale-[1.02]" : ""}
                                  ${isSearchMatch ? "ring-2 ring-yellow-400 bg-yellow-50/60" : ""}
                                  ${hasRisk ? "ring-1 ring-red-300" : ""}`}
                                style={{ backgroundColor: isDragSrc ? undefined : getDeptColor(seat.department) + "55" }}
                                draggable={editMode && !isEditing}
                                onDragStart={editMode ? (e) => handleDragStart(e, seat) : undefined}
                                onDragEnd={editMode ? handleDragEnd : undefined}
                                onDragOver={editMode ? (e) => handleDragOver(e, row, col) : undefined}
                                onDragLeave={editMode ? handleDragLeave : undefined}
                                onDrop={editMode ? (e) => handleDrop(e, row, col) : undefined}
                                onDoubleClick={editMode ? () => startEditing(seat) : undefined}
                              >
                                {/* Risk indicator */}
                                {hasRisk && (
                                  <div className="absolute -top-1 -right-1 h-3.5 w-3.5 bg-red-500 rounded-full flex items-center justify-center shadow-sm" title={`Risk Score: ${seat.riskScore}`}>
                                    <AlertTriangle className="h-2 w-2 text-white" />
                                  </div>
                                )}

                                {/* Drag handle */}
                                {editMode && !isEditing && (
                                  <div className="flex items-center justify-center mb-0.5 opacity-40 group-hover:opacity-70 transition-opacity">
                                    <GripVertical className="h-3 w-3 text-slate-500" />
                                  </div>
                                )}

                                {/* Editing mode */}
                                {isEditing ? (
                                  <div className="flex items-center gap-0.5">
                                    <input
                                      ref={editInputRef}
                                      type="text"
                                      value={editingCell!.value}
                                      onChange={(e) => setEditingCell({ ...editingCell!, value: e.target.value })}
                                      onKeyDown={handleEditKeyDown}
                                      className="w-full text-[11px] font-mono font-medium border border-primary/40 rounded px-1 py-0.5 bg-white focus:outline-none focus:ring-1 focus:ring-primary"
                                      disabled={saving}
                                    />
                                    <button onClick={confirmEdit} disabled={saving} className="text-green-600 hover:text-green-700 p-0.5">
                                      <Check className="h-3.5 w-3.5" />
                                    </button>
                                    <button onClick={cancelEditing} className="text-red-500 hover:text-red-600 p-0.5">
                                      <X className="h-3.5 w-3.5" />
                                    </button>
                                  </div>
                                ) : (
                                  <>
                                    <div className="font-mono text-[11px] font-semibold text-slate-800 leading-tight">
                                      {seat.registerNumber}
                                    </div>
                                    <div className="text-[9px] text-slate-500 truncate max-w-[105px] leading-tight mt-0.5">
                                      {seat.subjectCode}
                                    </div>
                                    <div className="text-[8px] text-slate-400 truncate max-w-[105px] leading-tight">
                                      {seat.department}
                                    </div>
                                  </>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Department Legend */}
                <div className="flex flex-wrap gap-1.5 pt-3 mt-3 border-t border-slate-100">
                  <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-wider mr-1 self-center">Departments:</span>
                  {allDepts.map(dept => (
                    <span key={dept} className="text-[10px] px-2 py-0.5 rounded-full font-semibold shadow-sm"
                      style={{ backgroundColor: getDeptColor(dept), color: "#333" }}>
                      {dept}
                    </span>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {!currentHall && (
            <div className="flex items-center justify-center h-40 text-muted-foreground text-sm">
              No allocation data found for this batch.
            </div>
          )}
        </div>

        {/* ==================== SIDEBAR ==================== */}
        <div className="hidden lg:block w-64 shrink-0 space-y-3">
          {/* Hall Quick Nav */}
          <Card className="border-slate-200/70">
            <CardHeader className="py-2.5 px-3">
              <CardTitle className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5" /> All Halls
              </CardTitle>
            </CardHeader>
            <CardContent className="p-2 max-h-48 overflow-y-auto">
              <div className="space-y-0.5">
                {halls.map((h, idx) => (
                  <button key={h.hallId} onClick={() => setCurrentHallIdx(idx)}
                    className={`w-full text-left px-2.5 py-1.5 rounded-md text-xs transition-colors flex items-center justify-between
                      ${idx === currentHallIdx ? "bg-primary/10 text-primary font-semibold" : "hover:bg-slate-50 text-slate-600"}`}>
                    <span className="truncate">{h.hallName}</span>
                    <Badge variant="secondary" className="text-[9px] h-4 px-1.5 shrink-0">{h.seats.length}</Badge>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Edit History */}
          <Card className="border-slate-200/70">
            <CardHeader className="py-2.5 px-3">
              <CardTitle className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                <Undo2 className="h-3.5 w-3.5" /> Edit History
              </CardTitle>
            </CardHeader>
            <CardContent className="p-2 max-h-56 overflow-y-auto">
              {history.length === 0 ? (
                <p className="text-xs text-muted-foreground text-center py-3">No changes made yet</p>
              ) : (
                <div className="space-y-1">
                  {history.slice(0, 20).map((h, i) => (
                    <div key={i} className="text-[10px] px-2 py-1.5 rounded bg-slate-50 border border-slate-100 leading-tight">
                      <div className="flex items-center gap-1 mb-0.5">
                        {h.type === "swap" ? (
                          <ArrowLeftRight className="h-2.5 w-2.5 text-blue-500 shrink-0" />
                        ) : (
                          <Pencil className="h-2.5 w-2.5 text-amber-500 shrink-0" />
                        )}
                        <span className="font-medium text-slate-700 capitalize">{h.type}</span>
                      </div>
                      <div className="text-slate-500">{h.description}</div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Instructions */}
          {editMode && (
            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-3">
                <p className="text-[10px] font-semibold text-primary uppercase tracking-wider mb-2">How to Use</p>
                <ul className="text-[10px] text-slate-600 space-y-1.5 leading-relaxed">
                  <li className="flex items-start gap-1.5">
                    <GripVertical className="h-3 w-3 text-primary shrink-0 mt-0.5" />
                    <span><strong>Drag & Drop</strong> a student to swap with another student or move to an empty seat.</span>
                  </li>
                  <li className="flex items-start gap-1.5">
                    <Pencil className="h-3 w-3 text-primary shrink-0 mt-0.5" />
                    <span><strong>Double-click</strong> a cell to edit the register number inline.</span>
                  </li>
                  <li className="flex items-start gap-1.5">
                    <Search className="h-3 w-3 text-primary shrink-0 mt-0.5" />
                    <span><strong>Search</strong> by name, register no., department, or subject code.</span>
                  </li>
                  <li className="flex items-start gap-1.5">
                    <AlertTriangle className="h-3 w-3 text-red-500 shrink-0 mt-0.5" />
                    <span>Red dots indicate <strong>adjacency risk</strong>. Move those students to fix violations.</span>
                  </li>
                </ul>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
