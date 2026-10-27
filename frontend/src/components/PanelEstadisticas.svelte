<script>
  import { formatoTiempo } from "../utils/formato.js";
  export let datos = null;
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white">Estadisticas del Sistema</div>
  <div class="card-body">
    {#if !datos}
      <p class="text-muted mb-0">Cargando estadisticas...</p>
    {:else}
      <div class="row text-center g-2">
        <div class="col-6">
          <div class="border rounded p-2 h-100">
            <div class="numero-grande text-success">{datos.completados}</div>
            <small class="text-muted">Completados</small>
          </div>
        </div>
        <div class="col-6">
          <div class="border rounded p-2 h-100">
            <div class="numero-grande text-primary">{datos.enProceso}</div>
            <small class="text-muted">En proceso</small>
          </div>
        </div>
        <div class="col-6">
          <div class="border rounded p-2 h-100">
            <div class="numero-grande text-warning">{datos.enEspera}</div>
            <small class="text-muted">En espera</small>
          </div>
        </div>
        <div class="col-6">
          <div class="border rounded p-2 h-100">
            <div class="numero-grande">{formatoTiempo(datos.esperaPromedioMs)}</div>
            <small class="text-muted">Espera promedio</small>
          </div>
        </div>
        <div class="col-12">
          <div class="border rounded p-2">
            <div class="numero-grande">{formatoTiempo(datos.procesoPromedioMs)}</div>
            <small class="text-muted">Proceso promedio</small>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div>
