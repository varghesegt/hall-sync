import React from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ShieldCheck, Layers, Users, ArrowRight, CheckCircle2, ChevronRight } from "lucide-react";

const Home = () => {
  return (
    <div className="flex flex-col items-center w-full min-h-screen bg-[#FAFAFA] relative overflow-hidden font-sans selection:bg-indigo-500/20">
      {/* Premium Elegant Background Effects */}
      <div className="absolute top-0 left-0 w-full h-[800px] bg-gradient-to-b from-slate-100 to-transparent pointer-events-none" />
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-indigo-500/5 blur-[120px] rounded-full pointer-events-none" />
      <div className="absolute top-[10%] right-[-10%] w-[40%] h-[40%] bg-purple-500/5 blur-[120px] rounded-full pointer-events-none" />

      {/* Hero Section */}
      <section className="container relative z-10 mx-auto px-6 lg:px-8 py-24 lg:py-36 flex flex-col items-center text-center animate-in fade-in slide-in-from-bottom-4 duration-1000">
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white text-indigo-600 font-semibold text-xs tracking-wide mb-10 border border-slate-200/60 shadow-[0_2px_10px_rgb(0,0,0,0.02)]">
          <span className="flex h-2 w-2 rounded-full bg-indigo-600"></span>
          Advanced Examination Logistics
        </div>
        
        <h1 className="text-5xl lg:text-7xl font-extrabold tracking-tight text-slate-900 max-w-5xl leading-[1.1] mb-8">
          Precision Seating Allocation for <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">Universities</span>
        </h1>
        
        <p className="text-xl lg:text-2xl text-slate-500 max-w-3xl mb-12 font-medium leading-relaxed">
          HallSync replaces manual spreadsheet planning with a secure, rule-based seating engine that generates conflict-free floor plans and duty rosters in minutes.
        </p>
        
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto">
          <Link to="/pricing" className="w-full sm:w-auto">
            <Button size="lg" className="w-full sm:w-auto h-14 px-8 text-lg font-bold bg-slate-900 hover:bg-slate-800 text-white rounded-full shadow-[0_8px_30px_rgb(0,0,0,0.12)] hover:shadow-[0_8px_30px_rgb(0,0,0,0.16)] transition-all">
              View Pricing <ArrowRight className="ml-2" size={20} />
            </Button>
          </Link>
          <Link to="/about" className="w-full sm:w-auto">
            <Button size="lg" variant="outline" className="w-full sm:w-auto h-14 px-8 text-lg font-bold bg-white text-slate-900 hover:bg-slate-50 border-slate-200 shadow-sm rounded-full transition-all">
              How It Works
            </Button>
          </Link>
        </div>
      </section>

      {/* Features Section */}
      <section className="w-full py-32 bg-white relative z-10 border-t border-slate-200/50">
        <div className="container mx-auto px-6 lg:px-8">
          <div className="text-center mb-20 max-w-3xl mx-auto">
            <h2 className="text-3xl lg:text-4xl font-extrabold text-slate-900 mb-6 tracking-tight">Built for institutional workflows</h2>
            <p className="text-lg text-slate-500 font-medium leading-relaxed">
              Designed in collaboration with Controllers of Examinations to strictly enforce academic separation policies and campus infrastructure constraints.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-12 max-w-7xl mx-auto">
            <Card className="bg-slate-50/50 border border-slate-200/60 shadow-sm hover:shadow-[0_20px_40px_rgb(0,0,0,0.04)] transition-all duration-500 rounded-3xl overflow-hidden group">
              <CardContent className="p-10 lg:p-12 h-full flex flex-col">
                <div className="h-16 w-16 rounded-2xl bg-white shadow-sm border border-slate-200/60 flex items-center justify-center text-slate-700 mb-8 group-hover:scale-110 transition-transform duration-500">
                  <Layers size={28} strokeWidth={1.5} />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 mb-4">Infrastructure Mapping</h3>
                <p className="text-slate-600 font-medium leading-relaxed flex-grow">
                  Digitize your campus layout. Define hall capacities, row structures, and dynamically mask unusable desks for specialized examinations to ensure total accuracy.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-50/50 border border-slate-200/60 shadow-sm hover:shadow-[0_20px_40px_rgb(0,0,0,0.04)] transition-all duration-500 rounded-3xl overflow-hidden group">
              <CardContent className="p-10 lg:p-12 h-full flex flex-col">
                <div className="h-16 w-16 rounded-2xl bg-white shadow-sm border border-slate-200/60 flex items-center justify-center text-slate-700 mb-8 group-hover:scale-110 transition-transform duration-500">
                  <Users size={28} strokeWidth={1.5} />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 mb-4">Strict Separation</h3>
                <p className="text-slate-600 font-medium leading-relaxed flex-grow">
                  The allocation engine strictly enforces departmental boundaries, spacing out students systematically to prevent proximity conflicts and deter malpractice.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-50/50 border border-slate-200/60 shadow-sm hover:shadow-[0_20px_40px_rgb(0,0,0,0.04)] transition-all duration-500 rounded-3xl overflow-hidden group">
              <CardContent className="p-10 lg:p-12 h-full flex flex-col">
                <div className="h-16 w-16 rounded-2xl bg-white shadow-sm border border-slate-200/60 flex items-center justify-center text-slate-700 mb-8 group-hover:scale-110 transition-transform duration-500">
                  <ShieldCheck size={28} strokeWidth={1.5} />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 mb-4">Role-Based Security</h3>
                <p className="text-slate-600 font-medium leading-relaxed flex-grow">
                  Grant granular access controls to chief superintendents, clerks, and faculty, ensuring that sensitive examination data remains completely confidential.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container mx-auto px-6 lg:px-8 py-32 relative z-10 w-full max-w-7xl">
        <div className="bg-slate-900 rounded-[2.5rem] p-10 lg:p-20 text-center relative overflow-hidden shadow-2xl">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900" />
          <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/10 rounded-full blur-[100px]" />
          <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-purple-500/10 rounded-full blur-[100px]" />
          
          <div className="relative z-10 max-w-4xl mx-auto">
            <h2 className="text-4xl lg:text-5xl font-extrabold text-white mb-8 tracking-tight leading-tight">
              Standardize your examination logistics today.
            </h2>
            <p className="text-xl text-slate-300 font-medium mb-12 max-w-2xl mx-auto leading-relaxed">
              Join leading autonomous institutions utilizing HallSync for core examination logistics. Full deployment and data migration within 48 hours.
            </p>
            
            <ul className="flex flex-col sm:flex-row items-center justify-center gap-6 lg:gap-10 mb-12 text-slate-200 font-semibold text-lg">
              <li className="flex items-center gap-3"><CheckCircle2 className="text-emerald-400" size={24}/> Dedicated Support</li>
              <li className="flex items-center gap-3"><CheckCircle2 className="text-emerald-400" size={24}/> ERP Integration</li>
              <li className="flex items-center gap-3"><CheckCircle2 className="text-emerald-400" size={24}/> High Availability</li>
            </ul>
            
            <Link to="/contact">
              <Button size="lg" className="h-16 px-10 text-lg font-bold bg-white text-slate-900 hover:bg-slate-100 hover:scale-105 transition-all rounded-full shadow-[0_8px_30px_rgb(255,255,255,0.12)]">
                Contact Sales Team <ChevronRight className="ml-2" size={20} />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
