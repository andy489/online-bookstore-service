import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../api'
import type { Order, PageResponse } from '../../types'

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-pending',
  SHIPPED: 'badge-shipped',
  DELIVERED: 'badge-delivered',
  CANCELLED: 'badge-cancelled',
}

const NEXT_STATUSES: Record<string, string[]> = {
  PENDING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
}

export default function AdminOrdersPage() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<PageResponse<Order> | null>(null)
  const [page, setPage] = useState(0)
  const [updating, setUpdating] = useState<number | null>(null)

  const load = () =>
    api
      .get('/orders', { params: { page, size: 15 } })
      .then(({ data }) => setOrders(data))

  useEffect(() => {
    load()
  }, [page])

  const updateStatus = async (orderId: number, status: string) => {
    setUpdating(orderId)
    try {
      await api.patch(`/orders/${orderId}/status`, { status })
      load()
    } finally {
      setUpdating(null)
    }
  }

  return (
    <div className="page">
      <h1>Admin — Orders</h1>
      {orders && (
        <>
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Customer</th>
                <th>Date</th>
                <th>Total</th>
                <th>Status</th>
                <th>Transition</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {orders.content.map((o) => (
                <tr key={o.id}>
                  <td>#{o.id}</td>
                  <td>
                    {o.customer ? (
                      <div className="customer-cell">
                        <span className="customer-name">
                          {o.customer.firstName} {o.customer.lastName}
                        </span>
                        <span className="customer-email">{o.customer.email}</span>
                      </div>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{new Date(o.createdAt).toLocaleDateString()}</td>
                  <td>${Number(o.totalPrice).toFixed(2)}</td>
                  <td>
                    <span className={`badge ${STATUS_COLOR[o.status]}`}>{o.status}</span>
                  </td>
                  <td>
                    {NEXT_STATUSES[o.status].map((s) => (
                      <button
                        key={s}
                        className="btn-sm btn-ghost"
                        disabled={updating === o.id}
                        onClick={() => updateStatus(o.id, s)}
                      >
                        → {s}
                      </button>
                    ))}
                  </td>
                  <td>
                    <button
                      className="btn-sm btn-ghost"
                      onClick={() => navigate(`/orders/${o.id}`)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {orders.page + 1} of {orders.totalPages}
            </span>
            <button
              disabled={page >= orders.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  )
}
