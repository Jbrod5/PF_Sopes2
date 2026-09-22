package com.redxela.service;

import com.redxela.model.LoteMercancia;
import com.redxela.model.Producto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// servicio del area de recepcion que funciona como productor enviando lotes al buffer
public class ServicioRecepcion implements Runnable {

    // referencia exclusiva al buffer acotado para comunicar con inventario
    private final BufferRecepcion bufferRecepcion;

    // catalogo de productos disponibles para recepcion
    private final List<Producto> catalogoProductos;

    // bandera de control para el hilo de ejecucion
    private volatile boolean activo;

    // intervalo de tiempo entre llegadas de camiones en milisegundos
    private long intervaloSimulacionMs;

    // generador de valores aleatorios
    private final Random aleatorio;

    // contador secuencial para identificadores de lotes
    private int contadorLote;

    /**
     * Constructor sobrecargado con buffer y catalogo de productos.
     *
     * @param bufferRecepcion buffer acotado hacia inventario.
     * @param catalogoProductos lista de productos que pueden recibirse.
     */
    public ServicioRecepcion(BufferRecepcion bufferRecepcion, List<Producto> catalogoProductos) {
        this.bufferRecepcion = bufferRecepcion;
        this.catalogoProductos = new ArrayList<Producto>();
        if (catalogoProductos != null) {
            for (int i = 0; i < catalogoProductos.size(); i++) {
                this.catalogoProductos.add(catalogoProductos.get(i));
            }
        }
        this.activo = false;
        this.intervaloSimulacionMs = 1500L;
        this.aleatorio = new Random();
        this.contadorLote = 1;
    }

    /**
     * Constructor sobrecargado con intervalo personalizado de simulacion.
     *
     * @param bufferRecepcion buffer acotado hacia inventario.
     * @param catalogoProductos lista de productos.
     * @param intervaloSimulacionMs tiempo entre llegadas de camiones.
     */
    public ServicioRecepcion(BufferRecepcion bufferRecepcion, List<Producto> catalogoProductos, long intervaloSimulacionMs) {
        this.bufferRecepcion = bufferRecepcion;
        this.catalogoProductos = new ArrayList<Producto>();
        if (catalogoProductos != null) {
            for (int i = 0; i < catalogoProductos.size(); i++) {
                this.catalogoProductos.add(catalogoProductos.get(i));
            }
        }
        this.activo = false;
        this.intervaloSimulacionMs = intervaloSimulacionMs;
        this.aleatorio = new Random();
        this.contadorLote = 1;
    }

    /**
     * Registrar manualmente la llegada de un lote de mercaderia a la recepcion.
     *
     * @param lote lote recibido por el camion.
     * @return verdadero si fue depositado en el buffer hacia inventario.
     */
    public boolean recibirLoteDeCamion(LoteMercancia lote) {
        if (lote == null) {
            return false;
        }
        System.out.println("[RECEPCION] Camion arriba con lote " + lote.getIdLote() + " (" + lote.getCantidad() + " unidades de " + lote.getProducto().getNombre() + ")");
        boolean enviado = this.bufferRecepcion.enviarLote(lote);
        if (enviado) {
            System.out.println("[RECEPCION] Lote " + lote.getIdLote() + " depositado en buffer hacia inventario");
        }
        return enviado;
    }

    /**
     * Generar e ingresar un lote aleatorio de mercaderia entrante.
     *
     * @return lote generado y enviado.
     */
    public LoteMercancia generarLoteAleatorio() {
        if (this.catalogoProductos.isEmpty()) {
            return null;
        }

        // seleccionar un producto aleatorio del catalogo
        int indice = this.aleatorio.nextInt(this.catalogoProductos.size());
        Producto producto = this.catalogoProductos.get(indice);

        // determinar cantidad de unidades del lote entre 2 y 6
        int cantidad = 2 + this.aleatorio.nextInt(5);

        String idLote = "LOT-" + this.contadorLote;
        this.contadorLote = this.contadorLote + 1;

        LoteMercancia nuevoLote = new LoteMercancia(idLote, producto, cantidad);
        this.recibirLoteDeCamion(nuevoLote);
        return nuevoLote;
    }

    /**
     * Iniciar la simulacion continua en segundo plano de llegada de mercaderia.
     */
    public void iniciar() {
        this.activo = true;
    }

    /**
     * Detener la simulacion de recepcion de camiones.
     */
    public void detener() {
        this.activo = false;
    }

    /**
     * Ejecucion del hilo productor que genera y envia lotes de forma continua.
     */
    @Override
    public void run() {
        this.activo = true;
        System.out.println("[RECEPCION] Servicio de recepcion iniciado en segundo plano");

        while (this.activo) {
            try {
                // esperar el intervalo de simulacion entre camiones
                Thread.sleep(this.intervaloSimulacionMs);

                if (!this.activo) {
                    break;
                }

                // generar lote y enviarlo al buffer
                this.generarLoteAleatorio();

            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
                System.err.println("[RECEPCION] Hilo de recepcion interrumpido: " + excepcion.getMessage());
                break;
            }
        }

        System.out.println("[RECEPCION] Servicio de recepcion finalizado");
    }

    /**
     * Consultar si el servicio de recepcion esta activo.
     *
     * @return verdadero si esta en ejecucion continua.
     */
    public boolean isActivo() {
        return this.activo;
    }
}
