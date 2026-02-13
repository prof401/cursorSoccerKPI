import { useState } from "react";
import axios from "axios";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;

export default function Home() {
  const [homeTeam, setHomeTeam] = useState("");
  const [awayTeam, setAwayTeam] = useState("");
  const [gameId, setGameId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreateGame = async () => {
    if (!API_BASE) {
      setError("NEXT_PUBLIC_API_BASE_URL is not configured");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      // Ensure API_BASE doesn't have trailing slash
      const apiUrl = API_BASE.replace(/\/$/, "");
      const res = await axios.post(`${apiUrl}/games`, {
        homeTeam: homeTeam || undefined,
        awayTeam: awayTeam || undefined
      }, {
        headers: {
          "Content-Type": "application/json"
        }
      });
      setGameId(res.data.gameId);
    } catch (e: any) {
      console.error("Create game error:", e);
      const errorMessage = e?.response?.data?.message 
        || e?.message 
        || `HTTP ${e?.response?.status}: ${e?.response?.statusText || "Unknown error"}`;
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-md bg-slate-800 rounded-xl p-6 shadow-lg space-y-4">
        <h1 className="text-2xl font-semibold mb-2 text-center">
          Soccer KPI Tracker
        </h1>
        <div className="space-y-2">
          <label className="block text-sm text-slate-300">
            Home Team
            <input
              className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              value={homeTeam}
              onChange={(e) => setHomeTeam(e.target.value)}
            />
          </label>
          <label className="block text-sm text-slate-300">
            Away Team
            <input
              className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              value={awayTeam}
              onChange={(e) => setAwayTeam(e.target.value)}
            />
          </label>
        </div>

        <button
          onClick={handleCreateGame}
          disabled={loading}
          className="w-full mt-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-semibold py-3 transition-colors"
        >
          {loading ? "Creating..." : "Create Game"}
        </button>

        {error && (
          <p className="text-sm text-red-400 mt-2 text-center">{error}</p>
        )}

        {gameId && (
          <div className="mt-4 space-y-2 text-sm text-center">
            <p className="text-slate-300">
              Game created with ID:
              <span className="font-mono ml-1">{gameId}</span>
            </p>
            <p>
              Player UI:{" "}
              <code className="bg-slate-900 px-2 py-1 rounded">
                /game/{gameId}/track
              </code>
            </p>
            <p>
              Coach dashboard:{" "}
              <code className="bg-slate-900 px-2 py-1 rounded">
                /game/{gameId}/dashboard
              </code>
            </p>
          </div>
        )}
      </div>
    </main>
  );
}

