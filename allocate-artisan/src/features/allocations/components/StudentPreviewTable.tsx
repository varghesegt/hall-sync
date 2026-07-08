import { useState, useMemo } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useStudentPreview } from "@/hooks/useStudentPreview";
import { AlertCircle, Search, Users, ChevronDown, ChevronUp } from "lucide-react";
import { cn } from "@/lib/utils";

interface StudentPreviewTableProps {
  fileId: string | null;
}

const PAGE_SIZE = 20;

export function StudentPreviewTable({ fileId }: StudentPreviewTableProps) {
  const { data, isLoading, isError, error } = useStudentPreview(fileId);
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(true);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);

  const departments = useMemo(() => {
    if (!data?.students) return [];
    return [...new Set(data.students.map((s) => s.department))].sort();
  }, [data]);

  const deptStats = useMemo(() => {
    if (!data?.students) return {};
    const stats: Record<string, { count: number; classes: Set<string> }> = {};
    for (const s of data.students) {
      if (!stats[s.department]) stats[s.department] = { count: 0, classes: new Set() };
      stats[s.department].count++;
      stats[s.department].classes.add(s.className || s.department);
    }
    return stats;
  }, [data]);

  const filtered = useMemo(() => {
    if (!data?.students) return [];
    let list = data.students;
    if (deptFilter) list = list.filter((s) => s.department === deptFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (s) =>
          s.registerNumber.toLowerCase().includes(q) ||
          s.name.toLowerCase().includes(q) ||
          s.className.toLowerCase().includes(q) ||
          (s.subjectName && s.subjectName.toLowerCase().includes(q)) ||
          (s.subjectCode && s.subjectCode.toLowerCase().includes(q))
      );
    }
    return list;
  }, [data, search, deptFilter]);

  const visible = filtered.slice(0, visibleCount);

  if (!fileId) return null;

  return (
    <Card className="transition-all duration-500 overflow-hidden glass-panel">
      <CardHeader
        className="cursor-pointer select-none bg-slate-50/50 border-b border-slate-100/50 pb-5 hover:bg-slate-100/30 transition-colors"
        onClick={() => setExpanded((p) => !p)}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-black tracking-tight text-slate-900 flex items-center gap-2">
                Student Roster Preview
                {data && (
                  <Badge variant="secondary" className="ml-2 font-black uppercase tracking-wider text-[10px] bg-slate-200 text-slate-700">
                    {data.totalStudents} Candidates
                  </Badge>
                )}
              </CardTitle>
              <CardDescription className="text-xs font-semibold uppercase tracking-wider mt-1 text-slate-500">
                {isLoading
                  ? "Loading parsed student data…"
                  : data
                  ? `${departments.length} departments · ${Object.values(deptStats).reduce((a, d) => a + d.classes.size, 0)} distinct classes`
                  : "Preview ingested student data payload"}
              </CardDescription>
            </div>
          </div>
          <div className="p-1.5 rounded-lg bg-slate-100/50 text-slate-400 group-hover:bg-slate-200 transition-colors">
            {expanded ? (
              <ChevronUp className="h-5 w-5" />
            ) : (
              <ChevronDown className="h-5 w-5" />
            )}
          </div>
        </div>
      </CardHeader>

      {expanded && (
        <CardContent className="space-y-3">
          {/* Loading */}
          {isLoading && (
            <div className="flex items-center gap-2 rounded-md border bg-muted/50 p-4 text-sm text-muted-foreground">
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-muted-foreground" />
              Fetching student data…
            </div>
          )}

          {/* Error */}
          {isError && (
            <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{error?.message || "Failed to load student preview."}</span>
            </div>
          )}

          {/* Data */}
          {data && (
            <>
              {/* Department chips */}
              <div className="flex flex-wrap gap-1.5">
                <button
                  onClick={() => setDeptFilter(null)}
                  className={cn(
                    "rounded-md border px-2.5 py-1 text-xs font-medium transition-colors",
                    !deptFilter
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border bg-background text-muted-foreground hover:bg-muted"
                  )}
                >
                  All ({data.totalStudents})
                </button>
                {departments.map((dept) => (
                  <button
                    key={dept}
                    onClick={() => setDeptFilter(deptFilter === dept ? null : dept)}
                    className={cn(
                      "rounded-md border px-2.5 py-1 text-xs font-medium transition-colors",
                      deptFilter === dept
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-background text-muted-foreground hover:bg-muted"
                    )}
                  >
                    {dept} ({deptStats[dept]?.count})
                  </button>
                ))}
              </div>

              {/* Search */}
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by register number, name, class, or subject…"
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setVisibleCount(PAGE_SIZE);
                  }}
                  className="pl-9 h-9 text-sm"
                />
              </div>

              {/* Results count */}
              <p className="text-xs text-muted-foreground">
                Showing {Math.min(visibleCount, filtered.length)} of {filtered.length} students
              </p>

              {/* Table */}
              <div className="rounded-md border max-h-[400px] overflow-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[60px] text-xs">#</TableHead>
                      <TableHead className="text-xs">Register Number</TableHead>
                      <TableHead className="text-xs">Name</TableHead>
                      <TableHead className="text-xs">Class</TableHead>
                      <TableHead className="text-xs">Subject</TableHead>
                      <TableHead className="text-xs">Subject Code</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {visible.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                          No students match the current filter.
                        </TableCell>
                      </TableRow>
                    ) : (
                      visible.map((s, i) => (
                        <TableRow key={`${s.registerNumber}-${i}`}>
                          <TableCell className="text-xs text-muted-foreground font-mono">
                            {i + 1}
                          </TableCell>
                          <TableCell className="text-xs font-mono">{s.registerNumber}</TableCell>
                          <TableCell className="text-xs">{s.name.replace(/^\d+[.)]?\s*/, '')}</TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-xs font-normal">
                              {s.className}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-xs font-medium">
                            {s.subjectName || <span className="text-muted-foreground italic">None</span>}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground font-mono">
                            {s.subjectCode || "-"}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>

              {/* Load more */}
              {visibleCount < filtered.length && (
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full text-xs"
                  onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
                >
                  Show more ({filtered.length - visibleCount} remaining)
                </Button>
              )}
            </>
          )}
        </CardContent>
      )}
    </Card>
  );
}
