import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import type { Book, PageResponse, Category } from '../types'
import { useCartStore } from '../stores/cartStore'

const SORT_OPTIONS = [
  { label: 'Title A–Z', sortBy: 'title', sortDir: 'asc' },
  { label: 'Title Z–A', sortBy: 'title', sortDir: 'desc' },
  { label: 'Price Low–High', sortBy: 'price', sortDir: 'asc' },
  { label: 'Price High–Low', sortBy: 'price', sortDir: 'desc' },
  { label: 'Newest', sortBy: 'createdAt', sortDir: 'desc' },
]

// Valid: empty, "10", "10.5", "10.99" — at most 2 decimal places, no letters
function isValidPrice(val: string): boolean {
  if (val === '') return true
  return /^\d+(\.\d{1,2})?$/.test(val)
}

export default function BooksPage() {
  const addItem = useCartStore((s) => s.addItem)
  const [books, setBooks] = useState<PageResponse<Book> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [minPriceErr, setMinPriceErr] = useState('')
  const [maxPriceErr, setMaxPriceErr] = useState('')
  const [sortIdx, setSortIdx] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [adding, setAdding] = useState<number | null>(null)
  const [addedMap, setAddedMap] = useState<Record<number, boolean>>({})

  const pricesValid = !minPriceErr && !maxPriceErr

  const handleMinPrice = (val: string) => {
    setMinPrice(val)
    if (!isValidPrice(val)) {
      setMinPriceErr('Use format: 9.99')
    } else if (val && maxPrice && isValidPrice(maxPrice) && Number(val) > Number(maxPrice)) {
      setMinPriceErr('Must be ≤ max')
    } else {
      setMinPriceErr('')
      // clear max cross-error too if it was set for the same reason
      if (maxPriceErr === 'Must be ≥ min') setMaxPriceErr('')
    }
  }

  const handleMaxPrice = (val: string) => {
    setMaxPrice(val)
    if (!isValidPrice(val)) {
      setMaxPriceErr('Use format: 9.99')
    } else if (val && minPrice && isValidPrice(minPrice) && Number(val) < Number(minPrice)) {
      setMaxPriceErr('Must be ≥ min')
    } else {
      setMaxPriceErr('')
      if (minPriceErr === 'Must be ≤ max') setMinPriceErr('')
    }
  }

  const fetchBooks = useCallback(async () => {
    if (!pricesValid) return
    setLoading(true)
    try {
      const sort = SORT_OPTIONS[sortIdx]
      const params: Record<string, string | number> = {
        page,
        size: 12,
        sortBy: sort.sortBy,
        sortDir: sort.sortDir,
      }
      if (query) params.query = query
      if (categoryId) params.categoryId = categoryId
      if (minPrice) params.minPrice = minPrice
      if (maxPrice) params.maxPrice = maxPrice
      const { data } = await api.get('/books', { params })
      setBooks(data)
    } finally {
      setLoading(false)
    }
  }, [query, categoryId, minPrice, maxPrice, pricesValid, sortIdx, page])

  useEffect(() => {
    api.get('/categories').then(({ data }) => setCategories(data))
  }, [])

  useEffect(() => {
    setPage(0)
  }, [query, categoryId, minPrice, maxPrice, sortIdx])

  useEffect(() => {
    fetchBooks()
  }, [fetchBooks])

  const handleAdd = async (book: Book) => {
    setAdding(book.id)
    try {
      await addItem(book.id, 1)
      setAddedMap((m) => ({ ...m, [book.id]: true }))
      setTimeout(() => setAddedMap((m) => ({ ...m, [book.id]: false })), 1500)
    } catch {
      // cart error handled globally
    } finally {
      setAdding(null)
    }
  }

  return (
    <div className="page">
      <div className="filters-bar">
        <input
          className="search-input"
          placeholder="Search books, authors…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <input
          placeholder="Min $"
          type="text"
          inputMode="decimal"
          value={minPrice}
          onChange={(e) => handleMinPrice(e.target.value)}
          className={`price-input${minPriceErr ? ' input-error' : ''}`}
          title={minPriceErr || undefined}
        />
        <input
          placeholder="Max $"
          type="text"
          inputMode="decimal"
          value={maxPrice}
          onChange={(e) => handleMaxPrice(e.target.value)}
          className={`price-input${maxPriceErr ? ' input-error' : ''}`}
          title={maxPriceErr || undefined}
        />
        <select value={sortIdx} onChange={(e) => setSortIdx(Number(e.target.value))}>
          {SORT_OPTIONS.map((o, i) => (
            <option key={i} value={i}>
              {o.label}
            </option>
          ))}
        </select>
      </div>
      {(minPriceErr || maxPriceErr) && (
        <p className="price-filter-err">
          {minPriceErr && <span>Min price: {minPriceErr}</span>}
          {minPriceErr && maxPriceErr && ' · '}
          {maxPriceErr && <span>Max price: {maxPriceErr}</span>}
        </p>
      )}
      {loading && <div className="spinner" />}

      {!loading && books && (
        <>
          <p className="result-count">
            {books.totalElements} book{books.totalElements !== 1 ? 's' : ''}
          </p>
          {books.content.length === 0 ? (
            <div className="empty-state">No books found. Try different filters.</div>
          ) : (
            <div className="books-grid">
              {books.content.map((book) => (
                <div key={book.id} className="book-card">
                  <Link to={`/books/${book.id}`}>
                    <img
                      src={book.coverImageUrl}
                      alt={book.title}
                      className="book-cover"
                      onError={(e) => {
                        ;(e.target as HTMLImageElement).src = '/placeholder-cover.svg'
                      }}
                    />
                  </Link>
                  <div className="book-info">
                    <Link to={`/books/${book.id}`} className="book-title">
                      {book.title}
                    </Link>
                    <p className="book-author">{book.author}</p>
                    <p className="book-price">${Number(book.price).toFixed(2)}</p>
                    {!book.inStock ? (
                      <span className="badge badge-out">Out of stock</span>
                    ) : (
                      <button
                        className="btn-primary btn-sm"
                        disabled={adding === book.id || addedMap[book.id]}
                        onClick={() => handleAdd(book)}
                      >
                        {addedMap[book.id] ? '✓ Added' : adding === book.id ? '…' : 'Add to Cart'}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {books.page + 1} of {books.totalPages}
            </span>
            <button
              disabled={page >= books.totalPages - 1}
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
