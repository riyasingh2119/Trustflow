import { useState } from 'react';
import api, { errorText } from '../api.js';
import RiskMeter from './RiskMeter.jsx';

const VERDICT = {
  ALLOW: { cls: 'allow', title: 'Access allowed' },
  MFA_REQUIRED: { cls: 'mfa', title: 'Verification needed' },
  DENY: { cls: 'deny', title: 'Access denied' },
};

const FACTORS = [
  { key: 'device', label: 'Device', max: 30 },
  { key: 'resource', label: 'Resource sensitivity', max: 30 },
  { key: 'time', label: 'Time of access', max: 15 },
  { key: 'auth', label: 'Recent failed logins', max: 25 },
];

function MfaStep({ challengeId, onVerified }) {
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [hint, setHint] = useState('');

  async function fillDemo() {
    setError('');
    try {
      const { data } = await api.get('/auth/mfa/demo-code');
      setCode(data.code);
      setHint(`Demo authenticator code, valid for ${data.secondsRemaining}s`);
    } catch {
      setHint('The demo authenticator is switched off. Enter the code from your authenticator app.');
    }
  }

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/auth/mfa/verify', { challengeId, code });
      await onVerified(data.mfaToken);
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="mfa" onSubmit={submit}>
      <label htmlFor="otp">Enter the 6-digit one-time code</label>
      <div className="inline">
        <input id="otp" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code}
               onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} placeholder="123456" />
        <button type="submit" className="primary" disabled={busy || code.length !== 6}>Verify and retry</button>
        <button type="button" className="ghost" onClick={fillDemo}>Use demo authenticator</button>
      </div>
      {hint && <p className="hint">{hint}</p>}
      {error && <p className="error" role="alert">{error}</p>}
    </form>
  );
}

export default function DecisionPanel({ result, onMfaToken, thresholds }) {
  const v = VERDICT[result.decision];
  const noScore = result.hour === -1 || (result.score === 0 && result.reason?.startsWith('NO_POLICY'))
    || result.reason?.startsWith('USER_DISABLED');

  return (
    <section className={`decision ${v.cls}`} aria-live="polite">
      <header>
        <h2>{v.title}</h2>
        <p className="target">
          {result.resource}
          {result.sensitivity && <span className="chip">{result.sensitivity.toLowerCase()} sensitivity</span>}
          {result.mfaSatisfied && <span className="chip good">verified with one-time code</span>}
        </p>
      </header>

      {noScore ? (
        <p className="reason">{result.reason}</p>
      ) : (
        <>
          <RiskMeter score={result.score} allowMax={thresholds.allowMax} mfaMax={thresholds.mfaMax} />
          <ul className="factors">
            {FACTORS.map((f) => {
              const val = result.factors?.[f.key] ?? 0;
              return (
                <li key={f.key}>
                  <span className="f-label">{f.label}</span>
                  <span className="f-bar"><i style={{ width: `${(val / f.max) * 100}%` }} /></span>
                  <span className="f-val">+{val}</span>
                </li>
              );
            })}
          </ul>
          <p className="reason">{result.reason}</p>
          <p className="meta">
            Device status {result.deviceStatus?.toLowerCase().replace('_', ' ')} · local hour {result.hour}:00 · audit record #{result.auditId}
          </p>
        </>
      )}

      {result.decision === 'MFA_REQUIRED' && result.challengeId && (
        <MfaStep challengeId={result.challengeId} onVerified={onMfaToken} />
      )}
      {result.decision === 'DENY' && !noScore && (
        <p className="alerted">A high-risk alert was sent to the administrator.</p>
      )}
    </section>
  );
}
