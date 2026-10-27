<script>
  import { onMount, onDestroy } from "svelte";
  import PanelRecursos from "./components/PanelRecursos.svelte";
  import PanelPedidos from "./components/PanelPedidos.svelte";
  import PanelDeadlock from "./components/PanelDeadlock.svelte";
  import PanelEstadisticas from "./components/PanelEstadisticas.svelte";
  import PanelProcesando from "./components/PanelProcesando.svelte";
  import PanelCola from "./components/PanelCola.svelte";
  import PanelMercaderia from "./components/PanelMercaderia.svelte";
  import PanelClientes from "./components/PanelClientes.svelte";
  import PanelBitacora from "./components/PanelBitacora.svelte";
  import MapaAlmacen from "./components/MapaAlmacen.svelte";
  import { obtenerRecursos, obtenerEstadisticas, obtenerProcesando, obtenerCola } from "./services/servicioApi.js";
  import { conectarWebSocket } from "./services/servicioWebSocket.js";
  import { recursos, pedidos, almacen, alertaDeadlock } from "./stores/tiendaSimulacion.js";

  let datosRecursos = [];
  let datosEstadisticas = null;
  let datosProcesando = [];
  let datosCola = null;
  let celdasAlmacen = [];
  let intervalo = null;
  let enLinea = false;

  async function actualizarTablero() {
    try {
      const lista = await obtenerRecursos();
      datosRecursos = lista;
      recursos.set(lista);
      enLinea = true;
    } catch (e) {
      enLinea = false;
    }
    try {
      datosEstadisticas = await obtenerEstadisticas();
    } catch (e) {
      console.error("Error obteniendo estadisticas:", e);
    }
    try {
      datosProcesando = await obtenerProcesando();
    } catch (e) {
      console.error("Error obteniendo procesando:", e);
    }
    try {
      datosCola = await obtenerCola();
    } catch (e) {
      console.error("Error obteniendo cola:", e);
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
        // refrescar el tablero ante cualquier evento del servidor
        actualizarTablero();
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

<nav class="navbar navbar-dark navbar-redxela mb-4">
  <div class="container-fluid">
    <span class="navbar-brand mb-0 h1">REDXela - Centro de Distribucion</span>
    <span class="d-flex align-items-center gap-2">
      {#if enLinea}
        <span class="badge bg-success">En linea</span>
      {:else}
        <span class="badge bg-danger">Sin conexion</span>
      {/if}
    </span>
  </div>
</nav>

<div class="container-fluid pb-4">
  <div class="row g-3">
    <div class="col-lg-6">
      <PanelRecursos datos={datosRecursos} />
    </div>
    <div class="col-lg-6">
      <PanelCola datos={datosCola} />
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-lg-6">
      <PanelProcesando datos={datosProcesando} />
    </div>
    <div class="col-lg-6">
      <PanelEstadisticas datos={datosEstadisticas} />
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-lg-6">
      <PanelPedidos />
    </div>
    <div class="col-lg-6">
      <PanelClientes />
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-lg-6">
      <PanelMercaderia />
    </div>
    <div class="col-lg-6">
      <PanelDeadlock />
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-12">
      <MapaAlmacen celdas={celdasAlmacen} />
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-12">
      <PanelBitacora />
    </div>
  </div>
  <footer class="text-center text-muted mt-4">
    <small>Sistema de simulacion REDXela - Sistemas Operativos 2</small>
  </footer>
</div>
