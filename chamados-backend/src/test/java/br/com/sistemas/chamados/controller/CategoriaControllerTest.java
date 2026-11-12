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

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest {

    private static final String JSON_VALIDO = """
            {"nome": "Hardware", "descricao": "Problemas de hardware"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService service;

    @Test
    void criaCategoria() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
                .thenReturn(new CategoriaResponse(1L, "Hardware", "Problemas de hardware"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/categorias/1"))
                .andExpect(jsonPath("$.nome").value("Hardware"));
    }

    @Test
    void rejeitaCategoriaInvalida() throws Exception {
        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"descricao\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    void rejeitaCategoriaDuplicada() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
                .thenThrow(new RegraNegocioException("Já existe categoria com o nome Hardware"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void listaCategorias() throws Exception {
        when(service.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Hardware", "Problemas de hardware")));

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Hardware"));
    }

    @Test
    void buscaCategoriaPorId() throws Exception {
        when(service.buscar(1L)).thenReturn(new CategoriaResponse(1L, "Hardware", "Problemas de hardware"));

        mockMvc.perform(get("/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void retorna404QuandoCategoriaNaoExiste() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrada"));

        mockMvc.perform(get("/categorias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizaCategoria() throws Exception {
        when(service.atualizar(any(Long.class), any(CategoriaRequest.class)))
                .thenReturn(new CategoriaResponse(1L, "Hardware", "Problemas de hardware"));

        mockMvc.perform(put("/categorias/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void excluiCategoria() throws Exception {
        mockMvc.perform(delete("/categorias/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void retorna404AoExcluirCategoriaInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrada")).when(service).excluir(99L);

        mockMvc.perform(delete("/categorias/99"))
                .andExpect(status().isNotFound());
    }
}
