package cl.truckmanager.taller.repository;

import cl.truckmanager.taller.entity.TareaDefinicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaDefinicionRepository extends JpaRepository<TareaDefinicion, String> {

    Page<TareaDefinicion> findByEmpresaIdAndActivoOrderByNombreAsc(String empresaId, Integer activo, Pageable pageable);

    List<TareaDefinicion> findByEmpresaIdAndActivoOrderByNombreAsc(String empresaId, Integer activo);
}
