import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { ShieldAlert, ShieldCheck, Loader2 } from "lucide-react";

const AdminLogin = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  React.useEffect(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("coe_auth");
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiClient.post("/auth/login", {
        email: username,
        password: password
      });

      if (response.data.role !== "ROLE_SUPER_ADMIN") {
        toast.error("Access Denied", {
          description: "This console is reserved exclusively for System Administrators.",
          icon: <ShieldAlert className="w-5 h-5 text-destructive" />,
        });
        return;
      }
      
      const token = response.data.token;
      localStorage.setItem("token", token);
      localStorage.setItem("coe_auth", token);
      localStorage.setItem("user_email", username);
      localStorage.setItem("user_role", response.data.role);
      
      const activeTenant = response.data.tenantId || "krce";
      localStorage.setItem("tenant_id", activeTenant);
      localStorage.setItem("tenant", activeTenant);
      
      toast.success("Welcome, System Administrator", {
        description: `Logged in successfully to System Console.`,
        icon: <ShieldCheck className="w-5 h-5 text-indigo-500" />,
      });
      
      navigate("/admin");
    } catch (error: any) {
      localStorage.removeItem("token");
      localStorage.removeItem("coe_auth");
      localStorage.removeItem("user_email");
      localStorage.removeItem("user_role");
      toast.error("Authentication Failed", {
        description: "Please check your credentials.",
        icon: <ShieldAlert className="w-5 h-5 text-destructive" />,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden mesh-gradient p-4">
      {/* Dark violet / Indigo design elements */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 blur-[120px] rounded-full animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-violet-500/10 blur-[120px] rounded-full animate-pulse decoration-1000" />
      
      <div className="relative w-full max-w-md space-y-8 animate-in fade-in zoom-in slide-in-from-top-10 duration-1000">
        <div className="text-center space-y-4">
          <div className="mx-auto flex h-20 w-20 items-center justify-center overflow-hidden group transition-colors animate-float">
            <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500" />
          </div>
          <div>
            <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 drop-shadow-sm">
              HallSync <span className="text-indigo-600 italic">Admin</span>
            </h1>
            <p className="mt-3 text-slate-500 font-medium">
              System Administration and Provisioning Portal
            </p>
          </div>
        </div>

        <Card className="glass border-white/20 overflow-hidden shadow-2xl transition-all duration-500 hover:shadow-indigo-500/5">
          <div className="h-2 bg-gradient-to-r from-indigo-600 via-violet-400 to-slate-400 w-full" />
          <CardHeader className="space-y-1 pb-6">
            <CardTitle className="text-2xl font-bold tracking-tight">System Login</CardTitle>
            <CardDescription className="text-slate-500">
              Access the master database and colleges setup panel.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleLogin}>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="username" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">Admin User</Label>
                <div className="relative group">
                  <Input
                    id="username"
                    placeholder="hallsync@admin"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="h-12 bg-white/50 border-slate-200 focus:bg-white focus:ring-indigo-500/20 transition-all rounded-xl"
                    disabled={loading}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="password" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">Password</Label>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="h-12 bg-white/50 border-slate-200 focus:bg-white focus:ring-indigo-500/20 transition-all rounded-xl"
                  disabled={loading}
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-4 pt-2 pb-8">
              <Button 
                type="submit" 
                className="w-full h-12 text-base font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-500/20 transition-all active:scale-[0.98] rounded-xl"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                    Connecting...
                  </>
                ) : (
                  "Identify & Login"
                )}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>

      <footer className="absolute bottom-10 w-full px-8 flex flex-col md:flex-row items-center justify-between gap-4 opacity-50 hover:opacity-80 transition-opacity">
        <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">
          Hall<span className="text-slate-600">Sync ADMIN</span>
        </p>
        <div className="flex items-center gap-2 text-[10px] font-bold text-slate-500">
          <span>DEVELOPED BY</span>
          <span className="text-slate-900 font-black uppercase tracking-tighter">
            Varghese G T
          </span>
          <span className="w-1 h-1 rounded-full bg-slate-300" />
          <span className="text-slate-400 font-medium">Mechanical Engineering</span>
        </div>
      </footer>
    </div>
  );
};

export default AdminLogin;
