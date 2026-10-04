// modulo para gestionar la conexion WebSocket

let socket = null;

export function conectarWebSocket(callback) {
  socket = new WebSocket("ws://localhost:7070/ws/simulacion");
  socket.onmessage = (event) => {
    if (callback) callback(event.data);
  };
  socket.onopen = () => {
    console.log("WebSocket conectado :D");
  };
}

export function desconectarWebSocket() {
  if (socket) {
    socket.close();
  }
}
