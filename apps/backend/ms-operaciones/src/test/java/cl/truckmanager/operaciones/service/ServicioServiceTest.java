package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.AsignarServicioDto;
import cl.truckmanager.operaciones.dto.ServicioDto;
import cl.truckmanager.operaciones.entity.Servicio;
import cl.truckmanager.operaciones.repository.ServicioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioService — pruebas unitarias")
class ServicioServiceTest {

    @Mock
    private ServicioRepository repositorio;

    @InjectMocks
    private ServicioService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String SERVICIO_ID = "SRV-001";

    private Servicio servicioEjemplo;

    @BeforeEach
    void configurar() {
        servicioEjemplo = new Servicio();
        servicioEjemplo.setId(SERVICIO_ID);
        servicioEjemplo.setEmpresaId(EMPRESA_ID);
        servicioEjemplo.setOrigen("Santiago");
        servicioEjemplo.setDestino("Valparaíso");
        servicioEjemplo.setEstado("PENDIENTE");
        servicioEjemplo.setFechaServicio(LocalDate.now());
        servicioEjemplo.setEliminado(0);
        servicioEjemplo.setValorNeto(BigDecimal.ZERO);
        servicioEjemplo.setIva(BigDecimal.ZERO);
        servicioEjemplo.setValorTotal(BigDecimal.ZERO);
    }

    // ─── getAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll devuelve pagina filtrada por empresa")
    void getAll_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(),
                isNull(), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(servicioEjemplo)));

        Page<Servicio> resultado = servicio.getAll(
            EMPRESA_ID, null, null, null, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("getAll con estado en blanco lo convierte a null")
    void getAll_estadoEnBlanco_pasaNull() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(),
                isNull(), isNull(), isNull(), any()))
            .thenReturn(Page.empty());

        servicio.getAll(EMPRESA_ID, "  ", "  ", null, null, null, 0, 20);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(),
                isNull(), isNull(), isNull(), any());
    }

    // ─── getById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById retorna servicio cuando existe")
    void getById_existente_retornaServicio() {
        when(repositorio.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioEjemplo));

        Servicio resultado = servicio.getById(SERVICIO_ID);

        assertThat(resultado.getId()).isEqualTo(SERVICIO_ID);
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException cuando no existe")
    void getById_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("getById con empresa retorna servicio cuando pertenece a la empresa")
    void getById_conEmpresa_retornaServicio() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));

        Servicio resultado = servicio.getById(SERVICIO_ID, EMPRESA_ID);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("getById con empresa lanza 404 cuando pertenece a otra empresa")
    void getById_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById(SERVICIO_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear calcula IVA y valor total desde el valor neto")
    void crear_conValorNeto_calculaIva() {
        ServicioDto dto = new ServicioDto();
        dto.setOrigen("Santiago");
        dto.setDestino("Concepción");
        dto.setValorNeto(new BigDecimal("100000"));

        when(repositorio.ultimoNumero(EMPRESA_ID)).thenReturn("SRV-0001");
        when(repositorio.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getIva()).isEqualByComparingTo(new BigDecimal("19000.00"));
        assertThat(resultado.getValorTotal()).isEqualByComparingTo(new BigDecimal("119000.00"));
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("crear asigna estado PENDIENTE por defecto cuando el dto no lo incluye")
    void crear_sinEstado_asignaPendiente() {
        ServicioDto dto = new ServicioDto();
        dto.setOrigen("Temuco");
        dto.setDestino("Osorno");

        when(repositorio.ultimoNumero(EMPRESA_ID)).thenReturn(null);
        when(repositorio.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("crear asigna fecha de hoy cuando el dto no incluye fechaServicio")
    void crear_sinFechaServicio_asignaHoy() {
        ServicioDto dto = new ServicioDto();
        dto.setOrigen("Rancagua");
        dto.setDestino("La Serena");

        when(repositorio.ultimoNumero(EMPRESA_ID)).thenReturn("SRV-0005");
        when(repositorio.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getFechaServicio()).isEqualTo(LocalDate.now());
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar modifica destino y recalcula valores cuando se envía valorNeto")
    void actualizar_conNuevoValorNeto_recalcula() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));
        when(repositorio.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDto dto = new ServicioDto();
        dto.setDestino("Antofagasta");
        dto.setValorNeto(new BigDecimal("200000"));

        Servicio resultado = servicio.actualizar(SERVICIO_ID, EMPRESA_ID, dto);

        assertThat(resultado.getDestino()).isEqualTo("Antofagasta");
        assertThat(resultado.getIva()).isEqualByComparingTo(new BigDecimal("38000.00"));
    }

    @Test
    @DisplayName("actualizar lanza 404 cuando el servicio pertenece a otra empresa")
    void actualizar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(SERVICIO_ID, "EMP-999", new ServicioDto()))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ─── cambiarEstado ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado transiciona de PENDIENTE a APROBADO correctamente")
    void cambiarEstado_pendienteAAprobado_transicionaOk() {
        servicioEjemplo.setEstado("PENDIENTE");
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Servicio resultado = servicio.cambiarEstado(
            SERVICIO_ID, EMPRESA_ID, "APROBADO", List.of("BORRADOR", "PENDIENTE"));

        assertThat(resultado.getEstado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("cambiarEstado lanza IllegalStateException cuando el estado actual no esta permitido")
    void cambiarEstado_estadoNoPermitido_lanzaExcepcion() {
        servicioEjemplo.setEstado("COMPLETADO");
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));

        assertThatThrownBy(() -> servicio.cambiarEstado(
            SERVICIO_ID, EMPRESA_ID, "APROBADO", List.of("PENDIENTE")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("cambiarEstado lanza 404 cuando el servicio pertenece a otra empresa")
    void cambiarEstado_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(
            SERVICIO_ID, "EMP-999", "APROBADO", List.of("PENDIENTE")))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ─── asignar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("asignar actualiza vehiculo y conductor cuando el servicio esta APROBADO")
    void asignar_servicioAprobado_asignaCorrectamente() {
        servicioEjemplo.setEstado("APROBADO");
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AsignarServicioDto dto = new AsignarServicioDto();
        dto.setVehiculoId("VEH-001");
        dto.setConductorId("CON-001");

        Servicio resultado = servicio.asignar(SERVICIO_ID, EMPRESA_ID, dto);

        assertThat(resultado.getVehiculoId()).isEqualTo("VEH-001");
        assertThat(resultado.getConductorId()).isEqualTo("CON-001");
    }

    @Test
    @DisplayName("asignar lanza IllegalStateException cuando el servicio no esta APROBADO")
    void asignar_servicioNoAprobado_lanzaExcepcion() {
        servicioEjemplo.setEstado("PENDIENTE");
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));

        assertThatThrownBy(() -> servicio.asignar(SERVICIO_ID, EMPRESA_ID, new AsignarServicioDto()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APROBADO");
    }

    // ─── viajesConductor ─────────────────────────────────────────────────────

    @Test
    @DisplayName("viajesConductor retorna lista de servicios del conductor")
    void viajesConductor_conductorConViajes_retornaLista() {
        when(repositorio.findByConductorIdAndEliminadoOrderByFechaServicioDesc("CON-001", 0))
            .thenReturn(List.of(servicioEjemplo));

        List<Servicio> resultado = servicio.viajesConductor("CON-001");

        assertThat(resultado).hasSize(1);
        verify(repositorio).findByConductorIdAndEliminadoOrderByFechaServicioDesc("CON-001", 0);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar marca el servicio como eliminado logicamente")
    void eliminar_servicioPropio_marcaEliminado() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(servicioEjemplo));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.eliminar(SERVICIO_ID, EMPRESA_ID);

        assertThat(servicioEjemplo.getEliminado()).isEqualTo(1);
        verify(repositorio).save(servicioEjemplo);
    }

    @Test
    @DisplayName("eliminar lanza 404 cuando el servicio es de otra empresa")
    void eliminar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(SERVICIO_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(SERVICIO_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class);
        verify(repositorio, never()).save(any());
    }
}
