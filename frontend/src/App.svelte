<script>
  import { onMount } from "svelte";
  import PanelRecursos from "./components/PanelRecursos.svelte";
  import PanelPedidos from "./components/PanelPedidos.svelte";
  import PanelDeadlock from "./components/PanelDeadlock.svelte";
  import MapaAlmacen from "./components/MapaAlmacen.svelte";
  import { obtenerEstado } from "./services/servicioApi.js";
  import { conectarWebSocket } from "./services/servicioWebSocket.js";
  import { recursos, pedidos, almacen, alertaDeadlock } from "./stores/tiendaSimulacion.js";

  let datosRecursos = [];
  let celdasAlmacen = [];

  onMount(async () => {
    // conectar al backend para obtener estado inicial
    try {
      const estado = await obtenerEstado();
      datosRecursos = [
        { nombre: "Montacargas", disponibles: estado.montacargas },
        { nombre: "Estaciones Empaque", disponibles: estado.estacionesEmpaque },
        { nombre: "Areas Carga", disponibles: estado.areasCarga },
        { nombre: "Encargados Bodega", disponibles: estado.encargadosBodega },
        { nombre: "Estaciones Calidad", disponibles: estado.estacionesCalidad },
        { nombre: "Sistemas Escaneo", disponibles: estado.sistemasEscaneo }
      ];
      recursos.set(datosRecursos);
    } catch (e) {
      console.error("Error obteniendo estado:", e);
    }

    // conectar WebSocket para actualizaciones en tiempo real
    conectarWebSocket((mensaje) => {
      try {
        const datos = JSON.parse(mensaje);
        if (datos.tipo === "recursos") {
          recursos.set(datos.datos);
          datosRecursos = datos.datos;
        } else if (datos.tipo === "pedidos") {
          pedidos.set(datos.datos);
        } else if (datos.tipo === "almacen") {
          almacen.set(datos.datos);
          celdasAlmacen = datos.datos;
        } else if (datos.tipo === "deadlock") {
          alertaDeadlock.set(datos.datos);
        }
      } catch (e) {
        console.error("Error procesando mensaje WebSocket:", e);
      }
    });
  });
</script>

<div class="container-fluid">
  <div class="row">
    <div class="col-md-6">
      <PanelRecursos datos={datosRecursos} />
    </div>
    <div class="col-md-6">
      <PanelPedidos />
    </div>
  </div>
  <div class="row">
    <div class="col-md-6">
      <PanelDeadlock />
    </div>
    <div class="col-md-6">
      <MapaAlmacen celdas={celdasAlmacen} />
    </div>
  </div>
</div>
