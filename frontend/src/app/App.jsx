import {Routes, Route, Navigate} from 'react-router-dom'
import {RequireAuth, RequireRole} from '../auth/Guards'
import {AppShell} from '../components/AppShell'
import {Login} from '../features/Login'
import {
  Dashboard,
  MenuPage,
  OrdersPage,
  KitchenPage,
  UsersPage,
  AnalyticsPage,
} from '../features/StaffPages'
import {GuestEntry, GuestMenu, GuestCheckout, GuestTracking} from '../features/Guest'
import {NotificationsPage} from '../features/Notifications'
import {
  RestaurantsPage,
  TablesPage,
  UserManagement,
  MenuManagement,
  StaffOrderCreate,
  OrderDetail,
  KitchenHistory,
} from '../features/Operations'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/guest/:qrToken" element={<GuestEntry />} />
      <Route path="/guest/:qrToken/menu" element={<GuestMenu />} />
      <Route path="/guest/:qrToken/checkout" element={<GuestCheckout />} />
      <Route path="/guest/order/:trackingToken" element={<GuestTracking />} />
      <Route element={<RequireAuth />}>
        <Route
          path="/forbidden"
          element={
            <main className="center">
              <h1>Access denied</h1>
              <p>Your role does not allow this page.</p>
            </main>
          }
        />
        <Route path="/app" element={<AppShell />}>
          <Route path="admin" element={<Dashboard />} />
          <Route path="manager" element={<Dashboard manager />} />
          <Route path="menu" element={<MenuPage />} />
          <Route path="waiter/orders" element={<OrdersPage />} />
          <Route path="waiter/orders/new" element={<StaffOrderCreate />} />
          <Route path="orders/:orderId" element={<OrderDetail />} />
          <Route path="kitchen" element={<KitchenPage />} />
          <Route path="kitchen/history" element={<KitchenHistory />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route element={<RequireRole roles={['ADMIN']} />}>
            <Route path="users" element={<UserManagement />} />
            <Route path="restaurants" element={<RestaurantsPage />} />
          </Route>
          <Route element={<RequireRole roles={['ADMIN', 'MANAGER']} />}>
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="menu/manage" element={<MenuManagement />} />
            <Route path="branches/:branchId/tables" element={<TablesPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
