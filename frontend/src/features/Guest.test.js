import {describe,expect,it} from 'vitest'
import {cartReducer} from './Guest'
describe('cartReducer',()=>{
  it('adds and increments an item',()=>{const item={id:1,name:'Dosa',price:120};const once=cartReducer([],{type:'ADD',item});expect(cartReducer(once,{type:'ADD',item})).toEqual([{...item,quantity:2}])})
  it('removes an item when quantity reaches zero',()=>expect(cartReducer([{id:1,quantity:1}],{type:'DEC',id:1})).toEqual([]))
  it('clears the cart',()=>expect(cartReducer([{id:1,quantity:3}],{type:'CLEAR'})).toEqual([]))
})
