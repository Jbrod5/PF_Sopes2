// funciones fetch para interactuar con los endpoints REST

export async function obtenerEstado() {
  const res = await fetch("http://localhost:7070/api/estado");
  return res.json();
}

export async function registrarPedido(datos) {
  const res = await fetch("http://localhost:7070/api/pedidos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });
  return res.text();
}

export async function cambiarPolitica(politica) {
  const res = await fetch("http://localhost:7070/api/politica", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ politica }),
  });
  return res.text();
}

export async function inducirDeadlock() {
  const res = await fetch("http://localhost:7070/api/deadlock/inducir", {
    method: "POST",
  });
  return res.text();
}

export async function resolverDeadlock(id) {
  const res = await fetch("http://localhost:7070/api/deadlock/resolver", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id }),
  });
  return res.text();
}
