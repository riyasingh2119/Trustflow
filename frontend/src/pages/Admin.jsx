import { useCallback, useEffect, useState } from 'react';
import api, { errorText } from '../api.js';

const TABS = ['Users', 'Resources', 'Policies', 'Risk rules'];

function useLoad(path) {
  const [rows, setRows] = useState([]);
  const load = useCallback(() => api.get(path).then(({ data }) => setRows(data)), [path]);
  useEffect(() => { load(); }, [load]);
  return [rows, load];
}

function Feedback({ msg, error }) {
  return (
    <>
      {msg && <p className="ok" role="status">{msg}</p>}
      {error && <p className="error" role="alert">{error}</p>}
    </>
  );
}

/* ---------------- Users ---------------- */
function Users() {
  const [users, load] = useLoad('/admin/users');
  const [roles] = useLoad('/admin/roles');
  const [form, setForm] = useState({ username: '', email: '', password: '', role: 'DEVELOPER' });
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  async function run(fn, ok) {
    setMsg(''); setError('');
    try { await fn(); setMsg(ok); load(); } catch (e) { setError(errorText(e)); }
  }

  return (
    <>
      <Feedback msg={msg} error={error} />
      <table>
        <thead><tr><th>User</th><th>Email</th><th>Roles</th><th>Status</th><th /></tr></thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>
                <select aria-label={`Role for ${u.username}`} value={u.roles[0] ?? ''}
                        onChange={(e) => run(() => api.put(`/admin/users/${u.id}`, { roles: e.target.value ? [e.target.value] : [] }), 'Role updated.')}>
                  <option value="">no role</option>
                  {roles.map((r) => <option key={r.id}>{r.name}</option>)}
                </select>
              </td>
              <td><span className={`pill ${u.enabled ? 'allow' : 'deny'}`}>{u.enabled ? 'enabled' : 'disabled'}</span></td>
              <td className="actions">
                <button className="ghost small" onClick={() => run(() => api.put(`/admin/users/${u.id}`, { enabled: !u.enabled }), u.enabled ? 'User disabled.' : 'User enabled.')}>
                  {u.enabled ? 'Disable' : 'Enable'}
                </button>
                <button className="ghost small danger" onClick={() => window.confirm(`Delete ${u.username}?`) && run(() => api.delete(`/admin/users/${u.id}`), 'User deleted.')}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Add a user</h3>
      <form className="inline-form" onSubmit={(e) => { e.preventDefault(); run(() => api.post('/admin/users', { username: form.username, email: form.email, password: form.password, roles: [form.role] }), 'User created.'); }}>
        <div><label htmlFor="nu">Username</label><input id="nu" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required minLength={3} /></div>
        <div><label htmlFor="ne">Email</label><input id="ne" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></div>
        <div><label htmlFor="np">Password</label><input id="np" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} /></div>
        <div>
          <label htmlFor="nr">Role</label>
          <select id="nr" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            {roles.map((r) => <option key={r.id}>{r.name}</option>)}
          </select>
        </div>
        <button className="primary">Create user</button>
      </form>
    </>
  );
}

/* ---------------- Resources ---------------- */
function Resources() {
  const [rows, load] = useLoad('/admin/resources');
  const blank = { name: '', type: 'API', environment: 'DEV', sensitivity: 'LOW', path: '' };
  const [form, setForm] = useState(blank);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  async function run(fn, ok) {
    setMsg(''); setError('');
    try { await fn(); setMsg(ok); load(); } catch (e) { setError(errorText(e)); }
  }
  const opt = (arr) => arr.map((a) => <option key={a}>{a}</option>);

  return (
    <>
      <Feedback msg={msg} error={error} />
      <table>
        <thead><tr><th>Name</th><th>Type</th><th>Environment</th><th>Sensitivity</th><th>Path</th><th /></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td>{r.name}</td><td>{r.type}</td><td>{r.environment}</td>
              <td>
                <select aria-label={`Sensitivity of ${r.name}`} value={r.sensitivity}
                        onChange={(e) => run(() => api.put(`/admin/resources/${r.id}`, { ...r, sensitivity: e.target.value }), 'Sensitivity updated.')}>
                  {opt(['LOW', 'MEDIUM', 'HIGH'])}
                </select>
              </td>
              <td>{r.path}</td>
              <td className="actions">
                <button className="ghost small danger" onClick={() => window.confirm(`Delete ${r.name}? Its policies are removed too.`) && run(() => api.delete(`/admin/resources/${r.id}`), 'Resource deleted.')}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <h3>Add a resource</h3>
      <form className="inline-form" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.post('/admin/resources', form); setForm(blank); }, 'Resource created.'); }}>
        <div><label htmlFor="rn1">Name</label><input id="rn1" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></div>
        <div><label htmlFor="rt">Type</label><select id="rt" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>{opt(['API', 'DATABASE', 'REPOSITORY', 'CONFIG', 'SERVICE'])}</select></div>
        <div><label htmlFor="re">Environment</label><select id="re" value={form.environment} onChange={(e) => setForm({ ...form, environment: e.target.value })}>{opt(['DEV', 'PROD'])}</select></div>
        <div><label htmlFor="rs">Sensitivity</label><select id="rs" value={form.sensitivity} onChange={(e) => setForm({ ...form, sensitivity: e.target.value })}>{opt(['LOW', 'MEDIUM', 'HIGH'])}</select></div>
        <div><label htmlFor="rp">Path</label><input id="rp" value={form.path} onChange={(e) => setForm({ ...form, path: e.target.value })} required placeholder="/res/example" /></div>
        <button className="primary">Create resource</button>
      </form>
    </>
  );
}

/* ---------------- Policies ---------------- */
function Policies() {
  const [rows, load] = useLoad('/admin/policies');
  const [resources] = useLoad('/admin/resources');
  const [roles] = useLoad('/admin/roles');
  const [form, setForm] = useState({ resourceId: '', roleId: '', action: 'READ', validUntil: '' });
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setForm((f) => ({
      ...f,
      resourceId: f.resourceId || (resources[0] ? String(resources[0].id) : ''),
      roleId: f.roleId || (roles[0] ? String(roles[0].id) : ''),
    }));
  }, [resources, roles]);

  async function run(fn, ok) {
    setMsg(''); setError('');
    try { await fn(); setMsg(ok); load(); } catch (e) { setError(errorText(e)); }
  }
  const resName = (id) => resources.find((r) => r.id === id)?.name ?? id;
  const roleName = (id) => roles.find((r) => r.id === id)?.name ?? id;
  const asRequest = (p, over = {}) => ({
    resourceId: p.resourceId, roleId: p.roleId, action: p.action, status: p.status,
    validFrom: p.validFrom, validUntil: p.validUntil, conditions: p.conditions, ...over,
  });

  return (
    <>
      <Feedback msg={msg} error={error} />
      <div className="scroll">
        <table>
          <thead><tr><th>Resource</th><th>Role</th><th>Action</th><th>Status</th><th>Valid until</th><th>Note</th><th /></tr></thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id}>
                <td>{resName(p.resourceId)}</td>
                <td>{roleName(p.roleId)}{p.userId ? <span className="chip">one user</span> : null}</td>
                <td>{p.action}</td>
                <td><span className={`pill ${p.status === 'ACTIVE' ? 'allow' : 'deny'}`}>{p.status.toLowerCase()}</span></td>
                <td>{p.validUntil ? new Date(p.validUntil).toLocaleString() : 'no end'}</td>
                <td>{p.conditions}</td>
                <td className="actions">
                  <button className="ghost small" onClick={() => run(() => api.put(`/admin/policies/${p.id}`, asRequest(p, { status: p.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })), 'Policy updated.')}>
                    {p.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                  </button>
                  <button className="ghost small danger" onClick={() => run(() => api.delete(`/admin/policies/${p.id}`), 'Policy deleted.')}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <h3>Add a policy</h3>
      <form className="inline-form" onSubmit={(e) => { e.preventDefault(); run(() => api.post('/admin/policies', {
        resourceId: Number(form.resourceId), roleId: Number(form.roleId), action: form.action,
        validUntil: form.validUntil ? new Date(form.validUntil).toISOString() : null,
      }), 'Policy created.'); }}>
        <div><label htmlFor="pr">Resource</label><select id="pr" value={form.resourceId} onChange={(e) => setForm({ ...form, resourceId: e.target.value })}>{resources.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}</select></div>
        <div><label htmlFor="po">Role</label><select id="po" value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}>{roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}</select></div>
        <div><label htmlFor="pa">Action</label><select id="pa" value={form.action} onChange={(e) => setForm({ ...form, action: e.target.value })}>{['READ', 'WRITE', 'DELETE', 'ADMIN'].map((a) => <option key={a}>{a}</option>)}</select></div>
        <div><label htmlFor="pv">Ends at (optional)</label><input id="pv" type="datetime-local" value={form.validUntil} onChange={(e) => setForm({ ...form, validUntil: e.target.value })} /></div>
        <button className="primary">Create policy</button>
      </form>
    </>
  );
}

/* ---------------- Risk rules ---------------- */
const GROUPS = [
  { title: 'Device', keys: [['device.unknown', 'Unknown device adds'], ['device.noncompliant', 'Non-compliant device adds']] },
  { title: 'Resource sensitivity', keys: [['resource.low', 'Low adds'], ['resource.medium', 'Medium adds'], ['resource.high', 'High adds']] },
  { title: 'Time of access', keys: [['time.shoulder', '06:00–09:00 and 18:00–22:00 adds'], ['time.night', '22:00–06:00 adds']] },
  { title: 'Failed logins', keys: [['auth.per_failure', 'Each failure adds'], ['auth.cap', 'Most this factor can add'], ['auth.window_minutes', 'Minutes counted']] },
  { title: 'Decision thresholds', keys: [['threshold.allow_max', 'Highest score that is allowed'], ['threshold.mfa_max', 'Highest score that asks for a code']] },
];

function RiskRules() {
  const [cfg, setCfg] = useState(null);
  const [draft, setDraft] = useState({});
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => api.get('/admin/risk-config').then(({ data }) => { setCfg(data); setDraft({}); }), []);
  useEffect(() => { load(); }, [load]);

  async function save() {
    setMsg(''); setError('');
    try {
      const body = Object.fromEntries(Object.entries(draft).map(([k, v]) => [k, Number(v)]));
      const { data } = await api.put('/admin/risk-config', body);
      setCfg(data); setDraft({}); setMsg('Rules saved. They apply to new requests within 30 seconds.');
    } catch (e) { setError(errorText(e)); load(); }
  }

  if (!cfg) return <p>Loading…</p>;
  const dirty = Object.keys(draft).length > 0;

  return (
    <>
      <Feedback msg={msg} error={error} />
      <p className="lead">The score is the sum of the four factors, so the maximum with the default weights is 100. Edit the weights to tune how strict the gateway is.</p>
      <div className="rules">
        {GROUPS.map((g) => (
          <fieldset key={g.title}>
            <legend>{g.title}</legend>
            {g.keys.map(([k, label]) => (
              <div className="rule" key={k}>
                <label htmlFor={k}>{label}</label>
                <input id={k} type="number" min="0" max="100" value={draft[k] ?? cfg[k]}
                       onChange={(e) => setDraft({ ...draft, [k]: e.target.value })} />
              </div>
            ))}
          </fieldset>
        ))}
      </div>
      <button className="primary" disabled={!dirty} onClick={save}>Save rules</button>
      {dirty && <button className="ghost" onClick={() => setDraft({})}>Discard changes</button>}
    </>
  );
}

export default function Admin() {
  const [tab, setTab] = useState(TABS[0]);
  return (
    <div className="page wide-page">
      <h1>Administration</h1>
      <div className="tabs" role="tablist">
        {TABS.map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} className={tab === t ? 'on' : ''} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>
      {tab === 'Users' && <Users />}
      {tab === 'Resources' && <Resources />}
      {tab === 'Policies' && <Policies />}
      {tab === 'Risk rules' && <RiskRules />}
    </div>
  );
}
