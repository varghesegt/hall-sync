import React, { useState, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getBatches } from "@/api/allocationApi";
import { Loader2 } from "lucide-react";

interface BatchSelectorProps {
  onBatchSelect: (batchId: string) => void;
  selectedBatchId?: string | null;
  className?: string;
  statusFilter?: string[]; // e.g. ["ACTIVE", "COMPLETED"]
}

export function BatchSelector({ onBatchSelect, selectedBatchId, className, statusFilter }: BatchSelectorProps) {
  const [selectedSeason, setSelectedSeason] = useState<string>("");

  const { data: batches = [], isLoading } = useQuery({
    queryKey: ["batches"],
    queryFn: async () => {
      const response = await getBatches();
      return response as any[];
    },
  });

  const filteredByStatus = useMemo(() => {
    if (!statusFilter || statusFilter.length === 0) return batches;
    return batches.filter((b) => statusFilter.includes(b.status));
  }, [batches, statusFilter]);

  const uniqueSeasons = useMemo(() => {
    const map = new Set<string>();
    filteredByStatus.forEach(b => {
      if (b.examSession && b.examSession.seasonId) {
        map.add(b.examSession.seasonId);
      }
    });
    return Array.from(map);
  }, [filteredByStatus]);

  const availableBatches = useMemo(() => {
    if (!selectedSeason) return [];
    return filteredByStatus.filter((b) => b.examSession?.seasonId === selectedSeason);
  }, [filteredByStatus, selectedSeason]);

  // Handle external reset or initialization
  useEffect(() => {
    if (selectedBatchId && batches.length > 0) {
      const batch = batches.find((b: any) => b.id === selectedBatchId);
      if (batch && batch.examSession && batch.examSession.seasonId !== selectedSeason) {
        setSelectedSeason(batch.examSession.seasonId);
      }
    }
  }, [selectedBatchId, batches, selectedSeason]);

  if (isLoading) {
    return <div className="flex items-center text-slate-500 text-sm"><Loader2 className="w-4 h-4 mr-2 animate-spin"/> Loading...</div>;
  }

  return (
    <div className={`flex flex-col sm:flex-row gap-3 w-full max-w-2xl ${className || ""}`}>
      <div className="flex-1">
        <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">1. Select Exam Season</label>
        <select 
          className="w-full rounded-lg border border-slate-300 py-2.5 pl-3 pr-10 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 sm:text-sm bg-white"
          value={selectedSeason}
          onChange={(e) => {
            setSelectedSeason(e.target.value);
            onBatchSelect(""); // reset batch when season changes
          }}
        >
          <option value="">-- Choose Season --</option>
          {uniqueSeasons.map((season) => (
            <option key={season} value={season}>
              {season}
            </option>
          ))}
        </select>
      </div>

      <div className="flex-1">
        <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">2. Select Batch</label>
        <select
          className="w-full rounded-lg border border-slate-300 py-2.5 pl-3 pr-10 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 sm:text-sm bg-white disabled:bg-slate-50 disabled:text-slate-400 disabled:border-slate-200"
          value={selectedBatchId || ""}
          onChange={(e) => onBatchSelect(e.target.value)}
          disabled={!selectedSeason || availableBatches.length === 0}
        >
          <option value="">{selectedSeason ? (availableBatches.length > 0 ? "-- Choose Batch --" : "No Batches Available") : "Select Season First"}</option>
          {availableBatches.map((b: any) => (
            <option key={b.id} value={b.id}>
              {b.examSession?.date} - {b.examSession?.session} {b.status === "ACTIVE" ? "(Active)" : "(Completed)"}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
