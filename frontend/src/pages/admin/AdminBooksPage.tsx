import { useState, useEffect, useRef } from 'react'
import api from '../../api'
import type { Book, Category, PageResponse } from '../../types'

export default function AdminBooksPage() {
  const [books, setBooks] = useState<PageResponse<Book> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [page, setPage] = useState(0)
  const [form, setForm] = useState({
    title: '',
    author: '',
    description: '',
    price: '',
    stock: '',
    categoryId: '',
  })
  const [editing, setEditing] = useState<Book | null>(null)
  const [error, setError] = useState('')
  const [coverFile, setCoverFile] = useState<File | null>(null)
  const [uploadingId, setUploadingId] = useState<number | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const load = () =>
    api
      .get('/books', { params: { page, size: 10, sortBy: 'id', sortDir: 'asc' } })
      .then(({ data }) => setBooks(data))

  useEffect(() => {
    api.get('/categories').then(({ data }) => setCategories(data))
  }, [])

  useEffect(() => {
    load()
  }, [page])

  const setField = (field: string, value: string) =>
    setForm((f) => ({ ...f, [field]: value }))

  const startEdit = (b: Book) => {
    setEditing(b)
    setForm({
      title: b.title,
      author: b.author,
      description: b.description,
      price: String(b.price),
      stock: String(b.stock),
      categoryId: String(b.category?.id ?? ''),
    })
  }

  const cancel = () => {
    setEditing(null)
    setForm({ title: '', author: '', description: '', price: '', stock: '', categoryId: '' })
    setError('')
  }

  const save = async () => {
    setError('')
    const payload = {
      ...form,
      price: parseFloat(form.price),
      stock: parseInt(form.stock, 10),
      categoryId: form.categoryId ? parseInt(form.categoryId, 10) : undefined,
    }
    try {
      if (editing) {
        await api.put(`/books/${editing.id}`, payload)
      } else {
        await api.post('/books', payload)
      }
      cancel()
      load()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save.')
    }
  }

  const remove = async (id: number) => {
    if (!confirm('Soft-delete this book?')) return
    await api.delete(`/books/${id}`)
    load()
  }

  const uploadCover = async (bookId: number) => {
    if (!coverFile) return
    setUploadingId(bookId)
    const fd = new FormData()
    fd.append('file', coverFile)
    try {
      await api.post(`/books/${bookId}/cover`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setCoverFile(null)
      if (fileRef.current) fileRef.current.value = ''
      load()
    } catch (err: any) {
      alert(err.response?.data?.message || 'Upload failed.')
    } finally {
      setUploadingId(null)
    }
  }

  return (
    <div className="page">
      <h1>Admin — Books</h1>

      <div className="admin-form">
        <h3>{editing ? `Editing: ${editing.title}` : 'New Book'}</h3>
        <input placeholder="Title" value={form.title} onChange={(e) => setField('title', e.target.value)} />
        <input placeholder="Author" value={form.author} onChange={(e) => setField('author', e.target.value)} />
        <textarea
          placeholder="Description"
          value={form.description}
          onChange={(e) => setField('description', e.target.value)}
          rows={3}
        />
        <div className="form-row">
          <input placeholder="Price" type="number" step="0.01" value={form.price} onChange={(e) => setField('price', e.target.value)} />
          <input placeholder="Stock" type="number" value={form.stock} onChange={(e) => setField('stock', e.target.value)} />
        </div>
        <select value={form.categoryId} onChange={(e) => setField('categoryId', e.target.value)}>
          <option value="">Select category</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {error && <p className="error-msg">{error}</p>}
        <div className="form-actions">
          <button className="btn-primary" onClick={save}>
            {editing ? 'Update' : 'Create'}
          </button>
          {editing && (
            <button className="btn-ghost" onClick={cancel}>
              Cancel
            </button>
          )}
        </div>
      </div>

      {books && (
        <>
          <table className="admin-table">
            <thead>
              <tr>
                <th>Cover</th>
                <th>Title</th>
                <th>Author</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {books.content.map((b) => (
                <tr key={b.id}>
                  <td>
                    <img src={b.coverImageUrl} alt="" className="admin-thumb" />
                  </td>
                  <td>{b.title}</td>
                  <td>{b.author}</td>
                  <td>${Number(b.price).toFixed(2)}</td>
                  <td>{b.stock}</td>
                  <td className="actions-cell">
                    <button className="btn-sm btn-ghost" onClick={() => startEdit(b)}>
                      Edit
                    </button>
                    <button className="btn-sm btn-danger" onClick={() => remove(b.id)}>
                      Delete
                    </button>
                    <div className="cover-upload-row">
                      <input
                        ref={fileRef}
                        type="file"
                        accept="image/*"
                        onChange={(e) => setCoverFile(e.target.files?.[0] ?? null)}
                      />
                      <button
                        className="btn-sm btn-ghost"
                        disabled={!coverFile || uploadingId === b.id}
                        onClick={() => uploadCover(b.id)}
                      >
                        {uploadingId === b.id ? 'Uploading…' : 'Upload Cover'}
                      </button>
                    </div>
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
