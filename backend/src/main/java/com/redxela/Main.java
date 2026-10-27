package com.redxela;

import com.redxela.almacen.AlmacenPaginado;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.controller.ControladorServidor;
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
import com.redxela.service.RegistroClientes;
import com.redxela.service.ServicioInventario;
import com.redxela.service.ServicioRecepcion;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.util.ConsolaFormato;
import com.redxela.util.RegistroEventos;

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

        // crear bitacora de eventos visible en la interfaz
        RegistroEventos bitacora = new RegistroEventos();
        formato.setRegistroEventos(bitacora);

        // crear catalogos base de clientes y productos
        List<Cliente> catalogoClientes = crearCatalogoClientes(formato);
        List<Producto> catalogoProductos = crearCatalogoProductos(formato);

        // crear registro de cuentas con los clientes del catalogo
        RegistroClientes registroClientes = new RegistroClientes();
        for (int i = 0; i < catalogoClientes.size(); i++) {
            registroClientes.registrarCliente(catalogoClientes.get(i));
        }

        // ejecutar etapa de cola con cambio dinamico de politicas
        ColaPrioridadPedidos colaPedidos = ejecutarEtapaCola(formato, catalogoClientes, catalogoProductos);

        // crear almacen paginado y controlador de existencias compartidos con el servidor
        AlmacenPaginado almacenCompartido = new AlmacenPaginado(4, 3, 6);
        ControladorInventario inventarioCompartido = new ControladorInventario(almacenCompartido);

        // crear gestor de recursos compartido
        GestorRecursos gestorRecursos = new GestorRecursos();

        // crear motor en vivo con validacion de stock aun sin arrancar
        MotorPlanificador motorVivo = new MotorPlanificador(colaPedidos, gestorRecursos, inventarioCompartido);

        // iniciar servidor javalin primero para observar las etapas en la interfaz
        formato.imprimirInfo("Iniciando servidor javalin para observar las etapas en vivo");
        iniciarServidor(colaPedidos, gestorRecursos, motorVivo, catalogoProductos, registroClientes, almacenCompartido, inventarioCompartido, bitacora);

        // ejecutar comunicacion entre recepcion e inventario sobre las instancias vivas
        ejecutarEtapaBuffer(formato, catalogoProductos, inventarioCompartido);

        // mostrar asignacion no contigua del almacen
        ejecutarEtapaAlmacen(formato, almacenCompartido);

        // despachar pedidos con motor y generador concurrente
        ejecutarEtapaMotor(formato, colaPedidos, catalogoClientes, catalogoProductos, gestorRecursos);

        // verificar restauracion de semaforos tras la ejecucion
        verificarIntegridadRecursos(formato, gestorRecursos);

        // demostrar deadlock y resolucion manual
        ejecutarEtapaDeadlock(formato, colaPedidos, gestorRecursos, catalogoClientes);

        // iniciar despacho continuo para atender la interfaz en vivo
        formato.imprimirInfo("Iniciando despacho continuo del sistema en vivo");
        Thread hiloMotorVivo = new Thread(motorVivo, "Hilo-Planificador-Vivo");
        hiloMotorVivo.start();
        GeneradorPedidosAutomatico generadorVivo = new GeneradorPedidosAutomatico(colaPedidos, catalogoClientes, catalogoProductos, 2000L);
        Thread hiloGeneradorVivo = new Thread(generadorVivo, "Hilo-Generador-Vivo");
        hiloGeneradorVivo.start();

        formato.imprimirEncabezado("SISTEMA EN VIVO - OBSERVE LAS ETAPAS EN LA INTERFAZ");
    }

    // crear catalogo base con los tres clientes de prueba
    private static List<Cliente> crearCatalogoClientes(ConsolaFormato formato) {
        formato.imprimirInfo("Configurando catalogo base de clientes");
        Cliente cliente1 = new Cliente("CLI-001", "Comercializadora Los Altos", "contacto@losaltos.com");
        Cliente cliente2 = new Cliente("CLI-002", "Farmacia La Union", "pedidos@launion.com");
        Cliente cliente3 = new Cliente("CLI-003", "Distribuidora Central", "ventas@central.com");
        List<Cliente> catalogoClientes = new ArrayList<Cliente>();
        catalogoClientes.add(cliente1);
        catalogoClientes.add(cliente2);
        catalogoClientes.add(cliente3);
        return catalogoClientes;
    }

    // crear catalogo con los cinco tipos de mercancia
    private static List<Producto> crearCatalogoProductos(ConsolaFormato formato) {
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
        return catalogoProductos;
    }

    // probar cola de prioridad y cambio dinamico de politicas
    private static ColaPrioridadPedidos ejecutarEtapaCola(ConsolaFormato formato, List<Cliente> clientes, List<Producto> productos) {
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 1: Verificacion de cola de prioridad y reordenamiento dinamico");
        ColaPrioridadPedidos colaPedidos = new ColaPrioridadPedidos(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);
        Pedido pA = new Pedido("PED-A", clientes.get(0), NivelServicio.ECONOMICO);
        pA.agregarItem(new ItemPedido(productos.get(0), 2));
        Pedido pB = new Pedido("PED-B", clientes.get(1), NivelServicio.EXPRES);
        pB.agregarItem(new ItemPedido(productos.get(3), 8));
        Pedido pC = new Pedido("PED-C", clientes.get(2), NivelServicio.ESTANDAR);
        pC.agregarItem(new ItemPedido(productos.get(1), 1));
        Pedido pD = new Pedido("PED-D", clientes.get(0), NivelServicio.PRIORITARIO);
        pD.agregarItem(new ItemPedido(productos.get(2), 10));
        Pedido pE = new Pedido("PED-E", clientes.get(1), NivelServicio.PROGRAMADO);
        pE.agregarItem(new ItemPedido(productos.get(4), 3));
        colaPedidos.encolarPedido(pA);
        colaPedidos.encolarPedido(pB);
        colaPedidos.encolarPedido(pC);
        colaPedidos.encolarPedido(pD);
        colaPedidos.encolarPedido(pE);
        formato.imprimirInfo("Orden de atencion bajo politica: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenServicio = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenServicio.size(); i++) {
            Pedido p = ordenServicio.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Nivel: " + p.getNivelServicio().getNombre() + " (Prioridad " + p.getNivelServicio().getPrioridad() + ") | Articulos: " + p.getCantidadTotal());
        }
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.MENOR_CANTIDAD_ARTICULOS);
        formato.imprimirInfo("Orden tras cambio dinamico a: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenArticulos = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenArticulos.size(); i++) {
            Pedido p = ordenArticulos.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Articulos: " + p.getCantidadTotal() + " | Nivel: " + p.getNivelServicio().getNombre());
        }
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.TIEMPO_ESPERA_FIFO);
        formato.imprimirInfo("Orden tras cambio dinamico a: " + colaPedidos.getPoliticaActual().getNombre());
        List<Pedido> ordenFifo = colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < ordenFifo.size(); i++) {
            Pedido p = ordenFifo.get(i);
            System.out.println("  Posicion " + (i + 1) + ": " + p.getId() + " | Hora creacion ms: " + p.getTiempoCreacionMs() + " | Nivel: " + p.getNivelServicio().getNombre());
        }
        colaPedidos.cambiarPolitica(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);
        return colaPedidos;
    }

    // comunicar recepcion con inventario mediante buffer acotado
    private static void ejecutarEtapaBuffer(ConsolaFormato formato, List<Producto> productos, ControladorInventario controlador) {
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 2: Comunicacion Productor-Consumidor entre Recepcion e Inventario");
        BufferRecepcion bufferRecepcion = new BufferRecepcion(4);
        ServicioRecepcion servicioRecepcion = new ServicioRecepcion(bufferRecepcion, productos);
        ServicioInventario servicioInventario = new ServicioInventario(bufferRecepcion, controlador, 150L);
        LoteMercancia lote1 = new LoteMercancia("LOT-01", productos.get(1), 4);
        LoteMercancia lote2 = new LoteMercancia("LOT-02", productos.get(2), 6);
        LoteMercancia lote3 = new LoteMercancia("LOT-03", productos.get(3), 3);
        LoteMercancia lote4 = new LoteMercancia("LOT-04", productos.get(4), 5);
        servicioRecepcion.recibirLoteDeCamion(lote1);
        servicioRecepcion.recibirLoteDeCamion(lote2);
        servicioRecepcion.recibirLoteDeCamion(lote3);
        servicioRecepcion.recibirLoteDeCamion(lote4);
        formato.imprimirInfo("Lotes depositados en buffer: " + bufferRecepcion.getCantidadElementos() + " / " + bufferRecepcion.getCapacidad());
        formato.imprimirInfo("Inventario consumiendo lotes del buffer de forma secuencial");
        while (!bufferRecepcion.estaVacio()) {
            servicioInventario.procesarSiguienteLote();
        }
        formato.imprimirInfo("Buffer vaciado exitosamente. Elementos remanentes: " + bufferRecepcion.getCantidadElementos());
    }

    // mostrar tablas de paginacion y mapa de ocupacion del almacen
    private static void ejecutarEtapaAlmacen(ConsolaFormato formato, AlmacenPaginado almacen) {
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 3: Asignacion no contigua de celdas en el almacen");
        System.out.println(almacen.consultarTablaLote("LOT-01").toString());
        System.out.println(almacen.consultarTablaLote("LOT-02").toString());
        System.out.println(almacen.consultarTablaLote("LOT-03").toString());
        System.out.println(almacen.consultarTablaLote("LOT-04").toString());
        almacen.imprimirMapaOcupacion();
    }

    // despachar pedidos con motor planificador y generador automatico
    private static void ejecutarEtapaMotor(ConsolaFormato formato, ColaPrioridadPedidos cola, List<Cliente> clientes, List<Producto> productos, GestorRecursos gestorRecursos) {
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 4: Motor planificador despachando pedidos con hilos concurrentes");
        gestorRecursos.imprimirEstadoRecursos();
        MotorPlanificador motor = new MotorPlanificador(cola, gestorRecursos);
        Thread hiloMotor = new Thread(motor, "Hilo-Planificador");
        hiloMotor.start();
        GeneradorPedidosAutomatico generador = new GeneradorPedidosAutomatico(cola, clientes, productos, 400L);
        Thread hiloGenerador = new Thread(generador, "Hilo-Generador");
        hiloGenerador.start();
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
        formato.imprimirInfo("Deteniendo generador automatico de pedidos");
        generador.detener();
        try {
            hiloGenerador.join();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
        formato.imprimirInfo("Esperando a que la cola de pedidos se vacie");
        while (!cola.estaVacia()) {
            try {
                Thread.sleep(300L);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }
        formato.imprimirInfo("Esperando finalizacion de tareas de procesamiento activas");
        motor.esperarHilosActivos();
        motor.detener();
        try {
            hiloMotor.join();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
    }

    // verificar restauracion completa de los semaforos
    private static void verificarIntegridadRecursos(ConsolaFormato formato, GestorRecursos gestor) {
        formato.imprimirSeparador();
        formato.imprimirInfo("Verificando restauracion de todos los semaforos tras la ejecucion");
        gestor.imprimirEstadoRecursos();
        boolean integros = true;
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
            int disponibles = gestor.consultarDisponibles(t);
            int totales = gestor.consultarTotal(t);
            if (disponibles != totales) {
                integros = false;
                System.err.println("Alerta: El recurso " + t.getNombre() + " no fue completamente restaurado");
            }
        }
        if (integros) {
            formato.imprimirInfo("Prueba superada con exito: Todos los semaforos y estructuras finalizaron en estado optimo");
        } else {
            formato.imprimirInfo("Fallo: Se detecto fuga de recursos en los semaforos");
        }
    }

    // provocar deadlock deliberado y resolver por despropiacion manual
    private static void ejecutarEtapaDeadlock(ConsolaFormato formato, ColaPrioridadPedidos cola, GestorRecursos gestor, List<Cliente> clientes) {
        formato.imprimirSeparador();
        formato.imprimirInfo("ETAPA 5: Demostracion de deadlock y resolucion manual");
        Pedido pedidoDlA = new Pedido("DL-1", clientes.get(0), NivelServicio.PRIORITARIO);
        Pedido pedidoDlB = new Pedido("DL-2", clientes.get(1), NivelServicio.PRIORITARIO);
        SimuladorDeadlock simuladorDl = new SimuladorDeadlock(gestor);
        InformacionConflicto conflictoDl = simuladorDl.forzarDeadlockDeliberado(pedidoDlA, TipoRecurso.MONTACARGAS, TipoRecurso.ESTACION_EMPAQUE, pedidoDlB);
        if (conflictoDl != null) {
            System.out.println("[DEADLOCK] " + conflictoDl);
            simuladorDl.resolverConflictoManual(pedidoDlA.getId());
            Pedido perdedor = simuladorDl.getPedidoPerdedor();
            if (perdedor != null) {
                cola.encolarPedido(perdedor);
            }
        }
        gestor.imprimirEstadoRecursos();
    }

    // iniciar servidor javalin para la conexion con el frontend
    private static void iniciarServidor(ColaPrioridadPedidos cola, GestorRecursos gestor, MotorPlanificador motor, List<Producto> productos, RegistroClientes registro, AlmacenPaginado almacenServidor, ControladorInventario inventarioVivo, RegistroEventos bitacora) {
        ControladorServidor servidor = new ControladorServidor(cola, gestor, almacenServidor, motor);
        BufferRecepcion bufferVivo = new BufferRecepcion(10);
        ServicioRecepcion recepcionViva = new ServicioRecepcion(bufferVivo, productos, 2500L);
        ServicioInventario inventarioServicio = new ServicioInventario(bufferVivo, inventarioVivo, 300L);
        Thread hiloInventario = new Thread(inventarioServicio, "Hilo-Inventario-Vivo");
        hiloInventario.start();
        // iniciar llegada continua de camiones para alimentar stock en vivo
        Thread hiloRecepcion = new Thread(recepcionViva, "Hilo-Recepcion-Viva");
        hiloRecepcion.start();
        servidor.setServicioRecepcion(recepcionViva);
        servidor.setRegistroClientes(registro);
        servidor.setRegistroEventos(bitacora);
        System.out.println("Servidor Javalin iniciado en el puerto configurado (7070 por defecto)");
    }
}
