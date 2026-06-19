import { useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useMutation } from "@tanstack/react-query";
import { createInternalSession } from "@/api/internalApi";
import type { SessionRequest, SessionResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { AlertCircle, CheckCircle2, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

interface InternalSessionCardProps {
  fileId: string | null;
  onSuccess: (sessionId: string, totalStudents: number) => void;
  sessionId: string | null;
}

export function InternalSessionCard({ fileId, onSuccess, sessionId }: InternalSessionCardProps) {
  const [examName, setExamName] = useState("");
  const [examDate, setExamDate] = useState("");
  const [seasonId, setSeasonId] = useState("");
  const [sessionType, setSessionType] = useState("FN");
  const [validationError, setValidationError] = useState<string | null>(null);

  const session = useMutation<SessionResponse, ApiError, SessionRequest>({
    mutationFn: createInternalSession,
  });

  const disabled = !fileId;
  const isComplete = !!sessionId;

  const handleSubmit = () => {
    setValidationError(null);
    if (!examName.trim()) { setValidationError("Exam name is required."); return; }
    if (!examDate) { setValidationError("Exam date is required."); return; }
    if (!seasonId.trim()) { setValidationError("Season ID is required."); return; }

    session.mutate(
      { fileId: fileId!, examName: examName.trim(), examDate, seasonId: seasonId.trim(), session: sessionType },
      { onSuccess: (data: any) => onSuccess(data.examSessionId, data.totalStudents) }
    );
  };

  return (
    <Card className={cn(disabled && "opacity-60", isComplete && "border-emerald-300/50")}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base">2. Create Exam Session</CardTitle>
            <CardDescription>
              {disabled ? "Complete upload first" : "Configure internal exam session details"}
            </CardDescription>
          </div>
          {isComplete && <CheckCircle2 className="h-5 w-5 text-emerald-500" />}
        </div>
      </CardHeader>
      <CardContent>
        {!isComplete ? (
          <fieldset disabled={disabled} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="intExamName">Exam Name</Label>
              <Input id="intExamName" placeholder="e.g. Internal Assessment 1" value={examName} onChange={(e) => setExamName(e.target.value)} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="intExamDate">Exam Date</Label>
                <Input id="intExamDate" type="date" value={examDate} onChange={(e) => setExamDate(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="intSeasonId">Season ID</Label>
                <Input id="intSeasonId" placeholder="e.g. 2024-IA1" value={seasonId} onChange={(e) => setSeasonId(e.target.value)} />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Exam Session</Label>
              <Tabs value={sessionType} onValueChange={setSessionType} className="w-full">
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="FN" className="flex items-center gap-2">
                    <Clock className="h-3.5 w-3.5" /> Forenoon (FN)
                  </TabsTrigger>
                  <TabsTrigger value="AN" className="flex items-center gap-2">
                    <Clock className="h-3.5 w-3.5" /> Afternoon (AN)
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
                      <p key={field} className="mt-1"><strong>{field}:</strong> {msgs.join(", ")}</p>
                    ))}
                </div>
              </div>
            )}

            <Button onClick={handleSubmit} disabled={session.isPending} className="w-full bg-emerald-600 hover:bg-emerald-700">
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
          <div className="rounded-md border border-emerald-200 bg-emerald-50/50 p-3 text-sm">
            <p><span className="text-muted-foreground">Session:</span> <span className="font-mono">{sessionId!.slice(0, 8)}…</span></p>
            <p><span className="text-muted-foreground">Exam:</span> {examName}</p>
            <p>
              <span className="text-muted-foreground">Session:</span>{" "}
              <span className={cn("inline-flex items-center rounded-sm px-1.5 py-0.5 text-xs font-medium",
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
