import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/ui/PageHeader";
import { Trash2, Plus, Loader2, Pencil, X, Building, Grid3X3, Layers } from "lucide-react";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export default function HallManagement() {
  const [newHallId, setNewHallId] = useState("");
  const [newHallName, setNewHallName] = useState("");
  
  const [semRows, setSemRows] = useState("5");
  const [semCols, setSemCols] = useState("5");
  
  const [internalRows, setInternalRows] = useState("7");
  const [internalCols, setInternalCols] = useState("6");

  const [editingId, setEditingId] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: halls = [], isLoading } = useQuery({
    queryKey: ["halls"],
    queryFn: async () => {
      const res = await apiClient.get("/halls");
      return res.data;
    },
  });

  const resetForm = () => {
    setNewHallId("");
    setNewHallName("");
    setSemRows("5");
    setSemCols("5");
    setInternalRows("7");
    setInternalCols("6");
    setEditingId(null);
  };

  const addMutation = useMutation({
    mutationFn: async (newHall: any) => {
      await apiClient.post("/halls", newHall);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["halls"] });
      toast.success("Hall Added Successfully");
      resetForm();
    },
    onError: () => {
      toast.error("Failed to add hall. Make sure the ID is unique.");
    }
  });

  const updateMutation = useMutation({
    mutationFn: async (updatedHall: any) => {
      await apiClient.put(`/halls/${updatedHall.id}`, updatedHall);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["halls"] });
      toast.success("Hall Updated Successfully");
      resetForm();
    },
    onError: () => {
      toast.error("Failed to update hall.");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/halls/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["halls"] });
      toast.success("Hall Deleted");
    },
    onError: () => {
      toast.error("Failed to delete hall");
    }
  });

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newHallId || !newHallName || !semRows || !semCols || !internalRows || !internalCols) return;
    
    const payload = {
      id: newHallId,
      name: newHallName,
      semRows: parseInt(semRows),
      semCols: parseInt(semCols),
      internalRows: parseInt(internalRows),
      internalCols: parseInt(internalCols)
    };

    if (editingId) {
      updateMutation.mutate(payload);
    } else {
      addMutation.mutate(payload);
    }
  };

  const renderGridVisualizer = (rows: number, cols: number) => {
    // Limit to reasonable visual size
    const displayRows = Math.min(rows, 15);
    const displayCols = Math.min(cols, 15);
    const totalCapacity = rows * cols;
    
    return (
      <div className="mt-4 p-4 bg-slate-50 border rounded-lg overflow-hidden flex flex-col items-center justify-center min-h-[160px]">
        <div className="text-xs font-bold text-slate-500 mb-3 flex items-center justify-between w-full">
          <span>Grid Preview</span>
          <span className="bg-slate-200 text-slate-700 px-2 py-0.5 rounded-full">Total Capacity: {totalCapacity} seats</span>
        </div>
        <div 
          className="grid gap-1.5 justify-center" 
          style={{ gridTemplateColumns: `repeat(${displayCols}, minmax(0, 1fr))` }}
        >
          {Array.from({ length: displayRows * displayCols }).map((_, i) => (
            <div 
              key={i} 
              className="w-4 h-4 sm:w-5 sm:h-5 md:w-6 md:h-6 rounded-sm bg-slate-300 border border-slate-400"
              title={`Seat ${i + 1}`}
            />
          ))}
        </div>
        {(rows > 15 || cols > 15) && (
          <div className="text-xs text-slate-500 mt-2 italic text-center">
            Preview limited to 15x15. Actual size: {rows}x{cols}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Hall Master"
        subtitle="Manage examination halls and configure flexible seating grid dimensions."
        icon={Building}
      />

      <div className="grid xl:grid-cols-12 gap-6">
        <div className="xl:col-span-4">
          <Card className="shadow-md border-slate-200">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-800 flex items-center">
                  <Grid3X3 className="w-5 h-5 mr-2 text-primary" />
                  {editingId ? "Edit Hall Configuration" : "New Hall Setup"}
                </h3>
                {editingId && (
                  <span className="bg-amber-100 text-amber-800 text-xs px-2 py-1 rounded font-semibold uppercase tracking-wider">
                    Editing
                  </span>
                )}
              </div>
              
              <form onSubmit={handleAdd} className="space-y-5">
                <div className="space-y-2">
                  <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Hall ID</Label>
                  <Input 
                    required 
                    placeholder="e.g. LH-601" 
                    value={newHallId} 
                    onChange={e => setNewHallId(e.target.value)} 
                    disabled={editingId !== null}
                    className="font-mono bg-slate-50"
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Display Name</Label>
                  <Input required placeholder="e.g. Lecture Hall 601" value={newHallName} onChange={e => setNewHallName(e.target.value)} />
                </div>
                
                <Tabs defaultValue="semester" className="w-full mt-4">
                  <TabsList className="grid w-full grid-cols-2 mb-4">
                    <TabsTrigger value="semester" className="text-xs font-semibold">Semester Exam</TabsTrigger>
                    <TabsTrigger value="internal" className="text-xs font-semibold">Internal Exam</TabsTrigger>
                  </TabsList>
                  
                  <TabsContent value="semester" className="space-y-4 m-0 animate-in fade-in-50">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Rows</Label>
                        <Input required type="number" min="1" max="50" value={semRows} onChange={e => setSemRows(e.target.value)} />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Columns</Label>
                        <Input required type="number" min="1" max="50" value={semCols} onChange={e => setSemCols(e.target.value)} />
                      </div>
                    </div>
                    {renderGridVisualizer(parseInt(semRows) || 0, parseInt(semCols) || 0)}
                  </TabsContent>
                  
                  <TabsContent value="internal" className="space-y-4 m-0 animate-in fade-in-50">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Rows</Label>
                        <Input required type="number" min="1" max="50" value={internalRows} onChange={e => setInternalRows(e.target.value)} />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-xs font-bold text-slate-500 uppercase tracking-wide">Columns</Label>
                        <Input required type="number" min="1" max="50" value={internalCols} onChange={e => setInternalCols(e.target.value)} />
                      </div>
                    </div>
                    {renderGridVisualizer(parseInt(internalRows) || 0, parseInt(internalCols) || 0)}
                  </TabsContent>
                </Tabs>

                <div className="pt-4 border-t border-slate-100">
                  {editingId ? (
                    <div className="flex gap-2">
                      <Button type="submit" className="flex-1 bg-slate-900 hover:bg-slate-800" disabled={updateMutation.isPending}>
                        {updateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Pencil className="w-4 h-4 mr-2" />}
                        Update Grid Profile
                      </Button>
                      <Button 
                        type="button" 
                        variant="outline"
                        size="icon"
                        onClick={resetForm}
                        title="Cancel Editing"
                      >
                        <X className="w-4 h-4 text-slate-500" />
                      </Button>
                    </div>
                  ) : (
                    <Button type="submit" className="w-full bg-slate-900 hover:bg-slate-800" disabled={addMutation.isPending}>
                      {addMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4 mr-2" />}
                      Add Configured Hall
                    </Button>
                  )}
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="xl:col-span-8">
          <Card className="h-full flex flex-col shadow-md border-slate-200">
            <div className="p-4 border-b flex items-center justify-between bg-slate-50/80 backdrop-blur-sm sticky top-0 z-20 rounded-t-lg">
              <h3 className="font-semibold text-slate-800 flex items-center">
                <Layers className="w-5 h-5 mr-2 text-slate-500" />
                Active Hall Directory
              </h3>
              <div className="text-sm text-slate-600 bg-white px-3 py-1.5 rounded-full border shadow-sm font-medium">
                Total Capacity Profile: <span className="font-bold text-slate-900 ml-1">{halls.length} Halls</span>
              </div>
            </div>
            
            <div className="flex-1 overflow-auto relative min-h-[500px]">
              {isLoading ? (
                <div className="absolute inset-0 flex items-center justify-center bg-white/50 backdrop-blur-sm z-10">
                  <Loader2 className="w-8 h-8 animate-spin text-primary" />
                </div>
              ) : (
                <Table>
                  <TableHeader className="bg-slate-100/50 sticky top-0 shadow-sm z-10">
                    <TableRow className="border-b-slate-200 hover:bg-transparent">
                      <TableHead className="font-semibold text-slate-700">Hall Details</TableHead>
                      <TableHead className="text-center font-semibold text-slate-700">Semester Grid</TableHead>
                      <TableHead className="text-center font-semibold text-slate-700">Internal Grid</TableHead>
                      <TableHead className="text-right w-[100px] pr-6 font-semibold text-slate-700">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {halls.map((hall: any) => (
                      <TableRow key={hall.id} className="group hover:bg-slate-50/80 transition-colors">
                        <TableCell>
                          <div className="font-bold text-slate-900">{hall.id}</div>
                          <div className="text-xs text-slate-500 mt-0.5">{hall.name}</div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="inline-flex items-center justify-center px-2.5 py-1 rounded-md bg-blue-50 text-blue-700 border border-blue-100 font-mono text-sm">
                            {hall.semRows || 5} × {hall.semCols || 5}
                            <span className="ml-2 pl-2 border-l border-blue-200 font-bold">
                              {(hall.semRows || 5) * (hall.semCols || 5)}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="inline-flex items-center justify-center px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-100 font-mono text-sm">
                            {hall.internalRows || 7} × {hall.internalCols || 6}
                            <span className="ml-2 pl-2 border-l border-emerald-200 font-bold">
                              {(hall.internalRows || 7) * (hall.internalCols || 6)}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell className="text-right pr-4">
                          <div className="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="text-slate-400 hover:text-primary hover:bg-primary/10 h-8 w-8"
                              onClick={() => {
                                setEditingId(hall.id);
                                setNewHallId(hall.id);
                                setNewHallName(hall.name);
                                setSemRows(String(hall.semRows || 5));
                                setSemCols(String(hall.semCols || 5));
                                setInternalRows(String(hall.internalRows || 7));
                                setInternalCols(String(hall.internalCols || 6));
                              }}
                            >
                              <Pencil className="w-4 h-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="text-slate-400 hover:text-red-600 hover:bg-red-50 h-8 w-8"
                              onClick={() => deleteMutation.mutate(hall.id)}
                              disabled={deleteMutation.isPending}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                    {halls.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} className="text-center text-slate-500 py-16">
                          <div className="flex flex-col items-center justify-center">
                            <Building className="w-12 h-12 text-slate-200 mb-4" />
                            <p className="text-base font-medium text-slate-600">No Halls Configured</p>
                            <p className="text-sm mt-1">Add a hall from the side panel to get started.</p>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
