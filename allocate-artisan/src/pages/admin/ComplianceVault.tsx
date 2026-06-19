import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { archiveApi } from "../../api/archiveApi";
import { accreditationApi } from "../../api/accreditationApi";
import { PageHeader } from "../../components/ui/PageHeader";
import { Card, CardContent } from "../../components/ui/card";
import { Download, ShieldCheck, CheckCircle2, XCircle } from "lucide-react";
import { toast } from "sonner";

export default function ComplianceVault() {
  const [isExporting, setIsExporting] = useState<string | null>(null);

  const { data: archives = [], isLoading } = useQuery({
    queryKey: ["exam-archives"],
    queryFn: () => archiveApi.getAll().then((res) => res.data),
  });

  const handleExportEvidence = async (archiveId: string) => {
    try {
      setIsExporting(archiveId);
      const res = await accreditationApi.downloadEvidencePack(archiveId);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `NAAC_Evidence_Pack_${archiveId}.zip`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success("Evidence Pack exported successfully");
    } catch (error) {
      toast.error("Failed to export evidence pack");
    } finally {
      setIsExporting(null);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <PageHeader
        title="Compliance Vault"
        subtitle="Permanent, immutable archive of all conducted examinations for NAAC/NBA accreditation."
        icon={ShieldCheck}
      />

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b text-sm font-medium text-slate-600">
                  <th className="p-4">Exam Date</th>
                  <th className="p-4">Session</th>
                  <th className="p-4">Type</th>
                  <th className="p-4">Students</th>
                  <th className="p-4">Halls</th>
                  <th className="p-4">Malpractices</th>
                  <th className="p-4">Snapshots (Seat/Duty/Att)</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y text-sm">
                {isLoading ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-slate-500">
                      Loading compliance vault...
                    </td>
                  </tr>
                ) : archives.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-slate-500">
                      No archived exams found in the vault.
                    </td>
                  </tr>
                ) : (
                  archives.map((archive) => (
                    <tr key={archive.id} className="hover:bg-slate-50 transition-colors">
                      <td className="p-4 font-medium">{archive.examDate}</td>
                      <td className="p-4">{archive.session}</td>
                      <td className="p-4">
                        <span className="px-2 py-1 bg-slate-100 text-slate-700 rounded-md text-xs font-medium border">
                          {archive.examType}
                        </span>
                      </td>
                      <td className="p-4">{archive.totalStudents}</td>
                      <td className="p-4">{archive.totalHalls}</td>
                      <td className="p-4">
                        {archive.totalMalpractice > 0 ? (
                          <span className="text-red-600 font-semibold">{archive.totalMalpractice}</span>
                        ) : (
                          <span className="text-emerald-600 font-medium">None</span>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex gap-2">
                          {archive.hasSeatingPlan ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <XCircle className="w-4 h-4 text-red-400" />}
                          {archive.hasDutySheet ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <XCircle className="w-4 h-4 text-red-400" />}
                          {archive.hasAttendance ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <XCircle className="w-4 h-4 text-red-400" />}
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => handleExportEvidence(archive.id)}
                          disabled={isExporting === archive.id}
                          className="inline-flex items-center gap-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors text-xs font-medium disabled:opacity-50"
                        >
                          <Download className="w-3.5 h-3.5" />
                          {isExporting === archive.id ? "Exporting..." : "Evidence Pack"}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
