# Chamados – Frontend (React + Vite)

1. Suba o backend Spring Boot (`./mvnw spring-boot:run`, porta 8080).
2. Neste diretório: `npm install` e `npm run dev` → http://localhost:5173

As chamadas usam o prefixo `/api`, redirecionado pelo Vite para `http://localhost:8080` (ver `vite.config.js`), então não é preciso alterar o backend.
Para produção, `npm run build` gera a pasta `dist/`.

Telas: Dashboard, Chamados (lista com filtros e paginação, novo/editar, detalhe com status, técnico e comentários), Clientes, Técnicos e Categorias.
