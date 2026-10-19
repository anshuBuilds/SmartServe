import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import {GuestCheckout, GuestTracking} from './Guest'
import {guestApi} from '../api/client'

vi.mock('../api/client', () => ({
  guestApi: {order: vi.fn(), verifyPayment: vi.fn(), track: vi.fn()},
}))

const cart = [{id: 7, name: 'Paneer Tikka', price: 220, quantity: 1}]

function renderCheckout() {
  const client = new QueryClient({defaultOptions: {queries: {retry: false}}})
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[{pathname: '/guest/qr-1/checkout', state: {cart}}]}>
        <Routes>
          <Route path="/guest/:qrToken/menu" element={<h1>Menu page</h1>} />
          <Route path="/guest/:qrToken/checkout" element={<GuestCheckout />} />
          <Route path="/guest/order/:trackingToken" element={<GuestTracking />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('GuestCheckout', () => {
  beforeEach(() => {
    // Fake Checkout: approve the payment as soon as it opens.
    window.Razorpay = vi.fn(function (options) {
      this.on = vi.fn()
      this.open = () =>
        options.handler({
          razorpay_order_id: 'order_1',
          razorpay_payment_id: 'pay_1',
          razorpay_signature: 'sig',
        })
    })
    guestApi.order.mockResolvedValue({
      trackingToken: 'track-1',
      order: {id: 42},
      payment: {
        razorpayKeyId: 'key',
        amountInPaise: 22000,
        currency: 'INR',
        razorpayOrderId: 'order_1',
      },
    })
    guestApi.verifyPayment.mockResolvedValue({})
    guestApi.track.mockResolvedValue({
      id: 42,
      orderStatus: 'PENDING',
      totalAmount: 220,
      items: [{id: 1, itemName: 'Paneer Tikka', quantity: 1, lineTotal: 220}],
    })
  })

  afterEach(() => {
    delete window.Razorpay
    vi.clearAllMocks()
  })

  it('shows the cart passed through router state', () => {
    renderCheckout()

    expect(screen.getByText('Almost there')).toBeInTheDocument()
    expect(screen.getByText('1x Paneer Tikka')).toBeInTheDocument()
  })

  it('lands on the tracking page, not the menu, after payment is verified', async () => {
    renderCheckout()

    await userEvent.type(screen.getByLabelText('Your name'), 'Anshu')
    await userEvent.click(screen.getByRole('button', {name: /pay and place order/i}))

    expect(await screen.findByText('ORDER #42')).toBeInTheDocument()
    expect(screen.queryByText('Menu page')).not.toBeInTheDocument()
    expect(guestApi.verifyPayment).toHaveBeenCalledWith('track-1', {
      razorpayOrderId: 'order_1',
      razorpayPaymentId: 'pay_1',
      razorpaySignature: 'sig',
    })
  })
})
