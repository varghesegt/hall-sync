import { useState, useEffect } from "react";
import { dutyApi, DutyDto } from "@/api/dutyApi";
import apiClient from "@/api/axios";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import { Download, Play, CheckCircle2, XCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { BatchSelector } from "@/components/BatchSelector";

export default function DutyAllocation() {
  const [selectedBatch, setSelectedBatch] = useState<string>("");
  const [duties, setDuties] = useState<DutyDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [allocating, setAllocating] = useState(false);



  // Fetch duties when batch changes
  useEffect(() => {
    if (selectedBatch) {
      fetchDuties(selectedBatch);
    }
  }, [selectedBatch]);

  const fetchDuties = async (batchId: string) => {
    setLoading(true);
    try {
      const response = await dutyApi.getByBatch(batchId);
      setDuties(response.data);
    } catch (error) {
      toast.error("Failed to load duties");
    } finally {
      setLoading(false);
    }
  };

  const handleAllocate = async () => {
    if (!selectedBatch) return;
    setAllocating(true);
    try {
      const response = await dutyApi.allocate(selectedBatch);
      toast.success(
        `Successfully allocated ${response.data.assigned} invigilators for ${response.data.totalHalls} halls.`
      );
      if (response.data.warnings && response.data.warnings.length > 0) {
        toast.warning(`${response.data.warnings.length} constraints relaxed`, {
          description: "Some faculty were assigned with relaxed constraints. Check warnings.",
        });
      }
      fetchDuties(selectedBatch);
    } catch (error: any) {
      toast.error("Allocation Failed", {
        description: error.message || "Failed to allocate duties",
      });
    } finally {
      setAllocating(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedBatch) return;
    try {
      const response = await dutyApi.downloadExcel(selectedBatch);
      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `invigilation_duties_${selectedBatch}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      toast.error("Failed to download duty sheet");
    }
  };

  const toggleAttendance = async (dutyId: string, currentState: boolean | null) => {
    try {
      const newState = !currentState;
      await dutyApi.markAttendance(dutyId, newState);
      setDuties(
        duties.map((d) => (d.id === dutyId ? { ...d, isPresent: newState } : d))
      );
      toast.success("Attendance marked");
    } catch (error) {
      toast.error("Failed to mark attendance");
    }
  };

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">
            Duty Allocation
          </h1>
          <p className="text-slate-500 mt-1">
            Allocate and manage invigilation duties for exam sessions.
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="w-full md:w-[450px]">
            <BatchSelector 
              selectedBatchId={selectedBatch} 
              onBatchSelect={setSelectedBatch} 
              statusFilter={["ACTIVE", "COMPLETED"]}
            />
          </div>

          <Button
            onClick={handleAllocate}
            disabled={!selectedBatch || allocating}
            className="gap-2"
          >
            <Play size={16} />
            {allocating ? "Allocating..." : "Run Engine"}
          </Button>

          <Button
            variant="outline"
            onClick={handleDownload}
            disabled={!selectedBatch || duties.length === 0}
            className="gap-2 border-blue-200 text-blue-700 hover:bg-blue-50"
          >
            <Download size={16} /> Duty Sheet
          </Button>

          <Button
            variant="outline"
            onClick={() => {
              if (selectedBatch) {
                const url = `${import.meta.env.VITE_API_BASE_URL || "/api/v1"}/attendance/batch/${selectedBatch}/export-absentees`;
                window.open(url, "_blank");
              }
            }}
            disabled={!selectedBatch || duties.length === 0}
            className="gap-2 border-amber-200 text-amber-700 hover:bg-amber-50"
          >
            <Download size={16} /> Absentee Report
          </Button>
        </div>
      </div>

      <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b bg-slate-50/50 flex items-center justify-between">
          <div className="font-semibold text-slate-700">
            Assigned Duties: {duties.length}
          </div>
        </div>

        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>Faculty Name</TableHead>
                <TableHead>Department</TableHead>
                <TableHead>Hall</TableHead>
                <TableHead>Shift</TableHead>
                <TableHead>Duty Type</TableHead>
                <TableHead className="text-right">Attendance</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-slate-500">
                    Loading duties...
                  </TableCell>
                </TableRow>
              ) : duties.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-slate-500">
                    No duties allocated for this batch yet.
                  </TableCell>
                </TableRow>
              ) : (
                duties.map((duty) => (
                  <TableRow key={duty.id}>
                    <TableCell className="font-medium text-slate-900">
                      <div>{duty.facultyName}</div>
                      <div className="text-xs text-slate-500 font-mono">
                        {duty.employeeId}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{duty.facultyDepartment}</Badge>
                    </TableCell>
                    <TableCell className="font-semibold">{duty.hallName}</TableCell>
                    <TableCell>{duty.shift}</TableCell>
                    <TableCell>{duty.dutyType}</TableCell>
                    <TableCell className="text-right">
                      {duty.isPresent === true ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-green-600 hover:text-green-700 hover:bg-green-50"
                          onClick={() => toggleAttendance(duty.id, duty.isPresent)}
                        >
                          <CheckCircle2 size={18} className="mr-1" /> Present
                        </Button>
                      ) : duty.isPresent === false ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          onClick={() => toggleAttendance(duty.id, duty.isPresent)}
                        >
                          <XCircle size={18} className="mr-1" /> Absent
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => toggleAttendance(duty.id, null)}
                        >
                          Mark
                        </Button>
                      )}
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
