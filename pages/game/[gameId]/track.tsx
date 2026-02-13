import { useRouter } from "next/router";
import { useEffect, useState } from "react";
import axios from "axios";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;

type KpiType = "COUNTER" | "TOGGLE";

interface KpiDefinition {
  gameId: string;
  kpiId: string;
  label: string;
  type: KpiType;
}

export default function TrackGame() {
  const router = useRouter();
  const { gameId } = router.query as { gameId?: string };

  const [kpis, setKpis] = useState<KpiDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [eventError, setEventError] = useState<string | null>(null);

  useEffect(() => {
    if (!gameId || !API_BASE) return;
    const fetchKpis = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await axios.get<{ kpis: KpiDefinition[] }>(
          `${API_BASE}/games/${gameId}/kpis`
        );
        setKpis(res.data.kpis);
      } catch (e: any) {
        setError(
          e?.response?.data?.message || "Failed to load KPI definitions"
        );
      } finally {
        setLoading(false);
      }
    };
    fetchKpis();
  }, [gameId]);

  const sendCounterEvent = async (kpiId: string, delta: number) => {
    if (!API_BASE || !gameId) return;
    setEventError(null);
    try {
      await axios.post(`${API_BASE}/games/${gameId}/events`, {
        kpiId,
        delta
      });
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to record event. Try again.";
      setEventError(msg);
      console.error("Failed to record event", e);
      setTimeout(() => setEventError(null), 5000);
    }
  };

  const sendToggleEvent = async (kpiId: string, value: boolean) => {
    if (!API_BASE || !gameId) return;
    setEventError(null);
    try {
      await axios.post(`${API_BASE}/games/${gameId}/events`, {
        kpiId,
        toggleValue: value
      });
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to record toggle. Try again.";
      setEventError(msg);
      console.error("Failed to record toggle", e);
      setTimeout(() => setEventError(null), 5000);
    }
  };

  // For toggles: simple in-memory state so player sees feedback; source of truth is backend
  const [toggleState, setToggleState] = useState<Record<string, boolean>>({});

  const handleToggleTap = async (kpiId: string) => {
    const next = !toggleState[kpiId];
    setToggleState((prev) => ({ ...prev, [kpiId]: next }));
    await sendToggleEvent(kpiId, next);
  };

  const renderKpiButton = (kpi: KpiDefinition) => {
    if (kpi.type === "COUNTER") {
      let pressTimer: NodeJS.Timeout | null = null;
      const longPressMs = 450;

      const onPointerDown = () => {
        pressTimer = setTimeout(() => {
          sendCounterEvent(kpi.kpiId, -1);
          pressTimer = null;
        }, longPressMs);
      };

      const onPointerUp = () => {
        if (pressTimer) {
          clearTimeout(pressTimer);
          pressTimer = null;
          sendCounterEvent(kpi.kpiId, +1);
        }
      };

      const onPointerLeave = () => {
        if (pressTimer) {
          clearTimeout(pressTimer);
          pressTimer = null;
        }
      };

      return (
        <button
          key={kpi.kpiId}
          className="h-24 rounded-2xl bg-emerald-500 hover:bg-emerald-400 active:bg-emerald-300 text-lg font-semibold text-slate-900 shadow-md touch-manipulation flex items-center justify-center text-center px-2"
          onPointerDown={onPointerDown}
          onPointerUp={onPointerUp}
          onPointerLeave={onPointerLeave}
        >
          {kpi.label}
          <span className="block text-xs mt-1 text-emerald-900">
            Tap = +1 / Long press = -1
          </span>
        </button>
      );
    }

    const active = !!toggleState[kpi.kpiId];
    return (
      <button
        key={kpi.kpiId}
        onClick={() => handleToggleTap(kpi.kpiId)}
        className={`h-24 rounded-2xl text-lg font-semibold shadow-md touch-manipulation flex items-center justify-center text-center px-2 border-2 ${
          active
            ? "bg-amber-400 text-slate-900 border-amber-300"
            : "bg-slate-800 text-slate-100 border-slate-600"
        }`}
      >
        {kpi.label}
        <span className="block text-xs mt-1 text-slate-300">
          {active ? "YES" : "NO"}
        </span>
      </button>
    );
  };

  if (!API_BASE) {
    return (
      <main className="min-h-screen flex items-center justify-center px-4">
        <p className="text-red-400 text-sm">
          NEXT_PUBLIC_API_BASE_URL is not configured.
        </p>
      </main>
    );
  }

  return (
    <main className="min-h-screen flex flex-col px-4 py-4">
      <header className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-xl font-semibold">Player Tracking</h1>
          <p className="text-xs text-slate-400 font-mono">
            Game: {gameId || "..."}
          </p>
        </div>
      </header>

      {loading && <p className="text-sm text-slate-300 mb-2">Loading KPIs…</p>}
      {error && <p className="text-sm text-red-400 mb-2">{error}</p>}
      {eventError && (
        <p className="text-sm text-amber-400 mb-2" role="alert">
          {eventError}
        </p>
      )}

      <section className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
        {kpis.map((k) => renderKpiButton(k))}
      </section>
    </main>
  );
}

