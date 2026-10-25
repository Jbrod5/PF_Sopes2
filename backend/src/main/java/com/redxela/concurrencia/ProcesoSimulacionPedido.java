package com.redxela.concurrencia;

import com.redxela.model.EstadoPedido;
import com.redxela.model.Pedido;
import com.redxela.model.TipoRecurso;

import java.util.ArrayList;
import java.util.List;

// tarea ejecutable en hilo que simula el procesamiento concurrente de un pedido
public class ProcesoSimulacionPedido implements Runnable {

    // pedido asignado para ser procesado por este hilo
    private final Pedido pedido;

    // referencia al gestor central de recursos compartidos
    private final GestorRecursos gestorRecursos;

    // tiempo en milisegundos que el pedido retiene los recursos simulando trabajo
    private final long tiempoProcesamientoMs;

    // marca de inicio de espera de recursos
    private volatile long inicioEsperaMs;

    // marca de inicio de procesamiento efectivo
    private volatile long inicioProcesoMs;

    /**
     * Constructor sobrecargado con pedido y gestor de recursos.
     *
     * @param pedido pedido a simular.
     * @param gestorRecursos instancia compartida del gestor de recursos.
     */
    public ProcesoSimulacionPedido(Pedido pedido, GestorRecursos gestorRecursos) {
        this.pedido = pedido;
        this.gestorRecursos = gestorRecursos;
        // calcular tiempo base proporcional a la cantidad de articulos
        int cantidad = pedido.getCantidadTotal();
        if (cantidad <= 0) {
            cantidad = 1;
        }
        this.tiempoProcesamientoMs = cantidad * 300L;
        this.inicioEsperaMs = 0L;
        this.inicioProcesoMs = 0L;
    }

    /**
     * Constructor sobrecargado con tiempo explicito de procesamiento.
     *
     * @param pedido pedido a simular.
     * @param gestorRecursos instancia compartida del gestor de recursos.
     * @param tiempoProcesamientoMs duracion de la simulacion en milisegundos.
     */
    public ProcesoSimulacionPedido(Pedido pedido, GestorRecursos gestorRecursos, long tiempoProcesamientoMs) {
        this.pedido = pedido;
        this.gestorRecursos = gestorRecursos;
        this.tiempoProcesamientoMs = tiempoProcesamientoMs;
        this.inicioEsperaMs = 0L;
        this.inicioProcesoMs = 0L;
    }

    /**
     * Ejecucion del hilo que solicita recursos procesa el pedido y libera los recursos.
     */
    @Override
    public void run() {
        // registrar marca de inicio de espera de recursos
        this.inicioEsperaMs = System.currentTimeMillis();
        // obtener los recursos requeridos por el pedido
        List<TipoRecurso> requeridos = this.pedido.obtenerRecursosRequeridos();
        List<TipoRecurso> adquiridos = new ArrayList<TipoRecurso>();

        // marcar el pedido en estado de espera
        this.pedido.setEstado(EstadoPedido.EN_ESPERA);
        System.out.println("[ESPERA] Hilo " + Thread.currentThread().getName() + " solicita recursos para pedido " + this.pedido.getId());

        try {
            // adquirir cada uno de los recursos necesarios secuencialmente
            for (int i = 0; i < requeridos.size(); i++) {
                TipoRecurso recurso = requeridos.get(i);
                System.out.println("[SOLICITUD] Hilo " + Thread.currentThread().getName() + " pidiendo " + recurso.getNombre() + " para pedido " + this.pedido.getId());
                boolean adquirido = this.gestorRecursos.adquirirRecursoPara(recurso, this.pedido.getId());
                if (adquirido) {
                    adquiridos.add(recurso);
                    System.out.println("[ASIGNADO] Hilo " + Thread.currentThread().getName() + " obtuvo " + recurso.getNombre() + " para pedido " + this.pedido.getId());
                } else {
                    System.err.println("[FALLO] Hilo " + Thread.currentThread().getName() + " no pudo obtener " + recurso.getNombre());
                    this.pedido.setEstado(EstadoPedido.CANCELADO);
                    return;
                }
            }

            // una vez obtenidos todos los recursos cambiar estado a procesando
            this.pedido.setEstado(EstadoPedido.PROCESANDO);
            this.inicioProcesoMs = System.currentTimeMillis();
            System.out.println("[PROCESANDO] Pedido " + this.pedido.getId() + " en ejecucion (" + this.tiempoProcesamientoMs + " ms)");

            // simular el trabajo con los recursos asignados
            Thread.sleep(this.tiempoProcesamientoMs);

            // actualizar estado a completado
            this.pedido.setEstado(EstadoPedido.COMPLETADO);
            System.out.println("[COMPLETADO] Pedido " + this.pedido.getId() + " ha finalizado su procesamiento");

        } catch (InterruptedException excepcion) {
            // procesar interrupcion del hilo
            Thread.currentThread().interrupt();
            this.pedido.setEstado(EstadoPedido.CANCELADO);
            System.err.println("[INTERRUMPIDO] Hilo de pedido " + this.pedido.getId() + ": " + excepcion.getMessage());
        } finally {
            // garantizar siempre la liberacion de los recursos adquiridos en orden inverso
            for (int i = adquiridos.size() - 1; i >= 0; i--) {
                TipoRecurso recurso = adquiridos.get(i);
                this.gestorRecursos.liberarRecursoDe(recurso, this.pedido.getId());
                System.out.println("[LIBERADO] Hilo " + Thread.currentThread().getName() + " devolvio " + recurso.getNombre() + " de pedido " + this.pedido.getId());
            }
        }
    }

    /**
     * Obtener el tiempo acumulado en espera de recursos en milisegundos.
     *
     * @return milisegundos en espera.
     */
    public long getTiempoEsperaMs() {
        if (this.inicioEsperaMs == 0L) {
            return 0L;
        }
        long finEspera = this.inicioProcesoMs;
        if (finEspera == 0L) {
            finEspera = System.currentTimeMillis();
        }
        return finEspera - this.inicioEsperaMs;
    }

    /**
     * Obtener el tiempo restante estimado de procesamiento en milisegundos.
     *
     * @return milisegundos restantes.
     */
    public long getTiempoRestanteMs() {
        if (this.inicioProcesoMs == 0L) {
            return this.tiempoProcesamientoMs;
        }
        long finEstimado = this.inicioProcesoMs + this.tiempoProcesamientoMs;
        long restante = finEstimado - System.currentTimeMillis();
        if (restante < 0L) {
            return 0L;
        }
        return restante;
    }

    /**
     * Obtener el avance porcentual estimado del procesamiento.
     *
     * @return entero entre 0 y 100.
     */
    public int getProgreso() {
        if (this.inicioProcesoMs == 0L) {
            return 0;
        }
        if (this.tiempoProcesamientoMs <= 0L) {
            return 100;
        }
        long transcurrido = System.currentTimeMillis() - this.inicioProcesoMs;
        long avance = transcurrido * 100L / this.tiempoProcesamientoMs;
        if (avance > 100L) {
            return 100;
        }
        if (avance < 0L) {
            return 0;
        }
        return (int) avance;
    }

    /**
     * Obtener el pedido asociado al hilo.
     *
     * @return instancia del pedido.
     */
    public Pedido getPedido() {
        return this.pedido;
    }
}
