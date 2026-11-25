import { useCallback, useEffect, useState } from 'react';
import api, { errorText } from '../api.js';

export default function Audit() {
  const [filters, setFilters] = useState({ username: '', decision: '', resource: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const params = { page, size: 15 };
    Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });
    api.get('/audit', { params }).then(({ data: d }) => setData(d)).catch((e) => setError(errorText(e)));
    api.get('/audit/summary').then(({ data: d }) => setSummary(d)).catch(() => {});
  }, [filters, page]);

  useEffect(() => { load(); }, [load]);

  const set = (k) => (e) => { setPage(0); setFilters({ ...filters, [k]: e.target.value }); };

  return (
    <div className="page wide-page">
      <h1>Audit trail</h1>
      <p className="lead">Every decision is stored with its score, the contribution of each factor and a plain-language reason. Records cannot be edited or deleted.</p>

      {summary && (
        <div className="summary">
          <div><b>{summary.total}</b><span>decisions recorded</span></div>
          <div><b>{summary.byDecision.ALLOW}</b><span>allowed</span></div>
          <div><b>{summary.byDecision.MFA_REQUIRED}</b><span>asked for a code</span></div>
          <div><b>{summary.byDecision.DENY}</b><span>denied</span></div>
          <div><b>{summary.averageScore}</b><span>average risk score</span></div>
        </div>
      )}

      <div className="filters">
        <div><label htmlFor="fu">Username</label><input id="fu" value={filters.username} onChange={set('username')} /></div>
        <div><label htmlFor="fr">Resource contains</label><input id="fr" value={filters.resource} onChange={set('resource')} /></div>
        <div>
          <label htmlFor="fd">Decision</label>
          <select id="fd" value={filters.decision} onChange={set('decision')}>
            <option value="">All</option>
            <option value="ALLOW">Allowed</option>
            <option value="MFA_REQUIRED">Asked for a code</option>
            <option value="DENY">Denied</option>
          </select>
        </div>
        <button className="ghost" onClick={load}>Refresh</button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}

      <div className="scroll">
        <table>
          <thead>
            <tr><th>Time</th><th>User</th><th>Resource</th><th>Action</th><th>Score</th><th>Device / Resource / Time / Logins</th><th>Decision</th><th>Reason</th><th>IP</th></tr>
          </thead>
          <tbody>
            {data.content.length === 0 && <tr><td colSpan={9} className="empty-row">No audit events match these filters.</td></tr>}
            {data.content.map((e) => (
              <tr key={e.id}>
                <td>{new Date(e.timestamp).toLocaleString()}</td>
                <td>{e.username}</td>
                <td>{e.resourceName}</td>
                <td>{e.action}</td>
                <td>{e.riskScore}</td>
                <td>{e.deviceRisk} / {e.resourceRisk} / {e.timeRisk} / {e.authRisk}</td>
                <td><span className={`pill ${e.decision.toLowerCase()}`}>{e.decision.replace('_', ' ').toLowerCase()}</span></td>
                <td className="reason-cell">{e.reason}</td>
                <td>{e.ip}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="pager">
        <button className="ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
        <span>Page {page + 1} of {Math.max(data.totalPages, 1)} ({data.totalElements} events)</span>
        <button className="ghost small" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </div>
  );
}
