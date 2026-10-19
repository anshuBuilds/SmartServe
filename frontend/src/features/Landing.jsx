import {useEffect, useState} from 'react'
import {Link} from 'react-router-dom'
import {
  ArrowRight,
  Bell,
  ChartColumn,
  Check,
  ChefHat,
  Clock,
  CreditCard,
  QrCode,
  ScanLine,
  ShieldCheck,
  Smartphone,
  Store,
  Users,
} from 'lucide-react'
import {useAuth} from '../auth/AuthProvider'
import {roleHome} from '../lib/format'
import './landing.css'

const STEPS = ['Received', 'Preparing', 'Ready', 'Served']

const FEATURES = [
  {
    icon: QrCode,
    title: 'QR table ordering',
    text: 'Every table gets its own QR code. Guests scan, browse photos and order. No app, no sign-up.',
  },
  {
    icon: CreditCard,
    title: 'UPI, cards & netbanking',
    text: 'Razorpay checkout built in. Orders reach the kitchen only after the payment is verified on the server.',
  },
  {
    icon: Smartphone,
    title: 'Live order tracking',
    text: 'Guests watch their meal move from received to served on their own phone.',
  },
  {
    icon: ChefHat,
    title: 'Kitchen display',
    text: 'A clean ticket queue for the kitchen. One tap to start, one tap when it is ready.',
  },
  {
    icon: Users,
    title: 'Roles for every shift',
    text: 'Admin, manager, waiter and kitchen logins, each seeing exactly what they need.',
  },
  {
    icon: ChartColumn,
    title: 'Sales analytics',
    text: 'Revenue, average order value, best sellers and busy hours, per branch.',
  },
  {
    icon: Store,
    title: 'Branches & tables',
    text: 'Run several branches, set table sizes and rotate QR codes whenever you need.',
  },
  {
    icon: Bell,
    title: 'Staff notifications',
    text: 'New, ready and cancelled orders show up for the right people straight away.',
  },
]

const TICKETS = [
  {id: 38, table: 'T5', wait: '9 min', items: ['Chicken Biryani', 'Raita']},
  {id: 39, table: 'T2', wait: '7 min', items: ['Paneer Tikka', 'Mango Lassi']},
  {id: 40, table: 'B1', wait: '5 min', items: ['Pav Bhaji']},
  {id: 41, table: 'T3', wait: '4 min', items: ['Butter Chicken', 'Garlic Naan ×2']},
  {id: 42, table: 'T1', wait: '2 min', items: ['Dal Makhani', 'Jeera Rice']},
  {id: 43, table: 'B2', wait: 'now', items: ['Masala Dosa', 'Filter Coffee']},
]

function useTick(length, ms) {
  const [tick, setTick] = useState(0)
  useEffect(() => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (reduce) return undefined
    const id = setInterval(() => setTick(t => (t + 1) % length), ms)
    return () => clearInterval(id)
  }, [length, ms])
  return tick
}

function PhoneMock() {
  const step = useTick(STEPS.length, 1800)
  return (
    <div className="lp-phoneWrap" aria-hidden="true">
      <div className="lp-phone">
        <div className="lp-phoneNotch" />
        <div className="lp-phoneScreen">
          <div className="lp-phoneTop">
            <span className="lp-logoMark small">SS</span>
            <div>
              <b>Table 1</b>
              <small>Scan · Order · Track</small>
            </div>
          </div>
          <div className="lp-dish">
            <img src="/menu/dal-makhani.jpg" alt="" />
            <div>
              <b>Dal Makhani</b>
              <small>Slow-cooked black lentils</small>
              <span>₹240</span>
            </div>
          </div>
          <div className="lp-dish">
            <img src="/menu/crispy-corn-chaat.jpg" alt="" />
            <div>
              <b>Crispy Corn Chaat</b>
              <small>Tangy, crunchy, spiced</small>
              <span>₹180</span>
            </div>
          </div>
          <div className="lp-dish">
            <img src="/menu/sweet-corn-chicken-soup.jpg" alt="" />
            <div>
              <b>Sweet Corn Soup</b>
              <small>Warm, silky, comforting</small>
              <span>₹130</span>
            </div>
          </div>
          <div className="lp-cartBar">
            <span>3 items</span>
            <b>Pay ₹550</b>
          </div>
          <div className="lp-track">
            <small>ORDER #42</small>
            <b key={step} className="lp-trackStatus">
              {STEPS[step]}
            </b>
            <div className="lp-trackBar">
              {STEPS.map((s, i) => (
                <i key={s} className={i <= step ? 'on' : ''} />
              ))}
            </div>
          </div>
        </div>
      </div>
      <div className="lp-float lp-floatPay">
        <span className="lp-floatIcon">
          <Check size={16} />
        </span>
        <div>
          <b>₹550 paid</b>
          <small>UPI · verified</small>
        </div>
      </div>
      <div className="lp-float lp-floatKitchen">
        <span className="lp-floatIcon orange">
          <ChefHat size={16} />
        </span>
        <div>
          <b>New ticket · T1</b>
          <small>sent to kitchen</small>
        </div>
      </div>
    </div>
  )
}

function KitchenBoard() {
  // Each tick moves the queue one column to the right, so tickets flow
  // Pending → Preparing → Ready like they do on the real kitchen screen.
  const tick = useTick(3, 2200)
  const columns = ['Pending', 'Preparing', 'Ready']
  return (
    <div className="lp-board" aria-hidden="true">
      {columns.map((col, c) => (
        <div className="lp-col" key={col}>
          <div className="lp-colHead">
            <span className={`lp-dot c${c}`} />
            {col}
          </div>
          {TICKETS.filter((_, i) => (i + tick) % 3 === c).map(t => (
            <div className="lp-ticket" key={t.id}>
              <div className="lp-ticketHead">
                <b>#{t.id}</b>
                <span>{t.table}</span>
              </div>
              <em className="lp-wait">
                <Clock size={11} /> {t.wait}
              </em>
              {t.items.map(item => (
                <small key={item}>{item}</small>
              ))}
              <button tabIndex={-1} className={`lp-ticketBtn c${c}`}>
                {c === 0 ? 'Start' : c === 1 ? 'Mark ready' : 'Serve'}
              </button>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}

function AnalyticsMock() {
  const bars = [38, 52, 44, 70, 96, 84, 58]
  return (
    <div className="lp-analytics" aria-hidden="true">
      <div className="lp-analyticsHead">
        <b>This week</b>
        <span>Sample dashboard</span>
      </div>
      <div className="lp-kpis">
        <div>
          <small>Revenue</small>
          <b>₹1.84L</b>
        </div>
        <div>
          <small>Orders</small>
          <b>412</b>
        </div>
        <div>
          <small>Avg. order</small>
          <b>₹447</b>
        </div>
      </div>
      <div className="lp-bars">
        {bars.map((h, i) => (
          <div key={i} className="lp-barCol">
            <i style={{height: `${h}%`}} />
            <small>{'MTWTFSS'[i]}</small>
          </div>
        ))}
      </div>
      <div className="lp-top">
        <small>Best sellers</small>
        {['Butter Chicken', 'Masala Dosa', 'Dal Makhani'].map((d, i) => (
          <div key={d}>
            <span>{d}</span>
            <i style={{width: `${90 - i * 18}%`}} />
          </div>
        ))}
      </div>
    </div>
  )
}

export function Landing() {
  const {user} = useAuth()
  const appLink = user ? roleHome[user.role] || '/app' : '/login'
  const appLabel = user ? 'Open dashboard' : 'Staff login'

  return (
    <div className="lp">
      <header className="lp-nav">
        <a className="lp-brand" href="#top">
          <span className="lp-logoMark">SS</span>
          SmartServe
        </a>
        <nav className="lp-links">
          <a href="#how">How it works</a>
          <a href="#kitchen">Kitchen</a>
          <a href="#features">Features</a>
        </nav>
        <Link className="lp-btn lp-btnGhost" to={appLink}>
          {appLabel}
        </Link>
      </header>

      <section className="lp-hero" id="top">
        <div className="lp-heroCopy">
          <span className="lp-pill">
            <ScanLine size={14} /> QR ordering for restaurants
          </span>
          <h1>
            From <em>scan</em> to <em>served</em>, in one smooth flow.
          </h1>
          <p>
            SmartServe gives your restaurant QR table ordering, Razorpay payments, a live kitchen
            screen and sales analytics. Guests don&apos;t download anything, and staff don&apos;t
            juggle paper tickets.
          </p>
          <div className="lp-ctas">
            <Link className="lp-btn lp-btnPrimary" to={appLink}>
              {appLabel} <ArrowRight size={18} />
            </Link>
            <a className="lp-btn lp-btnLight" href="#how">
              See how it works
            </a>
          </div>
          <ul className="lp-checks">
            <li>
              <Check size={16} /> No app for guests
            </li>
            <li>
              <Check size={16} /> UPI, cards &amp; netbanking
            </li>
            <li>
              <Check size={16} /> Updates every 5 seconds
            </li>
          </ul>
        </div>
        <PhoneMock />
      </section>

      <section className="lp-strip" aria-label="Highlights">
        {[
          'QR table ordering',
          'Razorpay payments',
          'Live order tracking',
          'Kitchen display',
          'Role-based staff app',
          'Branch analytics',
        ].map(t => (
          <span key={t}>{t}</span>
        ))}
      </section>

      <section className="lp-section" id="how">
        <span className="lp-eyebrow">HOW IT WORKS</span>
        <h2>Three taps for the guest. Zero chaos for the floor.</h2>
        <div className="lp-steps">
          <article className="lp-step">
            <span className="lp-stepNo">01</span>
            <span className="lp-stepIcon">
              <QrCode size={26} />
            </span>
            <h3>Scan the table QR</h3>
            <p>Each table has its own code, so every order knows exactly where it&apos;s going.</p>
          </article>
          <article className="lp-step">
            <span className="lp-stepNo">02</span>
            <span className="lp-stepIcon">
              <CreditCard size={26} />
            </span>
            <h3>Order &amp; pay</h3>
            <p>
              Guests pick dishes with photos and pay with UPI, card or netbanking through Razorpay.
            </p>
          </article>
          <article className="lp-step">
            <span className="lp-stepNo">03</span>
            <span className="lp-stepIcon">
              <Clock size={26} />
            </span>
            <h3>Track it live</h3>
            <p>Received, preparing, ready, served. The guest&apos;s phone follows the kitchen.</p>
          </article>
        </div>
      </section>

      <section className="lp-section lp-split" id="kitchen">
        <div className="lp-splitCopy">
          <span className="lp-eyebrow">KITCHEN DISPLAY</span>
          <h2>A kitchen screen that keeps up with the rush.</h2>
          <p>
            Paid orders land in the queue the moment they&apos;re verified. Cooks tap <b>Start</b>{' '}
            and <b>Mark ready</b>, waiters get notified, and the guest sees it too.
          </p>
          <ul className="lp-list">
            <li>
              <Check size={16} /> Tickets in order, with table and items
            </li>
            <li>
              <Check size={16} /> Status changes checked on the server
            </li>
            <li>
              <Check size={16} /> History for every ticket, per branch
            </li>
          </ul>
        </div>
        <KitchenBoard />
      </section>

      <section className="lp-section" id="features">
        <span className="lp-eyebrow">EVERYTHING IN ONE PLACE</span>
        <h2>Built for the way restaurants actually run.</h2>
        <div className="lp-grid">
          {FEATURES.map(({icon: Icon, title, text}) => (
            <article className="lp-feature" key={title}>
              <span className="lp-featureIcon">
                <Icon size={22} />
              </span>
              <h3>{title}</h3>
              <p>{text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="lp-section lp-split reverse">
        <AnalyticsMock />
        <div className="lp-splitCopy">
          <span className="lp-eyebrow">ANALYTICS</span>
          <h2>Know what sells, and when.</h2>
          <p>
            See revenue, average order value, best sellers and peak hours for each branch, worked
            out from real orders rather than spreadsheets.
          </p>
          <ul className="lp-list">
            <li>
              <ShieldCheck size={16} /> Only verified payments count as revenue
            </li>
            <li>
              <Check size={16} /> Filter by branch and date range
            </li>
          </ul>
        </div>
      </section>

      <section className="lp-cta">
        <h2>Bring SmartServe to your tables.</h2>
        <p>Set up branches, print table QR codes and start taking orders.</p>
        <Link className="lp-btn lp-btnPrimary" to={appLink}>
          {appLabel} <ArrowRight size={18} />
        </Link>
      </section>

      <footer className="lp-footer">
        <span className="lp-brand">
          <span className="lp-logoMark small">SS</span>
          SmartServe
        </span>
        <small>© {new Date().getFullYear()} SmartServe. Restaurant ordering, simplified.</small>
      </footer>
    </div>
  )
}
