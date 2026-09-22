package com.redxela.service;

// enumeracion que define las politicas de planificacion disponibles para ordenar pedidos
public enum PoliticaPlanificacion {

    NIVEL_SERVICIO_ESTRICTO(
            "Nivel de servicio estricto",
            "Prioriza estrictamente segun el nivel de servicio del cliente dando paso primero a expres y prioritario"
    ),
    MENOR_CANTIDAD_ARTICULOS(
            "Menor cantidad de articulos",
            "Prioriza pedidos con menor cantidad de unidades para desocupar estaciones y recursos rapidamente"
    ),
    TIEMPO_ESPERA_FIFO(
            "Tiempo de espera FIFO",
            "Atiende en estricto orden de llegada dando prioridad a los pedidos con mayor tiempo en cola"
    );

    // nombre descriptivo de la politica
    private final String nombre;

    // descripcion detallada del criterio de atencion
    private final String descripcion;

    // constructor del enum
    PoliticaPlanificacion(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /**
     * Obtener el nombre descriptivo de la politica de planificacion.
     *
     * @return nombre de la politica.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Obtener la descripcion del criterio de planificacion.
     *
     * @return texto explicativo del criterio.
     */
    public String getDescripcion() {
        return this.descripcion;
    }
}
