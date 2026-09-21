package com.redxela.model;

// enumeracion para representar los niveles de servicio y prioridades de atencion
public enum NivelServicio {

    EXPRES(1, "Expres", "Procesamiento casi instantaneo", 0),
    PRIORITARIO(2, "Prioritario", "Maximo 10 minutos en espera", 10),
    ESTANDAR(3, "Estandar", "Maximo 30 minutos en espera", 30),
    PROGRAMADO(4, "Programado", "Maximo 60 minutos en espera", 60),
    ECONOMICO(5, "Economico", "Maximo 120 minutos en espera", 120);

    // prioridad numerica donde 1 es la prioridad mas alta
    private final int prioridad;

    // nombre descriptivo del nivel de servicio
    private final String nombre;

    // descripcion del tiempo objetivo de procesamiento
    private final String tiempoObjetivoDescripcion;

    // tiempo maximo de espera en minutos
    private final int tiempoMaximoEsperaMinutos;

    // constructor del enum para inicializar propiedades
    NivelServicio(int prioridad, String nombre, String tiempoObjetivoDescripcion, int tiempoMaximoEsperaMinutos) {
        this.prioridad = prioridad;
        this.nombre = nombre;
        this.tiempoObjetivoDescripcion = tiempoObjetivoDescripcion;
        this.tiempoMaximoEsperaMinutos = tiempoMaximoEsperaMinutos;
    }

    /**
     * Obtener el valor numerico de prioridad del servicio.
     *
     * @return entero que representa la prioridad.
     */
    public int getPrioridad() {
        return this.prioridad;
    }

    /**
     * Obtener el nombre del nivel de servicio.
     *
     * @return nombre del nivel.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Obtener el texto descriptivo del tiempo objetivo de procesamiento.
     *
     * @return descripcion del objetivo temporal.
     */
    public String getTiempoObjetivoDescripcion() {
        return this.tiempoObjetivoDescripcion;
    }

    /**
     * Obtener los minutos maximos tolerados de espera.
     *
     * @return cantidad de minutos limite.
     */
    public int getTiempoMaximoEsperaMinutos() {
        return this.tiempoMaximoEsperaMinutos;
    }
}
