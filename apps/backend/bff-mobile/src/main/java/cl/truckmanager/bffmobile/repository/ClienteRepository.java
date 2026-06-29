package cl.truckmanager.bffmobile.repository;

import cl.truckmanager.bffmobile.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
}
