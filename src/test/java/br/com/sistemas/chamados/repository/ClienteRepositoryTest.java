package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void salvaClienteEGeraId() {
        Cliente salvo = repository.save(new Cliente("Maria", "maria@email.com", "47999999999"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).isPresent();
    }

    @Test
    void existsByEmailRetornaTrueQuandoEmailExiste() {
        repository.save(new Cliente("Maria", "maria@email.com", "47999999999"));

        assertThat(repository.existsByEmail("maria@email.com")).isTrue();
        assertThat(repository.existsByEmail("outro@email.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraORegistroAtual() {
        Cliente salvo = repository.save(new Cliente("Maria", "maria@email.com", "47999999999"));

        assertThat(repository.existsByEmailAndIdNot("maria@email.com", salvo.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("maria@email.com", salvo.getId() + 1)).isTrue();
    }

    @Test
    void naoPermiteEmailDuplicado() {
        repository.saveAndFlush(new Cliente("Maria", "maria@email.com", "47999999999"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Cliente("João", "maria@email.com", null)));
    }
}
