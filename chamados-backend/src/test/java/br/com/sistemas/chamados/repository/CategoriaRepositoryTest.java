package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Categoria;

@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository repository;

    @Test
    void salvaCategoriaEGeraId() {
        Categoria salva = repository.save(new Categoria("Hardware", "Problemas de hardware"));

        assertThat(salva.getId()).isNotNull();
        assertThat(repository.findById(salva.getId())).isPresent();
    }

    @Test
    void existsByNomeRetornaTrueQuandoNomeExiste() {
        repository.save(new Categoria("Hardware", "Problemas de hardware"));

        assertThat(repository.existsByNome("Hardware")).isTrue();
        assertThat(repository.existsByNome("Software")).isFalse();
    }

    @Test
    void existsByNomeAndIdNotIgnoraARegistroAtual() {
        Categoria salva = repository.save(new Categoria("Hardware", "Problemas de hardware"));

        assertThat(repository.existsByNomeAndIdNot("Hardware", salva.getId())).isFalse();
        assertThat(repository.existsByNomeAndIdNot("Hardware", salva.getId() + 1)).isTrue();
    }

    @Test
    void naoPermiteNomeDuplicado() {
        repository.saveAndFlush(new Categoria("Hardware", "Problemas de hardware"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Categoria("Hardware", "Outra descrição")));
    }
}
