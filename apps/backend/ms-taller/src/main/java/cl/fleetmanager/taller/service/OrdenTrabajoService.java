package cl.fleetmanager.taller.service;

import cl.fleetmanager.taller.dto.*;
import cl.fleetmanager.taller.entity.*;
import cl.fleetmanager.taller.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository repositorio;
    private final TareaOTRepository tareasRepo;

    public Page<OrdenTrabajo> obtenerTodos(String idEmpresa, String estado, String tipo, String vehiculoId, int pagina, int tamano) {
        return repositorio.buscarPorFiltros(
            idEmpresa,
            (estado     != null && !estado.isBlank())     ? estado     : null,
            (tipo       != null && !tipo.isBlank())       ? tipo       : null,
            (vehiculoId != null && !vehiculoId.isBlank()) ? vehiculoId : null,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaApertura"))
        );
    }

    public OrdenTrabajo obtenerPorId(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Orden de trabajo no encontrada: " + id));
    }

    public OrdenTrabajo crear(String idEmpresa, OrdenTrabajoDto datos) {
        String numero = generarNumero(idEmpresa);
        OrdenTrabajo ot = OrdenTrabajo.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(idEmpresa)
            .numero(numero)
            .vehiculoId(datos.getVehiculoId())
            .tipo(datos.getTipo())
            .descripcion(datos.getDescripcion())
            .mecanicoResponsable(datos.getMecanicoResponsable())
            .fechaCierreEst(datos.getFechaCierreEst())
            .costoManoObra(datos.getCostoManoObra() != null ? datos.getCostoManoObra() : BigDecimal.ZERO)
            .notas(datos.getNotas())
            .build();

        OrdenTrabajo guardada = repositorio.save(ot);

        if (datos.getTareas() != null && !datos.getTareas().isEmpty()) {
            AtomicInteger orden = new AtomicInteger(0);
            datos.getTareas().forEach(t -> {
                TareaOT tarea = TareaOT.builder()
                    .id(UUID.randomUUID().toString())
                    .ordenTrabajo(guardada)
                    .descripcion(t.getDescripcion())
                    .orden(t.getOrden() != null ? t.getOrden() : orden.getAndIncrement())
                    .build();
                tareasRepo.save(tarea);
            });
        }

        return repositorio.findById(guardada.getId()).orElse(guardada);
    }

    public OrdenTrabajo actualizar(String id, OrdenTrabajoDto datos) {
        OrdenTrabajo ot = obtenerPorId(id);
        if (datos.getDescripcion()         != null) ot.setDescripcion(datos.getDescripcion());
        if (datos.getMecanicoResponsable()  != null) ot.setMecanicoResponsable(datos.getMecanicoResponsable());
        if (datos.getFechaCierreEst()       != null) ot.setFechaCierreEst(datos.getFechaCierreEst());
        if (datos.getCostoManoObra()        != null) ot.setCostoManoObra(datos.getCostoManoObra());
        if (datos.getNotas()               != null) ot.setNotas(datos.getNotas());
        return repositorio.save(ot);
    }

    public OrdenTrabajo cerrar(String id, BigDecimal costoManoObra, String notas) {
        OrdenTrabajo ot = obtenerPorId(id);
        ot.setEstado("CERRADA");
        ot.setFechaCierreReal(LocalDate.now());
        ot.setAvance(100);
        if (costoManoObra != null) ot.setCostoManoObra(costoManoObra);
        if (notas         != null) ot.setNotas(notas);
        return repositorio.save(ot);
    }

    public TareaOT agregarTarea(String otId, TareaOTDto datos) {
        OrdenTrabajo ot = obtenerPorId(otId);
        int proximoOrden = ot.getTareas().size();
        TareaOT tarea = TareaOT.builder()
            .id(UUID.randomUUID().toString())
            .ordenTrabajo(ot)
            .descripcion(datos.getDescripcion())
            .orden(datos.getOrden() != null ? datos.getOrden() : proximoOrden)
            .build();
        TareaOT guardada = tareasRepo.save(tarea);
        recalcularAvance(ot);
        return guardada;
    }

    public OrdenTrabajo completarTarea(String otId, String tareaId) {
        TareaOT tarea = tareasRepo.findById(tareaId)
            .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada: " + tareaId));
        tarea.setCompletada(tarea.getCompletada() == 1 ? 0 : 1);
        tarea.setCompletadaAt(tarea.getCompletada() == 1 ? LocalDateTime.now() : null);
        tareasRepo.save(tarea);
        OrdenTrabajo ot = obtenerPorId(otId);
        recalcularAvance(ot);
        return repositorio.findById(otId).orElse(ot);
    }

    public void eliminarTarea(String otId, String tareaId) {
        tareasRepo.deleteById(tareaId);
        OrdenTrabajo ot = obtenerPorId(otId);
        recalcularAvance(ot);
    }

    public void eliminar(String id) {
        OrdenTrabajo ot = obtenerPorId(id);
        ot.setEliminado(1);
        repositorio.save(ot);
    }

    private void recalcularAvance(OrdenTrabajo ot) {
        List<TareaOT> tareas = tareasRepo.findByOrdenTrabajoIdOrderByOrdenAsc(ot.getId());
        if (tareas.isEmpty()) return;
        long completadas = tareas.stream().filter(t -> t.getCompletada() == 1).count();
        int avance = (int) Math.round((completadas * 100.0) / tareas.size());
        ot.setAvance(avance);
        if (avance == 100 && !"CERRADA".equals(ot.getEstado())) ot.setEstado("EN_EJECUCION");
        else if (avance > 0 && "PENDIENTE".equals(ot.getEstado()))  ot.setEstado("EN_EJECUCION");
        repositorio.save(ot);
    }

    private String generarNumero(String idEmpresa) {
        String ultimo = repositorio.ultimoNumero(idEmpresa);
        int siguiente = 1;
        if (ultimo != null && ultimo.contains("-")) {
            try { siguiente = Integer.parseInt(ultimo.split("-")[1]) + 1; } catch (Exception ignored) {}
        }
        return String.format("OT-%04d", siguiente);
    }
}
