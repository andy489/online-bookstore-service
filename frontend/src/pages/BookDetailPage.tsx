import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api'
import type { Book } from '../types'
import { useCartStore } from '../stores/cartStore'

export default function BookDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const addItem = useCartStore((s) => s.addItem)
  const [book, setBook] = useState<Book | null>(null)
  const [loading, setLoading] = useState(true)
  const [quantity, setQuantity] = useState(1)
  const [added, setAdded] = useState(false)
  const [adding, setAdding] = useState(false)

  useEffect(() => {
    api
      .get(`/books/${id}`)
      .then(({ data }) => setBook(data))
      .catch(() => navigate('/books', { replace: true }))
      .finally(() => setLoading(false))
  }, [id, navigate])

  const handleAdd = async () => {
    if (!book) return
    setAdding(true)
    try {
      await addItem(book.id, quantity)
      setAdded(true)
      setTimeout(() => setAdded(false), 2000)
    } finally {
      setAdding(false)
    }
  }

  if (loading) return <div className="spinner" />
  if (!book) return null

  return (
    <div className="page book-detail">
      <button className="btn-ghost" onClick={() => navigate(-1)}>
        ← Back
      </button>
      <div className="book-detail-layout">
        <img
          src={book.coverImageUrl}
          alt={book.title}
          className="book-detail-cover"
          onError={(e) => {
            ;(e.target as HTMLImageElement).src = '/placeholder-cover.svg'
          }}
        />
        <div className="book-detail-info">
          <h1>{book.title}</h1>
          <p className="book-author">{book.author}</p>
          {book.categoryName && <p className="book-category">{book.categoryName}</p>}
          <p className="book-detail-price">${Number(book.price).toFixed(2)}</p>
          <p className="book-description">{book.description}</p>

          {!book.inStock ? (
            <span className="badge badge-out">Out of stock</span>
          ) : (
            <div className="add-to-cart-section">
              <div className="add-to-cart-row">
                <div className="qty-control">
                  <button
                    className="qty-btn"
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    disabled={quantity <= 1}
                  >−</button>
                  <span className="qty-value">{quantity}</span>
                  <button
                    className="qty-btn"
                    onClick={() => setQuantity((q) => q + 1)}
                  >+</button>
                </div>
                <button className="btn-primary btn-lg" disabled={adding || added} onClick={handleAdd}>
                  {added ? '✓ Added to Cart' : adding ? 'Adding…' : 'Add to Cart'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
