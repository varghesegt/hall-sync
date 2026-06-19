import { cn } from "@/lib/utils";
import type { AllocationStatus } from "@/api/allocationApi";

const statusConfig: Record<AllocationStatus, { label: string; className: string }> = {
  NOT_STARTED: { label: "Not Started", className: "bg-muted text-muted-foreground border-border" },
  RUNNING: { label: "Running", className: "bg-warning/10 text-warning border-warning/30" },
  ACTIVE: { label: "Complete", className: "bg-success/10 text-success border-success/30" },
  FAILED: { label: "Failed", className: "bg-destructive/10 text-destructive border-destructive/30" },
};

interface StatusBadgeProps {
  status: AllocationStatus;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = statusConfig[status];
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium",
        config.className,
        className
      )}
    >
      {status === "RUNNING" && (
        <span className="mr-1.5 h-1.5 w-1.5 animate-pulse rounded-full bg-warning" />
      )}
      {config.label}
    </span>
  );
}
