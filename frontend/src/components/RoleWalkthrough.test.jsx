import {describe, expect, it} from 'vitest'
import {walkthroughs} from './RoleWalkthrough'

const allowedPaths = {
  ADMIN: [
    '/app/admin',
    '/app/restaurants',
    '/app/waiter/orders/new',
    '/app/kitchen',
    '/app/menu/manage',
    '/app/users',
    '/app/analytics',
    '/app/notifications',
  ],
  MANAGER: [
    '/app/manager',
    '/app/waiter/orders',
    '/app/waiter/orders/new',
    '/app/kitchen',
    '/app/menu/manage',
    '/app/analytics',
    '/app/notifications',
  ],
  WAITER: ['/app/waiter/orders', '/app/waiter/orders/new', '/app/menu', '/app/notifications'],
  KITCHEN: ['/app/kitchen', '/app/kitchen/history', '/app/menu', '/app/notifications'],
}

describe('role walkthroughs', () => {
  it.each(Object.keys(allowedPaths))('only guides %s users to routes they can access', role => {
    const guidedPaths = walkthroughs[role].map(step => step.path).filter(Boolean)

    expect(guidedPaths).toEqual(allowedPaths[role])
  })

  it.each(Object.keys(allowedPaths))('gives every %s step useful display content', role => {
    for (const step of walkthroughs[role]) {
      expect(step.target).toBeTruthy()
      expect(step.title).toBeTruthy()
      expect(step.description).toBeTruthy()
    }
  })
})
