package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository repository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    private Cliente cliente;
    private Cliente outroCliente;
    private Categoria categoria;
    private Tecnico tecnico;

    @BeforeEach
    void preparar() {
        cliente = clienteRepository.save(new Cliente("Maria", "maria@email.com", "47999999999"));
        outroCliente = clienteRepository.save(new Cliente("João", "joao@email.com", null));
        categoria = categoriaRepository.save(new Categoria("Hardware", "Problemas de hardware"));
        tecnico = tecnicoRepository.save(new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO));
    }

    private Chamado novoChamado(String titulo, Prioridade prioridade, Cliente dono) {
        return repository.save(new Chamado(titulo, "Descrição", prioridade, dono, categoria, tecnico));
    }

    @Test
    void persisteChamadoComRelacionamentos() {
        Chamado salvo = novoChamado("Computador não liga", Prioridade.ALTA, cliente);

        Chamado encontrado = repository.findById(salvo.getId()).orElseThrow();

        assertThat(encontrado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(encontrado.getDataAbertura()).isNotNull();
        assertThat(encontrado.getDataFechamento()).isNull();
        assertThat(encontrado.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(encontrado.getCategoria().getId()).isEqualTo(categoria.getId());
        assertThat(encontrado.getTecnico().getId()).isEqualTo(tecnico.getId());
    }

    @Test
    void filtraPorStatus() {
        novoChamado("Chamado 1", Prioridade.BAIXA, cliente);
        Chamado fechado = novoChamado("Chamado 2", Prioridade.BAIXA, cliente);
        fechado.setStatus(StatusChamado.FECHADO);

        Page<Chamado> resultado = repository.filtrar(StatusChamado.FECHADO, null, null, Pageable.unpaged());

        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("Chamado 2");
    }

    @Test
    void filtraPorPrioridade() {
        novoChamado("Chamado 1", Prioridade.BAIXA, cliente);
        novoChamado("Chamado 2", Prioridade.CRITICA, cliente);

        Page<Chamado> resultado = repository.filtrar(null, Prioridade.CRITICA, null, Pageable.unpaged());

        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("Chamado 2");
    }

    @Test
    void filtraPorCliente() {
        novoChamado("Chamado 1", Prioridade.BAIXA, cliente);
        novoChamado("Chamado 2", Prioridade.BAIXA, outroCliente);

        Page<Chamado> resultado = repository.filtrar(null, null, outroCliente.getId(), Pageable.unpaged());

        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("Chamado 2");
    }

    @Test
    void combinaFiltros() {
        novoChamado("Chamado 1", Prioridade.ALTA, cliente);
        novoChamado("Chamado 2", Prioridade.ALTA, outroCliente);
        novoChamado("Chamado 3", Prioridade.BAIXA, outroCliente);

        Page<Chamado> resultado = repository.filtrar(
                StatusChamado.ABERTO, Prioridade.ALTA, outroCliente.getId(), Pageable.unpaged());

        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("Chamado 2");
    }

    @Test
    void semFiltrosRetornaTodos() {
        novoChamado("Chamado 1", Prioridade.ALTA, cliente);
        novoChamado("Chamado 2", Prioridade.BAIXA, outroCliente);

        Page<Chamado> resultado = repository.filtrar(null, null, null, Pageable.unpaged());

        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void paginaEOrdenaResultados() {
        novoChamado("B", Prioridade.BAIXA, cliente);
        novoChamado("A", Prioridade.BAIXA, cliente);
        novoChamado("C", Prioridade.BAIXA, cliente);

        Page<Chamado> pagina = repository.filtrar(null, null, null,
                PageRequest.of(0, 2, Sort.by("titulo")));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(Chamado::getTitulo).containsExactly("A", "B");
    }

    @Test
    void persisteEListaComentariosDoChamado() {
        Chamado chamado = novoChamado("Chamado 1", Prioridade.MEDIA, cliente);
        Chamado outro = novoChamado("Chamado 2", Prioridade.MEDIA, cliente);
        comentarioRepository.save(new Comentario("Primeiro", "Carlos", chamado));
        comentarioRepository.save(new Comentario("Segundo", "Maria", chamado));
        comentarioRepository.save(new Comentario("De outro chamado", "Maria", outro));

        assertThat(comentarioRepository.findByChamadoIdOrderByDataAsc(chamado.getId()))
                .extracting(Comentario::getTexto)
                .containsExactlyInAnyOrder("Primeiro", "Segundo");
    }

    @Test
    void excluiComentariosPorChamado() {
        Chamado chamado = novoChamado("Chamado 1", Prioridade.MEDIA, cliente);
        comentarioRepository.save(new Comentario("Primeiro", "Carlos", chamado));

        comentarioRepository.deleteByChamadoId(chamado.getId());

        assertThat(comentarioRepository.findByChamadoIdOrderByDataAsc(chamado.getId())).isEmpty();
    }
}
