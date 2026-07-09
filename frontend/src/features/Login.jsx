import {useState} from 'react'
import {useForm} from 'react-hook-form'
import {zodResolver} from '@hookform/resolvers/zod'
import {z} from 'zod'
import {Navigate,useNavigate} from 'react-router-dom'
import {useAuth} from '../auth/AuthProvider'
import {roleHome} from '../lib/format'
const schema=z.object({username:z.string().min(1,'Username is required'),password:z.string().min(1,'Password is required')})
export function Login(){const auth=useAuth(),navigate=useNavigate(),[error,setError]=useState('');const {register,handleSubmit,formState:{errors,isSubmitting}}=useForm({resolver:zodResolver(schema)});if(auth.user)return <Navigate to={roleHome[auth.user.role]} replace/>;return <main className="login"><section className="loginHero"><div><span className="eyebrow">SERVICE, IN SYNC</span><h1>Run every table.<br/>Delight every guest.</h1><p>Orders, kitchen flow and business insight—one calm place for the whole team.</p></div></section><section className="loginPanel"><form className="card loginCard" onSubmit={handleSubmit(async values=>{setError('');try{const user=await auth.login(values);navigate(roleHome[user.role]||'/app')}catch(e){setError(e.status===401?'Invalid username or password':e.message)}})}><div className="logo">SS</div><h2>Welcome back</h2><p>Sign in to your SmartServe workspace.</p><label>Username<input autoFocus autoComplete="username" {...register('username')}/><small className="error">{errors.username?.message}</small></label><label>Password<input type="password" autoComplete="current-password" {...register('password')}/><small className="error">{errors.password?.message}</small></label>{error&&<div className="alert">{error}</div>}<button className="primary" disabled={isSubmitting}>{isSubmitting?'Signing in…':'Sign in'}</button></form></section></main>}
