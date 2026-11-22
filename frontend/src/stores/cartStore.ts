import { create } from 'zustand'
import api from '../api'
import type { Cart } from '../types'

interface CartState {
  cart: Cart | null
  loading: boolean
  fetch: () => Promise<void>
  addItem: (bookId: number, quantity: number) => Promise<void>
  updateItem: (bookId: number, quantity: number) => Promise<void>
  removeItem: (bookId: number) => Promise<void>
  clear: () => Promise<void>
}

export const useCartStore = create<CartState>((set) => ({
  cart: null,
  loading: false,
  fetch: async () => {
    set({ loading: true })
    try {
      const { data } = await api.get('/cart')
      set({ cart: data })
    } finally {
      set({ loading: false })
    }
  },
  addItem: async (bookId, quantity) => {
    await api.post('/cart/items', { bookId, quantity })
    const { data } = await api.get('/cart')
    set({ cart: data })
  },
  updateItem: async (bookId, quantity) => {
    await api.put(`/cart/items/${bookId}`, { quantity })
    const { data } = await api.get('/cart')
    set({ cart: data })
  },
  removeItem: async (bookId) => {
    await api.delete(`/cart/items/${bookId}`)
    const { data } = await api.get('/cart')
    set({ cart: data })
  },
  clear: async () => {
    await api.delete('/cart')
    set({ cart: { id: 0, items: [], total: 0 } })
  },
}))
