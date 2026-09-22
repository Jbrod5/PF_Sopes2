package com.redxela.service;

import com.redxela.model.LoteMercancia;

import java.util.concurrent.TimeUnit;

// servicio del area de inventario que funciona como consumidor extrayendo lotes del buffer
public class ServicioInventario implements Runnable {

    // referencia al buffer acotado compartido con recepcion
    private final BufferRecepcion bufferRecepcion;

    // controlador de inventario y almacen fisico
    private final ControladorInventario controladorInventario;

    // bandera de control para el hilo consumidor
    private volatile boolean activo;

    // tiempo de procesamiento de cada lote en milisegundos simulando clasificacion y estibado
    private long tiempoProcesamientoLoteMs;

    /**
     * Constructor sobrecargado con buffer y controlador de inventario.
     *
     * @param bufferRecepcion canal de comunicacion acotado con recepcion.
     * @param controladorInventario controlador que maneja existencias y celdas.
     */
    public ServicioInventario(BufferRecepcion bufferRecepcion, ControladorInventario controladorInventario) {
        this.bufferRecepcion = bufferRecepcion;
        this.controladorInventario = controladorInventario;
        this.activo = false;
        this.tiempoProcesamientoLoteMs = 500L;
    }

    /**
     * Constructor sobrecargado con tiempo de estibado personalizado.
     *
     * @param bufferRecepcion buffer acotado con recepcion.
     * @param controladorInventario controlador de existencias.
     * @param tiempoProcesamientoLoteMs duracion del registro por lote.
     */
    public ServicioInventario(
            BufferRecepcion bufferRecepcion,
            ControladorInventario controladorInventario,
            long tiempoProcesamientoLoteMs
    ) {
        this.bufferRecepcion = bufferRecepcion;
        this.controladorInventario = controladorInventario;
        this.activo = false;
        this.tiempoProcesamientoLoteMs = tiempoProcesamientoLoteMs;
    }

    /**
     * Procesar un unico lote extraido del buffer acotado.
     *
     * @return verdadero si el lote fue extraido y registrado exitosamente.
     */
    public boolean procesarSiguienteLote() {
        // recibir lote del buffer sin acoplar directamente con recepcion
        LoteMercancia lote = this.bufferRecepcion.recibirLoteConTiempo(1000L, TimeUnit.MILLISECONDS);
        if (lote == null) {
            return false;
        }

        System.out.println("[INVENTARIO] Lote " + lote.getIdLote() + " extraido del buffer para clasificacion y registro");

        // simular el tiempo que toma colocar la mercancia en los estantes
        try {
            Thread.sleep(this.tiempoProcesamientoLoteMs);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // procesar la entrada asignando celdas paginadas y actualizando existencias
        boolean exito = this.controladorInventario.procesarEntradaLote(lote);
        return exito;
    }

    /**
     * Iniciar la ejecucion continua en segundo plano del servicio de inventario.
     */
    public void iniciar() {
        this.activo = true;
    }

    /**
     * Detener la ejecucion del hilo de inventario.
     */
    public void detener() {
        this.activo = false;
    }

    /**
     * Ejecucion del hilo consumidor que procesa continuamente los lotes entrantes.
     */
    @Override
    public void run() {
        this.activo = true;
        System.out.println("[INVENTARIO] Servicio de inventario iniciado en segundo plano");

        while (this.activo) {
            try {
                // intentar procesar el siguiente lote del buffer acotado
                this.procesarSiguienteLote();

            } catch (Exception excepcion) {
                System.err.println("[INVENTARIO] Error procesando lote: " + excepcion.getMessage());
            }
        }

        System.out.println("[INVENTARIO] Servicio de inventario finalizado");
    }

    /**
     * Consultar si el servicio de inventario se encuentra en ejecucion continua.
     *
     * @return verdadero si esta activo.
     */
    public boolean isActivo() {
        return this.activo;
    }

    /**
     * Obtener el controlador de inventario asociado.
     *
     * @return controlador de inventario.
     */
    public ControladorInventario getControladorInventario() {
        return this.controladorInventario;
    }
}
