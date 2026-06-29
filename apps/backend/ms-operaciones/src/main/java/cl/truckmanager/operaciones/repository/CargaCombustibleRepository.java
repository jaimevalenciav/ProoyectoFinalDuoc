package cl.truckmanager.operaciones.repository;

import cl.truckmanager.operaciones.entity.CargaCombustible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CargaCombustibleRepository extends JpaRepository<CargaCombustible, String> {

    @Query("SELECT c FROM CargaCombustible c WHERE c.empresaId = :empresaId " +
           "AND (:vehiculoId IS NULL OR c.vehiculoId = :vehiculoId) " +
           "AND (:desde     IS NULL OR c.fechaCarga >= :desde) " +
           "AND (:hasta     IS NULL OR c.fechaCarga <= :hasta)")
    Page<CargaCombustible> buscarPorFiltros(
        @Param("empresaId")  String empresaId,
        @Param("vehiculoId") String vehiculoId,
        @Param("desde")      LocalDate desde,
        @Param("hasta")      LocalDate hasta,
        Pageable pageable
    );

    /** Última carga del vehículo para calcular consumo */
    List<CargaCombustible> findTop2ByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(String vehiculoId);

    /** La más reciente para validación en el formulario */
    java.util.Optional<CargaCombustible> findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(String vehiculoId);

    /** Cargas con consumo anómalo (>35 L/100km o <5 L/100km) */
    @Query("SELECT c FROM CargaCombustible c WHERE c.empresaId = :empresaId " +
           "AND c.consumo100km IS NOT NULL " +
           "AND (c.consumo100km > 35 OR c.consumo100km < 5)")
    List<CargaCombustible> findAnomalias(@Param("empresaId") String empresaId);
}
