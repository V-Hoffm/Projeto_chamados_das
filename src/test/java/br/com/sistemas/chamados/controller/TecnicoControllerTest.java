package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(TecnicoController.class)
class TecnicoControllerTest {

    private static final String JSON_VALIDO = """
            {"nome": "Carlos", "email": "carlos@email.com", "perfil": "TECNICO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TecnicoService service;

    @Test
    void criaTecnico() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
                .thenReturn(new TecnicoResponse(1L, "Carlos", "carlos@email.com", Perfil.TECNICO));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/tecnicos/1"))
                .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void rejeitaTecnicoInvalido() throws Exception {
        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"email\": \"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
                .thenThrow(new RegraNegocioException("Já existe técnico com o e-mail carlos@email.com"));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void listaTecnicos() throws Exception {
        when(service.listar()).thenReturn(List.of(new TecnicoResponse(1L, "Carlos", "carlos@email.com", Perfil.TECNICO)));

        mockMvc.perform(get("/tecnicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Carlos"));
    }

    @Test
    void buscaTecnicoPorId() throws Exception {
        when(service.buscar(1L)).thenReturn(new TecnicoResponse(1L, "Carlos", "carlos@email.com", Perfil.TECNICO));

        mockMvc.perform(get("/tecnicos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void retorna404QuandoTecnicoNaoExiste() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Técnico 99 não encontrado"));

        mockMvc.perform(get("/tecnicos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizaTecnico() throws Exception {
        when(service.atualizar(any(Long.class), any(TecnicoRequest.class)))
                .thenReturn(new TecnicoResponse(1L, "Carlos", "carlos@email.com", Perfil.TECNICO));

        mockMvc.perform(put("/tecnicos/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void excluiTecnico() throws Exception {
        mockMvc.perform(delete("/tecnicos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void retorna404AoExcluirTecnicoInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Técnico 99 não encontrado")).when(service).excluir(99L);

        mockMvc.perform(delete("/tecnicos/99"))
                .andExpect(status().isNotFound());
    }
}
