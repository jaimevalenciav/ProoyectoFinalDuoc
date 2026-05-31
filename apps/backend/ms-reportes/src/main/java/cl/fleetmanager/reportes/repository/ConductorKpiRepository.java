package cl.fleetmanager.reportes.repository;

import cl.fleetmanager.reportes.entity.ConductorKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConductorKpiRepository extends JpaRepository<ConductorKpi, String> {

    @Query("SELECT COUNT(c) FROM ConductorKpi c WHERE c.empresaId = :emp AND c.estado = 'ACTIVO'")
    long countConductores(@Param("emp") String emp);
}
