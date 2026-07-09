import {describe,expect,it} from 'vitest'
import {money,roleHome,statusLabel} from './format'
describe('format helpers',()=>{
  it('formats INR safely',()=>expect(money(1250)).toContain('1,250'))
  it('maps every staff role home',()=>expect(Object.keys(roleHome)).toEqual(['ADMIN','MANAGER','WAITER','KITCHEN']))
  it('maps kitchen status to guest language',()=>expect(statusLabel.PREPARING).toBe('Being prepared'))
})
