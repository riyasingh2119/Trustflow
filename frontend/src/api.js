import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

let token = sessionStorage.getItem('tf_token');
let onUnauthorized = () => {};

export function setToken(t) {
  token = t;
  if (t) sessionStorage.setItem('tf_token', t);
  else sessionStorage.removeItem('tf_token');
}
export const getToken = () => token;
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

api.interceptors.request.use((cfg) => {
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const url = err.config?.url || '';
    const isCredentialCheck = url.includes('/auth/login') || url.includes('/mfa/verify');
    if (err.response?.status === 401 && !isCredentialCheck) onUnauthorized();
    return Promise.reject(err);
  },
);

/** Turn an Axios error into one readable sentence. */
export function errorText(err) {
  const d = err.response?.data;
  if (d?.fields) return Object.entries(d.fields).map(([k, v]) => `${k}: ${v}`).join('; ');
  return d?.error || d?.message || err.message || 'Something went wrong';
}

export default api;
