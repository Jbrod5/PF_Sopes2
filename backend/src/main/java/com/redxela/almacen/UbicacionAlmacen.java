package com.redxela.almacen;

// clase que representa una ubicacion fisica individual dentro del almacen
public class UbicacionAlmacen {

    // identificador formateado de la ubicacion
    private String idUbicacion;

    // numero de pasillo en el almacen
    private int pasillo;

    // numero de nivel o altura de estanteria
    private int nivel;

    // numero de espacio o casillero en el nivel
    private int espacio;

    // estado de ocupacion fisica
    private boolean ocupada;

    // identificador del lote que ocupa esta posicion
    private String idLoteAsignado;

    // identificador del producto almacenado
    private String idProductoAsignado;

    /**
     * Constructor por defecto de la ubicacion del almacen.
     */
    public UbicacionAlmacen() {
        this.pasillo = 1;
        this.nivel = 1;
        this.espacio = 1;
        this.idUbicacion = "P01-N01-E01";
        this.ocupada = false;
        this.idLoteAsignado = "";
        this.idProductoAsignado = "";
    }

    /**
     * Constructor sobrecargado con coordenadas fisicas de la ubicacion.
     *
     * @param pasillo numero de pasillo.
     * @param nivel numero de nivel o estante.
     * @param espacio numero de espacio en el estante.
     */
    public UbicacionAlmacen(int pasillo, int nivel, int espacio) {
        this.pasillo = pasillo;
        this.nivel = nivel;
        this.espacio = espacio;
        this.idUbicacion = this.generarIdUbicacion(pasillo, nivel, espacio);
        this.ocupada = false;
        this.idLoteAsignado = "";
        this.idProductoAsignado = "";
    }

    // generar el identificador textual de la coordenada
    private String generarIdUbicacion(int pasillo, int nivel, int espacio) {
        StringBuilder constructor = new StringBuilder();
        constructor.append("P");
        if (pasillo < 10) {
            constructor.append("0");
        }
        constructor.append(pasillo);
        constructor.append("-N");
        if (nivel < 10) {
            constructor.append("0");
        }
        constructor.append(nivel);
        constructor.append("-E");
        if (espacio < 10) {
            constructor.append("0");
        }
        constructor.append(espacio);
        return constructor.toString();
    }

    /**
     * Asignar un lote y producto a esta ubicacion fisica marcandola como ocupada.
     *
     * @param idLote identificador del lote asignado.
     * @param idProducto identificador del producto asignado.
     */
    public void asignarLote(String idLote, String idProducto) {
        this.ocupada = true;
        this.idLoteAsignado = idLote;
        this.idProductoAsignado = idProducto;
    }

    /**
     * Liberar la ubicacion dejandola disponible para nuevas asignaciones.
     */
    public void liberar() {
        this.ocupada = false;
        this.idLoteAsignado = "";
        this.idProductoAsignado = "";
    }

    /**
     * Obtener el identificador formateado de la ubicacion.
     *
     * @return identificador de coordenada.
     */
    public String getIdUbicacion() {
        return this.idUbicacion;
    }

    /**
     * Obtener el numero de pasillo.
     *
     * @return numero de pasillo.
     */
    public int getPasillo() {
        return this.pasillo;
    }

    /**
     * Obtener el numero de nivel.
     *
     * @return numero de nivel.
     */
    public int getNivel() {
        return this.nivel;
    }

    /**
     * Obtener el numero de espacio.
     *
     * @return numero de espacio.
     */
    public int getEspacio() {
        return this.espacio;
    }

    /**
     * Consultar si la ubicacion se encuentra ocupada.
     *
     * @return verdadero si esta ocupada o falso si disponible.
     */
    public boolean isOcupada() {
        return this.ocupada;
    }

    /**
     * Obtener el lote asignado a la ubicacion.
     *
     * @return identificador del lote o cadena vacia si libre.
     */
    public String getIdLoteAsignado() {
        return this.idLoteAsignado;
    }

    /**
     * Obtener el producto asignado a la ubicacion.
     *
     * @return identificador del producto o cadena vacia si libre.
     */
    public String getIdProductoAsignado() {
        return this.idProductoAsignado;
    }

    /**
     * Representacion textual descriptiva de la celda de almacenamiento.
     *
     * @return cadena con los datos de la ubicacion.
     */
    @Override
    public String toString() {
        StringBuilder constructor = new StringBuilder();
        constructor.append("Ubicacion[id=");
        constructor.append(this.idUbicacion);
        constructor.append(", ocupada=");
        constructor.append(this.ocupada);
        if (this.ocupada) {
            constructor.append(", lote=");
            constructor.append(this.idLoteAsignado);
        }
        constructor.append("]");
        return constructor.toString();
    }
}
