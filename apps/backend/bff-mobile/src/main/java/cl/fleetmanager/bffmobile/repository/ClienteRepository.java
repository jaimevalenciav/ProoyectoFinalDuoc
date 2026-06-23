package cl.fleetmanager.bffmobile.repository;

import cl.fleetmanager.bffmobile.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
}
