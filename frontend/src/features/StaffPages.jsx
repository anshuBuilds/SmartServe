import {useMemo} from 'react'
import {useQuery,useMutation,useQueryClient} from '@tanstack/react-query'
import {useSearchParams} from 'react-router-dom'
import {api} from '../api/client'
import {analyticsKeys, kitchenKeys, menuKeys, orderKeys} from '../api/queryKeys'
import {money} from '../lib/format'
import {useAuth} from '../auth/AuthProvider'
import {BarChart,Bar,XAxis,YAxis,Tooltip,ResponsiveContainer,PieChart,Pie,Cell,Legend} from 'recharts'

const COLORS=['#d45b32','#173c36','#e3a33b','#6c8ebf','#7a7f86']
const MS_PER_DAY=86400000
const Loader=()=> <div className="state"><div className="spinner"/>Loading live data…</div>
const ErrorState=({error})=><div className="state error">{error.message}</div>
const dateInput=date=>date.toISOString().slice(0,10)
const startOfDayIso=value=>new Date(`${value}T00:00:00`).toISOString()
const endOfDayIso=value=>new Date(`${value}T23:59:59`).toISOString()

export function Dashboard({manager=false}){
  const paths=manager?['/orders','/kitchen/tickets']:['/restaurants','/users']
  const q=useQuery({queryKey:['dashboard',manager],queryFn:()=>Promise.all(paths.map(p=>api.get(p)))})
  const data=q.data||[[],[]]
  return <Page title={manager?'Manager overview':'Good service starts here'} subtitle="A live pulse of your operation.">
    {q.isLoading?<Loader/>:<div className="stats">
      <Stat label={manager?'Active orders':'Restaurants'} value={Array.isArray(data[0])?data[0].length:data[0]?.tickets?.length||0}/>
      <Stat label={manager?'Kitchen tickets':'Team members'} value={Array.isArray(data[1])?data[1].length:data[1]?.tickets?.length||0}/>
      <Stat label="System status" value="Online" tone="good"/>
    </div>}
  </Page>
}

export function MenuPage(){
  const q=useQuery({queryKey:menuKeys.items(),queryFn:()=>api.get('/menu/items')})
  return <Page title="Menu" subtitle="Everything guests can order.">
    {q.isLoading?<Loader/>:q.error?<ErrorState error={q.error}/>:<div className="menuGrid">
      {q.data.map(item=><article className="card menuItem" key={item.id}>
        <div className="foodIcon">{item.foodType==='VEG'?'?':'?'}</div>
        <div><span className="eyebrow">{item.foodType} · {item.spiceLevel}</span><h3>{item.name}</h3><p>{item.description}</p><b>{money(item.price)}</b></div>
        <span className={`badge ${item.available?'green':'gray'}`}>{item.available?'Available':'Unavailable'}</span>
      </article>)}
    </div>}
  </Page>
}

export function OrdersPage(){
  const {user}=useAuth()
  const filters={branchId:user.branchId||null}
  const url=user.branchId?`/orders?branchId=${user.branchId}`:'/orders'
  const q=useQuery({queryKey:orderKeys.list(filters),queryFn:()=>api.get(url),refetchInterval:5000})
  const qc=useQueryClient()
  const serve=useMutation({mutationFn:id=>api.patch(`/orders/${id}/serve`),onSuccess:()=>qc.invalidateQueries({queryKey:['orders']})})
  return <Page title="Orders" subtitle="Live floor activity, refreshed every five seconds.">
    {q.isLoading?<Loader/>:q.error?<ErrorState error={q.error}/>:<div className="cards">
      {q.data.length?q.data.map(o=><article className="card order" key={o.id}>
        <div><span className={`badge status-${o.orderStatus}`}>{o.orderStatus}</span><h3>Order #{o.id}</h3><p>{o.tableNumber?`Table ${o.tableNumber}`:'Takeaway'} · {o.customerName}</p></div>
        <div><b>{money(o.totalAmount)}</b><small>{o.items.length} items</small>{o.orderStatus==='READY'&&<button className="primary compact" onClick={()=>serve.mutate(o.id)}>Serve</button>}</div>
      </article>):<div className="state">No orders in this view.</div>}
    </div>}
  </Page>
}

export function KitchenPage(){
  const q=useQuery({queryKey:kitchenKeys.queue(),queryFn:()=>api.get('/kitchen/tickets'),refetchInterval:5000})
  const qc=useQueryClient()
  const move=useMutation({mutationFn:({id,status})=>api.patch(`/kitchen/tickets/${id}/${status==='PENDING'?'start':'ready'}`),onSettled:()=>qc.invalidateQueries({queryKey:['kitchen']})})
  const tickets=q.data?.tickets||q.data||[]
  return <Page title="Kitchen board" subtitle="New tickets on the left. Ready food on the right.">
    {q.isLoading?<Loader/>:q.error?<ErrorState error={q.error}/>:<div className="board">
      {['PENDING','PREPARING','READY'].map(status=><section key={status}>
        <header><h3>{status==='PENDING'?'New':status[0]+status.slice(1).toLowerCase()}</h3><span>{tickets.filter(t=>(t.orderStatus||t.status)===status).length}</span></header>
        {tickets.filter(t=>(t.orderStatus||t.status)===status).map(t=><article className="card ticket" key={t.orderId}>
          <b>#{t.orderId} · {t.tableNumber?`Table ${t.tableNumber}`:'Takeaway'}</b>
          <p>{t.items?.map(i=>`${i.quantity}× ${i.itemName}`).join(', ')}</p>
          {t.specialInstructions&&<div className="note">{t.specialInstructions}</div>}
          {status!=='READY'&&<button className="primary" disabled={move.isPending} onClick={()=>move.mutate({id:t.orderId,status})}>{status==='PENDING'?'Start preparation':'Mark ready'}</button>}
        </article>)}
      </section>)}
    </div>}
  </Page>
}

export function UsersPage(){
  const q=useQuery({queryKey:['users'],queryFn:()=>api.get('/users')})
  return <Page title="Team" subtitle="Roles, branches and account status.">
    {q.isLoading?<Loader/>:<div className="tableWrap"><table><thead><tr><th>Name</th><th>Username</th><th>Role</th><th>Branch</th><th>Status</th></tr></thead><tbody>
      {q.data?.map(u=><tr key={u.id}><td>{u.fullName}</td><td>{u.username}</td><td><span className="badge">{u.role}</span></td><td>{u.branchName||'All branches'}</td><td><span className={`badge ${u.active?'green':'gray'}`}>{u.active?'Active':'Inactive'}</span></td></tr>)}
    </tbody></table></div>}
  </Page>
}

export function AnalyticsPage(){
  const [params,setParams]=useSearchParams()
  const defaultTo=dateInput(new Date())
  const defaultFrom=dateInput(new Date(Date.now()-30*MS_PER_DAY))
  const from=params.get('from')||defaultFrom
  const to=params.get('to')||defaultTo
  const filters=useMemo(()=>({from:startOfDayIso(from),to:endOfDayIso(to)}),[from,to])
  const qs=new URLSearchParams(filters).toString()
  const q=useQuery({
    queryKey:analyticsKeys.dashboard(filters),
    queryFn:async()=>{
      const [summary,statuses,items,tables]=await Promise.all([
        api.get(`/analytics/sales-summary?${qs}`),
        api.get(`/analytics/orders-by-status?${qs}`),
        api.get(`/analytics/top-items?${qs}&limit=8`),
        api.get(`/analytics/table-performance?${qs}`),
      ])
      return{summary,statuses:statuses||[],items:items||[],tables:tables||[]}
    },
  })

  const updateRange=next=>setParams({from:next.from||from,to:next.to||to})

  if(q.isLoading)return <Page title="Analytics"><Loader/></Page>
  if(q.error)return <Page title="Analytics"><ErrorState error={q.error}/></Page>

  const {summary,statuses,items,tables}=q.data
  const revenue=summary.totalRevenue??summary.revenue??0
  const totalOrders=summary.totalOrders??summary.orderCount??0
  const servedOrders=summary.servedOrders??summary.servedOrderCount??0
  const cancelledOrders=summary.cancelledOrders??summary.cancelledOrderCount??0
  const averageOrderValue=summary.averageOrderValue??summary.avgOrderValue??0

  return <Page title="Analytics" subtitle="Sales, item movement and table performance for the selected range.">
    <div className="card filterBar">
      <label>From<input type="date" value={from} onChange={e=>updateRange({from:e.target.value})}/></label>
      <label>To<input type="date" value={to} onChange={e=>updateRange({to:e.target.value})}/></label>
      <button className="secondary" type="button" onClick={()=>setParams({from:defaultFrom,to:defaultTo})}>Last 30 days</button>
    </div>
    <div className="stats">
      <Stat label="Revenue" value={money(revenue)}/>
      <Stat label="Average order" value={money(averageOrderValue)}/>
      <Stat label="Total orders" value={totalOrders}/>
      <Stat label="Served" value={servedOrders} tone="good"/>
      <Stat label="Cancelled" value={cancelledOrders}/>
    </div>
    <div className="charts">
      <article className="card chart">
        <h2>Orders by status</h2>
        {statuses.length?<ResponsiveContainer width="100%" height={280}><PieChart><Pie data={statuses} dataKey="orderCount" nameKey="orderStatus" outerRadius={90} label>{statuses.map((_,i)=><Cell key={i} fill={COLORS[i%COLORS.length]}/>)}</Pie><Tooltip/><Legend/></PieChart></ResponsiveContainer>:<div className="state">No order data for this range.</div>}
      </article>
      <article className="card chart">
        <h2>Top menu items</h2>
        {items.length?<ResponsiveContainer width="100%" height={280}><BarChart data={items} layout="vertical" margin={{left:12,right:24}}><XAxis type="number"/><YAxis dataKey="itemName" type="category" width={130}/><Tooltip formatter={(value,name)=>name?.toLowerCase().includes('revenue')?money(value):value}/><Bar dataKey="quantitySold" fill="#d45b32" radius={[0,6,6,0]}/></BarChart></ResponsiveContainer>:<div className="state">No item sales for this range.</div>}
      </article>
    </div>
    <div className="tableWrap"><table><thead><tr><th>Table</th><th>Served orders</th><th>Revenue</th><th>Average order</th></tr></thead><tbody>
      {tables.length?tables.map((t,i)=><tr key={t.tableId||i}><td>{t.tableNumber}</td><td>{t.servedOrders||t.orderCount}</td><td>{money(t.revenue||t.totalRevenue)}</td><td>{money(t.averageOrderValue)}</td></tr>):<tr><td colSpan="4">No table performance data for this range.</td></tr>}
    </tbody></table></div>
  </Page>
}

function Page({title,subtitle,children}){return <><header className="pageHeader"><div><span className="eyebrow">SMARTSERVE OPERATIONS</span><h1>{title}</h1><p>{subtitle}</p></div><span className="live"><i/>Live</span></header>{children}</>}
function Stat({label,value,tone}){return <article className={`card stat ${tone||''}`}><span>{label}</span><strong>{value??0}</strong></article>}