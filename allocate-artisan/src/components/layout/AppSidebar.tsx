import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { Link, useLocation } from "react-router-dom";
import {
  BarChart3,
  CalendarDays,
  FileText,
  Users,
  ShieldAlert,
  Archive,
  Settings,
  GraduationCap,
  IndianRupee,
  MapPin,
  User,
  Building,
  Briefcase,
  Activity,
  ShieldCheck,
  TrendingUp,
  PackageCheck,
  Mail,
  Upload,
  Target,
  GitCommit,
  LayoutGrid
} from "lucide-react";

export function AppSidebar({ settings }: { settings?: any }) {
  const location = useLocation();
  const userEmail = localStorage.getItem("user_email") || "admin@hallsync.com";

  const menuGroups = [
    {
      label: "Overview",
      items: [
        { title: "Command Center", url: "/dashboard", icon: LayoutGrid }
      ]
    },
    {
      label: "Exam Planning",
      items: [
        { title: "Semester Exams", url: "/dashboard/semester", icon: GraduationCap },
        { title: "Internal Exams", url: "/dashboard/internal", icon: FileText }
      ]
    },
    {
      label: "Duty & Allocations",
      items: [
        { title: "Semester Duty Allocation", url: "/dashboard/duty", icon: CalendarDays },
        { title: "Internal Exam Duties", url: "/dashboard/internal-duty", icon: ShieldCheck },
        { title: "Appointment Orders", url: "/dashboard/appointments", icon: FileText },
        { title: "Communications", url: "/dashboard/communications", icon: Mail }
      ]
    },

    {
      label: "Finance & Claims",
      items: [
        { title: "Theory Remuneration", url: "/dashboard/claims", icon: IndianRupee },
        { title: "Lab Claims", url: "/dashboard/lab-claims", icon: Briefcase }
      ]
    },
    {
      label: "Audit & Compliance",
      items: [
        { title: "Accreditation", url: "/dashboard/reports", icon: Building },
        { title: "Audit Center", url: "/dashboard/audit-center", icon: ShieldCheck }
      ]
    },
    {
      label: "Masters & Settings",
      items: [
        { title: "Faculty Master", url: "/dashboard/faculty", icon: Users },
        { title: "Hall Master", url: "/dashboard/halls", icon: Building },
        { title: "College Distance", url: "/dashboard/colleges", icon: MapPin },
        { title: "System Settings", url: "/dashboard/settings", icon: Settings }
      ]
    }
  ];

  const logoSrc = settings?.isConfigured ? "/api/v1/settings/logo" : "/logo.png";
  const collegeName = settings?.collegeName || "HallSync";
  const isDefaultLogo = !settings?.isConfigured;

  const renderMenuButton = (item: any, isExact: boolean = false) => {
    const isActive = isExact 
      ? location.pathname === item.url 
      : location.pathname.startsWith(item.url) && (item.url !== "/dashboard" || location.pathname === "/dashboard");

    const targetId = `tour-${item.url.replace(/\//g, "-").replace(/^-/, "")}`;

    return (
      <SidebarMenuItem key={item.title} id={targetId} className="tour-menu-item mb-1">
        <SidebarMenuButton
          asChild
          isActive={isActive}
          tooltip={item.title}
          className={`h-11 px-3.5 rounded-lg transition-all duration-200 group relative overflow-hidden ${
            isActive 
              ? "bg-indigo-50 text-indigo-700 font-bold" 
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 font-medium"
          }`}
        >
          <Link to={item.url} className="flex items-center gap-3.5 w-full h-full relative z-10 group-data-[collapsible=icon]:justify-center">
            <div className={`p-1.5 rounded-md transition-all duration-200 ${isActive ? "bg-indigo-600 text-white group-data-[collapsible=icon]:scale-100" : "text-slate-400 group-hover:text-indigo-600"}`}>
              <item.icon className="h-4 w-4 shrink-0" strokeWidth={isActive ? 2.5 : 2} />
            </div>
            <span className="text-[14px] tracking-tight relative z-10 group-data-[collapsible=icon]:hidden">{item.title}</span>
          </Link>
        </SidebarMenuButton>
      </SidebarMenuItem>
    );
  };

  return (
    <Sidebar variant="sidebar" collapsible="icon" className="border-r border-slate-200 bg-white shadow-sm transition-all duration-300">
      <SidebarHeader className="h-[72px] flex items-center justify-center px-5 group-data-[collapsible=icon]:px-0 border-b border-slate-200 bg-white">
        <div className="flex items-center gap-4 w-full group-data-[collapsible=icon]:justify-center">
          <div className="h-10 w-10 rounded-lg bg-indigo-600 flex items-center justify-center shrink-0 relative overflow-hidden group">
            <div className="absolute inset-0 bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
            {isDefaultLogo ? (
              <Briefcase className="h-5 w-5 text-white relative z-10" />
            ) : (
              <img 
                src={logoSrc} 
                alt="Logo" 
                className="w-full h-full object-cover rounded-2xl relative z-10"
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display = 'none';
                }}
              />
            )}
          </div>
          <div className="flex flex-col group-data-[collapsible=icon]:hidden overflow-hidden">
            <span className="font-extrabold text-[17px] tracking-tight text-slate-900 leading-none truncate bg-clip-text text-transparent bg-gradient-to-br from-slate-900 to-slate-700 pb-0.5" title={collegeName}>
              {isDefaultLogo ? "HallSync" : collegeName}
            </span>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent className="px-4 py-8 gap-8 group-data-[collapsible=icon]:px-2 group-data-[collapsible=icon]:py-4 group-data-[collapsible=icon]:gap-4 scrollbar-hide">
        {menuGroups.map((group, idx) => (
          <SidebarGroup key={idx} className="p-0 m-0 group-data-[collapsible=icon]:mt-2">
            <SidebarGroupLabel className="h-6 px-3 mb-2 text-[11px] font-black text-slate-400 uppercase tracking-[0.15em] bg-transparent flex items-center gap-2 group-data-[collapsible=icon]:hidden">
              {group.label}
              <div className="h-[1px] flex-1 bg-gradient-to-r from-slate-200 to-transparent" />
            </SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu className="gap-1.5">
                {group.items.map(item => renderMenuButton(item, item.url === "/dashboard"))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>

      <SidebarFooter className="border-t border-slate-200 p-5 group-data-[collapsible=icon]:p-2 bg-white">
        <div className="flex items-center gap-3.5 w-full group-data-[collapsible=icon]:hidden bg-slate-50 border border-slate-200 rounded-lg p-3 hover:bg-slate-100 transition-all cursor-pointer">
          <div className="h-10 w-10 rounded-md bg-indigo-100 flex items-center justify-center shrink-0 border border-indigo-200">
            <User className="h-5 w-5 text-indigo-600" />
          </div>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-[13px] font-bold text-slate-900 truncate leading-tight">{userEmail}</span>
            <span className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Administrator</span>
          </div>
        </div>
        <div className="hidden group-data-[collapsible=icon]:flex items-center justify-center w-full">
          <div className="h-10 w-10 rounded-md bg-indigo-100 flex items-center justify-center shrink-0 border border-indigo-200">
            <User className="h-5 w-5 text-indigo-600" />
          </div>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
