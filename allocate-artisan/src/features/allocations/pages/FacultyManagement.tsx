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
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Upload, Download, Plus, Search, Trash2, Edit, Building2, Globe } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";

export default function FacultyManagement() {
  const [facultyList, setFacultyList] = useState<FacultyDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Form State
  const [formData, setFormData] = useState<Partial<FacultyDto>>({
    name: "",
    employeeId: "",
    department: "",
    designation: "",
    phone: "",
    email: "",
    isInternal: true,
    isAvailable: true,
    collegeName: "",
  });

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

  const handleUpload = async () => {
    if (!selectedFile) {
      toast.error("Please select a file to upload");
      return;
    }
    try {
      const response = await facultyApi.bulkUpload(selectedFile);
      toast.success(`Successfully imported ${response.data.imported} faculty.`);
      if (response.data.skipped > 0) {
        const details = response.data.skippedDetails || [];
        const preview = details.slice(0, 3).join(", ");
        const more = details.length > 3 ? "..." : "";
        toast.warning(`Skipped ${response.data.skipped} entries: ${preview}${more}`);
      }
      setIsUploadOpen(false);
      setSelectedFile(null);
      fetchFaculty();
    } catch (error) {
      toast.error("Failed to upload faculty data");
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await facultyApi.downloadTemplate();
      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "faculty_template.xlsx");
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      toast.error("Failed to download template");
    }
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingId) {
        await facultyApi.update(editingId, formData);
        toast.success("Faculty member updated successfully");
      } else {
        await facultyApi.create(formData);
        toast.success("Faculty member added successfully");
      }
      setIsAddOpen(false);
      setEditingId(null);
      setFormData({
        name: "",
        employeeId: "",
        department: "",
        designation: "",
        phone: "",
        email: "",
        isInternal: true,
        isAvailable: true,
        collegeName: "",
      });
      fetchFaculty();
    } catch (error) {
      toast.error("Failed to add faculty member");
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to deactivate this faculty member?")) return;
    try {
      await facultyApi.delete(id);
      toast.success("Faculty member deactivated");
      fetchFaculty();
    } catch (error) {
      toast.error("Failed to deactivate faculty member");
    }
  };

  const handleDeleteAll = async () => {
    if (facultyList.length === 0) {
      toast.info("No faculty members to delete.");
      return;
    }
    if (!confirm(`Are you sure you want to delete ALL ${facultyList.length} faculty members?`)) return;
    try {
      await facultyApi.deleteAll();
      toast.success("All faculty members deleted successfully");
      fetchFaculty();
    } catch (error) {
      toast.error("Failed to delete all faculty members");
    }
  };

  const handleToggleAvailability = async (id: string, currentStatus: boolean | undefined) => {
    try {
      const isAvailable = currentStatus === undefined ? false : !currentStatus;
      await facultyApi.update(id, { isAvailable });
      toast.success(`Faculty marked as ${isAvailable ? "Available" : "Unavailable"}`);
      fetchFaculty();
    } catch (error) {
      toast.error("Failed to update availability");
    }
  };

  const handleEdit = (faculty: FacultyDto) => {
    setFormData({
      name: faculty.name,
      employeeId: faculty.employeeId,
      department: faculty.department,
      designation: faculty.designation || "",
      phone: faculty.phone || "",
      email: faculty.email || "",
      isInternal: faculty.isInternal !== false,
      isAvailable: faculty.isAvailable !== false,
      collegeName: faculty.collegeName || "",
    });
    setEditingId(faculty.id);
    setIsAddOpen(true);
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
            Faculty Management
          </h1>
          <p className="text-sm font-medium text-slate-500 mt-1">
            Manage invigilators, add new faculty, or bulk import from Excel.
          </p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <Dialog open={isUploadOpen} onOpenChange={setIsUploadOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" className="gap-2">
                <Upload size={16} /> Bulk Import
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Import Faculty from Excel</DialogTitle>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <Button
                  variant="outline"
                  onClick={handleDownloadTemplate}
                  className="w-full gap-2"
                >
                  <Download size={16} /> Download Template
                </Button>
                <div className="flex flex-col gap-2 mt-4">
                  <Label>Upload Filled Template</Label>
                  <Input
                    type="file"
                    accept=".xlsx, .xls"
                    onChange={(e) =>
                      setSelectedFile(e.target.files ? e.target.files[0] : null)
                    }
                  />
                </div>
                <Button onClick={handleUpload} disabled={!selectedFile} className="mt-2">
                  Import Data
                </Button>
              </div>
            </DialogContent>
          </Dialog>

          <Button
            variant="outline"
            className="gap-2 text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700 shadow-sm"
            onClick={handleDeleteAll}
            disabled={facultyList.length === 0}
          >
            <Trash2 size={16} /> Delete All Staffs
          </Button>

          <Dialog open={isAddOpen} onOpenChange={(open) => {
              setIsAddOpen(open);
              if (!open) {
                setEditingId(null);
                setFormData({ name: "", employeeId: "", department: "", designation: "", phone: "", email: "", isInternal: true, isAvailable: true, collegeName: "" });
              }
            }}>
            <DialogTrigger asChild>
              <Button className="gap-2 bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm" onClick={() => {
                setEditingId(null);
                setFormData({ name: "", employeeId: "", department: "", designation: "", phone: "", email: "", isInternal: true, isAvailable: true, collegeName: "" });
              }}>
                <Plus size={16} /> Add Faculty
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{editingId ? "Edit Faculty Member" : "Add New Faculty Member"}</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleAddSubmit} className="grid gap-4 py-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <Label>Full Name *</Label>
                    <Input
                      required
                      value={formData.name}
                      onChange={(e) =>
                        setFormData({ ...formData, name: e.target.value })
                      }
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Employee ID *</Label>
                    <Input
                      required
                      value={formData.employeeId}
                      onChange={(e) =>
                        setFormData({ ...formData, employeeId: e.target.value })
                      }
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <Label>Department *</Label>
                    <Input
                      required
                      value={formData.department}
                      onChange={(e) =>
                        setFormData({ ...formData, department: e.target.value })
                      }
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Designation</Label>
                    <Input
                      value={formData.designation}
                      onChange={(e) =>
                        setFormData({ ...formData, designation: e.target.value })
                      }
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <Label>Email</Label>
                    <Input
                      type="email"
                      value={formData.email}
                      onChange={(e) =>
                        setFormData({ ...formData, email: e.target.value })
                      }
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Phone</Label>
                    <Input
                      value={formData.phone}
                      onChange={(e) =>
                        setFormData({ ...formData, phone: e.target.value })
                      }
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4 items-center mt-2 border-t pt-4">
                  <div className="flex flex-col gap-3">
                    <Label>Faculty Type</Label>
                    <div className="flex items-center gap-2">
                      <Switch 
                        checked={formData.isInternal !== false} 
                        onCheckedChange={(checked) => setFormData({ ...formData, isInternal: checked })} 
                      />
                      <span className="text-sm font-medium">{formData.isInternal !== false ? "Internal (Tenant College)" : "External Faculty"}</span>
                    </div>
                  </div>
                  
                  {formData.isInternal === false && (
                    <div className="flex flex-col gap-2">
                      <Label>External College Name *</Label>
                      <Input
                        required={formData.isInternal === false}
                        placeholder="E.g., NIT Trichy"
                        value={formData.collegeName || ""}
                        onChange={(e) =>
                          setFormData({ ...formData, collegeName: e.target.value })
                        }
                      />
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2 mt-2">
                   <Switch 
                     checked={formData.isAvailable !== false} 
                     onCheckedChange={(checked) => setFormData({ ...formData, isAvailable: checked })} 
                   />
                   <Label className="font-medium">Currently Available for Exam Duties</Label>
                </div>
                <Button type="submit" className="mt-4 bg-indigo-600 hover:bg-indigo-700 text-white">
                  Save Faculty
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200 bg-white flex items-center justify-between">
          <div className="relative w-full max-w-sm flex items-center">
            <Search className="absolute left-3 text-slate-400" size={16} />
            <Input
              placeholder="Search by name, ID, or dept..."
              className="pl-10 h-9 bg-slate-50 border-slate-200 focus-visible:ring-indigo-500"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="text-xs font-bold text-slate-500 tracking-wider uppercase">
            Total Active: <span className="text-slate-900">{facultyList.length}</span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>Type</TableHead>
                <TableHead>Employee ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Department</TableHead>
                <TableHead>Designation</TableHead>
                <TableHead>Contact</TableHead>
                <TableHead>Availability</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-8 text-slate-500">
                    Loading faculty data...
                  </TableCell>
                </TableRow>
              ) : filteredFaculty.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-8 text-slate-500">
                    No faculty members found.
                  </TableCell>
                </TableRow>
              ) : (
                filteredFaculty.map((faculty) => (
                  <TableRow key={faculty.id} className={faculty.isAvailable === false ? "opacity-60 bg-slate-50/50" : ""}>
                    <TableCell>
                      {faculty.isInternal !== false ? (
                        <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-600 bg-emerald-50 px-2 py-1 rounded-md w-fit">
                          <Building2 size={14} /> Internal
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 text-xs font-medium text-amber-600 bg-amber-50 px-2 py-1 rounded-md w-fit" title={faculty.collegeName}>
                          <Globe size={14} /> External
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="font-mono text-sm font-medium">
                      {faculty.employeeId}
                    </TableCell>
                    <TableCell className="font-medium text-slate-900">
                      <div>{faculty.name}</div>
                      {faculty.isInternal === false && faculty.collegeName && (
                        <div className="text-xs text-slate-500 font-normal">{faculty.collegeName}</div>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{faculty.department}</Badge>
                    </TableCell>
                    <TableCell className="text-slate-500">
                      {faculty.designation || "-"}
                    </TableCell>
                    <TableCell>
                      <div className="text-sm text-slate-600">
                        {faculty.email && <div>{faculty.email}</div>}
                        {faculty.phone && <div>{faculty.phone}</div>}
                        {!faculty.email && !faculty.phone && "-"}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Switch 
                          checked={faculty.isAvailable !== false} 
                          onCheckedChange={() => handleToggleAvailability(faculty.id, faculty.isAvailable)} 
                        />
                        <span className="text-xs font-medium text-slate-600">{faculty.isAvailable !== false ? "Available" : "Unavailable"}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-indigo-600 hover:bg-indigo-50 hover:text-indigo-700 h-8 w-8"
                          onClick={() => handleEdit(faculty)}
                        >
                          <Edit size={16} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-rose-600 hover:bg-rose-50 hover:text-rose-700 h-8 w-8"
                          onClick={() => handleDelete(faculty.id)}
                        >
                          <Trash2 size={16} />
                        </Button>
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
