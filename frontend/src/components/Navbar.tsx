import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import { useCartStore } from '../stores/cartStore'
import api from '../api'

export default function Navbar() {
  const navigate = useNavigate()
  const { user, token, logout } = useAuthStore()
  const cart = useCartStore((s) => s.cart)

  const cartCount = cart?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0

  const handleLogout = async () => {
    if (token) {
      try {
        await api.post('/auth/logout')
      } catch {
        // best-effort
      }
    }
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <Link to="/" className="nav-brand">
        <svg width="28" height="28" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" style={{marginRight: '0.45rem', verticalAlign: 'middle', flexShrink: 0}}>
          <rect width="32" height="32" rx="8" fill="#e11d48"/>
          <path d="M10 6 h12 a1 1 0 0 1 1 1 v19 l-6.5-4.5 L10 26 V7 a1 1 0 0 1 1-1z" fill="white" opacity="0.95"/>
        </svg>
        Bookstore
      </Link>
      <div className="nav-links">
        <Link to="/books">Browse</Link>
        {user && (
          <>
            <Link to="/cart" className="cart-link">
              Cart {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
            </Link>
            <Link to="/orders">My Orders</Link>
          </>
        )}
        {user?.role === 'ADMIN' && (
          <div className="dropdown">
            <span className="dropdown-toggle">Admin ▾</span>
            <div className="dropdown-menu">
              <Link to="/admin/books">Books</Link>
              <Link to="/admin/categories">Categories</Link>
              <Link to="/admin/orders">Orders</Link>
            </div>
          </div>
        )}
        {user ? (
          <div className="dropdown">
            <span className="dropdown-toggle">{user.firstName} ▾</span>
            <div className="dropdown-menu">
              <Link to="/profile">Profile</Link>
              <button onClick={handleLogout}>Logout</button>
            </div>
          </div>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register" className="btn-primary btn-sm nav-cta">
              Register
            </Link>
          </>
        )}
      </div>
    </nav>
  )
}
