<script>
  import { registrarPedido, cambiarPolitica } from "../services/servicioApi.js";
  let nombre = "";
  let nivel = "ESTANDAR";
  let contador = 1;

  async function enviar() {
    const id = "WEB-" + contador;
    contador = contador + 1;
    await registrarPedido({ id, cliente: nombre, nivel });
    nombre = "";
  }

  async function actualizarPolitica() {
    await cambiarPolitica(nivel);
  }
</script>

<div class="container mt-3">
  <h4>Panel de Pedidos</h4>
  <form on:submit|preventDefault={enviar}>
    <input bind:value={nombre} placeholder="Nombre pedido" class="form-control mb-2" />
    <select bind:value={nivel} class="form-select mb-2">
      <option>ESTANDAR</option>
      <option>EXPRES</option>
      <option>PRIORITARIO</option>
    </select>
    <button type="submit" class="btn btn-primary">Registrar pedido</button>
  </form>
</div>
