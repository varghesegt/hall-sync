import { useRef, useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useMutation } from "@tanstack/react-query";
import { uploadInternalExcel } from "@/api/internalApi";
import type { UploadResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { Upload, FileSpreadsheet, AlertCircle, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface ExcelUploadCardProps {
  onSuccess: (fileId: string) => void;
  fileId: string | null;
}

export function ExcelUploadCard({ onSuccess, fileId }: ExcelUploadCardProps) {
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

  const isComplete = !!fileId;

  return (
    <Card className={cn(isComplete && "border-emerald-300/50")}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base">1. Upload Student List</CardTitle>
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
          <div className="flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50/50 p-3 text-sm">
            <FileSpreadsheet className="h-4 w-4 text-emerald-600" />
            <span className="truncate font-medium text-slate-700">{fileName}</span>
            <span className="ml-auto text-xs text-slate-400">ID: {fileId.slice(0, 8)}…</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
