package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Tecnico;

@DataJpaTest
class TecnicoRepositoryTest {

    @Autowired
    private TecnicoRepository repository;

    @Test
    void salvaTecnicoEGeraId() {
        Tecnico salvo = repository.save(new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO));

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).isPresent();
    }

    @Test
    void existsByEmailRetornaTrueQuandoEmailExiste() {
        repository.save(new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO));

        assertThat(repository.existsByEmail("carlos@email.com")).isTrue();
        assertThat(repository.existsByEmail("outro@email.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraORegistroAtual() {
        Tecnico salvo = repository.save(new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO));

        assertThat(repository.existsByEmailAndIdNot("carlos@email.com", salvo.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("carlos@email.com", salvo.getId() + 1)).isTrue();
    }

    @Test
    void naoPermiteEmailDuplicado() {
        repository.saveAndFlush(new Tecnico("Carlos", "carlos@email.com", Perfil.TECNICO));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Tecnico("Ana", "carlos@email.com", Perfil.ADMIN)));
    }
}
