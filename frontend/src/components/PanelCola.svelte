<script>
  import { claseNivel, formatoTiempo } from "../utils/formato.js";
  export let datos = null;
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
    <span>Lista de Espera</span>
    {#if datos}
      <span class="badge bg-light text-dark">{datos.enEspera} en espera</span>
    {/if}
  </div>
  <div class="card-body">
    {#if !datos}
      <p class="text-muted mb-0">Cargando lista de espera...</p>
    {:else}
      <p class="mb-2"><small class="text-muted">Criterio de orden:</small> <span class="badge bg-info text-dark">{datos.politica}</span></p>
      {#if datos.pedidos.length === 0}
        <div class="alert alert-success mb-0">Sin pedidos en espera.</div>
      {:else}
        <div class="table-responsive">
          <table class="table table-sm table-hover tabla-compacta mb-0">
            <thead>
              <tr>
                <th>#</th>
                <th>Pedido</th>
                <th>Nivel</th>
                <th class="text-center">Art.</th>
                <th>Espera</th>
              </tr>
            </thead>
            <tbody>
              {#each datos.pedidos as p}
                <tr>
                  <td>{p.posicion}</td>
                  <td><strong>{p.id}</strong><br /><small class="text-muted">{p.cliente}</small></td>
                  <td><span class="badge {claseNivel(p.nivel)}">{p.nivel}</span></td>
                  <td class="text-center">{p.articulos}</td>
                  <td>{formatoTiempo(p.esperaMs)}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    {/if}
  </div>
</div>
