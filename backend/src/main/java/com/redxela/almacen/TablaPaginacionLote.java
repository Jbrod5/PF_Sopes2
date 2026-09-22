package com.redxela.almacen;

import java.util.ArrayList;
import java.util.List;

// clase que representa la tabla de paginacion de un lote asignado en el almacen
public class TablaPaginacionLote {

    // identificador unico del lote asociado
    private String idLote;

    // identificador del producto contenido
    private String idProducto;

    // cantidad total de unidades y paginas asignadas al lote
    private int cantidadUnidades;

    // lista de ubicaciones fisicas asignadas a cada unidad o bloque logico del lote
    private List<UbicacionAlmacen> ubicacionesAsignadas;

    /**
     * Constructor por defecto de la tabla de paginacion del lote.
     */
    public TablaPaginacionLote() {
        this.idLote = "";
        this.idProducto = "";
        this.cantidadUnidades = 0;
        this.ubicacionesAsignadas = new ArrayList<UbicacionAlmacen>();
    }

    /**
     * Constructor sobrecargado con identificadores y cantidad requerida.
     *
     * @param idLote identificador del lote.
     * @param idProducto identificador del producto.
     * @param cantidadUnidades cantidad de paginas o celdas a ocupar.
     */
    public TablaPaginacionLote(String idLote, String idProducto, int cantidadUnidades) {
        this.idLote = idLote;
        this.idProducto = idProducto;
        this.cantidadUnidades = cantidadUnidades;
        this.ubicacionesAsignadas = new ArrayList<UbicacionAlmacen>();
    }

    /**
     * Anadir una nueva entrada a la tabla de asignacion fisica de paginas.
     *
     * @param ubicacion celda fisica asignada a una unidad del lote.
     */
    public void agregarEntrada(UbicacionAlmacen ubicacion) {
        if (ubicacion != null) {
            this.ubicacionesAsignadas.add(ubicacion);
        }
    }

    /**
     * Obtener la celda fisica correspondiente a un indice logico del lote.
     *
     * @param indiceLogico posicion logica dentro del lote.
     * @return ubicacion fisica asignada o null si el indice esta fuera de rango.
     */
    public UbicacionAlmacen obtenerUbicacion(int indiceLogico) {
        if (indiceLogico >= 0 && indiceLogico < this.ubicacionesAsignadas.size()) {
            return this.ubicacionesAsignadas.get(indiceLogico);
        }
        return null;
    }

    /**
     * Obtener el identificador del lote.
     *
     * @return identificador de lote.
     */
    public String getIdLote() {
        return this.idLote;
    }

    /**
     * Obtener el identificador del producto.
     *
     * @return identificador del producto.
     */
    public String getIdProducto() {
        return this.idProducto;
    }

    /**
     * Obtener la cantidad de unidades registradas.
     *
     * @return cantidad de unidades.
     */
    public int getCantidadUnidades() {
        return this.cantidadUnidades;
    }

    /**
     * Obtener una copia de la lista de ubicaciones fisicas asignadas.
     *
     * @return lista de celdas fisicas asignadas.
     */
    public List<UbicacionAlmacen> getUbicacionesAsignadas() {
        List<UbicacionAlmacen> copia = new ArrayList<UbicacionAlmacen>();
        for (int i = 0; i < this.ubicacionesAsignadas.size(); i++) {
            copia.add(this.ubicacionesAsignadas.get(i));
        }
        return copia;
    }

    /**
     * Representacion descriptiva de la tabla de paginacion en texto.
     *
     * @return cadena con el detalle de ubicaciones asignadas.
     */
    @Override
    public String toString() {
        StringBuilder constructor = new StringBuilder();
        constructor.append("TablaPaginacion[lote=");
        constructor.append(this.idLote);
        constructor.append(", producto=");
        constructor.append(this.idProducto);
        constructor.append(", celdas=[");
        for (int i = 0; i < this.ubicacionesAsignadas.size(); i++) {
            constructor.append(this.ubicacionesAsignadas.get(i).getIdUbicacion());
            if (i < this.ubicacionesAsignadas.size() - 1) {
                constructor.append(", ");
            }
        }
        constructor.append("]]");
        return constructor.toString();
    }
}
