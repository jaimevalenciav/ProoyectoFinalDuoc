package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.RevisionTecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RevisionTecnicaRepository extends JpaRepository<RevisionTecnica, String> {
    List<RevisionTecnica> findByVehiculoIdOrderByFechaRevisionDescCreatedAtDesc(String vehiculoId);
}
