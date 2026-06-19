import { useState } from "react";
import { accreditationApi } from "@/api/accreditationApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Download, FileText, CheckCircle, ShieldCheck } from "lucide-react";

export default function AccreditationReports() {
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [loadingType, setLoadingType] = useState<string | null>(null);

  const handleDownload = async (type: "naac" | "nba" | "audit") => {
    setLoadingType(type);
    try {
      let response;
      if (type === "naac") {
        response = await accreditationApi.getNaacReport(fromDate || undefined, toDate || undefined);
      } else if (type === "nba") {
        response = await accreditationApi.getNbaReport(fromDate || undefined, toDate || undefined);
      } else {
        response = await accreditationApi.getAuditReport(fromDate || undefined, toDate || undefined);
      }

      const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${type}_report_${new Date().toISOString().split("T")[0]}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.success(`${type.toUpperCase()} Report downloaded successfully.`);
    } catch (error) {
      toast.error(`Failed to generate ${type.toUpperCase()} report`);
    } finally {
      setLoadingType(null);
    }
  };

  return (
    <div className="container mx-auto py-8 px-4 max-w-5xl animate-in fade-in duration-500">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <ShieldCheck className="text-primary" /> Accreditation & Audit Reports
        </h1>
        <p className="text-slate-500 mt-1">
          Generate officially formatted reports for NAAC, NBA, and internal quality audits.
        </p>
      </div>

      <div className="bg-white p-6 rounded-xl border shadow-sm mb-8">
        <h3 className="text-lg font-semibold mb-4">Date Range Filter</h3>
        <div className="flex flex-col md:flex-row gap-6">
          <div className="flex-1 space-y-2">
            <Label>From Date</Label>
            <Input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>
          <div className="flex-1 space-y-2">
            <Label>To Date</Label>
            <Input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>
        </div>
        <p className="text-xs text-slate-500 mt-4 italic">
          If dates are left blank, reports will default to the last academic year (NAAC/NBA) or last quarter (Audit).
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* NAAC Report Card */}
        <Card className="shadow-sm border-slate-200 hover:border-blue-200 transition-colors">
          <CardHeader>
            <CardTitle className="text-xl flex items-center gap-2">
              <CheckCircle className="text-blue-500" size={20} /> NAAC Report
            </CardTitle>
            <CardDescription>
              Comprehensive summary of examinations conducted, student appearances, and malpractice records for NAAC criteria.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              className="w-full gap-2 bg-blue-600 hover:bg-blue-700"
              onClick={() => handleDownload("naac")}
              disabled={loadingType !== null}
            >
              <Download size={16} />
              {loadingType === "naac" ? "Generating..." : "Download NAAC PDF"}
            </Button>
          </CardContent>
        </Card>

        {/* NBA Report Card */}
        <Card className="shadow-sm border-slate-200 hover:border-green-200 transition-colors">
          <CardHeader>
            <CardTitle className="text-xl flex items-center gap-2">
              <FileText className="text-green-500" size={20} /> NBA Report
            </CardTitle>
            <CardDescription>
              Focused examination statistics emphasizing invigilation ratios, program-level compliance, and assessment metrics.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              className="w-full gap-2 bg-green-600 hover:bg-green-700"
              onClick={() => handleDownload("nba")}
              disabled={loadingType !== null}
            >
              <Download size={16} />
              {loadingType === "nba" ? "Generating..." : "Download NBA PDF"}
            </Button>
          </CardContent>
        </Card>

        {/* Internal Audit Report Card */}
        <Card className="shadow-sm border-slate-200 hover:border-amber-200 transition-colors">
          <CardHeader>
            <CardTitle className="text-xl flex items-center gap-2">
              <ShieldCheck className="text-amber-500" size={20} /> Internal Audit
            </CardTitle>
            <CardDescription>
              Detailed logging of all exam archiving activities, digital footprints, and specific malpractice interventions.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              className="w-full gap-2 bg-amber-600 hover:bg-amber-700"
              onClick={() => handleDownload("audit")}
              disabled={loadingType !== null}
            >
              <Download size={16} />
              {loadingType === "audit" ? "Generating..." : "Download Audit PDF"}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
