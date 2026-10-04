import { writable } from "svelte/store";

export const recursos = writable([]);
export const pedidos = writable([]);
export const almacen = writable([]);
export const alertaDeadlock = writable(null);
