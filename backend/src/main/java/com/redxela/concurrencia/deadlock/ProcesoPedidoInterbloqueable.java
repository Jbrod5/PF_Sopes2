package com.redxela.concurrencia.deadlock;

import com.redxela.concurrencia.GestorRecursos;
import com.redxela.model.EstadoPedido;
import com.redxela.model.Pedido;
import com.redxela.model.TipoRecurso;

// tarea de pedido disenada para simulacion controlada de interbloqueo y soporte de desapropiacion
public class ProcesoPedidoInterbloqueable implements Runnable {

    // pedido asignado
    private final Pedido pedido;

    // gestor de recursos compartidos
    private final GestorRecursos gestorRecursos;

    // registro de asignacion y espera
    private final RegistroAsignacionRecursos registro;

    // primer recurso a solicitar
    private final TipoRecurso recursoInicial;

    // segundo recurso a solicitar en orden inverso
    private final TipoRecurso recursoSegundo;

    // pausa intermedia para forzar que ambos hilos adquieran su primer recurso
    private final long pausaIntermediaMs;

    // duracion de la simulacion de procesamiento
    private final long tiempoTrabajoMs;

    // bandera volatil que indica si este proceso fue desapropiado manualmente
    private volatile boolean desapropiado;

    // hilo que se encuentra ejecutando esta tarea
    private volatile Thread hiloEjecutor;

    // estado de retencion de recursos
    private boolean recursoInicialAdquirido;
    private boolean recursoSegundoAdquirido;

    /**
     * Constructor sobrecargado con pedido gestor registro y orden especifico de recursos.
     *
     * @param pedido pedido a procesar.
     * @param gestorRecursos gestor central de recursos.
     * @param registro registro del grafo de asignaciones.
     * @param recursoInicial primer recurso a solicitar.
     * @param recursoSegundo segundo recurso a solicitar.
     * @param pausaIntermediaMs tiempo de retencion intermedia en milisegundos.
     * @param tiempoTrabajoMs duracion de ejecucion con ambos recursos en milisegundos.
     */
    public ProcesoPedidoInterbloqueable(
            Pedido pedido,
            GestorRecursos gestorRecursos,
            RegistroAsignacionRecursos registro,
            TipoRecurso recursoInicial,
            TipoRecurso recursoSegundo,
            long pausaIntermediaMs,
            long tiempoTrabajoMs
    ) {
        this.pedido = pedido;
        this.gestorRecursos = gestorRecursos;
        this.registro = registro;
        this.recursoInicial = recursoInicial;
        this.recursoSegundo = recursoSegundo;
        this.pausaIntermediaMs = pausaIntermediaMs;
        this.tiempoTrabajoMs = tiempoTrabajoMs;
        this.desapropiado = false;
        this.recursoInicialAdquirido = false;
        this.recursoSegundoAdquirido = false;
    }

    /**
     * Desapropiar de inmediato los recursos retenidos por este pedido interrumpiendo su espera.
     */
    public void desapropiar() {
        this.desapropiado = true;
        System.out.println("[DESAPROPIACION] Forzando desapropiacion de recursos para pedido " + this.pedido.getId());
        if (this.hiloEjecutor != null) {
            this.hiloEjecutor.interrupt();
        }
    }

    /**
     * Ciclo de ejecucion del hilo que solicita recursos en orden especifico.
     */
    @Override
    public void run() {
        this.hiloEjecutor = Thread.currentThread();
        this.pedido.setEstado(EstadoPedido.EN_ESPERA);

        try {
            // solicitar el primer recurso
            System.out.println("[SOLICITUD-1] Pedido " + this.pedido.getId() + " solicitando " + this.recursoInicial.getNombre());
            boolean obtenido1 = this.gestorRecursos.adquirirRecurso(this.recursoInicial);
            if (!obtenido1 || this.desapropiado) {
                return;
            }

            this.recursoInicialAdquirido = true;
            this.registro.registrarAsignacion(this.pedido.getId(), this.recursoInicial);
            System.out.println("[RETENIDO-1] Pedido " + this.pedido.getId() + " obtuvo y retiene " + this.recursoInicial.getNombre());

            // pausar brevemente para permitir que el hilo opuesto adquiera su primer recurso
            try {
                Thread.sleep(this.pausaIntermediaMs);
            } catch (InterruptedException excepcion) {
                if (this.desapropiado) {
                    return;
                }
            }

            if (this.desapropiado) {
                return;
            }

            // registrar la solicitud del segundo recurso en el grafo
            this.registro.registrarSolicitud(this.pedido.getId(), this.recursoSegundo);
            System.out.println("[SOLICITUD-2] Pedido " + this.pedido.getId() + " esperando " + this.recursoSegundo.getNombre());

            // intentar adquirir el segundo recurso quedando potencialmente bloqueado
            boolean obtenido2 = this.gestorRecursos.adquirirRecurso(this.recursoSegundo);
            if (!obtenido2 || this.desapropiado) {
                System.out.println("[INTERRUPCION] Pedido " + this.pedido.getId() + " fue desapropiado mientras esperaba");
                // liberar el recurso inicial retenido antes de retornar
                if (this.recursoInicialAdquirido) {
                    this.gestorRecursos.liberarRecurso(this.recursoInicial);
                    this.recursoInicialAdquirido = false;
                }
                return;
            }

            this.recursoSegundoAdquirido = true;
            this.registro.registrarAsignacion(this.pedido.getId(), this.recursoSegundo);

            // ambos recursos obtenidos proceder al procesamiento
            this.pedido.setEstado(EstadoPedido.PROCESANDO);
            System.out.println("[PROCESANDO] Pedido " + this.pedido.getId() + " ejecutando con " + this.recursoInicial.getNombre() + " y " + this.recursoSegundo.getNombre());

            Thread.sleep(this.tiempoTrabajoMs);

            this.pedido.setEstado(EstadoPedido.COMPLETADO);
            System.out.println("[COMPLETADO] Pedido " + this.pedido.getId() + " finalizado exitosamente");

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        } finally {
            // gestionar liberacion segun si fue desapropiado o si completo normalmente
            if (this.desapropiado) {
                // si fue desapropiado devolver el recurso retenido y retornar a lista de espera
                if (this.recursoInicialAdquirido) {
                    this.gestorRecursos.liberarRecurso(this.recursoInicial);
                    this.recursoInicialAdquirido = false;
                    System.out.println("[LIBERACION-PREEMPT] Pedido " + this.pedido.getId() + " libero " + this.recursoInicial.getNombre() + " por desapropiacion");
                }
                this.pedido.setEstado(EstadoPedido.EN_ESPERA);
                this.registro.limpiarTodoPedido(this.pedido.getId());
            } else {
                // si completo normalmente liberar ambos recursos
                if (this.recursoSegundoAdquirido) {
                    this.gestorRecursos.liberarRecurso(this.recursoSegundo);
                    this.recursoSegundoAdquirido = false;
                }
                if (this.recursoInicialAdquirido) {
                    this.gestorRecursos.liberarRecurso(this.recursoInicial);
                    this.recursoInicialAdquirido = false;
                }
                this.registro.limpiarTodoPedido(this.pedido.getId());
            }
        }
    }

    /**
     * Obtener el pedido asociado.
     *
     * @return pedido en conflicto.
     */
    public Pedido getPedido() {
        return this.pedido;
    }

    /**
     * Consultar si el proceso fue desapropiado.
     *
     * @return verdadero si fue desapropiado.
     */
    public boolean isDesapropiado() {
        return this.desapropiado;
    }

    /**
     * Obtener el primer recurso configurado.
     *
     * @return tipo de recurso inicial.
     */
    public TipoRecurso getRecursoInicial() {
        return this.recursoInicial;
    }

    /**
     * Obtener el segundo recurso configurado.
     *
     * @return tipo de recurso segundo.
     */
    public TipoRecurso getRecursoSegundo() {
        return this.recursoSegundo;
    }
}
