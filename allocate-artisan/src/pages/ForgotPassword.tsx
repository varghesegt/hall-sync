import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import apiClient from "@/api/axios";
import { Mail, ArrowLeft, Loader2 } from "lucide-react";

const ForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await apiClient.post("/auth/forgot-password", { email });
      toast.success("Reset Link Sent", {
        description: `If an account exists with that email, a password reset link has been sent.`,
      });
      navigate("/login");
    } catch (error: any) {
      toast.error("Request Failed", {
        description: "Unable to process your request at this time.",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden mesh-gradient p-4">
      <div className="relative w-full max-w-md space-y-8 animate-in fade-in zoom-in slide-in-from-top-10 duration-1000">
        <Button 
          variant="ghost" 
          className="absolute -top-12 left-0 text-slate-500 hover:text-slate-900"
          onClick={() => navigate("/login")}
        >
          <ArrowLeft className="mr-2 h-4 w-4" /> Back to Login
        </Button>
        <Card className="glass border-white/20 overflow-hidden shadow-2xl transition-all duration-500">
          <div className="h-2 bg-gradient-to-r from-primary via-indigo-400 to-emerald-400 w-full" />
          <CardHeader className="space-y-1 pb-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-4">
              <Mail size={32} />
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight">Forgot Password</CardTitle>
            <CardDescription className="text-slate-500">
              Enter your email address to receive a password reset link.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleForgotPassword}>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="email" className="text-xs font-bold uppercase tracking-widest text-slate-400 ml-1">Email Address</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="admin@college.edu"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
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
                    Sending Request...
                  </>
                ) : (
                  "Send Reset Link"
                )}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
};

export default ForgotPassword;
