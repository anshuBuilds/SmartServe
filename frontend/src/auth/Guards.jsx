import {Navigate,Outlet} from 'react-router-dom'
import {useAuth} from './AuthProvider'
export function RequireAuth(){const {user,loading}=useAuth();if(loading)return <main className="center"><div className="spinner"/>Restoring your session…</main>;return user?<Outlet/>:<Navigate to="/login" replace/>}
export function RequireRole({roles}){const {user}=useAuth();return roles.includes(user?.role)?<Outlet/>:<Navigate to="/forbidden" replace/>}
