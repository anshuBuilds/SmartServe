import {createContext, useContext, useEffect, useMemo, useState} from 'react'
import {authApi, TOKEN_KEY, USER_KEY} from '../api/client'
const AuthContext = createContext(null)
export function AuthProvider({children}) {
  const [user, setUser] = useState(() => JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'))
  const [loading, setLoading] = useState(Boolean(sessionStorage.getItem(TOKEN_KEY)))
  useEffect(() => {
    if (!sessionStorage.getItem(TOKEN_KEY)) return
    authApi
      .me()
      .then(u => {
        setUser(u)
        sessionStorage.setItem(USER_KEY, JSON.stringify(u))
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])
  const value = useMemo(
    () => ({
      user,
      loading,
      async login(values) {
        const result = await authApi.login(values)
        sessionStorage.setItem(TOKEN_KEY, result.token)
        sessionStorage.setItem(USER_KEY, JSON.stringify(result.user))
        setUser(result.user)
        return result.user
      },
      logout() {
        sessionStorage.clear()
        setUser(null)
      },
    }),
    [user, loading],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth = () => useContext(AuthContext)
