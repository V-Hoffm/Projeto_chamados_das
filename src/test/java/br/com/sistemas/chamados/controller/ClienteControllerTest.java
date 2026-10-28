package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ClienteController.class)
@EnableSpringDataWebSupport
class ClienteControllerTest {

    private static final String JSON_VALIDO = """
            {"nome": "Maria", "email": "maria@email.com", "telefone": "47999999999"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService service;

    @MockitoBean
    private ChamadoService chamadoService;

    @Test
    void criaCliente() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
                .thenReturn(new ClienteResponse(1L, "Maria", "maria@email.com", "47999999999"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/clientes/1"))
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    void rejeitaClienteInvalido() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"email\": \"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
                .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail maria@email.com"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void listaClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(new ClienteResponse(1L, "Maria", "maria@email.com", null)));

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria"));
    }

    @Test
    void buscaClientePorId() throws Exception {
        when(service.buscar(1L)).thenReturn(new ClienteResponse(1L, "Maria", "maria@email.com", null));

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void retorna404QuandoClienteNaoExiste() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizaCliente() throws Exception {
        when(service.atualizar(any(Long.class), any(ClienteRequest.class)))
                .thenReturn(new ClienteResponse(1L, "Maria", "maria@email.com", "47999999999"));

        mockMvc.perform(put("/clientes/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void excluiCliente() throws Exception {
        mockMvc.perform(delete("/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void retorna404AoExcluirClienteInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado")).when(service).excluir(99L);

        mockMvc.perform(delete("/clientes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaChamadosDoCliente() throws Exception {
        ChamadoResponse chamado = new ChamadoResponse(1L, "Computador não liga", "Descrição",
                LocalDateTime.now(), null, Prioridade.ALTA, StatusChamado.ABERTO, 1L, 2L, 3L);
        when(chamadoService.listarPorCliente(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(chamado)));

        mockMvc.perform(get("/clientes/1/chamados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Computador não liga"));
    }

    @Test
    void retorna404AoListarChamadosDeClienteInexistente() throws Exception {
        when(chamadoService.listarPorCliente(eq(99L), any(Pageable.class)))
                .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99/chamados"))
                .andExpect(status().isNotFound());
    }
}
