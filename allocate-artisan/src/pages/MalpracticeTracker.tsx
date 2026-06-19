import { useState, useEffect } from "react";
import { malpracticeApi, MalpracticeCaseDto } from "@/api/malpracticeApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Upload, Plus, Download, Search, AlertTriangle, ShieldAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export default function MalpracticeTracker() {
  const [cases, setCases] = useState<MalpracticeCaseDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [isAddOpen, setIsAddOpen] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    studentId: "",
    hallId: "",
    caseType: "COPYING",
    description: "",
    severity: "MODERATE",
  });
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null);

  const fetchCases = async () => {
    setLoading(true);
    try {
      const response = await malpracticeApi.getAll();
      setCases(response.data);
    } catch (error) {
      toast.error("Failed to load malpractice cases");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCases();
  }, []);

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await malpracticeApi.create({
        // Ideally we would send proper UUIDs here, but for now we just pass what user entered
        // In a real app we'd have search fields for students/halls
        caseType: formData.caseType,
        description: formData.description,
        severity: formData.severity,
      });

      if (evidenceFile) {
        await malpracticeApi.uploadEvidence(response.data.id, evidenceFile);
      }

      toast.success("Case reported successfully");
      setIsAddOpen(false);
      setFormData({
        studentId: "",
        hallId: "",
        caseType: "COPYING",
        description: "",
        severity: "MODERATE",
      });
      setEvidenceFile(null);
      fetchCases();
    } catch (error) {
      toast.error("Failed to report case");
    }
  };

  const handleStatusUpdate = async (id: string, currentStatus: string) => {
    const nextStatusMap: Record<string, string> = {
      REPORTED: "UNDER_REVIEW",
      UNDER_REVIEW: "ACTION_TAKEN",
      ACTION_TAKEN: "CLOSED",
      CLOSED: "CLOSED",
    };
    const newStatus = nextStatusMap[currentStatus];
    if (newStatus === currentStatus) return;

    let actionTaken = undefined;
    if (newStatus === "ACTION_TAKEN") {
      actionTaken = prompt("Enter action taken (e.g., Debarred for 1 semester):");
      if (!actionTaken) return; // Cancelled
    }

    try {
      await malpracticeApi.updateStatus(id, newStatus, actionTaken);
      toast.success(`Case status updated to ${newStatus}`);
      fetchCases();
    } catch (error) {
      toast.error("Failed to update status");
    }
  };

  const handleDownloadEvidence = async (id: string, filename: string) => {
    try {
      const response = await malpracticeApi.downloadEvidence(id);
      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      toast.error("Failed to download evidence");
    }
  };

  const filteredCases = cases.filter(
    (c) =>
      c.caseType.toLowerCase().includes(search.toLowerCase()) ||
      c.status.toLowerCase().includes(search.toLowerCase()) ||
      (c.studentRegNo && c.studentRegNo.toLowerCase().includes(search.toLowerCase())) ||
      (c.studentName && c.studentName.toLowerCase().includes(search.toLowerCase()))
  );

  const renderKanbanColumn = (status: string, title: string) => {
    const columnCases = filteredCases.filter((c) => c.status === status);
    return (
      <div className="flex-1 w-full lg:min-w-[300px] bg-slate-50 border rounded-xl p-4 flex flex-col h-[400px] lg:h-[calc(100vh-250px)]">
        <div className="flex items-center justify-between mb-4 pb-2 border-b">
          <h3 className="font-semibold text-slate-800">{title}</h3>
          <Badge variant="outline" className="bg-white">
            {columnCases.length}
          </Badge>
        </div>
        <div className="flex-1 overflow-y-auto space-y-3 pr-2 custom-scrollbar">
          {columnCases.map((c) => (
            <div
              key={c.id}
              className="bg-white border shadow-sm rounded-lg p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex justify-between items-start mb-2">
                <Badge
                  variant={
                    c.severity === "CRITICAL"
                      ? "destructive"
                      : c.severity === "HIGH"
                      ? "destructive"
                      : "secondary"
                  }
                >
                  {c.severity}
                </Badge>
                <span className="text-xs text-slate-400">
                  {new Date(c.reportedAt).toLocaleDateString()}
                </span>
              </div>
              <h4 className="font-bold text-slate-800 mb-1">{c.caseType}</h4>
              {c.studentName && (
                <div className="text-sm text-slate-600 mb-2">
                  {c.studentName} ({c.studentRegNo})
                </div>
              )}
              <p className="text-xs text-slate-500 mb-4 line-clamp-3">
                {c.description || "No description provided."}
              </p>

              {c.actionTaken && (
                <div className="bg-amber-50 border border-amber-200 rounded p-2 text-xs text-amber-800 mb-3">
                  <strong>Action:</strong> {c.actionTaken}
                </div>
              )}

              <div className="flex items-center justify-between pt-3 border-t mt-auto">
                {c.hasEvidence ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 text-xs text-blue-600 px-2"
                    onClick={() =>
                      handleDownloadEvidence(c.id, c.evidenceFilename || "evidence.file")
                    }
                  >
                    <Download size={14} className="mr-1" /> Evidence
                  </Button>
                ) : (
                  <span className="text-xs text-slate-400 italic px-2">No evidence</span>
                )}

                {status !== "CLOSED" && (
                  <Button
                    size="sm"
                    className="h-8 text-xs"
                    onClick={() => handleStatusUpdate(c.id, status)}
                  >
                    Move Forward
                  </Button>
                )}
              </div>
            </div>
          ))}
          {columnCases.length === 0 && (
            <div className="text-center text-slate-400 text-sm py-8 italic">
              No cases
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="w-full max-w-7xl mx-auto py-8 px-4 animate-in fade-in duration-500 overflow-hidden sm:overflow-visible">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4 md:flex-wrap">
        <div className="flex-1 min-w-0 pr-4">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 flex items-center gap-2 truncate">
            <ShieldAlert className="text-destructive shrink-0" /> <span className="truncate">Malpractice Tracker</span>
          </h1>
          <p className="text-slate-500 mt-1 text-sm sm:text-base truncate">
            Track and manage disciplinary cases from report to resolution.
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto shrink-0 flex-wrap sm:flex-nowrap">
          <div className="relative w-full sm:w-[200px] flex items-center">
            <Search className="absolute left-3 text-slate-400" size={16} />
            <Input
              placeholder="Search cases..."
              className="pl-9 h-10 w-full"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2 shrink-0 whitespace-nowrap">
                <Plus size={16} /> Report Case
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md w-[95vw] sm:w-full max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>Report New Malpractice Case</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleAddSubmit} className="grid gap-4 py-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <Label>Case Type</Label>
                    <select
                      className="flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      value={formData.caseType}
                      onChange={(e) => setFormData({ ...formData, caseType: e.target.value })}
                    >
                      <option value="COPYING">Copying</option>
                      <option value="MOBILE_USAGE">Mobile Usage</option>
                      <option value="IMPERSONATION">Impersonation</option>
                      <option value="MATERIAL_POSSESSION">Material Possession</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Severity</Label>
                    <select
                      className="flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      value={formData.severity}
                      onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
                    >
                      <option value="LOW">Low</option>
                      <option value="MODERATE">Moderate</option>
                      <option value="HIGH">High</option>
                      <option value="CRITICAL">Critical</option>
                    </select>
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <Label>Description</Label>
                  <textarea
                    className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="Provide details of the incident..."
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label>Evidence (Photo/PDF)</Label>
                  <Input
                    type="file"
                    onChange={(e) => setEvidenceFile(e.target.files ? e.target.files[0] : null)}
                  />
                </div>

                <Button type="submit" className="mt-4 w-full">
                  Submit Report
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="w-full overflow-x-auto custom-scrollbar pb-6 -mx-4 px-4 sm:mx-0 sm:px-0">
        <div className="flex flex-col lg:flex-row gap-4 min-w-full lg:min-w-max pb-2">
          {renderKanbanColumn("REPORTED", "Reported")}
          {renderKanbanColumn("UNDER_REVIEW", "Under Review")}
          {renderKanbanColumn("ACTION_TAKEN", "Action Taken")}
          {renderKanbanColumn("CLOSED", "Closed")}
        </div>
      </div>
    </div>
  );
}
