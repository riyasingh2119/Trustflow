import { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { errorText } from '../api.js';

const DEMO = [
  { u: 'dev', note: 'Developer, three devices' },
  { u: 'analyst', note: 'Analyst' },
  { u: 'auditor', note: 'Auditor, read-only audit' },
  { u: 'admin', note: 'Administrator' },
];

export default function Login() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(username, password);
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <div className="login-copy">
        <div className="brand big"><span className="mark" aria-hidden="true" />TrustFlow</div>
        <h1>Every request is checked, every time.</h1>
        <p>TrustFlow weighs who is asking, what they want and the situation they are in, then allows the request, asks for a one-time code, or denies it. Each decision is recorded with the reasons.</p>
      </div>
      <form className="login-form" onSubmit={submit}>
        <h2>Sign in</h2>
        <label htmlFor="u">Username</label>
        <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        <label htmlFor="p">Password</label>
        <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        {error && <p className="error" role="alert">{error}</p>}
        <button className="primary wide" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>

        <div className="demo">
          <p>Demo accounts all use the password <b>Passw0rd!</b></p>
          <div className="chips">
            {DEMO.map((d) => (
              <button type="button" key={d.u} className="chip-btn" title={d.note}
                      onClick={() => { setUsername(d.u); setPassword('Passw0rd!'); }}>
                {d.u}
              </button>
            ))}
          </div>
        </div>
      </form>
    </div>
  );
}
