import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Login from './pages/Login.jsx';
import Console from './pages/Console.jsx';
import Requests from './pages/Requests.jsx';
import Audit from './pages/Audit.jsx';
import Admin from './pages/Admin.jsx';

function Guard({ roles, children }) {
  const { user, hasRole } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !hasRole(...roles)) return <Navigate to="/" replace />;
  return children;
}

function Shell({ children }) {
  const { user, logout, hasRole } = useAuth();
  return (
    <div className="shell">
      <aside className="rail">
        <div className="brand"><span className="mark" aria-hidden="true" />TrustFlow</div>
        <nav>
          <NavLink to="/" end>Request access</NavLink>
          <NavLink to="/requests">Temporary access</NavLink>
          {hasRole('ADMIN', 'AUDITOR') && <NavLink to="/audit">Audit trail</NavLink>}
          {hasRole('ADMIN') && <NavLink to="/admin">Administration</NavLink>}
        </nav>
        <div className="who">
          <strong>{user.username}</strong>
          <span>{user.roles.length ? user.roles.join(', ') : 'No role assigned'}</span>
          <button className="link" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main>{children}</main>
    </div>
  );
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/" element={<Guard><Shell><Console /></Shell></Guard>} />
      <Route path="/requests" element={<Guard><Shell><Requests /></Shell></Guard>} />
      <Route path="/audit" element={<Guard roles={['ADMIN', 'AUDITOR']}><Shell><Audit /></Shell></Guard>} />
      <Route path="/admin" element={<Guard roles={['ADMIN']}><Shell><Admin /></Shell></Guard>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
