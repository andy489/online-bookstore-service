import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCartStore } from '../stores/cartStore'

export default function CartPage() {
  const navigate = useNavigate()
  const { cart, loading, fetch, updateItem, removeItem, clear } = useCartStore()

  useEffect(() => {
    fetch()
  }, [fetch])

  if (loading && !cart) return <div className="spinner" />

  const items = cart?.items ?? []

  return (
    <div className="page">
      <h1>Shopping Cart</h1>
      {items.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🛒</div>
          <h2>Your cart is empty</h2>
          <p>Add some books to get started.</p>
          <button className="btn-primary" onClick={() => navigate('/books')}>
            Browse Books
          </button>
        </div>
      ) : (
        <>
          <div className="cart-list">
            {items.map((item) => (
              <div key={item.bookId} className="cart-item">
                <img
                  src={item.coverImageUrl}
                  alt={item.title}
                  className="cart-item-cover"
                  onError={(e) => {
                    ;(e.target as HTMLImageElement).src = '/placeholder-cover.svg'
                  }}
                />
                <div className="cart-item-info">
                  <p className="cart-item-title">{item.title}</p>
                  <p className="cart-item-author">{item.author}</p>
                </div>
                <div className="cart-item-controls">
                  <button
                    className="qty-btn"
                    disabled={item.quantity <= 1}
                    onClick={() => updateItem(item.bookId, item.quantity - 1)}
                  >
                    −
                  </button>
                  <span className="qty-value">{item.quantity}</span>
                  <button
                    className="qty-btn"
                    onClick={() => updateItem(item.bookId, item.quantity + 1)}
                  >
                    +
                  </button>
                </div>
                <p className="cart-item-price">
                  ${(Number(item.price) * item.quantity).toFixed(2)}
                </p>
                <button className="btn-danger btn-sm" onClick={() => removeItem(item.bookId)}>
                  Remove
                </button>
              </div>
            ))}
          </div>

          <div className="cart-footer">
            <button className="btn-ghost" onClick={() => clear()}>
              Clear Cart
            </button>
            <div className="cart-total">
              <strong>Total: ${Number(cart?.total ?? 0).toFixed(2)}</strong>
              <button className="btn-primary" onClick={() => navigate('/checkout')}>
                Checkout
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
