package cl.fleetmanager.taller.service;

import cl.fleetmanager.taller.dto.TareaDefinicionDto;
import cl.fleetmanager.taller.entity.TareaDefArticulo;
import cl.fleetmanager.taller.entity.TareaDefinicion;
import cl.fleetmanager.taller.repository.TareaDefinicionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TareaDefinicionService {

    private final TareaDefinicionRepository repo;

    public Page<TareaDefinicion> getAll(String emp, int page, int size) {
        return repo.findByEmpresaIdAndActivoOrderByNombreAsc(emp, 1, PageRequest.of(page, size));
    }

    public List<TareaDefinicion> getAllActivos(String emp) {
        return repo.findByEmpresaIdAndActivoOrderByNombreAsc(emp, 1);
    }

    public TareaDefinicion getById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TareaDefinicion no encontrada: " + id));
    }

    public TareaDefinicion crear(String emp, TareaDefinicionDto dto) {
        TareaDefinicion td = TareaDefinicion.builder()
                .empresaId(emp)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .tipoOt(dto.getTipoOt())
                .articulos(new ArrayList<>())
                .build();

        if (dto.getArticulos() != null) {
            for (TareaDefinicionDto.TareaDefArticuloDto artDto : dto.getArticulos()) {
                TareaDefArticulo art = TareaDefArticulo.builder()
                        .tareaDefinicion(td)
                        .repuestoId(artDto.getRepuestoId())
                        .cantidad(artDto.getCantidad())
                        .build();
                td.getArticulos().add(art);
            }
        }

        return repo.save(td);
    }

    public TareaDefinicion actualizar(String id, TareaDefinicionDto dto) {
        TareaDefinicion td = getById(id);
        td.setNombre(dto.getNombre());
        td.setDescripcion(dto.getDescripcion());
        td.setTipoOt(dto.getTipoOt());

        // Replace articulos
        td.getArticulos().clear();
        if (dto.getArticulos() != null) {
            for (TareaDefinicionDto.TareaDefArticuloDto artDto : dto.getArticulos()) {
                TareaDefArticulo art = TareaDefArticulo.builder()
                        .tareaDefinicion(td)
                        .repuestoId(artDto.getRepuestoId())
                        .cantidad(artDto.getCantidad())
                        .build();
                td.getArticulos().add(art);
            }
        }

        return repo.save(td);
    }

    public void eliminar(String id) {
        TareaDefinicion td = getById(id);
        td.setActivo(0);
        repo.save(td);
    }
}
