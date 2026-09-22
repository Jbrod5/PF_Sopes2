package com.redxela.model;

// entidad que representa un lote de mercancia entrante al centro de distribucion
public class LoteMercancia {

    // identificador unico del lote
    private String idLote;

    // producto contenido en este lote
    private Producto producto;

    // cantidad total de unidades recibidas en el lote
    private int cantidad;

    // marca de tiempo de recepcion en milisegundos
    private long tiempoLlegadaMs;

    /**
     * Constructor por defecto de la clase lote de mercancia.
     */
    public LoteMercancia() {
        // inicializar propiedades por defecto
        this.idLote = "";
        this.producto = new Producto();
        this.cantidad = 1;
        this.tiempoLlegadaMs = System.currentTimeMillis();
    }

    /**
     * Constructor sobrecargado con identificador producto y cantidad.
     *
     * @param idLote identificador unico del lote.
     * @param producto producto correspondiente.
     * @param cantidad numero de unidades recibidas.
     */
    public LoteMercancia(String idLote, Producto producto, int cantidad) {
        this.idLote = idLote;
        this.producto = producto;
        this.cantidad = cantidad;
        this.tiempoLlegadaMs = System.currentTimeMillis();
    }

    /**
     * Constructor sobrecargado completo con tiempo de llegada.
     *
     * @param idLote identificador unico del lote.
     * @param producto producto correspondiente.
     * @param cantidad numero de unidades recibidas.
     * @param tiempoLlegadaMs marca de tiempo en milisegundos.
     */
    public LoteMercancia(String idLote, Producto producto, int cantidad, long tiempoLlegadaMs) {
        this.idLote = idLote;
        this.producto = producto;
        this.cantidad = cantidad;
        this.tiempoLlegadaMs = tiempoLlegadaMs;
    }

    /**
     * Obtener el identificador del lote.
     *
     * @return identificador unico del lote.
     */
    public String getIdLote() {
        return this.idLote;
    }

    /**
     * Asignar el identificador del lote.
     *
     * @param idLote identificador a registrar.
     */
    public void setIdLote(String idLote) {
        this.idLote = idLote;
    }

    /**
     * Obtener el producto del lote.
     *
     * @return producto contenido.
     */
    public Producto getProducto() {
        return this.producto;
    }

    /**
     * Asignar el producto del lote.
     *
     * @param producto producto a registrar.
     */
    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    /**
     * Obtener la cantidad de unidades en el lote.
     *
     * @return numero de unidades.
     */
    public int getCantidad() {
        return this.cantidad;
    }

    /**
     * Asignar la cantidad de unidades en el lote.
     *
     * @param cantidad numero de unidades a registrar.
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Obtener la marca de tiempo de llegada.
     *
     * @return tiempo en milisegundos.
     */
    public long getTiempoLlegadaMs() {
        return this.tiempoLlegadaMs;
    }

    /**
     * Asignar la marca de tiempo de llegada.
     *
     * @param tiempoLlegadaMs tiempo en milisegundos a registrar.
     */
    public void setTiempoLlegadaMs(long tiempoLlegadaMs) {
        this.tiempoLlegadaMs = tiempoLlegadaMs;
    }

    /**
     * Representacion descriptiva del lote en cadena de texto.
     *
     * @return cadena con los datos del lote.
     */
    @Override
    public String toString() {
        StringBuilder constructorCadena = new StringBuilder();
        constructorCadena.append("LoteMercancia[id=");
        constructorCadena.append(this.idLote);
        constructorCadena.append(", producto=");
        constructorCadena.append(this.producto.getNombre());
        constructorCadena.append(", cantidad=");
        constructorCadena.append(this.cantidad);
        constructorCadena.append("]");
        return constructorCadena.toString();
    }
}
