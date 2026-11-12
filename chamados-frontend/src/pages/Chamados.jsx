import { useState } from 'react';
import { api, qs } from '../api';
import { nav } from '../App.jsx';
import {
  useLoad, useRefs, useToast, Spinner, ErrorBox, Empty, Modal, Field, FormErrors, Pager,
  StatusBadge, PriorBadge, STATUS, PRIOR, fmt,
} from '../ui.jsx';

const Opts = ({ list, label = 'nome' }) => list.map(i => <option key={i.id} value={i.id}>{i[label]}</option>);

/* ---------- Lista ---------- */
export function Chamados() {
  const [f, setF] = useState({ status: '', prioridade: '', clienteId: '', page: 0 });
  const [novo, setNovo] = useState(false);
  const refs = useRefs();
  const { data, loading, error, reload } = useLoad(() => api.get('/chamados' + qs({ ...f, size: 10 })), [JSON.stringify(f)]);
  const set = (k, v) => setF({ ...f, [k]: v, page: 0 });

  return (
    <>
      <div className="page-head"><div><h1>Chamados</h1><p className="muted">Acompanhe e gerencie os atendimentos</p></div>
        <button className="btn" onClick={() => setNovo(true)}>+ Novo chamado</button></div>

      <div className="card filters">
        <select value={f.status} onChange={e => set('status', e.target.value)}>
          <option value="">Todos os status</option>{Object.entries(STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select value={f.prioridade} onChange={e => set('prioridade', e.target.value)}>
          <option value="">Todas as prioridades</option>{Object.entries(PRIOR).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select value={f.clienteId} onChange={e => set('clienteId', e.target.value)}>
          <option value="">Todos os clientes</option><Opts list={refs.clientes} />
        </select>
        {(f.status || f.prioridade || f.clienteId) && <button className="link" onClick={() => setF({ status: '', prioridade: '', clienteId: '', page: 0 })}>Limpar</button>}
      </div>

      <div className="card table-wrap">
        {loading ? <Spinner /> : error ? <ErrorBox msg={error} retry={reload} /> : data.content.length === 0 ? <Empty>Nenhum chamado encontrado.</Empty> : (
          <table>
            <thead><tr><th>#</th><th>Título</th><th>Cliente</th><th>Técnico</th><th>Prioridade</th><th>Status</th><th>Abertura</th></tr></thead>
            <tbody>
              {data.content.map(c => (
                <tr key={c.id} className="click" onClick={() => nav(`/chamados/${c.id}`)}>
                  <td className="muted">{c.id}</td><td><b>{c.titulo}</b></td>
                  <td>{refs.cMap[c.clienteId]?.nome || '—'}</td><td>{refs.tMap[c.tecnicoId]?.nome || '—'}</td>
                  <td><PriorBadge v={c.prioridade} /></td><td><StatusBadge v={c.status} /></td><td className="muted">{fmt(c.dataAbertura)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {data && <Pager page={data} onChange={p => setF({ ...f, page: p })} />}
      {novo && <ChamadoForm refs={refs} onClose={() => setNovo(false)} onSaved={c => { setNovo(false); nav(`/chamados/${c.id}`); }} />}
    </>
  );
}

/* ---------- Formulário (novo / editar) ---------- */
function ChamadoForm({ initial, refs, onClose, onSaved }) {
  const toast = useToast();
  const [v, setV] = useState(initial || { titulo: '', descricao: '', prioridade: 'MEDIA', clienteId: '', categoriaId: '', tecnicoId: '' });
  const [err, setErr] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k, x) => setV({ ...v, [k]: x });

  const submit = async e => {
    e.preventDefault(); setBusy(true); setErr(null);
    const body = { ...v, clienteId: +v.clienteId || null, categoriaId: +v.categoriaId || null, tecnicoId: +v.tecnicoId || null };
    try {
      const r = initial ? await api.put(`/chamados/${initial.id}`, body) : await api.post('/chamados', body);
      toast(initial ? 'Chamado atualizado' : 'Chamado criado'); onSaved(r);
    } catch (e2) { setErr(e2); setBusy(false); }
  };

  return (
    <Modal title={initial ? `Editar chamado #${initial.id}` : 'Novo chamado'} onClose={onClose}>
      <form onSubmit={submit} className="form">
        <Field label="Título"><input value={v.titulo} maxLength={150} onChange={e => set('titulo', e.target.value)} required /></Field>
        <Field label="Descrição"><textarea rows={4} value={v.descricao} maxLength={2000} onChange={e => set('descricao', e.target.value)} required /></Field>
        <div className="grid two-f">
          <Field label="Prioridade"><select value={v.prioridade} onChange={e => set('prioridade', e.target.value)}>
            {Object.entries(PRIOR).map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select></Field>
          <Field label="Categoria"><select value={v.categoriaId} onChange={e => set('categoriaId', e.target.value)} required>
            <option value="">Selecione…</option><Opts list={refs.categorias} /></select></Field>
          <Field label="Cliente"><select value={v.clienteId} onChange={e => set('clienteId', e.target.value)} required>
            <option value="">Selecione…</option><Opts list={refs.clientes} /></select></Field>
          <Field label="Técnico responsável"><select value={v.tecnicoId} onChange={e => set('tecnicoId', e.target.value)} required>
            <option value="">Selecione…</option><Opts list={refs.tecnicos} /></select></Field>
        </div>
        <FormErrors err={err} />
        <footer><button type="button" className="btn ghost" onClick={onClose}>Cancelar</button><button className="btn" disabled={busy}>{busy ? 'Salvando…' : 'Salvar'}</button></footer>
      </form>
    </Modal>
  );
}

/* ---------- Detalhe ---------- */
export function ChamadoDetalhe({ id }) {
  const toast = useToast();
  const refs = useRefs();
  const { data: c, loading, error, reload } = useLoad(() => api.get(`/chamados/${id}`), [id]);
  const [edit, setEdit] = useState(false);
  const [com, setCom] = useState({ autor: '', texto: '' });
  const [err, setErr] = useState(null);

  if (loading && !c) return <Spinner />;
  if (error) return <ErrorBox msg={error} retry={reload} />;
  const fechado = c.status === 'FECHADO';

  const act = async (fn, okMsg) => {
    setErr(null);
    try { await fn(); toast(okMsg); reload(); } catch (e) { toast(e.message, 'err'); }
  };
  const comentar = async e => {
    e.preventDefault(); setErr(null);
    try { await api.post(`/chamados/${id}/comentarios`, com); setCom({ ...com, texto: '' }); toast('Comentário adicionado'); reload(); }
    catch (e2) { setErr(e2); }
  };
  const excluir = () => {
    if (!confirm(`Excluir o chamado #${id}? Esta ação não pode ser desfeita.`)) return;
    api.del(`/chamados/${id}`).then(() => { toast('Chamado excluído'); nav('/chamados'); }).catch(e => toast(e.message, 'err'));
  };

  return (
    <>
      <a className="back" href="#/chamados">← Voltar</a>
      <div className="page-head">
        <div><h1>#{c.id} · {c.titulo}</h1><div className="row"><StatusBadge v={c.status} /><PriorBadge v={c.prioridade} /></div></div>
        <div className="row">
          <button className="btn ghost" disabled={fechado} onClick={() => setEdit(true)}>Editar</button>
          <button className="btn danger" disabled={fechado} onClick={excluir}>Excluir</button>
        </div>
      </div>
      {fechado && <div className="info">Este chamado está fechado e não pode mais ser alterado.</div>}

      <div className="grid detail">
        <div className="stack">
          <section className="card"><h3>Descrição</h3><p className="pre">{c.descricao}</p></section>

          <section className="card">
            <h3>Comentários ({c.comentarios.length})</h3>
            {c.comentarios.length === 0 && <p className="muted">Nenhum comentário ainda.</p>}
            <ul className="comments">
              {c.comentarios.map(m => (
                <li key={m.id}><span className="avatar">{m.autor.charAt(0).toUpperCase()}</span>
                  <div><b>{m.autor}</b> <small className="muted">{fmt(m.data)}</small><p className="pre">{m.texto}</p></div></li>
              ))}
            </ul>
            <form onSubmit={comentar} className="form">
              <Field label="Autor"><input value={com.autor} maxLength={100} onChange={e => setCom({ ...com, autor: e.target.value })} required /></Field>
              <Field label="Comentário"><textarea rows={3} value={com.texto} maxLength={1000} onChange={e => setCom({ ...com, texto: e.target.value })} required /></Field>
              <FormErrors err={err} />
              <footer><button className="btn">Comentar</button></footer>
            </form>
          </section>
        </div>

        <aside className="stack">
          <section className="card">
            <h3>Gerenciar</h3>
            <Field label="Status"><select value={c.status} disabled={fechado}
              onChange={e => act(() => api.patch(`/chamados/${id}/status`, { status: e.target.value }), 'Status atualizado')}>
              {Object.entries(STATUS).map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select></Field>
            <Field label="Técnico responsável"><select value={c.tecnicoId} disabled={fechado}
              onChange={e => act(() => api.patch(`/chamados/${id}/atribuir`, { tecnicoId: +e.target.value }), 'Técnico atribuído')}>
              <Opts list={refs.tecnicos} /></select></Field>
          </section>
          <section className="card">
            <h3>Informações</h3>
            <dl>
              <dt>Cliente</dt><dd>{refs.cMap[c.clienteId]?.nome || '—'}</dd>
              <dt>Categoria</dt><dd>{refs.gMap[c.categoriaId]?.nome || '—'}</dd>
              <dt>Aberto em</dt><dd>{fmt(c.dataAbertura)}</dd>
              <dt>Fechado em</dt><dd>{fmt(c.dataFechamento)}</dd>
            </dl>
          </section>
        </aside>
      </div>
      {edit && <ChamadoForm initial={c} refs={refs} onClose={() => setEdit(false)} onSaved={() => { setEdit(false); reload(); }} />}
    </>
  );
}
