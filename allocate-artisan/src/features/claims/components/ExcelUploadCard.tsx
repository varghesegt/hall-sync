import { useRef, useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useMutation } from "@tanstack/react-query";
import { uploadInternalExcel } from "@/api/internalApi";
import type { UploadResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { Upload, FileSpreadsheet, AlertCircle, CheckCircle2, RotateCcw } from "lucide-react";
import { cn } from "@/lib/utils";

interface ExcelUploadCardProps {
  onSuccess: (fileId: string) => void;
  fileId: string | null;
  onReset?: () => void;
}

export function ExcelUploadCard({ onSuccess, fileId, onReset }: ExcelUploadCardProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  
  const upload = useMutation<UploadResponse, ApiError, File>({
    mutationFn: uploadInternalExcel,
  });

  const handleFile = (file: File) => {
    if (!file.name.toLowerCase().endsWith(".xlsx")) {
      return;
    }
    setFileName(file.name);
    upload.mutate(file, {
      onSuccess: (data) => onSuccess(data.fileId),
    });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const handleReset = () => {
    setFileName(null);
    upload.reset();
    if (onReset) {
      onReset();
    } else {
      onSuccess("");
    }
  };

  const isComplete = !!fileId;

  return (
    <Card className={cn("transition-all duration-300", isComplete && "border-emerald-300/50 shadow-emerald-500/5 shadow-md bg-white")}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-bold text-slate-900">1. Upload Student List</CardTitle>
            <CardDescription>Upload an Excel (.xlsx) file containing the student roster</CardDescription>
          </div>
          {isComplete && <CheckCircle2 className="h-5 w-5 text-emerald-500" />}
        </div>
      </CardHeader>
      <CardContent>
        <input
          ref={inputRef}
          type="file"
          accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) handleFile(f);
          }}
        />

        {!isComplete ? (
          <div className="space-y-3">
            <div
              onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={handleDrop}
              className={cn(
                "border-2 border-dashed rounded-xl p-8 text-center transition-all duration-300 cursor-pointer",
                isDragOver
                  ? "border-emerald-400 bg-emerald-50/50"
                  : "border-slate-200 hover:border-emerald-300 hover:bg-slate-50/50"
              )}
              onClick={() => inputRef.current?.click()}
            >
              {upload.isPending ? (
                <div className="flex flex-col items-center gap-2">
                  <span className="h-8 w-8 animate-spin rounded-full border-3 border-slate-200 border-t-emerald-500" />
                  <span className="text-sm font-medium text-slate-600">Processing Excel file…</span>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2">
                  <FileSpreadsheet className={cn("h-10 w-10", isDragOver ? "text-emerald-500" : "text-slate-300")} />
                  <span className="text-sm font-medium text-slate-600">
                    {isDragOver ? "Drop Excel file here" : "Drag & drop or click to select Excel file"}
                  </span>
                  <span className="text-xs text-slate-400">Only .xlsx files accepted</span>
                </div>
              )}
            </div>

            {upload.isError && (
              <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{upload.error.message}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50/60 p-4 text-sm shadow-xs">
            <div className="flex items-center gap-3 min-w-0">
              <div className="p-2.5 bg-emerald-100 text-emerald-700 rounded-lg shrink-0">
                <FileSpreadsheet className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <p className="truncate font-bold text-slate-800">{fileName || "Student Roster Uploaded"}</p>
                <p className="text-xs font-semibold text-slate-500 tracking-wider">ID: {fileId.slice(0, 8)}… • Verified & Ready</p>
              </div>
            </div>

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleReset}
              className="h-9 px-3.5 text-xs font-bold text-slate-700 hover:text-rose-600 hover:border-rose-300 hover:bg-rose-50 border-slate-300 bg-white shrink-0 gap-1.5 transition-all shadow-2xs"
              title="Reset and upload a different Excel file"
            >
              <RotateCcw className="h-3.5 w-3.5 text-slate-500" />
              Reset / Change File
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
