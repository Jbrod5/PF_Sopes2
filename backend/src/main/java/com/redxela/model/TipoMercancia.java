package com.redxela.model;

import java.util.ArrayList;
import java.util.List;

// enumeracion que define los cinco tipos de mercancia y sus recursos requeridos
public enum TipoMercancia {

    ESTANDAR(
            "Estandar",
            "Mercancia general sin requerimientos especiales de manipulacion",
            new TipoRecurso[]{TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_EMPAQUE}
    ),
    FRAGIL(
            "Fragil",
            "Articulos delicados de vidrio o ceramica que requieren control de calidad y empaque",
            new TipoRecurso[]{TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_CONTROL_CALIDAD, TipoRecurso.ESTACION_EMPAQUE}
    ),
    PESADA(
            "Pesada",
            "Cargas de gran volumen o peso que requieren montacargas y area de carga",
            new TipoRecurso[]{TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.MONTACARGAS, TipoRecurso.AREA_CARGA}
    ),
    VALIOSA(
            "Valiosa",
            "Mercancia de alto valor que requiere escaneo control de calidad y empaque",
            new TipoRecurso[]{TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.SISTEMA_ESCANEO, TipoRecurso.ESTACION_CONTROL_CALIDAD, TipoRecurso.ESTACION_EMPAQUE}
    ),
    REFRIGERADA(
            "Refrigerada",
            "Productos perecederos que demandan escaneo montacargas y despacho rapido en area de carga",
            new TipoRecurso[]{TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.MONTACARGAS, TipoRecurso.AREA_CARGA, TipoRecurso.SISTEMA_ESCANEO}
    );

    // nombre descriptivo de la mercancia
    private final String nombre;

    // descripcion de las caracteristicas de manipulacion
    private final String descripcion;

    // arreglo con los recursos requeridos para procesar esta mercancia
    private final TipoRecurso[] recursosRequeridos;

    // constructor del enum para inicializar los atributos
    TipoMercancia(String nombre, String descripcion, TipoRecurso[] recursosRequeridos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.recursosRequeridos = recursosRequeridos;
    }

    /**
     * Obtener el nombre del tipo de mercancia.
     *
     * @return nombre asignado al tipo.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Obtener la descripcion de las caracteristicas de la mercancia.
     *
     * @return texto descriptivo.
     */
    public String getDescripcion() {
        return this.descripcion;
    }

    /**
     * Obtener la lista de recursos necesarios para el procesamiento.
     *
     * @return lista de tipos de recurso requeridos.
     */
    public List<TipoRecurso> getRecursosRequeridos() {
        // crear lista tradicional para evitar metodos de fabrica estaticos
        List<TipoRecurso> lista = new ArrayList<TipoRecurso>();
        // recorrer el arreglo de recursos requeridos
        for (int i = 0; i < this.recursosRequeridos.length; i++) {
            // agregar cada recurso a la lista resultante
            lista.add(this.recursosRequeridos[i]);
        }
        return lista;
    }

    /**
     * Obtener el arreglo original de recursos requeridos.
     *
     * @return arreglo de tipos de recurso.
     */
    public TipoRecurso[] getArregloRecursosRequeridos() {
        return this.recursosRequeridos;
    }
}
