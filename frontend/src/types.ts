export interface User {
  id: number
  email: string
  firstName: string
  lastName: string
  role: 'USER' | 'ADMIN'
}

export interface Category {
  id: number
  name: string
  description: string
}

export interface Book {
  id: number
  title: string
  author: string
  description: string
  price: number
  inStock: boolean
  coverImageUrl: string
  categoryId: number
  categoryName: string
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CartItem {
  bookId: number
  title: string
  author: string
  coverImageUrl: string
  price: number
  quantity: number
  subtotal: number
}

export interface Cart {
  id: number
  items: CartItem[]
  total: number
}

export interface OrderAdjustment {
  bookTitle: string
  requested: number
  fulfilled: number
  reason: string
}

export interface OrderItem {
  bookId: number
  title: string
  author: string
  quantity: number
  priceAtPurchase: number
  subtotal: number
}

export interface OrderCustomer {
  id: number
  email: string
  firstName: string
  lastName: string
}

export interface Order {
  id: number
  customer: OrderCustomer
  status: 'PENDING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
  totalPrice: number
  items: OrderItem[]
  adjustments?: OrderAdjustment[]
  createdAt: string
  updatedAt: string
}

export interface OrderResponse extends Order {
  adjustments: OrderAdjustment[]
}
