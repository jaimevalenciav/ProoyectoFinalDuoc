package cl.fleetmanager.operaciones.repository;

import cl.fleetmanager.operaciones.entity.AlertaCombustible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaCombustibleRepository extends JpaRepository<AlertaCombustible, String> {

    /** Alertas activas (leida=0) o leídas (leida=1) por empresa */
    List<AlertaCombustible> findByEmpresaIdAndLeidaOrderByCreatedAtDesc(String empresaId, int leida);

    /** Todas las alertas de la empresa, más recientes primero */
    List<AlertaCombustible> findByEmpresaIdOrderByCreatedAtDesc(String empresaId);
}
