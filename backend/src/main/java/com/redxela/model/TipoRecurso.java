package com.redxela.model;

import com.redxela.config.ConstantesSimulacion;

// enumeracion que define los tipos de recursos compartidos del centro
public enum TipoRecurso {

    ESTACION_EMPAQUE("Estacion de empaque", ConstantesSimulacion.TOTAL_ESTACIONES_EMPAQUE),
    AREA_CARGA("Area de carga", ConstantesSimulacion.TOTAL_AREAS_CARGA),
    ENCARGADO_BODEGA("Encargado de bodega", ConstantesSimulacion.TOTAL_ENCARGADOS_BODEGA),
    MONTACARGAS("Montacargas", ConstantesSimulacion.TOTAL_MONTACARGAS),
    ESTACION_CONTROL_CALIDAD("Estacion de control de calidad", ConstantesSimulacion.TOTAL_ESTACIONES_CONTROL_CALIDAD),
    SISTEMA_ESCANEO("Sistema de escaneo compartido", ConstantesSimulacion.TOTAL_SISTEMAS_ESCANEO);

    // nombre descriptivo del recurso
    private final String nombre;

    // capacidad maxima total configurada
    private final int capacidadTotal;

    // constructor del enum para inicializar atributos
    TipoRecurso(String nombre, int capacidadTotal) {
        this.nombre = nombre;
        this.capacidadTotal = capacidadTotal;
    }

    /**
     * Obtener el nombre descriptivo del recurso.
     *
     * @return nombre del recurso.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Obtener la capacidad total asignada al recurso.
     *
     * @return cantidad maxima de unidades.
     */
    public int getCapacidadTotal() {
        return this.capacidadTotal;
    }
}
