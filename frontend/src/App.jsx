import { useState } from "react";
import "./App.css";

export default function App() {
  const [longUrl, setLongUrl] = useState("");
  const [alias, setAlias] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setResult(null);

    const trimmed = longUrl.trim();
    if (!trimmed) {
      setError("Enter a long URL.");
      return;
    }

    const body = { longUrl: trimmed };
    const a = alias.trim();
    if (a) {
      if (a.length < 3 || a.length > 32) {
        setError("Alias must be between 3 and 32 characters.");
        return;
      }
      if (!/^[a-zA-Z0-9-]+$/.test(a)) {
        setError("Alias can only contain letters, numbers, and hyphens.");
        return;
      }
      body.alias = a;
    }
    if (expiresAt) body.expiresAt = expiresAt;

    setLoading(true);
    try {
      const res = await fetch("/api/v1/shorten", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const text = await res.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch {
        /* ignore */
      }

      if (!res.ok) {
        const msg =
          data?.message ||
          data?.error ||
          (typeof data === "string" ? data : null) ||
          text ||
          `Request failed (${res.status})`;
        setError(String(msg));
        return;
      }

      setResult(data);
    } catch (err) {
      setError(err.message || "Network error");
    } finally {
      setLoading(false);
    }
  }

  async function copyShortUrl() {
    if (!result?.shortUrl) return;
    try {
      await navigator.clipboard.writeText(result.shortUrl);
    } catch {
      /* ignore */
    }
  }

  return (
    <div className="card">
      <h1>Shorten a URL</h1>

      <form onSubmit={onSubmit}>
        <label htmlFor="longUrl">Long URL</label>
        <input
          id="longUrl"
          type="url"
          placeholder="https://example.com/very/long/path"
          value={longUrl}
          onChange={(e) => setLongUrl(e.target.value)}
          autoComplete="url"
        />

        <label htmlFor="alias">Alias (optional)</label>
        <p className="hint">Enter between 3–32 chars: letters, numbers, hyphens.</p>
        <input
          id="alias"
          type="text"
          placeholder="e.g. my-link"
          value={alias}
          onChange={(e) => setAlias(e.target.value)}
          autoComplete="off"
        />

        <label htmlFor="expires">Expiration (optional)</label>
       <input
          id="expires"
          type="datetime-local"
          value={expiresAt}
          onChange={(e) => setExpiresAt(e.target.value)}
        />

        <button type="submit" disabled={loading}>
          {loading ? "Shortening…" : "Get short link"}
        </button>
      </form>

      {error ? <div className="err">{error}</div> : null}

      {result ? (
        <div className="ok">
          <h2>Short code</h2>
          <div className="row mono">{result.shortCode}</div>
          <h2 style={{ marginTop: "0.75rem" }}>Short URL</h2>
          <div className="row">
            <a href={result.shortUrl} target="_blank" rel="noreferrer">
              {result.shortUrl}
            </a>
          </div>
          <button type="button" className="copy" onClick={copyShortUrl}>
            Copy short URL
          </button>
        </div>
      ) : null}
    </div>
  );
}
