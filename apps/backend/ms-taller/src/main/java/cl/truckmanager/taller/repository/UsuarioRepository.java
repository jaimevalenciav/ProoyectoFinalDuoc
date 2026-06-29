package cl.truckmanager.taller.repository;

import cl.truckmanager.taller.entity.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioSistema, String> {
    Optional<UsuarioSistema> findByAzureOid(String azureOid);
}
