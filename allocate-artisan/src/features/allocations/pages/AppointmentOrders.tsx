import { useState, useEffect } from "react";
import { FacultyDto, facultyApi } from "@/api/facultyApi";
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Search, FileText, Download, Building2, Globe, Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { CustomOrderDialog } from "./CustomOrderDialog";

export default function AppointmentOrders() {
  const [facultyList, setFacultyList] = useState<FacultyDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  
  const [customOrderInitialFacultyId, setCustomOrderInitialFacultyId] = useState<string>("");
  const [isCustomDialogOpen, setIsCustomDialogOpen] = useState(false);

  const roles = [
    "Invigilator",
    "Lab Incharge",
    "Internal Examiner",
    "External Examiner",
    "Squad Member",
    "Chief Superintendent"
  ];

  const fetchFaculty = async () => {
    setLoading(true);
    try {
      const response = await facultyApi.getAll();
      setFacultyList(response.data);
    } catch (error) {
      toast.error("Failed to load faculty data");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFaculty();
  }, []);

  const openCustomOrderDialog = (facultyId?: string) => {
    setCustomOrderInitialFacultyId(facultyId || "");
    setIsCustomDialogOpen(true);
  };

  const filteredFaculty = facultyList.filter(
    (f) =>
      f.name.toLowerCase().includes(search.toLowerCase()) ||
      f.employeeId.toLowerCase().includes(search.toLowerCase()) ||
      f.department.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">
            Appointment Orders
          </h1>
          <p className="text-slate-500 mt-1">
            Generate and download official appointment documents for faculty members.
          </p>
        </div>
        <Button onClick={() => openCustomOrderDialog()} className="gap-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-md shadow-blue-600/20">
          <Plus size={16} />
          Create Custom Order
        </Button>
      </div>

      <div className="bg-white border rounded-2xl shadow-sm overflow-hidden mb-8">
        <div className="p-6 border-b bg-gradient-to-r from-slate-50 to-white flex items-center justify-between">
          <div className="relative w-full max-w-md flex items-center">
            <Search className="absolute left-3 text-slate-400" size={18} />
            <input
              type="text"
              placeholder="Search by name, ID, or dept..."
              className="w-full rounded-xl border border-slate-200 bg-white/50 pl-10 pr-4 py-2 text-sm text-slate-700 shadow-sm transition-all focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-blue-500/10 placeholder:text-slate-400"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="text-sm text-slate-500 font-medium bg-slate-100 px-4 py-1.5 rounded-full border border-slate-200">
            <span className="text-slate-900 font-bold">{filteredFaculty.length}</span> Available Staff
          </div>
        </div>

        <div className="overflow-x-auto max-h-[600px] custom-scrollbar">
          <Table>
            <TableHeader className="bg-white/95 backdrop-blur-sm sticky top-0 z-10 shadow-sm">
              <TableRow className="border-slate-100 hover:bg-transparent">
                <TableHead className="font-bold text-slate-700 pl-6 w-[140px]">Type</TableHead>
                <TableHead className="font-bold text-slate-700 w-[140px]">Employee ID</TableHead>
                <TableHead className="font-bold text-slate-700 min-w-[200px]">Faculty Member</TableHead>
                <TableHead className="font-bold text-slate-700">Department</TableHead>
                <TableHead className="font-bold text-slate-700">Designation</TableHead>
                <TableHead className="font-bold text-slate-700 text-right pr-6">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center gap-3 text-slate-400">
                      <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                      <p className="font-medium text-sm">Loading faculty data...</p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : filteredFaculty.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center text-slate-400 gap-3">
                      <div className="p-4 bg-slate-50 rounded-full">
                        <FileText size={32} className="text-slate-300" />
                      </div>
                      <p className="font-medium text-slate-500">No faculty members found.</p>
                      <p className="text-sm text-slate-400 max-w-sm">Try adjusting your search criteria.</p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                filteredFaculty.map((faculty) => (
                  <TableRow key={faculty.id} className="hover:bg-slate-50/80 transition-colors border-slate-100 group">
                    <TableCell className="pl-6 py-4">
                      {faculty.isInternal !== false ? (
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1.5 rounded-lg w-fit border border-emerald-200/60 shadow-sm">
                          <Building2 size={14} className="text-emerald-500" /> Internal
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-700 bg-amber-50 px-2.5 py-1.5 rounded-lg w-fit border border-amber-200/60 shadow-sm" title={faculty.collegeName}>
                          <Globe size={14} className="text-amber-500" /> External
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="font-mono text-sm font-semibold text-slate-600">
                      {faculty.employeeId}
                    </TableCell>
                    <TableCell className="font-bold text-slate-800">
                      <div>{faculty.name}</div>
                      {faculty.isInternal === false && faculty.collegeName && (
                        <div className="text-xs font-medium text-slate-500 mt-0.5">{faculty.collegeName}</div>
                      )}
                    </TableCell>
                    <TableCell>
                      <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-1 text-xs font-bold text-slate-600 ring-1 ring-inset ring-slate-500/10 uppercase tracking-widest">
                        {faculty.department}
                      </span>
                    </TableCell>
                    <TableCell className="text-slate-600 font-medium">
                      {faculty.designation || "-"}
                    </TableCell>
                    <TableCell className="text-right pr-6">
                      <Button
                        variant="default"
                        size="sm"
                        className="gap-2 bg-blue-600 hover:bg-blue-700 text-white shadow-md shadow-blue-600/20 rounded-xl px-4 transition-all opacity-90 group-hover:opacity-100"
                        onClick={() => openCustomOrderDialog(faculty.id)}
                      >
                        <FileText size={16} />
                        Generate Order
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      <CustomOrderDialog 
        open={isCustomDialogOpen} 
        onOpenChange={setIsCustomDialogOpen} 
        facultyList={facultyList}
        onFacultyCreated={fetchFaculty}
        initialFacultyId={customOrderInitialFacultyId}
      />
    </div>
  );
}
