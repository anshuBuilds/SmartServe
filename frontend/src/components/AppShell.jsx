import {
  BarChart3,
  Bell,
  ChefHat,
  CircleHelp,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Menu as MenuIcon,
  Store,
  Users,
  X,
} from 'lucide-react'
import {NavLink, Outlet} from 'react-router-dom'
import {useState} from 'react'
import {useAuth} from '../auth/AuthProvider'
import {NotificationBell} from './NotificationBell'
import {RoleWalkthrough} from './RoleWalkthrough'

const links = [
  ['Dashboard', '/app/admin', LayoutDashboard, ['ADMIN'], 'nav-dashboard'],
  ['Dashboard', '/app/manager', LayoutDashboard, ['MANAGER'], 'nav-dashboard'],
  ['Restaurant setup', '/app/restaurants', Store, ['ADMIN'], 'nav-restaurants'],
  ['Orders', '/app/waiter/orders', ClipboardList, ['ADMIN', 'MANAGER', 'WAITER'], 'nav-orders'],
  [
    'New order',
    '/app/waiter/orders/new',
    ClipboardList,
    ['ADMIN', 'MANAGER', 'WAITER'],
    'nav-new-order',
  ],
  ['Kitchen', '/app/kitchen', ChefHat, ['ADMIN', 'MANAGER', 'KITCHEN'], 'nav-kitchen'],
  [
    'Kitchen history',
    '/app/kitchen/history',
    ChefHat,
    ['ADMIN', 'MANAGER', 'KITCHEN'],
    'nav-kitchen-history',
  ],
  ['Browse menu', '/app/menu', Store, ['WAITER', 'KITCHEN'], 'nav-menu'],
  ['Manage menu', '/app/menu/manage', Store, ['ADMIN', 'MANAGER'], 'nav-manage-menu'],
  ['Users', '/app/users', Users, ['ADMIN'], 'nav-users'],
  ['Analytics', '/app/analytics', BarChart3, ['ADMIN', 'MANAGER'], 'nav-analytics'],
  [
    'Notifications',
    '/app/notifications',
    Bell,
    ['ADMIN', 'MANAGER', 'WAITER', 'KITCHEN'],
    'nav-notifications',
  ],
]

export function AppShell() {
  const {user, logout} = useAuth()
  const [open, setOpen] = useState(false)
  const [tourRunning, setTourRunning] = useState(false)
  const initials = user.fullName?.[0] || user.username?.[0] || 'S'

  return (
    <div className={`shell${tourRunning ? ' tourRunning' : ''}`}>
      <header className="mobileHeader">
        <button className="icon" onClick={() => setOpen(true)} aria-label="Open menu">
          <MenuIcon />
        </button>
        <strong>SmartServe</strong>
        <div className="mobileTools">
          <NotificationBell />
        </div>
      </header>

      <aside className={open ? 'open' : ''}>
        <div className="brand" data-tour="brand">
          <span>SS</span>
          <div>
            <b>SmartServe</b>
            <small>Restaurant OS</small>
          </div>
          <button className="icon close" onClick={() => setOpen(false)} aria-label="Close menu">
            <X />
          </button>
        </div>
        <nav>
          {links
            .filter(x => x[3].includes(user.role))
            .map(([label, to, Icon, , tour]) => (
              <NavLink key={to} to={to} end onClick={() => setOpen(false)} data-tour={tour}>
                <Icon />
                {label}
              </NavLink>
            ))}
        </nav>
        <div className="profile staffCard" data-tour="profile">
          <div className="avatar">{initials}</div>
          <div className="profileMeta">
            <b title={user.fullName || user.username}>{user.fullName || user.username}</b>
            <span className="rolePill">{user.role}</span>
            {user.branchName && <small title={user.branchName}>{user.branchName}</small>}
          </div>
          <div className="profileActions">
            <button
              className="icon"
              onClick={() => window.dispatchEvent(new Event('smartserve:replay-walkthrough'))}
              title="Replay walkthrough"
              aria-label="Replay walkthrough"
            >
              <CircleHelp />
            </button>
            <button
              className="icon logoutButton"
              onClick={logout}
              title="Log out"
              aria-label="Log out"
            >
              <LogOut />
            </button>
          </div>
        </div>
      </aside>

      <main className="content">
        <div className="desktopTopbar">
          <div>
            <span className="eyebrow">SIGNED IN</span>
            <b>{user.fullName || user.username}</b>
            <small>
              {user.role}
              {user.branchName ? ` · ${user.branchName}` : ''}
            </small>
          </div>
          <NotificationBell />
        </div>
        <Outlet />
      </main>
      {open && <button className="scrim" onClick={() => setOpen(false)} aria-label="Close menu" />}
      <RoleWalkthrough
        user={user}
        onActiveChange={setTourRunning}
        onNavigationStep={showMenu => setOpen(showMenu && window.innerWidth <= 800)}
      />
    </div>
  )
}
