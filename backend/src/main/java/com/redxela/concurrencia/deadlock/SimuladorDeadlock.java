package com.redxela.concurrencia.deadlock;

import com.redxela.concurrencia.GestorRecursos;
import com.redxela.model.EstadoPedido;
import com.redxela.model.Pedido;
import com.redxela.model.TipoRecurso;

// coordinador que permite provocar deliberadamente un interbloqueo y resolverlo mediante desapropiacion
public class SimuladorDeadlock {

    // referencia al gestor central de recursos
    private final GestorRecursos gestorRecursos;

    // registro de asignacion de recursos y grafo de dependencias
    private final RegistroAsignacionRecursos registro;

    // detector de interbloqueos
    private final DetectorDeadlock detector;

    // ultimo conflicto detectado y pendiente o resuelto
    private InformacionConflicto ultimoConflicto;

    // referencias a las tareas involucradas en el conflicto
    private ProcesoPedidoInterbloqueable tarea1;
    private ProcesoPedidoInterbloqueable tarea2;

    // referencias a los hilos en conflicto
    private Thread hilo1;
    private Thread hilo2;

    // conteo de permisos retenidos para saturar recursos con capacidad mayor a uno
    private int excedenteRetenidoA;
    private int excedenteRetenidoB;
    private TipoRecurso recursoSaturadoA;
    private TipoRecurso recursoSaturadoB;
    private Pedido pedidoPerdedor;

    /**
     * Constructor sobrecargado con gestor de recursos.
     *
     * @param gestorRecursos gestor central de semaforos de recursos.
     */
    public SimuladorDeadlock(GestorRecursos gestorRecursos) {
        this.gestorRecursos = gestorRecursos;
        this.registro = new RegistroAsignacionRecursos();
        this.detector = new DetectorDeadlock(this.registro);
        this.ultimoConflicto = null;
        this.excedenteRetenidoA = 0;
        this.excedenteRetenidoB = 0;
    }

    /**
     * Forzar deliberadamente una situacion de interbloqueo con dos pedidos y dos recursos en orden inverso.
     *
     * @param pedido1 primer pedido a competir.
     * @param recursoA primer recurso solicitado por pedido 1 y segundo por pedido 2.
     * @param recursoB segundo recurso solicitado por pedido 1 y primero por pedido 2.
     * @param pedido2 segundo pedido a competir.
     * @return informacion del conflicto generado y detectado o null si no se provoco.
     */
    public InformacionConflicto forzarDeadlockDeliberado(
            Pedido pedido1,
            TipoRecurso recursoA,
            TipoRecurso recursoB,
            Pedido pedido2
    ) {
        if (pedido1 == null || pedido2 == null || recursoA == null || recursoB == null) {
            return null;
        }

        System.out.println("[SIMULADOR-DEADLOCK] Iniciando escenario de interbloqueo deliberado");
        System.out.println("  Pedido 1 (" + pedido1.getId() + "): solicita " + recursoA.getNombre() + " y luego " + recursoB.getNombre());
        System.out.println("  Pedido 2 (" + pedido2.getId() + "): solicita " + recursoB.getNombre() + " y luego " + recursoA.getNombre());

        this.recursoSaturadoA = recursoA;
        this.recursoSaturadoB = recursoB;

        // retener los permisos excedentes para que cada recurso disponga unicamente de una unidad libre
        int disponiblesA = this.gestorRecursos.consultarDisponibles(recursoA);
        if (disponiblesA > 1) {
            this.excedenteRetenidoA = disponiblesA - 1;
            this.gestorRecursos.adquirirRecurso(recursoA, this.excedenteRetenidoA);
        } else {
            this.excedenteRetenidoA = 0;
        }

        int disponiblesB = this.gestorRecursos.consultarDisponibles(recursoB);
        if (disponiblesB > 1) {
            this.excedenteRetenidoB = disponiblesB - 1;
            this.gestorRecursos.adquirirRecurso(recursoB, this.excedenteRetenidoB);
        } else {
            this.excedenteRetenidoB = 0;
        }

        // instanciar las dos tareas de pedido interbloqueables
        this.tarea1 = new ProcesoPedidoInterbloqueable(
                pedido1,
                this.gestorRecursos,
                this.registro,
                recursoA,
                recursoB,
                250L,
                600L
        );

        this.tarea2 = new ProcesoPedidoInterbloqueable(
                pedido2,
                this.gestorRecursos,
                this.registro,
                recursoB,
                recursoA,
                250L,
                600L
        );

        // crear y arrancar ambos hilos concurrentemente
        this.hilo1 = new Thread(this.tarea1, "Hilo-DL-" + pedido1.getId());
        this.hilo2 = new Thread(this.tarea2, "Hilo-DL-" + pedido2.getId());

        this.hilo1.start();
        this.hilo2.start();

        // esperar brevemente a que ambos hilos adquieran su primer recurso y soliciten el segundo
        try {
            Thread.sleep(600L);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // ejecutar la deteccion de ciclos en el grafo de espera
        InformacionConflicto conflicto = this.detector.detectarInterbloqueo();
        if (conflicto != null) {
            pedido1.setEstado(EstadoPedido.EN_CONFLICTO);
            pedido2.setEstado(EstadoPedido.EN_CONFLICTO);
            this.ultimoConflicto = conflicto;
            System.out.println("[ALERTA-DEADLOCK] Interbloqueo confirmado por el detector:");
            System.out.println("  " + conflicto.getCausaBloqueo());
        } else {
            System.out.println("[SIMULADOR-DEADLOCK] No se detecto espera circular en este intento");
        }

        return conflicto;
    }

    /**
     * Resolver manualmente el conflicto seleccionando el pedido que continuara con los recursos.
     *
     * @param idPedidoGanador identificador del pedido seleccionado para continuar.
     * @return verdadero si el conflicto fue resuelto exitosamente o falso en caso contrario.
     */
    public boolean resolverConflictoManual(String idPedidoGanador) {
        if (this.ultimoConflicto == null || idPedidoGanador == null) {
            return false;
        }

        System.out.println("[RESOLUCION-MANUAL] Seleccionando como ganador al pedido: " + idPedidoGanador);

        ProcesoPedidoInterbloqueable tareaGanadora;
        ProcesoPedidoInterbloqueable tareaPerdedora;
        Thread hiloGanador;

        // identificar la tarea que debe ganar y la que debe ser desapropiada
        if (this.tarea1 != null && idPedidoGanador.equals(this.tarea1.getPedido().getId())) {
            tareaGanadora = this.tarea1;
            tareaPerdedora = this.tarea2;
            hiloGanador = this.hilo1;
        } else if (this.tarea2 != null && idPedidoGanador.equals(this.tarea2.getPedido().getId())) {
            tareaGanadora = this.tarea2;
            tareaPerdedora = this.tarea1;
            hiloGanador = this.hilo2;
        } else {
            System.err.println("[RESOLUCION-MANUAL] El pedido indicado no forma parte del conflicto activo");
            return false;
        }

        // asignar pedido perdedor para acceso posterior
        this.pedidoPerdedor = tareaPerdedora.getPedido();

        // desapropiar los recursos retenidos por la tarea perdedora para romper el ciclo
        tareaPerdedora.desapropiar();

        // esperar a que el hilo ganador complete su procesamiento con el recurso liberado
        try {
            if (hiloGanador != null) {
                hiloGanador.join(3000L);
            }
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // liberar los permisos excedentes retenidos al inicio para restaurar la capacidad normal
        if (this.excedenteRetenidoA > 0 && this.recursoSaturadoA != null) {
            this.gestorRecursos.liberarRecurso(this.recursoSaturadoA, this.excedenteRetenidoA);
            this.excedenteRetenidoA = 0;
        }
        if (this.excedenteRetenidoB > 0 && this.recursoSaturadoB != null) {
            this.gestorRecursos.liberarRecurso(this.recursoSaturadoB, this.excedenteRetenidoB);
            this.excedenteRetenidoB = 0;
        }

        // actualizar el estado de resolucion del conflicto
        this.ultimoConflicto.setResuelto(true);
        this.ultimoConflicto.setIdPedidoGanador(idPedidoGanador);

        System.out.println("[RESOLUCION-MANUAL] Conflicto resuelto satisfactoriamente");
        System.out.println("  Pedido ganador " + tareaGanadora.getPedido().getId() + " finalizado en estado " + tareaGanadora.getPedido().getEstado().name());
        System.out.println("  Pedido desapropiado " + tareaPerdedora.getPedido().getId() + " retornado a lista de espera (" + tareaPerdedora.getPedido().getEstado().name() + ")");

        return true;
    }

    /**
     * Obtener el pedido que fue desapropiado (perdedor) durante la resolucion.
     *
     * @return pedido perdedor o null si no se ha resuelto conflicto.
     */
    public Pedido getPedidoPerdedor() {
        return this.pedidoPerdedor;
    }

    /**
     * Obtener el ultimo conflicto registrado.
     *
     * @return informacion del conflicto o null si no hay ninguno.
     */
    public InformacionConflicto getUltimoConflicto() {
        return this.ultimoConflicto;
    }

    /**
     * Obtener el detector de interbloqueos.
     *
     * @return instancia del detector.
     */
    public DetectorDeadlock getDetector() {
        return this.detector;
    }

    /**
     * Obtener el registro de asignacion de recursos.
     *
     * @return registro central de asignaciones.
     */
    public RegistroAsignacionRecursos getRegistro() {
        return this.registro;
    }
}
