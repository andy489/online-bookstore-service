import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="page not-found">
      <h1>404</h1>
      <p>Page not found.</p>
      <Link to="/" className="btn-primary">
        Go Home
      </Link>
    </div>
  )
}
