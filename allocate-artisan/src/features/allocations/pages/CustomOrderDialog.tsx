import { useState, useEffect } from "react";
import { FacultyDto, facultyApi } from "@/api/facultyApi";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Download, Plus, FileText, UserPlus, X } from "lucide-react";
import { Switch } from "@/components/ui/switch";

interface CustomOrderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  facultyList: FacultyDto[];
  onFacultyCreated: () => void;
  initialFacultyId?: string;
}

export function CustomOrderDialog({ open, onOpenChange, facultyList, onFacultyCreated, initialFacultyId }: CustomOrderDialogProps) {
  const [season, setSeason] = useState("");
  const [date, setDate] = useState("");
  const [role, setRole] = useState("Invigilator");
  const [selectedFacultyId, setSelectedFacultyId] = useState("");
  
  useEffect(() => {
    if (open) {
      setSelectedFacultyId(initialFacultyId || "");
    }
  }, [open, initialFacultyId]);
  
  const [isCreatingStaff, setIsCreatingStaff] = useState(false);
  const [isSubmittingStaff, setIsSubmittingStaff] = useState(false);
  const [newStaff, setNewStaff] = useState({
    name: "",
    employeeId: "",
    department: "",
    designation: "Assistant Professor",
    isInternal: true,
    collegeName: "",
  });

  const roles = [
    "Invigilator",
    "Lab Incharge",
    "Internal Examiner",
    "External Examiner",
    "Squad Member",
    "Chief Superintendent"
  ];

  const handleGenerateOrder = () => {
    if (!selectedFacultyId) {
      toast.error("Please select a staff member");
      return;
    }

    let url = `/api/v1/appointments/order/${selectedFacultyId}?role=${encodeURIComponent(role)}`;
    if (season) url += `&season=${encodeURIComponent(season)}`;
    if (date) url += `&date=${encodeURIComponent(date)}`;

    const link = document.createElement("a");
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    toast.success("Generating custom appointment order...");
    onOpenChange(false);
  };

  const handleCreateStaff = async () => {
    if (!newStaff.name || !newStaff.employeeId || !newStaff.department) {
      toast.error("Please fill all required fields");
      return;
    }

    setIsSubmittingStaff(true);
    try {
      const response = await facultyApi.create(newStaff);
      toast.success("Staff member created successfully");
      setSelectedFacultyId(response.data.id);
      setIsCreatingStaff(false);
      onFacultyCreated(); // Refreshes the list in parent
    } catch (error) {
      toast.error("Failed to create staff member");
    } finally {
      setIsSubmittingStaff(false);
    }
  };

  const resetForm = () => {
    setIsCreatingStaff(false);
    setNewStaff({
      name: "",
      employeeId: "",
      department: "",
      designation: "Assistant Professor",
      isInternal: true,
      collegeName: "",
    });
  };

  return (
    <Dialog open={open} onOpenChange={(val) => {
      onOpenChange(val);
      if (!val) resetForm();
    }}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold">
            <FileText className="h-5 w-5 text-blue-600" />
            Custom Appointment Order
          </DialogTitle>
        </DialogHeader>
        
        <div className="grid gap-5 py-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label className="font-semibold text-slate-700">Exam Season (Optional)</Label>
              <Input 
                placeholder="e.g. Nov/Dec 2024" 
                value={season}
                onChange={(e) => setSeason(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label className="font-semibold text-slate-700">Order Date (Optional)</Label>
              <Input 
                type="date" 
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Label className="font-semibold text-slate-700">Select Role <span className="text-red-500">*</span></Label>
            <select 
              className="flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              {roles.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2">
            <Label className="font-semibold text-slate-700">Select Staff Member <span className="text-red-500">*</span></Label>
            
            {!isCreatingStaff ? (
              <div className="flex gap-2">
                <select 
                  className="flex h-10 flex-1 items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={selectedFacultyId}
                  onChange={(e) => setSelectedFacultyId(e.target.value)}
                >
                  <option value="" disabled>-- Select Staff --</option>
                  {facultyList.map((f) => (
                    <option key={f.id} value={f.id}>{f.name} ({f.department}) - {f.employeeId}</option>
                  ))}
                </select>
                <Button variant="outline" className="gap-2 shrink-0 border-blue-200 text-blue-700 hover:bg-blue-50" onClick={() => setIsCreatingStaff(true)}>
                  <Plus size={16} /> New Staff
                </Button>
              </div>
            ) : (
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 animate-in fade-in zoom-in-95 duration-200">
                <div className="flex justify-between items-center mb-4">
                  <h4 className="font-bold flex items-center gap-2 text-slate-800">
                    <UserPlus size={18} className="text-blue-600" /> Create New Staff
                  </h4>
                  <Button variant="ghost" size="icon" className="h-6 w-6 text-slate-400" onClick={() => setIsCreatingStaff(false)}>
                    <X size={16} />
                  </Button>
                </div>
                
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs">Name <span className="text-red-500">*</span></Label>
                    <Input className="h-8 text-sm" value={newStaff.name} onChange={e => setNewStaff({...newStaff, name: e.target.value})} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs">Employee ID <span className="text-red-500">*</span></Label>
                    <Input className="h-8 text-sm" value={newStaff.employeeId} onChange={e => setNewStaff({...newStaff, employeeId: e.target.value})} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs">Department <span className="text-red-500">*</span></Label>
                    <Input className="h-8 text-sm" value={newStaff.department} onChange={e => setNewStaff({...newStaff, department: e.target.value})} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs">Designation</Label>
                    <Input className="h-8 text-sm" value={newStaff.designation} onChange={e => setNewStaff({...newStaff, designation: e.target.value})} />
                  </div>
                </div>

                <div className="flex items-center gap-4 mb-4">
                  <div className="flex items-center space-x-2">
                    <Switch 
                      checked={newStaff.isInternal} 
                      onCheckedChange={(c) => setNewStaff({...newStaff, isInternal: c})} 
                    />
                    <Label className="text-xs font-semibold">Internal Staff</Label>
                  </div>
                  
                  {!newStaff.isInternal && (
                    <div className="flex-1 flex flex-col gap-1.5 animate-in fade-in">
                      <Label className="text-xs">College Name</Label>
                      <Input className="h-8 text-sm" value={newStaff.collegeName} onChange={e => setNewStaff({...newStaff, collegeName: e.target.value})} placeholder="External College" />
                    </div>
                  )}
                </div>

                <Button className="w-full h-8 text-xs bg-slate-800 text-white hover:bg-slate-700" disabled={isSubmittingStaff} onClick={handleCreateStaff}>
                  {isSubmittingStaff ? "Saving..." : "Save Staff Member"}
                </Button>
              </div>
            )}
          </div>
        </div>
        
        <DialogFooter className="border-t pt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleGenerateOrder} className="gap-2 bg-blue-600 hover:bg-blue-700 text-white" disabled={isCreatingStaff || !selectedFacultyId}>
            <Download size={16} /> Generate Custom Order
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
