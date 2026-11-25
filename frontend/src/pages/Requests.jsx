import { useCallback, useEffect, useState } from 'react';
import api, { errorText } from '../api.js';
import { useAuth } from '../auth.jsx';

export default function Requests() {
  const { hasRole } = useAuth();
  const admin = hasRole('ADMIN');
  const [resources, setResources] = useState([]);
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState({ resourceId: '', action: 'READ', reason: '' });
  const [hours, setHours] = useState({});
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.get('/requests').then(({ data }) => setRows(data)).catch((e) => setError(errorText(e)));
  }, []);

  useEffect(() => {
    load();
    api.get('/resources').then(({ data }) => {
      setResources(data);
      if (data.length) setForm((f) => ({ ...f, resourceId: String(data[0].id) }));
    });
  }, [load]);

  const nameOf = (id) => resources.find((r) => r.id === id)?.name ?? `Resource ${id}`;

  async function submit(e) {
    e.preventDefault();
    setError(''); setMsg('');
    try {
      await api.post('/requests', { resourceId: Number(form.resourceId), action: form.action, reason: form.reason });
      setMsg('Request sent. An administrator will review it.');
      setForm({ ...form, reason: '' });
      load();
    } catch (err) { setError(errorText(err)); }
  }

  async function decide(id, approve) {
    setError(''); setMsg('');
    try {
      await api.put(`/requests/${id}/approve`, { approve, hours: Number(hours[id] || 8) });
      setMsg(approve ? 'Approved. A time-limited policy now exists for this person.' : 'Request rejected.');
      load();
    } catch (err) { setError(errorText(err)); }
  }

  return (
    <div className="page">
      <h1>Temporary access</h1>
      <p className="lead">Need something your role does not cover? Ask for it here. Approved access lasts for a set number of hours and then ends by itself.</p>

      <form className="request-form inline-form" onSubmit={submit}>
        <div>
          <label htmlFor="rr">Resource</label>
          <select id="rr" value={form.resourceId} onChange={(e) => setForm({ ...form, resourceId: e.target.value })}>
            {resources.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="ra">Action</label>
          <select id="ra" value={form.action} onChange={(e) => setForm({ ...form, action: e.target.value })}>
            {['READ', 'WRITE', 'DELETE', 'ADMIN'].map((a) => <option key={a}>{a}</option>)}
          </select>
        </div>
        <div className="grow">
          <label htmlFor="rn">Why do you need it?</label>
          <input id="rn" value={form.reason} maxLength={380} onChange={(e) => setForm({ ...form, reason: e.target.value })} required />
        </div>
        <button className="primary">Send request</button>
      </form>

      {msg && <p className="ok" role="status">{msg}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      <table>
        <thead>
          <tr>{admin && <th>Requested by</th>}<th>Resource</th><th>Action</th><th>Reason</th><th>Status</th><th>Access ends</th>{admin && <th>Review</th>}</tr>
        </thead>
        <tbody>
          {rows.length === 0 && <tr><td colSpan={admin ? 7 : 5} className="empty-row">No requests yet.</td></tr>}
          {rows.map((r) => (
            <tr key={r.id}>
              {admin && <td>{r.username}</td>}
              <td>{nameOf(r.resourceId)}</td>
              <td>{r.action}</td>
              <td>{r.reason}</td>
              <td><span className={`pill st-${r.status.toLowerCase()}`}>{r.status.toLowerCase()}</span></td>
              <td>{r.expiresAt ? new Date(r.expiresAt).toLocaleString() : '—'}</td>
              {admin && (
                <td>
                  {r.status === 'PENDING' ? (
                    <span className="inline">
                      <input className="narrow" type="number" min="1" max="168" aria-label="Hours of access"
                             value={hours[r.id] ?? 8} onChange={(e) => setHours({ ...hours, [r.id]: e.target.value })} />
                      <span className="hint">hours</span>
                      <button className="primary small" onClick={() => decide(r.id, true)}>Approve</button>
                      <button className="ghost small" onClick={() => decide(r.id, false)}>Reject</button>
                    </span>
                  ) : '—'}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
