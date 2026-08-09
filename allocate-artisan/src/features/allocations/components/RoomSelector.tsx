import { useRef, useEffect } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import {
  CheckCircle2,
  Building2,
  Search,
  XCircle,
  DoorOpen,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useRoomSelection } from "@/hooks/useRoomSelection";

import type { FloorGroup } from "@/hooks/useRoomSelection";

interface RoomSelectorProps {
  /** Disables the entire selector */
  disabled?: boolean;
  /** Called when selection changes */
  onSelectionChange?: (roomIds: string[]) => void;
  /** Controlled selected rooms (optional) */
  value?: string[];
  /** Total students requiring seating (optional) */
  totalStudents?: number | null;
  /** Whether this selector is for internal exam (40 capacity, 7x6 grid) */
  isInternal?: boolean;
}

// ─── Floor Label Helpers ────────────────────────────────────────────────────

function floorLabel(floor: number): string {
  if (floor === 0) return "Conference Halls";
  const suffixes: Record<number, string> = { 1: "st", 2: "nd", 3: "rd" };
  const suffix = suffixes[floor] || "th";
  return `${floor}${suffix} Floor`;
}

function blockDisplayName(block: string): string {
  if (block === "LH") return "LH Block (Lecture Hall)";
  if (block === "SH") return "SH Block (Seminar Hall)";
  if (block === "CONF") return "CONF Block (Conference Hall)";
  return block;
}

function blockIcon(block: string) {
  return Building2;
}

// ─── Sub-Components ─────────────────────────────────────────────────────────

function RoomChip({
  id,
  name,
  selected,
  visible,
  onToggle,
  disabled,
}: {
  id: string;
  name: string;
  selected: boolean;
  visible: boolean;
  onToggle: (id: string) => void;
  disabled?: boolean;
}) {
  if (!visible) return null;

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onToggle(id)}
      className={cn(
        "group relative flex items-center gap-1.5 rounded-lg border px-3 py-2 text-sm font-medium transition-all duration-200",
        "hover:shadow-md hover:-translate-y-0.5 active:translate-y-0",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-1",
        selected
          ? "border-emerald-400 bg-emerald-50 text-emerald-800 shadow-sm shadow-emerald-100"
          : "border-gray-200 bg-white text-gray-600 hover:border-emerald-300 hover:bg-emerald-50/50",
        disabled && "pointer-events-none opacity-40"
      )}
      id={`room-chip-${id}`}
    >
      <DoorOpen
        className={cn(
          "h-3.5 w-3.5 transition-colors",
          selected ? "text-emerald-600" : "text-gray-400 group-hover:text-emerald-500"
        )}
      />
      <span>{name}</span>
      {selected && (
        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 animate-in zoom-in-50 duration-200" />
      )}
    </button>
  );
}

function FloorSection({
  block,
  floorGroup,
  selectedIds,
  filteredRooms,
  isFloorSelected,
  isFloorIndeterminate,
  toggleFloor,
  toggleRoom,
  disabled,
}: {
  block: string;
  floorGroup: FloorGroup;
  selectedIds: Set<string>;
  filteredRooms: Set<string>;
  isFloorSelected: boolean;
  isFloorIndeterminate: boolean;
  toggleFloor: () => void;
  toggleRoom: (id: string) => void;
  disabled?: boolean;
}) {
  const visibleRooms = floorGroup.rooms.filter((r) => filteredRooms.has(r.id));
  if (visibleRooms.length === 0) return null;

  const selectedCountInFloor = floorGroup.rooms.filter((r) => selectedIds.has(r.id)).length;

  return (
    <div className="space-y-2.5">
      {/* Floor Header */}
      <div className="flex items-center justify-between sticky top-0 z-10 bg-gray-50/95 backdrop-blur-sm rounded-lg px-3 py-2 border border-gray-100">
        <label className="flex items-center gap-2.5 cursor-pointer select-none" htmlFor={`floor-${block}-${floorGroup.floor}`}>
          <Checkbox
            id={`floor-${block}-${floorGroup.floor}`}
            checked={isFloorSelected ? true : isFloorIndeterminate ? "indeterminate" : false}
            onCheckedChange={toggleFloor}
            disabled={disabled}
            className={cn(
              "transition-all duration-150",
              isFloorSelected && "border-emerald-500 bg-emerald-500 text-white data-[state=checked]:bg-emerald-500 data-[state=checked]:border-emerald-500"
            )}
          />
          <span className="text-sm font-semibold text-gray-700">
            {floorLabel(floorGroup.floor)}
          </span>
        </label>
        <Badge
          variant="secondary"
          className={cn(
            "text-xs font-medium transition-colors",
            selectedCountInFloor > 0
              ? "bg-emerald-100 text-emerald-700"
              : "bg-gray-100 text-gray-500"
          )}
        >
          {selectedCountInFloor}/{floorGroup.rooms.length}
        </Badge>
      </div>

      {/* Room Grid */}
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-2 pl-2">
        {floorGroup.rooms.map((room: any) => (
          <RoomChip
            key={room.id}
            id={room.id}
            name={room.name}
            selected={selectedIds.has(room.id)}
            visible={filteredRooms.has(room.id)}
            onToggle={toggleRoom}
            disabled={disabled}
          />
        ))}
      </div>
    </div>
  );
}

// ─── Main Component ─────────────────────────────────────────────────────────

export function RoomSelector({ disabled = false, onSelectionChange, totalStudents, isInternal = false }: RoomSelectorProps) {
  const rs = useRoomSelection();
  const prevSelectionRef = useRef<string>("");

  // Notify parent of selection changes
  useEffect(() => {
    const key = rs.selectedRoomIds.sort().join(",");
    if (key !== prevSelectionRef.current) {
      prevSelectionRef.current = key;
      onSelectionChange?.(rs.selectedRoomIds);
    }
  }, [rs.selectedRoomIds, onSelectionChange]);

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden glass-panel", disabled && "opacity-50 grayscale-[0.2]")}>
      {/* ── Header ────────────────────────────────────────────────── */}
      <CardHeader className="bg-slate-50/50 border-b border-slate-100/50 pb-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-600 flex items-center justify-center shrink-0">
              <DoorOpen className="w-5 h-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-black tracking-tight text-slate-900 flex items-center gap-2">
                3. Physical Environment Selection
                {rs.isLoading && <span className="ml-2 text-xs font-normal text-emerald-600 flex items-center gap-1 animate-pulse">Syncing<span className="flex gap-[2px]"><span className="w-1 h-1 rounded-full bg-emerald-600 inline-block animate-bounce" style={{animationDelay:"0s"}}></span><span className="w-1 h-1 rounded-full bg-emerald-600 inline-block animate-bounce" style={{animationDelay:"0.2s"}}></span><span className="w-1 h-1 rounded-full bg-emerald-600 inline-block animate-bounce" style={{animationDelay:"0.4s"}}></span></span></span>}
              </CardTitle>
              <CardDescription className="text-xs font-semibold uppercase tracking-wider mt-1 text-slate-500">
                Allocate halls for candidate seating
              </CardDescription>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Badge
              variant="outline"
              className={cn(
                "text-[11px] font-bold uppercase tracking-widest px-3 py-1.5 transition-all duration-300 rounded-lg",
                rs.selectedCount > 0
                  ? "border-emerald-500/30 bg-emerald-50 text-emerald-700 shadow-sm"
                  : "border-slate-200 text-slate-500 bg-white"
              )}
            >
              {rs.selectedCount} room{rs.selectedCount !== 1 ? "s" : ""} selected
            </Badge>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4 pt-0">
        {/* ── Global Controls ─────────────────────────────────────── */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 pb-3 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer select-none" htmlFor="select-all-rooms">
              <Checkbox
                id="select-all-rooms"
                checked={rs.isAllSelected ? true : rs.isAllIndeterminate ? "indeterminate" : false}
                onCheckedChange={() => (rs.isAllSelected ? rs.clearAll() : rs.selectAll())}
                disabled={disabled}
                className={cn(
                  rs.isAllSelected && "border-emerald-500 bg-emerald-500 text-white data-[state=checked]:bg-emerald-500 data-[state=checked]:border-emerald-500"
                )}
              />
              <span className="text-sm font-medium text-gray-700">Select All</span>
            </label>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={rs.clearAll}
              disabled={disabled || rs.selectedCount === 0}
              className="text-xs text-gray-500 hover:text-red-500 hover:bg-red-50 gap-1.5 h-8"
            >
              <XCircle className="h-3.5 w-3.5" />
              Clear All
            </Button>
          </div>

          {/* Search */}
          <div className="relative flex-1 w-full sm:max-w-xs sm:ml-auto">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search rooms..."
              value={rs.searchQuery}
              onChange={(e) => rs.setSearchQuery(e.target.value)}
              disabled={disabled}
              className="pl-9 h-8 text-sm bg-gray-50 border-gray-200 focus:border-emerald-400 focus:ring-emerald-400"
              id="room-search-input"
            />
            {rs.searchQuery && (
              <button
                type="button"
                onClick={() => rs.setSearchQuery("")}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                <XCircle className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Professional Capacity Analysis Summary */}
        {totalStudents && totalStudents > 0 && (() => {
          const allHalls = rs.blockGroups.flatMap((bg) => bg.allRooms);
          const perRoomCapacity = isInternal ? 40 : 25;
          
          const selectedCapacity = allHalls
            .filter((h) => rs.selectedIds.has(h.id))
            .reduce((sum, h) => sum + (isInternal ? (h.internalCapacity || 40) : (h.capacity || 25)), 0);

          const requiredHalls = Math.ceil(totalStudents / perRoomCapacity);
          const underCapacity = selectedCapacity < totalStudents;
          const remainingHalls = Math.max(0, requiredHalls - rs.selectedCount);

          return (
            <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm mb-4">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 divide-y md:divide-y-0 md:divide-x divide-slate-100">
                
                {/* Metric 1: Parsed Students */}
                <div className="px-2">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                    Parsed Students
                  </span>
                  <span className="text-xl font-bold text-slate-900 mt-0.5 block">
                    {totalStudents}
                  </span>
                  <span className="text-[11px] text-slate-400">From uploaded roster</span>
                </div>

                {/* Metric 2: Required Halls */}
                <div className="pt-2 md:pt-0 md:px-4">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                    Required Halls
                  </span>
                  <span className="text-xl font-bold text-slate-900 mt-0.5 block">
                    {requiredHalls} <span className="text-xs font-normal text-slate-500">Halls</span>
                  </span>
                  <span className="text-[11px] text-slate-400">{perRoomCapacity} seats/hall ({isInternal ? "7×6 Grid" : "5×5 Grid"})</span>
                </div>

                {/* Metric 3: Selected Capacity */}
                <div className="pt-2 md:pt-0 md:px-4">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                    Selected Capacity
                  </span>
                  <span className="text-xl font-bold text-slate-900 mt-0.5 block">
                    {selectedCapacity} / {totalStudents} <span className="text-xs font-normal text-slate-500">seats</span>
                  </span>
                  <span className="text-[11px] text-slate-400">{rs.selectedCount} / {requiredHalls} halls selected</span>
                </div>

                {/* Metric 4: Capacity Status */}
                <div className="pt-2 md:pt-0 md:px-4 flex flex-col justify-between">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                    Capacity Status
                  </span>
                  <div className="mt-1">
                    {underCapacity ? (
                      <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">
                        Need {remainingHalls} more hall{remainingHalls !== 1 ? 's' : ''} ({totalStudents - selectedCapacity} seats short)
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-semibold">
                        Capacity Satisfied
                      </span>
                    )}
                  </div>
                </div>

              </div>
            </div>
          );
        })()}

        {/* ── Block Sections (Scrollable) ─────────────────────────── */}
        <ScrollArea className="h-[420px] pr-3">
          <div className="space-y-6">
            {rs.blockGroups.map((bg) => {
              const Icon = blockIcon(bg.block);
              const hasVisibleRooms = bg.allRooms.some((r) => rs.filteredRooms.has(r.id));
              if (!hasVisibleRooms) return null;

              const selectedInBlock = bg.allRooms.filter((r) => rs.selectedIds.has(r.id)).length;

              return (
                <div
                  key={bg.block}
                  className="rounded-xl border border-gray-200 bg-white overflow-hidden"
                >
                  {/* Block Header */}
                  <div className="sticky top-0 z-20 flex items-center justify-between bg-gradient-to-r from-gray-50 to-white px-4 py-3 border-b border-gray-100">
                    <label
                      className="flex items-center gap-3 cursor-pointer select-none"
                      htmlFor={`block-${bg.block}`}
                    >
                      <Checkbox
                        id={`block-${bg.block}`}
                        checked={
                          rs.isBlockSelected(bg.block)
                            ? true
                            : rs.isBlockIndeterminate(bg.block)
                            ? "indeterminate"
                            : false
                        }
                        onCheckedChange={() => rs.toggleBlock(bg.block)}
                        disabled={disabled}
                        className={cn(
                          "transition-all duration-150",
                          rs.isBlockSelected(bg.block) &&
                            "border-emerald-500 bg-emerald-500 text-white data-[state=checked]:bg-emerald-500 data-[state=checked]:border-emerald-500"
                        )}
                      />
                      <Icon className="h-4.5 w-4.5 text-emerald-600" />
                      <span className="text-sm font-bold text-gray-800">
                        {blockDisplayName(bg.block)}
                      </span>
                    </label>
                    <Badge
                      className={cn(
                        "text-xs font-semibold transition-colors",
                        selectedInBlock > 0
                          ? "bg-emerald-500 text-white hover:bg-emerald-600"
                          : "bg-gray-200 text-gray-600"
                      )}
                    >
                      {selectedInBlock}/{bg.allRooms.length}
                    </Badge>
                  </div>

                  {/* Floors within block */}
                  <div className="p-3 space-y-4 bg-gray-50/30">
                    {bg.floors.map((fg) => (
                      <FloorSection
                        key={`${bg.block}-${fg.floor}`}
                        block={bg.block}
                        floorGroup={fg}
                        selectedIds={rs.selectedIds}
                        filteredRooms={rs.filteredRooms}
                        isFloorSelected={rs.isFloorSelected(bg.block, fg.floor)}
                        isFloorIndeterminate={rs.isFloorIndeterminate(bg.block, fg.floor)}
                        toggleFloor={() => rs.toggleFloor(bg.block, fg.floor)}
                        toggleRoom={rs.toggleRoom}
                        disabled={disabled}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
