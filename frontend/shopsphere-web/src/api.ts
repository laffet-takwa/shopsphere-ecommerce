import axios from 'axios'

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080' })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('shopsphere-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export interface Product {
  id: number
  name: string
  description: string
  price: number
  category: string
  sku: string
  imageUrl?: string
  active: boolean
}

export interface CartLine { product: Product; quantity: number }

export function imageFor(product: Product) {
  if (product.imageUrl) return product.imageUrl
  const images: Record<string, string> = {
    Home: 'photo-1616486338812-3dadae4b4ace',
    Tech: 'photo-1496181133206-80ce9b88a853',
    Wellness: 'photo-1608571423902-eed4a5ad8108',
    Accessories: 'photo-1523275335684-37898b6baf30',
  }
  return `https://images.unsplash.com/${images[product.category] ?? 'photo-1490481651871-ab68de25d43d'}?auto=format&fit=crop&w=900&q=85`
}