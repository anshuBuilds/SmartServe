import {Bell,CheckCheck,Clock,ExternalLink,Inbox} from 'lucide-react'
import {useMutation,useQuery,useQueryClient} from '@tanstack/react-query'
import {Link,useNavigate} from 'react-router-dom'
import {api} from '../api/client'
import {notificationKeys} from '../api/queryKeys'

const isUnread=n=>!n.readAt&&n.status!=='READ'
const formatTime=value=>value?new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short'}).format(new Date(value)):''

export function NotificationsPage(){
  const qc=useQueryClient()
  const navigate=useNavigate()
  const inbox=useQuery({queryKey:notificationKeys.inbox(),queryFn:()=>api.get('/notifications'),refetchInterval:30000})
  const markRead=useMutation({
    mutationFn:id=>api.patch(`/notifications/${id}/read`),
    onSuccess:()=>invalidateNotifications(qc),
  })
  const readAll=useMutation({
    mutationFn:()=>api.patch('/notifications/read-all'),
    onSuccess:()=>invalidateNotifications(qc),
  })

  const notifications=inbox.data||[]
  const unreadCount=notifications.filter(isUnread).length

  const openNotification=n=>{
    if(isUnread(n))markRead.mutate(n.id)
    if(n.orderId)navigate(`/app/orders/${n.orderId}`)
  }

  return <section>
    <header className="pageHeader notificationHero">
      <div>
        <span className="eyebrow">SMARTSERVE NOTIFICATIONS</span>
        <h1>Inbox</h1>
        <p>Operational alerts for orders, branches and service flow.</p>
      </div>
      <div className="notificationHeroActions">
        <span className="notificationCount"><Bell size={18}/>{unreadCount} unread</span>
        <button className="secondary" disabled={!unreadCount||readAll.isPending} onClick={()=>readAll.mutate()}>
          <CheckCheck size={17}/> Mark all read
        </button>
      </div>
    </header>

    {inbox.isLoading&&<div className="state"><div className="spinner"/>Loading notifications…</div>}
    {inbox.error&&<div className="state error">{inbox.error.message}</div>}
    {!inbox.isLoading&&!inbox.error&&!notifications.length&&<EmptyInbox/>}
    {!!notifications.length&&<div className="notificationList card">
      {notifications.map(n=><article className={`notificationRow ${isUnread(n)?'unread':''}`} key={n.id}>
        <div className="notificationIcon"><Bell size={18}/></div>
        <button className="notificationBody" onClick={()=>openNotification(n)}>
          <span className="notificationMeta">
            <b>{n.type?.replaceAll('_',' ')||'Notification'}</b>
            {n.branchName&&<span>{n.branchName}</span>}
            <span><Clock size={13}/>{formatTime(n.createdAt)}</span>
          </span>
          <strong>{n.title}</strong>
          <p>{n.message}</p>
          {n.errorMessage&&<small className="notificationError">{n.errorMessage}</small>}
        </button>
        <div className="notificationActions">
          {n.orderId&&<Link className="secondary" to={`/app/orders/${n.orderId}`}><ExternalLink size={15}/> Order #{n.orderId}</Link>}
          {isUnread(n)&&<button className="secondary" disabled={markRead.isPending} onClick={()=>markRead.mutate(n.id)}>Mark read</button>}
        </div>
      </article>)}
    </div>}
  </section>
}

function EmptyInbox(){
  return <div className="card emptyInbox">
    <Inbox size={42}/>
    <h2>No notifications yet</h2>
    <p>When orders are created, cancelled, ready, or need attention, they will appear here.</p>
  </div>
}

export function invalidateNotifications(queryClient){
  queryClient.invalidateQueries({queryKey:['notifications']})
}
