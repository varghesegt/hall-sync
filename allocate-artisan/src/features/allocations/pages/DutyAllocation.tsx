import { useState, useEffect } from "react";
import { dutyApi, DutyDto } from "@/api/dutyApi";
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
import { Download, Play, CheckCircle2, XCircle, Search, Users, ShieldAlert, CalendarDays, Building2, UserCheck, ShieldCheck, RefreshCw, FileText } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { getBatchPreview, PreviewSeat } from "@/api/allocationApi";
import { Checkbox } from "@/components/ui/checkbox";

export default function DutyAllocation() {
  const [selectedBatch, setSelectedBatch] = useState<string>("");
  const [duties, setDuties] = useState<DutyDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [allocating, setAllocating] = useState(false);

  // Stats & Preview State
  const [totalStudents, setTotalStudents] = useState<number>(0);
  const [requiredHalls, setRequiredHalls] = useState<number>(0);

  // Faculty selection
  const [availableFaculty, setAvailableFaculty] = useState<any[]>([]);
  const [selectedFacultyIds, setSelectedFacultyIds] = useState<Set<string>>(new Set());
  const [loadingFaculty, setLoadingFaculty] = useState(false);
  const [facultySearch, setFacultySearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("ALL");

  useEffect(() => {
    if (selectedBatch) {
      fetchDuties(selectedBatch);
      fetchBatchRequirements(selectedBatch);
      fetchAllFaculty();
    } else {
      setRequiredHalls(0);
      setTotalStudents(0);
      setSelectedFacultyIds(new Set());
    }
  }, [selectedBatch]);

  const fetchBatchRequirements = async (batchId: string) => {
    try {
      const preview: PreviewSeat[] = await getBatchPreview(batchId);
      const uniqueHalls = new Set(preview.map((p) => p.hallId));
      setRequiredHalls(uniqueHalls.size);
      setTotalStudents(preview.length);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchAllFaculty = async () => {
    setLoadingFaculty(true);
    try {
      const response = await dutyApi.getAllFaculty();
      setAvailableFaculty(response.data.filter((f) => f.isActive && f.isAvailable));
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
      setSelectedFacultyIds(new Set(availableFaculty.map((f) => f.id)));
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
        toast.warning(`${response.data.warnings.length} constraints relaxed`);
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
      toast.success("Duty allocation cleared.");
      setFacultySearch("");
      fetchDuties(selectedBatch);
    } catch (error) {
      toast.error("Failed to clear allocation");
    }
  };

  const handleDownload = async () => {
    if (!selectedBatch) return;
    try {
      const response = await dutyApi.downloadExcel(selectedBatch);
      const contentDisposition = response.headers["content-disposition"];
      let filename = `Semester_Exam_Duty_Sheet_${new Date().toISOString().split("T")[0]}.xlsx`;
      if (contentDisposition) {
        const match = contentDisposition.match(/filename="?([^";]+)"?/);
        if (match && match[1]) filename = match[1];
      }

      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success(`Downloaded ${filename}`);
    } catch (error) {
      toast.error("Failed to download duty sheet");
    }
  };

  const handleDownloadWord = async () => {
    if (!selectedBatch) return;
    try {
      const response = await dutyApi.downloadWord(selectedBatch);
      const contentDisposition = response.headers["content-disposition"];
      let filename = `Semester_Exam_Duty_Chart_${new Date().toISOString().split("T")[0]}.docx`;
      if (contentDisposition) {
        const match = contentDisposition.match(/filename="?([^";]+)"?/);
        if (match && match[1]) filename = match[1];
      }

      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success(`Downloaded ${filename}`);
    } catch (error) {
      toast.error("Failed to download Word duty sheet");
    }
  };

  const handleSwap = async (dutyId: string, newFacultyId: string) => {
    try {
      await dutyApi.swapDuty(dutyId, newFacultyId);
      toast.success("Duty reassigned successfully.");
      if (selectedBatch) fetchDuties(selectedBatch);
    } catch (e) {
      toast.error("Failed to reassign duty");
    }
  };

  const toggleAttendance = async (dutyId: string, currentState: boolean | null) => {
    try {
      const newState = !currentState;
      await dutyApi.markAttendance(dutyId, newState);
      setDuties(duties.map((d) => (d.id === dutyId ? { ...d, isPresent: newState } : d)));
      toast.success("Attendance updated");
    } catch (error) {
      toast.error("Failed to update attendance");
    }
  };

  const departmentsList = Array.from(new Set(availableFaculty.map((f) => f.department)));

  return (
    <div className="container mx-auto py-8 px-4 max-w-7xl animate-in fade-in duration-500">
      {/* Header Glassmorphic Banner */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center mb-8 gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-600 text-white rounded-xl shadow-md shadow-indigo-200">
              <CalendarDays className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
                Semester Exam Duty Allocation
              </h1>
              <p className="text-xs font-medium text-slate-500 mt-0.5">
                Automated invigilation duty allocation with workload balancing & department isolation.
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
          <div className="w-full sm:w-[380px]">
            <BatchSelector
              selectedBatchId={selectedBatch}
              onBatchSelect={setSelectedBatch}
              statusFilter={["ACTIVE", "COMPLETED"]}
              examTypeFilter="SEMESTER"
            />
          </div>

          <Button
            onClick={handleAllocate}
            disabled={!selectedBatch || allocating || selectedFacultyIds.size < requiredHalls || duties.length > 0}
            className="gap-2 bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
          >
            <Play size={16} />
            {allocating ? "Allocating..." : "Auto Allocate"}
          </Button>

          {duties.length > 0 && (
            <Button
              variant="outline"
              onClick={handleReverse}
              className="gap-2 border-red-200 text-red-700 hover:bg-red-50"
            >
              <XCircle size={16} /> Reset
            </Button>
          )}

          <Button
            variant="outline"
            onClick={handleDownloadWord}
            disabled={!selectedBatch || duties.length === 0}
            className="gap-2 border-indigo-200 text-indigo-700 hover:bg-indigo-50"
          >
            <Download size={16} /> Word Chart (.docx)
          </Button>

          <Button
            variant="outline"
            onClick={handleDownload}
            disabled={!selectedBatch || duties.length === 0}
            className="gap-2 border-emerald-200 text-emerald-700 hover:bg-emerald-50"
          >
            <Download size={16} /> Excel Sheet
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

      {/* Overview Stat Cards */}
      {selectedBatch && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
              <Building2 size={24} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Exam Halls</p>
              <p className="text-2xl font-black text-slate-900 mt-0.5">{requiredHalls}</p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl">
              <Users size={24} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Seated Students</p>
              <p className="text-2xl font-black text-slate-900 mt-0.5">{totalStudents}</p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
              <UserCheck size={24} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Required Invigilators</p>
              <p className="text-2xl font-black text-slate-900 mt-0.5">{requiredHalls}</p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
              <ShieldCheck size={24} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Dept Isolation Rule</p>
              <p className="text-sm font-bold text-emerald-700 mt-1">100% Enforced</p>
            </div>
          </div>
        </div>
      )}

      {/* Staff Selection Panel (Before Allocation) */}
      {selectedBatch && duties.length === 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden mb-8">
          <div className="p-6 border-b border-slate-200 bg-slate-50/50 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-start gap-4">
              <div className="p-3 bg-indigo-100 text-indigo-600 rounded-xl border border-indigo-200">
                <Users size={24} />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">Select Available Faculty for Semester Duty</h3>
                <p className="text-xs text-slate-500 mt-1 max-w-lg leading-relaxed">
                  Select at least <strong className="text-slate-800">{requiredHalls}</strong> faculty members to cover all halls for this session.
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
              <Badge
                variant={selectedFacultyIds.size >= requiredHalls ? "default" : "secondary"}
                className={`px-3 py-1.5 text-xs font-bold uppercase tracking-wider ${
                  selectedFacultyIds.size >= requiredHalls
                    ? "bg-emerald-500 hover:bg-emerald-600 text-white shadow-sm"
                    : "text-slate-600"
                }`}
              >
                Selected: {selectedFacultyIds.size} / {requiredHalls}
              </Badge>

              <div className="relative flex-1 md:w-56">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search faculty..."
                  className="w-full rounded-xl border border-slate-200 bg-white pl-9 pr-4 py-1.5 text-xs text-slate-700 shadow-sm focus:border-indigo-500 focus:outline-none"
                  value={facultySearch}
                  onChange={(e) => setFacultySearch(e.target.value)}
                />
              </div>

              <select
                className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-700 shadow-sm focus:outline-none"
                value={deptFilter}
                onChange={(e) => setDeptFilter(e.target.value)}
              >
                <option value="ALL">All Departments</option>
                {departmentsList.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>

              <Button variant="outline" size="sm" onClick={toggleAllFaculty} className="border-slate-200 text-xs rounded-xl px-3">
                {selectedFacultyIds.size === availableFaculty.length ? "Deselect All" : "Select All"}
              </Button>
            </div>
          </div>

          <div className="max-h-[420px] overflow-y-auto">
            {loadingFaculty ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3 text-slate-400">
                <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
                <p className="font-medium text-xs">Loading available faculty...</p>
              </div>
            ) : availableFaculty.length === 0 ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3 text-slate-400">
                <ShieldAlert size={32} className="text-slate-300" />
                <p className="font-medium text-sm">No active faculty found.</p>
              </div>
            ) : (
              <Table>
                <TableHeader className="bg-white sticky top-0 z-10 shadow-sm">
                  <TableRow className="border-slate-100">
                    <TableHead className="w-[50px] pl-6"></TableHead>
                    <TableHead className="font-bold text-slate-700 text-xs">Faculty Name</TableHead>
                    <TableHead className="font-bold text-slate-700 text-xs">Department</TableHead>
                    <TableHead className="font-bold text-slate-700 text-xs">Designation</TableHead>
                    <TableHead className="font-bold text-slate-700 text-xs text-right pr-8">Employee ID</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {availableFaculty
                    .filter((f) => (deptFilter === "ALL" ? true : f.department === deptFilter))
                    .filter(
                      (f) =>
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
                            className={`w-4 h-4 rounded ${
                              selectedFacultyIds.has(f.id) ? "bg-indigo-600 border-indigo-600" : "border-slate-300"
                            }`}
                          />
                        </TableCell>
                        <TableCell className="font-bold text-slate-800 text-xs">{f.name}</TableCell>
                        <TableCell>
                          <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600 uppercase">
                            {f.department}
                          </span>
                        </TableCell>
                        <TableCell className="text-xs text-slate-600">{f.designation || "Assistant Professor"}</TableCell>
                        <TableCell className="text-slate-500 font-mono text-xs text-right pr-8">{f.employeeId}</TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
      )}

      {/* Allocated Duties Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-200 bg-white flex items-center justify-between">
          <h3 className="font-extrabold text-slate-900 text-base">
            Assigned Semester Invigilation Duties ({duties.length})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead className="font-bold text-slate-700 text-xs">Faculty Name</TableHead>
                <TableHead className="font-bold text-slate-700 text-xs">Department</TableHead>
                <TableHead className="font-bold text-slate-700 text-xs">Assigned Hall</TableHead>
                <TableHead className="font-bold text-slate-700 text-xs">Shift</TableHead>
                <TableHead className="font-bold text-slate-700 text-xs">Duty Role</TableHead>
                <TableHead className="font-bold text-slate-700 text-xs text-right">Reassign & Attendance</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-slate-500 text-xs">
                    Loading invigilation duties...
                  </TableCell>
                </TableRow>
              ) : duties.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center text-slate-400 gap-3">
                      <div className="p-4 bg-slate-50 rounded-full">
                        <CheckCircle2 size={32} className="text-slate-300" />
                      </div>
                      <p className="font-bold text-slate-600 text-sm">No duties allocated for this batch yet.</p>
                      <p className="text-xs text-slate-400 max-w-sm">
                        Select a batch, select faculty members, and click Auto Allocate.
                      </p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                duties.map((duty) => (
                  <TableRow key={duty.id} className="hover:bg-slate-50/60 transition-colors">
                    <TableCell className="font-bold text-slate-900 text-xs">
                      <div>{duty.facultyName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">{duty.employeeId}</div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary" className="text-[11px] uppercase">{duty.facultyDepartment}</Badge>
                    </TableCell>
                    <TableCell className="font-extrabold text-indigo-700 text-xs">{duty.hallName}</TableCell>
                    <TableCell className="text-xs font-medium text-slate-600">{duty.shift}</TableCell>
                    <TableCell>
                      <Badge className="bg-indigo-100 text-indigo-700 hover:bg-indigo-200 border-indigo-200 text-[11px]">
                        {duty.dutyType}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right flex items-center justify-end gap-2">
                      <Select
                        onValueChange={(newFacId) => handleSwap(duty.id, newFacId)}
                        defaultValue={duty.facultyId}
                      >
                        <SelectTrigger className="w-[140px] h-8 text-xs bg-slate-50 border-slate-200">
                          <SelectValue placeholder="Reassign" />
                        </SelectTrigger>
                        <SelectContent>
                          {availableFaculty.map((f) => (
                            <SelectItem key={f.id} value={f.id} className="text-xs">
                              {f.name} ({f.department})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>

                      {duty.isPresent === true ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 h-8 text-xs"
                          onClick={() => toggleAttendance(duty.id, duty.isPresent)}
                        >
                          <CheckCircle2 size={15} className="mr-1" /> Present
                        </Button>
                      ) : duty.isPresent === false ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-rose-600 hover:text-rose-700 hover:bg-rose-50 h-8 text-xs"
                          onClick={() => toggleAttendance(duty.id, duty.isPresent)}
                        >
                          <XCircle size={15} className="mr-1" /> Absent
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 text-xs"
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
