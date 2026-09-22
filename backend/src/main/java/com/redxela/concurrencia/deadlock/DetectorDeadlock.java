package com.redxela.concurrencia.deadlock;

import com.redxela.model.TipoRecurso;

import java.util.ArrayList;
import java.util.List;

// detector de interbloqueos que analiza el grafo de espera circular de recursos
public class DetectorDeadlock {

    // referencia al registro de asignacion y espera
    private final RegistroAsignacionRecursos registro;

    /**
     * Constructor sobrecargado que recibe el registro de asignacion.
     *
     * @param registro registro central de asignacion de recursos.
     */
    public DetectorDeadlock(RegistroAsignacionRecursos registro) {
        this.registro = registro;
    }

    /**
     * Detectar si existe un interbloqueo activo entre pedidos analizando el grafo de espera.
     *
     * @return informacion del conflicto detectado o null si no hay interbloqueo.
     */
    public InformacionConflicto detectarInterbloqueo() {
        List<String> pedidosEnEspera = this.registro.obtenerPedidosEnEspera();

        // iterar sobre todos los pares posibles de pedidos en espera buscando ciclos
        for (int i = 0; i < pedidosEnEspera.size(); i++) {
            String p1 = pedidosEnEspera.get(i);
            TipoRecurso recursoDeseadoPorP1 = this.registro.obtenerRecursoEsperado(p1);
            if (recursoDeseadoPorP1 == null) {
                continue;
            }

            for (int j = 0; j < pedidosEnEspera.size(); j++) {
                if (i == j) {
                    continue;
                }

                String p2 = pedidosEnEspera.get(j);
                TipoRecurso recursoDeseadoPorP2 = this.registro.obtenerRecursoEsperado(p2);
                if (recursoDeseadoPorP2 == null) {
                    continue;
                }

                // verificar si p2 retiene el recurso que p1 desea
                List<TipoRecurso> retenidosPorP2 = this.registro.obtenerRecursosRetenidos(p2);
                boolean p2TieneRecursoDeP1 = retenidosPorP2.contains(recursoDeseadoPorP1);

                // verificar si p1 retiene el recurso que p2 desea
                List<TipoRecurso> retenidosPorP1 = this.registro.obtenerRecursosRetenidos(p1);
                boolean p1TieneRecursoDeP2 = retenidosPorP1.contains(recursoDeseadoPorP2);

                // si ambas condiciones se cumplen se ha cerrado un ciclo de espera circular
                if (p2TieneRecursoDeP1 && p1TieneRecursoDeP2) {
                    InformacionConflicto conflicto = new InformacionConflicto(
                            p1,
                            recursoDeseadoPorP2,
                            recursoDeseadoPorP1,
                            p2,
                            recursoDeseadoPorP1,
                            recursoDeseadoPorP2
                    );
                    return conflicto;
                }
            }
        }

        return null;
    }

    /**
     * Consultar todos los conflictos de espera circular detectados en el sistema.
     *
     * @return lista de informaciones de conflicto activas.
     */
    public List<InformacionConflicto> detectarTodosLosConflictos() {
        List<InformacionConflicto> lista = new ArrayList<InformacionConflicto>();
        InformacionConflicto conflicto = this.detectarInterbloqueo();
        if (conflicto != null) {
            lista.add(conflicto);
        }
        return lista;
    }

    /**
     * Obtener la referencia al registro de asignacion.
     *
     * @return registro de asignacion de recursos.
     */
    public RegistroAsignacionRecursos getRegistro() {
        return this.registro;
    }
}
