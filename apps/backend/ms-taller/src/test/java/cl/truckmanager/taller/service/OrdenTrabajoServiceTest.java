package cl.truckmanager.taller.service;

import cl.truckmanager.taller.dto.OrdenTrabajoDto;
import cl.truckmanager.taller.dto.TareaOTDto;
import cl.truckmanager.taller.entity.OrdenTrabajo;
import cl.truckmanager.taller.entity.TareaOT;
import cl.truckmanager.taller.repository.OrdenTrabajoRepository;
import cl.truckmanager.taller.repository.TareaOTRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdenTrabajoService — pruebas unitarias")
class OrdenTrabajoServiceTest {

    @Mock private OrdenTrabajoRepository repositorio;
    @Mock private TareaOTRepository      tareasRepo;

    @InjectMocks private OrdenTrabajoService servicio;

    private static final String EMP  = "EMP-001";
    private static final String OT_ID = "OT-001";

    private OrdenTrabajo ordenTrabajo;

    @BeforeEach
    void setUp() {
        ordenTrabajo = OrdenTrabajo.builder()
                .id(OT_ID)
                .empresaId(EMP)
                .numero("OT-2024-001")
                .vehiculoId("VEH-003")
                .tipo("MANTENCION")
                .estado("PENDIENTE")
                .descripcion("Cambio de aceite y filtros")
                .mecanicoResponsable("Juan Perez")
                .costoManoObra(new BigDecimal("25000"))
                .build();
    }

    @Test
    @DisplayName("obtenerTodos devuelve pagina filtrada")
    void obtenerTodos_devuelvePagina() {
        Page<OrdenTrabajo> pagina = new PageImpl<>(List.of(ordenTrabajo));
        when(repositorio.buscarPorFiltros(eq(EMP), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<OrdenTrabajo> resultado = servicio.obtenerTodos(EMP, null, null, null, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNumero()).isEqualTo("OT-2024-001");
    }

    @Test
    @DisplayName("obtenerTodos con filtros de estado y tipo")
    void obtenerTodos_conFiltros() {
        Page<OrdenTrabajo> pagina = new PageImpl<>(List.of(ordenTrabajo));
        when(repositorio.buscarPorFiltros(eq(EMP), eq("PENDIENTE"), eq("MANTENCION"), any(), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<OrdenTrabajo> resultado = servicio.obtenerTodos(EMP, "PENDIENTE", "MANTENCION", null, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("obtenerPorId retorna orden existente")
    void obtenerPorId_retornaOrden() {
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));

        OrdenTrabajo resultado = servicio.obtenerPorId(OT_ID);

        assertThat(resultado.getId()).isEqualTo(OT_ID);
        assertThat(resultado.getTipo()).isEqualTo("MANTENCION");
    }

    @Test
    @DisplayName("obtenerPorId lanza EntityNotFoundException si no existe")
    void obtenerPorId_lanzaExcepcionSiNoExiste() {
        when(repositorio.findById(OT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(OT_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(OT_ID);
    }

    @Test
    @DisplayName("crear genera numero de OT y persiste la orden")
    void crear_persisteOrden() {
        OrdenTrabajoDto dto = new OrdenTrabajoDto();
        dto.setVehiculoId("VEH-003");
        dto.setTipo("REPARACION");
        dto.setDescripcion("Reparacion de frenos");
        dto.setCostoManoObra(new BigDecimal("35000"));

        when(repositorio.ultimoNumero(EMP)).thenReturn(null);
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        OrdenTrabajo resultado = servicio.crear(EMP, dto);

        assertThat(resultado).isNotNull();
        verify(repositorio).save(any(OrdenTrabajo.class));
    }

    @Test
    @DisplayName("crear sin costo mano de obra usa BigDecimal.ZERO")
    void crear_sinCostoManoObra_usaCero() {
        OrdenTrabajoDto dto = new OrdenTrabajoDto();
        dto.setVehiculoId("VEH-001");
        dto.setTipo("INSPECCION");
        dto.setDescripcion("Inspeccion periodica");
        dto.setCostoManoObra(null);

        when(repositorio.ultimoNumero(EMP)).thenReturn("OT-0005");
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        OrdenTrabajo resultado = servicio.crear(EMP, dto);

        assertThat(resultado).isNotNull();
        verify(repositorio).save(argThat(ot -> ot.getCostoManoObra().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    @DisplayName("actualizar modifica campos de la orden")
    void actualizar_modificaCampos() {
        OrdenTrabajoDto dto = new OrdenTrabajoDto();
        dto.setDescripcion("Descripcion actualizada");
        dto.setMecanicoResponsable("Pedro Lopez");
        dto.setCostoManoObra(new BigDecimal("40000"));

        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        OrdenTrabajo resultado = servicio.actualizar(OT_ID, dto);

        assertThat(resultado).isNotNull();
        verify(repositorio).save(ordenTrabajo);
    }

    @Test
    @DisplayName("cerrar cambia estado a CERRADA y registra fecha")
    void cerrar_cambiaEstadoACerrada() {
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        OrdenTrabajo resultado = servicio.cerrar(OT_ID, new BigDecimal("50000"), "Trabajo completado");

        assertThat(resultado).isNotNull();
        assertThat(ordenTrabajo.getEstado()).isEqualTo("CERRADA");
        assertThat(ordenTrabajo.getAvance()).isEqualTo(100);
    }

    @Test
    @DisplayName("cerrar sin costo ni notas solo cambia estado")
    void cerrar_sinCostoNiNotas_soloEstado() {
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        servicio.cerrar(OT_ID, null, null);

        assertThat(ordenTrabajo.getEstado()).isEqualTo("CERRADA");
    }

    @Test
    @DisplayName("agregarTarea agrega tarea a orden existente")
    void agregarTarea_agregaTarea() {
        TareaOTDto dto = new TareaOTDto();
        dto.setDescripcion("Cambiar filtro de aceite");

        TareaOT tareaGuardada = TareaOT.builder()
                .id("TAR-001")
                .ordenTrabajo(ordenTrabajo)
                .descripcion("Cambiar filtro de aceite")
                .completada(0)
                .build();

        ordenTrabajo.setTareas(new java.util.ArrayList<>());
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(tareasRepo.save(any(TareaOT.class))).thenReturn(tareaGuardada);
        when(tareasRepo.findByOrdenTrabajoIdOrderByOrdenAsc(OT_ID)).thenReturn(List.of(tareaGuardada));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        TareaOT resultado = servicio.agregarTarea(OT_ID, dto);

        assertThat(resultado.getDescripcion()).isEqualTo("Cambiar filtro de aceite");
        verify(tareasRepo).save(any(TareaOT.class));
    }

    @Test
    @DisplayName("completarTarea alterna entre completada y pendiente")
    void completarTarea_alternaBitToggle() {
        TareaOT tarea = TareaOT.builder()
                .id("TAR-001")
                .ordenTrabajo(ordenTrabajo)
                .descripcion("Tarea pendiente")
                .completada(0)
                .build();

        ordenTrabajo.setTareas(List.of(tarea));
        when(tareasRepo.findById("TAR-001")).thenReturn(Optional.of(tarea));
        when(tareasRepo.save(any(TareaOT.class))).thenReturn(tarea);
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(tareasRepo.findByOrdenTrabajoIdOrderByOrdenAsc(OT_ID)).thenReturn(List.of(tarea));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        servicio.completarTarea(OT_ID, "TAR-001");

        assertThat(tarea.getCompletada()).isEqualTo(1);
    }

    @Test
    @DisplayName("eliminar marca la OT como eliminada")
    void eliminar_marcaComoEliminada() {
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(repositorio.save(any(OrdenTrabajo.class))).thenReturn(ordenTrabajo);

        servicio.eliminar(OT_ID);

        assertThat(ordenTrabajo.getEliminado()).isEqualTo(1);
        verify(repositorio).save(ordenTrabajo);
    }

    @Test
    @DisplayName("eliminarTarea elimina tarea y recalcula avance")
    void eliminarTarea_eliminaYRecalcula() {
        ordenTrabajo.setTareas(new java.util.ArrayList<>());
        when(repositorio.findById(OT_ID)).thenReturn(Optional.of(ordenTrabajo));
        when(tareasRepo.findByOrdenTrabajoIdOrderByOrdenAsc(OT_ID)).thenReturn(List.of());

        servicio.eliminarTarea(OT_ID, "TAR-001");

        verify(tareasRepo).deleteById("TAR-001");
    }
}
