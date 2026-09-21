package com.redxela.concurrencia;

import com.redxela.config.ConstantesSimulacion;
import com.redxela.model.TipoRecurso;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// gestor centralizado de recursos compartidos mediante semaforos de conteo
public class GestorRecursos {

    // semaforo para controlar el acceso a las seis estaciones de empaque
    private final Semaphore semaforoEstacionesEmpaque;

    // semaforo para controlar el acceso a las dos areas de carga
    private final Semaphore semaforoAreasCarga;

    // semaforo para controlar los diez encargados de bodega
    private final Semaphore semaforoEncargadosBodega;

    // semaforo para controlar los tres montacargas disponibles
    private final Semaphore semaforoMontacargas;

    // semaforo para controlar las cuatro estaciones de control de calidad
    private final Semaphore semaforoEstacionesControlCalidad;

    // semaforo binario para el sistema de escaneo compartido
    private final Semaphore semaforoSistemaEscaneo;

    // capacidades totales registradas para cada recurso
    private final int totalEstacionesEmpaque;
    private final int totalAreasCarga;
    private final int totalEncargadosBodega;
    private final int totalMontacargas;
    private final int totalEstacionesControlCalidad;
    private final int totalSistemasEscaneo;

    /**
     * Constructor por defecto que inicializa los semaforos con las capacidades oficiales.
     */
    public GestorRecursos() {
        // asignar capacidades predeterminadas desde las constantes
        this.totalEstacionesEmpaque = ConstantesSimulacion.TOTAL_ESTACIONES_EMPAQUE;
        this.totalAreasCarga = ConstantesSimulacion.TOTAL_AREAS_CARGA;
        this.totalEncargadosBodega = ConstantesSimulacion.TOTAL_ENCARGADOS_BODEGA;
        this.totalMontacargas = ConstantesSimulacion.TOTAL_MONTACARGAS;
        this.totalEstacionesControlCalidad = ConstantesSimulacion.TOTAL_ESTACIONES_CONTROL_CALIDAD;
        this.totalSistemasEscaneo = ConstantesSimulacion.TOTAL_SISTEMAS_ESCANEO;

        // instanciar semaforos con politica de equidad activada para garantizar orden fifo
        this.semaforoEstacionesEmpaque = new Semaphore(this.totalEstacionesEmpaque, true);
        this.semaforoAreasCarga = new Semaphore(this.totalAreasCarga, true);
        this.semaforoEncargadosBodega = new Semaphore(this.totalEncargadosBodega, true);
        this.semaforoMontacargas = new Semaphore(this.totalMontacargas, true);
        this.semaforoEstacionesControlCalidad = new Semaphore(this.totalEstacionesControlCalidad, true);
        this.semaforoSistemaEscaneo = new Semaphore(this.totalSistemasEscaneo, true);
    }

    /**
     * Constructor sobrecargado para permitir parametrizar las capacidades iniciales.
     *
     * @param estacionesEmpaque cantidad de estaciones de empaque.
     * @param areasCarga cantidad de areas de carga.
     * @param encargadosBodega cantidad de encargados de bodega.
     * @param montacargas cantidad de montacargas.
     * @param estacionesCalidad cantidad de estaciones de calidad.
     * @param sistemasEscaneo cantidad de sistemas de escaneo.
     */
    public GestorRecursos(
            int estacionesEmpaque,
            int areasCarga,
            int encargadosBodega,
            int montacargas,
            int estacionesCalidad,
            int sistemasEscaneo
    ) {
        // asignar capacidades personalizadas recibidas por parametro
        this.totalEstacionesEmpaque = estacionesEmpaque;
        this.totalAreasCarga = areasCarga;
        this.totalEncargadosBodega = encargadosBodega;
        this.totalMontacargas = montacargas;
        this.totalEstacionesControlCalidad = estacionesCalidad;
        this.totalSistemasEscaneo = sistemasEscaneo;

        // crear semaforos con las capacidades especificadas
        this.semaforoEstacionesEmpaque = new Semaphore(estacionesEmpaque, true);
        this.semaforoAreasCarga = new Semaphore(areasCarga, true);
        this.semaforoEncargadosBodega = new Semaphore(encargadosBodega, true);
        this.semaforoMontacargas = new Semaphore(montacargas, true);
        this.semaforoEstacionesControlCalidad = new Semaphore(estacionesCalidad, true);
        this.semaforoSistemaEscaneo = new Semaphore(sistemasEscaneo, true);
    }

    // obtener la referencia al semaforo segun el tipo de recurso solicitado
    private Semaphore obtenerSemaforoPorTipo(TipoRecurso tipo) {
        if (tipo == TipoRecurso.ESTACION_EMPAQUE) {
            return this.semaforoEstacionesEmpaque;
        } else if (tipo == TipoRecurso.AREA_CARGA) {
            return this.semaforoAreasCarga;
        } else if (tipo == TipoRecurso.ENCARGADO_BODEGA) {
            return this.semaforoEncargadosBodega;
        } else if (tipo == TipoRecurso.MONTACARGAS) {
            return this.semaforoMontacargas;
        } else if (tipo == TipoRecurso.ESTACION_CONTROL_CALIDAD) {
            return this.semaforoEstacionesControlCalidad;
        } else if (tipo == TipoRecurso.SISTEMA_ESCANEO) {
            return this.semaforoSistemaEscaneo;
        }
        return null;
    }

    /**
     * Adquirir de forma bloqueante una cantidad especifica de permisos del recurso.
     *
     * @param tipo tipo de recurso requerido.
     * @param cantidad numero de permisos solicitados.
     * @return verdadero si la adquisicion fue exitosa o falso en interrupcion.
     */
    public boolean adquirirRecurso(TipoRecurso tipo, int cantidad) {
        Semaphore semaforo = this.obtenerSemaforoPorTipo(tipo);
        if (semaforo == null) {
            return false;
        }
        try {
            // solicitar los permisos al semaforo bloqueando el hilo si no hay cupo
            semaforo.acquire(cantidad);
            return true;
        } catch (InterruptedException excepcion) {
            // restaurar el estado de interrupcion del hilo
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al adquirir recurso " + tipo.getNombre() + ": " + excepcion.getMessage());
            return false;
        }
    }

    /**
     * Adquirir de forma bloqueante una sola unidad del recurso indicado.
     *
     * @param tipo tipo de recurso a solicitar.
     * @return verdadero si se obtuvo el recurso.
     */
    public boolean adquirirRecurso(TipoRecurso tipo) {
        // delegar al metodo general pasando una unidad
        return this.adquirirRecurso(tipo, 1);
    }

    /**
     * Intentar adquirir permisos de forma no bloqueante o con tiempo limite.
     *
     * @param tipo tipo de recurso.
     * @param cantidad numero de permisos.
     * @param tiempoEspera tiempo maximo de espera.
     * @param unidad unidad temporal de medida.
     * @return verdadero si se adquirio antes del tiempo limite.
     */
    public boolean intentarAdquirirRecurso(TipoRecurso tipo, int cantidad, long tiempoEspera, TimeUnit unidad) {
        Semaphore semaforo = this.obtenerSemaforoPorTipo(tipo);
        if (semaforo == null) {
            return false;
        }
        try {
            // intentar adquirir los permisos dentro del tiempo indicado
            boolean exito = semaforo.tryAcquire(cantidad, tiempoEspera, unidad);
            return exito;
        } catch (InterruptedException excepcion) {
            // procesar la interrupcion restableciendo la senal
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al intentar adquirir con tiempo " + tipo.getNombre() + ": " + excepcion.getMessage());
            return false;
        }
    }

    /**
     * Intentar adquirir inmediatamente una unidad del recurso sin bloquear.
     *
     * @param tipo tipo de recurso a intentar.
     * @return verdadero si estaba disponible de inmediato.
     */
    public boolean intentarAdquirirRecurso(TipoRecurso tipo) {
        Semaphore semaforo = this.obtenerSemaforoPorTipo(tipo);
        if (semaforo == null) {
            return false;
        }
        // intentar adquirir un permiso inmediatamente
        return semaforo.tryAcquire();
    }

    /**
     * Liberar una cantidad especifica de permisos al recurso correspondiente.
     *
     * @param tipo tipo de recurso a devolver.
     * @param cantidad numero de unidades a restituir.
     */
    public void liberarRecurso(TipoRecurso tipo, int cantidad) {
        Semaphore semaforo = this.obtenerSemaforoPorTipo(tipo);
        if (semaforo != null) {
            // devolver los permisos al semaforo despertando hilos en cola
            semaforo.release(cantidad);
        }
    }

    /**
     * Liberar una sola unidad del recurso indicado.
     *
     * @param tipo tipo de recurso a restituir.
     */
    public void liberarRecurso(TipoRecurso tipo) {
        // delegar al metodo de liberacion pasando una unidad
        this.liberarRecurso(tipo, 1);
    }

    /**
     * Adquirir secuencialmente todos los recursos contenidos en una lista.
     *
     * @param listaRecursos lista de recursos a adquirir en orden.
     * @return verdadero si todos los recursos fueron obtenidos.
     */
    public boolean adquirirRecursos(List<TipoRecurso> listaRecursos) {
        if (listaRecursos == null) {
            return true;
        }
        // recorrer la lista para solicitar cada recurso
        for (int i = 0; i < listaRecursos.size(); i++) {
            TipoRecurso recurso = listaRecursos.get(i);
            boolean obtenido = this.adquirirRecurso(recurso, 1);
            if (!obtenido) {
                // liberar en reversa los recursos previamente adquiridos para evitar fugas
                for (int j = i - 1; j >= 0; j--) {
                    this.liberarRecurso(listaRecursos.get(j), 1);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Liberar secuencialmente todos los recursos contenidos en una lista.
     *
     * @param listaRecursos lista de recursos a restituir.
     */
    public void liberarRecursos(List<TipoRecurso> listaRecursos) {
        if (listaRecursos == null) {
            return;
        }
        // recorrer la lista en orden inverso para liberar los recursos
        for (int i = listaRecursos.size() - 1; i >= 0; i--) {
            TipoRecurso recurso = listaRecursos.get(i);
            this.liberarRecurso(recurso, 1);
        }
    }

    /**
     * Consultar la cantidad de permisos actualmente disponibles de un recurso.
     *
     * @param tipo tipo de recurso a consultar.
     * @return cantidad de unidades libres.
     */
    public int consultarDisponibles(TipoRecurso tipo) {
        Semaphore semaforo = this.obtenerSemaforoPorTipo(tipo);
        if (semaforo == null) {
            return 0;
        }
        return semaforo.availablePermits();
    }

    /**
     * Consultar la capacidad total configurada para el recurso.
     *
     * @param tipo tipo de recurso.
     * @return capacidad maxima del recurso.
     */
    public int consultarTotal(TipoRecurso tipo) {
        if (tipo == TipoRecurso.ESTACION_EMPAQUE) {
            return this.totalEstacionesEmpaque;
        } else if (tipo == TipoRecurso.AREA_CARGA) {
            return this.totalAreasCarga;
        } else if (tipo == TipoRecurso.ENCARGADO_BODEGA) {
            return this.totalEncargadosBodega;
        } else if (tipo == TipoRecurso.MONTACARGAS) {
            return this.totalMontacargas;
        } else if (tipo == TipoRecurso.ESTACION_CONTROL_CALIDAD) {
            return this.totalEstacionesControlCalidad;
        } else if (tipo == TipoRecurso.SISTEMA_ESCANEO) {
            return this.totalSistemasEscaneo;
        }
        return 0;
    }

    /**
     * Consultar la cantidad de unidades en uso actual de un recurso.
     *
     * @param tipo tipo de recurso.
     * @return unidades ocupadas.
     */
    public int consultarEnUso(TipoRecurso tipo) {
        int total = this.consultarTotal(tipo);
        int disponibles = this.consultarDisponibles(tipo);
        int ocupados = total - disponibles;
        return ocupados;
    }

    /**
     * Adquirir una estacion de empaque.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirEstacionEmpaque() {
        return this.adquirirRecurso(TipoRecurso.ESTACION_EMPAQUE);
    }

    /**
     * Liberar una estacion de empaque.
     */
    public void liberarEstacionEmpaque() {
        this.liberarRecurso(TipoRecurso.ESTACION_EMPAQUE);
    }

    /**
     * Adquirir una area de carga.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirAreaCarga() {
        return this.adquirirRecurso(TipoRecurso.AREA_CARGA);
    }

    /**
     * Liberar una area de carga.
     */
    public void liberarAreaCarga() {
        this.liberarRecurso(TipoRecurso.AREA_CARGA);
    }

    /**
     * Adquirir un encargado de bodega.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirEncargadoBodega() {
        return this.adquirirRecurso(TipoRecurso.ENCARGADO_BODEGA);
    }

    /**
     * Liberar un encargado de bodega.
     */
    public void liberarEncargadoBodega() {
        this.liberarRecurso(TipoRecurso.ENCARGADO_BODEGA);
    }

    /**
     * Adquirir un montacargas.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirMontacargas() {
        return this.adquirirRecurso(TipoRecurso.MONTACARGAS);
    }

    /**
     * Liberar un montacargas.
     */
    public void liberarMontacargas() {
        this.liberarRecurso(TipoRecurso.MONTACARGAS);
    }

    /**
     * Adquirir una estacion de control de calidad.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirControlCalidad() {
        return this.adquirirRecurso(TipoRecurso.ESTACION_CONTROL_CALIDAD);
    }

    /**
     * Liberar una estacion de control de calidad.
     */
    public void liberarControlCalidad() {
        this.liberarRecurso(TipoRecurso.ESTACION_CONTROL_CALIDAD);
    }

    /**
     * Adquirir el sistema de escaneo compartido.
     *
     * @return verdadero si se adquirio con exito.
     */
    public boolean adquirirSistemaEscaneo() {
        return this.adquirirRecurso(TipoRecurso.SISTEMA_ESCANEO);
    }

    /**
     * Liberar el sistema de escaneo compartido.
     */
    public void liberarSistemaEscaneo() {
        this.liberarRecurso(TipoRecurso.SISTEMA_ESCANEO);
    }

    /**
     * Imprimir en consola el estado actual de ocupacion de todos los recursos.
     */
    public void imprimirEstadoRecursos() {
        System.out.println("================================================================");
        System.out.println("ESTADO ACTUAL DE RECURSOS EN EL CENTRO DE DISTRIBUCION REDXELA");
        System.out.println("================================================================");

        TipoRecurso[] recursos = new TipoRecurso[]{
                TipoRecurso.ESTACION_EMPAQUE,
                TipoRecurso.AREA_CARGA,
                TipoRecurso.ENCARGADO_BODEGA,
                TipoRecurso.MONTACARGAS,
                TipoRecurso.ESTACION_CONTROL_CALIDAD,
                TipoRecurso.SISTEMA_ESCANEO
        };

        for (int i = 0; i < recursos.length; i++) {
            TipoRecurso recurso = recursos[i];
            int total = this.consultarTotal(recurso);
            int disponibles = this.consultarDisponibles(recurso);
            int ocupados = this.consultarEnUso(recurso);

            StringBuilder linea = new StringBuilder();
            linea.append(recurso.getNombre());
            linea.append(": ");
            linea.append(disponibles);
            linea.append(" disponibles / ");
            linea.append(ocupados);
            linea.append(" en uso (total: ");
            linea.append(total);
            linea.append(")");
            System.out.println(linea.toString());
        }
        System.out.println("================================================================");
    }
}
