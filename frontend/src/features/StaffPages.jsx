import {useMemo,useState} from 'react'
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
const Select=({label,children,...props})=><label>{label}<select {...props}>{children}</select></label>
const dateInput=date=>date.toISOString().slice(0,10)
const startOfDayIso=value=>new Date(`${value}T00:00:00`).toISOString()
const endOfDayIso=value=>new Date(`${value}T23:59:59`).toISOString()

export function Dashboard({manager=false}){
  const {user}=useAuth()
  const branchParam=manager&&user.branchId?`?branchId=${user.branchId}`:''
  const paths=manager?[`/orders${branchParam}`,`/kitchen/tickets${branchParam}`]:['/restaurants','/users']
  const q=useQuery({queryKey:['dashboard',manager,user.branchId||'all'],queryFn:()=>Promise.all(paths.map(path=>api.get(path))),refetchInterval:manager?5000:false})
  const data=q.data||[[],[]]
  const activeOrders=manager&&Array.isArray(data[0])?data[0].filter(o=>!['SERVED','CANCELLED'].includes(o.orderStatus)).length:Array.isArray(data[0])?data[0].length:data[0]?.tickets?.length||0
  const kitchenTickets=manager?(data[1]?.tickets?.length||0):Array.isArray(data[1])?data[1].length:data[1]?.tickets?.length||0
  return <Page title={manager?'Manager overview':'Good service starts here'} subtitle="A live pulse of your operation.">
    {q.isLoading?<Loader/>:q.error?<ErrorState error={q.error}/>:<div className="stats">
      <Stat label={manager?'Active orders':'Restaurants'} value={activeOrders}/>
      <Stat label={manager?'Kitchen tickets':'Team members'} value={kitchenTickets}/>
      <Stat label="System status" value="Online" tone="good"/>
    </div>}
  </Page>
}

export function MenuPage(){
  const [activeCategory,setActiveCategory]=useState('all')
  const [search,setSearch]=useState('')
  const items=useQuery({queryKey:menuKeys.items(),queryFn:()=>api.get('/menu/items')})
  const cats=useQuery({queryKey:menuKeys.categories(),queryFn:()=>api.get('/menu/categories?activeOnly=true')})
  const visible=(items.data||[]).filter(i=>(activeCategory==='all'||String(i.categoryId)===String(activeCategory))&&(`${i.name} ${i.description||''}`.toLowerCase().includes(search.toLowerCase())))
  const categories=(cats.data||[]).filter(c=>(items.data||[]).some(i=>String(i.categoryId)===String(c.id)))
  return <Page title="Menu" subtitle="Browse dishes by category, with availability and prep details.">
    {items.isLoading?<Loader/>:items.error?<ErrorState error={items.error}/>:<>
      <div className="card orderToolbar menuBrowseToolbar"><input className="menuSearch" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search the menu…"/><span>{visible.length} items</span></div>
      <div className="categoryRail"><button className={activeCategory==='all'?'active':''} onClick={()=>setActiveCategory('all')}>All</button>{categories.map(c=><button className={String(activeCategory)===String(c.id)?'active':''} onClick={()=>setActiveCategory(c.id)} key={c.id}>{c.name}</button>)}</div>
      <div className="foodCardGrid">{visible.map(item=><article className="card foodCard" key={item.id}><div className="foodPhoto">{item.imageUrl?<img src={item.imageUrl} alt="" onError={e=>{e.currentTarget.style.display='none'}}/>:<span>{item.foodType==='VEG'?'🌿':'🍽️'}</span>}</div><div className="foodInfo"><span className="eyebrow">{item.categoryName} · {item.foodType} · {item.spiceLevel}</span><h3>{item.name}</h3><p>{item.description}</p><div className="foodMeta"><b>{money(item.price)}</b><small>{item.preparationTimeMinutes} min</small><span className={`badge ${item.available?'green':'gray'}`}>{item.available?'Available':'Unavailable'}</span></div></div></article>)}</div>
    </>}
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
  const {user}=useAuth()
  const needsBranch=['ADMIN','MANAGER'].includes(user.role)
  const [restaurant,setRestaurant]=useState('')
  const [selectedBranch,setSelectedBranch]=useState(user.branchId||'')
  const branchId=needsBranch?(user.branchId||Number(selectedBranch)||null):null
  const restaurants=useQuery({queryKey:['restaurants','kitchen'],queryFn:()=>api.get('/restaurants'),enabled:needsBranch&&!user.branchId})
  const branches=useQuery({queryKey:['branches','kitchen',restaurant],queryFn:()=>api.get(`/restaurants/${restaurant}/branches`),enabled:needsBranch&&!user.branchId&&Boolean(restaurant)})
  const queryString=branchId?`?branchId=${branchId}`:''
  const q=useQuery({queryKey:kitchenKeys.queue({branchId:branchId||'assigned'}),queryFn:()=>api.get(`/kitchen/tickets${queryString}`),refetchInterval:5000,enabled:!needsBranch||Boolean(branchId)})
  const qc=useQueryClient()
  const move=useMutation({mutationFn:({id,status})=>api.patch(`/kitchen/tickets/${id}/${status==='PENDING'?'start':'ready'}${queryString}`),onSettled:()=>qc.invalidateQueries({queryKey:['kitchen']})})
  const tickets=q.data?.tickets||q.data||[]
  return <Page title="Kitchen board" subtitle="New tickets on the left. Ready food on the right.">
    {needsBranch&&!user.branchId&&<div className="card branchChooser kitchenBranchChooser"><Select label="Restaurant" value={restaurant} onChange={e=>{setRestaurant(e.target.value);setSelectedBranch('')}}><option value="">Choose restaurant</option>{restaurants.data?.map(r=><option value={r.id} key={r.id}>{r.name}</option>)}</Select><Select label="Kitchen branch" value={selectedBranch} onChange={e=>setSelectedBranch(e.target.value)} disabled={!restaurant}><option value="">Choose branch</option>{branches.data?.map(b=><option value={b.id} key={b.id}>{b.name}</option>)}</Select></div>}
    {needsBranch&&!branchId?<div className="state">Choose a branch to open the kitchen recovery board.</div>:q.isLoading?<Loader/>:q.error?<ErrorState error={q.error}/>:<div className="board">
      {['PENDING','PREPARING','READY'].map(status=><section key={status}>
        <header><h3>{status==='PENDING'?'New':status[0]+status.slice(1).toLowerCase()}</h3><span>{tickets.filter(t=>(t.orderStatus||t.status)===status).length}</span></header>
        {tickets.filter(t=>(t.orderStatus||t.status)===status).length===0&&<div className="columnEmpty">No {status.toLowerCase()} tickets.</div>}
        {tickets.filter(t=>(t.orderStatus||t.status)===status).map(t=><article className="card ticket" key={t.orderId}>
          <b>#{t.orderId} · {t.tableNumber?`Table ${t.tableNumber}`:'Takeaway'}</b>
          <p>{t.items?.map(i=>`${i.quantity}× ${i.itemName}`).join(', ')}</p>
          {t.specialInstructions&&<div className="note">{t.specialInstructions}</div>}
          {status!=='READY'&&<button className="primary" disabled={move.isPending} onClick={()=>move.mutate({id:t.orderId,status})}>{status==='PENDING'?'Start prep':'Mark ready'}</button>}
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
  const [defaultRange]=useState(()=>{const now=new Date();return{to:dateInput(now),from:dateInput(new Date(now.getTime()-30*MS_PER_DAY))}})
  const {from:defaultFrom,to:defaultTo}=defaultRange
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