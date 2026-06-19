import { useRef, useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useUpload } from "@/hooks/useUpload";
import { Upload, FileText, AlertCircle, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface UploadCardProps {
  onSuccess: (fileId: string) => void;
  fileId: string | null;
}

export function UploadCard({ onSuccess, fileId }: UploadCardProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const upload = useUpload();

  const handleFile = (file: File) => {
    if (file.type !== "application/pdf") {
      return;
    }
    setFileName(file.name);
    upload.mutate(file, {
      onSuccess: (data) => onSuccess(data.fileId),
    });
  };

  const isComplete = !!fileId;

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden", isComplete ? "border-success/40 shadow-success/10 shadow-lg bg-white/90" : "glass-panel")}>
      <CardHeader className="bg-slate-50/50 border-b border-slate-100/50 pb-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={cn("p-2 rounded-xl flex items-center justify-center shrink-0 transition-colors", isComplete ? "bg-success/10 text-success" : "bg-primary/10 text-primary")}>
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-black tracking-tight text-slate-900">1. Data Ingestion</CardTitle>
              <CardDescription className="text-xs font-semibold uppercase tracking-wider mt-1 text-slate-500">Upload nominal roll roster</CardDescription>
            </div>
          </div>
          {isComplete && <CheckCircle2 className="h-6 w-6 text-success animate-in zoom-in duration-500" />}
        </div>
      </CardHeader>
      <CardContent className="pt-6">
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) handleFile(f);
          }}
        />

        {!isComplete ? (
          <div className="space-y-4">
            <div 
              className="border-2 border-dashed border-slate-200 hover:border-primary/50 bg-slate-50/50 hover:bg-primary/5 rounded-2xl p-8 transition-all duration-300 cursor-pointer flex flex-col items-center justify-center gap-3 group"
              onClick={() => inputRef.current?.click()}
            >
              <div className="p-4 bg-white rounded-full shadow-sm group-hover:shadow-md transition-all group-hover:-translate-y-1">
                <FileText className="h-8 w-8 text-slate-400 group-hover:text-primary transition-colors" />
              </div>
              <div className="text-center">
                <p className="text-sm font-bold text-slate-700">Click to browse or drag and drop</p>
                <p className="text-xs text-slate-500 mt-1 font-medium">PDF documents only</p>
              </div>
            </div>

            {upload.isPending && (
              <div className="flex items-center justify-center gap-3 p-4 bg-primary/5 rounded-xl border border-primary/10 animate-pulse">
                <span className="h-5 w-5 animate-spin rounded-full border-2 border-primary/20 border-t-primary" />
                <span className="text-sm font-bold text-primary">Processing Document...</span>
              </div>
            )}

            {upload.isError && (
              <div className="flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive shadow-sm">
                <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
                <span className="font-medium">{upload.error.message}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="p-2.5 bg-slate-100 rounded-lg shrink-0">
              <FileText className="h-5 w-5 text-slate-600" />
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-bold text-slate-900 truncate">{fileName}</span>
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest mt-0.5">ID: {fileId.slice(0, 8)}…</span>
            </div>
            <div className="ml-auto pl-2">
              <Button variant="ghost" size="sm" onClick={() => onSuccess("")} className="text-xs font-bold text-slate-400 hover:text-rose-500">
                Reset
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
