package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}