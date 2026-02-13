import { useRouter } from "next/router";
import { useEffect, useState } from "react";
import axios from "axios";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;

type KpiType = "COUNTER" | "TOGGLE";

interface KpiSummary {
  kpiId: string;
  label: string;
  total?: number;
  value?: boolean;
}

interface GameSummaryResponse {
  gameId: string;
  kpis: KpiSummary[];
}

export default function Dashboard() {
  const router = useRouter();
  const { gameId } = router.query as { gameId?: string };

  const [summary, setSummary] = useState<GameSummaryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pollIntervalSec, setPollIntervalSec] = useState(10);

  useEffect(() => {
    if (!gameId || !API_BASE) return;

    let cancelled = false;

    const loadSummary = async () => {
      if (!gameId || !API_BASE) return;
      try {
        setLoading(true);
        const res = await axios.get<GameSummaryResponse>(
          `${API_BASE}/games/${gameId}/summary`
        );
        if (!cancelled) {
          setSummary(res.data);
          setError(null);
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.response?.data?.message || "Failed to load summary");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadSummary();
    const interval = setInterval(loadSummary, pollIntervalSec * 1000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [gameId, pollIntervalSec]);

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
          <h1 className="text-xl font-semibold">Coach Dashboard</h1>
          <p className="text-xs text-slate-400 font-mono">
            Game: {gameId || "..."}
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-300">
          <span>Refresh every</span>
          <select
            className="bg-slate-800 border border-slate-600 rounded px-2 py-1"
            value={pollIntervalSec}
            onChange={(e) => setPollIntervalSec(Number(e.target.value))}
          >
            <option value={5}>5s</option>
            <option value={10}>10s</option>
            <option value={20}>20s</option>
            <option value={30}>30s</option>
          </select>
        </div>
      </header>

      {loading && <p className="text-sm text-slate-300 mb-2">Loading…</p>}
      {error && <p className="text-sm text-red-400 mb-2">{error}</p>}

      <section className="w-full max-w-3xl">
        <table className="w-full text-sm border-separate border-spacing-y-1">
          <thead>
            <tr className="text-left text-slate-400">
              <th className="px-3 py-1">KPI</th>
              <th className="px-3 py-1 w-24 text-right">Value</th>
            </tr>
          </thead>
          <tbody>
            {summary?.kpis.map((kpi) => (
              <tr
                key={kpi.kpiId}
                className="bg-slate-800/80 hover:bg-slate-700/80 transition-colors"
              >
                <td className="px-3 py-2">{kpi.label}</td>
                <td className="px-3 py-2 text-right font-semibold">
                  {typeof kpi.total === "number"
                    ? kpi.total
                    : kpi.value
                    ? "YES"
                    : "NO"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {!summary && !loading && (
          <p className="text-sm text-slate-400 mt-4">
            No data yet. Start tracking events on the player UI.
          </p>
        )}
      </section>
    </main>
  );
}

