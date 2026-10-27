package com.redxela.util;

// clase de utilidad para impresion estructurada en la consola
public class ConsolaFormato {

    // registro opcional de eventos para la bitacora visible
    private RegistroEventos registroEventos;

    /**
     * Constructor por defecto de la clase de utilidad.
     */
    public ConsolaFormato() {
        // constructor vacio para instanciacion tradicional
        this.registroEventos = null;
    }

    /**
     * Asignar el registro donde tambien se anotan los mensajes.
     *
     * @param registroEventos registro concurrente de eventos.
     */
    public void setRegistroEventos(RegistroEventos registroEventos) {
        this.registroEventos = registroEventos;
    }

    /**
     * Imprimir una linea separadora decorativa en consola.
     */
    public void imprimirSeparador() {
        System.out.println("----------------------------------------------------------------");
        this.anotarEvento("----------------------------------------------------------------");
    }

    /**
     * Imprimir un encabezado con formato visual destacado.
     *
     * @param titulo texto del encabezado.
     */
    public void imprimirEncabezado(String titulo) {
        System.out.println("================================================================");
        System.out.println(titulo);
        System.out.println("================================================================");
        this.anotarEvento("==== " + titulo + " ====");
    }

    /**
     * Imprimir un mensaje informativo con prefijo estandar.
     *
     * @param mensaje texto a mostrar en consola.
     */
    public void imprimirInfo(String mensaje) {
        System.out.println("[INFO] " + mensaje);
        this.anotarEvento("[INFO] " + mensaje);
    }

    // anotar el mensaje en el registro cuando existe uno asignado
    private void anotarEvento(String evento) {
        if (this.registroEventos != null) {
            this.registroEventos.anotar(evento);
        }
    }
}
