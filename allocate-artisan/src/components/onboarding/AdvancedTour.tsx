import { useState, useEffect } from "react";
import { Joyride, EventData, STATUS, Step, TooltipRenderProps } from "react-joyride";
import { useLocation } from "react-router-dom";
import { ArrowRight, ArrowLeft, X, Layers, Hexagon, ShieldCheck, Database, LayoutDashboard, Component, Briefcase, Settings2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface AdvancedTourProps {
  run: boolean;
  onFinish: () => void;
}

// Custom Tooltip Component for Premium Experience
function PremiumTooltip({
  continuous,
  index,
  step,
  backProps,
  closeProps,
  primaryProps,
  skipProps,
  tooltipProps,
  isLastStep,
  size
}: TooltipRenderProps) {
  return (
    <div
      {...tooltipProps}
      className="bg-[#0a0a0a]/80 backdrop-blur-3xl border border-white/[0.08] shadow-[0_0_0_1px_rgba(255,255,255,0.03),0_30px_60px_-15px_rgba(0,0,0,0.8),0_0_30px_-5px_rgba(99,102,241,0.15)] rounded-3xl w-full max-w-[380px] overflow-hidden animate-in zoom-in-[0.97] fade-in duration-500 font-sans group/tooltip"
    >
      {/* Animated Top Progress Bar */}
      <div className="w-full h-[2px] bg-white/[0.05] relative overflow-hidden">
        <div 
          className="absolute top-0 left-0 h-full bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-500 transition-all duration-700 ease-out shadow-[0_0_10px_rgba(99,102,241,0.8)]"
          style={{ width: `${((index + 1) / size) * 100}%` }}
        />
      </div>

      <div className="p-7 relative">
        {/* Deep ambient background mesh glow */}
        <div className="absolute top-0 right-0 w-48 h-48 bg-indigo-500/10 rounded-full blur-[50px] pointer-events-none opacity-50 group-hover/tooltip:opacity-100 transition-opacity duration-1000" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-purple-500/10 rounded-full blur-[40px] pointer-events-none opacity-30" />

        {/* Header / Title */}
        <div className="flex items-start justify-between mb-5 relative z-10">
          <div className="flex items-center gap-4">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-b from-slate-800 to-slate-900 flex items-center justify-center shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_4px_10px_rgba(0,0,0,0.5)] border border-white/[0.05] shrink-0 relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-tr from-indigo-500/20 to-transparent" />
              <div className="relative z-10">
                {step.data?.icon || <Hexagon className="h-5 w-5 text-indigo-400" />}
              </div>
            </div>
            <div>
              <div className="flex items-center gap-1.5 mb-1.5">
                {Array.from({ length: size }).map((_, i) => (
                  <div 
                    key={i} 
                    className={`h-1 rounded-full transition-all duration-300 ${i === index ? 'w-3 bg-indigo-400 shadow-[0_0_8px_rgba(129,140,248,0.8)]' : i < index ? 'w-1 bg-indigo-400/40' : 'w-1 bg-slate-700'}`}
                  />
                ))}
              </div>
              <h3 className="font-semibold text-[17px] text-slate-100 tracking-tight leading-tight">
                {step.title}
              </h3>
            </div>
          </div>
          <button
            {...closeProps}
            className="text-slate-500 hover:text-white transition-all shrink-0 p-1.5 rounded-lg hover:bg-white/10"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Content Body */}
        <div className="text-[14px] text-slate-400 leading-[1.7] tracking-wide relative z-10 font-medium">
          {step.content}
        </div>
      </div>

      {/* Footer Controls */}
      <div className="px-7 py-5 bg-[#050505]/60 border-t border-white/[0.04] flex items-center justify-between relative z-10">
        <div className="flex items-center gap-2">
          {!isLastStep && (
            <button
              {...skipProps}
              className="text-[11px] font-bold text-slate-500 hover:text-slate-300 transition-colors tracking-[0.15em] uppercase px-2 py-1"
            >
              Dismiss
            </button>
          )}
        </div>
        <div className="flex items-center gap-3">
          {index > 0 && (
            <Button
              variant="ghost"
              size="sm"
              {...backProps}
              className="h-9 px-4 text-[13px] gap-1.5 font-semibold text-slate-400 hover:text-white hover:bg-white/10 rounded-full transition-all"
            >
              <ArrowLeft className="h-3.5 w-3.5" />
              Back
            </Button>
          )}
          <Button
            size="sm"
            {...primaryProps}
            className="group h-9 px-6 text-[13px] gap-2 font-bold bg-white hover:bg-slate-200 text-black rounded-full shadow-[0_0_15px_rgba(255,255,255,0.15)] transition-all"
          >
            {isLastStep ? (
              "Complete"
            ) : (
              <>
                Continue 
                <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function AdvancedTour({ run, onFinish }: AdvancedTourProps) {
  const location = useLocation();

  const steps: Step[] = [
    {
      target: "#tour-dashboard",
      title: "Platform Navigation",
      content: (
        <p>
          Welcome to the HallSync ecosystem. We will guide you through the core modules of your administrative interface, starting with the Command Center.
        </p>
      ),
      placement: "right-start",
      skipBeacon: true,
      data: { icon: <Layers className="h-4 w-4 text-white" /> }
    },
    {
      target: "#tour-dashboard-semester",
      title: "Examination Planning",
      content: (
        <p>
          Process student nominal rolls and let the intelligent allocation engine compute optimal seating arrangements.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Component className="h-4 w-4 text-white" /> }
    },
    {
      target: "#tour-dashboard-duty",
      title: "Resource Allocation",
      content: (
        <p>
          Automated invigilator distribution. The system automatically balances departmental workloads to ensure perfect equity.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Briefcase className="h-4 w-4 text-white" /> }
    },
    {
      target: "#tour-dashboard-qp-security",
      title: "Security & Operations",
      content: (
        <p>
          Maintain strict operational integrity. Track physical question paper distributions and log incident reports dynamically.
        </p>
      ),
      placement: "right-start",
      data: { icon: <ShieldCheck className="h-4 w-4 text-white" /> }
    },
    {
      target: "#tour-dashboard-compliance-vault",
      title: "Compliance Vault",
      content: (
        <p>
          Your immutable ledger. Archive all examination records and instantly synthesize evidence documentation for regulatory audits.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Database className="h-4 w-4 text-white" /> }
    },
    {
      target: "#tour-dashboard-settings",
      title: "System Configuration",
      content: (
        <p>
          Manage institutional parameters, update branding assets, and configure remuneration baseline metrics.
        </p>
      ),
      placement: "right-end",
      data: { icon: <Settings2 className="h-4 w-4 text-white" /> }
    },
    {
      target: ".tour-header-controls",
      title: "Initialization Complete",
      content: (
        <p>
          The interface is fully operational. Access this guide sequence at any time via the Tour Guide module in the primary header.
        </p>
      ),
      placement: "bottom-end",
      data: { icon: <Hexagon className="h-4 w-4 text-white" /> }
    }
  ];

  const handleJoyrideEvent = (data: EventData) => {
    const { status } = data;
    const finishedStatuses: string[] = [STATUS.FINISHED, STATUS.SKIPPED];

    if (finishedStatuses.includes(status)) {
      onFinish();
    }
  };

  return (
    <Joyride
      key={run ? "joyride-running" : "joyride-stopped"}
      onEvent={handleJoyrideEvent}
      continuous
      run={run}
      scrollToFirstStep
      steps={steps}
      tooltipComponent={PremiumTooltip}
      options={{
        showProgress: true,
        buttons: ['back', 'close', 'primary', 'skip'],
        zIndex: 10000,
        arrowColor: '#0f172a', // Matches bg-slate-900 (the premium tooltip background)
        overlayColor: 'rgba(2, 6, 23, 0.65)', // Elegant deep dark overlay
      }}
    />
  );
}
