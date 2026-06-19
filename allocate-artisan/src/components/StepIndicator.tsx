import { cn } from "@/lib/utils";
import { Check } from "lucide-react";

interface Step {
  label: string;
  completed: boolean;
  active: boolean;
}

interface StepIndicatorProps {
  steps: Step[];
}

export function StepIndicator({ steps }: StepIndicatorProps) {
  return (
    <div className="flex items-center gap-2">
      {steps.map((step, i) => (
        <div key={step.label} className="flex items-center gap-2">
          <div
            className={cn(
              "flex h-6 w-6 items-center justify-center rounded-full border text-xs font-medium",
              step.completed
                ? "border-success bg-success text-success-foreground"
                : step.active
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-muted text-muted-foreground"
            )}
          >
            {step.completed ? <Check className="h-3.5 w-3.5" /> : i + 1}
          </div>
          <span
            className={cn(
              "text-sm",
              step.active ? "font-medium text-foreground" : "text-muted-foreground"
            )}
          >
            {step.label}
          </span>
          {i < steps.length - 1 && (
            <div className={cn("h-px w-8", step.completed ? "bg-success" : "bg-border")} />
          )}
        </div>
      ))}
    </div>
  );
}
