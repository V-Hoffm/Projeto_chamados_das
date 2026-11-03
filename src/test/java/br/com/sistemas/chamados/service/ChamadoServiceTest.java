package br.com.sistemas.chamados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository repository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private ComentarioService comentarioService;

    @InjectMocks
    private ChamadoService service;

    private Cliente cliente() {
        return new Cliente("Maria", "maria@email.com", "47999999999");
    }

    private Categoria categoria() {
        return new Categoria("Hardware", "Problemas de hardware");
    }

    private Tecnico tecnico() {
        return new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO);
    }

    private Chamado chamadoAberto() {
        return new Chamado("Computador não liga", "Descrição", Prioridade.ALTA,
                cliente(), categoria(), tecnico());
    }

    private Chamado chamadoFechado() {
        Chamado chamado = chamadoAberto();
        chamado.setStatus(StatusChamado.FECHADO);
        return chamado;
    }

    @Test
    void criaChamadoAbertoComDataAbertura() {
        ChamadoRequest dto = new ChamadoRequest("Computador não liga", "Descrição",
                Prioridade.ALTA, 1L, 2L, 3L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria()));
        when(tecnicoRepository.findById(3L)).thenReturn(Optional.of(tecnico()));
        when(repository.save(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

        ChamadoResponse resposta = service.criar(dto);

        assertThat(resposta.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(resposta.dataAbertura()).isNotNull();
        assertThat(resposta.dataFechamento()).isNull();
    }

    @Test
    void naoCriaChamadoParaClienteInexistente() {
        ChamadoRequest dto = new ChamadoRequest("Computador não liga", "Descrição",
                Prioridade.ALTA, 1L, 2L, 3L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.criar(dto));
    }

    @Test
    void buscaChamadoInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscar(99L));
    }

    @Test
    void transicaoValidaAlteraStatusSemPreencherDataFechamento() {
        Chamado chamado = chamadoAberto();
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));

        ChamadoResponse resposta = service.alterarStatus(1L,
                new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO));

        assertThat(resposta.status()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        assertThat(resposta.dataFechamento()).isNull();
    }

    @Test
    void fecharChamadoPreencheDataFechamento() {
        Chamado chamado = chamadoAberto();
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));

        ChamadoResponse resposta = service.alterarStatus(1L,
                new AlterarStatusRequest(StatusChamado.FECHADO));

        assertThat(resposta.status()).isEqualTo(StatusChamado.FECHADO);
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @Test
    void transicaoInvalidaChamadoFechadoNaoPodeMudarDeStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoFechado()));

        assertThrows(RegraNegocioException.class, () -> service.alterarStatus(1L,
                new AlterarStatusRequest(StatusChamado.ABERTO)));
    }

    @Test
    void atribuiTecnicoEmChamadoAberto() {
        Chamado chamado = chamadoAberto();
        Tecnico novoTecnico = new Tecnico("Ana", "ana@email.com", Perfil.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));
        when(tecnicoRepository.findById(5L)).thenReturn(Optional.of(novoTecnico));

        service.atribuir(1L, new AtribuirTecnicoRequest(5L));

        assertThat(chamado.getTecnico()).isSameAs(novoTecnico);
    }

    @Test
    void naoAtribuiTecnicoEmChamadoFechado() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoFechado()));

        assertThrows(RegraNegocioException.class,
                () -> service.atribuir(1L, new AtribuirTecnicoRequest(5L)));
        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void naoAtribuiTecnicoInexistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoAberto()));
        when(tecnicoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.atribuir(1L, new AtribuirTecnicoRequest(5L)));
    }

    @Test
    void naoAtualizaChamadoFechado() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoFechado()));
        ChamadoRequest dto = new ChamadoRequest("Novo título", "Nova descrição",
                Prioridade.BAIXA, 1L, 2L, 3L);

        assertThrows(RegraNegocioException.class, () -> service.atualizar(1L, dto));
    }

    @Test
    void naoExcluiChamadoFechado() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoFechado()));

        assertThrows(RegraNegocioException.class, () -> service.excluir(1L));
        verifyNoInteractions(comentarioService);
    }
}
