package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.ConductorKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConductorKpiRepository extends JpaRepository<ConductorKpi, String> {

    @Query(value = "SELECT COUNT(*) FROM CONDUCTORES WHERE EMPRESA_ID = :emp AND ELIMINADO = 0", nativeQuery = true)
    long countConductores(@Param("emp") String emp);
}
