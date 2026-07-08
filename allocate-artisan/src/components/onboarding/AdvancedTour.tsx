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
      className="bg-[#05050a]/90 backdrop-blur-3xl border border-white/[0.08] shadow-[0_0_0_1px_rgba(255,255,255,0.03),0_40px_80px_-20px_rgba(0,0,0,0.9),0_0_40px_-5px_rgba(99,102,241,0.2)] rounded-3xl w-full max-w-[420px] overflow-hidden animate-in zoom-in-[0.97] slide-in-from-bottom-2 fade-in duration-500 font-sans group/tooltip"
    >
      {/* Animated Top Progress Bar */}
      <div className="w-full h-[2px] bg-white/[0.03] relative overflow-hidden">
        <div 
          className="absolute top-0 left-0 h-full bg-gradient-to-r from-indigo-500 via-purple-500 to-emerald-500 transition-all duration-700 ease-out shadow-[0_0_15px_rgba(99,102,241,0.9)]"
          style={{ width: `${((index + 1) / size) * 100}%` }}
        />
      </div>

      <div className="p-8 relative">
        {/* Deep ambient background mesh glow */}
        <div className="absolute top-0 right-0 w-56 h-56 bg-indigo-500/10 rounded-full blur-[60px] pointer-events-none opacity-40 group-hover/tooltip:opacity-100 transition-opacity duration-1000" />
        <div className="absolute bottom-0 left-0 w-40 h-40 bg-purple-500/10 rounded-full blur-[50px] pointer-events-none opacity-20" />

        {/* Header / Title */}
        <div className="flex items-start justify-between mb-6 relative z-10 pr-24">
          <div className="flex items-center gap-5">
            <div className="h-12 w-12 rounded-2xl bg-gradient-to-b from-slate-800 to-slate-950 flex items-center justify-center shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_8px_16px_rgba(0,0,0,0.6)] border border-white/[0.05] shrink-0 relative overflow-hidden group-hover/tooltip:border-indigo-500/30 transition-colors duration-500">
              <div className="absolute inset-0 bg-gradient-to-tr from-indigo-500/20 to-transparent opacity-50 group-hover/tooltip:opacity-100 transition-opacity duration-500" />
              <div className="relative z-10 scale-110">
                {step.data?.icon || <Hexagon className="h-5 w-5 text-indigo-400" />}
              </div>
            </div>
            <div>
              <div className="flex items-center gap-1.5 mb-2">
                {Array.from({ length: size }).map((_, i) => (
                  <div 
                    key={i} 
                    className={`h-1 rounded-full transition-all duration-300 ${i === index ? 'w-4 bg-indigo-400 shadow-[0_0_10px_rgba(129,140,248,0.9)]' : i < index ? 'w-1.5 bg-indigo-400/40' : 'w-1.5 bg-slate-700'}`}
                  />
                ))}
              </div>
              <h3 className="font-bold text-[19px] text-slate-50 tracking-tight leading-tight">
                {step.title}
              </h3>
            </div>
          </div>
        </div>

        {/* Content Body */}
        <div className="text-[14.5px] text-slate-400/90 leading-[1.75] tracking-wide relative z-10 font-medium pr-2">
          {step.content}
        </div>
      </div>

      {/* Footer Controls */}
      <div className="px-8 py-5 bg-[#020205]/80 border-t border-white/[0.04] flex items-center justify-between relative z-10">
        <div className="flex items-center gap-2">
          {!isLastStep && (
            <button
              {...skipProps}
              className="text-[10px] font-black text-slate-600 hover:text-slate-300 transition-colors tracking-[0.2em] uppercase px-2 py-1"
            >
              Dismiss Tour
            </button>
          )}
        </div>
        <div className="flex items-center gap-3">
          {index > 0 && (
            <Button
              variant="ghost"
              size="sm"
              {...backProps}
              className="h-10 px-5 text-[13px] gap-2 font-bold text-slate-400 hover:text-white hover:bg-white/10 rounded-full transition-all"
            >
              <ArrowLeft className="h-4 w-4" />
              Back
            </Button>
          )}
          <Button
            size="sm"
            {...primaryProps}
            className="group h-10 px-7 text-[13px] gap-2 font-black bg-white hover:bg-indigo-50 text-slate-950 rounded-full shadow-[0_0_20px_rgba(255,255,255,0.15)] transition-all hover:scale-105"
          >
            {isLastStep ? (
              "Complete Setup"
            ) : (
              <>
                Continue 
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
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
      target: "#tour-dashboard-semester",
      title: "Intelligent Examination Planning",
      content: (
        <p>
          Process student nominal rolls and let the allocation engine compute optimal, collision-free seating arrangements automatically.
        </p>
      ),
      placement: "right-start",
      skipBeacon: true,
      data: { icon: <Component className="h-5 w-5 text-indigo-300" /> }
    },
    {
      target: "#tour-dashboard-duty",
      title: "Algorithmic Duty Allocation",
      content: (
        <p>
          The system automatically balances departmental workloads to ensure perfect equity across all faculty members and invigilators.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Briefcase className="h-5 w-5 text-purple-300" /> }
    },
    {
      target: "#tour-dashboard-appointments",
      title: "Automated Appointment Orders",
      content: (
        <p>
          Instantly generate professional, print-ready appointment orders for Invigilators, Lab Incharges, and Examiners with one click.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Layers className="h-5 w-5 text-blue-300" /> }
    },
    {
      target: "#tour-dashboard-claims",
      title: "Claims Processing Engine",
      content: (
        <p>
          Seamlessly ingest valuation claims via Excel upload or direct WhatsApp public links. Let the engine automatically calculate TA, DA, and total remuneration.
        </p>
      ),
      placement: "right-start",
      data: { icon: <Database className="h-5 w-5 text-emerald-300" /> }
    },
    {
      target: "#tour-dashboard-reports",
      title: "NAAC/NBA Accreditation",
      content: (
        <p>
          Generate instantly certified, system-verified compliance evidence packs for regulatory audits and inspections.
        </p>
      ),
      placement: "right-start",
      data: { icon: <ShieldCheck className="h-5 w-5 text-amber-300" /> }
    },
    {
      target: "#tour-dashboard-settings",
      title: "System Configuration",
      content: (
        <p>
          Manage institutional parameters, update your college branding assets, and configure system-wide remuneration baseline metrics.
        </p>
      ),
      placement: "right-end",
      data: { icon: <Settings2 className="h-5 w-5 text-rose-300" /> }
    },
    {
      target: ".tour-header-controls",
      title: "Command Center Initialized",
      content: (
        <p>
          The HallSync platform is fully operational. You can trigger this interactive guide at any time from the primary header.
        </p>
      ),
      placement: "bottom-end",
      data: { icon: <Hexagon className="h-5 w-5 text-cyan-300" /> }
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
