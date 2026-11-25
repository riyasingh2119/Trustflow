import { useEffect, useState } from 'react';
import api, { errorText } from '../api.js';
import DecisionPanel from '../components/DecisionPanel.jsx';

const ACTIONS = ['READ', 'WRITE', 'DELETE', 'ADMIN'];
const UNREGISTERED = '__unregistered__';

export default function Console() {
  const [resources, setResources] = useState([]);
  const [devices, setDevices] = useState([]);
  const [thresholds, setThresholds] = useState({ allowMax: 30, mfaMax: 60 });
  const [form, setForm] = useState({ resourceId: '', action: 'READ', device: '', hour: 'live' });
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get('/resources').then(({ data }) => {
      setResources(data);
      if (data.length) setForm((f) => ({ ...f, resourceId: String(data[0].id) }));
    }).catch((e) => setError(errorText(e)));
    api.get('/auth/me').then(({ data }) => {
      setDevices(data.devices);
      if (data.devices.length) setForm((f) => ({ ...f, device: data.devices[0].fingerprint }));
    }).catch(() => {});
    // Thresholds are only readable by admins; other users see the defaults from the design document.
    api.get('/admin/risk-config').then(({ data }) => setThresholds({
      allowMax: data['threshold.allow_max'], mfaMax: data['threshold.mfa_max'],
    })).catch(() => {});
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function send(mfaToken) {
    setBusy(true);
    setError('');
    const deviceHeader = form.device === UNREGISTERED ? 'unregistered-device' : form.device;
    const headers = {};
    if (deviceHeader) headers['X-Device-Id'] = deviceHeader;
    if (mfaToken) headers['X-MFA-Token'] = mfaToken;
    const body = { resourceId: Number(form.resourceId), action: form.action };
    if (form.hour !== 'live') body.simulatedHour = Number(form.hour);
    try {
      const { data } = await api.post('/gateway/access', body, { headers });
      setResult(data);
      return data;
    } catch (err) {
      if (err.response?.data?.decision) {      // a DENY comes back as HTTP 403 with the full decision body
        setResult(err.response.data);
        return err.response.data;
      }
      setError(errorText(err));
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function submit(e) {
    e.preventDefault();
    const data = await send(null);
    if (data) {
      setHistory((h) => [{ ...data, action: form.action, at: new Date().toLocaleTimeString() }, ...h].slice(0, 6));
    }
  }

  async function retryWithToken(mfaToken) {
    const data = await send(mfaToken);
    if (data) setHistory((h) => [{ ...data, action: form.action, at: new Date().toLocaleTimeString() }, ...h].slice(0, 6));
  }

  const selected = resources.find((r) => String(r.id) === form.resourceId);

  return (
    <div className="page">
      <h1>Request access</h1>
      <p className="lead">Choose what you want to do and the situation to test. The gateway scores the request and answers on the right.</p>

      <div className="console">
        <form onSubmit={submit} className="request-form">
          <label htmlFor="res">Resource</label>
          <select id="res" value={form.resourceId} onChange={set('resourceId')} required>
            {resources.map((r) => (
              <option key={r.id} value={r.id}>{r.name} ({r.environment.toLowerCase()}, {r.sensitivity.toLowerCase()})</option>
            ))}
          </select>
          {selected && <p className="hint">{selected.type.toLowerCase()} at {selected.path}</p>}

          <label htmlFor="act">Action</label>
          <select id="act" value={form.action} onChange={set('action')}>
            {ACTIONS.map((a) => <option key={a}>{a}</option>)}
          </select>

          <label htmlFor="dev">Device sending the request</label>
          <select id="dev" value={form.device} onChange={set('device')}>
            {devices.map((d) => (
              <option key={d.fingerprint} value={d.fingerprint}>
                {d.fingerprint} ({d.status.toLowerCase().replace('_', ' ')})
              </option>
            ))}
            <option value={UNREGISTERED}>a device that is not registered</option>
          </select>

          <label htmlFor="hr">Time of access</label>
          <select id="hr" value={form.hour} onChange={set('hour')}>
            <option value="live">Now (server clock)</option>
            <option value="11">11:00, business hours</option>
            <option value="19">19:00, evening</option>
            <option value="23">23:00, night</option>
            <option value="3">03:00, night</option>
          </select>
          <p className="hint">Simulated times work only while the server runs in demo mode.</p>

          {error && <p className="error" role="alert">{error}</p>}
          <button className="primary wide" disabled={busy || !form.resourceId}>{busy ? 'Checking…' : 'Check access'}</button>
        </form>

        <div className="outcome">
          {result ? (
            <DecisionPanel result={result} thresholds={thresholds} onMfaToken={retryWithToken} />
          ) : (
            <div className="empty">
              <h2>No request yet</h2>
              <p>Pick a resource and select <b>Check access</b>. Try the same resource from a registered laptop and from an unregistered device to see the score change.</p>
            </div>
          )}
        </div>
      </div>

      {history.length > 0 && (
        <section className="history">
          <h3>This session</h3>
          <table>
            <thead><tr><th>Time</th><th>Resource</th><th>Action</th><th>Score</th><th>Decision</th></tr></thead>
            <tbody>
              {history.map((h, i) => (
                <tr key={i}>
                  <td>{h.at}</td><td>{h.resource}</td><td>{h.action}</td><td>{h.score}</td>
                  <td><span className={`pill ${h.decision.toLowerCase()}`}>{h.decision.replace('_', ' ').toLowerCase()}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
