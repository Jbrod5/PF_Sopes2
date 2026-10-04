<script>
  import { inducirDeadlock, resolverDeadlock } from "../services/servicioApi";
  let ganador = "";

  async function inducir() {
    await inducirDeadlock();
  }

  async function resolver() {
    await resolverDeadlock(ganador);
  }
</script>

<div class="container mt-3">
  <h4>Panel Deadlock</h4>
  <div class="alert alert-warning">
    Indicador de interbloqueo activo
  </div>
  <button on:click={inducir} class="btn btn-danger me-2">Inducir</button>
  <input bind:value={ganador} placeholder="ID ganador" class="form-control mb-2" />
  <button on:click={resolver} class="btn btn-success">Resolver</button>
</div>
