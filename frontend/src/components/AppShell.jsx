import {BarChart3,Bell,ChefHat,ClipboardList,LayoutDashboard,LogOut,Menu as MenuIcon,Store,Users,X} from 'lucide-react'
import {NavLink,Outlet} from 'react-router-dom'
import {useState} from 'react'
import {useAuth} from '../auth/AuthProvider'
import {NotificationBell} from './NotificationBell'

const links=[
  ['Dashboard','/app/admin',LayoutDashboard,['ADMIN']],
  ['Dashboard','/app/manager',LayoutDashboard,['MANAGER']],
  ['Restaurant setup','/app/restaurants',Store,['ADMIN']],
  ['Orders','/app/waiter/orders',ClipboardList,['ADMIN','MANAGER','WAITER']],
  ['New order','/app/waiter/orders/new',ClipboardList,['ADMIN','MANAGER','WAITER']],
  ['Kitchen','/app/kitchen',ChefHat,['ADMIN','MANAGER','KITCHEN']],
  ['Kitchen history','/app/kitchen/history',ChefHat,['ADMIN','MANAGER','KITCHEN']],
  ['Browse menu','/app/menu',Store,['WAITER','KITCHEN']],
  ['Manage menu','/app/menu/manage',Store,['ADMIN','MANAGER']],
  ['Users','/app/users',Users,['ADMIN']],
  ['Analytics','/app/analytics',BarChart3,['ADMIN','MANAGER']],
  ['Notifications','/app/notifications',Bell,['ADMIN','MANAGER','WAITER','KITCHEN']],
]

export function AppShell(){
  const {user,logout}=useAuth()
  const [open,setOpen]=useState(false)
  const initials=user.fullName?.[0]||user.username?.[0]||'S'

  return <div className="shell">
    <header className="mobileHeader">
      <button className="icon" onClick={()=>setOpen(true)} aria-label="Open menu"><MenuIcon/></button>
      <strong>SmartServe</strong>
      <div className="mobileTools"><NotificationBell/></div>
    </header>

    <aside className={open?'open':''}>
      <div className="brand">
        <span>SS</span>
        <div><b>SmartServe</b><small>Restaurant OS</small></div>
        <button className="icon close" onClick={()=>setOpen(false)}><X/></button>
      </div>
      <nav>{links.filter(x=>x[3].includes(user.role)).map(([label,to,Icon])=><NavLink key={to} to={to} onClick={()=>setOpen(false)}><Icon/>{label}</NavLink>)}</nav>
      <div className="profile">
        <div className="avatar">{initials}</div>
        <div><b>{user.fullName||user.username}</b><small>{user.role}{user.branchName?` · ${user.branchName}`:''}</small></div>
        <button className="icon" onClick={logout} title="Log out"><LogOut/></button>
      </div>
    </aside>

    <main className="content">
      <div className="desktopTopbar">
        <div><span className="eyebrow">SIGNED IN</span><b>{user.fullName||user.username}</b><small>{user.role}{user.branchName?` · ${user.branchName}`:''}</small></div>
        <NotificationBell/>
      </div>
      <Outlet/>
    </main>
    {open&&<button className="scrim" onClick={()=>setOpen(false)}/>} 
  </div>
}