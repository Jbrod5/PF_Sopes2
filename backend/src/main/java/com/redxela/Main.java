// package com.redxela;
// 
// import com.redxela.concurrencia.GestorRecursos;
// import com.redxela.concurrencia.ProcesoSimulacionPedido;
// import com.redxela.model.Cliente;
// import com.redxela.model.ItemPedido;
// import com.redxela.model.NivelServicio;
// import com.redxela.model.Pedido;
// import com.redxela.model.Producto;
// import com.redxela.model.TipoMercancia;
// import com.redxela.model.TipoRecurso;
// import com.redxela.util.ConsolaFormato;
// 
// public class Main {
//     public static void main(String[] args) {
//         ConsolaFormato formato = new ConsolaFormato();
//         formato.imprimirEncabezado("SIMULACION DE CENTRO DE DISTRIBUCION REDXELA - MODULO 1");
//         GestorRecursos gestorRecursos = new GestorRecursos();
//         gestorRecursos.imprimirEstadoRecursos();
//         Cliente cliente1 = new Cliente("CLI-001", "Comercializadora Los Altos", "contacto@losaltos.com");
//         Cliente cliente2 = new Cliente("CLI-002", "Farmacia La Union", "pedidos@launion.com");
//         Cliente cliente3 = new Cliente("CLI-003", "Distribuidora Central", "ventas@central.com");
//         Producto prodEstandar = new Producto("PRD-01", "Cajas de abarrotes basicos", TipoMercancia.ESTANDAR, 5.0, 45.0);
//         Producto prodFragil = new Producto("PRD-02", "Juegos de cristaleria fina", TipoMercancia.FRAGIL, 2.5, 350.0);
//         Producto prodPesado = new Producto("PRD-03", "Motores y repuestos industriales", TipoMercancia.PESADA, 180.0, 2500.0);
//         Producto prodValioso = new Producto("PRD-04", "Lote de telefonos inteligentes", TipoMercancia.VALIOSA, 1.2, 5000.0);
//         Producto prodRefrigerado = new Producto("PRD-05", "Contenedores de medicamentos termolabiles", TipoMercancia.REFRIGERADA, 8.0, 1800.0);
//         Pedido pedido1 = new Pedido("PED-101", cliente1, NivelServicio.EXPRES);
//         pedido1.agregarItem(new ItemPedido(prodFragil, 3));
//         Pedido pedido2 = new Pedido("PED-102", cliente2, NivelServicio.PRIORITARIO);
//         pedido2.agregarItem(new ItemPedido(prodPesado, 2));
//         Pedido pedido3 = new Pedido("PED-103", cliente3, NivelServicio.ESTANDAR);
//         pedido3.agregarItem(new ItemPedido(prodValioso, 1));
//         Pedido pedido4 = new Pedido("PED-104", cliente1, NivelServicio.PROGRAMADO);
//         pedido4.agregarItem(new ItemPedido(prodRefrigerado, 4));
//         Pedido pedido5 = new Pedido("PED-105", cliente2, NivelServicio.ECONOMICO);
//         pedido5.agregarItem(new ItemPedido(prodEstandar, 6));
//         Pedido pedido6 = new Pedido("PED-106", cliente3, NivelServicio.EXPRES);
//         pedido6.agregarItem(new ItemPedido(prodValioso, 2));
//         Pedido pedido7 = new Pedido("PED-107", cliente1, NivelServicio.PRIORITARIO);
//         pedido7.agregarItem(new ItemPedido(prodFragil, 1));
//         Pedido[] listaPedidos = new Pedido[]{
//                 pedido1, pedido2, pedido3, pedido4, pedido5, pedido6, pedido7
//         };
//         Thread[] hilos = new Thread[listaPedidos.length];
//         for (int i = 0; i < listaPedidos.length; i++) {
//             Pedido actual = listaPedidos[i];
//             ProcesoSimulacionPedido tarea = new ProcesoSimulacionPedido(actual, gestorRecursos, 600L);
//             Thread hilo = new Thread(tarea, "Hilo-" + actual.getId());
//             hilos[i] = hilo;
//             hilo.start();
//         }
//         for (int i = 0; i < hilos.length; i++) {
//             try {
//                 hilos[i].join();
//             } catch (InterruptedException excepcion) {
//                 Thread.currentThread().interrupt();
//             }
//         }
//         gestorRecursos.imprimirEstadoRecursos();
//     }
// }

package com.redxela;

import com.redxela.almacen.AlmacenPaginado;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.model.Cliente;
import com.redxela.model.ItemPedido;
import com.redxela.model.LoteMercancia;
import com.redxela.model.NivelServicio;
import com.redxela.model.Pedido;
import com.redxela.model.Producto;
import com.redxela.model.TipoMercancia;
import com.redxela.model.TipoRecurso;
import com.redxela.service.BufferRecepcion;
import com.redxela.service.ColaPrioridadPedidos;
import com.redxela.service.ControladorInventario;
import com.redxela.service.GeneradorPedidosAutomatico;
import com.redxela.service.MotorPlanificador;
import com.redxela.service.PoliticaPlanificacion;
import com.redxela.service.ServicioInventario;
import com.redxela.service.ServicioRecepcion;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.util.ConsolaFormato;

import java.util.ArrayList;
import java.util.List;

// clase principal de prueba para verificar colas de prioridad buffer acotado y almacen paginado
public class Main {

    /**
     * Punto de entrada principal para la ejecucion de la prueba integral de los modulos 1 y 2.
     *
     * @param args argumentos de la linea de comandos.
     */
    public static void main(String[] args) {
        ConsolaFormato formato = new ConsolaFormato();
        formato.imprimirEncabezado("SIMULACION DE CENTRO DE DISTRIBUCION REDXELA - MODULO 2");

        // instanciar catalogo base de clientes
        formato.imprimirInfo("Configurando catalogo base de clientes");
        Cliente cliente1 = new Cliente("CLI-001", "Comercializadora Los Altos", "contacto@losaltos.com");
        Cliente cliente2 = new Cliente("CLI-002", "Farmacia La Union", "pedidos@launion.com");
        Cliente cliente3 = new Cliente("CLI-003", "Distribuidora Central", "ventas@central.com");
        List<Cliente> catalogoClientes = new ArrayList<Cliente>();
        catalogoClientes.add(cliente1);
        catalogoClientes.add(cliente2);
        catalogoClientes.add(cliente3);

        // instanciar catalogo de productos para los cinco tipos de mercancia
        formato.imprimirInfo("Configurando catalogo base de productos");
        Producto prodEstandar = new Producto("PRD-01", "Cajas de abarrotes basicos", TipoMercancia.ESTANDAR, 5.0, 45.0);
        Producto prodFragil = new Producto("PRD-02", "Juegos de cristaleria fina", TipoMercancia.FRAGIL, 2.5, 350.0);
        Producto prodPesado = new Producto("PRD-03", "Motores y repuestos industriales", TipoMercancia.PESADA, 180.0, 2500.0);
        Producto prodValioso = new Producto("PRD-04", "Lote de telefonos inteligentes", TipoMercancia.VALIOSA, 1.2, 5000.0);
        Producto prodRefrigerado = new Producto("PRD-05", "Contenedores de medicamentos termolabiles", TipoMercancia.REFRIGERADA, 8.0, 1800.0);
        List<Producto> catalogoProductos = new ArrayList<Producto>();
        catalogoProductos.add(prodEstandar);
        catalogoProductos.add(prodFragil);
        catalogoProductos.add(prodPesado);
        catalogoProductos.add(prodValioso);
        catalogoProductos.add(prodRefrigerado);

        // =========================================================================
        // ETAPA 1: PRUEBA DE COLA DE PRIORIDAD Y CAMBIO DINAMICO DE POLITICAS
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 1: Verificacion de cola de prioridad y reordenamiento dinamico");

        ColaPrioridadPedidos colaPedidos = new ColaPrioridadPedidos(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);

        // crear pedidos de prueba con diferentes niveles y volumenes de articulos
        Pedido pA = new Pedido("PED-A", cliente1, NivelServicio.ECONOMICO);
        pA.agregarItem(new ItemPedido(prodEstandar, 2));

        Pedido pB = new Pedido("PED-B", cliente2, NivelServicio.EXPRES);
        pB.agregarItem(new ItemPedido(prodValioso, 8));

        Pedido pC = new Pedido("PED-C", cliente3, NivelServicio.ESTANDAR);
        pC.agregarItem(new ItemPedido(prodFragil, 1));

        Pedido pD = new Pedido("PED-D", cliente1, NivelServicio.PRIORITARIO);
        pD.agregarItem(new ItemPedido(prodPesado, 10));

        Pedido pE = new Pedido("PED-E", cliente2, NivelServicio.PROGRAMADO);
        pE.agregarItem(new ItemPedido(prodRefrigerado, 3));

        // ingresar pedidos a la cola
        colaPedidos.encolarPedido(pA);
        colaPedidos.encolarPedido(pB);
        colaPedidos.encolarPedido(pC);
        colaPedidos.encolarPedido(pD);
        colaPedidos.encolarPedido(pE);

        // mostrar orden bajo politica de nivel de servicio estricto
        formato.imprimirInfo("Orden de atencion bajo politica: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenServicio = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenServicio.size(); i++) {
            Pedido p = ordenServicio.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Nivel: " + p.getNivelServicio().getNombre() + " (Prioridad " + p.getNivelServicio().getPrioridad() + ") | Articulos: " + p.getCantidadTotal());
        }

        // cambiar dinamicamente a politica por menor cantidad de articulos
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.MENOR_CANTIDAD_ARTICULOS);
        formato.imprimirInfo("Orden tras cambio dinamico a: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenArticulos = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenArticulos.size(); i++) {
            Pedido p = ordenArticulos.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Articulos: " + p.getCantidadTotal() + " | Nivel: " + p.getNivelServicio().getNombre());
        }

        // cambiar dinamicamente a politica fifo por orden cronologico de llegada
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.TIEMPO_ESPERA_FIFO);
        formato.imprimirInfo("Orden tras cambio dinamico a: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenFifo = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenFifo.size(); i++) {
            Pedido p = ordenFifo.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Hora creacion ms: " + p.getTiempoCreacionMs() + " | Nivel: " + p.getNivelServicio().getNombre());
        }

        // regresar la politica a nivel de servicio para la ejecucion
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);

        // =========================================================================
        // ETAPA 2: COMUNICACION RECEPCION - BUFFER ACOTADO - INVENTARIO
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 2: Comunicacion Productor-Consumidor entre Recepcion e Inventario");

        // crear buffer acotado con capacidad maxima de 4 lotes
        BufferRecepcion bufferRecepcion = new BufferRecepcion(4);

        // crear almacen paginado y controlador de existencias
        AlmacenPaginado almacen = new AlmacenPaginado(4, 3, 6);
        ControladorInventario controladorInventario = new ControladorInventario(almacen);

        // crear servicios de recepcion (productor) e inventario (consumidor)
        ServicioRecepcion servicioRecepcion = new ServicioRecepcion(bufferRecepcion, catalogoProductos);
        ServicioInventario servicioInventario = new ServicioInventario(bufferRecepcion, controladorInventario, 150L);

        // crear lotes de mercancia simulados que arriban en camiones
        LoteMercancia lote1 = new LoteMercancia("LOT-01", prodFragil, 4);
        LoteMercancia lote2 = new LoteMercancia("LOT-02", prodPesado, 6);
        LoteMercancia lote3 = new LoteMercancia("LOT-03", prodValioso, 3);
        LoteMercancia lote4 = new LoteMercancia("LOT-04", prodRefrigerado, 5);

        // recepcion deposita lotes en el buffer acotado
        servicioRecepcion.recibirLoteDeCamion(lote1);
        servicioRecepcion.recibirLoteDeCamion(lote2);
        servicioRecepcion.recibirLoteDeCamion(lote3);
        servicioRecepcion.recibirLoteDeCamion(lote4);

        formato.imprimirInfo("Lotes depositados en buffer: " + bufferRecepcion.getCantidadElementos() + " / " + bufferRecepcion.getCapacidad());

        // inventario procesa los lotes extrayendolos del buffer y registrandolos en almacen
        formato.imprimirInfo("Inventario consumiendo lotes del buffer de forma secuencial");
        while (!bufferRecepcion.estaVacio()) {
            servicioInventario.procesarSiguienteLote();
        }

        formato.imprimirInfo("Buffer vaciado exitosamente. Elementos remanentes: " + bufferRecepcion.getCantidadElementos());

        // =========================================================================
        // ETAPA 3: ASIGNACION NO CONTIGUA EN EL ALMACEN (PAGINACION)
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 3: Asignacion no contigua de celdas en el almacen");

        // imprimir tabla de paginacion de cada lote registrado
        System.out.println(almacen.consultarTablaLote("LOT-01").toString());
        System.out.println(almacen.consultarTablaLote("LOT-02").toString());
        System.out.println(almacen.consultarTablaLote("LOT-03").toString());
        System.out.println(almacen.consultarTablaLote("LOT-04").toString());

        // imprimir representacion grafica de la memoria paginada del almacen
        almacen.imprimirMapaOcupacion();

        // =========================================================================
        // ETAPA 4: MOTOR PLANIFICADOR Y GENERADOR AUTOMATICO CONCURRENTE
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 4: Motor planificador despachando pedidos con hilos concurrentes");

        GestorRecursos gestorRecursos = new GestorRecursos();
        gestorRecursos.imprimirEstadoRecursos();

        // instanciar motor planificador que extrae pedidos de la cola
        MotorPlanificador motor = new MotorPlanificador(colaPedidos, gestorRecursos);
        Thread hiloMotor = new Thread(motor, "Hilo-Planificador");
        hiloMotor.start();

        // instanciar generador automatico de pedidos en segundo plano
        GeneradorPedidosAutomatico generador = new GeneradorPedidosAutomatico(
                colaPedidos,
                catalogoClientes,
                catalogoProductos,
                400L
        );
        Thread hiloGenerador = new Thread(generador, "Hilo-Generador");
        hiloGenerador.start();

        // permitir que el generador emita varios pedidos concurrentes
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // detener el generador y pausar la admision
        formato.imprimirInfo("Deteniendo generador automatico de pedidos");
        generador.detener();
        try {
            hiloGenerador.join();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // esperar a que el motor termine de procesar todos los pedidos de la cola
        formato.imprimirInfo("Esperando a que la cola de pedidos se vacie");
        while (!colaPedidos.estaVacia()) {
            try {
                Thread.sleep(300L);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }

        // esperar que terminen todos los hilos que estan ejecutando pedidos actualmente
        formato.imprimirInfo("Esperando finalizacion de tareas de procesamiento activas");
        motor.esperarHilosActivos();

        // detener el hilo del motor planificador
        motor.detener();
        try {
            hiloMotor.join();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }

        // =========================================================================
        // BALANCE FINAL Y VALIDACION DE INTEGRIDAD
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("Verificando restauracion de todos los semaforos tras la ejecucion");
        gestorRecursos.imprimirEstadoRecursos();

        boolean recursosIntegros = true;
        TipoRecurso[] tipos = new TipoRecurso[]{
                TipoRecurso.ESTACION_EMPAQUE,
                TipoRecurso.AREA_CARGA,
                TipoRecurso.ENCARGADO_BODEGA,
                TipoRecurso.MONTACARGAS,
                TipoRecurso.ESTACION_CONTROL_CALIDAD,
                TipoRecurso.SISTEMA_ESCANEO
        };

        for (int i = 0; i < tipos.length; i++) {
            TipoRecurso t = tipos[i];
            int disp = gestorRecursos.consultarDisponibles(t);
            int tot = gestorRecursos.consultarTotal(t);
            if (disp != tot) {
                recursosIntegros = false;
                System.err.println("Alerta: El recurso " + t.getNombre() + " no fue completamente restaurado");
            }
        }

        if (recursosIntegros) {
            formato.imprimirInfo("Prueba superada con exito: Todos los semaforos y estructuras finalizaron en estado optimo");
        } else {
            formato.imprimirInfo("Fallo: Se detecto fuga de recursos en los semaforos");
        }

        // =========================================================================
        // ETAPA 5: DEMOSTRACION DE DEADLOCK Y RESOLUCION MANUAL
        // =========================================================================
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 5: Demostracion de deadlock y resolucion manual");

        // crear dos pedidos simples para provocar deadlock
        Pedido pedidoDlA = new Pedido("DL-1", cliente1, NivelServicio.PRIORITARIO);
        Pedido pedidoDlB = new Pedido("DL-2", cliente2, NivelServicio.PRIORITARIO);

        // iniciar simulador de deadlock con el gestor de recursos existente
        SimuladorDeadlock simuladorDl = new SimuladorDeadlock(gestorRecursos);
        InformacionConflicto conflictoDl = simuladorDl.forzarDeadlockDeliberado(pedidoDlA, TipoRecurso.MONTACARGAS, TipoRecurso.ESTACION_EMPAQUE, pedidoDlB);
        if (conflictoDl != null) {
            System.out.println("[DEADLOCK] " + conflictoDl);
            // resolver manteniendo el pedido A como ganador
            simuladorDl.resolverConflictoManual(pedidoDlA.getId());
            // reencolar el pedido perdedor si existe
            Pedido perdedor = simuladorDl.getPedidoPerdedor();
            if (perdedor != null) {
                colaPedidos.encolarPedido(perdedor);
            }
        }

        // imprimir estado de recursos luego de la resolucion
        gestorRecursos.imprimirEstadoRecursos();

        // iniciar servidor javalin en el puerto 7070 para la conexion con el frontend
        AlmacenPaginado almacenServidor = new AlmacenPaginado(4, 3, 6);
        com.redxela.controller.ControladorServidor servidor = new com.redxela.controller.ControladorServidor(colaPedidos, gestorRecursos, almacenServidor);
        System.out.println("Servidor Javalin iniciado en el puerto 7070");

        formato.imprimirEncabezado("FIN DE LA EJECUCION DEL MODULO 2");
    }
}

