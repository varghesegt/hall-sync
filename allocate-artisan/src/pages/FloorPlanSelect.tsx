import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BatchSelector } from "@/components/BatchSelector";
import { LayoutGrid, ArrowRight, Info } from "lucide-react";

export default function FloorPlanSelect() {
  const [selectedBatchId, setSelectedBatchId] = useState("");
  const navigate = useNavigate();

  return (
    <div className="w-full max-w-3xl mx-auto py-8 px-4 animate-in fade-in duration-500">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-primary/10">
            <LayoutGrid className="h-6 w-6 text-primary" />
          </div>
          Visual Floor Plan Editor
        </h1>
        <p className="text-sm text-slate-500 mt-2 ml-[52px]">
          Select an allocation batch to open the interactive drag-and-drop floor plan editor.
        </p>
      </div>

      <Card className="border-slate-200/70 shadow-sm">
        <CardHeader className="border-b bg-slate-50/50">
          <CardTitle className="text-base">Select Batch</CardTitle>
          <CardDescription>
            Choose the exam season and batch you want to visually edit.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-6 space-y-6">
          <BatchSelector
            selectedBatchId={selectedBatchId}
            onBatchSelect={setSelectedBatchId}
            statusFilter={["ACTIVE"]}
          />

          <Button
            disabled={!selectedBatchId}
            onClick={() => navigate(`/dashboard/floor-plan/${selectedBatchId}`)}
            className="w-full gap-2 h-11"
          >
            <LayoutGrid className="h-4 w-4" />
            Open Floor Plan Editor
            <ArrowRight className="h-4 w-4 ml-auto" />
          </Button>

          <div className="flex items-start gap-2.5 p-3 rounded-lg bg-blue-50/70 border border-blue-100 text-xs text-blue-700 leading-relaxed">
            <Info className="h-4 w-4 shrink-0 mt-0.5" />
            <div>
              <strong>How it works:</strong> The Floor Plan Editor displays a visual grid of each exam hall.
              You can drag students between seats to swap them, double-click to edit register numbers,
              and search for specific students. All changes are saved instantly and will be reflected in
              PDF and Excel downloads.
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
