<script>
  import { claseNivel, claseEstado, formatoTiempo } from "../utils/formato.js";
  export let datos = [];
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
    <span>Pedidos en Proceso</span>
    <span class="badge bg-light text-dark">{datos.length} activos</span>
  </div>
  <div class="card-body">
    {#if datos.length === 0}
      <div class="alert alert-secondary mb-0">Sin pedidos en proceso.</div>
    {:else}
      {#each datos as p}
        <div class="border rounded p-2 mb-2">
          <div class="d-flex justify-content-between align-items-center">
            <strong>{p.id}</strong>
            <span class="badge {claseEstado(p.estado)}">{p.estado}</span>
          </div>
          <div class="text-muted small mb-1">
            {p.cliente} - <span class="badge {claseNivel(p.nivel)}">{p.nivel}</span> - {p.articulos} art.
          </div>
          <div class="progress barra-fina">
            <div class="progress-bar progress-bar-striped progress-bar-animated" style="width: {Math.min(100, Math.max(0, p.progreso))}%"></div>
          </div>
          <div class="d-flex justify-content-between mt-1">
            <small class="text-muted">Progreso: {p.progreso}%</small>
            <small class="text-muted">Restante: {formatoTiempo(p.restanteMs)} - Espera: {formatoTiempo(p.esperaMs)}</small>
          </div>
        </div>
      {/each}
    {/if}
  </div>
</div>
