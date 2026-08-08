import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {useNavigate, useSearchParams} from 'react-router-dom'
import {QRCodeSVG} from 'qrcode.react'
import {api} from '../api/client'
import {money} from '../lib/format'
import {useAuth} from '../auth/AuthProvider'

const useApiMutation = (fn, keys = []) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => keys.forEach(k => qc.invalidateQueries({queryKey: [k]})),
  })
}
const Field = ({label, ...props}) => (
  <label>
    {label}
    <input {...props} />
  </label>
)
const Select = ({label, children, ...props}) => (
  <label>
    {label}
    <select {...props}>{children}</select>
  </label>
)
const Notice = ({mutation}) =>
  mutation.error ? (
    <div className="alert">{mutation.error.message}</div>
  ) : mutation.isSuccess ? (
    <div className="success">Saved successfully.</div>
  ) : null
const Page = ({title, subtitle, actions, children}) => (
  <>
    <header className="pageHeader">
      <div>
        <span className="eyebrow">SMARTSERVE OPERATIONS</span>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {actions}
    </header>
    {children}
  </>
)
const normalizePhone = value => {
  const raw = String(value || '').trim()
  if (!raw) return null
  const compact = raw.replace(/[\s()-]/g, '')
  if (compact.startsWith('+')) return compact
  const digits = compact.replace(/\D/g, '')
  if (digits.length === 10) return `+91${digits}`
  if (digits.length === 12 && digits.startsWith('91')) return `+${digits}`
  return compact
}

export function RestaurantsPage() {
  const restaurants = useQuery({queryKey: ['restaurants'], queryFn: () => api.get('/restaurants')}),
    [selected, setSelected] = useState(null)
  const branches = useQuery({
    queryKey: ['branches', selected],
    queryFn: () => api.get(`/restaurants/${selected}/branches`),
    enabled: Boolean(selected),
  })
  const createRestaurant = useApiMutation(d => api.post('/restaurants', d), ['restaurants'])
  const createBranch = useApiMutation(
    d => api.post(`/restaurants/${selected}/branches`, d),
    ['branches'],
  )
  return (
    <Page title="Restaurant setup" subtitle="Create restaurants and their operating branches.">
      <div className="split">
        <section className="card panel">
          <h2>Restaurants</h2>
          <form
            onSubmit={e => {
              e.preventDefault()
              const f = new FormData(e.currentTarget)
              createRestaurant.mutate({name: f.get('name'), ownerName: f.get('owner')})
              e.currentTarget.reset()
            }}
          >
            <Field name="name" label="Restaurant name" required maxLength="120" />
            <Field name="owner" label="Owner name" required maxLength="120" />
            <Notice mutation={createRestaurant} />
            <button className="primary">Create restaurant</button>
          </form>
          <div className="list">
            {restaurants.data?.map(r => (
              <button
                className={selected === r.id ? 'selected' : ''}
                onClick={() => setSelected(r.id)}
                key={r.id}
              >
                <span>
                  <b>{r.name}</b>
                  <small>{r.ownerName}</small>
                </span>
                <span>Open</span>
              </button>
            ))}
          </div>
        </section>
        <section className="card panel">
          <h2>Branches</h2>
          {!selected ? (
            <div className="state">Choose a restaurant first.</div>
          ) : (
            <>
              <form
                onSubmit={e => {
                  e.preventDefault()
                  const f = new FormData(e.currentTarget)
                  createBranch.mutate(Object.fromEntries(f))
                  e.currentTarget.reset()
                }}
              >
                <Field name="name" label="Branch name" required />
                <Field name="address" label="Address" required />
                <Field name="phone" label="Phone" required />
                <Notice mutation={createBranch} />
                <button className="primary">Create branch</button>
              </form>
              <div className="list">
                {branches.data?.map(b => (
                  <a href={`/app/branches/${b.id}/tables`} key={b.id}>
                    <span>
                      <b>{b.name}</b>
                      <small>{b.address}</small>
                    </span>
                    <span>Manage tables</span>
                  </a>
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </Page>
  )
}

const guestUrl = token =>
  new URL(`/guest/${encodeURIComponent(token)}`, window.location.origin).toString()

const printQrCard = button => {
  const sheet = button.closest('.tableCard')?.querySelector('.qrCustomerCard')
  if (!sheet) return
  const cleanup = () => {
    sheet.classList.remove('printing')
    document.body.classList.remove('qr-print-mode')
  }
  sheet.classList.add('printing')
  document.body.classList.add('qr-print-mode')
  window.addEventListener('afterprint', cleanup, {once: true})
  window.setTimeout(() => window.print(), 100)
}

export function TablesPage() {
  const [params] = useSearchParams()
  const pathId = location.pathname.match(/branches\/(\d+)/)?.[1]
  const branchId = pathId || params.get('branchId')
  const q = useQuery({
    queryKey: ['tables', branchId],
    queryFn: () => api.get(`/branches/${branchId}/tables`),
    enabled: Boolean(branchId),
  })
  const create = useApiMutation(d => api.post(`/branches/${branchId}/tables`, d), ['tables'])
  const status = useApiMutation(
    ({id, value}) => api.patch(`/branches/${branchId}/tables/${id}/status`, {status: value}),
    ['tables'],
  )
  const rotate = useApiMutation(
    id => api.post(`/branches/${branchId}/tables/${id}/qr-token/rotate`),
    ['tables'],
  )
  return (
    <Page title="Tables" subtitle="Availability, capacity and guest QR access.">
      <form
        className="card inlineForm"
        onSubmit={e => {
          e.preventDefault()
          const f = new FormData(e.currentTarget)
          create.mutate({tableNumber: f.get('tableNumber'), capacity: Number(f.get('capacity'))})
          e.currentTarget.reset()
        }}
      >
        <Field name="tableNumber" label="Table number" required />
        <Field name="capacity" label="Capacity" type="number" min="1" max="50" required />
        <button className="primary">Add table</button>
      </form>
      {!branchId ? (
        <div className="state">Open tables from a branch on Restaurant setup.</div>
      ) : (
        <div className="tableCards">
          {q.data?.map(t => {
            const url = t.qrToken ? guestUrl(t.qrToken) : null
            return (
              <article className="card tableCard" key={t.id}>
                <div>
                  <span className={`badge table-${t.status}`}>{t.status}</span>
                  <h2>{t.tableNumber}</h2>
                  <p>{t.capacity} seats</p>
                </div>
                <Select
                  label="Change status"
                  value={t.status}
                  onChange={e => status.mutate({id: t.id, value: e.target.value})}
                >
                  <option>AVAILABLE</option>
                  <option>RESERVED</option>
                  <option>OUT_OF_SERVICE</option>
                </Select>
                <div className="qrCustomerCard">
                  <div className="qrPrintBrand">
                    <span>SS</span>
                    <strong>SmartServe</strong>
                  </div>
                  <p>Scan to view the menu and place your order</p>
                  {url ? (
                    <QRCodeSVG
                      value={url}
                      size={180}
                      level="H"
                      marginSize={3}
                      title={`Ordering QR for table ${t.tableNumber}`}
                    />
                  ) : (
                    <div className="qrPlaceholder">QR unavailable</div>
                  )}
                  <h3>Table {t.tableNumber}</h3>
                  {url && <small>{url}</small>}
                </div>
                <div className="token">
                  <small>Guest token</small>
                  <code>{t.qrToken || 'Generated on refresh'}</code>
                </div>
                <div className="tableQrActions">
                  <button
                    type="button"
                    className="primary"
                    disabled={!url}
                    onClick={e => printQrCard(e.currentTarget)}
                  >
                    Print QR card
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    disabled={rotate.isPending}
                    onClick={() => rotate.mutate(t.id)}
                  >
                    Rotate QR token
                  </button>
                </div>
                <small className="qrRotationWarning">
                  Rotating the token invalidates every previously printed QR for this table.
                </small>
              </article>
            )
          })}
        </div>
      )}
    </Page>
  )
}

export function UserManagement() {
  const users = useQuery({queryKey: ['users'], queryFn: () => api.get('/users')}),
    restaurants = useQuery({queryKey: ['restaurants'], queryFn: () => api.get('/restaurants')}),
    [restaurant, setRestaurant] = useState(''),
    branches = useQuery({
      queryKey: ['branches', restaurant],
      queryFn: () => api.get(`/restaurants/${restaurant}/branches`),
      enabled: Boolean(restaurant),
    }),
    create = useApiMutation(d => api.post('/users', d), ['users']),
    toggle = useApiMutation(u => api.patch(`/users/${u.id}/status`, {active: !u.active}), ['users'])
  return (
    <Page title="Team management" subtitle="Create accounts and control access.">
      <details className="card disclosure" open>
        <summary>Add team member</summary>
        <form
          className="formGrid"
          onSubmit={e => {
            e.preventDefault()
            const f = new FormData(e.currentTarget)
            create.mutate({
              username: f.get('username'),
              password: f.get('password'),
              fullName: f.get('fullName'),
              role: f.get('role'),
              branchId: f.get('branchId') ? Number(f.get('branchId')) : null,
            })
          }}
        >
          <Field name="fullName" label="Full name" required />
          <Field name="username" label="Username" minLength="3" required />
          <Field
            name="password"
            label="Temporary password"
            type="password"
            minLength="8"
            required
          />
          <Select name="role" label="Role">
            <option>WAITER</option>
            <option>KITCHEN</option>
            <option>MANAGER</option>
            <option>ADMIN</option>
          </Select>
          <Select
            label="Restaurant"
            value={restaurant}
            onChange={e => setRestaurant(e.target.value)}
          >
            <option value="">Choose restaurant</option>
            {restaurants.data?.map(r => (
              <option value={r.id} key={r.id}>
                {r.name}
              </option>
            ))}
          </Select>
          <Select name="branchId" label="Branch assignment">
            <option value="">No branch</option>
            {branches.data?.map(b => (
              <option value={b.id} key={b.id}>
                {b.name}
              </option>
            ))}
          </Select>
          <Notice mutation={create} />
          <button className="primary">Create user</button>
        </form>
      </details>
      <div className="tableWrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Role</th>
              <th>Branch</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {users.data?.map(u => (
              <tr key={u.id}>
                <td>
                  <b>{u.fullName}</b>
                  <br />
                  <small>{u.username}</small>
                </td>
                <td>{u.role}</td>
                <td>{u.branchName || 'All branches'}</td>
                <td>
                  <span className={`badge ${u.active ? 'green' : 'gray'}`}>
                    {u.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>
                  <button className="secondary" onClick={() => toggle.mutate(u)}>
                    {u.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Page>
  )
}

export function MenuManagement() {
  const cats = useQuery({queryKey: ['categories'], queryFn: () => api.get('/menu/categories')}),
    items = useQuery({queryKey: ['menu'], queryFn: () => api.get('/menu/items')}),
    catCreate = useApiMutation(d => api.post('/menu/categories', d), ['categories']),
    itemCreate = useApiMutation(d => api.post('/menu/items', d), ['menu']),
    availability = useApiMutation(
      item => api.put(`/menu/items/${item.id}`, {...item, available: !item.available}),
      ['menu'],
    )
  return (
    <Page
      title="Menu management"
      subtitle="Categories, pricing, availability and preparation details."
    >
      <div className="split">
        <details className="card disclosure">
          <summary>Create category</summary>
          <form
            onSubmit={e => {
              e.preventDefault()
              const f = new FormData(e.currentTarget)
              catCreate.mutate({
                categoryName: f.get('name'),
                description: f.get('description'),
                displayOrder: Number(f.get('order') || 0),
              })
            }}
          >
            <Field name="name" label="Name" required />
            <Field name="description" label="Description" />
            <Field name="order" label="Display order" type="number" defaultValue="0" />
            <Notice mutation={catCreate} />
            <button className="primary">Create category</button>
          </form>
        </details>
        <details className="card disclosure" open>
          <summary>Create menu item</summary>
          <form
            className="formGrid"
            onSubmit={e => {
              e.preventDefault()
              const f = new FormData(e.currentTarget)
              itemCreate.mutate({
                name: f.get('name'),
                description: f.get('description'),
                price: Number(f.get('price')),
                categoryId: Number(f.get('categoryId')),
                preparationTimeMinutes: Number(f.get('prep')),
                imageUrl: f.get('imageUrl') || null,
                foodType: f.get('foodType'),
                spiceLevel: f.get('spiceLevel'),
              })
            }}
          >
            <Field name="name" label="Item name" required />
            <Field name="description" label="Description" />
            <Field name="price" label="Price" type="number" step="0.01" min="0.01" required />
            <Select name="categoryId" label="Category" required>
              <option value="">Choose</option>
              {cats.data?.map(c => (
                <option value={c.id} key={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
            <Field name="prep" label="Preparation minutes" type="number" min="1" required />
            <Field name="imageUrl" label="Image URL" type="url" />
            <Select name="foodType" label="Food type">
              <option>VEG</option>
              <option>NON_VEG</option>
              <option>VEGAN</option>
            </Select>
            <Select name="spiceLevel" label="Spice level">
              <option>NONE</option>
              <option>MILD</option>
              <option>MEDIUM</option>
              <option>HOT</option>
            </Select>
            <Notice mutation={itemCreate} />
            <button className="primary">Create item</button>
          </form>
        </details>
      </div>
      <div className="tableWrap">
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th>Category</th>
              <th>Price</th>
              <th>Prep</th>
              <th>Availability</th>
            </tr>
          </thead>
          <tbody>
            {items.data?.map(i => (
              <tr key={i.id}>
                <td>
                  <b>{i.name}</b>
                  <br />
                  <small>
                    {i.foodType} - {i.spiceLevel}
                  </small>
                </td>
                <td>{i.categoryName}</td>
                <td>{money(i.price)}</td>
                <td>{i.preparationTimeMinutes} min</td>
                <td>
                  <button
                    className={`badge ${i.available ? 'green' : 'gray'}`}
                    onClick={() => availability.mutate(i)}
                  >
                    {i.available ? 'Available' : 'Unavailable'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Page>
  )
}

export function StaffOrderCreate() {
  const {user} = useAuth(),
    nav = useNavigate()
  const [type, setType] = useState('DINE_IN')
  const [cart, setCart] = useState([])
  const [restaurant, setRestaurant] = useState('')
  const [selectedBranch, setSelectedBranch] = useState(user.branchId || '')
  const [activeCategory, setActiveCategory] = useState('all')
  const [search, setSearch] = useState('')
  const branchId = user.branchId || Number(selectedBranch) || null
  const restaurants = useQuery({
    queryKey: ['restaurants'],
    queryFn: () => api.get('/restaurants'),
    enabled: !user.branchId,
  })
  const branches = useQuery({
    queryKey: ['branches', restaurant],
    queryFn: () => api.get(`/restaurants/${restaurant}/branches`),
    enabled: !user.branchId && Boolean(restaurant),
  })
  const cats = useQuery({
    queryKey: ['categories', 'active'],
    queryFn: () => api.get('/menu/categories?activeOnly=true'),
  })
  const menu = useQuery({
    queryKey: ['menu', 'available'],
    queryFn: () => api.get('/menu/items?availableOnly=true'),
  })
  const tables = useQuery({
    queryKey: ['tables', branchId],
    queryFn: () => api.get(`/branches/${branchId}/tables`),
    enabled: Boolean(branchId),
  })
  const create = useMutation({
    mutationFn: d => api.post('/orders', d),
    onSuccess: o => nav(`/app/orders/${o.id}`),
  })
  const add = i =>
    setCart(c => {
      const old = c.find(x => x.id === i.id)
      return old
        ? c.map(x => (x.id === i.id ? {...x, quantity: x.quantity + 1} : x))
        : [...c, {...i, quantity: 1}]
    })
  const dec = id =>
    setCart(c =>
      c.map(x => (x.id === id ? {...x, quantity: x.quantity - 1} : x)).filter(x => x.quantity > 0),
    )
  const availableTables = (tables.data || []).filter(t => t.status === 'AVAILABLE')
  const items = (menu.data || []).filter(
    i =>
      (activeCategory === 'all' || String(i.categoryId) === String(activeCategory)) &&
      `${i.name} ${i.description || ''}`.toLowerCase().includes(search.toLowerCase()),
  )
  const categories = (cats.data || []).filter(c =>
    (menu.data || []).some(i => String(i.categoryId) === String(c.id)),
  )
  const total = cart.reduce((n, i) => n + i.price * i.quantity, 0)
  return (
    <Page title="Create order" subtitle="Choose a branch, browse by category, and build the order.">
      <div className="orderBuilder upgradedOrderBuilder">
        <section className="orderMenuPanel">
          {!user.branchId && (
            <div className="card branchChooser">
              <Select
                label="Restaurant"
                value={restaurant}
                onChange={e => {
                  setRestaurant(e.target.value)
                  setSelectedBranch('')
                }}
              >
                <option value="">Choose restaurant</option>
                {restaurants.data?.map(r => (
                  <option value={r.id} key={r.id}>
                    {r.name}
                  </option>
                ))}
              </Select>
              <Select
                label="Branch"
                value={selectedBranch}
                onChange={e => setSelectedBranch(e.target.value)}
                disabled={!restaurant}
              >
                <option value="">Choose branch</option>
                {branches.data?.map(b => (
                  <option value={b.id} key={b.id}>
                    {b.name}
                  </option>
                ))}
              </Select>
            </div>
          )}
          <div className="orderToolbar card">
            <div className="segmented">
              <button
                type="button"
                className={type === 'DINE_IN' ? 'active' : ''}
                onClick={() => setType('DINE_IN')}
              >
                Dine in
              </button>
              <button
                type="button"
                className={type === 'TAKEAWAY' ? 'active' : ''}
                onClick={() => setType('TAKEAWAY')}
              >
                Takeaway
              </button>
            </div>
            <input
              className="menuSearch"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search dishes, drinks, desserts…"
            />
          </div>
          <div className="categoryRail">
            <button
              className={activeCategory === 'all' ? 'active' : ''}
              onClick={() => setActiveCategory('all')}
            >
              All
            </button>
            {categories.map(c => (
              <button
                className={String(activeCategory) === String(c.id) ? 'active' : ''}
                onClick={() => setActiveCategory(c.id)}
                key={c.id}
              >
                {c.name}
              </button>
            ))}
          </div>
          {menu.isLoading ? (
            <div className="state">
              <div className="spinner" />
              Loading menu…
            </div>
          ) : (
            <div className="foodCardGrid">
              {items.map(i => (
                <article className="card foodCard" key={i.id}>
                  <div className="foodPhoto">
                    {i.imageUrl ? (
                      <img
                        src={i.imageUrl}
                        alt=""
                        onError={e => {
                          e.currentTarget.style.display = 'none'
                        }}
                      />
                    ) : (
                      <span>{i.foodType === 'VEG' ? '🌿' : '🍽️'}</span>
                    )}
                  </div>
                  <div className="foodInfo">
                    <span className="eyebrow">
                      {i.categoryName} · {i.foodType} · {i.spiceLevel}
                    </span>
                    <h3>{i.name}</h3>
                    <p>{i.description}</p>
                    <div className="foodMeta">
                      <b>{money(i.price)}</b>
                      <small>{i.preparationTimeMinutes} min</small>
                    </div>
                  </div>
                  <button className="round" type="button" onClick={() => add(i)}>
                    +
                  </button>
                </article>
              ))}
            </div>
          )}
        </section>
        <form
          className="card orderSummary improvedSummary"
          onSubmit={e => {
            e.preventDefault()
            const f = new FormData(e.currentTarget)
            create.mutate({
              branchId,
              tableId: type === 'DINE_IN' ? Number(f.get('tableId')) : null,
              customerName: f.get('customerName'),
              orderType: type,
              customerPhone: normalizePhone(f.get('phone')),
              smsConsent: f.get('sms') === 'on',
              specialInstructions: f.get('notes') || null,
              items: cart.map(i => ({menuItemId: i.id, quantity: i.quantity})),
            })
          }}
        >
          <h2>Order summary</h2>
          {!branchId && (
            <div className="alert">
              Choose a branch first so tables and orders can be linked correctly.
            </div>
          )}
          {!cart.length && <div className="state smallState">Add items from the menu.</div>}
          {cart.map(i => (
            <div className="cartLine" key={i.id}>
              <div>
                <b>{i.name}</b>
                <small>{money(i.price)} each</small>
              </div>
              <div className="qty">
                <button type="button" onClick={() => dec(i.id)}>
                  −
                </button>
                <span>{i.quantity}</span>
                <button type="button" onClick={() => add(i)}>
                  +
                </button>
              </div>
              <b>{money(i.price * i.quantity)}</b>
            </div>
          ))}
          {type === 'DINE_IN' && (
            <Select name="tableId" label="Available table" required disabled={!branchId}>
              <option value="">{branchId ? 'Choose table' : 'Choose branch first'}</option>
              {availableTables.map(t => (
                <option value={t.id} key={t.id}>
                  {t.tableNumber} · {t.capacity} seats
                </option>
              ))}
            </Select>
          )}
          {type === 'DINE_IN' && branchId && tables.isSuccess && !availableTables.length && (
            <div className="alert">
              No available tables in this branch right now. Choose takeaway or free a table.
            </div>
          )}
          <Field name="customerName" label="Customer name" required />
          <Field
            name="phone"
            label={
              type === 'TAKEAWAY'
                ? 'Phone (required, +91 auto-added)'
                : 'Phone (+91 auto-added if needed)'
            }
            required={type === 'TAKEAWAY'}
            placeholder="8800834788 or +918800834788"
            inputMode="tel"
          />
          <label>
            Special instructions
            <textarea
              name="notes"
              maxLength="500"
              placeholder="Allergies, spice level, packing notes…"
            />
          </label>
          <label className="check">
            <input type="checkbox" name="sms" /> Customer explicitly consents to SMS
          </label>
          {create.error && <div className="alert">{create.error.message}</div>}
          <button className="primary" disabled={!branchId || !cart.length || create.isPending}>
            Create order · {money(total)}
          </button>
        </form>
      </div>
    </Page>
  )
}
export function OrderDetail() {
  const id = location.pathname.match(/orders\/(\d+)/)?.[1],
    q = useQuery({queryKey: ['order', id], queryFn: () => api.get(`/orders/${id}`)}),
    action = useApiMutation(kind => api.patch(`/orders/${id}/${kind}`), ['order'])
  if (!q.data) return <div className="state">Loading order...</div>
  const o = q.data
  return (
    <Page
      title={`Order #${o.id}`}
      subtitle={`${o.tableNumber ? `Table ${o.tableNumber}` : 'Takeaway'} - ${o.customerName}`}
    >
      <article className="card detail">
        <span className={`badge status-${o.orderStatus}`}>{o.orderStatus}</span>
        {o.items.map(i => (
          <div className="line" key={i.id}>
            <span>
              {i.quantity}x {i.itemName}
            </span>
            <b>{money(i.lineTotal)}</b>
          </div>
        ))}
        <div className="line total">
          <span>Total</span>
          <b>{money(o.totalAmount)}</b>
        </div>
        <div className="actions">
          {o.orderStatus === 'READY' && (
            <button className="primary" onClick={() => action.mutate('serve')}>
              Mark served
            </button>
          )}
          {['PENDING', 'PREPARING'].includes(o.orderStatus) && (
            <button
              className="danger"
              onClick={() => confirm('Cancel this order?') && action.mutate('cancel')}
            >
              Cancel order
            </button>
          )}
        </div>
      </article>
    </Page>
  )
}

export function KitchenHistory() {
  const [params, setParams] = useSearchParams()
  const {user} = useAuth()
  const needsBranch = ['ADMIN', 'MANAGER'].includes(user.role) && !user.branchId
  const [restaurant, setRestaurant] = useState('')
  const [selectedBranch, setSelectedBranch] = useState(user.branchId || '')
  const [defaultRange] = useState(() => {
    const now = new Date()
    return {
      from: new Date(now.getTime() - 7 * 86400000).toISOString().slice(0, 10),
      to: now.toISOString().slice(0, 10),
    }
  })
  const from = params.get('from') || defaultRange.from
  const to = params.get('to') || defaultRange.to
  const status = params.get('status') || 'SERVED'
  const branchId = user.branchId || Number(selectedBranch) || null
  const restaurants = useQuery({
    queryKey: ['restaurants', 'kitchen-history'],
    queryFn: () => api.get('/restaurants'),
    enabled: needsBranch,
  })
  const branches = useQuery({
    queryKey: ['branches', 'kitchen-history', restaurant],
    queryFn: () => api.get(`/restaurants/${restaurant}/branches`),
    enabled: needsBranch && Boolean(restaurant),
  })
  const update = next =>
    setParams({from: next.from || from, to: next.to || to, status: next.status || status})
  const qs = new URLSearchParams({
    from: new Date(`${from}T00:00:00`).toISOString(),
    to: new Date(`${to}T23:59:59`).toISOString(),
    status,
    branchId: String(branchId || ''),
  })
  const q = useQuery({
    queryKey: ['kitchen-history', qs.toString()],
    queryFn: () => api.get(`/kitchen/tickets/history?${qs}`),
    enabled: Boolean(branchId),
  })
  const rows = q.data?.content || q.data?.tickets || []
  return (
    <Page
      title="Kitchen history"
      subtitle="Completed, ready, and cancelled tickets for a selected branch."
    >
      {needsBranch && (
        <div className="card branchChooser kitchenBranchChooser">
          <Select
            label="Restaurant"
            value={restaurant}
            onChange={e => {
              setRestaurant(e.target.value)
              setSelectedBranch('')
            }}
          >
            <option value="">Choose restaurant</option>
            {restaurants.data?.map(r => (
              <option value={r.id} key={r.id}>
                {r.name}
              </option>
            ))}
          </Select>
          <Select
            label="Kitchen branch"
            value={selectedBranch}
            onChange={e => setSelectedBranch(e.target.value)}
            disabled={!restaurant}
          >
            <option value="">Choose branch</option>
            {branches.data?.map(b => (
              <option value={b.id} key={b.id}>
                {b.name}
              </option>
            ))}
          </Select>
        </div>
      )}
      <div className="card filterBar">
        <Field
          label="From"
          type="date"
          value={from}
          onChange={e => update({from: e.target.value})}
        />
        <Field label="To" type="date" value={to} onChange={e => update({to: e.target.value})} />
        <Select label="Status" value={status} onChange={e => update({status: e.target.value})}>
          <option>SERVED</option>
          <option>READY</option>
          <option>CANCELLED</option>
        </Select>
      </div>
      {!branchId ? (
        <div className="state">Choose a branch to load kitchen history.</div>
      ) : q.isLoading ? (
        <div className="state">
          <div className="spinner" />
          Loading kitchen history...
        </div>
      ) : q.error ? (
        <div className="state error">{q.error.message}</div>
      ) : rows.length ? (
        <div className="cards">
          {rows.map(t => (
            <article className="card ticket" key={t.orderId}>
              <b>
                #{t.orderId} - {t.tableNumber ? `Table ${t.tableNumber}` : 'Takeaway'}
              </b>
              <span className={`badge status-${t.orderStatus || t.status}`}>
                {t.orderStatus || t.status}
              </span>
              <p>{t.items?.map(i => `${i.quantity}x ${i.itemName}`).join(', ')}</p>
              {t.specialInstructions && <div className="note">{t.specialInstructions}</div>}
            </article>
          ))}
        </div>
      ) : (
        <div className="state">
          No {status.toLowerCase()} tickets found for this branch and date range.
        </div>
      )}
    </Page>
  )
}
