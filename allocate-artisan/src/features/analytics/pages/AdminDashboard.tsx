import React, { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { Loader2, Plus, Building2 } from "lucide-react";

const AdminDashboard = () => {
  const [tenantId, setTenantId] = useState("");
  const [collegeName, setCollegeName] = useState("");
  const [adminEmail, setAdminEmail] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await apiClient.post("/admin/tenants", {
        tenantId,
        collegeName,
        adminEmail,
        adminPassword
      });
      toast.success("College Created Successfully", {
        description: `Tenant database for ${collegeName} has been provisioned.`,
      });
      // Clear form
      setTenantId("");
      setCollegeName("");
      setAdminEmail("");
      setAdminPassword("");
    } catch (error: any) {
      toast.error("Creation Failed", {
        description: error?.message || "Unable to provision college.",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">System Admin Dashboard</h1>
        <p className="text-slate-500 mt-2">Manage colleges and auto-provision their separate database environments.</p>
      </div>

      <Card className="shadow-lg border-slate-200">
        <CardHeader className="bg-slate-50/50 border-b border-slate-100 pb-6">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-primary/10 text-primary rounded-lg">
              <Building2 size={20} />
            </div>
            <CardTitle>Register New College</CardTitle>
          </div>
          <CardDescription>
            This action will automatically create a new isolated PostgreSQL database, run necessary migrations, and set up the college's administrator account.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleCreateTenant}>
          <CardContent className="space-y-6 pt-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="tenantId">Tenant ID <span className="text-destructive">*</span></Label>
                <Input
                  id="tenantId"
                  placeholder="e.g. collegeA, srm, vit"
                  required
                  value={tenantId}
                  onChange={(e) => setTenantId(e.target.value)}
                  className="bg-white"
                  disabled={loading}
                />
                <p className="text-[10px] text-slate-400">Must be unique. No spaces or special characters.</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="collegeName">College Name <span className="text-destructive">*</span></Label>
                <Input
                  id="collegeName"
                  placeholder="e.g. K. Ramakrishnan College of Engineering"
                  required
                  value={collegeName}
                  onChange={(e) => setCollegeName(e.target.value)}
                  className="bg-white"
                  disabled={loading}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="adminEmail">Administrator Email <span className="text-destructive">*</span></Label>
                <Input
                  id="adminEmail"
                  type="email"
                  placeholder="admin@college.edu"
                  required
                  value={adminEmail}
                  onChange={(e) => setAdminEmail(e.target.value)}
                  className="bg-white"
                  disabled={loading}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="adminPassword">Administrator Password <span className="text-destructive">*</span></Label>
                <Input
                  id="adminPassword"
                  type="password"
                  required
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  className="bg-white"
                  disabled={loading}
                />
              </div>
            </div>
          </CardContent>
          <CardFooter className="bg-slate-50/50 border-t border-slate-100 flex justify-end p-6">
            <Button type="submit" disabled={loading} className="gap-2">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {loading ? "Provisioning Environment..." : "Create College Environment"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
};

export default AdminDashboard;
