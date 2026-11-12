import { useEffect, useState, useCallback } from 'react';
import { ToastCtx } from './ui.jsx';
import Dashboard from './pages/Dashboard.jsx';
import { Chamados, ChamadoDetalhe } from './pages/Chamados.jsx';
import { Clientes, Tecnicos, Categorias } from './pages/Cadastros.jsx';

const MENU = [
  ['/', 'Dashboard', '▦'],
  ['/chamados', 'Chamados', '✉'],
  ['/clientes', 'Clientes', '☺'],
  ['/tecnicos', 'Técnicos', '⚙'],
  ['/categorias', 'Categorias', '❖'],
];

const route = () => location.hash.replace(/^#/, '') || '/';
export const nav = p => { location.hash = p; };

export default function App() {
  const [path, setPath] = useState(route());
  const [toast, setToast] = useState(null);
  useEffect(() => {
    const h = () => setPath(route());
    window.addEventListener('hashchange', h);
    return () => window.removeEventListener('hashchange', h);
  }, []);
  const notify = useCallback((msg, type = 'ok') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  const [, base, id] = path.match(/^\/([^/]*)\/?([^/]*)/) || [];
  let page;
  if (!base) page = <Dashboard />;
  else if (base === 'chamados' && id) page = <ChamadoDetalhe key={id} id={id} />;
  else if (base === 'chamados') page = <Chamados />;
  else if (base === 'clientes') page = <Clientes />;
  else if (base === 'tecnicos') page = <Tecnicos />;
  else if (base === 'categorias') page = <Categorias />;
  else page = <div className="center muted">Página não encontrada.</div>;

  return (
    <ToastCtx.Provider value={notify}>
      <div className="layout">
        <aside className="sidebar">
          <div className="brand"><span className="logo">C</span> Chamados</div>
          <nav>
            {MENU.map(([to, label, ico]) => {
              const active = to === '/' ? path === '/' : path.startsWith(to);
              return <a key={to} href={'#' + to} className={active ? 'active' : ''}><i>{ico}</i>{label}</a>;
            })}
          </nav>
        </aside>
        <main className="content">{page}</main>
      </div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </ToastCtx.Provider>
  );
}
