package cl.truckmanager.bffmobile.repository;

import cl.truckmanager.bffmobile.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, String> {
    Optional<Conductor> findByEmailAndEliminado(String email, Integer eliminado);
    Optional<Conductor> findByAzureOidAndEliminado(String azureOid, Integer eliminado);
}
