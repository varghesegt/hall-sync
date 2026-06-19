import React, { useState } from "react";
import { useLocation, useNavigate, Navigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { ShieldCheck, CreditCard, Lock, Loader2, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

const formatINR = (amount: number) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
};

const Payment = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  // If accessed directly without selecting a plan, redirect to pricing
  if (!location.state) {
    return <Navigate to="/pricing" replace />;
  }

  const { planName, price, billingCycle } = location.state as { planName: string, price: number, billingCycle: string };

  const handlePayment = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    // Simulate payment gateway processing
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
      toast.success("Payment Successful!", {
        description: `Welcome to HallSync ${planName}. Your workspace is ready.`,
      });
      
      // Redirect to login or dashboard after success
      setTimeout(() => {
        navigate("/login");
      }, 3000);
    }, 2500);
  };

  if (success) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center bg-slate-50 animate-in zoom-in duration-500">
        <div className="text-center space-y-6 max-w-md bg-white p-12 rounded-3xl shadow-xl border border-slate-100">
          <div className="mx-auto w-24 h-24 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center animate-bounce shadow-inner">
            <CheckCircle2 size={48} strokeWidth={3} />
          </div>
          <h2 className="text-3xl font-black text-slate-900 tracking-tight">Payment Complete!</h2>
          <p className="text-slate-500 text-lg font-medium">
            Thank you for subscribing to the {planName} plan. Your receipt has been emailed to you.
          </p>
          <p className="text-sm text-indigo-600 font-semibold flex items-center justify-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" /> Redirecting you to the portal...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 relative overflow-hidden py-12 md:py-20 animate-in fade-in duration-700">
      {/* Premium Light Background Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] opacity-40 pointer-events-none">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-200 via-purple-200 to-pink-200 rounded-full blur-[100px] mix-blend-multiply" />
      </div>

      <div className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8 relative z-10">
        
        {/* Checkout Form */}
        <div className="md:col-span-2">
          <Card className="shadow-2xl shadow-indigo-500/5 border-slate-200/60 bg-white/80 backdrop-blur-xl rounded-2xl">
            <CardHeader className="border-b border-slate-100 pb-6 rounded-t-2xl">
              <CardTitle className="text-2xl font-black flex items-center gap-2 text-slate-900">
                <CreditCard className="text-indigo-600" /> Payment Details
              </CardTitle>
              <CardDescription className="font-medium text-slate-500">
                Complete your purchase securely.
              </CardDescription>
            </CardHeader>
            <CardContent className="p-6 md:p-8">
              <form onSubmit={handlePayment} className="space-y-8">
                {/* Personal Details */}
                <div className="space-y-4">
                  <h3 className="font-bold text-slate-900">Personal Information</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="fname" className="text-slate-600 font-semibold">First Name</Label>
                      <Input id="fname" required className="bg-white focus:bg-white border-slate-200" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="lname" className="text-slate-600 font-semibold">Last Name</Label>
                      <Input id="lname" required className="bg-white focus:bg-white border-slate-200" />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-slate-600 font-semibold">Email Address</Label>
                    <Input id="email" type="email" required className="bg-white focus:bg-white border-slate-200" />
                  </div>
                </div>

                <hr className="border-slate-100" />

                {/* Card Details (Mocked Interface) */}
                <div className="space-y-4">
                  <h3 className="font-bold text-slate-900">Card Details</h3>
                  <div className="relative space-y-2">
                    <Label htmlFor="cardName" className="text-slate-600 font-semibold">Name on Card</Label>
                    <Input id="cardName" required placeholder="John Doe" className="bg-white focus:bg-white border-slate-200" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="cardNum" className="text-slate-600 font-semibold">Card Number</Label>
                    <div className="relative">
                      <Input id="cardNum" required placeholder="0000 0000 0000 0000" className="pl-10 bg-white focus:bg-white border-slate-200" />
                      <CreditCard className="absolute left-3 top-2.5 text-slate-400" size={18} />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="expiry" className="text-slate-600 font-semibold">Expiry Date</Label>
                      <Input id="expiry" required placeholder="MM/YY" className="bg-white focus:bg-white border-slate-200" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="cvc" className="text-slate-600 font-semibold">CVC</Label>
                      <Input id="cvc" required placeholder="123" type="password" maxLength={4} className="bg-white focus:bg-white border-slate-200" />
                    </div>
                  </div>
                </div>

                <Button 
                  type="submit" 
                  className="w-full h-14 text-lg font-bold shadow-[0_4px_14px_0_rgba(79,70,229,0.39)] hover:shadow-[0_6px_20px_rgba(79,70,229,0.23)] bg-indigo-600 hover:bg-indigo-700 transition-all text-white rounded-xl"
                  disabled={loading}
                >
                  {loading ? (
                    <><Loader2 className="mr-2 animate-spin" size={20} /> Processing Payment...</>
                  ) : (
                    <><Lock className="mr-2" size={18} /> Pay {formatINR(price)}</>
                  )}
                </Button>
                
                <p className="text-center text-xs text-slate-500 font-medium flex items-center justify-center gap-1.5 mt-4 bg-slate-100/50 py-2 rounded-lg">
                  <ShieldCheck size={14} className="text-emerald-500" /> Payments are secure and encrypted.
                </p>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Order Summary */}
        <div className="md:col-span-1">
          <Card className="bg-white/60 backdrop-blur-xl border-slate-200/60 shadow-xl shadow-indigo-500/5 sticky top-24 rounded-2xl">
            <CardHeader className="bg-slate-50/50 border-b border-slate-100 rounded-t-2xl">
              <CardTitle className="text-lg font-black text-slate-900">Order Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6 pt-6">
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-bold text-slate-900">{planName} Plan</h4>
                  <p className="text-sm text-indigo-600 font-semibold mt-0.5">Billed {billingCycle}</p>
                </div>
                <div className="text-lg font-bold text-slate-900">{formatINR(price)}</div>
              </div>
              
              <hr className="border-slate-100" />
              
              <div className="space-y-3 text-sm text-slate-600 font-medium">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span>{formatINR(price)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Tax (Estimated)</span>
                  <span className="text-slate-400">{formatINR(0)}</span>
                </div>
              </div>
              
              <hr className="border-slate-100" />
              
              <div className="flex justify-between items-center bg-indigo-50/50 p-4 rounded-xl border border-indigo-100/50">
                <span className="font-black text-slate-900">Total</span>
                <span className="text-2xl font-black tracking-tight text-indigo-600">{formatINR(price)}</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Payment;
