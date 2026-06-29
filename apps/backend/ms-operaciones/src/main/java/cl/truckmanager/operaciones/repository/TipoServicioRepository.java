package cl.truckmanager.operaciones.repository;

import cl.truckmanager.operaciones.entity.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, String> {
    List<TipoServicio> findByEmpresaIdAndActivo(String empresaId, Integer activo);
    List<TipoServicio> findByEmpresaId(String empresaId);
}
