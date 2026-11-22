import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useCartStore } from '../stores/cartStore'
import type { OrderResponse } from '../types'

export default function CheckoutPage() {
  const navigate = useNavigate()
  const { cart, fetch } = useCartStore()
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<OrderResponse | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetch()
  }, [fetch])

  const placeOrder = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await api.post('/orders')
      setResult(data)
      await fetch()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Order failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (result) {
    return (
      <div className="page">
        <div className="confirmation-card">
          <h1>Order Confirmed!</h1>
          <p>Order #{result.id}</p>
          <p>Total: ${Number(result.totalPrice).toFixed(2)}</p>

          {result.adjustments?.length > 0 && (
            <div className="adjustments-box">
              <h3>Some items were adjusted:</h3>
              {result.adjustments.map((a, i) => (
                <p key={i}>
                  <strong>{a.bookTitle}</strong>: requested {a.requested}, fulfilled {a.fulfilled} — {a.reason}
                </p>
              ))}
            </div>
          )}

          <div className="confirmation-actions">
            <button className="btn-primary" onClick={() => navigate('/orders')}>
              View My Orders
            </button>
            <button className="btn-ghost" onClick={() => navigate('/books')}>
              Continue Shopping
            </button>
          </div>
        </div>
      </div>
    )
  }

  const items = cart?.items ?? []

  return (
    <div className="page">
      <h1>Checkout</h1>
      {items.length === 0 ? (
        <div className="empty-state">
          <p>Your cart is empty.</p>
          <button className="btn-primary" onClick={() => navigate('/books')}>
            Browse Books
          </button>
        </div>
      ) : (
        <div className="checkout-layout">
          <div className="checkout-items">
            {items.map((item) => (
              <div key={item.bookId} className="checkout-item">
                <span>{item.title}</span>
                <span>×{item.quantity}</span>
                <span>${(Number(item.price) * item.quantity).toFixed(2)}</span>
              </div>
            ))}
            <div className="checkout-total">
              <strong>Total: ${Number(cart?.total ?? 0).toFixed(2)}</strong>
            </div>
          </div>
          {error && <p className="error-msg">{error}</p>}
          <button className="btn-primary btn-lg" disabled={loading} onClick={placeOrder}>
            {loading ? 'Placing order…' : 'Place Order'}
          </button>
        </div>
      )}
    </div>
  )
}
