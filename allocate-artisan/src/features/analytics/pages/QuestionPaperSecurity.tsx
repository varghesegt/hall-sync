import { useState } from "react";
import { securityApi, QuestionPaperTracker } from '@/api/securityApi';
import { getBatches } from '@/api/allocationApi';
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ShieldCheck, ArrowRightLeft, PackageCheck, AlertTriangle } from "lucide-react";
import { format } from "date-fns";

export function QuestionPaperSecurity() {
  const queryClient = useQueryClient();
  const [selectedBatchId, setSelectedBatchId] = useState("");

  const [receiveForm, setReceiveForm] = useState({ subjectCode: "", totalReceived: 0, storedLocation: "" });

  const { data: batches = [] } = useQuery({
    queryKey: ["batches"],
    queryFn: async () => {
      const res = await getBatches();
      return res;
    }
  });

  const selectedBatch = batches.find((b: any) => b.id === selectedBatchId);
  const derivedSessionId = selectedBatch?.examSession?.id || "";

  const { data: trackers = [] } = useQuery({
    queryKey: ["qp-trackers", derivedSessionId],
    queryFn: async () => {
      if (!derivedSessionId) return [];
      const res = await securityApi.getTrackersForSession(derivedSessionId);
      return res.data;
    },
    enabled: !!derivedSessionId
  });

  const receiveMutation = useMutation({
    mutationFn: (data: any) => securityApi.receiveQP(data),
    onSuccess: () => {
      toast.success("Question papers received successfully");
      queryClient.invalidateQueries({ queryKey: ["qp-trackers", derivedSessionId] });
      setReceiveForm({ subjectCode: "", totalReceived: 0, storedLocation: "" });
    },
    onError: () => toast.error("Failed to receive question papers")
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string, status: string }) => securityApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success("Status updated successfully");
      queryClient.invalidateQueries({ queryKey: ["qp-trackers", derivedSessionId] });
    },
    onError: () => toast.error("Failed to update status")
  });

  const handleReceive = (e: React.FormEvent) => {
    e.preventDefault();
    if (!derivedSessionId) {
      toast.error("Please select a batch first");
      return;
    }
    receiveMutation.mutate({
      examSession: { id: derivedSessionId },
      subjectCode: receiveForm.subjectCode,
      totalReceived: receiveForm.totalReceived,
      storedLocation: receiveForm.storedLocation
    });
  };

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Question Paper Security</h1>
          <p className="text-gray-500 mt-1">Track custody and distribution of question papers</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">Select Exam Batch Context</label>
        <BatchSelector 
          selectedBatchId={selectedBatchId} 
          onBatchSelect={setSelectedBatchId} 
          statusFilter={["ACTIVE", "COMPLETED"]}
        />
      </div>

      {derivedSessionId && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="p-4 border-b border-gray-200 bg-gray-50">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                  <PackageCheck className="w-5 h-5 text-indigo-600" />
                  Receive New Packets
                </h2>
              </div>
              <div className="p-6">
                <form onSubmit={handleReceive} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Subject Code</label>
                    <input 
                      type="text" 
                      required
                      className="w-full rounded-md border border-gray-300 py-2 px-3 shadow-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 sm:text-sm"
                      value={receiveForm.subjectCode}
                      onChange={e => setReceiveForm(prev => ({ ...prev, subjectCode: e.target.value }))}
                      placeholder="e.g. CS8151"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Total Packets/Papers</label>
                    <input 
                      type="number" 
                      required
                      min="1"
                      className="w-full rounded-md border border-gray-300 py-2 px-3 shadow-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 sm:text-sm"
                      value={receiveForm.totalReceived || ""}
                      onChange={e => setReceiveForm(prev => ({ ...prev, totalReceived: parseInt(e.target.value) }))}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Strong Room Location</label>
                    <input 
                      type="text" 
                      required
                      className="w-full rounded-md border border-gray-300 py-2 px-3 shadow-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 sm:text-sm"
                      value={receiveForm.storedLocation}
                      onChange={e => setReceiveForm(prev => ({ ...prev, storedLocation: e.target.value }))}
                      placeholder="e.g. Rack A, Shelf 2"
                    />
                  </div>
                  <button 
                    type="submit"
                    disabled={receiveMutation.isPending}
                    className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 px-4 rounded-md font-medium transition-colors disabled:opacity-50"
                  >
                    {receiveMutation.isPending ? "Saving..." : "Log Receipt"}
                  </button>
                </form>
              </div>
            </div>
          </div>

          <div className="lg:col-span-2">
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="p-4 border-b border-gray-200 bg-gray-50">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  Custody Chain
                </h2>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Subject</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {trackers.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-6 py-8 text-center text-gray-500">No question papers logged for this session.</td>
                      </tr>
                    ) : (
                      trackers.map((t) => (
                        <tr key={t.id} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm font-medium text-gray-900">{t.subjectCode}</div>
                            <div className="text-xs text-gray-500">{t.storedLocation}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {t.totalReceived}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                              ${t.status === 'RECEIVED' ? 'bg-blue-100 text-blue-800' : 
                                t.status === 'DISTRIBUTED' ? 'bg-amber-100 text-amber-800' : 
                                t.status === 'RETURNED' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}`}>
                              {t.status}
                            </span>
                            {t.distributedTime && <div className="text-xs text-gray-400 mt-1">Out: {format(new Date(t.distributedTime), "HH:mm")}</div>}
                            {t.returnedTime && <div className="text-xs text-gray-400">In: {format(new Date(t.returnedTime), "HH:mm")}</div>}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                            {t.status === 'RECEIVED' && (
                              <button 
                                onClick={() => updateStatusMutation.mutate({ id: t.id, status: "DISTRIBUTED" })}
                                className="text-amber-600 hover:text-amber-900 flex items-center gap-1"
                              >
                                <ArrowRightLeft className="w-4 h-4" /> Distribute
                              </button>
                            )}
                            {t.status === 'DISTRIBUTED' && (
                              <button 
                                onClick={() => updateStatusMutation.mutate({ id: t.id, status: "RETURNED" })}
                                className="text-emerald-600 hover:text-emerald-900 flex items-center gap-1"
                              >
                                <ShieldCheck className="w-4 h-4" /> Return (Safe)
                              </button>
                            )}
                            {t.status === 'RETURNED' && (
                              <span className="text-gray-400 italic">Completed</span>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
