<script>
  import { onMount, onDestroy } from "svelte";
  import PanelRecursos from "./components/PanelRecursos.svelte";
  import PanelPedidos from "./components/PanelPedidos.svelte";
  import PanelDeadlock from "./components/PanelDeadlock.svelte";
  import PanelEstadisticas from "./components/PanelEstadisticas.svelte";
  import MapaAlmacen from "./components/MapaAlmacen.svelte";
  import { obtenerRecursos, obtenerEstadisticas } from "./services/servicioApi.js";
  import { conectarWebSocket } from "./services/servicioWebSocket.js";
  import { recursos, pedidos, almacen, alertaDeadlock } from "./stores/tiendaSimulacion.js";

  let datosRecursos = [];
  let datosEstadisticas = null;
  let celdasAlmacen = [];
  let intervalo = null;

  async function actualizarTablero() {
    try {
      const lista = await obtenerRecursos();
      datosRecursos = lista;
      recursos.set(lista);
    } catch (e) {
      console.error("Error obteniendo recursos:", e);
    }
    try {
      const metricas = await obtenerEstadisticas();
      datosEstadisticas = metricas;
    } catch (e) {
      console.error("Error obteniendo estadisticas:", e);
    }
  }

  onMount(async () => {
    // cargar estado inicial y programar actualizacion periodica
    await actualizarTablero();
    intervalo = setInterval(actualizarTablero, 2000);

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

  onDestroy(() => {
    if (intervalo) {
      clearInterval(intervalo);
    }
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
  <div class="row">
    <div class="col-md-6">
      <PanelEstadisticas datos={datosEstadisticas} />
    </div>
  </div>
</div>
