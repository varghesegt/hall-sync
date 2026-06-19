import { useState, useEffect } from "react";
import { ServerCrash, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

export function NetworkOfflineOverlay() {
  const [isOffline, setIsOffline] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);

  useEffect(() => {
    const handleOffline = () => setIsOffline(true);
    const handleOnline = () => {
      setIsRetrying(false);
      setIsOffline(false);
    };

    window.addEventListener("hallsync:network_offline", handleOffline);
    window.addEventListener("hallsync:network_online", handleOnline);

    return () => {
      window.removeEventListener("hallsync:network_offline", handleOffline);
      window.removeEventListener("hallsync:network_online", handleOnline);
    };
  }, []);

  if (!isOffline) return null;

  return (
    <div className="fixed inset-0 z-[100000] flex items-center justify-center bg-slate-900/40 backdrop-blur-md animate-in fade-in duration-300">
      <div className="bg-white rounded-3xl shadow-2xl max-w-sm w-full p-8 text-center border border-slate-100 mx-4 transform transition-all animate-in zoom-in-95 duration-500">
        <div className="mx-auto w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mb-6 relative">
          <div className="absolute inset-0 bg-rose-200 rounded-full animate-ping opacity-20" />
          <ServerCrash className="h-8 w-8 text-rose-600 relative z-10" />
        </div>
        
        <h2 className="text-xl font-bold text-slate-900 mb-2 tracking-tight">
          Connection Lost
        </h2>
        <p className="text-sm text-slate-500 mb-8 leading-relaxed">
          We cannot reach the HallSync server. Your unsaved changes are paused. Please wait while we reconnect.
        </p>

        <Button 
          onClick={() => {
            setIsRetrying(true);
            window.dispatchEvent(new Event("hallsync:network_retry"));
          }}
          disabled={isRetrying}
          className="w-full bg-slate-900 hover:bg-slate-800 text-white rounded-full h-12 font-semibold shadow-lg shadow-slate-900/20"
        >
          {isRetrying ? (
            <>
              <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
              Reconnecting...
            </>
          ) : (
            "Try Again Now"
          )}
        </Button>
      </div>
    </div>
  );
}
