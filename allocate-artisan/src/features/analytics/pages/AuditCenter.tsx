import { useState } from "react";
import { accreditationApi } from '@/api/accreditationApi';
import { erpApi } from '@/api/erpApi';
import { toast } from "sonner";
import { useQuery } from "@tanstack/react-query";
import { getBatches } from '@/api/allocationApi';
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { Download, Database, ShieldCheck, ArrowDownToLine, Loader2 } from "lucide-react";

export function AuditCenter() {
  const [selectedBatchId, setSelectedBatchId] = useState<string>("");
  const [isDownloading, setIsDownloading] = useState(false);

  // activeBatches filtering is now handled internally by BatchSelector if we pass statusFilter

  const handleDownloadNaacPack = async () => {
    if (!selectedBatchId) {
      toast.error("Please select a batch first.");
      return;
    }
    
    setIsDownloading(true);
    try {
      const response = await accreditationApi.downloadEvidencePack(selectedBatchId);
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `NAAC_Evidence_Pack_${selectedBatchId}.zip`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      
      toast.success("NAAC Evidence Pack downloaded successfully!");
    } catch (error) {
      console.error("Download failed", error);
      toast.error("Failed to download evidence pack.");
    } finally {
      setIsDownloading(false);
    }
  };

  const handleErpExport = async (type: "camu" | "icloudems") => {
    if (!selectedBatchId) {
      toast.error("Please select a batch first.");
      return;
    }

    try {
      const response = type === "camu" 
        ? await erpApi.exportToCamu(selectedBatchId) 
        : await erpApi.exportToICloudEms(selectedBatchId);

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${type === "camu" ? "Camu" : "iCloudEMS"}_Export_${selectedBatchId}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      toast.success(`${type === "camu" ? "Camu" : "iCloudEMS"} Export successful!`);
    } catch (error) {
      console.error("Export failed", error);
      toast.error("Failed to export data.");
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Audit Center</h1>
          <p className="text-gray-500 mt-1">One-click compliance and evidence generation</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Select Batch Context</h2>
        <BatchSelector 
          selectedBatchId={selectedBatchId} 
          onBatchSelect={setSelectedBatchId} 
          statusFilter={["ACTIVE", "COMPLETED"]}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        
        {/* NAAC Pack Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow group">
          <div className="p-6">
            <div className="w-12 h-12 rounded-lg bg-indigo-50 flex items-center justify-center mb-4 group-hover:bg-indigo-100 transition-colors">
              <ShieldCheck className="w-6 h-6 text-indigo-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900">NAAC Evidence Pack</h3>
            <p className="text-sm text-gray-500 mt-1 mb-6">
              Generates a comprehensive ZIP containing seating plans, invigilator reports, and attendance records formatted for NAAC compliance.
            </p>
            <button 
              onClick={handleDownloadNaacPack}
              disabled={isDownloading || !selectedBatchId}
              className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white py-2 px-4 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
              {isDownloading ? "Generating..." : "Download ZIP"}
            </button>
          </div>
        </div>

        {/* Camu Export Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow group">
          <div className="p-6">
            <div className="w-12 h-12 rounded-lg bg-emerald-50 flex items-center justify-center mb-4 group-hover:bg-emerald-100 transition-colors">
              <Database className="w-6 h-6 text-emerald-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900">Camu ERP Export</h3>
            <p className="text-sm text-gray-500 mt-1 mb-6">
              Exports seating allocation data into a CSV format pre-mapped for direct import into the Camu ERP system.
            </p>
            <button 
              onClick={() => handleErpExport("camu")}
              disabled={!selectedBatchId}
              className="w-full flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-700 text-white py-2 px-4 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ArrowDownToLine className="w-4 h-4" />
              Export to Camu CSV
            </button>
          </div>
        </div>

        {/* iCloudEMS Export Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow group">
          <div className="p-6">
            <div className="w-12 h-12 rounded-lg bg-blue-50 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
              <Database className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900">iCloudEMS Export</h3>
            <p className="text-sm text-gray-500 mt-1 mb-6">
              Exports seating allocation data into a CSV format compatible with iCloudEMS bulk upload requirements.
            </p>
            <button 
              onClick={() => handleErpExport("icloudems")}
              disabled={!selectedBatchId}
              className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ArrowDownToLine className="w-4 h-4" />
              Export to iCloudEMS CSV
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
