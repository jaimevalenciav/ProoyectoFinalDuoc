package cl.truckmanager.vehiculos.repository;

import cl.truckmanager.vehiculos.entity.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    @Query("SELECT v FROM Vehiculo v WHERE v.empresaId = :idEmpresa " +
           "AND (:estado IS NULL OR v.estado = :estado) " +
           "AND (:busqueda IS NULL OR LOWER(v.patente) LIKE LOWER(CONCAT('%',:busqueda,'%')) " +
           "     OR LOWER(v.marca) LIKE LOWER(CONCAT('%',:busqueda,'%')))")
    Page<Vehiculo> buscarPorFiltros(
        @Param("idEmpresa") String idEmpresa,
        @Param("estado") String estado,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

    Optional<Vehiculo> findByQrCode(String qrCode);

    /** Busca un vehículo validando que pertenezca a la empresa — evita IDOR */
    Optional<Vehiculo> findByIdAndEmpresaId(String id, String empresaId);
}
