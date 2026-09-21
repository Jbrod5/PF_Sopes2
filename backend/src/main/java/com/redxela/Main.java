package com.redxela;

import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.ProcesoSimulacionPedido;
import com.redxela.model.Cliente;
import com.redxela.model.ItemPedido;
import com.redxela.model.NivelServicio;
import com.redxela.model.Pedido;
import com.redxela.model.Producto;
import com.redxela.model.TipoMercancia;
import com.redxela.model.TipoRecurso;
import com.redxela.util.ConsolaFormato;

// clase principal de prueba para verificar asignacion y liberacion concurrente de recursos
public class Main {

    /**
     * Punto de entrada principal para la ejecucion de la prueba del modulo 1.
     *
     * @param args argumentos de la linea de comandos.
     */
    public static void main(String[] args) {
        // instanciar formateador para mensajes limpios en consola
        ConsolaFormato formato = new ConsolaFormato();
        formato.imprimirEncabezado("SIMULACION DE CENTRO DE DISTRIBUCION REDXELA - MODULO 1");

        // instanciar el gestor central de recursos compartidos
        formato.imprimirInfo("Inicializando gestor central de recursos con semaforos");
        GestorRecursos gestorRecursos = new GestorRecursos();

        // imprimir disponibilidad inicial de recursos
        gestorRecursos.imprimirEstadoRecursos();

        // crear catalogo de clientes de prueba usando constructores sobrecargados
        formato.imprimirInfo("Creando clientes para la prueba");
        Cliente cliente1 = new Cliente("CLI-001", "Comercializadora Los Altos", "contacto@losaltos.com");
        Cliente cliente2 = new Cliente("CLI-002", "Farmacia La Union", "pedidos@launion.com");
        Cliente cliente3 = new Cliente("CLI-003", "Distribuidora Central", "ventas@central.com");

        // crear catalogo de productos para cada uno de los cinco tipos de mercancia
        formato.imprimirInfo("Creando catalogo de productos representativos");
        Producto prodEstandar = new Producto("PRD-01", "Cajas de abarrotes basicos", TipoMercancia.ESTANDAR, 5.0, 45.0);
        Producto prodFragil = new Producto("PRD-02", "Juegos de cristaleria fina", TipoMercancia.FRAGIL, 2.5, 350.0);
        Producto prodPesado = new Producto("PRD-03", "Motores y repuestos industriales", TipoMercancia.PESADA, 180.0, 2500.0);
        Producto prodValioso = new Producto("PRD-04", "Lote de telefonos inteligentes", TipoMercancia.VALIOSA, 1.2, 5000.0);
        Producto prodRefrigerado = new Producto("PRD-05", "Contenedores de medicamentos termolabiles", TipoMercancia.REFRIGERADA, 8.0, 1800.0);

        // instanciar pedidos de prueba con diferentes niveles de servicio
        formato.imprimirInfo("Construyendo pedidos de prueba");
        Pedido pedido1 = new Pedido("PED-101", cliente1, NivelServicio.EXPRES);
        pedido1.agregarItem(new ItemPedido(prodFragil, 3));

        Pedido pedido2 = new Pedido("PED-102", cliente2, NivelServicio.PRIORITARIO);
        pedido2.agregarItem(new ItemPedido(prodPesado, 2));

        Pedido pedido3 = new Pedido("PED-103", cliente3, NivelServicio.ESTANDAR);
        pedido3.agregarItem(new ItemPedido(prodValioso, 1));

        Pedido pedido4 = new Pedido("PED-104", cliente1, NivelServicio.PROGRAMADO);
        pedido4.agregarItem(new ItemPedido(prodRefrigerado, 4));

        Pedido pedido5 = new Pedido("PED-105", cliente2, NivelServicio.ECONOMICO);
        pedido5.agregarItem(new ItemPedido(prodEstandar, 6));

        Pedido pedido6 = new Pedido("PED-106", cliente3, NivelServicio.EXPRES);
        pedido6.agregarItem(new ItemPedido(prodValioso, 2));

        Pedido pedido7 = new Pedido("PED-107", cliente1, NivelServicio.PRIORITARIO);
        pedido7.agregarItem(new ItemPedido(prodFragil, 1));

        // agrupar pedidos en un arreglo tradicional
        Pedido[] listaPedidos = new Pedido[]{
                pedido1, pedido2, pedido3, pedido4, pedido5, pedido6, pedido7
        };

        // preparar arreglo de hilos para ejecucion concurrente
        Thread[] hilos = new Thread[listaPedidos.length];
        formato.imprimirInfo("Lanzando " + listaPedidos.length + " hilos concurrentes compitiendo por recursos");

        // iniciar los hilos de procesamiento
        for (int i = 0; i < listaPedidos.length; i++) {
            Pedido actual = listaPedidos[i];
            ProcesoSimulacionPedido tarea = new ProcesoSimulacionPedido(actual, gestorRecursos, 600L);
            Thread hilo = new Thread(tarea, "Hilo-" + actual.getId());
            hilos[i] = hilo;
            hilo.start();
        }

        // esperar a que todos los hilos terminen su procesamiento
        formato.imprimirInfo("Esperando finalizacion de todos los hilos");
        for (int i = 0; i < hilos.length; i++) {
            try {
                hilos[i].join();
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupcion esperando hilo " + hilos[i].getName() + ": " + excepcion.getMessage());
            }
        }

        // verificar y mostrar el estado final de los recursos
        formato.imprimirSeparador();
        formato.imprimirInfo("Todos los hilos han terminado su ciclo de ejecucion");
        gestorRecursos.imprimirEstadoRecursos();

        // comprobar que ningun semaforo haya quedado con fugas de permisos
        boolean todosRestaurados = true;
        TipoRecurso[] tiposRecurso = new TipoRecurso[]{
                TipoRecurso.ESTACION_EMPAQUE,
                TipoRecurso.AREA_CARGA,
                TipoRecurso.ENCARGADO_BODEGA,
                TipoRecurso.MONTACARGAS,
                TipoRecurso.ESTACION_CONTROL_CALIDAD,
                TipoRecurso.SISTEMA_ESCANEO
        };

        for (int i = 0; i < tiposRecurso.length; i++) {
            TipoRecurso tipo = tiposRecurso[i];
            int disponibles = gestorRecursos.consultarDisponibles(tipo);
            int total = gestorRecursos.consultarTotal(tipo);
            if (disponibles != total) {
                todosRestaurados = false;
                System.err.println("Alerta: El recurso " + tipo.getNombre() + " no regreso a su capacidad total");
            }
        }

        if (todosRestaurados) {
            formato.imprimirInfo("Prueba superada: Todos los semaforos regresaron a su capacidad maxima sin fugas");
        } else {
            formato.imprimirInfo("Error: Se detectaron recursos no liberados adecuadamente");
        }

        formato.imprimirEncabezado("FIN DE LA EJECUCION DE PRUEBA");
    }
}
