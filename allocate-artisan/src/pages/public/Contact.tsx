import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { Mail, MapPin, Phone, Send, Building2 } from "lucide-react";
import { toast } from "sonner";

const Contact = () => {
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    // Simulate API call
    setTimeout(() => {
      setLoading(false);
      toast.success("Request Submitted", {
        description: "A sales representative will contact your institution shortly.",
      });
      (e.target as HTMLFormElement).reset();
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-slate-50 relative overflow-hidden py-12 md:py-20 animate-in fade-in duration-700">
      {/* Premium Light Background Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] opacity-40 pointer-events-none">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-200 via-purple-200 to-pink-200 rounded-full blur-[100px] mix-blend-multiply" />
      </div>

      <div className="container relative z-10 mx-auto px-4 md:px-8">
        <div className="max-w-6xl mx-auto">
          <div className="text-center space-y-6 mb-16">
            <h1 className="text-4xl md:text-6xl font-black tracking-tight text-slate-900">
              Contact <span className="text-indigo-600">Sales</span>
            </h1>
            <p className="text-xl text-slate-600 font-medium max-w-2xl mx-auto">
              Request a demo, discuss enterprise pricing, or inquire about ERP integrations for your university.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
            <div className="lg:col-span-1 space-y-10 mt-4">
              <div>
                <h3 className="text-2xl font-black text-slate-900 mb-2 tracking-tight">Direct Support</h3>
                <p className="text-slate-600 font-medium mb-8">We prioritize rapid response times for institutional clients.</p>
              </div>
              
              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-xl bg-white border border-slate-100 shadow-sm flex items-center justify-center shrink-0">
                  <MapPin className="text-indigo-600" size={24} />
                </div>
                <div>
                  <h4 className="font-bold text-slate-900">Headquarters</h4>
                  <p className="text-slate-600 font-medium mt-1">Chennai, Tamil Nadu<br/>India 600001</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-xl bg-white border border-slate-100 shadow-sm flex items-center justify-center shrink-0">
                  <Mail className="text-indigo-600" size={24} />
                </div>
                <div>
                  <h4 className="font-bold text-slate-900">Email Directory</h4>
                  <p className="text-slate-600 font-medium mt-1">sales@hallsync.in<br/>support@hallsync.in</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-xl bg-white border border-slate-100 shadow-sm flex items-center justify-center shrink-0">
                  <Building2 className="text-indigo-600" size={24} />
                </div>
                <div>
                  <h4 className="font-bold text-slate-900">Operating Hours</h4>
                  <p className="text-slate-600 font-medium mt-1">Monday - Friday<br/>9:00 AM - 6:00 PM IST</p>
                </div>
              </div>
            </div>

            <div className="lg:col-span-2">
              <Card className="bg-white shadow-xl shadow-indigo-500/5 border-slate-100 rounded-3xl overflow-hidden">
                <CardContent className="p-8 md:p-12">
                  <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <Label htmlFor="firstName" className="font-semibold text-slate-700">First Name</Label>
                        <Input id="firstName" placeholder="Jane" required className="bg-slate-50 border-slate-200 focus:bg-white h-12 rounded-xl" />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="lastName" className="font-semibold text-slate-700">Last Name</Label>
                        <Input id="lastName" placeholder="Doe" required className="bg-slate-50 border-slate-200 focus:bg-white h-12 rounded-xl" />
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <Label htmlFor="email" className="font-semibold text-slate-700">Official Work Email</Label>
                        <Input id="email" type="email" placeholder="jane@university.edu.in" required className="bg-slate-50 border-slate-200 focus:bg-white h-12 rounded-xl" />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="phone" className="font-semibold text-slate-700">Phone Number</Label>
                        <Input id="phone" type="tel" placeholder="+91 98765 43210" className="bg-slate-50 border-slate-200 focus:bg-white h-12 rounded-xl" />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="institution" className="font-semibold text-slate-700">Institution Name</Label>
                      <Input id="institution" placeholder="Enter full college or university name" className="bg-slate-50 border-slate-200 focus:bg-white h-12 rounded-xl" required />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="message" className="font-semibold text-slate-700">Inquiry Details</Label>
                      <Textarea 
                        id="message" 
                        placeholder="Please describe your current allocation process and how we can assist..." 
                        className="min-h-[150px] bg-slate-50 border-slate-200 focus:bg-white rounded-xl resize-none p-4" 
                        required 
                      />
                    </div>

                    <Button 
                      type="submit" 
                      className="w-full h-14 text-lg font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-[0_4px_14px_0_rgba(79,70,229,0.39)] hover:shadow-[0_6px_20px_rgba(79,70,229,0.23)] transition-all mt-4"
                      disabled={loading}
                    >
                      {loading ? "Transmitting..." : <><Send className="mr-2" size={20} /> Submit Inquiry</>}
                    </Button>
                  </form>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Contact;
