package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.CargaAdBlueDto;
import cl.truckmanager.operaciones.entity.CargaAdBlue;
import cl.truckmanager.operaciones.repository.CargaAdBlueRepository;
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
@DisplayName("CargaAdBlueService — pruebas unitarias")
class CargaAdBlueServiceTest {

    @Mock
    private CargaAdBlueRepository repositorio;

    @InjectMocks
    private CargaAdBlueService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String VEHICULO_ID = "VEH-001";
    private static final String CARGA_ID    = "CAR-001";

    private CargaAdBlue cargaEjemplo;

    @BeforeEach
    void configurar() {
        cargaEjemplo = CargaAdBlue.builder()
            .id(CARGA_ID)
            .empresaId(EMPRESA_ID)
            .vehiculoId(VEHICULO_ID)
            .litros(new BigDecimal("20.5"))
            .precioLitro(new BigDecimal("650"))
            .costoTotal(new BigDecimal("13325.00"))
            .fechaCarga(LocalDate.now())
            .proveedor("Copec")
            .build();
    }

    @Test
    @DisplayName("getAll devuelve pagina filtrada por empresa")
    void getAll_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(cargaEjemplo)));

        Page<CargaAdBlue> resultado = servicio.getAll(EMPRESA_ID, null, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById retorna carga cuando existe")
    void getById_existente_retornaCarga() {
        when(repositorio.findById(CARGA_ID)).thenReturn(Optional.of(cargaEjemplo));

        CargaAdBlue resultado = servicio.getById(CARGA_ID);

        assertThat(resultado.getId()).isEqualTo(CARGA_ID);
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException cuando no existe")
    void getById_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("registrar calcula costoTotal como litros x precioLitro")
    void registrar_conLitrosYPrecio_calculaCostoTotal() {
        when(repositorio.save(any(CargaAdBlue.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaAdBlueDto dto = new CargaAdBlueDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("20"));
        dto.setPrecioLitro(new BigDecimal("650"));
        dto.setProveedor("Copec");

        CargaAdBlue resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getCostoTotal()).isEqualByComparingTo(new BigDecimal("13000.00"));
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("registrar asigna proveedor por defecto cuando no se especifica")
    void registrar_sinProveedor_asignaProveedorDefault() {
        when(repositorio.save(any(CargaAdBlue.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaAdBlueDto dto = new CargaAdBlueDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("10"));
        dto.setPrecioLitro(new BigDecimal("500"));

        CargaAdBlue resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getProveedor()).isEqualTo("Sin especificar");
    }

    @Test
    @DisplayName("registrar asigna fecha de hoy cuando no se especifica fechaCarga")
    void registrar_sinFecha_asignaHoy() {
        when(repositorio.save(any(CargaAdBlue.class))).thenAnswer(inv -> inv.getArgument(0));

        CargaAdBlueDto dto = new CargaAdBlueDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setLitros(new BigDecimal("5"));
        dto.setPrecioLitro(new BigDecimal("600"));

        CargaAdBlue resultado = servicio.registrar(EMPRESA_ID, dto);

        assertThat(resultado.getFechaCarga()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("eliminar invoca deleteById en el repositorio")
    void eliminar_cargaExistente_eliminaCorrectamente() {
        servicio.eliminar(CARGA_ID);

        verify(repositorio).deleteById(CARGA_ID);
    }

    @Test
    @DisplayName("getUltimaCarga retorna ultima carga del vehiculo")
    void getUltimaCarga_vehiculoConCargas_retornaUltima() {
        when(repositorio.findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(VEHICULO_ID))
            .thenReturn(Optional.of(cargaEjemplo));

        Optional<CargaAdBlue> resultado = servicio.getUltimaCarga(VEHICULO_ID);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getVehiculoId()).isEqualTo(VEHICULO_ID);
    }

    @Test
    @DisplayName("getAnomalias retorna lista de cargas anomalas de la empresa")
    void getAnomalias_retornaAnomalias() {
        when(repositorio.findAnomalias(EMPRESA_ID)).thenReturn(List.of(cargaEjemplo));

        List<CargaAdBlue> resultado = servicio.getAnomalias(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
    }
}
