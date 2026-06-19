import { useState } from "react";
import { notificationApi } from "../../api/notificationApi";
import { toast } from "sonner";
import { BatchSelector } from "../../components/BatchSelector";
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
  const queryClient = useQueryClient();

  // activeBatches filtering and fetching is handled by BatchSelector

  const { data: logs = [], isLoading: logsLoading } = useQuery<NotificationLogEntry[]>({
    queryKey: ["notification-log"],
    queryFn: async () => {
      const response = await notificationApi.getLog();
      return response.data;
    },
  });


  const dutyEmailMutation = useMutation({
    mutationFn: (batchId: string) => notificationApi.sendDutyEmails(batchId),
    onSuccess: (response) => {
      const data = response.data;
      toast.success(
        `Duty emails: ${data.sent} sent, ${data.failed} failed, ${data.skipped} skipped`
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
    dutyEmailMutation.mutate(selectedBatchId);
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
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div>
        <h1 className="text-3xl font-bold text-gray-900 tracking-tight flex items-center gap-3">
          <Mail className="text-primary" /> Communication Center
        </h1>
        <p className="text-gray-500 mt-1">
          Send duty allocations, exam notifications, and hall change alerts to
          faculty automatically.
        </p>
      </div>

      {/* Batch selector */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <label className="block text-sm font-semibold text-gray-700 mb-2">
          Select Exam Batch Context
        </label>
        <BatchSelector 
          selectedBatchId={selectedBatchId} 
          onBatchSelect={setSelectedBatchId} 
          statusFilter={["ACTIVE", "COMPLETED"]}
        />
      </div>

      {/* Action cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Duty Allocation Emails */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-50 rounded-lg">
              <Send className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">
                Send Duty Allocation Emails
              </h3>
              <p className="text-xs text-gray-500">
                Emails each faculty their hall, date, and shift details
              </p>
            </div>
          </div>
          <button
            onClick={handleSendDutyEmails}
            disabled={!selectedBatchId || dutyEmailMutation.isPending}
            className="w-full inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {dutyEmailMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
            Send Duty Emails
          </button>
        </div>

        {/* Exam Broadcast */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-violet-50 rounded-lg">
              <Megaphone className="w-5 h-5 text-violet-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">
                Exam Notification Broadcast
              </h3>
              <p className="text-xs text-gray-500">
                Send a custom message to all assigned faculty
              </p>
            </div>
          </div>
          <textarea
            className="w-full rounded-lg border border-gray-300 p-3 text-sm resize-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500"
            rows={3}
            placeholder="Type your notification message here..."
            value={broadcastMessage}
            onChange={(e) => setBroadcastMessage(e.target.value)}
          />
          <button
            onClick={handleSendBroadcast}
            disabled={
              !selectedBatchId ||
              !broadcastMessage.trim() ||
              broadcastMutation.isPending
            }
            className="w-full inline-flex items-center justify-center gap-2 rounded-lg bg-violet-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {broadcastMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Megaphone className="w-4 h-4" />
            )}
            Broadcast Message
          </button>
        </div>
      </div>

      {/* Notification Log */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-5 border-b border-gray-200 flex items-center gap-2">
          <MessageSquare className="w-5 h-5 text-gray-400" />
          <h2 className="text-lg font-semibold text-gray-900">
            Notification Log
          </h2>
          <span className="ml-auto text-xs text-gray-400">
            Last 50 notifications
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-5 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Recipient
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Subject
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Time
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {logsLoading ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-5 py-8 text-center text-gray-500"
                  >
                    <Loader2 className="w-5 h-5 animate-spin mx-auto" />
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-5 py-8 text-center text-gray-500"
                  >
                    No notifications sent yet. Select a batch and send your
                    first notification.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-5 py-3">
                      <div className="text-sm font-medium text-gray-900">
                        {log.recipientName}
                      </div>
                      <div className="text-xs text-gray-500">
                        {log.recipientEmail}
                      </div>
                    </td>
                    <td className="px-5 py-3 text-sm text-gray-700 max-w-xs truncate">
                      {log.subject}
                    </td>
                    <td className="px-5 py-3">{typeBadge(log.notificationType)}</td>
                    <td className="px-5 py-3">
                      {statusBadge(log.status)}
                      {log.errorMessage && (
                        <div className="text-xs text-rose-500 mt-1 max-w-xs truncate" title={log.errorMessage}>
                          {log.errorMessage}
                        </div>
                      )}
                    </td>
                    <td className="px-5 py-3 text-xs text-gray-500 whitespace-nowrap">
                      {log.sentAt
                        ? new Date(log.sentAt).toLocaleString()
                        : new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
