import { LucideIcon } from "lucide-react";

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  icon?: LucideIcon;
}

export function PageHeader({ title, subtitle, icon: Icon }: PageHeaderProps) {
  return (
    <div className="flex flex-col mb-8 animate-in fade-in slide-in-from-bottom-2 duration-500">
      <div className="flex items-center gap-3">
        {Icon && (
          <div className="p-2.5 bg-blue-100 rounded-xl text-blue-600 shadow-sm border border-blue-200">
            <Icon className="w-6 h-6" />
          </div>
        )}
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">{title}</h1>
          {subtitle && (
            <p className="text-slate-500 mt-1 font-medium">{subtitle}</p>
          )}
        </div>
      </div>
    </div>
  );
}
