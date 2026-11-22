import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import type { Order } from '../types'

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-pending',
  SHIPPED: 'badge-shipped',
  DELIVERED: 'badge-delivered',
  CANCELLED: 'badge-cancelled',
}

export default function OrdersPage() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get('/orders/my')
      .then(({ data }) => {
        const list = Array.isArray(data) ? data : (data.content ?? [])
        setOrders(list)
      })
      .catch(() => setError('Failed to load orders.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="spinner" />

  return (
    <div className="page">
      <h1>My Orders</h1>
      {error ? (
        <div className="empty-state">
          <div className="empty-state-icon">⚠️</div>
          <h2>Something went wrong</h2>
          <p>{error}</p>
        </div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📦</div>
          <h2>No orders yet</h2>
          <p>Your order history will appear here once you make a purchase.</p>
          <button className="btn-primary" onClick={() => navigate('/books')}>
            Browse Books
          </button>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => (
            <div
              key={order.id}
              className="order-row"
              onClick={() => navigate(`/orders/${order.id}`)}
            >
              <div className="order-row-left">
                <span className="order-row-id">Order #{order.id}</span>
                <span className="order-date">
                  {new Date(order.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric', month: 'short', day: 'numeric',
                  })}
                </span>
              </div>
              <span className={`badge ${STATUS_COLOR[order.status] ?? ''}`}>{order.status}</span>
              <span className="order-row-price">${Number(order.totalPrice).toFixed(2)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
