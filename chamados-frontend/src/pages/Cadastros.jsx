import { useState } from 'react';
import { api } from '../api';
import { useLoad, useToast, Spinner, ErrorBox, Empty, Modal, Field, FormErrors, Badge } from '../ui.jsx';

/* CRUD genérico dirigido por configuração */
function Crud({ title, subtitle, path, fields, columns, empty }) {
  const toast = useToast();
  const { data, loading, error, reload } = useLoad(() => api.get(path), []);
  const [form, setForm] = useState(null); // null | {} (novo) | item (editar)
  const blank = Object.fromEntries(fields.map(f => [f.name, f.options ? Object.keys(f.options)[0] : '']));
  const [v, setV] = useState(blank);
  const [err, setErr] = useState(null);

  const open = item => { setV(item ? Object.fromEntries(fields.map(f => [f.name, item[f.name] ?? ''])) : blank); setErr(null); setForm(item || {}); };
  const save = async e => {
    e.preventDefault(); setErr(null);
    try {
      form.id ? await api.put(`${path}/${form.id}`, v) : await api.post(path, v);
      toast('Registro salvo'); setForm(null); reload();
    } catch (e2) { setErr(e2); }
  };
  const remove = async item => {
    if (!confirm('Excluir este registro?')) return;
    try { await api.del(`${path}/${item.id}`); toast('Registro excluído'); reload(); }
    catch (e) { toast('Não foi possível excluir: o registro pode estar em uso por chamados.', 'err'); }
  };

  return (
    <>
      <div className="page-head"><div><h1>{title}</h1><p className="muted">{subtitle}</p></div>
        <button className="btn" onClick={() => open(null)}>+ Novo</button></div>
      <div className="card table-wrap">
        {loading ? <Spinner /> : error ? <ErrorBox msg={error} retry={reload} /> : data.length === 0 ? <Empty>{empty}</Empty> : (
          <table>
            <thead><tr>{columns.map(c => <th key={c.key}>{c.label}</th>)}<th /></tr></thead>
            <tbody>{data.map(i => (
              <tr key={i.id}>
                {columns.map(c => <td key={c.key}>{c.render ? c.render(i[c.key]) : (i[c.key] || '—')}</td>)}
                <td className="actions"><button className="link" onClick={() => open(i)}>Editar</button><button className="link del" onClick={() => remove(i)}>Excluir</button></td>
              </tr>
            ))}</tbody>
          </table>
        )}
      </div>
      {form && (
        <Modal title={form.id ? `Editar ${title.toLowerCase()}` : `Novo registro`} onClose={() => setForm(null)}>
          <form className="form" onSubmit={save}>
            {fields.map(f => (
              <Field key={f.name} label={f.label}>
                {f.options ? <select value={v[f.name]} onChange={e => setV({ ...v, [f.name]: e.target.value })}>
                  {Object.entries(f.options).map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select>
                  : f.area ? <textarea rows={3} maxLength={f.max} value={v[f.name]} onChange={e => setV({ ...v, [f.name]: e.target.value })} required={f.required} />
                  : <input type={f.type || 'text'} maxLength={f.max} value={v[f.name]} onChange={e => setV({ ...v, [f.name]: e.target.value })} required={f.required} />}
              </Field>
            ))}
            <FormErrors err={err} />
            <footer><button type="button" className="btn ghost" onClick={() => setForm(null)}>Cancelar</button><button className="btn">Salvar</button></footer>
          </form>
        </Modal>
      )}
    </>
  );
}

export const Clientes = () => (
  <Crud title="Clientes" subtitle="Quem abre os chamados" path="/clientes" empty="Nenhum cliente cadastrado."
    fields={[
      { name: 'nome', label: 'Nome', max: 100, required: true },
      { name: 'email', label: 'E-mail', type: 'email', max: 120, required: true },
      { name: 'telefone', label: 'Telefone', max: 20 },
    ]}
    columns={[{ key: 'nome', label: 'Nome' }, { key: 'email', label: 'E-mail' }, { key: 'telefone', label: 'Telefone' }]} />
);

export const Tecnicos = () => (
  <Crud title="Técnicos" subtitle="Equipe responsável pelos atendimentos" path="/tecnicos" empty="Nenhum técnico cadastrado."
    fields={[
      { name: 'nome', label: 'Nome', max: 100, required: true },
      { name: 'email', label: 'E-mail', type: 'email', max: 120, required: true },
      { name: 'perfil', label: 'Perfil', options: { TECNICO: 'Técnico', ADMIN: 'Administrador' } },
    ]}
    columns={[{ key: 'nome', label: 'Nome' }, { key: 'email', label: 'E-mail' },
      { key: 'perfil', label: 'Perfil', render: p => <Badge kind={p === 'ADMIN' ? 'pr-ALTA' : 'st-ABERTO'}>{p === 'ADMIN' ? 'Administrador' : 'Técnico'}</Badge> }]} />
);

export const Categorias = () => (
  <Crud title="Categorias" subtitle="Classificação dos chamados" path="/categorias" empty="Nenhuma categoria cadastrada."
    fields={[
      { name: 'nome', label: 'Nome', max: 100, required: true },
      { name: 'descricao', label: 'Descrição', max: 400, required: true, area: true },
    ]}
    columns={[{ key: 'nome', label: 'Nome' }, { key: 'descricao', label: 'Descrição' }]} />
);
