import { createContext, useContext, useEffect, useState } from 'react';
import { api } from './api';

export const STATUS = { ABERTO: 'Aberto', EM_ATENDIMENTO: 'Em atendimento', AGUARDANDO_CLIENTE: 'Aguardando cliente', FECHADO: 'Fechado' };
export const PRIOR = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };
export const fmt = d => (d ? new Date(d).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—');

export const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);

export function useLoad(fn, deps) {
  const [s, setS] = useState({ data: null, loading: true, error: null });
  const [n, setN] = useState(0);
  useEffect(() => {
    let ok = true;
    setS(x => ({ ...x, loading: true, error: null }));
    fn().then(d => ok && setS({ data: d, loading: false, error: null }))
      .catch(e => ok && setS({ data: null, loading: false, error: e.message }));
    return () => { ok = false; };
  }, [...deps, n]);
  return { ...s, reload: () => setN(x => x + 1) };
}

export function useRefs() {
  const c = useLoad(() => api.get('/clientes'), []);
  const t = useLoad(() => api.get('/tecnicos'), []);
  const g = useLoad(() => api.get('/categorias'), []);
  const L = x => x.data || [];
  const map = x => Object.fromEntries(L(x).map(i => [i.id, i]));
  return { clientes: L(c), tecnicos: L(t), categorias: L(g), cMap: map(c), tMap: map(t), gMap: map(g), loading: c.loading || t.loading || g.loading };
}

export const Badge = ({ kind, children }) => <span className={`badge ${kind}`}>{children}</span>;
export const StatusBadge = ({ v }) => <Badge kind={`st-${v}`}>{STATUS[v] || v}</Badge>;
export const PriorBadge = ({ v }) => <Badge kind={`pr-${v}`}>{PRIOR[v] || v}</Badge>;
export const Spinner = () => <div className="center muted">Carregando…</div>;
export const ErrorBox = ({ msg, retry }) => (
  <div className="alert">{msg} {retry && <button className="link" onClick={retry}>Tentar novamente</button>}</div>
);
export const Empty = ({ children }) => <div className="center muted">{children}</div>;

export function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = e => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <header><h3>{title}</h3><button className="icon" onClick={onClose} aria-label="Fechar">×</button></header>
        {children}
      </div>
    </div>
  );
}

export function FormErrors({ err }) {
  if (!err) return null;
  return <div className="alert">{err.message}{err.detalhes?.length > 0 && <ul>{err.detalhes.map((d, i) => <li key={i}>{d}</li>)}</ul>}</div>;
}

export function Pager({ page, onChange }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="pager">
      <span className="muted">{page.totalElements} registros</span>
      <button className="btn ghost" disabled={page.number === 0} onClick={() => onChange(page.number - 1)}>Anterior</button>
      <span>Página {page.number + 1} de {page.totalPages}</span>
      <button className="btn ghost" disabled={page.number + 1 >= page.totalPages} onClick={() => onChange(page.number + 1)}>Próxima</button>
    </div>
  );
}
