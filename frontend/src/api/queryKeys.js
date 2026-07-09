export const authKeys = {
  me: ['auth', 'me'],
}

export const restaurantKeys = {
  all: ['restaurants'],
  detail: id => ['restaurants', Number(id)],
}

export const branchKeys = {
  byRestaurant: restaurantId => ['branches', Number(restaurantId)],
}

export const tableKeys = {
  byBranch: branchId => ['tables', Number(branchId)],
}

export const menuKeys = {
  categories: filters => ['menu', 'categories', filters || {}],
  items: filters => ['menu', 'items', filters || {}],
}

export const orderKeys = {
  list: filters => ['orders', filters || {}],
  detail: id => ['orders', Number(id)],
}

export const kitchenKeys = {
  queue: filters => ['kitchen', 'queue', filters || {}],
  history: filters => ['kitchen', 'history', filters || {}],
}

export const analyticsKeys = {
  dashboard: filters => ['analytics', 'dashboard', filters || {}],
}

export const notificationKeys = {
  inbox: filters => ['notifications', 'inbox', filters || {}],
  unreadCount: ['notifications', 'unread-count'],
}

export const guestKeys = {
  session: token => ['guest', 'session', token],
  menu: token => ['guest', 'menu', token],
  order: trackingToken => ['guest', 'order', trackingToken],
}
