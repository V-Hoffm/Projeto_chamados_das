import { api, qs } from '../api';
import { useLoad, useRefs, Spinner, ErrorBox, StatusBadge, PriorBadge, STATUS, PRIOR, fmt, Empty } from '../ui.jsx';

export default function Dashboard() {
  const { data, loading, error, reload } = useLoad(() => api.get('/chamados' + qs({ size: 200 })), []);
  const refs = useRefs();
  if (loading) return <Spinner />;
  if (error) return <ErrorBox msg={error} retry={reload} />;

  const list = data.content;
  const by = (k, v) => list.filter(c => c[k] === v).length;
  const max = Math.max(1, ...Object.keys(PRIOR).map(p => by('prioridade', p)));

  return (
    <>
      <div className="page-head"><div><h1>Dashboard</h1><p className="muted">Visão geral dos chamados</p></div>
        <a className="btn" href="#/chamados">Ver chamados</a></div>

      <div className="grid cards">
        <div className="card stat"><span>Total</span><strong>{data.totalElements}</strong></div>
        {Object.keys(STATUS).map(s => (
          <div key={s} className="card stat"><span>{STATUS[s]}</span><strong>{by('status', s)}</strong></div>
        ))}
      </div>

      <div className="grid two">
        <section className="card">
          <h3>Por prioridade</h3>
          {Object.keys(PRIOR).map(p => (
            <div key={p} className="bar-row">
              <span>{PRIOR[p]}</span>
              <div className="bar"><div style={{ width: `${(by('prioridade', p) / max) * 100}%` }} /></div>
              <b>{by('prioridade', p)}</b>
            </div>
          ))}
        </section>

        <section className="card">
          <h3>Chamados recentes</h3>
          {list.length === 0 ? <Empty>Nenhum chamado ainda.</Empty> : (
            <ul className="recent">
              {list.slice(0, 5).map(c => (
                <li key={c.id}>
                  <a href={`#/chamados/${c.id}`}><b>#{c.id} {c.titulo}</b>
                    <small className="muted">{refs.cMap[c.clienteId]?.nome || '—'} · {fmt(c.dataAbertura)}</small></a>
                  <span className="row"><PriorBadge v={c.prioridade} /><StatusBadge v={c.status} /></span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  );
}
