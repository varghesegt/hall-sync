import { useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useSession } from "@/hooks/useSession";
import { AlertCircle, CheckCircle2, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

interface SessionCardProps {
  fileId: string | null;
  onSuccess: (sessionId: string, totalStudents: number) => void;
  sessionId: string | null;
}

export function SessionCard({ fileId, onSuccess, sessionId }: SessionCardProps) {
  const [examName, setExamName] = useState("");
  const [examDate, setExamDate] = useState("");
  const [seasonId, setSeasonId] = useState("");
  const [sessionType, setSessionType] = useState("FN");
  const [validationError, setValidationError] = useState<string | null>(null);
  const session = useSession();

  const disabled = !fileId;
  const isComplete = !!sessionId;

  const handleSubmit = () => {
    setValidationError(null);
    if (!examName.trim()) {
      setValidationError("Exam name is required.");
      return;
    }
    if (!examDate) {
      setValidationError("Exam date is required.");
      return;
    }
    if (!seasonId.trim()) {
      setValidationError("Season ID is required.");
      return;
    }

    session.mutate(
      { fileId: fileId!, examName: examName.trim(), examDate, seasonId: seasonId.trim(), session: sessionType },
      { onSuccess: (data: any) => onSuccess(data.examSessionId, data.totalStudents) }
    );
  };

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden", disabled ? "opacity-50 grayscale-[0.2]" : "", isComplete ? "border-success/40 shadow-success/10 shadow-lg bg-white/90" : "glass-panel")}>
      <CardHeader className="bg-slate-50/50 border-b border-slate-100/50 pb-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={cn("p-2 rounded-xl flex items-center justify-center shrink-0 transition-colors", isComplete ? "bg-success/10 text-success" : "bg-primary/10 text-primary")}>
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-black tracking-tight text-slate-900">2. Configure Exam Parameters</CardTitle>
              <CardDescription className="text-xs font-semibold uppercase tracking-wider mt-1 text-slate-500">
                {disabled ? "Awaiting data ingestion..." : "Set examination meta-data"}
              </CardDescription>
            </div>
          </div>
          {isComplete && <CheckCircle2 className="h-6 w-6 text-success animate-in zoom-in duration-500" />}
        </div>
      </CardHeader>
      <CardContent className="pt-6">
        {!isComplete ? (
          <fieldset disabled={disabled} className="space-y-5">
            <div className="space-y-1.5">
              <Label htmlFor="examName">Exam Name</Label>
              <Input
                id="examName"
                placeholder="e.g. Final Mathematics Exam"
                value={examName}
                onChange={(e) => setExamName(e.target.value)}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="examDate">Exam Date</Label>
                <Input
                  id="examDate"
                  type="date"
                  value={examDate}
                  onChange={(e) => setExamDate(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="seasonId">Season ID</Label>
                <Input
                  id="seasonId"
                  placeholder="e.g. 2024-S1"
                  value={seasonId}
                  onChange={(e) => setSeasonId(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Exam Session</Label>
              <Tabs
                value={sessionType}
                onValueChange={setSessionType}
                className="w-full"
              >
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="FN" className="flex items-center gap-2">
                    <Clock className="h-3.5 w-3.5" />
                    Forenoon (FN)
                  </TabsTrigger>
                  <TabsTrigger value="AN" className="flex items-center gap-2">
                    <Clock className="h-3.5 w-3.5" />
                    Afternoon (AN)
                  </TabsTrigger>
                </TabsList>
              </Tabs>
            </div>

            {(validationError || session.isError) && (
              <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <div>
                  <p>{validationError || session.error?.message}</p>
                  {session.error?.details &&
                    Object.entries(session.error.details).map(([field, msgs]) => (
                      <p key={field} className="mt-1">
                        <strong>{field}:</strong> {msgs.join(", ")}
                      </p>
                    ))}
                </div>
              </div>
            )}

            <Button onClick={handleSubmit} disabled={session.isPending} className="w-full">
              {session.isPending ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                  Creating…
                </>
              ) : (
                "Create Session"
              )}
            </Button>
          </fieldset>
        ) : (
          <div className="rounded-md border bg-muted/50 p-3 text-sm">
            <p>
              <span className="text-muted-foreground">Session:</span>{" "}
              <span className="font-mono">{sessionId!.slice(0, 8)}…</span>
            </p>
            <p>
              <span className="text-muted-foreground">Exam:</span> {examName}
            </p>
            <p>
              <span className="text-muted-foreground">Session:</span>{" "}
              <span className={cn(
                "inline-flex items-center rounded-sm px-1.5 py-0.5 text-xs font-medium",
                sessionType === "FN" ? "bg-blue-100 text-blue-700" : "bg-orange-100 text-orange-700"
              )}>
                {sessionType === "FN" ? "Forenoon (FN)" : "Afternoon (AN)"}
              </span>
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
