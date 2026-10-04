<script>
  export let celdas = [];
</script>

<div class="container mt-3">
  <h4>Mapa Almacen</h4>
  <div class="row">
    {#each celdas as c}
      <div class="col-1 mb-1">
        <div class="border p-1 text-center" style="font-size: 0.8rem;">{c.id}</div>
      </div>
    {/each}
  </div>
</div>
