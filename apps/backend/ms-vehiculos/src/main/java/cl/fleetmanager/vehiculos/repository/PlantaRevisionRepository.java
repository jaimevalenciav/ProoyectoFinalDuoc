package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.PlantaRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlantaRevisionRepository extends JpaRepository<PlantaRevision, String> {
    List<PlantaRevision> findByEmpresaIdAndEliminadoOrderByNombreAsc(String empresaId, int eliminado);
}
