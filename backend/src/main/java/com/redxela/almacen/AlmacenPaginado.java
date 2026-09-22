package com.redxela.almacen;

import com.redxela.model.LoteMercancia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// gestor de almacenamiento fisico que implementa asignacion no contigua mediante paginacion
public class AlmacenPaginado {

    // dimensiones fisicas del almacen
    private final int totalPasillos;
    private final int totalNiveles;
    private final int totalEspaciosPorNivel;

    // matriz tridimensional con las celdas fisicas de almacenamiento
    private final UbicacionAlmacen[][][] celdas;

    // mapa que asocia el identificador de cada lote con su tabla de paginacion
    private final Map<String, TablaPaginacionLote> tablasPaginacion;

    // cerrojo para garantizar exclusion mutua en la asignacion y liberacion de celdas
    private final ReentrantLock cerrojoAlmacen;

    /**
     * Constructor por defecto con dimensiones predeterminadas de 4 pasillos 3 niveles y 8 casillas.
     */
    public AlmacenPaginado() {
        this.totalPasillos = 4;
        this.totalNiveles = 3;
        this.totalEspaciosPorNivel = 8;
        this.cerrojoAlmacen = new ReentrantLock(true);
        this.tablasPaginacion = new HashMap<String, TablaPaginacionLote>();

        // inicializar la matriz de celdas fisicas
        this.celdas = new UbicacionAlmacen[this.totalPasillos][this.totalNiveles][this.totalEspaciosPorNivel];
        this.inicializarCeldas();
    }

    /**
     * Constructor sobrecargado con dimensiones personalizadas del centro de almacenamiento.
     *
     * @param totalPasillos cantidad de pasillos.
     * @param totalNiveles cantidad de estantes o niveles verticales.
     * @param totalEspaciosPorNivel cantidad de espacios o casillas por nivel.
     */
    public AlmacenPaginado(int totalPasillos, int totalNiveles, int totalEspaciosPorNivel) {
        this.totalPasillos = totalPasillos;
        this.totalNiveles = totalNiveles;
        this.totalEspaciosPorNivel = totalEspaciosPorNivel;
        this.cerrojoAlmacen = new ReentrantLock(true);
        this.tablasPaginacion = new HashMap<String, TablaPaginacionLote>();

        this.celdas = new UbicacionAlmacen[totalPasillos][totalNiveles][totalEspaciosPorNivel];
        this.inicializarCeldas();
    }

    // instanciar cada celda fisica del almacen con sus coordenadas
    private void inicializarCeldas() {
        for (int p = 0; p < this.totalPasillos; p++) {
            for (int n = 0; n < this.totalNiveles; n++) {
                for (int e = 0; e < this.totalEspaciosPorNivel; e++) {
                    this.celdas[p][n][e] = new UbicacionAlmacen(p + 1, n + 1, e + 1);
                }
            }
        }
    }

    /**
     * Asignar de forma no contigua las celdas necesarias para un lote entrante.
     *
     * @param lote lote de mercancia a almacenar.
     * @return verdadero si se asigno exitosamente o falso si no habia espacio suficiente.
     */
    public boolean asignarEspacioNoContiguo(LoteMercancia lote) {
        if (lote == null) {
            return false;
        }

        int cantidadRequerida = lote.getCantidad();
        if (cantidadRequerida <= 0) {
            return false;
        }

        this.cerrojoAlmacen.lock();
        try {
            // verificar si hay suficiente espacio disponible en total
            int disponibles = this.contarEspaciosDisponiblesSinBloqueo();
            if (disponibles < cantidadRequerida) {
                System.err.println("[ALMACEN] Espacio insuficiente para lote " + lote.getIdLote() + " (requiere " + cantidadRequerida + ", disponibles " + disponibles + ")");
                return false;
            }

            // crear la tabla de paginacion para el nuevo lote
            TablaPaginacionLote tabla = new TablaPaginacionLote(
                    lote.getIdLote(),
                    lote.getProducto().getId(),
                    cantidadRequerida
            );

            int asignados = 0;

            // buscar celdas libres dispersas en cualquier pasillo nivel y espacio
            for (int p = 0; p < this.totalPasillos; p++) {
                for (int n = 0; n < this.totalNiveles; n++) {
                    for (int e = 0; e < this.totalEspaciosPorNivel; e++) {
                        UbicacionAlmacen celdaActual = this.celdas[p][n][e];
                        if (!celdaActual.isOcupada()) {
                            // ocupar la celda con el lote y producto
                            celdaActual.asignarLote(lote.getIdLote(), lote.getProducto().getId());

                            // registrar la entrada en la tabla de paginacion
                            tabla.agregarEntrada(celdaActual);

                            asignados = asignados + 1;

                            // si completamos la cantidad requerida finalizar la busqueda
                            if (asignados == cantidadRequerida) {
                                break;
                            }
                        }
                    }
                    if (asignados == cantidadRequerida) {
                        break;
                    }
                }
                if (asignados == cantidadRequerida) {
                    break;
                }
            }

            // guardar la tabla de paginacion en el registro central
            this.tablasPaginacion.put(lote.getIdLote(), tabla);

            System.out.println("[ALMACEN] Lote " + lote.getIdLote() + " asignado en " + cantidadRequerida + " celdas no contiguas: " + tabla.toString());
            return true;

        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }

    /**
     * Liberar todas las celdas fisicas asignadas a un lote determinado.
     *
     * @param idLote identificador del lote a desocupar.
     * @return verdadero si el lote fue encontrado y liberado o falso si no existia.
     */
    public boolean liberarEspacioLote(String idLote) {
        if (idLote == null) {
            return false;
        }

        this.cerrojoAlmacen.lock();
        try {
            TablaPaginacionLote tabla = this.tablasPaginacion.get(idLote);
            if (tabla == null) {
                return false;
            }

            // recorrer todas las celdas fisicas registradas en la tabla de paginacion
            List<UbicacionAlmacen> celdasAsignadas = tabla.getUbicacionesAsignadas();
            for (int i = 0; i < celdasAsignadas.size(); i++) {
                UbicacionAlmacen celda = celdasAsignadas.get(i);
                celda.liberar();
            }

            // remover la tabla del registro activo
            this.tablasPaginacion.remove(idLote);

            System.out.println("[ALMACEN] Lote " + idLote + " desocupado liberando " + celdasAsignadas.size() + " celdas fisicas");
            return true;

        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }

    /**
     * Consultar la tabla de paginacion asociada a un lote especifico.
     *
     * @param idLote identificador del lote a buscar.
     * @return tabla de paginacion del lote o null si no esta registrado.
     */
    public TablaPaginacionLote consultarTablaLote(String idLote) {
        this.cerrojoAlmacen.lock();
        try {
            return this.tablasPaginacion.get(idLote);
        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }

    // contar espacios disponibles de forma interna asumiendo cerrojo adquirido
    private int contarEspaciosDisponiblesSinBloqueo() {
        int contador = 0;
        for (int p = 0; p < this.totalPasillos; p++) {
            for (int n = 0; n < this.totalNiveles; n++) {
                for (int e = 0; e < this.totalEspaciosPorNivel; e++) {
                    if (!this.celdas[p][n][e].isOcupada()) {
                        contador = contador + 1;
                    }
                }
            }
        }
        return contador;
    }

    /**
     * Consultar la cantidad total de celdas de almacenamiento del centro.
     *
     * @return capacidad total.
     */
    public int getCapacidadTotal() {
        return this.totalPasillos * this.totalNiveles * this.totalEspaciosPorNivel;
    }

    /**
     * Consultar la cantidad de celdas actualmente disponibles.
     *
     * @return cantidad de espacios libres.
     */
    public int getEspaciosDisponibles() {
        this.cerrojoAlmacen.lock();
        try {
            return this.contarEspaciosDisponiblesSinBloqueo();
        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }

    /**
     * Consultar la cantidad de celdas actualmente ocupadas.
     *
     * @return cantidad de espacios ocupados.
     */
    public int getEspaciosOcupados() {
        this.cerrojoAlmacen.lock();
        try {
            int total = this.getCapacidadTotal();
            int disponibles = this.contarEspaciosDisponiblesSinBloqueo();
            return total - disponibles;
        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }

    /**
     * Imprimir en consola el mapa visual de ocupacion del almacen.
     */
    public void imprimirMapaOcupacion() {
        this.cerrojoAlmacen.lock();
        try {
            System.out.println("================================================================");
            System.out.println("MAPA DE ASIGNACION NO CONTIGUA DEL ALMACEN (MEMORIA PAGINADA)");
            System.out.println("================================================================");

            for (int p = 0; p < this.totalPasillos; p++) {
                System.out.println("--- Pasillo " + (p + 1) + " ---");
                for (int n = this.totalNiveles - 1; n >= 0; n--) {
                    StringBuilder fila = new StringBuilder();
                    fila.append("Nivel ").append(n + 1).append(": ");
                    for (int e = 0; e < this.totalEspaciosPorNivel; e++) {
                        UbicacionAlmacen u = this.celdas[p][n][e];
                        if (u.isOcupada()) {
                            fila.append("[").append(u.getIdLoteAsignado()).append("] ");
                        } else {
                            fila.append("[LIBRE] ");
                        }
                    }
                    System.out.println(fila.toString());
                }
            }
            System.out.println("================================================================");
            System.out.println("Ocupadas: " + this.getEspaciosOcupados() + " / Libres: " + this.getEspaciosDisponibles() + " (Total: " + this.getCapacidadTotal() + ")");
            System.out.println("================================================================");
        } finally {
            this.cerrojoAlmacen.unlock();
        }
    }
}
