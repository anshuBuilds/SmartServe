import {ArrowLeft, ArrowRight, Check, X} from 'lucide-react'
import {useCallback, useEffect, useMemo, useState} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {roleHome} from '../lib/format'

const common = {
  notifications: {
    target: 'nav-notifications',
    path: '/app/notifications',
    title: 'Stay up to date',
    description: 'Open Notifications to see important order and restaurant updates in one place.',
  },
}

export const walkthroughs = {
  ADMIN: [
    {
      target: 'brand',
      title: 'Welcome to SmartServe',
      description:
        'This quick tour shows you the main admin tools. You can skip it now and replay it from your profile later.',
    },
    {
      target: 'nav-dashboard',
      path: '/app/admin',
      title: 'See the whole restaurant',
      description:
        'The dashboard gives you a quick view of orders, revenue, and restaurant activity.',
    },
    {
      target: 'nav-restaurants',
      path: '/app/restaurants',
      title: 'Set up restaurants',
      description: 'Manage restaurant and branch information from Restaurant setup.',
    },
    {
      target: 'nav-new-order',
      path: '/app/waiter/orders/new',
      title: 'Create an order',
      description: 'Use New order when you need to place an order on behalf of a guest.',
    },
    {
      target: 'nav-kitchen',
      path: '/app/kitchen',
      title: 'Watch kitchen work',
      description: 'The Kitchen view shows paid orders moving through preparation.',
    },
    {
      target: 'nav-manage-menu',
      path: '/app/menu/manage',
      title: 'Keep the menu current',
      description: 'Add dishes, update prices, availability, descriptions, and images here.',
    },
    {
      target: 'nav-users',
      path: '/app/users',
      title: 'Manage the team',
      description: 'Create staff accounts and assign the correct role and branch.',
    },
    {
      target: 'nav-analytics',
      path: '/app/analytics',
      title: 'Understand performance',
      description: 'Analytics helps you review sales and operational trends.',
    },
    common.notifications,
  ],
  MANAGER: [
    {
      target: 'brand',
      title: 'Welcome to SmartServe',
      description:
        'This quick tour shows the tools available to restaurant managers. You can skip it and replay it later.',
    },
    {
      target: 'nav-dashboard',
      path: '/app/manager',
      title: 'Start with the dashboard',
      description: 'Review the branch overview, current orders, and key restaurant activity.',
    },
    {
      target: 'nav-orders',
      path: '/app/waiter/orders',
      title: 'Follow every order',
      description: 'Orders gives you the current status and details of guest orders.',
    },
    {
      target: 'nav-new-order',
      path: '/app/waiter/orders/new',
      title: 'Place an order',
      description: 'Create an order for a guest directly from the staff application.',
    },
    {
      target: 'nav-kitchen',
      path: '/app/kitchen',
      title: 'Coordinate the kitchen',
      description: 'See paid orders and how they progress through preparation.',
    },
    {
      target: 'nav-manage-menu',
      path: '/app/menu/manage',
      title: 'Manage the menu',
      description: 'Keep dishes, prices, availability, and images accurate.',
    },
    {
      target: 'nav-analytics',
      path: '/app/analytics',
      title: 'Review performance',
      description: 'Use Analytics to understand sales and operational trends.',
    },
    common.notifications,
  ],
  WAITER: [
    {
      target: 'brand',
      title: 'Welcome to SmartServe',
      description:
        'This short tour shows the tools you will use while serving guests. You can skip it and replay it later.',
    },
    {
      target: 'nav-orders',
      path: '/app/waiter/orders',
      title: 'Track guest orders',
      description: 'See existing orders, their totals, and their current status.',
    },
    {
      target: 'nav-new-order',
      path: '/app/waiter/orders/new',
      title: 'Create a new order',
      description: 'Choose menu items and place an order for a guest from here.',
    },
    {
      target: 'nav-menu',
      path: '/app/menu',
      title: 'Browse the menu',
      description: 'Quickly check available dishes, prices, and preparation times.',
    },
    common.notifications,
  ],
  KITCHEN: [
    {
      target: 'brand',
      title: 'Welcome to SmartServe',
      description:
        'This short tour shows your kitchen workflow. You can skip it and replay it later.',
    },
    {
      target: 'nav-kitchen',
      path: '/app/kitchen',
      title: 'Work the live queue',
      description: 'Paid orders appear here. Update each order as the kitchen prepares it.',
    },
    {
      target: 'nav-kitchen-history',
      path: '/app/kitchen/history',
      title: 'Review completed work',
      description: 'Kitchen history keeps previous orders available for reference.',
    },
    {
      target: 'nav-menu',
      path: '/app/menu',
      title: 'Check menu details',
      description: 'Browse dishes, ingredients, prices, and preparation times.',
    },
    common.notifications,
  ],
}

function storageKey(user) {
  const identity = user.id ?? user.username ?? 'staff'
  return `smartserve.walkthrough.v1.${identity}.${user.role}`
}

function getTargetRect(target) {
  const element = document.querySelector(`[data-tour="${target}"]`)
  if (!element) return null
  const rect = element.getBoundingClientRect()
  return rect.width && rect.height
    ? {top: rect.top - 6, left: rect.left - 6, width: rect.width + 12, height: rect.height + 12}
    : null
}

export function RoleWalkthrough({user, onActiveChange, onNavigationStep}) {
  const navigate = useNavigate()
  const location = useLocation()
  const steps = walkthroughs[user.role] ?? []
  const key = useMemo(() => storageKey(user), [user])
  const [active, setActive] = useState(() => steps.length > 0 && !localStorage.getItem(key))
  const [stepIndex, setStepIndex] = useState(0)
  const [rect, setRect] = useState(null)
  const step = steps[stepIndex]

  const close = useCallback(
    status => {
      localStorage.setItem(key, JSON.stringify({status, finishedAt: new Date().toISOString()}))
      setActive(false)
      onNavigationStep?.(false)
      setStepIndex(0)
      navigate(roleHome[user.role] ?? '/login')
    },
    [key, navigate, onNavigationStep, user.role],
  )

  useEffect(() => {
    const replay = () => {
      setStepIndex(0)
      setActive(true)
    }
    window.addEventListener('smartserve:replay-walkthrough', replay)
    return () => window.removeEventListener('smartserve:replay-walkthrough', replay)
  }, [])

  useEffect(() => {
    onActiveChange?.(active)
    return () => onActiveChange?.(false)
  }, [active, onActiveChange])

  useEffect(() => {
    if (!active || !step) return
    if (step.path && location.pathname !== step.path) navigate(step.path)
    onNavigationStep?.(Boolean(step.target))
  }, [active, location.pathname, navigate, onNavigationStep, step])

  useEffect(() => {
    if (!active || !step) return
    const update = () => setRect(getTargetRect(step.target))
    const frame = requestAnimationFrame(update)
    const timer = setTimeout(update, 180)
    window.addEventListener('resize', update)
    window.addEventListener('scroll', update, true)
    return () => {
      cancelAnimationFrame(frame)
      clearTimeout(timer)
      window.removeEventListener('resize', update)
      window.removeEventListener('scroll', update, true)
    }
  }, [active, location.pathname, step])

  useEffect(() => {
    if (!active) return
    const onKeyDown = event => {
      if (event.key === 'Escape') close('skipped')
      if (event.key === 'ArrowRight' && stepIndex < steps.length - 1)
        setStepIndex(current => current + 1)
      if (event.key === 'ArrowLeft' && stepIndex > 0) setStepIndex(current => current - 1)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [active, close, stepIndex, steps.length])

  if (!active || !step) return null

  const isLast = stepIndex === steps.length - 1
  return (
    <div
      className="walkthrough"
      role="dialog"
      aria-modal="true"
      aria-labelledby="walkthrough-title"
    >
      <div className="walkthroughBlocker" />
      {rect && <div className="walkthroughSpotlight" style={rect} />}
      <section className="walkthroughPanel">
        <div className="walkthroughTopline">
          <span>{user.role} TOUR</span>
          <button
            className="walkthroughClose"
            onClick={() => close('skipped')}
            aria-label="Skip walkthrough"
            title="Skip walkthrough"
          >
            <X />
          </button>
        </div>
        <div
          className="walkthroughProgress"
          aria-label={`Step ${stepIndex + 1} of ${steps.length}`}
        >
          <span style={{width: `${((stepIndex + 1) / steps.length) * 100}%`}} />
        </div>
        <small>
          Step {stepIndex + 1} of {steps.length}
        </small>
        <h2 id="walkthrough-title">{step.title}</h2>
        <p>{step.description}</p>
        <div className="walkthroughActions">
          <button className="textButton" onClick={() => close('skipped')}>
            Skip tour
          </button>
          <div>
            {stepIndex > 0 && (
              <button
                className="secondaryButton"
                onClick={() => setStepIndex(current => current - 1)}
              >
                <ArrowLeft />
                Back
              </button>
            )}
            <button
              className="primaryButton"
              onClick={() => (isLast ? close('completed') : setStepIndex(current => current + 1))}
            >
              {isLast ? (
                <>
                  <Check />
                  Done
                </>
              ) : (
                <>
                  Next
                  <ArrowRight />
                </>
              )}
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
