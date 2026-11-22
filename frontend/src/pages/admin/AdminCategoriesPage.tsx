import { useState, useEffect } from 'react'
import api from '../../api'
import type { Category } from '../../types'

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editing, setEditing] = useState<Category | null>(null)
  const [error, setError] = useState('')

  const load = () => api.get('/categories').then(({ data }: { data: Category[] }) => setCategories(data))

  useEffect(() => {
    load()
  }, [])

  const startEdit = (c: Category) => {
    setEditing(c)
    setName(c.name)
    setDescription(c.description)
  }

  const cancel = () => {
    setEditing(null)
    setName('')
    setDescription('')
    setError('')
  }

  const save = async () => {
    setError('')
    try {
      if (editing) {
        await api.put(`/categories/${editing.id}`, { name, description })
      } else {
        await api.post('/categories', { name, description })
      }
      cancel()
      load()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save.')
    }
  }

  const remove = async (id: number) => {
    if (!confirm('Delete this category?')) return
    await api.delete(`/categories/${id}`)
    load()
  }

  return (
    <div className="page">
      <h1>Admin — Categories</h1>

      <div className="admin-form">
        <h3>{editing ? 'Edit Category' : 'New Category'}</h3>
        <input
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          placeholder="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
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

      <table className="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {categories.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td>{c.name}</td>
              <td>{c.description}</td>
              <td>
                <button className="btn-sm btn-ghost" onClick={() => startEdit(c)}>
                  Edit
                </button>
                <button className="btn-sm btn-danger" onClick={() => remove(c.id)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
