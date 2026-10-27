<script>
  export let datos = [];
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white">Estado de Recursos</div>
  <div class="card-body">
    {#if datos.length === 0}
      <p class="text-muted mb-0">Cargando recursos...</p>
    {:else}
      <div class="table-responsive">
        <table class="table table-sm table-hover tabla-compacta mb-0">
          <thead>
            <tr>
              <th>Recurso</th>
              <th class="text-center">Uso</th>
              <th>Ocupacion</th>
              <th>Retenido por</th>
            </tr>
          </thead>
          <tbody>
            {#each datos as r}
              <tr>
                <td><strong>{r.nombre}</strong><br /><small class="text-muted">{r.disponibles} de {r.total} libres</small></td>
                <td class="text-center"><span class="badge bg-secondary">{r.enUso}/{r.total}</span></td>
                <td style="min-width: 7rem;">
                  <div class="progress barra-fina">
                    <div
                      class="progress-bar {r.enUso > 0 ? 'bg-warning' : 'bg-success'}"
                      style="width: {r.total > 0 ? Math.round((r.enUso / r.total) * 100) : 0}%"
                    ></div>
                  </div>
                </td>
                <td>
                  {#if r.tenedores && r.tenedores.length > 0}
                    {#each r.tenedores as t}
                      <span class="badge bg-dark me-1">{t}</span>
                    {/each}
                  {:else}
                    <span class="text-muted">Libre</span>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </div>
</div>
