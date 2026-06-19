import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { CheckCircle2, XCircle, Sparkles } from "lucide-react";

const Pricing = () => {
  const [annual, setAnnual] = useState(true);
  const navigate = useNavigate();

  const handleSelectPlan = (planName: string, price: number) => {
    navigate("/payment", { state: { planName, price, billingCycle: annual ? 'Annually' : 'Monthly', currency: 'INR' } });
  };

  const formatINR = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const plans = [
    {
      name: "Starter",
      description: "Perfect for small departments and single faculties.",
      monthlyPrice: 5999,
      annualPrice: 4999,
      features: [
        "Up to 1,000 students",
        "5 Examination Halls",
        "Basic Conflict Resolution",
        "Email Support",
        "Standard Export (CSV)"
      ],
      notIncluded: [
        "Custom Room Layouts",
        "SSO Integration",
        "API Access"
      ],
      highlighted: false,
    },
    {
      name: "Professional",
      description: "Ideal for mid-sized colleges and multiple departments.",
      monthlyPrice: 18999,
      annualPrice: 14999,
      features: [
        "Up to 5,000 students",
        "Unlimited Examination Halls",
        "Advanced AI Conflict Resolution",
        "Custom Room Layouts",
        "Priority Support 24/7",
        "PDF & Excel Exports"
      ],
      notIncluded: [
        "SSO Integration",
        "Dedicated Account Manager"
      ],
      highlighted: true,
    },
    {
      name: "Enterprise",
      description: "For large universities requiring complete control.",
      monthlyPrice: 42999,
      annualPrice: 34999,
      features: [
        "Unlimited students",
        "Multi-campus Support",
        "Enterprise-grade Security",
        "SSO Integration (SAML/OIDC)",
        "API Access & Webhooks",
        "Dedicated Account Manager",
        "Custom Onboarding"
      ],
      notIncluded: [],
      highlighted: false,
    }
  ];

  return (
    <div className="min-h-screen relative overflow-hidden bg-slate-50 py-24 animate-in fade-in duration-1000">
      {/* Premium Light Background Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] opacity-40 pointer-events-none">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-200 via-purple-200 to-pink-200 rounded-full blur-[100px] mix-blend-multiply" />
      </div>

      <div className="container relative z-10 mx-auto px-4 md:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-600 text-xs font-bold tracking-widest uppercase mb-6 shadow-sm">
            <Sparkles className="w-3.5 h-3.5" />
            Institutional Pricing
          </div>
          <h1 className="text-4xl md:text-6xl font-black tracking-tight text-slate-900 mb-6">
            Invest in <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">Excellence</span>
          </h1>
          <p className="text-xl text-slate-600 mb-10 font-medium">
            Transparent, scalable pricing tailored for Indian educational institutions. Upgrade your examination ecosystem today.
          </p>

          {/* Billing Toggle */}
          <div className="inline-flex items-center p-1.5 bg-white rounded-full border border-slate-200 shadow-sm">
            <button 
              className={`px-8 py-3.5 rounded-full text-sm font-bold transition-all duration-300 ${!annual ? 'bg-slate-100 text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              onClick={() => setAnnual(false)}
            >
              Monthly Billing
            </button>
            <button 
              className={`px-8 py-3.5 rounded-full text-sm font-bold transition-all duration-300 flex items-center gap-2 ${annual ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-md shadow-indigo-500/20' : 'text-slate-500 hover:text-slate-700'}`}
              onClick={() => setAnnual(true)}
            >
              Annual Billing 
              <span className={`text-[10px] px-2 py-0.5 rounded-full uppercase tracking-wider ${annual ? 'bg-white/20 text-white' : 'bg-indigo-50 text-indigo-600'}`}>Save 20%</span>
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto">
          {plans.map((plan, idx) => (
            <Card 
              key={idx} 
              className={`relative flex flex-col h-full overflow-hidden transition-all duration-500 hover:-translate-y-2 border-0 ${
                plan.highlighted 
                  ? 'bg-white shadow-2xl shadow-indigo-500/10 scale-105 z-10 ring-1 ring-indigo-500/30' 
                  : 'bg-white/60 shadow-xl backdrop-blur-xl ring-1 ring-slate-200/50 mt-0 md:mt-4 mb-0 md:mb-4'
              }`}
            >
              {plan.highlighted && (
                <div className="absolute top-0 inset-x-0 h-1.5 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500" />
              )}
              
              <CardHeader className="p-8 pb-6">
                {plan.highlighted && (
                  <div className="inline-block px-3 py-1 bg-indigo-50 text-indigo-600 text-xs font-bold rounded-full mb-4 w-fit tracking-wider uppercase border border-indigo-100">
                    Most Popular
                  </div>
                )}
                <h3 className="text-2xl font-bold text-slate-900">{plan.name}</h3>
                <p className="text-sm text-slate-500 mt-2 h-10">{plan.description}</p>
                
                <div className="mt-8 flex items-baseline text-slate-900">
                  <span className="text-5xl font-black tracking-tight">{formatINR(annual ? plan.annualPrice : plan.monthlyPrice)}</span>
                  <span className="text-slate-500 ml-2 font-medium">/mo</span>
                </div>
                {annual && (
                  <p className="text-sm text-indigo-600 mt-2 font-medium">Billed {formatINR(plan.annualPrice * 12)} annually</p>
                )}
              </CardHeader>
              
              <CardContent className="p-8 pt-0">
                <div className="space-y-4">
                  {plan.features.map((feature, i) => (
                    <div key={i} className="flex items-start gap-3">
                      <CheckCircle2 className="text-indigo-500 shrink-0 mt-0.5" size={18} />
                      <span className="text-slate-700 text-sm font-medium leading-tight">{feature}</span>
                    </div>
                  ))}
                  
                  {plan.notIncluded.map((feature, i) => (
                    <div key={i} className="flex items-start gap-3 opacity-50">
                      <XCircle className="text-slate-400 shrink-0 mt-0.5" size={18} />
                      <span className="text-slate-500 text-sm line-through font-medium leading-tight">{feature}</span>
                    </div>
                  ))}
                </div>
              </CardContent>
              
              <CardFooter className="p-8 pt-4 mt-auto">
                <Button 
                  onClick={() => handleSelectPlan(plan.name, annual ? plan.annualPrice * 12 : plan.monthlyPrice)}
                  className={`w-full h-12 font-bold rounded-xl transition-all duration-300 ${
                    plan.highlighted 
                      ? 'bg-slate-900 text-white hover:bg-slate-800 shadow-[0_4px_14px_0_rgba(15,23,42,0.39)] hover:shadow-[0_6px_20px_rgba(15,23,42,0.23)] hover:scale-[1.02]' 
                      : 'bg-white text-slate-900 hover:bg-slate-50 border border-slate-200 shadow-sm hover:scale-[1.02]'
                  }`}
                >
                  Choose {plan.name}
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Pricing;
