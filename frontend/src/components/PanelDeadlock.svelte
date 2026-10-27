<script>
  import { onMount, onDestroy } from "svelte";
  import { inducirDeadlock, resolverDeadlock, obtenerDetalleDeadlock } from "../services/servicioApi";
  let ganador = "";
  let detalle = null;
  let intervalo = null;
  let mensaje = "";

  async function actualizar() {
    try {
      detalle = await obtenerDetalleDeadlock();
    } catch (e) {
      console.error("Error obteniendo detalle:", e);
    }
  }

  async function inducir() {
    mensaje = "";
    await inducirDeadlock();
    await actualizar();
  }

  async function resolver() {
    mensaje = "";
    if (!ganador) {
      mensaje = "Escriba el ID del pedido ganador antes de resolver";
      return;
    }
    await resolverDeadlock(ganador);
    await actualizar();
  }

  onMount(async () => {
    await actualizar();
    intervalo = setInterval(actualizar, 2000);
  });

  onDestroy(() => {
    if (intervalo) {
      clearInterval(intervalo);
    }
  });
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
    <span>Conflicto de Recursos</span>
    {#if detalle && detalle.activo && !detalle.resuelto}
      <span class="badge bg-danger">Activo</span>
    {:else if detalle && detalle.activo && detalle.resuelto}
      <span class="badge bg-success">Resuelto</span>
    {:else}
      <span class="badge bg-light text-dark">Sin conflicto</span>
    {/if}
  </div>
  <div class="card-body">
    {#if detalle && detalle.activo}
      <div class="alert {detalle.resuelto ? 'alert-success' : 'alert-danger'} py-2">{detalle.causa}</div>
      <div class="table-responsive">
        <table class="table table-sm tabla-compacta">
          <thead>
            <tr><th>Pedido</th><th>Retiene</th><th>Espera</th></tr>
          </thead>
          <tbody>
            <tr><td><strong>{detalle.pedido1.id}</strong></td><td>{detalle.pedido1.retiene}</td><td>{detalle.pedido1.espera}</td></tr>
            <tr><td><strong>{detalle.pedido2.id}</strong></td><td>{detalle.pedido2.retiene}</td><td>{detalle.pedido2.espera}</td></tr>
          </tbody>
        </table>
      </div>
      {#if detalle.resuelto}
        <p class="mb-2">Ganador: <span class="badge bg-success">{detalle.ganador}</span> (el perdedor regreso a la espera)</p>
      {/if}
    {:else}
      <div class="alert alert-success">Sin interbloqueo activo</div>
    {/if}
    <div class="row g-2">
      <div class="col-4">
        <button on:click={inducir} class="btn btn-danger w-100">Inducir</button>
      </div>
      <div class="col-4">
        <input bind:value={ganador} placeholder="ID ganador" class="form-control" />
      </div>
      <div class="col-4">
        <button on:click={resolver} class="btn btn-success w-100">Resolver</button>
      </div>
    </div>
    {#if mensaje}
      <div class="alert alert-warning mt-2 mb-0 py-2">{mensaje}</div>
    {/if}
  </div>
</div>
