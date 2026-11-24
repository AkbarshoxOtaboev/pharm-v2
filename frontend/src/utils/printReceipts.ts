import type { OrderDetailResponse } from '@/types/api'
import { dateTime, money } from '@/utils/format'

export interface ReceiptLabels {
  title: string
  number: string
  date: string
  customer: string
  phone: string
  address: string
  place: string
  house: string
  entrance: string
  apartment: string
  floor: string
  orientation: string
  courier: string
  product: string
  quantity: string
  price: string
  amount: string
  bonus: string
  total: string
  empty: string
}

const HOST_ID = 'pharm-print-host'
const STYLE_ID = 'pharm-print-style'

function esc(value: unknown) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function cell(value?: string | null, fallback = '') {
  const text = value?.trim()
  return esc(text || fallback)
}

function addressText(order: OrderDetailResponse, labels: ReceiptLabels) {
  const parts = [order.address?.trim()]
  const bits = [
    order.house?.trim() ? `${labels.house}: ${order.house.trim()}` : '',
    order.entrance?.trim() ? `${labels.entrance}: ${order.entrance.trim()}` : '',
    order.apartment?.trim() ? `${labels.apartment}: ${order.apartment.trim()}` : '',
    order.floor?.trim() ? `${labels.floor}: ${order.floor.trim()}` : '',
  ].filter(Boolean)
  if (bits.length) parts.push(bits.join(' · '))
  if (order.orientation?.trim()) parts.push(`${labels.orientation}: ${order.orientation.trim()}`)
  return parts.filter(Boolean).join(' — ')
}

function receiptHtml(order: OrderDetailResponse, labels: ReceiptLabels) {
  const items = order.items || []
  const rows = items.length
    ? items
        .map(
          (item) => `<tr>
            <td>${esc(item.productName || item.productId || labels.empty)}${item.isBonus ? ` <em>(${esc(labels.bonus)})</em>` : ''}</td>
            <td class="num">${esc(item.quantity ?? 0)}</td>
            <td class="num">${esc(money(item.price))}</td>
            <td class="num">${esc(money(item.totalSum))}</td>
          </tr>`,
        )
        .join('')
    : `<tr><td colspan="4" class="empty">${esc(labels.empty)}</td></tr>`

  const place = [order.regionName, order.districtName].filter(Boolean).join(' · ')

  return `<article class="receipt">
    <div class="head">
      <span>${esc(labels.number)}: ${cell(order.orderNumber || String(order.orderId), labels.empty)}</span>
      <span>${esc(labels.date)}: ${esc(dateTime(order.createdDate) || labels.empty)}</span>
    </div>
    <table class="meta">
      <thead>
        <tr>
          <th>${esc(labels.customer)}</th>
          <th>${esc(labels.phone)}</th>
          <th>${esc(labels.address)}</th>
          <th>${esc(labels.place)}</th>
          <th>${esc(labels.courier)}</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>${cell(order.fullName, labels.empty)}</td>
          <td>${cell(order.phone, labels.empty)}</td>
          <td>${cell(addressText(order, labels), labels.empty)}</td>
          <td>${cell(place, labels.empty)}</td>
          <td>${cell(order.courier?.fullName, labels.empty)}</td>
        </tr>
      </tbody>
    </table>
    <table class="items">
      <thead>
        <tr>
          <th>${esc(labels.product)}</th>
          <th class="num">${esc(labels.quantity)}</th>
          <th class="num">${esc(labels.price)}</th>
          <th class="num">${esc(labels.amount)}</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
    <p class="total"><span>${esc(labels.total)}</span> ${esc(money(order.totalAmount))}</p>
  </article>`
}

function ensureStyle() {
  let style = document.getElementById(STYLE_ID) as HTMLStyleElement | null
  if (!style) {
    style = document.createElement('style')
    style.id = STYLE_ID
    document.head.appendChild(style)
  }
  style.textContent = `
    #${HOST_ID} { display: none; }
    @page { size: A4 portrait; margin: 8mm; }
    @media print {
      body > * { display: none !important; }
      #${HOST_ID} {
        display: block !important;
        position: static !important;
        width: 100%;
        background: #fff !important;
        color: #111 !important;
      }
      #${HOST_ID}, #${HOST_ID} * { color: #111 !important; }
      #${HOST_ID} .receipt {
        width: 100%;
        margin: 0 0 4mm;
        page-break-inside: avoid;
        break-inside: avoid;
        font: 11px/1.3 Arial, sans-serif;
      }
      #${HOST_ID} .head {
        display: flex;
        align-items: baseline;
        gap: 14px;
        margin-bottom: 2mm;
        padding-bottom: 1.5mm;
        border-bottom: 1.5px solid #111;
      }
      #${HOST_ID} table { width: 100%; border-collapse: collapse; }
      #${HOST_ID} th, #${HOST_ID} td {
        border: 1px solid #222;
        padding: 2px 6px;
        text-align: left;
        vertical-align: top;
        background: #fff !important;
      }
      #${HOST_ID} th { font-weight: 700; white-space: nowrap; }
      #${HOST_ID} .meta { margin-bottom: 2mm; }
      #${HOST_ID} .meta td:nth-child(3) { width: 34%; }
      #${HOST_ID} .items { margin-top: 0; }
      #${HOST_ID} .num { text-align: right; white-space: nowrap; width: 1%; }
      #${HOST_ID} .empty { text-align: center; }
      #${HOST_ID} .total {
        margin: 1.5mm 0 0;
        font-size: 12px;
        font-weight: 700;
        display: flex;
        justify-content: flex-end;
        gap: 16px;
      }
      #${HOST_ID} em { font-style: normal; font-size: 10px; }
    }
  `
}

export function printOrderReceipts(orders: OrderDetailResponse[], labels: ReceiptLabels) {
  if (!orders.length) return false
  ensureStyle()
  let host = document.getElementById(HOST_ID)
  if (!host) {
    host = document.createElement('div')
    host.id = HOST_ID
    document.body.appendChild(host)
  }
  host.innerHTML = orders.map((order) => receiptHtml(order, labels)).join('')
  const clear = () => {
    host.innerHTML = ''
    window.removeEventListener('afterprint', clear)
  }
  window.addEventListener('afterprint', clear)
  window.print()
  return true
}
