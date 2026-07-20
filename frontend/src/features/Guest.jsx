import {createContext,useContext,useEffect,useReducer,useState} from 'react'
import {useMutation,useQuery} from '@tanstack/react-query'
import {Navigate,useNavigate,useParams} from 'react-router-dom'
import {guestApi} from '../api/client'
import {money,statusLabel} from '../lib/format'
const CartContext=createContext(null)
const normalizePhone=value=>{const raw=String(value||'').trim();if(!raw)return null;const compact=raw.replace(/[\s()-]/g,'');if(compact.startsWith('+'))return compact;const digits=compact.replace(/\D/g,'');if(digits.length===10)return `+91${digits}`;if(digits.length===12&&digits.startsWith('91'))return `+${digits}`;return compact}
let razorpayScriptPromise

function loadRazorpayScript(){
  if(window.Razorpay)return Promise.resolve()
  if(razorpayScriptPromise)return razorpayScriptPromise

  razorpayScriptPromise=new Promise((resolve,reject)=>{
    const script=document.createElement('script')
    script.src='https://checkout.razorpay.com/v1/checkout.js'
    script.async=true
    script.onload=resolve
    script.onerror=()=>{
      razorpayScriptPromise=undefined
      reject(new Error('Could not load Razorpay Checkout. Check your internet connection and try again.'))
    }
    document.body.appendChild(script)
  })

  return razorpayScriptPromise
}

function openRazorpayCheckout(checkoutResponse,customer){
  const {trackingToken,order,payment}=checkoutResponse

  return new Promise((resolve,reject)=>{
    let verificationStarted=false
    let finished=false
    let failureMessage

    const checkout=new window.Razorpay({
      key:payment.razorpayKeyId,
      amount:payment.amountInPaise,
      currency:payment.currency,
      name:'SmartServe',
      description:`Payment for order #${order.id}`,
      order_id:payment.razorpayOrderId,
      prefill:{
        name:customer.customerName,
        contact:customer.customerPhone||''
      },
      theme:{color:'#166534'},
      handler:async razorpayResult=>{
        verificationStarted=true
        try{
          await guestApi.verifyPayment(trackingToken,{
            razorpayOrderId:razorpayResult.razorpay_order_id,
            razorpayPaymentId:razorpayResult.razorpay_payment_id,
            razorpaySignature:razorpayResult.razorpay_signature
          })
          finished=true
          resolve(checkoutResponse)
        }catch(error){
          reject(error)
        }
      },
      modal:{
        ondismiss:()=>{
          if(!finished&&!verificationStarted){
            reject(new Error(failureMessage||'Payment was cancelled. Your cart is still available so you can try again.'))
          }
        }
      }
    })

    checkout.on('payment.failed',response=>{
      failureMessage=response.error?.description||'Payment failed. Please try again.'
    })
    checkout.open()
  })
}

export function cartReducer(s,a){if(a.type==='ADD'){const found=s.find(x=>x.id===a.item.id);return found?s.map(x=>x.id===a.item.id?{...x,quantity:x.quantity+1}:x):[...s,{...a.item,quantity:1}]}if(a.type==='DEC')return s.map(x=>x.id===a.id?{...x,quantity:x.quantity-1}:x).filter(x=>x.quantity>0);if(a.type==='CLEAR')return [];return s}
function CartProvider({token,children}){const key=`smartserve.cart.${token}`;const [cart,dispatch]=useReducer(cartReducer,[],()=>JSON.parse(sessionStorage.getItem(key)||'[]'));useEffect(()=>sessionStorage.setItem(key,JSON.stringify(cart)),[cart,key]);return <CartContext.Provider value={{cart,dispatch}}>{children}</CartContext.Provider>}
export function GuestEntry(){const {qrToken}=useParams(),q=useQuery({queryKey:['guest-session',qrToken],queryFn:()=>guestApi.session(qrToken),retry:false});if(q.isLoading)return <GuestState text="Checking your table..."/>;if(q.error)return <GuestState text={q.error.message}/>;if(!q.data.orderingEnabled)return <GuestState text="This table is not accepting new orders right now."/>;return <Navigate to={`/guest/${qrToken}/menu`} replace/>}
export function GuestMenu(){const {qrToken}=useParams();return <CartProvider token={qrToken}><GuestMenuInner token={qrToken}/></CartProvider>}
function GuestMenuInner({token}){
  const q=useQuery({queryKey:['guest-menu',token],queryFn:()=>guestApi.menu(token)})
  const {cart,dispatch}=useContext(CartContext),nav=useNavigate()
  const [activeCategory,setActiveCategory]=useState('all')
  const [search,setSearch]=useState('')
  if(q.isLoading)return <GuestState text="Preparing today’s menu…"/>
  const items=(q.data.items||[]).filter(i=>(activeCategory==='all'||String(i.categoryId)===String(activeCategory))&&(`${i.name} ${i.description||''}`.toLowerCase().includes(search.toLowerCase())))
  const categories=(q.data.categories||[]).filter(c=>(q.data.items||[]).some(i=>String(i.categoryId)===String(c.id)))
  return <div className="guest"><header><span className="eyebrow">SMARTSERVE</span><h1>What sounds good?</h1><p>Fresh from the kitchen, ordered right from your table.</p></header>
    <div className="guestMenuTools card"><input className="menuSearch" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search dishes…"/><span>{items.length} available</span></div>
    <div className="categoryRail guestRail"><button className={activeCategory==='all'?'active':''} onClick={()=>setActiveCategory('all')}>All</button>{categories.map(c=><button className={String(activeCategory)===String(c.id)?'active':''} onClick={()=>setActiveCategory(c.id)} key={c.id}>{c.name}</button>)}</div>
    <div className="foodCardGrid guestFoodGrid">{items.map(i=><article className="card foodCard guestFoodCard" key={i.id}><div className="foodPhoto">{i.imageUrl?<img src={i.imageUrl} alt="" onError={e=>{e.currentTarget.style.display='none'}}/>:<span>{i.foodType==='VEG'?'🌿':'🍽️'}</span>}</div><div className="foodInfo"><span className="eyebrow">{i.categoryName} · {i.foodType} · {i.spiceLevel}</span><h3>{i.name}</h3><p>{i.description}</p><div className="foodMeta"><b>{money(i.price)}</b><small>{i.preparationTimeMinutes} min</small></div></div><button className="round" onClick={()=>dispatch({type:'ADD',item:i})}>+</button></article>)}</div>
    {cart.length>0&&<button className="cartBar" onClick={()=>nav(`/guest/${token}/checkout`,{state:{cart}})}><span>{cart.reduce((n,x)=>n+x.quantity,0)} items</span><b>Review order · {money(cart.reduce((n,x)=>n+x.price*x.quantity,0))}</b></button>}
  </div>
}

export function GuestCheckout(){
  const {qrToken}=useParams()
  const nav=useNavigate()
  const cart=history.state?.usr?.cart||[]
  const total=cart.reduce((sum,item)=>sum+item.price*item.quantity,0)
  const [checkoutResponse,setCheckoutResponse]=useState(null)

  const paymentMutation=useMutation({
    mutationFn:async orderRequest=>{
      let response=checkoutResponse

      if(!response){
        response=await guestApi.order(qrToken,orderRequest)
        setCheckoutResponse(response)
      }

      await loadRazorpayScript()
      return openRazorpayCheckout(response,orderRequest)
    },
    onSuccess:response=>{
      sessionStorage.removeItem(`smartserve.cart.${qrToken}`)
      nav(`/guest/order/${response.trackingToken}`,{replace:true})
    }
  })

  if(!cart.length)return <Navigate to={`/guest/${qrToken}/menu`} replace/>

  function handleSubmit(event){
    event.preventDefault()
    const form=new FormData(event.currentTarget)

    paymentMutation.mutate({
      customerName:form.get('name'),
      customerPhone:normalizePhone(form.get('phone')),
      smsConsent:form.get('sms')==='on',
      specialInstructions:form.get('notes')||null,
      items:cart.map(item=>({menuItemId:item.id,quantity:item.quantity}))
    })
  }

  return <div className="guest checkout">
    <button className="back" onClick={()=>nav(-1)}>Back to menu</button>
    <h1>Almost there</h1>
    <div className="card">
      <h3>Your order</h3>
      {cart.map(item=><div className="line" key={item.id}><span>{item.quantity}x {item.name}</span><b>{money(item.quantity*item.price)}</b></div>)}
    </div>
    <form className="card" onSubmit={handleSubmit}>
      <label>Your name<input name="name" required maxLength="100"/></label>
      <label>Phone (optional)<input name="phone" placeholder="+91..."/></label>
      <label>Kitchen note<textarea name="notes" maxLength="500"/></label>
      <label className="check"><input type="checkbox" name="sms"/> Send me SMS updates about this order</label>
      {paymentMutation.error&&<div className="alert">{paymentMutation.error.message}</div>}
      <button className="primary" disabled={paymentMutation.isPending}>
        {paymentMutation.isPending?'Waiting for payment...':`Pay and place order - ${money(total)}`}
      </button>
    </form>
  </div>
}
export function GuestTracking(){const {trackingToken}=useParams(),q=useQuery({queryKey:['track',trackingToken],queryFn:()=>guestApi.track(trackingToken),refetchInterval:data=>['SERVED','CANCELLED'].includes(data.state.data?.orderStatus)?false:5000});if(q.isLoading)return <GuestState text="Finding your order..."/>;if(q.error)return <GuestState text={q.error.message}/>;const o=q.data;return <div className="guest tracking"><div className="logo">SS</div><span className="eyebrow">ORDER #{o.id}</span><h1>{statusLabel[o.orderStatus]}</h1><p>{o.orderStatus==='READY'?'Your food is ready - your server will bring it shortly.':'This page updates automatically.'}</p><div className="steps">{['PENDING','PREPARING','READY','SERVED'].map((s,i)=><div className={i<=['PENDING','PREPARING','READY','SERVED'].indexOf(o.orderStatus)?'done':''} key={s}><i/><span>{statusLabel[s]}</span></div>)}</div><div className="card"><h3>Order summary</h3>{o.items.map(i=><div className="line" key={i.id}><span>{i.quantity}x {i.itemName}</span><b>{money(i.lineTotal)}</b></div>)}<div className="line total"><span>Total</span><b>{money(o.totalAmount)}</b></div></div></div>}
function GuestState({text}){return <main className="center guestState"><div className="logo">SS</div><div className="spinner"/><h2>{text}</h2></main>}
