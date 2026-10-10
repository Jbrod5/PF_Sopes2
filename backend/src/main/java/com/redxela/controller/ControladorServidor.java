package com.redxela.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsMessageContext;

import com.redxela.service.ColaPrioridadPedidos;
import com.redxela.service.PoliticaPlanificacion;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.almacen.AlmacenPaginado;
import com.redxela.model.Pedido;
import com.redxela.model.EstadoPedido;
import com.redxela.model.Cliente;
import com.redxela.model.NivelServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

// servidor javalin que expone rutas para el modulo 3 y el sistema completo
public class ControladorServidor {

    // instancia del servidor javalin
    private final Javalin app;

    // referencia a los servicios compartidos
    private final ColaPrioridadPedidos cola;
    private final GestorRecursos gestor;
    private final AlmacenPaginado almacen;
    private final SimuladorDeadlock simulador;

    /**
     * Constructor sobrecargado que inicializa el servidor con los servicios existentes.
     *
     * @param cola cola de pedidos activa.
     * @param gestor gestor de recursos compartidos.
     * @param almacen almacen paginado del centro.
     */
    public ControladorServidor(ColaPrioridadPedidos cola, GestorRecursos gestor, AlmacenPaginado almacen) {
        // inicializar referencias compartidas
        this.cola = cola;
        this.gestor = gestor;
        this.almacen = almacen;
        this.simulador = new SimuladorDeadlock(gestor);
        // crear la instancia de javalin en el puerto 7070
        this.app = Javalin.create(config -> {
            // configurar archivos estaticos del frontend
            config.staticFiles.add("/home/jorge/Sopes2/PF_workspace/PF_Sopes2/frontend/dist", Location.EXTERNAL);
        }).start(7070);
        // configurar rutas disponibles del servidor
        this.configurarRutas();
    }

    // configurar las rutas disponibles en el servidor javalin
    private void configurarRutas() {

        // linea en blanco tras apertura de bloque
        this.app.get("/api/estado", this::manejadorEstado);
        this.app.get("/api/almacen", this::manejadorAlmacen);
        this.app.post("/api/pedidos", this::manejadorRegistrarPedido);
        this.app.post("/api/politica", this::manejadorCambiarPolitica);
        this.app.post("/api/deadlock/inducir", this::manejadorInducirDeadlock);
        this.app.post("/api/deadlock/resolver", this::manejadorResolverDeadlock);
        this.app.ws("/ws/simulacion", ws -> {
            ws.onMessage((WsMessageContext ctx) -> {
                // reenviar mensaje de confirmacion al cliente
                String mensaje = ctx.message();
                try {
                    ctx.send("Confirmado: " + mensaje);
                } catch (Exception e) {
                    System.err.println("Error enviando mensaje WebSocket: " + e.getMessage());
                }
            });
        });

        // linea en blanco antes de cierre de bloque
    }

    // manejar solicitud de estado general del sistema
    private void manejadorEstado(Context ctx) {
        // construir respuesta JSON con el estado de recursos
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"montacargas\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.MONTACARGAS));
        json.append(",");
        json.append("\"estacionesEmpaque\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ESTACION_EMPAQUE));
        json.append(",");
        json.append("\"areasCarga\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.AREA_CARGA));
        json.append(",");
        json.append("\"encargadosBodega\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ENCARGADO_BODEGA));
        json.append(",");
        json.append("\"estacionesCalidad\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ESTACION_CONTROL_CALIDAD));
        json.append(",");
        json.append("\"sistemasEscaneo\":");
        json.append(gestor.consultarDisponibles(com.redxela.model.TipoRecurso.SISTEMA_ESCANEO));
        json.append("}");
        ctx.contentType("application/json").result(json.toString());
    }

    // manejar solicitud del estado del almacen paginado
    private void manejadorAlmacen(Context ctx) {
        // construir JSON con las celdas del almacen
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"capacidadTotal\":");
        json.append(almacen.getCapacidadTotal());
        json.append(",");
        json.append("\"espaciosOcupados\":");
        json.append(almacen.getEspaciosOcupados());
        json.append(",");
        json.append("\"espaciosDisponibles\":");
        json.append(almacen.getEspaciosDisponibles());
        json.append(",");
        json.append("\"celdas\":[");
        // obtener todas las celdas del almacen
        for (int p = 0; p < 4; p++) {
            for (int n = 0; n < 3; n++) {
                for (int e = 0; e < 6; e++) {
                    json.append("{\"pasillo\":");
                    json.append(p + 1);
                    json.append(",\"nivel\":");
                    json.append(n + 1);
                    json.append(",\"espacio\":");
                    json.append(e + 1);
                    json.append(",\"ocupada\":");
                    json.append(almacen.getEspaciosOcupados() > 0);
                    json.append("}");
                    if (!(p == 3 && n == 2 && e == 5)) {
                        json.append(",");
                    }
                }
            }
        }
        json.append("]}");
        ctx.contentType("application/json").result(json.toString());
    }

    // registrar pedido manual recibido por post
    private void manejadorRegistrarPedido(Context ctx) {
        try {
            // parsear cuerpo JSON de la solicitud
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());

            String id = cuerpo.has("id") ? cuerpo.get("id").asText() : "MAN-001";
            String nombreCliente = cuerpo.has("cliente") ? cuerpo.get("cliente").asText() : "Cliente Generico";
            String nivelStr = cuerpo.has("nivel") ? cuerpo.get("nivel").asText() : "ESTANDAR";

            NivelServicio nivel = NivelServicio.ESTANDAR;
            if (nivelStr.equals("EXPRES")) {
                nivel = NivelServicio.EXPRES;
            } else if (nivelStr.equals("PRIORITARIO")) {
                nivel = NivelServicio.PRIORITARIO;
            } else if (nivelStr.equals("ECONOMICO")) {
                nivel = NivelServicio.ECONOMICO;
            } else if (nivelStr.equals("PROGRAMADO")) {
                nivel = NivelServicio.PROGRAMADO;
            }

            Cliente cliente = new Cliente(id + "-CLI", nombreCliente, "contacto@generico.com");
            Pedido nuevo = new Pedido(id, cliente, nivel);
            cola.encolarPedido(nuevo);

            ctx.contentType("application/json").result("{\"mensaje\":\"Pedido registrado exitosamente\",\"id\":\"" + id + "\"}");
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    // cambiar la politica de planificacion activa
    private void manejadorCambiarPolitica(Context ctx) {
        try {
            // parsear cuerpo JSON para obtener la nueva politica
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String politicaStr = cuerpo.has("politica") ? cuerpo.get("politica").asText() : "NIVEL_SERVICIO_ESTRICTO";

            if (politicaStr.equals("MENOR_CANTIDAD_ARTICULOS")) {
                cola.cambiarPolitica(PoliticaPlanificacion.MENOR_CANTIDAD_ARTICULOS);
            } else if (politicaStr.equals("TIEMPO_ESPERA_FIFO")) {
                cola.cambiarPolitica(PoliticaPlanificacion.TIEMPO_ESPERA_FIFO);
            } else {
                cola.cambiarPolitica(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);
            }

            ctx.contentType("application/json").result("{\"mensaje\":\"Politica actualizada\",\"politica\":\"" + politicaStr + "\"}");
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    // inducir el escenario de interbloqueo deliberado
    private void manejadorInducirDeadlock(Context ctx) {
        // usar referencias existentes para provocar deadlock
        Pedido p1 = new Pedido("DL-A", new Cliente(), com.redxela.model.NivelServicio.PRIORITARIO);
        Pedido p2 = new Pedido("DL-B", new Cliente(), com.redxela.model.NivelServicio.PRIORITARIO);
        InformacionConflicto conflicto = simulador.forzarDeadlockDeliberado(p1, com.redxela.model.TipoRecurso.MONTACARGAS, com.redxela.model.TipoRecurso.ESTACION_EMPAQUE, p2);
        if (conflicto != null) {
            ctx.result("Interbloqueo inducido: " + conflicto.getCausaBloqueo());
        } else {
            ctx.result("No se detecto interbloqueo");
        }
    }

    // resolver manualmente el conflicto mediante despropiacion
    private void manejadorResolverDeadlock(Context ctx) {
        try {
            // parsear cuerpo JSON para obtener el ID del pedido ganador
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String idGanador = cuerpo.has("id") ? cuerpo.get("id").asText() : "DL-A";

            boolean resuelto = simulador.resolverConflictoManual(idGanador);
            if (resuelto) {
                ctx.contentType("application/json").result("{\"mensaje\":\"Conflicto resuelto manualmente\",\"id\":\"" + idGanador + "\"}");
            } else {
                ctx.contentType("application/json").result("{\"mensaje\":\"No hay conflicto activo para resolver\"}");
            }
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    /**
     * Obtener el servidor javalin creado.
     *
     * @return instancia del servidor.
     */
    public Javalin getApp() {
        return this.app;
    }
}
