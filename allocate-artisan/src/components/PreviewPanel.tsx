import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { getBatchPreview, swapSeats, updateRegisterNumber, type PreviewSeat } from "@/api/allocationApi";
import { Eye, ChevronLeft, ChevronRight, Users, Building2, AlertTriangle, GripVertical, Pencil, Check, X, ArrowLeftRight, LayoutGrid } from "lucide-react";
import { toast } from "sonner";

interface PreviewPanelProps {
  batchId: string | null;
  status: string;
  onUpdate?: () => void;
}

const ALL_COLS = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII"];
const DEPT_COLORS: Record<string, string> = {};
const PALETTE = [
  "hsl(220, 70%, 92%)", "hsl(150, 60%, 90%)", "hsl(35, 80%, 90%)",
  "hsl(280, 50%, 92%)", "hsl(0, 60%, 92%)", "hsl(190, 60%, 90%)",
  "hsl(60, 60%, 90%)", "hsl(320, 50%, 92%)", "hsl(100, 50%, 90%)",
  "hsl(250, 50%, 92%)", "hsl(170, 50%, 90%)", "hsl(10, 70%, 92%)"
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

export function PreviewPanel({ batchId, status, onUpdate }: PreviewPanelProps) {
  const [seats, setSeats] = useState<PreviewSeat[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentHallIdx, setCurrentHallIdx] = useState(0);
  const [isOpen, setIsOpen] = useState(false);

  // Visual Override state
  const [editMode, setEditMode] = useState(false);
  const [dragSource, setDragSource] = useState<DragData | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null); // "row:col" key
  const [editingCell, setEditingCell] = useState<EditingCell | null>(null);
  const [saving, setSaving] = useState(false);
  const editInputRef = useRef<HTMLInputElement>(null);
  const nav = useNavigate();

  useEffect(() => {
    if (!batchId || status !== "ACTIVE") return;
    setLoading(true);
    setError(null);
    getBatchPreview(batchId)
      .then((data) => {
        setSeats(data);
        setCurrentHallIdx(0);
        setIsOpen(true);
      })
      .catch((e) => setError(e?.message || "Failed to load preview"))
      .finally(() => setLoading(false));
  }, [batchId, status]);

  // Group seats by hall — always called
  const halls = useMemo(() => {
    const map = new Map<string, { hallId: string; hallName: string; seats: PreviewSeat[] }>();
    for (const s of seats) {
      if (!map.has(s.hallId)) {
        map.set(s.hallId, { hallId: s.hallId, hallName: s.hallName || s.hallId, seats: [] });
      }
      map.get(s.hallId)!.seats.push(s);
    }
    // Sort halls by name ascending
    return Array.from(map.values()).sort((a, b) => a.hallName.localeCompare(b.hallName, undefined, { numeric: true }));
  }, [seats]);

  const currentHall = halls[currentHallIdx] ?? null;

  // Build grid for current hall — always called
  const grid = useMemo(() => {
    if (!currentHall) return null;
    const g: Record<string, Record<string, PreviewSeat>> = {};
    for (const s of currentHall.seats) {
      if (!g[s.seatRow]) g[s.seatRow] = {};
      g[s.seatRow][s.seatCol] = s;
    }
    return g;
  }, [currentHall]);

  // Determine dynamic columns and max row
  const { currentCols, rowNumbers } = useMemo(() => {
    if (!currentHall || currentHall.seats.length === 0) {
      return { currentCols: ["I", "II", "III", "IV", "V"], rowNumbers: [1, 2, 3, 4, 5] };
    }
    let maxR = 0;
    const colsSet = new Set<string>();
    for (const s of currentHall.seats) {
      if (s.seatRow > maxR) maxR = s.seatRow;
      colsSet.add(s.seatCol);
    }
    const mR = Math.max(5, maxR);
    const cCols = ALL_COLS.filter(c => colsSet.has(c));
    return {
      currentCols: cCols.length > 0 ? cCols : ["I", "II", "III", "IV", "V"],
      rowNumbers: Array.from({ length: mR }, (_, i) => i + 1)
    };
  }, [currentHall]);

  // Stats per column — always called
  const colDepts = useMemo(() => {
    if (!currentHall) return {};
    const map: Record<string, Set<string>> = {};
    for (const s of currentHall.seats) {
      if (!map[s.seatCol]) map[s.seatCol] = new Set();
      map[s.seatCol].add(s.department);
    }
    return Object.fromEntries(Object.entries(map).map(([k, v]) => [k, Array.from(v)]));
  }, [currentHall]);

  // ==================== DRAG & DROP HANDLERS ====================
  const handleDragStart = useCallback((e: React.DragEvent, seat: PreviewSeat) => {
    if (!editMode) return;
    const dragData: DragData = {
      hallId: seat.hallId,
      seatRow: seat.seatRow,
      seatCol: seat.seatCol,
      registerNumber: seat.registerNumber,
    };
    setDragSource(dragData);
    e.dataTransfer.setData("text/plain", JSON.stringify(dragData));
    e.dataTransfer.effectAllowed = "move";
  }, [editMode]);

  const handleDragOver = useCallback((e: React.DragEvent, row: number, col: string) => {
    if (!editMode) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDropTarget(`${row}:${col}`);
  }, [editMode]);

  const handleDragLeave = useCallback(() => {
    setDropTarget(null);
  }, []);

  const handleDrop = useCallback(async (e: React.DragEvent, targetRow: number, targetCol: string) => {
    e.preventDefault();
    setDropTarget(null);

    if (!batchId || !dragSource || !currentHall) return;

    // Don't swap with yourself
    if (dragSource.hallId === currentHall.hallId && dragSource.seatRow === targetRow && dragSource.seatCol === targetCol) {
      setDragSource(null);
      return;
    }

    setSaving(true);
    try {
      const refreshed = await swapSeats(batchId, {
        hallIdA: dragSource.hallId,
        seatRowA: dragSource.seatRow,
        seatColA: dragSource.seatCol,
        hallIdB: currentHall.hallId,
        seatRowB: targetRow,
        seatColB: targetCol,
      });
      setSeats(refreshed);
      if (onUpdate) onUpdate();

      const targetSeat = grid?.[targetRow]?.[targetCol];
      if (targetSeat) {
        toast.success("Seats Swapped", {
          description: `${dragSource.registerNumber} ↔ ${targetSeat.registerNumber}`,
        });
      } else {
        toast.success("Student Moved", {
          description: `${dragSource.registerNumber} → R${targetRow}:${targetCol}`,
        });
      }
    } catch (err: any) {
      toast.error("Swap Failed", { description: err?.message || "Could not swap seats." });
    } finally {
      setSaving(false);
      setDragSource(null);
    }
  }, [batchId, dragSource, currentHall, grid, onUpdate]);

  const handleDragEnd = useCallback(() => {
    setDragSource(null);
    setDropTarget(null);
  }, []);

  // ==================== INLINE EDIT HANDLERS ====================
  const startEditing = useCallback((seat: PreviewSeat) => {
    if (!editMode) return;
    setEditingCell({
      hallId: seat.hallId,
      seatRow: seat.seatRow,
      seatCol: seat.seatCol,
      value: seat.registerNumber,
    });
    // Focus input on next tick
    setTimeout(() => editInputRef.current?.focus(), 50);
  }, [editMode]);

  const cancelEditing = useCallback(() => {
    setEditingCell(null);
  }, []);

  const confirmEdit = useCallback(async () => {
    if (!batchId || !editingCell) return;
    const original = grid?.[editingCell.seatRow]?.[editingCell.seatCol];
    if (!original || original.registerNumber === editingCell.value.trim()) {
      setEditingCell(null);
      return;
    }

    setSaving(true);
    try {
      const refreshed = await updateRegisterNumber(batchId, {
        hallId: editingCell.hallId,
        seatRow: editingCell.seatRow,
        seatCol: editingCell.seatCol,
        newRegisterNumber: editingCell.value.trim(),
      });
      setSeats(refreshed);
      if (onUpdate) onUpdate();
      toast.success("Register Number Updated", {
        description: `${original.registerNumber} → ${editingCell.value.trim()}`,
      });
    } catch (err: any) {
      toast.error("Update Failed", { description: err?.message || "Could not update register number." });
    } finally {
      setSaving(false);
      setEditingCell(null);
    }
  }, [batchId, editingCell, grid, onUpdate]);

  const handleEditKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter") confirmEdit();
    if (e.key === "Escape") cancelEditing();
  }, [confirmEdit, cancelEditing]);

  // All hooks above — now safe to early return
  if (status !== "ACTIVE" || !batchId) return null;

  const totalStudents = seats.length;
  const totalHalls = halls.length;

  return (
    <Card className="mt-4 border-blue-200/50 bg-gradient-to-b from-blue-50/30 to-white dark:from-blue-950/20 dark:to-background">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Eye className="h-5 w-5 text-blue-600" />
            <CardTitle className="text-base">Allocation Preview</CardTitle>
            {editMode && (
              <span className="ml-2 inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-amber-700 animate-pulse">
                <Pencil className="h-3 w-3" /> Edit Mode
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant={editMode ? "default" : "outline"}
              size="sm"
              onClick={() => { setEditMode(!editMode); setEditingCell(null); }}
              className={`gap-1.5 text-xs h-8 ${editMode ? "bg-amber-500 hover:bg-amber-600 text-white" : ""}`}
            >
              {editMode ? <><Check className="h-3.5 w-3.5" /> Done Editing</> : <><Pencil className="h-3.5 w-3.5" /> Edit</>}
            </Button>
            <Button variant="outline" size="sm" className="gap-1.5 text-xs h-8" onClick={() => window.open(`/dashboard/floor-plan/${batchId}`, '_blank')}>
              <LayoutGrid className="h-3.5 w-3.5" /> Floor Plan
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setIsOpen(!isOpen)}>
              {isOpen ? "Collapse" : "Expand"}
            </Button>
          </div>
        </div>
        {/* Summary bar */}
        <div className="flex items-center gap-4 text-sm text-muted-foreground mt-1">
          <span className="flex items-center gap-1">
            <Users className="h-3.5 w-3.5" /> {totalStudents} students
          </span>
          <span className="flex items-center gap-1">
            <Building2 className="h-3.5 w-3.5" /> {totalHalls} halls
          </span>
          {editMode && (
            <span className="flex items-center gap-1 text-amber-600 text-xs font-medium">
              <ArrowLeftRight className="h-3.5 w-3.5" /> Drag seats to swap • Double-click to edit register no.
            </span>
          )}
        </div>
      </CardHeader>

      {isOpen && (
        <CardContent className="space-y-3">
          {loading && (
            <div className="flex items-center justify-center py-8 text-muted-foreground gap-2">
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-muted-foreground" />
              Loading preview…
            </div>
          )}

          {error && (
            <div className="flex items-center gap-2 text-sm text-destructive p-3 border border-destructive/20 rounded-md bg-destructive/5">
              <AlertTriangle className="h-4 w-4" /> {error}
            </div>
          )}

          {saving && (
            <div className="flex items-center gap-2 text-sm text-amber-700 p-2 border border-amber-200 rounded-md bg-amber-50">
              <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-amber-400/30 border-t-amber-600" />
              Saving changes…
            </div>
          )}

          {!loading && !error && currentHall && grid && (
            <>
              {/* Hall navigation */}
              <div className="flex items-center justify-between bg-slate-100 dark:bg-slate-800 rounded-lg p-2">
                <Button
                  variant="ghost" size="icon"
                  disabled={currentHallIdx === 0}
                  onClick={() => setCurrentHallIdx(i => i - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="text-center">
                  <div className="font-semibold text-sm">{currentHall.hallName}</div>
                  <div className="text-xs text-muted-foreground">
                    Hall {currentHallIdx + 1} of {halls.length} • {currentHall.seats.length} students
                  </div>
                </div>
                <Button
                  variant="ghost" size="icon"
                  disabled={currentHallIdx === halls.length - 1}
                  onClick={() => setCurrentHallIdx(i => i + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>

              {/* Seat Grid Table */}
              <div className="overflow-x-auto rounded-lg border">
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 dark:bg-slate-900">
                      <th className="border p-1.5 w-8 text-center font-medium text-muted-foreground">S.No</th>
                      {currentCols.map(col => {
                        const depts = colDepts[col];
                        const isEmpty = !depts || depts.length === 0;
                        return (
                          <th key={col} className="border p-1.5 text-center min-w-[100px]">
                            <div className="font-semibold">{col} Row</div>
                            {!isEmpty && (
                              <div
                                className="text-[10px] font-medium mt-0.5 px-1.5 py-0.5 rounded-full inline-block"
                                style={{ backgroundColor: getDeptColor(depts[0]), color: "#333" }}
                              >
                                {depts.join("/")}
                              </div>
                            )}
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  <tbody>
                    {rowNumbers.map(row => (
                      <tr key={row} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/50">
                        <td className="border p-1.5 text-center font-medium text-muted-foreground">{row}</td>
                        {currentCols.map(col => {
                          const seat = grid[row]?.[col];
                          const isDropZone = dropTarget === `${row}:${col}`;
                          const isDragSource = dragSource?.seatRow === row && dragSource?.seatCol === col && dragSource?.hallId === currentHall.hallId;
                          const isEditing = editingCell?.seatRow === row && editingCell?.seatCol === col && editingCell?.hallId === currentHall.hallId;

                          if (!seat) {
                            return (
                              <td
                                key={col}
                                className={`border p-1.5 text-center text-muted-foreground/30 transition-colors ${
                                  editMode ? "cursor-pointer" : ""
                                } ${isDropZone ? "bg-blue-100 border-blue-400 border-2 border-dashed" : ""}`}
                                onDragOver={editMode ? (e) => handleDragOver(e, row, col) : undefined}
                                onDragLeave={editMode ? handleDragLeave : undefined}
                                onDrop={editMode ? (e) => handleDrop(e, row, col) : undefined}
                              >
                                {isDropZone ? (
                                  <span className="text-blue-500 font-medium text-[10px]">Drop here</span>
                                ) : "—"}
                              </td>
                            );
                          }

                          return (
                            <td
                              key={col}
                              className={`border p-1.5 transition-all ${
                                editMode ? "cursor-grab active:cursor-grabbing" : ""
                              } ${isDragSource ? "opacity-40 ring-2 ring-amber-400" : ""
                              } ${isDropZone ? "ring-2 ring-blue-500 bg-blue-50" : ""}`}
                              style={{ backgroundColor: isDragSource ? undefined : getDeptColor(seat.department) + "66" }}
                              draggable={editMode && !isEditing}
                              onDragStart={editMode ? (e) => handleDragStart(e, seat) : undefined}
                              onDragEnd={editMode ? handleDragEnd : undefined}
                              onDragOver={editMode ? (e) => handleDragOver(e, row, col) : undefined}
                              onDragLeave={editMode ? handleDragLeave : undefined}
                              onDrop={editMode ? (e) => handleDrop(e, row, col) : undefined}
                              onDoubleClick={editMode ? () => startEditing(seat) : undefined}
                            >
                              {editMode && !isEditing && (
                                <div className="flex items-center justify-center mb-0.5">
                                  <GripVertical className="h-3 w-3 text-slate-400" />
                                </div>
                              )}
                              {isEditing ? (
                                <div className="flex items-center gap-1">
                                  <input
                                    ref={editInputRef}
                                    type="text"
                                    value={editingCell.value}
                                    onChange={(e) => setEditingCell({ ...editingCell, value: e.target.value })}
                                    onKeyDown={handleEditKeyDown}
                                    className="w-full text-[11px] font-mono font-medium border border-blue-300 rounded px-1 py-0.5 bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
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
                                  <div className="font-mono text-[11px] font-medium">{seat.registerNumber}</div>
                                  <div className="text-[9px] text-muted-foreground truncate max-w-[110px]">{seat.subjectCode}</div>
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
              <div className="flex flex-wrap gap-1.5 pt-1">
                {Object.entries(colDepts).flatMap(([, d]) => d).filter((v, i, a) => a.indexOf(v) === i).map(dept => (
                  <span
                    key={dept}
                    className="text-[10px] px-2 py-0.5 rounded-full font-medium"
                    style={{ backgroundColor: getDeptColor(dept), color: "#333" }}
                  >
                    {dept}
                  </span>
                ))}
              </div>
            </>
          )}
        </CardContent>
      )}
    </Card>
  );
}
