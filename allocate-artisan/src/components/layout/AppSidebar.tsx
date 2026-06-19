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
      label: "Executive Overview",
      items: [
        { title: "COE Dashboard", url: "/dashboard", icon: BarChart3 },
        { title: "Command Center", url: "/dashboard/command-center", icon: Activity },
        { title: "Management KPIs", url: "/dashboard/dashboard", icon: Target },
        { title: "Exam Lifecycle", url: "/dashboard/lifecycle", icon: GitCommit }
      ]
    },
    {
      label: "Exam Planning",
      items: [
        { title: "Semester Exams", url: "/dashboard/semester", icon: GraduationCap },
        { title: "Internal Exams", url: "/dashboard/internal", icon: FileText },
        { title: "Floor Plan Editor", url: "/dashboard/floor-plan-select", icon: LayoutGrid },
        { title: "Data Import", url: "/dashboard/import", icon: Upload }
      ]
    },
    {
      label: "Duty & Allocations",
      items: [
        { title: "Duty Allocation", url: "/dashboard/duty", icon: CalendarDays },
        { title: "Faculty Workload", url: "/dashboard/workload", icon: TrendingUp },
        { title: "Communications", url: "/dashboard/communications", icon: Mail }
      ]
    },
    {
      label: "Live Operations",
      items: [
        { title: "QP Security", url: "/dashboard/qp-security", icon: PackageCheck },
        { title: "Malpractice", url: "/dashboard/malpractice", icon: ShieldAlert }
      ]
    },
    {
      label: "Finance & Claims",
      items: [
        { title: "Remuneration", url: "/dashboard/claims", icon: IndianRupee }
      ]
    },
    {
      label: "Audit & Compliance",
      items: [
        { title: "Compliance Vault", url: "/dashboard/compliance-vault", icon: Archive },
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
      <SidebarMenuItem key={item.title} id={targetId} className="tour-menu-item">
        <SidebarMenuButton
          asChild
          isActive={isActive}
          tooltip={item.title}
          className={`h-10 px-3 rounded-xl transition-all duration-300 ${
            isActive 
              ? "bg-primary/10 text-primary font-bold shadow-[0_2px_10px_rgba(109,40,217,0.1)]" 
              : "text-slate-500 hover:bg-slate-100/50 hover:text-slate-900 font-medium"
          }`}
        >
          <Link to={item.url} className="flex items-center gap-3 w-full">
            <div className={`p-1.5 rounded-lg transition-colors ${isActive ? "bg-primary/20 text-primary" : "text-slate-400 group-hover:bg-slate-200 group-hover:text-slate-600"}`}>
              <item.icon className="h-4 w-4 shrink-0" strokeWidth={isActive ? 2.5 : 2} />
            </div>
            <span className="text-sm tracking-tight">{item.title}</span>
          </Link>
        </SidebarMenuButton>
      </SidebarMenuItem>
    );
  };

  return (
    <Sidebar variant="sidebar" collapsible="icon" className="border-r border-white/40 bg-white/40 backdrop-blur-3xl shadow-[0_0_40px_-15px_rgba(0,0,0,0.05)]">
      <SidebarHeader className="h-20 flex items-center justify-center px-4 border-b border-white/20">
        <div className="flex items-center gap-3 w-full">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-primary to-purple-600 flex items-center justify-center shadow-lg shadow-primary/20 shrink-0">
            {isDefaultLogo ? (
              <Briefcase className="h-4 w-4 text-white" />
            ) : (
              <img 
                src={logoSrc} 
                alt="Logo" 
                className="w-full h-full object-cover rounded-xl"
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display = 'none';
                }}
              />
            )}
          </div>
          <div className="flex flex-col group-data-[collapsible=icon]:hidden overflow-hidden">
            <span className="font-black text-[16px] tracking-tight text-slate-900 leading-none truncate" title={collegeName}>
              {isDefaultLogo ? "HallSync" : collegeName}
            </span>
            <span className="text-[10px] font-bold text-primary uppercase tracking-widest mt-1">Enterprise</span>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent className="px-3 py-6 gap-6 scrollbar-hide">
        {menuGroups.map((group, idx) => (
          <SidebarGroup key={idx} className="p-0 m-0">
            <SidebarGroupLabel className="h-6 px-3 text-[10px] font-bold text-slate-400 uppercase tracking-widest bg-transparent">
              {group.label}
            </SidebarGroupLabel>
            <SidebarGroupContent className="mt-2">
              <SidebarMenu className="gap-1">
                {group.items.map(item => renderMenuButton(item, item.url === "/dashboard"))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>

      <SidebarFooter className="border-t border-white/20 p-4 bg-white/20 backdrop-blur-md">
        <div className="flex items-center gap-3 w-full group-data-[collapsible=icon]:hidden">
          <div className="h-10 w-10 rounded-full bg-gradient-to-br from-slate-200 to-slate-300 flex items-center justify-center shadow-inner shrink-0 border border-white/60">
            <User className="h-4 w-4 text-slate-600" />
          </div>
          <div className="flex flex-col min-w-0">
            <span className="text-sm font-bold text-slate-900 truncate leading-tight">{userEmail}</span>
            <span className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider mt-0.5">Administrator</span>
          </div>
        </div>
        <div className="hidden group-data-[collapsible=icon]:flex items-center justify-center w-full">
          <div className="h-8 w-8 rounded-full bg-slate-200 flex items-center justify-center shrink-0">
            <User className="h-4 w-4 text-slate-600" />
          </div>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
