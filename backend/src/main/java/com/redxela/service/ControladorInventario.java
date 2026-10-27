package com.redxela.service;

import com.redxela.almacen.AlmacenPaginado;
import com.redxela.model.LoteMercancia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// controlador central de inventario que administra existencias y el almacen paginado
public class ControladorInventario {

    // mapa con el conteo de stock disponible por identificador de producto
    private final Map<String, Integer> existenciasPorProducto;

    // lotes registrados por producto en orden de llegada para consumo fifo
    private final Map<String, List<String>> lotesPorProducto;

    // unidades pendientes de cada lote registrado en el almacen
    private final Map<String, Integer> unidadesPorLote;

    // referencia al almacen fisico paginado
    private final AlmacenPaginado almacen;

    // cerrojo para garantizar consistencia en la consulta y actualizacion de stock
    private final ReentrantLock cerrojoStock;

    /**
     * Constructor por defecto que inicializa el controlador y un almacen con dimensiones estandar.
     */
    public ControladorInventario() {
        this.existenciasPorProducto = new HashMap<String, Integer>();
        this.lotesPorProducto = new HashMap<String, List<String>>();
        this.unidadesPorLote = new HashMap<String, Integer>();
        this.almacen = new AlmacenPaginado();
        this.cerrojoStock = new ReentrantLock(true);
    }

    /**
     * Constructor sobrecargado con instancia especifica de almacen paginado.
     *
     * @param almacen instancia del almacen fisico a administrar.
     */
    public ControladorInventario(AlmacenPaginado almacen) {
        this.existenciasPorProducto = new HashMap<String, Integer>();
        this.lotesPorProducto = new HashMap<String, List<String>>();
        this.unidadesPorLote = new HashMap<String, Integer>();
        this.almacen = almacen;
        this.cerrojoStock = new ReentrantLock(true);
    }

    /**
     * Registrar la llegada de un lote de mercancia asignando celdas e incrementando stock.
     *
     * @param lote lote de mercancia entrante.
     * @return verdadero si se pudo ubicar y actualizar el stock o falso en fallo.
     */
    public boolean procesarEntradaLote(LoteMercancia lote) {
        if (lote == null) {
            return false;
        }

        // asignar celdas fisicas de forma no contigua en el almacen
        boolean asignado = this.almacen.asignarEspacioNoContiguo(lote);
        if (!asignado) {
            return false;
        }

        // incrementar el stock disponible del producto
        this.cerrojoStock.lock();
        try {
            String idProd = lote.getProducto().getId();
            int actual = 0;
            if (this.existenciasPorProducto.containsKey(idProd)) {
                actual = this.existenciasPorProducto.get(idProd);
            }
            int nuevo = actual + lote.getCantidad();
            this.existenciasPorProducto.put(idProd, nuevo);

            // registrar el lote para consumo fifo posterior
            List<String> lotes = this.lotesPorProducto.get(idProd);
            if (lotes == null) {
                lotes = new ArrayList<String>();
                this.lotesPorProducto.put(idProd, lotes);
            }
            lotes.add(lote.getIdLote());
            this.unidadesPorLote.put(lote.getIdLote(), lote.getCantidad());

            System.out.println("[INVENTARIO] Stock actualizado para " + lote.getProducto().getNombre() + " (" + idProd + "): " + nuevo + " unidades");
            return true;
        } finally {
            this.cerrojoStock.unlock();
        }
    }

    /**
     * Retirar unidades de stock para satisfacer la demanda de un pedido.
     *
     * @param idProducto identificador del producto a descontar.
     * @param cantidad numero de unidades a sustraer.
     * @return verdadero si habia suficiente stock y se desconto o falso en caso contrario.
     */
    public boolean descontarStock(String idProducto, int cantidad) {
        if (idProducto == null || cantidad <= 0) {
            return false;
        }

        boolean exito = false;
        this.cerrojoStock.lock();
        List<String> lotesAgotados = new ArrayList<String>();
        try {
            int actual = 0;
            if (this.existenciasPorProducto.containsKey(idProducto)) {
                actual = this.existenciasPorProducto.get(idProducto);
            }

            if (actual < cantidad) {
                exito = false;
            } else {
                int nuevo = actual - cantidad;
                this.existenciasPorProducto.put(idProducto, nuevo);

                // consumir los lotes mas antiguos primero
                List<String> lotes = this.lotesPorProducto.get(idProducto);
                int restante = cantidad;
                if (lotes != null) {
                    while (restante > 0 && !lotes.isEmpty()) {
                        String idLote = lotes.get(0);
                        int disponibles = 0;
                        if (this.unidadesPorLote.containsKey(idLote)) {
                            disponibles = this.unidadesPorLote.get(idLote);
                        }
                        int tomar = disponibles;
                        if (tomar > restante) {
                            tomar = restante;
                        }
                        disponibles = disponibles - tomar;
                        restante = restante - tomar;
                        if (disponibles <= 0) {
                            lotes.remove(0);
                            this.unidadesPorLote.remove(idLote);
                            lotesAgotados.add(idLote);
                        } else {
                            this.unidadesPorLote.put(idLote, disponibles);
                        }
                    }
                }
                exito = true;
            }
        } finally {
            this.cerrojoStock.unlock();
        }
        // liberar fuera del cerrojo las celdas de los lotes agotados
        for (int i = 0; i < lotesAgotados.size(); i++) {
            this.almacen.liberarEspacioLote(lotesAgotados.get(i));
        }
        return exito;
    }

    /**
     * Consultar la cantidad de unidades en existencia de un producto.
     *
     * @param idProducto identificador del producto.
     * @return cantidad actual en existencias.
     */
    public int consultarStock(String idProducto) {
        if (idProducto == null) {
            return 0;
        }

        this.cerrojoStock.lock();
        try {
            if (this.existenciasPorProducto.containsKey(idProducto)) {
                return this.existenciasPorProducto.get(idProducto);
            }
            return 0;
        } finally {
            this.cerrojoStock.unlock();
        }
    }

    /**
     * Consultar si existen suficientes unidades para suplir un pedido.
     *
     * @param idProducto identificador del producto.
     * @param cantidad numero de unidades solicitadas.
     * @return verdadero si el stock cubre la cantidad o falso si es insuficiente.
     */
    public boolean tieneStockSuficiente(String idProducto, int cantidad) {
        int disponible = this.consultarStock(idProducto);
        if (disponible >= cantidad) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Obtener la referencia al almacen paginado administrado.
     *
     * @return instancia de almacen paginado.
     */
    public AlmacenPaginado getAlmacen() {
        return this.almacen;
    }
}
