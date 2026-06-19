import React from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

const About = () => {
  return (
    <div className="min-h-screen bg-slate-50 relative overflow-hidden py-12 md:py-20 animate-in fade-in duration-700">
      {/* Premium Light Background Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] opacity-40 pointer-events-none">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-200 via-purple-200 to-pink-200 rounded-full blur-[100px] mix-blend-multiply" />
      </div>

      <div className="container relative z-10 mx-auto px-4 md:px-8">
        <div className="max-w-3xl mx-auto space-y-12">
          <div className="text-center space-y-6 mb-16">
            <h1 className="text-4xl md:text-6xl font-black tracking-tight text-slate-900">
              About <span className="text-indigo-600">Us</span>
            </h1>
            <p className="text-xl text-slate-600 font-medium">
              We build specialized software for examination control boards.
            </p>
          </div>

          <div className="bg-white p-8 md:p-12 rounded-3xl shadow-xl border border-slate-100">
            <div className="prose prose-lg max-w-none text-slate-600 font-medium leading-relaxed">
              <p>
                HallSync was developed specifically for university examination departments. We recognized that the process of allocating students to examination halls was heavily reliant on manual spreadsheet calculations, which is both time-consuming and prone to human error.
              </p>

              <h2 className="text-2xl font-black text-slate-900 mt-12 mb-6 tracking-tight">Our Focus</h2>
              <p>
                We focus strictly on the logistical challenges of examination day. Our platform handles room capacities, prevents department overlaps to deter malpractice, and generates the necessary PDF attendance sheets directly required by invigilators. 
              </p>

              <div className="my-12 p-8 bg-indigo-50 border border-indigo-100 rounded-2xl">
                <h3 className="text-xl font-bold text-indigo-900 mb-3">Our Objective</h3>
                <p className="text-indigo-800 m-0">
                  To provide a stable, secure, and highly efficient seat allocation engine that saves administrative staff hundreds of hours every semester.
                </p>
              </div>

              <h2 className="text-2xl font-black text-slate-900 mt-12 mb-6 tracking-tight">Core Priorities</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 not-prose mt-8">
                {[
                  { title: "Practicality", desc: "No unnecessary features. We build tools that directly solve daily administrative bottlenecks." },
                  { title: "Data Security", desc: "Student data and examination schedules are secured behind strict access controls." },
                  { title: "High Availability", desc: "We ensure the platform operates seamlessly during critical examination weeks." },
                  { title: "Clear Reporting", desc: "Generate precise floor plans, seating charts, and signatures sheets instantly." }
                ].map((value, idx) => (
                  <div key={idx} className="p-6 rounded-2xl bg-slate-50 border border-slate-100 shadow-sm">
                    <h4 className="font-bold text-slate-900 mb-2">{value.title}</h4>
                    <p className="text-sm text-slate-600 leading-relaxed">{value.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="mt-20 text-center pb-12">
            <p className="text-lg font-bold text-slate-900 mb-6 tracking-tight">Interested in deploying HallSync?</p>
            <Link to="/contact">
              <Button size="lg" className="h-14 px-10 text-lg font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-[0_4px_14px_0_rgba(79,70,229,0.39)] hover:shadow-[0_6px_20px_rgba(79,70,229,0.23)] transition-all">
                Speak to our Team
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default About;
