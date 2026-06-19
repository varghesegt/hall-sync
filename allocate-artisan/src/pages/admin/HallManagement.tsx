import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/ui/PageHeader";
import { Trash2, Plus, Loader2, Pencil, X, Building } from "lucide-react";
import { toast } from "sonner";
import apiClient from "@/api/axios";

export default function HallManagement() {
  const [newHallId, setNewHallId] = useState("");
  const [newHallName, setNewHallName] = useState("");
  const [newHallCapacity, setNewHallCapacity] = useState("25");
  const [newHallInternalCapacity, setNewHallInternalCapacity] = useState("40");
  const [editingId, setEditingId] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: halls = [], isLoading } = useQuery({
    queryKey: ["halls"],
    queryFn: async () => {
      const res = await apiClient.get("/halls");
      return res.data;
    },
  });

  const addMutation = useMutation({
    mutationFn: async (newHall: any) => {
      await apiClient.post("/halls", newHall);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["halls"] });
      toast.success("Hall Added Successfully");
      setNewHallId("");
      setNewHallName("");
      setNewHallCapacity("25");
      setNewHallInternalCapacity("40");
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
      setEditingId(null);
      setNewHallId("");
      setNewHallName("");
      setNewHallCapacity("25");
      setNewHallInternalCapacity("40");
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
    if (!newHallId || !newHallName || !newHallCapacity || !newHallInternalCapacity) return;
    if (editingId) {
      updateMutation.mutate({
        id: editingId,
        name: newHallName,
        capacity: parseInt(newHallCapacity),
        internalCapacity: parseInt(newHallInternalCapacity)
      });
    } else {
      addMutation.mutate({
        id: newHallId,
        name: newHallName,
        capacity: parseInt(newHallCapacity),
        internalCapacity: parseInt(newHallInternalCapacity)
      });
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Hall Master"
        subtitle="Manage examination halls, their seating capacities, and active availability for allocations."
        icon={Building}
      />

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <Card>
            <CardContent className="pt-6">
              <h3 className="text-lg font-bold mb-4">{editingId ? "Edit Hall" : "Add New Hall"}</h3>
              <form onSubmit={handleAdd} className="space-y-4">
                <div className="space-y-2">
                  <Label className="text-xs font-bold text-slate-500 uppercase">Hall ID</Label>
                  <Input 
                    required 
                    placeholder="e.g. LH-601" 
                    value={newHallId} 
                    onChange={e => setNewHallId(e.target.value)} 
                    disabled={editingId !== null}
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-bold text-slate-500 uppercase">Display Name</Label>
                  <Input required placeholder="e.g. Lecture Hall 601" value={newHallName} onChange={e => setNewHallName(e.target.value)} />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label className="text-xs font-bold text-slate-500 uppercase">Sem Capacity</Label>
                    <Input required type="number" min="1" value={newHallCapacity} onChange={e => setNewHallCapacity(e.target.value)} />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-xs font-bold text-slate-500 uppercase">Internal Cap</Label>
                    <Input required type="number" min="1" value={newHallInternalCapacity} onChange={e => setNewHallInternalCapacity(e.target.value)} />
                  </div>
                </div>

                <div className="pt-2">
                  {editingId ? (
                    <div className="flex gap-2">
                      <Button type="submit" className="flex-1 bg-emerald-600 hover:bg-emerald-700" disabled={updateMutation.isPending}>
                        {updateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Pencil className="w-4 h-4 mr-2" />}
                        Update Hall
                      </Button>
                      <Button 
                        type="button" 
                        variant="outline"
                        size="icon"
                        onClick={() => {
                          setEditingId(null);
                          setNewHallId("");
                          setNewHallName("");
                          setNewHallCapacity("25");
                          setNewHallInternalCapacity("40");
                        }}
                      >
                        <X className="w-4 h-4 text-slate-500" />
                      </Button>
                    </div>
                  ) : (
                    <Button type="submit" className="w-full" disabled={addMutation.isPending}>
                      {addMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4 mr-2" />}
                      Add Hall
                    </Button>
                  )}
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="lg:col-span-2">
          <Card className="h-full flex flex-col">
            <div className="p-4 border-b flex items-center justify-between bg-slate-50/50">
              <h3 className="font-semibold text-slate-800">Configured Halls</h3>
              <div className="text-sm text-slate-500 bg-white px-3 py-1 rounded-full border shadow-sm">
                Total: <span className="font-bold text-slate-900">{halls.length}</span>
              </div>
            </div>
            
            <div className="flex-1 overflow-auto relative min-h-[400px]">
              {isLoading ? (
                <div className="absolute inset-0 flex items-center justify-center bg-white/50">
                  <Loader2 className="w-6 h-6 animate-spin text-primary" />
                </div>
              ) : (
                <Table>
                  <TableHeader className="bg-slate-50 sticky top-0 shadow-sm z-10">
                    <TableRow>
                      <TableHead>Hall ID</TableHead>
                      <TableHead>Display Name</TableHead>
                      <TableHead className="text-center">Semester Cap</TableHead>
                      <TableHead className="text-center">Internal Cap</TableHead>
                      <TableHead className="text-right w-[100px]">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {halls.map((hall: any) => (
                      <TableRow key={hall.id}>
                        <TableCell className="font-medium text-slate-900">{hall.id}</TableCell>
                        <TableCell className="text-slate-600">{hall.name}</TableCell>
                        <TableCell className="text-center font-mono text-sm text-slate-600">{hall.capacity}</TableCell>
                        <TableCell className="text-center font-mono text-sm text-slate-600">{hall.internalCapacity || 40}</TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-slate-500 hover:text-blue-600 hover:bg-blue-50 mr-1 h-8 w-8"
                            onClick={() => {
                              setEditingId(hall.id);
                              setNewHallId(hall.id);
                              setNewHallName(hall.name);
                              setNewHallCapacity(String(hall.capacity));
                              setNewHallInternalCapacity(String(hall.internalCapacity || 40));
                            }}
                          >
                            <Pencil className="w-4 h-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-slate-500 hover:text-red-600 hover:bg-red-50 h-8 w-8"
                            onClick={() => deleteMutation.mutate(hall.id)}
                            disabled={deleteMutation.isPending}
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                    {halls.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-slate-500 py-12">
                          No halls configured for this college. Add one using the form.
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
