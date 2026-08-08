export const money = value =>
  new Intl.NumberFormat('en-IN', {style: 'currency', currency: 'INR'}).format(Number(value || 0))
export const roleHome = {
  ADMIN: '/app/admin',
  MANAGER: '/app/manager',
  WAITER: '/app/waiter/orders',
  KITCHEN: '/app/kitchen',
}
export const statusLabel = {
  PENDING: 'Confirmed',
  PREPARING: 'Being prepared',
  READY: 'Ready',
  SERVED: 'Completed',
  CANCELLED: 'Cancelled',
}
