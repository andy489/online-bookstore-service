import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuthStore } from '../stores/authStore'
import type { Order } from '../types'

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-pending',
  SHIPPED: 'badge-shipped',
  DELIVERED: 'badge-delivered',
  CANCELLED: 'badge-cancelled',
}

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isAdmin = useAuthStore((s) => s.user?.role === 'ADMIN')
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get(`/orders/${id}`)
      .then(({ data }) => setOrder(data))
      .catch(() => navigate('/orders', { replace: true }))
      .finally(() => setLoading(false))
  }, [id, navigate])

  if (loading) return <div className="spinner" />
  if (!order) return null

  return (
    <div className="page">
      <button className="btn-ghost" onClick={() => navigate('/orders')}>
        ← My Orders
      </button>
      <h1>Order #{order.id}</h1>
      <div className="order-meta">
        <span className={`badge ${STATUS_COLOR[order.status]}`}>{order.status}</span>
        <span>{new Date(order.createdAt).toLocaleString()}</span>
      </div>

      {isAdmin && order.customer && (
        <div className="order-customer">
          <span className="customer-name">
            {order.customer.firstName} {order.customer.lastName}
          </span>
          <span className="customer-email">{order.customer.email}</span>
        </div>
      )}

      <div className="order-items">
        {order.items.map((item, i) => (
          <div key={i} className="order-item-row">
            <div className="order-item-book">
              <span className="order-item-title">{item.title}</span>
              <span className="order-item-author">{item.author}</span>
            </div>
            <span className="order-item-qty">×{item.quantity}</span>
            <span className="order-item-price">${(Number(item.priceAtPurchase) * item.quantity).toFixed(2)}</span>
          </div>
        ))}
      </div>

      <div className="order-total">
        <strong>Total: ${Number(order.totalPrice).toFixed(2)}</strong>
      </div>
    </div>
  )
}
