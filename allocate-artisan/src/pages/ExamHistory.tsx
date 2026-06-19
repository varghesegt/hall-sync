import { useState, useEffect } from "react";
import { archiveApi, ExamArchiveDto } from "@/api/archiveApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { toast } from "sonner";
import { Download, Search, Archive, CalendarDays, FileText } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { getBatches } from "@/api/allocationApi";
import { BatchSelector } from "@/components/BatchSelector";
import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";

export default function ExamHistory() {
  const [archives, setArchives] = useState<ExamArchiveDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  
  const [selectedBatchId, setSelectedBatchId] = useState("");
  const [isArchiving, setIsArchiving] = useState(false);

  // activeBatches filtering and fetching is handled by BatchSelector

  const handleArchive = async () => {
    if (!selectedBatchId) {
      toast.error("Please select a batch to archive.");
      return;
    }
    setIsArchiving(true);
    try {
      await archiveApi.archiveBatch(selectedBatchId, "Admin");
      toast.success("Batch successfully archived!");
      setSelectedBatchId("");
      fetchArchives();
    } catch (error) {
      toast.error("Failed to archive batch.");
    } finally {
      setIsArchiving(false);
    }
  };

  const fetchArchives = async () => {
    setLoading(true);
    try {
      const response = await archiveApi.getAll();
      setArchives(response.data);
    } catch (error) {
      toast.error("Failed to load exam history");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchArchives();
  }, []);

  const handleDownload = async (id: string, type: "seating" | "attendance" | "duty", date: string) => {
    try {
      const response = await archiveApi.downloadSnapshot(id, type);
      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${type}_${date}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      toast.error(`Failed to download ${type} snapshot`);
    }
  };

  const filteredArchives = archives.filter(
    (a) =>
      a.examDate.includes(search) ||
      a.session.toLowerCase().includes(search.toLowerCase()) ||
      a.examType.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Archive className="text-primary" /> Exam History
          </h1>
          <p className="text-slate-500 mt-1">
            Permanent records and snapshots of all conducted exams.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="flex flex-col sm:flex-row items-center gap-2 bg-slate-50 p-2 rounded-xl border border-slate-200 w-full md:w-auto">
            <BatchSelector 
              selectedBatchId={selectedBatchId} 
              onBatchSelect={setSelectedBatchId} 
              statusFilter={["ACTIVE", "COMPLETED"]}
            />
            <Button 
              size="sm" 
              onClick={handleArchive} 
              disabled={!selectedBatchId || isArchiving}
              className="h-[42px] px-6 mt-1 sm:mt-0 sm:self-end mb-[1px]"
            >
              {isArchiving ? <Loader2 className="animate-spin w-4 h-4 mr-2" /> : <Archive className="w-4 h-4 mr-2" />}
              Archive
            </Button>
          </div>

          <div className="relative w-full md:w-auto flex items-center">
            <Search className="absolute left-3 text-slate-400" size={18} />
            <Input
              placeholder="Search by date (YYYY-MM-DD)..."
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>Date & Session</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Metrics</TableHead>
                <TableHead>Issues</TableHead>
                <TableHead className="text-right">Snapshots</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-slate-500">
                    Loading archives...
                  </TableCell>
                </TableRow>
              ) : filteredArchives.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-slate-500">
                    No exam records found.
                  </TableCell>
                </TableRow>
              ) : (
                filteredArchives.map((archive) => (
                  <TableRow key={archive.id}>
                    <TableCell>
                      <div className="font-medium text-slate-900 flex items-center gap-2">
                        <CalendarDays size={14} className="text-slate-400" />
                        {archive.examDate}
                      </div>
                      <div className="text-xs text-slate-500 ml-5 mt-0.5 font-bold">
                        {archive.session}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="bg-slate-50">
                        {archive.examType}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm text-slate-600">
                        <div><span className="font-medium text-slate-800">{archive.totalStudents}</span> students</div>
                        <div className="text-xs text-slate-500 mt-0.5">
                          {archive.totalHalls} halls • {archive.totalInvigilators} invigilators
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        {archive.totalAbsentees > 0 ? (
                          <div className="text-amber-600 font-medium">
                            {archive.totalAbsentees} absent
                          </div>
                        ) : (
                          <div className="text-slate-400 text-xs">No absentees</div>
                        )}
                        {archive.totalMalpractice > 0 && (
                          <div className="text-destructive font-medium mt-0.5 text-xs">
                            {archive.totalMalpractice} malpractice
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        {archive.hasSeatingPlan && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 px-2 text-slate-600 hover:text-primary"
                            onClick={() => handleDownload(archive.id, "seating", archive.examDate)}
                            title="Seating Plan"
                          >
                            <FileText size={16} />
                          </Button>
                        )}
                        {archive.hasAttendance && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 px-2 text-slate-600 hover:text-amber-600"
                            onClick={() => handleDownload(archive.id, "attendance", archive.examDate)}
                            title="Attendance Sheet"
                          >
                            <FileText size={16} />
                          </Button>
                        )}
                        {archive.hasDutySheet && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 px-2 text-slate-600 hover:text-blue-600"
                            onClick={() => handleDownload(archive.id, "duty", archive.examDate)}
                            title="Duty Allocation"
                          >
                            <FileText size={16} />
                          </Button>
                        )}
                        {!archive.hasSeatingPlan && !archive.hasAttendance && !archive.hasDutySheet && (
                          <span className="text-xs text-slate-400 italic">No files</span>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
