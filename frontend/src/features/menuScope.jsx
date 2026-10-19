import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {api} from '../api/client'
import {useAuth} from '../auth/AuthProvider'

const STORAGE_KEY = 'smartserve.menu.restaurant'

function readStored() {
  try {
    return localStorage.getItem(STORAGE_KEY) || ''
  } catch {
    return ''
  }
}

/**
 * Menus belong to a restaurant. Admins pick which restaurant they are working on (remembered
 * per browser); other staff always get their own restaurant from the server, so no id is sent.
 */
export function useMenuRestaurant() {
  const {user} = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [picked, setPicked] = useState(readStored)
  const restaurants = useQuery({
    queryKey: ['restaurants'],
    queryFn: () => api.get('/restaurants'),
    enabled: isAdmin,
  })
  const list = restaurants.data || []
  const valid = list.some(r => String(r.id) === String(picked))
  const restaurantId = isAdmin ? (valid ? picked : list[0]?.id ? String(list[0].id) : '') : ''

  function setRestaurantId(id) {
    setPicked(id)
    try {
      localStorage.setItem(STORAGE_KEY, id)
    } catch {
      // Storage can be unavailable (private mode); the choice then lasts for this page only.
    }
  }

  // Appends ?restaurantId= (or &restaurantId=) for admins; leaves the path alone otherwise.
  function withRestaurant(path) {
    if (!restaurantId) return path
    return `${path}${path.includes('?') ? '&' : '?'}restaurantId=${restaurantId}`
  }

  return {
    isAdmin,
    restaurantId,
    setRestaurantId,
    restaurants: list,
    ready: !isAdmin || Boolean(restaurantId),
    withRestaurant,
  }
}

export function RestaurantPicker({scope}) {
  if (!scope.isAdmin) return null
  if (!scope.restaurants.length) {
    return <div className="state">Create a restaurant first to manage its menu.</div>
  }
  return (
    <label className="restaurantPicker">
      Restaurant
      <select value={scope.restaurantId} onChange={e => scope.setRestaurantId(e.target.value)}>
        {scope.restaurants.map(r => (
          <option key={r.id} value={r.id}>
            {r.name}
          </option>
        ))}
      </select>
    </label>
  )
}
