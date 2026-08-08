import {Bell, CheckCheck, ExternalLink} from 'lucide-react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Link, useNavigate} from 'react-router-dom'
import {useState} from 'react'
import {api} from '../api/client'
import {notificationKeys} from '../api/queryKeys'
import {invalidateNotifications} from '../features/Notifications'

const notificationsEnabled = import.meta.env.VITE_ENABLE_NOTIFICATIONS !== 'false'
const isUnread = n => !n.readAt && n.status !== 'READ'
const shortTime = value =>
  value
    ? new Intl.DateTimeFormat(undefined, {hour: '2-digit', minute: '2-digit'}).format(
        new Date(value),
      )
    : ''

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const qc = useQueryClient()
  const navigate = useNavigate()
  const count = useQuery({
    queryKey: notificationKeys.unreadCount,
    queryFn: () => api.get('/notifications/unread-count'),
    refetchInterval: 10000,
    enabled: notificationsEnabled,
  })
  const inbox = useQuery({
    queryKey: notificationKeys.inbox({preview: true}),
    queryFn: () => api.get('/notifications'),
    refetchInterval: 30000,
    enabled: notificationsEnabled && open,
  })
  const markRead = useMutation({
    mutationFn: id => api.patch(`/notifications/${id}/read`),
    onSuccess: () => invalidateNotifications(qc),
  })
  const readAll = useMutation({
    mutationFn: () => api.patch('/notifications/read-all'),
    onSuccess: () => invalidateNotifications(qc),
  })

  if (!notificationsEnabled) return null

  const unread = Number(count.data || 0)
  const preview = (inbox.data || []).slice(0, 5)
  const openNotification = n => {
    if (isUnread(n)) markRead.mutate(n.id)
    setOpen(false)
    if (n.orderId) navigate(`/app/orders/${n.orderId}`)
    else navigate('/app/notifications')
  }

  return (
    <div className="notificationBell">
      <button
        className={`bellButton ${unread ? 'hasUnread' : ''}`}
        aria-label={`Notifications${unread ? `: ${unread} unread` : ''}`}
        onClick={() => setOpen(v => !v)}
      >
        <Bell size={20} />
        {!!unread && <span>{unread > 99 ? '99+' : unread}</span>}
      </button>
      {open && (
        <>
          <button
            className="notificationBackdrop"
            aria-label="Close notifications"
            onClick={() => setOpen(false)}
          />
          <section className="notificationPanel card">
            <header>
              <div>
                <b>Notifications</b>
                <small>{unread ? `${unread} unread` : 'All caught up'}</small>
              </div>
              <button
                className="icon"
                disabled={!unread || readAll.isPending}
                title="Mark all read"
                onClick={() => readAll.mutate()}
              >
                <CheckCheck size={18} />
              </button>
            </header>
            <div className="notificationPreviewList">
              {inbox.isLoading && <div className="state compactState">Loading…</div>}
              {inbox.error && <div className="state error compactState">{inbox.error.message}</div>}
              {!inbox.isLoading && !preview.length && (
                <div className="compactEmpty">No notifications yet.</div>
              )}
              {preview.map(n => (
                <button
                  className={`notificationMini ${isUnread(n) ? 'unread' : ''}`}
                  key={n.id}
                  onClick={() => openNotification(n)}
                >
                  <span>
                    {n.type?.replaceAll('_', ' ') || 'Notification'} · {shortTime(n.createdAt)}
                  </span>
                  <b>{n.title}</b>
                  <small>{n.message}</small>
                </button>
              ))}
            </div>
            <Link
              className="notificationFooter"
              to="/app/notifications"
              onClick={() => setOpen(false)}
            >
              View full inbox <ExternalLink size={14} />
            </Link>
          </section>
        </>
      )}
    </div>
  )
}
