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
import { Download, Play, CheckCircle2, XCircle, Search, Users, ShieldAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { getBatchPreview, PreviewSeat } from "@/api/allocationApi";
import { Checkbox } from "@/components/ui/checkbox";

export default function DutyAllocation() {
  const [selectedBatch, setSelectedBatch] = useState<string>("");
  const [duties, setDuties] = useState<DutyDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [allocating, setAllocating] = useState(false);
  
  // New states for manual selection
  const [availableFaculty, setAvailableFaculty] = useState<any[]>([]);
  const [selectedFacultyIds, setSelectedFacultyIds] = useState<Set<string>>(new Set());
  const [requiredHalls, setRequiredHalls] = useState<number>(0);
  const [loadingFaculty, setLoadingFaculty] = useState(false);
  const [facultySearch, setFacultySearch] = useState("");



  // Fetch duties, requirements and faculty when batch changes
  useEffect(() => {
    if (selectedBatch) {
      fetchDuties(selectedBatch);
      fetchBatchRequirements(selectedBatch);
      fetchAllFaculty();
    } else {
      setRequiredHalls(0);
      setSelectedFacultyIds(new Set());
    }
  }, [selectedBatch]);

  const fetchBatchRequirements = async (batchId: string) => {
    try {
      const preview = await getBatchPreview(batchId);
      const uniqueHalls = new Set(preview.map(p => p.hallId));
      setRequiredHalls(uniqueHalls.size);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchAllFaculty = async () => {
    setLoadingFaculty(true);
    try {
      const response = await dutyApi.getAllFaculty();
      setAvailableFaculty(response.data.filter(f => f.isActive && f.isAvailable));
    } catch (error) {
      toast.error("Failed to load faculty list");
    } finally {
      setLoadingFaculty(false);
    }
  };

  const toggleFaculty = (id: string) => {
    const newSet = new Set(selectedFacultyIds);
    if (newSet.has(id)) newSet.delete(id);
    else newSet.add(id);
    setSelectedFacultyIds(newSet);
  };
  
  const toggleAllFaculty = () => {
    if (selectedFacultyIds.size === availableFaculty.length) {
      setSelectedFacultyIds(new Set());
    } else {
      setSelectedFacultyIds(new Set(availableFaculty.map(f => f.id)));
    }
  };

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
    if (selectedFacultyIds.size < requiredHalls) {
      toast.error(`Please select at least ${requiredHalls} faculty members for ${requiredHalls} halls.`);
      return;
    }
    
    setAllocating(true);
    try {
      const response = await dutyApi.allocate(selectedBatch, Array.from(selectedFacultyIds));
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

  const handleReverse = async () => {
    if (!selectedBatch) return;
    try {
      await dutyApi.clearAllocation(selectedBatch);
      toast.success("Duty allocation reversed successfully.");
      setFacultySearch("");
      fetchDuties(selectedBatch);
    } catch (error) {
      toast.error("Failed to reverse allocation");
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
            disabled={!selectedBatch || allocating || selectedFacultyIds.size < requiredHalls || duties.length > 0}
            className="gap-2"
          >
            <Play size={16} />
            {allocating ? "Allocating..." : "Run Engine"}
          </Button>

          {duties.length > 0 && (
            <Button
              variant="outline"
              onClick={handleReverse}
              className="gap-2 border-red-200 text-red-700 hover:bg-red-50"
            >
              <XCircle size={16} /> Reverse
            </Button>
          )}

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

      {selectedBatch && duties.length === 0 && (
        <div className="bg-white border rounded-xl shadow-sm overflow-hidden mb-8">
          <div className="p-6 border-b bg-gradient-to-r from-slate-50 to-white flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-start gap-4">
              <div className="p-3 bg-blue-100/50 text-blue-600 rounded-xl">
                <Users size={24} />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-800">Select Invigilators</h3>
                <p className="text-sm text-slate-500 mt-1 max-w-lg leading-relaxed">
                  Select <strong className="text-slate-700">{requiredHalls}</strong> staff members to allocate to <strong className="text-slate-700">{requiredHalls}</strong> exam halls.
                  The engine will strictly assign from this pool.
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4 w-full md:w-auto">
              <Badge variant={selectedFacultyIds.size >= requiredHalls ? "default" : "secondary"} className={`px-3 py-1.5 text-xs font-semibold uppercase tracking-wider ${selectedFacultyIds.size >= requiredHalls ? "bg-emerald-500 hover:bg-emerald-600 shadow-sm shadow-emerald-200 text-white" : "text-slate-600"}`}>
                Selected: {selectedFacultyIds.size} / {requiredHalls}
              </Badge>
              
              <div className="relative flex-1 md:w-64">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input 
                  type="text" 
                  placeholder="Search name or ID..." 
                  className="w-full rounded-xl border border-slate-200 bg-white/50 pl-9 pr-4 py-2 text-sm text-slate-700 shadow-sm transition-all focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-blue-500/10 placeholder:text-slate-400"
                  value={facultySearch}
                  onChange={(e) => setFacultySearch(e.target.value)}
                />
              </div>

              <Button variant="outline" size="sm" onClick={toggleAllFaculty} className="border-slate-200 shadow-sm hover:bg-slate-50 rounded-xl whitespace-nowrap px-4">
                {selectedFacultyIds.size === availableFaculty.length ? "Deselect All" : "Select All"}
              </Button>
            </div>
          </div>
          <div className="p-0 max-h-[500px] overflow-y-auto custom-scrollbar">
            {loadingFaculty ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3 text-slate-400">
                <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                <p className="font-medium text-sm">Loading faculty matrix...</p>
              </div>
            ) : availableFaculty.length === 0 ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3 text-slate-400">
                <ShieldAlert size={32} className="text-slate-300" />
                <p className="font-medium">No available faculty found for this configuration.</p>
              </div>
            ) : (
              <Table>
                <TableHeader className="bg-white/95 backdrop-blur-sm sticky top-0 z-10 shadow-sm">
                  <TableRow className="border-slate-100 hover:bg-transparent">
                    <TableHead className="w-[60px] pl-6"></TableHead>
                    <TableHead className="font-bold text-slate-700">Faculty Member</TableHead>
                    <TableHead className="font-bold text-slate-700">Department</TableHead>
                    <TableHead className="font-bold text-slate-700 text-right pr-8">Employee ID</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {availableFaculty
                    .filter(f => 
                      f.name.toLowerCase().includes(facultySearch.toLowerCase()) || 
                      f.department.toLowerCase().includes(facultySearch.toLowerCase()) ||
                      f.employeeId.toLowerCase().includes(facultySearch.toLowerCase())
                    )
                    .map((f) => (
                    <TableRow key={f.id} className="hover:bg-slate-50/80 transition-colors border-slate-100">
                      <TableCell className="pl-6">
                        <Checkbox 
                          checked={selectedFacultyIds.has(f.id)}
                          onCheckedChange={() => toggleFaculty(f.id)}
                          className={`w-5 h-5 rounded-md ${selectedFacultyIds.has(f.id) ? 'bg-blue-600 border-blue-600' : 'border-slate-300'}`}
                        />
                      </TableCell>
                      <TableCell className="font-semibold text-slate-800">{f.name}</TableCell>
                      <TableCell>
                        <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600 ring-1 ring-inset ring-slate-500/10 uppercase tracking-widest">
                          {f.department}
                        </span>
                      </TableCell>
                      <TableCell className="text-slate-500 font-mono text-sm text-right pr-8">{f.employeeId}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
      )}

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
                  <TableCell colSpan={6} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center text-slate-400 gap-3">
                      <div className="p-4 bg-slate-50 rounded-full">
                        <CheckCircle2 size={32} className="text-slate-300" />
                      </div>
                      <p className="font-medium text-slate-500">No duties allocated for this batch yet.</p>
                      <p className="text-sm text-slate-400 max-w-sm">Select a batch and run the engine to automatically allocate staff duties.</p>
                    </div>
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
