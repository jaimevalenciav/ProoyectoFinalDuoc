package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.VehiculoKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehiculoKpiRepository extends JpaRepository<VehiculoKpi, String> {

    @Query(value = "SELECT COUNT(*) FROM VEHICULOS WHERE EMPRESA_ID = :emp AND ELIMINADO = 0", nativeQuery = true)
    long countVehiculos(@Param("emp") String emp);

    @Query(value = "SELECT COUNT(*) FROM VEHICULOS WHERE EMPRESA_ID = :emp AND ELIMINADO = 0 AND ESTADO = 'OPERATIVO'", nativeQuery = true)
    long countOperativos(@Param("emp") String emp);

    @Query(value = "SELECT COUNT(*) FROM VEHICULOS WHERE EMPRESA_ID = :emp AND ELIMINADO = 0 AND ESTADO = 'EN_TALLER'", nativeQuery = true)
    long countEnTaller(@Param("emp") String emp);
}
