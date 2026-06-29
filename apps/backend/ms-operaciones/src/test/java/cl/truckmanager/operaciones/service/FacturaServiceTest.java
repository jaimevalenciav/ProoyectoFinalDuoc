package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.FacturarDto;
import cl.truckmanager.operaciones.entity.Cliente;
import cl.truckmanager.operaciones.entity.Factura;
import cl.truckmanager.operaciones.entity.Servicio;
import cl.truckmanager.operaciones.repository.ClienteRepository;
import cl.truckmanager.operaciones.repository.FacturaRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaService — pruebas unitarias")
class FacturaServiceTest {

    @Mock private FacturaRepository  facturaRepo;
    @Mock private ServicioRepository servicioRepo;
    @Mock private ClienteRepository  clienteRepo;

    @InjectMocks
    private FacturaService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String CLIENTE_ID  = "CLI-001";
    private static final String FACTURA_ID  = "FAC-001";
    private static final String SERVICIO_ID = "SRV-001";

    private Factura facturaEjemplo;
    private Servicio servicioEjemplo;
    private Cliente clienteEjemplo;

    @BeforeEach
    void configurar() {
        clienteEjemplo = Cliente.builder()
            .id(CLIENTE_ID).empresaId(EMPRESA_ID)
            .rut("76543210-K").razonSocial("Logística Chile SpA").build();

        servicioEjemplo = new Servicio();
        servicioEjemplo.setId(SERVICIO_ID);
        servicioEjemplo.setEmpresaId(EMPRESA_ID);
        servicioEjemplo.setClienteId(CLIENTE_ID);
        servicioEjemplo.setEstado("COMPLETADO");
        servicioEjemplo.setFacturado(0);
        servicioEjemplo.setValorNeto(new BigDecimal("100000"));
        servicioEjemplo.setIva(new BigDecimal("19000"));
        servicioEjemplo.setValorTotal(new BigDecimal("119000"));

        facturaEjemplo = Factura.builder()
            .id(FACTURA_ID).empresaId(EMPRESA_ID).clienteId(CLIENTE_ID)
            .numFactura("FAC-0001").estado("EMITIDA")
            .subtotal(new BigDecimal("100000"))
            .iva(new BigDecimal("19000"))
            .total(new BigDecimal("119000"))
            .build();
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll devuelve pagina de facturas de la empresa")
    void getAll_devuelveResultados() {
        when(facturaRepo.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(facturaEjemplo)));
        when(clienteRepo.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));

        Page<Factura> resultado = servicio.getAll(EMPRESA_ID, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getClienteRazonSocial())
            .isEqualTo("Logística Chile SpA");
    }

    // ─── getById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById retorna factura con razon social del cliente")
    void getById_existente_retornaFactura() {
        when(facturaRepo.findById(FACTURA_ID)).thenReturn(Optional.of(facturaEjemplo));
        when(clienteRepo.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));

        Factura resultado = servicio.getById(FACTURA_ID);

        assertThat(resultado.getId()).isEqualTo(FACTURA_ID);
        assertThat(resultado.getClienteRazonSocial()).isEqualTo("Logística Chile SpA");
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException cuando no existe")
    void getById_inexistente_lanzaExcepcion() {
        when(facturaRepo.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── facturar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("facturar genera factura sumando totales de los servicios")
    void facturar_serviciosValidos_generaFactura() {
        when(servicioRepo.findByIdInAndEmpresaId(anyList(), eq(EMPRESA_ID)))
            .thenReturn(List.of(servicioEjemplo));
        when(facturaRepo.ultimoNumero(EMPRESA_ID)).thenReturn(null);
        when(clienteRepo.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));
        when(facturaRepo.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            f.setId(FACTURA_ID);
            return f;
        });

        FacturarDto dto = new FacturarDto();
        dto.setClienteId(CLIENTE_ID);
        dto.setServicioIds(List.of(SERVICIO_ID));

        Factura resultado = servicio.facturar(EMPRESA_ID, dto);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(resultado.getIva()).isEqualByComparingTo(new BigDecimal("19000"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("119000"));
        assertThat(resultado.getEstado()).isEqualTo("EMITIDA");
        assertThat(resultado.getNumFactura()).isEqualTo("FAC-0001");
    }

    @Test
    @DisplayName("facturar lanza IllegalArgumentException cuando no hay servicios validos")
    void facturar_sinServicios_lanzaExcepcion() {
        when(servicioRepo.findByIdInAndEmpresaId(anyList(), eq(EMPRESA_ID)))
            .thenReturn(List.of());

        FacturarDto dto = new FacturarDto();
        dto.setClienteId(CLIENTE_ID);
        dto.setServicioIds(List.of("SRV-X"));

        assertThatThrownBy(() -> servicio.facturar(EMPRESA_ID, dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("servicios válidos");
    }

    @Test
    @DisplayName("facturar lanza IllegalArgumentException cuando servicios son de distintos clientes")
    void facturar_serviciosDistintosClientes_lanzaExcepcion() {
        Servicio otroServicio = new Servicio();
        otroServicio.setId("SRV-002");
        otroServicio.setClienteId("CLI-002");
        otroServicio.setFacturado(0);
        otroServicio.setValorNeto(BigDecimal.ZERO);
        otroServicio.setIva(BigDecimal.ZERO);
        otroServicio.setValorTotal(BigDecimal.ZERO);

        when(servicioRepo.findByIdInAndEmpresaId(anyList(), eq(EMPRESA_ID)))
            .thenReturn(List.of(servicioEjemplo, otroServicio));

        FacturarDto dto = new FacturarDto();
        dto.setClienteId(CLIENTE_ID);
        dto.setServicioIds(List.of(SERVICIO_ID, "SRV-002"));

        assertThatThrownBy(() -> servicio.facturar(EMPRESA_ID, dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mismo cliente");
    }

    @Test
    @DisplayName("facturar lanza IllegalArgumentException cuando un servicio ya esta facturado")
    void facturar_servicioYaFacturado_lanzaExcepcion() {
        servicioEjemplo.setFacturado(1);
        when(servicioRepo.findByIdInAndEmpresaId(anyList(), eq(EMPRESA_ID)))
            .thenReturn(List.of(servicioEjemplo));

        FacturarDto dto = new FacturarDto();
        dto.setClienteId(CLIENTE_ID);
        dto.setServicioIds(List.of(SERVICIO_ID));

        assertThatThrownBy(() -> servicio.facturar(EMPRESA_ID, dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("facturados");
    }

    // ─── anular ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("anular cambia estado de la factura a ANULADA y desvincula servicios")
    void anular_facturaEmitida_cambiaEstado() {
        when(facturaRepo.findById(FACTURA_ID)).thenReturn(Optional.of(facturaEjemplo));
        when(facturaRepo.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(servicioRepo.findByFacturaId(FACTURA_ID)).thenReturn(List.of(servicioEjemplo));
        when(clienteRepo.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));

        Factura resultado = servicio.anular(FACTURA_ID);

        assertThat(resultado.getEstado()).isEqualTo("ANULADA");
        assertThat(servicioEjemplo.getFacturado()).isEqualTo(0);
        assertThat(servicioEjemplo.getFacturaId()).isNull();
    }

    @Test
    @DisplayName("anular lanza IllegalArgumentException cuando la factura ya esta anulada")
    void anular_facturaYaAnulada_lanzaExcepcion() {
        facturaEjemplo.setEstado("ANULADA");
        when(facturaRepo.findById(FACTURA_ID)).thenReturn(Optional.of(facturaEjemplo));

        assertThatThrownBy(() -> servicio.anular(FACTURA_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya está anulada");
    }
}
