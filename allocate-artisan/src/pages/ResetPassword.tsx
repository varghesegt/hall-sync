import React, { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { KeyRound, Loader2 } from "lucide-react";

const ResetPassword = () => {
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      toast.error("Passwords do not match");
      return;
    }
    if (!token) {
      toast.error("Invalid or missing reset token");
      return;
    }

    setLoading(true);
    try {
      await apiClient.post("/auth/reset-password", { token, newPassword: password });
      toast.success("Password Reset Successful", {
        description: "You can now log in with your new password.",
      });
      navigate("/login");
    } catch (error: any) {
      toast.error("Reset Failed", {
        description: error?.response?.data || "Unable to reset password. Token may be expired.",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden mesh-gradient p-4">
      <div className="relative w-full max-w-md space-y-8 animate-in fade-in zoom-in slide-in-from-top-10 duration-1000">
        <Card className="glass border-white/20 overflow-hidden shadow-2xl transition-all duration-500">
          <div className="h-2 bg-gradient-to-r from-primary via-indigo-400 to-emerald-400 w-full" />
          <CardHeader className="space-y-1 pb-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-4">
              <KeyRound size={32} />
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight">Set New Password</CardTitle>
            <CardDescription className="text-slate-500">
              Please enter your new password below.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleResetPassword}>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="password" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">New Password</Label>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="h-12 bg-white/50 border-slate-200 focus:bg-white focus:ring-primary/20 transition-all rounded-xl"
                  disabled={loading}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">Confirm Password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="h-12 bg-white/50 border-slate-200 focus:bg-white focus:ring-primary/20 transition-all rounded-xl"
                  disabled={loading}
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-4 pt-2 pb-8">
              <Button 
                type="submit" 
                className="w-full h-12 text-base font-bold bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all active:scale-[0.98] rounded-xl"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                    Resetting Password...
                  </>
                ) : (
                  "Reset Password"
                )}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
};

export default ResetPassword;
