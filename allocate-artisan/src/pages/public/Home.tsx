import React from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ShieldCheck, Zap, Layers, Users, ArrowRight, CheckCircle2 } from "lucide-react";

const Home = () => {
  return (
    <div className="flex flex-col items-center w-full min-h-screen bg-slate-50 relative overflow-hidden">
      {/* Premium Light Background Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1200px] h-[600px] opacity-40 pointer-events-none">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-200 via-purple-200 to-pink-200 rounded-full blur-[120px] mix-blend-multiply" />
      </div>

      {/* Hero Section */}
      <section className="container relative z-10 mx-auto px-4 md:px-8 py-20 md:py-32 flex flex-col items-center text-center animate-in slide-in-from-bottom-8 duration-1000">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-600 font-bold text-xs uppercase tracking-widest mb-8 border border-indigo-100 shadow-sm">
          <Zap size={14} />
          <span>Institutional Examination Software</span>
        </div>
        <h1 className="text-5xl md:text-7xl font-black tracking-tight text-slate-900 max-w-4xl leading-tight mb-6">
          Automate Seat Allocation with <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">Total Precision</span>
        </h1>
        <p className="text-xl text-slate-600 max-w-2xl mb-10 font-medium leading-relaxed">
          HallSync is a dedicated platform built for Controllers of Examinations. Manage halls, generate conflict-free seating plans, and export attendance sheets instantly.
        </p>
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto">
          <Link to="/pricing" className="w-full sm:w-auto">
            <Button size="lg" className="w-full sm:w-auto h-14 px-8 text-lg font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-[0_4px_14px_0_rgba(79,70,229,0.39)] hover:shadow-[0_6px_20px_rgba(79,70,229,0.23)] transition-all">
              View Pricing <ArrowRight className="ml-2" size={20} />
            </Button>
          </Link>
          <Link to="/about" className="w-full sm:w-auto">
            <Button size="lg" variant="outline" className="w-full sm:w-auto h-14 px-8 text-lg font-bold bg-white text-slate-900 hover:bg-slate-50 border border-slate-200 shadow-sm rounded-xl transition-all">
              Learn More
            </Button>
          </Link>
        </div>
      </section>

      {/* Features Section */}
      <section className="w-full py-24 bg-white border-y border-slate-100 relative z-10">
        <div className="container mx-auto px-4 md:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-black text-slate-900 mb-4 tracking-tight">Built for modern universities</h2>
            <p className="text-lg text-slate-500 font-medium max-w-2xl mx-auto">
              Replace manual spreadsheets with a secure, automated workflow designed specifically for higher education.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto">
            <Card className="bg-slate-50 border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 rounded-2xl overflow-hidden hover:-translate-y-1">
              <CardContent className="p-8">
                <div className="h-14 w-14 rounded-xl bg-white shadow-sm border border-slate-100 flex items-center justify-center text-indigo-600 mb-6">
                  <Layers size={28} />
                </div>
                <h3 className="text-xl font-bold text-slate-900 mb-3">Room Mapping</h3>
                <p className="text-slate-600 font-medium leading-relaxed">
                  Map your campus infrastructure, define hall capacities, and easily block out unusable desks for specific examinations.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-50 border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 rounded-2xl overflow-hidden hover:-translate-y-1">
              <CardContent className="p-8">
                <div className="h-14 w-14 rounded-xl bg-white shadow-sm border border-slate-100 flex items-center justify-center text-emerald-600 mb-6">
                  <Users size={28} />
                </div>
                <h3 className="text-xl font-bold text-slate-900 mb-3">Conflict Prevention</h3>
                <p className="text-slate-600 font-medium leading-relaxed">
                  The allocation engine automatically ensures students taking the same exam are spaced out, preventing departmental overlap.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-50 border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 rounded-2xl overflow-hidden hover:-translate-y-1">
              <CardContent className="p-8">
                <div className="h-14 w-14 rounded-xl bg-white shadow-sm border border-slate-100 flex items-center justify-center text-purple-600 mb-6">
                  <ShieldCheck size={28} />
                </div>
                <h3 className="text-xl font-bold text-slate-900 mb-3">Role-Based Security</h3>
                <p className="text-slate-600 font-medium leading-relaxed">
                  Grant specific access to chief superintendents, clerks, and invigilators to ensure examination data remains strictly confidential.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container mx-auto px-4 md:px-8 py-24 relative z-10">
        <div className="bg-indigo-600 rounded-3xl p-8 md:p-16 text-center relative overflow-hidden shadow-2xl">
          <div className="absolute inset-0 bg-gradient-to-r from-indigo-600 to-purple-700" />
          <div className="absolute top-0 right-0 w-64 h-64 bg-white opacity-5 rounded-full blur-3xl" />
          <div className="relative z-10">
            <h2 className="text-3xl md:text-5xl font-black text-white mb-6 tracking-tight">Deploy HallSync today</h2>
            <p className="text-lg text-indigo-100 font-medium max-w-2xl mx-auto mb-10">
              Join leading technical institutions that use HallSync to manage their semester exam allocations. Setup takes less than 24 hours.
            </p>
            <ul className="flex flex-col sm:flex-row items-center justify-center gap-8 mb-10 text-white font-medium">
              <li className="flex items-center gap-2"><CheckCircle2 className="text-indigo-300" size={20}/> Dedicated Onboarding</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="text-indigo-300" size={20}/> ERP Integration Support</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="text-indigo-300" size={20}/> 99.9% Uptime SLA</li>
            </ul>
            <Link to="/contact">
              <Button size="lg" className="h-14 px-10 text-lg font-bold bg-white text-indigo-900 hover:bg-slate-50 hover:scale-105 transition-all rounded-xl shadow-lg">
                Contact Sales Team
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
