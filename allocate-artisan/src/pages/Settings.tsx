import { useState, useRef, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/api/axios";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Loader2, Save, Upload, ShieldCheck, AlertCircle } from "lucide-react";

interface TenantSettings {
  collegeName: string;
  remunerationRate: number;
  isConfigured: boolean;
  logoBase64: string;
}

export default function Settings() {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState<TenantSettings>({
    collegeName: "",
    remunerationRate: 150.0,
    isConfigured: false,
    logoBase64: "",
  });

  const { isLoading, data: currentSettings } = useQuery({
    queryKey: ["tenantSettings"],
    queryFn: async () => {
      const res = await apiClient.get<TenantSettings>("/settings");
      setFormData(res.data);
      return res.data;
    },
    staleTime: Infinity,
  });

  const mutation = useMutation({
    mutationFn: async (data: TenantSettings) => {
      // Force isConfigured to true upon saving
      const payload = { ...data, isConfigured: true };
      const res = await apiClient.put<TenantSettings>("/settings", payload);
      return res.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["tenant-settings"] });
      queryClient.setQueryData(["tenantSettings"], data);
      toast({
        title: "Settings Saved",
        description: "Your college settings have been updated successfully.",
      });
      // Force reload to update Layout if it was first time
      if (!currentSettings?.isConfigured) {
        window.location.href = "/dashboard";
      }
    },
    onError: () => {
      toast({
        variant: "destructive",
        title: "Error",
        description: "Failed to save settings. Please try again.",
      });
    },
  });

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      toast({
        variant: "destructive",
        title: "File too large",
        description: "Logo image must be less than 2MB.",
      });
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setFormData((prev) => ({ ...prev, logoBase64: reader.result as string }));
    };
    reader.readAsDataURL(file);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.logoBase64 && !currentSettings?.isConfigured) {
      toast({
        variant: "destructive",
        title: "Logo Required",
        description: "Please upload a college logo to complete setup.",
      });
      return;
    }
    mutation.mutate(formData);
  };

  if (isLoading) {
    return (
      <div className="flex h-[50vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  const isOnboarding = currentSettings && !currentSettings.isConfigured;

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in zoom-in duration-500">
      {isOnboarding && (
        <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-md shadow-sm mb-6 flex gap-3">
          <AlertCircle className="text-amber-500 mt-0.5" size={20} />
          <div>
            <h3 className="text-amber-800 font-bold">Welcome to HallSync!</h3>
            <p className="text-amber-700 text-sm mt-1">
              Please complete your college profile to activate your workspace. You must provide your institution's name and upload a logo.
            </p>
          </div>
        </div>
      )}

      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          {isOnboarding ? "Initial Setup" : "College Settings"}
        </h1>
        <p className="text-slate-500 mt-2">
          Manage your institution's global configuration, branding, and report headers.
        </p>
      </div>

      <Card className="border-t-4 border-t-primary shadow-md">
        <CardHeader>
          <CardTitle>Institution Profile</CardTitle>
          <CardDescription>
            These details will be reflected across all generated reports, master sheets, and your navigation bar.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              {/* Left Column: Form Fields */}
              <div className="space-y-6">
                <div className="space-y-2">
                  <Label htmlFor="collegeName" className="font-bold">College Full Name <span className="text-destructive">*</span></Label>
                  <Input
                    id="collegeName"
                    value={formData.collegeName}
                    onChange={(e) => setFormData({ ...formData, collegeName: e.target.value })}
                    required
                    placeholder="e.g. K. Ramakrishnan College of Engineering"
                    className="max-w-xl bg-slate-50"
                  />
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="remunerationRate" className="font-bold">Invigilation Remuneration Rate (per session) <span className="text-destructive">*</span></Label>
                  <div className="relative max-w-xs">
                    <span className="absolute left-3 top-2.5 text-slate-500 font-medium">₹</span>
                    <Input
                      id="remunerationRate"
                      type="number"
                      step="0.01"
                      min="0"
                      value={formData.remunerationRate}
                      onChange={(e) => setFormData({ ...formData, remunerationRate: parseFloat(e.target.value) || 0 })}
                      required
                      className="pl-8 bg-slate-50"
                    />
                  </div>
                  <p className="text-xs text-slate-500">
                    This amount is multiplied by the total sessions invigilated to generate the master payment sheet.
                  </p>
                </div>
              </div>

              {/* Right Column: Logo Upload */}
              <div className="space-y-2 flex flex-col items-center justify-center p-6 border-2 border-dashed border-slate-200 rounded-xl bg-slate-50">
                <Label className="font-bold mb-4">Institution Logo {isOnboarding && <span className="text-destructive">*</span>}</Label>
                
                <div className="relative h-32 w-32 rounded-full border-4 border-white shadow-lg overflow-hidden bg-white flex items-center justify-center mb-4 group cursor-pointer" onClick={() => fileInputRef.current?.click()}>
                  {formData.logoBase64 ? (
                    <img src={formData.logoBase64} alt="College Logo" className="w-full h-full object-contain p-2" />
                  ) : currentSettings?.isConfigured ? (
                    <img src="/api/v1/settings/logo" alt="College Logo" className="w-full h-full object-contain p-2" />
                  ) : (
                    <div className="text-slate-300 flex flex-col items-center">
                      <Upload size={32} />
                      <span className="text-[10px] uppercase font-bold mt-2 tracking-wider">Upload</span>
                    </div>
                  )}
                  <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <span className="text-white text-xs font-bold uppercase tracking-wider">Change</span>
                  </div>
                </div>
                
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  className="hidden" 
                  accept="image/png, image/jpeg, image/jpg" 
                  onChange={handleImageUpload} 
                />
                
                <p className="text-xs text-slate-400 text-center max-w-[200px]">
                  Recommended size: 256x256px. Formats: PNG, JPG. Max 2MB.
                </p>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100">
              <Button type="submit" disabled={mutation.isPending} className="w-full md:w-auto px-8 gap-2 h-11">
                {mutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : isOnboarding ? (
                  <ShieldCheck className="h-4 w-4" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                {isOnboarding ? "Complete Setup & Activate" : "Save Configuration"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
