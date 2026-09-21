package com.redxela.util;

// clase de utilidad para impresion estructurada en la consola
public class ConsolaFormato {

    /**
     * Constructor por defecto de la clase de utilidad.
     */
    public ConsolaFormato() {
        // constructor vacio para instanciacion tradicional
    }

    /**
     * Imprimir una linea separadora decorativa en consola.
     */
    public void imprimirSeparador() {
        System.out.println("----------------------------------------------------------------");
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
    }

    /**
     * Imprimir un mensaje informativo con prefijo estandar.
     *
     * @param mensaje texto a mostrar en consola.
     */
    public void imprimirInfo(String mensaje) {
        System.out.println("[INFO] " + mensaje);
    }
}
