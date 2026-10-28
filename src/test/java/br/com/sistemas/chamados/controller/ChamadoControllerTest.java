package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ComentarioService;

@WebMvcTest(ChamadoController.class)
@EnableSpringDataWebSupport
class ChamadoControllerTest {

    private static final String JSON_VALIDO = """
            {"titulo": "Computador não liga", "descricao": "Não dá sinal de vídeo",
             "prioridade": "ALTA", "clienteId": 1, "categoriaId": 2, "tecnicoId": 3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService service;

    @MockitoBean
    private ComentarioService comentarioService;

    private ChamadoResponse chamado(StatusChamado status) {
        return new ChamadoResponse(1L, "Computador não liga", "Não dá sinal de vídeo",
                LocalDateTime.now(), null, Prioridade.ALTA, status, 1L, 2L, 3L);
    }

    @Test
    void criaChamado() throws Exception {
        when(service.criar(any(ChamadoRequest.class))).thenReturn(chamado(StatusChamado.ABERTO));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/chamados/1"))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void rejeitaChamadoInvalido() throws Exception {
        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    void retorna404AoCriarChamadoComClienteInexistente() throws Exception {
        when(service.criar(any(ChamadoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Cliente 1 não encontrado"));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaChamadosComFiltros() throws Exception {
        when(service.listar(eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(chamado(StatusChamado.ABERTO))));

        mockMvc.perform(get("/chamados?status=ABERTO&prioridade=ALTA&clienteId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Computador não liga"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listaChamadosSemFiltros() throws Exception {
        when(service.listar(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(chamado(StatusChamado.ABERTO))));

        mockMvc.perform(get("/chamados?page=0&size=5&sort=titulo,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void buscaChamadoComComentarios() throws Exception {
        ComentarioResponse comentario = new ComentarioResponse(1L, "Em análise", LocalDateTime.now(), "Carlos", 1L);
        ChamadoDetalheResponse detalhe = new ChamadoDetalheResponse(1L, "Computador não liga",
                "Não dá sinal de vídeo", LocalDateTime.now(), null, Prioridade.ALTA,
                StatusChamado.ABERTO, 1L, 2L, 3L, List.of(comentario));
        when(service.buscar(1L)).thenReturn(detalhe);

        mockMvc.perform(get("/chamados/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarios[0].texto").value("Em análise"));
    }

    @Test
    void retorna404QuandoChamadoNaoExiste() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(get("/chamados/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizaChamado() throws Exception {
        when(service.atualizar(any(Long.class), any(ChamadoRequest.class)))
                .thenReturn(chamado(StatusChamado.ABERTO));

        mockMvc.perform(put("/chamados/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void rejeitaAtualizacaoDeChamadoFechado() throws Exception {
        when(service.atualizar(any(Long.class), any(ChamadoRequest.class)))
                .thenThrow(new RegraNegocioException("Chamado 1 está fechado e não pode ser alterado"));

        mockMvc.perform(put("/chamados/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void excluiChamado() throws Exception {
        mockMvc.perform(delete("/chamados/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void retorna404AoExcluirChamadoInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado")).when(service).excluir(99L);

        mockMvc.perform(delete("/chamados/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atribuiTecnico() throws Exception {
        when(service.atribuir(eq(1L), any(AtribuirTecnicoRequest.class)))
                .thenReturn(chamado(StatusChamado.ABERTO));

        mockMvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoId").value(3));
    }

    @Test
    void rejeitaAtribuicaoSemTecnico() throws Exception {
        mockMvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaAtribuicaoEmChamadoFechado() throws Exception {
        when(service.atribuir(eq(1L), any(AtribuirTecnicoRequest.class)))
                .thenThrow(new RegraNegocioException("Não é possível atribuir técnico ao chamado 1 pois ele está fechado"));

        mockMvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": 3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void alteraStatusComTransicaoValida() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
                .thenReturn(chamado(StatusChamado.EM_ATENDIMENTO));

        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"EM_ATENDIMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
    }

    @Test
    void rejeitaTransicaoInvalidaDeChamadoFechado() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
                .thenThrow(new RegraNegocioException("Chamado 1 está fechado e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ABERTO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejeitaStatusInexistente() throws Exception {
        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criaComentario() throws Exception {
        when(comentarioService.criar(eq(1L), any(ComentarioRequest.class)))
                .thenReturn(new ComentarioResponse(1L, "Em análise", LocalDateTime.now(), "Carlos", 1L));

        mockMvc.perform(post("/chamados/1/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Em análise\", \"autor\": \"Carlos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chamadoId").value(1));
    }

    @Test
    void rejeitaComentarioInvalido() throws Exception {
        mockMvc.perform(post("/chamados/1/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"\", \"autor\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    void retorna404AoComentarChamadoInexistente() throws Exception {
        when(comentarioService.criar(eq(99L), any(ComentarioRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(post("/chamados/99/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Em análise\", \"autor\": \"Carlos\"}"))
                .andExpect(status().isNotFound());
    }
}
