import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { ShieldAlert, ShieldCheck, Loader2 } from "lucide-react";

const Login = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [tenantId, setTenantId] = useState("");
  const [tenants, setTenants] = useState<{tenantId: string, collegeName: string}[]>([]);
  const [loading, setLoading] = useState(false);

  React.useEffect(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("coe_auth");
    apiClient.get("/auth/tenants")
      .then(res => {
        setTenants(res.data);
        if (res.data.length > 0) setTenantId(res.data[0].tenantId);
      })
      .catch(err => console.error("Failed to load colleges", err));
  }, []);

  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiClient.post("/auth/login", {
        email: username.trim(),
        password: password.trim(),
        tenantId: tenantId
      });

      if (response.data.role === "ROLE_SUPER_ADMIN") {
        toast.error("Portal Restrained", {
          description: "System Administrators must use the dedicated Admin Console.",
          icon: <ShieldAlert className="w-5 h-5 text-destructive" />,
        });
        return;
      }
      
      // Store JWT token from response under both standard keys
      localStorage.setItem("token", response.data.token);
      localStorage.setItem("coe_auth", response.data.token);
      localStorage.setItem("user_email", username);
      localStorage.setItem("user_role", response.data.role);
      
      const activeTenant = response.data.tenantId || tenantId || "krce";
      localStorage.setItem("tenant_id", activeTenant);
      localStorage.setItem("tenant", activeTenant);
      
      toast.success("Login Successful", {
        description: `Welcome back, Controller of Examinations.`,
        icon: <ShieldCheck className="w-5 h-5 text-green-500" />,
      });
      
      navigate("/dashboard");
    } catch (error: any) {
      localStorage.removeItem("token");
      localStorage.removeItem("coe_auth");
      localStorage.removeItem("user_email");
      localStorage.removeItem("user_role");
      toast.error("Authentication Failed", {
        description: "Please check your Username and Password.",
        icon: <ShieldAlert className="w-5 h-5 text-destructive" />,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden mesh-gradient p-4">
      {/* Decorative background elements for AI feel */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 blur-[120px] rounded-full animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full animate-pulse decoration-1000" />
      
      <div className="relative w-full max-w-md space-y-8 animate-in fade-in zoom-in slide-in-from-top-10 duration-1000">
        <div className="text-center space-y-4">
          <div className="mx-auto flex h-20 w-20 items-center justify-center overflow-hidden group transition-colors animate-float">
            <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500" />
          </div>
          <div>
            <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 drop-shadow-sm">
              HallSync <span className="text-primary italic">Portal</span>
            </h1>
            <p className="mt-3 text-slate-500 font-medium">
              Secure Intelligence for Examination Control
            </p>
          </div>
        </div>

        <Card className="glass border-white/20 overflow-hidden shadow-2xl transition-all duration-500 hover:shadow-primary/5">
          <div className="h-2 bg-gradient-to-r from-primary via-indigo-400 to-emerald-400 w-full" />
          <CardHeader className="space-y-1 pb-6">
            <CardTitle className="text-2xl font-bold tracking-tight">Authentication</CardTitle>
            <CardDescription className="text-slate-500">
              Access the mission-critical seat allocation engine.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleLogin}>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="tenant" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">College</Label>
                <div className="relative group">
                  <select
                    id="tenant"
                    value={tenantId}
                    onChange={(e) => setTenantId(e.target.value)}
                    className="flex h-11 w-full rounded-md border border-slate-200 bg-white/50 px-3 py-2 text-sm ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-300"
                    required
                  >
                    {tenants.map(t => (
                      <option key={t.tenantId} value={t.tenantId}>{t.collegeName}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="username" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">Username</Label>
                <div className="relative group">
                  <Input
                    id="username"
                    placeholder="hallsync@admin"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="h-12 bg-white/50 border-slate-200 focus:bg-white focus:ring-primary/20 transition-all rounded-xl"
                    disabled={loading}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between ml-1">
                  <Label htmlFor="password" className="text-xs font-bold uppercase tracking-widest text-slate-400">Password</Label>
                  <Button variant="link" className="p-0 h-auto text-xs text-primary" onClick={() => navigate("/forgot-password")} type="button">Forgot Password?</Button>
                </div>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="pl-4 h-11 bg-white/50 border-slate-200 focus-visible:ring-primary focus-visible:border-primary transition-all duration-300 rounded-xl"
                  disabled={loading}
                />
              </div>
            </CardContent>
            <CardFooter className="pt-2 pb-8 flex flex-col space-y-4">
              <Button 
                type="submit" 
                className="w-full h-12 text-base font-bold bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all active:scale-[0.98] rounded-xl"
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
          Hall<span className="text-slate-600">Sync PORTAL</span>
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

export default Login;
