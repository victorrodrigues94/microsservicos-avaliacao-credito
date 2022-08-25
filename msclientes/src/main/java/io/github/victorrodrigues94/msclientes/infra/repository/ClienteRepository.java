package io.github.victorrodrigues94.msclientes.infra.repository;

import io.github.victorrodrigues94.msclientes.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    public Optional<Cliente> findByCpf(String cpf);
}
