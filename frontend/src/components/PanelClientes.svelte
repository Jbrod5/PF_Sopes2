<script>
  import { onMount } from "svelte";
  import { obtenerClientes, registrarCliente } from "../services/servicioApi.js";

  let clientes = [];
  let id = "";
  let nombre = "";
  let correo = "";
  let mensaje = "";

  async function cargar() {
    try {
      clientes = await obtenerClientes();
    } catch (e) {
      mensaje = "No se pudo cargar el registro de clientes";
    }
  }

  onMount(cargar);

  async function enviar() {
    mensaje = "";
    try {
      const respuesta = await registrarCliente({ id, nombre, correo });
      if (respuesta.error) {
        mensaje = respuesta.error;
        return;
      }
      mensaje = "Cuenta registrada: " + respuesta.id;
      id = "";
      nombre = "";
      correo = "";
      await cargar();
    } catch (e) {
      mensaje = "Error de conexion con el servidor";
    }
  }
</script>

<div class="container mt-3">
  <h4>Panel de Clientes</h4>
  <form on:submit|preventDefault={enviar}>
    <input bind:value={id} placeholder="ID cliente (ej. CLI-010)" class="form-control mb-2" />
    <input bind:value={nombre} placeholder="Nombre del cliente" class="form-control mb-2" />
    <input bind:value={correo} placeholder="Correo electronico" class="form-control mb-2" />
    <button type="submit" class="btn btn-primary">Crear cuenta</button>
  </form>
  {#if mensaje}
    <div class="alert alert-info mt-2">{mensaje}</div>
  {/if}
  <h6 class="mt-3">Cuentas registradas ({clientes.length})</h6>
  <ul class="list-group">
    {#each clientes as c}
      <li class="list-group-item">{c.id} - {c.nombre}</li>
    {/each}
  </ul>
</div>
