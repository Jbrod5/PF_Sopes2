<script>
  export let datos = [];
</script>

<div class="container mt-3">
  <h4>Estado de Recursos</h4>
  <div class="row">
    {#each datos as r}
      <div class="col-md-4 mb-2">
        <div class="card p-2">
          <h6>{r.nombre}</h6>
          <p>Disponibles: {r.disponibles} / {r.total}</p>
          <p>En uso: {r.enUso}</p>
          {#if r.tenedores && r.tenedores.length > 0}
            <p>Retenido por: {r.tenedores.join(", ")}</p>
          {:else}
            <p>Retenido por: nadie</p>
          {/if}
        </div>
      </div>
    {/each}
  </div>
</div>
