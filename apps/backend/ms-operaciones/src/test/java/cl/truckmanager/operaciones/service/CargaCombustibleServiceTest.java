package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.CargaCombustibleDto;
import cl.truckmanager.operaciones.entity.AlertaCombustible;
import cl.truckmanager.operaciones.entity.CargaCombustible;
import cl.truckmanager.operaciones.repository.AlertaCombustibleRepository;
import cl.truckmanager.operaciones.repository.CargaCombustibleRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CargaCombustibleService — pruebas unitarias")
class CargaCombustibleServiceTest {

    @Mock
    private CargaCombustibleRepository repositorio;

    @Mock
    private AlertaCombustibleRepository alertaRepo;

    @InjectMocks
    private CargaCombustibleService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String VEHICULO_ID = "VEH-001";
    private static final String CARGA_ID    = "CAR-001";

    private CargaCombustible cargaEjemplo;

    @BeforeEach
    void configurar() {
        cargaEjemplo = CargaCombustible.builder()
            .id(CARGA_ID)
            .empresaId(EMPRESA_ID)
            .vehiculoId(VEHICULO_ID)
            .litros(new BigDecimal("50"))
            .precioLitro(new BigDecimal("1200"))
            .costoTotal(new BigDecimal("60000.00"))
            .kmVehiculo(150000L)
            .tipoCombustible("Diesel")
            .proveedor("Petrobras")
            .fechaCarga(LocalDate.now())
            .build();
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll devuelve pagina filtrada por empresa")
    void getAll_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(cargaEjemplo)));

        Page<CargaCombustible> resultado = servicio.getAll(EMPRESA_ID, null, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    // ─── getById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById retorna carga cuando existe")
    void getById_existente_retornaCarga() {
        when(repositorio.findById(CARGA_ID)).thenReturn(Optional.of(cargaEjemplo));

        CargaCombustible resultado = servicio.getById(CARGA_ID);

        assertThat(resultado.getId()).isEqualTo(CARGA_ID);
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException cuando no existe")
    void getById_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── registrar ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar calcula costoTotal como litros x precioLitro")
    void registrar_conLitrosYPrecio_calculaCostoTotal() {
        // kmVehiculo=null -> calcularConsumo devuelve null sin consultar el repositorio
        when(repositorio.save(any(CargaCombustible.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaCombustibleDto dto = new CargaCombustibleDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("50"));
        dto.setPrecioLitro(new BigDecimal("1200"));
        // sin kmVehiculo → calcularConsumo retorna null inmediatamente

        CargaCombustible resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getCostoTotal()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("registrar no calcula consumo para AdBlue")
    void registrar_tipoAdBlue_noCalculaConsumo() {
        when(repositorio.save(any(CargaCombustible.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaCombustibleDto dto = new CargaCombustibleDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setTipoCombustible("AdBlue");
        dto.setLitros(new BigDecimal("10"));
        dto.setPrecioLitro(new BigDecimal("800"));

        CargaCombustible resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getConsumo100km()).isNull();
        verify(repositorio, never()).findTop2ByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(any());
    }

    @Test
    @DisplayName("registrar genera alerta cuando consumo es anomalamente alto")
    void registrar_consumoAlto_generaAlerta() {
        // Carga previa: km 100000, ahora 100100 (100km recorridos, 50L = 50L/100km - anomalo)
        CargaCombustible cargaPrevia = CargaCombustible.builder()
            .vehiculoId(VEHICULO_ID).kmVehiculo(100000L).build();

        when(repositorio.findTop2ByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(VEHICULO_ID))
            .thenReturn(List.of(cargaPrevia));
        when(repositorio.save(any(CargaCombustible.class))).thenAnswer(inv -> {
            CargaCombustible c = inv.getArgument(0);
            c.setId(CARGA_ID);
            return c;
        });

        CargaCombustibleDto dto = new CargaCombustibleDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("50")); // 50L / 100km = 50 L/100km (anomalo alto > 35)
        dto.setPrecioLitro(new BigDecimal("1200"));
        dto.setKmVehiculo(100100L);

        servicio.registrar(EMPRESA_ID, dto);

        verify(alertaRepo).save(any(AlertaCombustible.class));
    }

    @Test
    @DisplayName("registrar asigna proveedor por defecto y fecha de hoy cuando no se especifican")
    void registrar_sinProveedorNiFecha_asignaDefaults() {
        // kmVehiculo=null → calcularConsumo no consulta el repositorio
        when(repositorio.save(any(CargaCombustible.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaCombustibleDto dto = new CargaCombustibleDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("30"));
        dto.setPrecioLitro(new BigDecimal("1100"));
        // sin proveedor, sin fechaCarga, sin kmVehiculo

        CargaCombustible resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getProveedor()).isEqualTo("Sin especificar");
        assertThat(resultado.getFechaCarga()).isEqualTo(LocalDate.now());
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar invoca deleteById en el repositorio")
    void eliminar_cargaExistente_eliminaCorrectamente() {
        servicio.eliminar(CARGA_ID);
        verify(repositorio).deleteById(CARGA_ID);
    }

    // ─── getUltimaCarga ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getUltimaCarga retorna ultima carga del vehiculo")
    void getUltimaCarga_vehiculoConCargas_retornaUltima() {
        when(repositorio.findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(VEHICULO_ID))
            .thenReturn(Optional.of(cargaEjemplo));

        Optional<CargaCombustible> resultado = servicio.getUltimaCarga(VEHICULO_ID);

        assertThat(resultado).isPresent();
    }

    // ─── getAnomalias ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAnomalias retorna lista de cargas anomalas")
    void getAnomalias_retornaAnomalias() {
        when(repositorio.findAnomalias(EMPRESA_ID)).thenReturn(List.of(cargaEjemplo));

        List<CargaCombustible> resultado = servicio.getAnomalias(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
    }
}
