// The score line: 0-100 with the two decision thresholds drawn on it and the request's score marked.
export default function RiskMeter({ score, allowMax = 30, mfaMax = 60 }) {
  const pct = (v) => `${Math.min(Math.max(v, 0), 100)}%`;
  return (
    <div className="meter" role="img" aria-label={`Risk score ${score} out of 100`}>
      <div className="meter-bands">
        <span className="band allow" style={{ width: pct(allowMax + 1) }} />
        <span className="band mfa" style={{ width: pct(mfaMax - allowMax) }} />
        <span className="band deny" style={{ width: pct(100 - mfaMax - 1) }} />
      </div>
      <div className="meter-pin" style={{ left: pct(score) }}>
        <span className="pin-score">{score}</span>
      </div>
      <div className="meter-scale">
        <span>0</span>
        <span style={{ left: pct(allowMax + 1) }}>{allowMax + 1}</span>
        <span style={{ left: pct(mfaMax + 1) }}>{mfaMax + 1}</span>
        <span className="end">100</span>
      </div>
      <div className="meter-legend">
        <span><i className="dot allow" />Allow up to {allowMax}</span>
        <span><i className="dot mfa" />Verify {allowMax + 1}–{mfaMax}</span>
        <span><i className="dot deny" />Deny from {mfaMax + 1}</span>
      </div>
    </div>
  );
}
