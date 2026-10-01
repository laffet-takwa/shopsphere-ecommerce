import type { Product } from '../types'

/**
 * Offline fallback catalogue.
 *
 * The API is the source of truth. This dataset exists so the storefront still renders something
 * credible when the gateway is unreachable, which is the difference between a confusing failure
 * screen and a storefront that explains itself. It is intentionally isolated in `src/mocks/` so
 * deleting it later cannot affect any component.
 */

interface Seed {
  name: string
  category: string
  price: number
  oldPrice?: number
  description: string
  image: string
  rating: number
  reviews: number
  badge?: string
}

const SEEDS: Seed[] = [
  {
    name: 'Wireless Noise-Cancelling Headphones',
    category: 'Electronics',
    price: 89.99,
    oldPrice: 129.99,
    rating: 4.8,
    reviews: 412,
    badge: 'Sale',
    description:
      'Over-ear headphones with adaptive noise cancelling, 40-hour battery life and a memory-foam headband built for long listening sessions.',
    image: 'photo-1505740420928-5e560c06d30e',
  },
  {
    name: 'Titan Smart Watch Series 4',
    category: 'Electronics',
    price: 149.0,
    rating: 4.6,
    reviews: 288,
    description:
      'Always-on AMOLED display with heart-rate tracking, sleep scoring and seven days of battery in a 38mm aluminium case.',
    image: 'photo-1523275335684-37898b6baf30',
  },
  {
    name: 'Low-Profile Mechanical Keyboard',
    category: 'Electronics',
    price: 118.5,
    rating: 4.7,
    reviews: 196,
    badge: 'New',
    description:
      'Hot-swappable tactile switches, PBT double-shot keycaps and a gasket-mounted aluminium plate for a softer, quieter typing feel.',
    image: 'photo-1587829741301-dc798b83add3',
  },
  {
    name: 'Everyday Runners',
    category: 'Sports',
    price: 74.95,
    oldPrice: 95.0,
    rating: 4.5,
    reviews: 634,
    badge: 'Sale',
    description:
      'Lightweight knit uppers on a responsive foam midsole, tested for 500km of daily wear across road and treadmill runs.',
    image: 'photo-1542291026-7eec264c27ff',
  },
  {
    name: 'Canvas Weekender Backpack',
    category: 'Accessories',
    price: 64.0,
    rating: 4.4,
    reviews: 152,
    description:
      'Waxed 18oz canvas with a padded 16-inch laptop sleeve, water-resistant base and leather grab handles.',
    image: 'photo-1553062407-98eeb64c6a62',
  },
  {
    name: 'Aurora 5G Smartphone',
    category: 'Electronics',
    price: 699.0,
    oldPrice: 799.0,
    rating: 4.7,
    reviews: 921,
    badge: 'Sale',
    description:
      '6.5-inch 120Hz OLED, a 50MP triple camera system and an all-day battery with 65W fast charging.',
    image: 'photo-1511707171634-5f897ff02aa9',
  },
  {
    name: 'Arc Table Lamp',
    category: 'Home & Living',
    price: 58.0,
    rating: 4.6,
    reviews: 221,
    description:
      'A weighted aluminium base with a dimmable warm-to-cool LED ring that throws an even pool of light across a desk.',
    image: 'photo-1507473885765-e6ed057f782c',
  },
  {
    name: 'Barista Espresso Machine',
    category: 'Home & Living',
    price: 429.0,
    rating: 4.9,
    reviews: 508,
    badge: 'Popular',
    description:
      '15-bar pump with a PID temperature controller, steam wand and a 1.8L reservoir for café-quality extraction at home.',
    image: 'photo-1517668808822-9ebb02f2a0e6',
  },
  {
    name: 'Linen Blend Overshirt',
    category: 'Fashion',
    price: 82.0,
    rating: 4.3,
    reviews: 174,
    description:
      'A relaxed cotton-linen overshirt with corozo buttons, garment-washed for softness from the first wear.',
    image: 'photo-1596755094514-f87e34085b2c',
  },
  {
    name: 'Vitamin C Brightening Serum',
    category: 'Beauty',
    price: 34.5,
    rating: 4.5,
    reviews: 743,
    description:
      'A 15% vitamin C blend with ferulic acid and hyaluronic acid that visibly evens tone in four weeks.',
    image: 'photo-1620916566398-39f1143ab7be',
  },
  {
    name: 'Merino Crew Socks',
    category: 'Fashion',
    price: 16.0,
    rating: 4.6,
    reviews: 289,
    description:
      'Temperature-regulating merino wool with a reinforced heel and a seamless toe, packed as a two-pair set.',
    image: 'photo-1586350977771-b3b0abd50c82',
  },
  {
    name: 'Matte Yoga Mat',
    category: 'Sports',
    price: 48.0,
    rating: 4.7,
    reviews: 356,
    description:
      'A 5mm natural-rubber mat with an aligned-grid top layer for alignment cues and a grippy, low-odour surface.',
    image: 'photo-1601925260368-ae2f83cf8b7f',
  },
]

export const CATEGORY_META: Array<{
  name: string
  blurb: string
  image: string
}> = [
  { name: 'Electronics', blurb: 'Audio, wearables and everyday tech', image: 'photo-1498049794561-7780e7231661' },
  { name: 'Fashion', blurb: 'Considered staples and knitwear', image: 'photo-1441986300917-64674bd600d8' },
  { name: 'Home & Living', blurb: 'Objects for better rooms and rituals', image: 'photo-1616486338812-3dadae4b4ace' },
  { name: 'Beauty', blurb: 'Skincare that earns its place', image: 'photo-1596462502278-27bfdc403348' },
  { name: 'Sports', blurb: 'Training, recovery and movement', image: 'photo-1461896836934-ffe607ba8211' },
  { name: 'Accessories', blurb: 'Bags, watches and finishing touches', image: 'photo-1523275335684-37898b6baf30' },
]

function imageUrl(image: string, width = 900): string {
  return `https://images.unsplash.com/${image}?auto=format&fit=crop&w=${width}&q=80`
}

export const MOCK_PRODUCTS: Product[] = SEEDS.map((seed, index) => ({
  id: 9000 + index,
  name: seed.name,
  description: seed.description,
  price: seed.price,
  category: seed.category,
  sku: `SS-${seed.category.slice(0, 3).toUpperCase()}-${String(index + 1).padStart(4, '0')}`,
  imageUrl: imageUrl(seed.image),
  active: true,
  createdAt: new Date(Date.now() - index * 86400000).toISOString(),
  updatedAt: new Date().toISOString(),
}))

export interface DemoRating {
  rating: number
  reviews: number
  badge?: string
  oldPrice?: number
}

const RATING_BY_ID = new Map<number, DemoRating>(
  MOCK_PRODUCTS.map((product, index) => {
    const seed = SEEDS[index]
    return [
      product.id,
      {
        rating: seed.rating,
        reviews: seed.reviews,
        ...(seed.badge ? { badge: seed.badge } : {}),
        ...(seed.oldPrice ? { oldPrice: seed.oldPrice } : {}),
      },
    ]
  }),
)

/** Presentation metadata. In a production build this would come from a reviews service. */
export function demoRating(productId: number): DemoRating {
  return RATING_BY_ID.get(productId) ?? { rating: 4.5, reviews: 0 }
}

export function categoryImage(category: string): string {
  const meta = CATEGORY_META.find((item) => item.name === category)
  return imageUrl(meta ? meta.image : 'photo-1441986300917-64674bd600d8', 600)
}

/** Deterministic fallback for products coming from the API without an image URL. */
export function fallbackImage(category: string): string {
  const known: Record<string, string> = {
    Electronics: 'photo-1498049794561-7780e7231661',
    Fashion: 'photo-1441986300917-64674bd600d8',
    'Home & Living': 'photo-1616486338812-3dadae4b4ace',
    Beauty: 'photo-1596462502278-27bfdc403348',
    Sports: 'photo-1461896836934-ffe607ba8211',
    Accessories: 'photo-1523275335684-37898b6baf30',
  }
  return imageUrl(known[category] ?? 'photo-1441984904996-e0b6ba687e04', 900)
}

export function imageFor(product: Pick<Product, 'id' | 'imageUrl' | 'category'>): string {
  if (product.imageUrl) return product.imageUrl
  return fallbackImage(product.category)
}
