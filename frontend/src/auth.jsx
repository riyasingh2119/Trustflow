import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { setToken, getToken, setUnauthorizedHandler } from './api.js';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = sessionStorage.getItem('tf_user');
    return raw && getToken() ? JSON.parse(raw) : null;
  });

  const logout = useCallback(() => {
    setToken(null);
    sessionStorage.removeItem('tf_user');
    setUser(null);
  }, []);

  useEffect(() => setUnauthorizedHandler(logout), [logout]);

  const login = useCallback(async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password });
    setToken(data.token);
    const u = { username: data.username, roles: data.roles };
    sessionStorage.setItem('tf_user', JSON.stringify(u));
    setUser(u);
  }, []);

  const value = useMemo(() => ({
    user, login, logout,
    hasRole: (...roles) => !!user && roles.some((r) => user.roles.includes(r)),
  }), [user, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
