const BASE = import.meta.env.VITE_API_URL ?? '/api';

async function req(path, { method = 'GET', body } = {}) {
  const r = await fetch(BASE + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  }).catch(() => { throw new Error('Não foi possível conectar ao servidor (porta 8080).'); });
  if (r.status === 204) return null;
  const d = await r.json().catch(() => null);
  if (!r.ok) {
    const e = new Error(d?.erro || 'Erro na requisição');
    e.detalhes = d?.detalhes || [];
    throw e;
  }
  return d;
}

export const api = {
  get: p => req(p),
  post: (p, b) => req(p, { method: 'POST', body: b }),
  put: (p, b) => req(p, { method: 'PUT', body: b }),
  patch: (p, b) => req(p, { method: 'PATCH', body: b }),
  del: p => req(p, { method: 'DELETE' }),
};

export const qs = o => {
  const s = new URLSearchParams(Object.entries(o).filter(([, v]) => v !== '' && v != null)).toString();
  return s ? '?' + s : '';
};
