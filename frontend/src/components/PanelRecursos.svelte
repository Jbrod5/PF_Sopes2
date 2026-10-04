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
          <p>Disponibles: {r.disponibles}</p>
        </div>
      </div>
    {/each}
  </div>
</div>
