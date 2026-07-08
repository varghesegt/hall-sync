import { useState } from "react";
import { notificationApi } from '@/api/notificationApi';
import { toast } from "sonner";
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Mail,
  Send,
  Megaphone,
  Loader2,
  CheckCircle2,
  XCircle,
  Clock,
  MessageSquare,
  Users,
  Eye,
  Briefcase
} from "lucide-react";

interface NotificationLogEntry {
  id: string;
  recipientEmail: string;
  recipientName: string;
  subject: string;
  notificationType: string;
  status: string;
  errorMessage: string | null;
  sentAt: string | null;
  createdAt: string;
}

export function CommunicationCenter() {
  const [selectedBatchId, setSelectedBatchId] = useState("");
  const [broadcastMessage, setBroadcastMessage] = useState("");
  const [targetCategory, setTargetCategory] = useState("ALL");
  const [activeTab, setActiveTab] = useState<"DUTY" | "BROADCAST" | "LOGS">("DUTY");
  
  const queryClient = useQueryClient();

  const { data: logs = [], isLoading: logsLoading } = useQuery<NotificationLogEntry[]>({
    queryKey: ["notification-log"],
    queryFn: async () => {
      const response = await notificationApi.getLog();
      return response.data;
    },
  });

  const dutyEmailMutation = useMutation({
    mutationFn: ({ batchId, category }: { batchId: string, category: string }) => 
      notificationApi.sendDutyEmails(batchId, category),
    onSuccess: (response) => {
      const data = response.data;
      toast.success(
        `Emails sent: ${data.sent} sent, ${data.failed} failed, ${data.skipped} skipped`
      );
      queryClient.invalidateQueries({ queryKey: ["notification-log"] });
    },
    onError: () => toast.error("Failed to send duty allocation emails."),
  });

  const broadcastMutation = useMutation({
    mutationFn: ({ batchId, message }: { batchId: string; message: string }) =>
      notificationApi.sendExamBroadcast(batchId, message),
    onSuccess: (response) => {
      const data = response.data;
      toast.success(
        `Broadcast: ${data.sent} sent, ${data.failed} failed, ${data.skipped} skipped`
      );
      setBroadcastMessage("");
      queryClient.invalidateQueries({ queryKey: ["notification-log"] });
    },
    onError: () => toast.error("Failed to send exam broadcast."),
  });

  const handleSendDutyEmails = () => {
    if (!selectedBatchId) {
      toast.error("Please select a batch first.");
      return;
    }
    dutyEmailMutation.mutate({ batchId: selectedBatchId, category: targetCategory });
  };

  const handleSendBroadcast = () => {
    if (!selectedBatchId) {
      toast.error("Please select a batch first.");
      return;
    }
    if (!broadcastMessage.trim()) {
      toast.error("Please enter a message to broadcast.");
      return;
    }
    broadcastMutation.mutate({
      batchId: selectedBatchId,
      message: broadcastMessage,
    });
  };

  const statusIcon = (status: string) => {
    switch (status) {
      case "SENT":
        return <CheckCircle2 className="w-4 h-4 text-emerald-500" />;
      case "FAILED":
        return <XCircle className="w-4 h-4 text-rose-500" />;
      default:
        return <Clock className="w-4 h-4 text-amber-500" />;
    }
  };

  const statusBadge = (status: string) => {
    const colors: Record<string, string> = {
      SENT: "bg-emerald-50 text-emerald-700 border-emerald-200",
      FAILED: "bg-rose-50 text-rose-700 border-rose-200",
      PENDING: "bg-amber-50 text-amber-700 border-amber-200",
    };
    return (
      <span
        className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${
          colors[status] || "bg-gray-50 text-gray-700 border-gray-200"
        }`}
      >
        {statusIcon(status)}
        {status}
      </span>
    );
  };

  const typeBadge = (type: string) => {
    const colors: Record<string, string> = {
      DUTY_ALLOCATION: "bg-blue-50 text-blue-700",
      EXAM_NOTIFICATION: "bg-violet-50 text-violet-700",
      HALL_CHANGE: "bg-amber-50 text-amber-700",
    };
    const labels: Record<string, string> = {
      DUTY_ALLOCATION: "Duty",
      EXAM_NOTIFICATION: "Broadcast",
      HALL_CHANGE: "Hall Change",
    };
    return (
      <span
        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
          colors[type] || "bg-gray-50 text-gray-700"
        }`}
      >
        {labels[type] || type}
      </span>
    );
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto animate-in fade-in zoom-in-95 duration-200 pb-12">
      <div className="bg-gradient-to-r from-white to-slate-50 rounded-2xl p-8 text-slate-900 shadow-sm border border-slate-200">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <Mail className="text-indigo-600 w-8 h-8" /> Communication Center
        </h1>
        <p className="text-slate-600 mt-2 text-lg max-w-2xl">
          A fully integrated dispatch hub for duty orders, announcements, and alerts.
          Deliver precision communications to your faculty in one click.
        </p>
      </div>

      {/* Batch selector */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
        <label className="block text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wider">
          1. Select Exam Context
        </label>
        <BatchSelector 
          selectedBatchId={selectedBatchId} 
          onBatchSelect={setSelectedBatchId} 
          statusFilter={["ACTIVE", "COMPLETED"]}
        />
      </div>

      {/* Navigation Tabs */}
      <div className="flex space-x-1 bg-slate-100 p-1 rounded-xl">
        {[
          { id: "DUTY", label: "Duty Orders", icon: Briefcase },
          { id: "BROADCAST", label: "General Broadcast", icon: Megaphone },
          { id: "LOGS", label: "Delivery Logs", icon: MessageSquare },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 rounded-lg text-sm font-medium transition-all duration-200 ${
                isActive
                  ? "bg-white text-indigo-600 shadow-sm ring-1 ring-slate-200"
                  : "text-slate-600 hover:text-slate-900 hover:bg-slate-200/50"
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? "text-indigo-600" : "text-slate-400"}`} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Tab Content */}
      <div className="mt-6">
        {/* DUTY ORDERS TAB */}
        {activeTab === "DUTY" && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-6">
              <div className="flex items-center gap-3 border-b border-slate-100 pb-4">
                <div className="p-2.5 bg-indigo-50 rounded-lg">
                  <Send className="w-5 h-5 text-indigo-600" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">
                    Dispatch Duty Orders
                  </h3>
                  <p className="text-sm text-slate-500">
                    Targeted delivery of appointment orders
                  </p>
                </div>
              </div>

              <div className="space-y-3">
                <label className="block text-sm font-semibold text-slate-700">
                  Target Audience
                </label>
                <div className="grid grid-cols-1 gap-3">
                  {[
                    { id: "ALL", label: "All Assigned Staff", desc: "Send to everyone allocated in this batch" },
                    { id: "INVIGILATOR_ONLY", label: "Invigilators Only", desc: "Excludes relievers, squad, and chiefs" },
                    { id: "OTHER_DUTIES", label: "Other Duties Only", desc: "Relievers, Squad, Chief Superintendents" }
                  ].map(option => (
                    <label 
                      key={option.id}
                      className={`relative flex cursor-pointer rounded-xl border p-4 shadow-sm focus:outline-none transition-all ${
                        targetCategory === option.id 
                          ? "border-indigo-600 ring-1 ring-indigo-600 bg-indigo-50/50" 
                          : "border-slate-300 hover:border-slate-400 bg-white"
                      }`}
                    >
                      <input 
                        type="radio" 
                        name="targetCategory" 
                        value={option.id} 
                        checked={targetCategory === option.id}
                        onChange={(e) => setTargetCategory(e.target.value)}
                        className="sr-only"
                      />
                      <span className="flex flex-1">
                        <span className="flex flex-col">
                          <span className={`block text-sm font-medium ${targetCategory === option.id ? "text-indigo-900" : "text-slate-900"}`}>
                            {option.label}
                          </span>
                          <span className={`mt-1 flex items-center text-xs ${targetCategory === option.id ? "text-indigo-700" : "text-slate-500"}`}>
                            {option.desc}
                          </span>
                        </span>
                      </span>
                      <CheckCircle2 className={`h-5 w-5 ${targetCategory === option.id ? "text-indigo-600" : "text-transparent"}`} />
                    </label>
                  ))}
                </div>
              </div>

              <button
                onClick={handleSendDutyEmails}
                disabled={!selectedBatchId || dutyEmailMutation.isPending}
                className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-3.5 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-md hover:shadow-lg"
              >
                {dutyEmailMutation.isPending ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <Send className="w-5 h-5" />
                )}
                Confirm & Dispatch Emails
              </button>
            </div>

            {/* Email Preview Area */}
            <div className="bg-slate-50 rounded-2xl border border-slate-200 p-6 flex flex-col">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wider flex items-center gap-2">
                  <Eye className="w-4 h-4" /> Live Preview
                </h3>
                <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-1 rounded font-medium">Template</span>
              </div>
              <div className="flex-1 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
                <div className="border-b border-slate-100 bg-slate-50 px-4 py-3 text-sm">
                  <div className="flex mb-1"><span className="text-slate-500 w-16">From:</span> <span className="font-medium text-slate-900">noreply@hallsync.krce.ac.in</span></div>
                  <div className="flex mb-1"><span className="text-slate-500 w-16">To:</span> <span className="text-slate-600 font-mono text-xs mt-0.5">{"{"}Faculty_Email{"}"}</span></div>
                  <div className="flex"><span className="text-slate-500 w-16">Subject:</span> <span className="font-medium text-slate-900">HallSync — Duty Allocation: {"{"}Date{"}"}</span></div>
                </div>
                <div className="p-6 text-slate-700 text-sm font-sans leading-relaxed flex-1">
                  <p className="mb-4 font-semibold text-slate-900">Dear {"{"}Faculty_Name{"}"},</p>
                  <p className="mb-4">You have been assigned invigilation duty for the upcoming examination.</p>
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-100 mb-4 font-mono text-xs space-y-2">
                    <div className="flex"><span className="w-20 text-slate-500">Date:</span><span className="font-medium text-slate-900">{"{"}Date{"}"}</span></div>
                    <div className="flex"><span className="w-20 text-slate-500">Shift:</span><span className="font-medium text-slate-900">{"{"}Shift{"}"}</span></div>
                    <div className="flex"><span className="w-20 text-slate-500">Hall:</span><span className="font-medium text-slate-900">{"{"}Hall_Name{"}"}</span></div>
                    <div className="flex"><span className="w-20 text-slate-500">Role:</span><span className="font-medium text-indigo-600 bg-indigo-50 px-1 rounded">{targetCategory === "INVIGILATOR_ONLY" ? "INVIGILATOR" : targetCategory === "OTHER_DUTIES" ? "RELIEVER / SQUAD" : "{Duty_Type}"}</span></div>
                  </div>
                  <p className="mb-4 text-rose-600 font-medium">Please report to the exam hall 15 minutes before the exam begins.</p>
                  <p className="text-slate-500 text-xs">Regards,<br/>HallSync Exam Management System</p>
                </div>
              </div>
            </div>

          </div>
        )}

        {/* BROADCAST TAB */}
        {activeTab === "BROADCAST" && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 lg:w-2/3 mx-auto">
            <div className="flex items-center gap-3 border-b border-slate-100 pb-4 mb-6">
              <div className="p-2.5 bg-violet-50 rounded-lg">
                <Megaphone className="w-5 h-5 text-violet-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-900">
                  Global Exam Broadcast
                </h3>
                <p className="text-sm text-slate-500">
                  Instantly alert all assigned faculty with custom announcements
                </p>
              </div>
            </div>
            
            <div className="space-y-4">
              <label className="block text-sm font-medium text-slate-700">Message Content</label>
              <textarea
                className="w-full rounded-xl border border-slate-300 p-4 text-sm resize-none focus:border-violet-500 focus:ring-2 focus:ring-violet-200 transition-all outline-none min-h-[160px]"
                placeholder="Type your urgent notification, hall change alerts, or general instructions here..."
                value={broadcastMessage}
                onChange={(e) => setBroadcastMessage(e.target.value)}
              />
              
              <div className="bg-amber-50 text-amber-800 text-sm p-4 rounded-lg border border-amber-200 flex items-start gap-3">
                <Users className="w-5 h-5 mt-0.5 shrink-0" />
                <p>This message will be delivered to <strong>every faculty member</strong> associated with the selected exam batch.</p>
              </div>

              <button
                onClick={handleSendBroadcast}
                disabled={
                  !selectedBatchId ||
                  !broadcastMessage.trim() ||
                  broadcastMutation.isPending
                }
                className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-violet-600 px-4 py-3.5 text-sm font-medium text-white hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-md hover:shadow-lg mt-4"
              >
                {broadcastMutation.isPending ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <Megaphone className="w-5 h-5" />
                )}
                Transmit Broadcast
              </button>
            </div>
          </div>
        )}

        {/* LOGS TAB */}
        {activeTab === "LOGS" && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-slate-900">
                  Delivery Trajectory
                </h2>
                <p className="text-sm text-slate-500 mt-1">Real-time status of the last 50 outgoing communications.</p>
              </div>
              <button onClick={() => queryClient.invalidateQueries({ queryKey: ["notification-log"] })} className="text-sm text-indigo-600 font-medium hover:text-indigo-800 bg-indigo-50 px-3 py-1.5 rounded-lg">
                Refresh Logs
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Recipient
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Type & Subject
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Delivery Status
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Timestamp
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {logsLoading ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center text-slate-500">
                        <Loader2 className="w-6 h-6 animate-spin mx-auto text-indigo-500" />
                        <p className="mt-2 text-sm">Loading transmission logs...</p>
                      </td>
                    </tr>
                  ) : logs.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center text-slate-500">
                        <div className="bg-slate-50 rounded-full w-12 h-12 flex items-center justify-center mx-auto mb-3">
                          <MessageSquare className="w-6 h-6 text-slate-400" />
                        </div>
                        <p className="text-sm">No communications dispatched yet.</p>
                      </td>
                    </tr>
                  ) : (
                    logs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50/50 transition-colors">
                        <td className="px-6 py-4">
                          <div className="text-sm font-semibold text-slate-900">
                            {log.recipientName}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            {log.recipientEmail}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="mb-1.5">{typeBadge(log.notificationType)}</div>
                          <div className="text-sm text-slate-700 max-w-sm truncate" title={log.subject}>
                            {log.subject}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          {statusBadge(log.status)}
                          {log.errorMessage && (
                            <div className="text-xs text-rose-600 mt-1.5 max-w-xs truncate bg-rose-50 px-2 py-1 rounded" title={log.errorMessage}>
                              {log.errorMessage}
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right text-sm text-slate-500 whitespace-nowrap tabular-nums">
                          {log.sentAt
                            ? new Date(log.sentAt).toLocaleString(undefined, {
                                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                              })
                            : new Date(log.createdAt).toLocaleString(undefined, {
                                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                              })}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
