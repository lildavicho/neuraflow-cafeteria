import { useState } from 'react'
import PropTypes from 'prop-types'

const TABS = [
  { key: 'TRANSFER', label: 'Transferencia' },
  { key: 'CASH', label: 'Efectivo' },
  { key: 'CARD', label: 'Tarjeta' },
]

const ModalCheckout = ({ open, onClose, onConfirm, total, items }) => {
  const [method, setMethod] = useState('TRANSFER')
  const [reference, setReference] = useState('')

  if (!open) return null

  const handleConfirm = async () => {
    await onConfirm({ method, reference: method === 'TRANSFER' ? reference.trim() : undefined })
  }

  return (
    <div className="fixed inset-0 z-[10000] bg-black/60 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-2xl">
        <header className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Checkout</h2>
          <button type="button" onClick={onClose} className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
            <i className="bi bi-x-lg"></i>
          </button>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-[1fr_1fr]">
          <section className="p-4 border-b md:border-b-0 md:border-r border-gray-200 dark:border-gray-700">
            <nav className="flex gap-2 mb-4">
              {TABS.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setMethod(tab.key)}
                  className={`px-3 py-2 rounded-lg border text-sm ${
                    method === tab.key
                      ? 'border-brand text-brand bg-brand/10'
                      : 'border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </nav>

            {method === 'TRANSFER' && (
              <div className="space-y-3">
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Realiza la transferencia y coloca la referencia para validar más rápido.
                </p>
                <input
                  type="text"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  placeholder="Referencia de transferencia"
                  className="input-brand w-full"
                />
              </div>
            )}

            {method === 'CASH' && (
              <div className="text-sm text-gray-600 dark:text-gray-400">
                Se confirmará al entregar el pedido.
              </div>
            )}

            {method === 'CARD' && (
              <div className="text-sm text-gray-600 dark:text-gray-400">
                Se iniciará el pago con tarjeta.
              </div>
            )}
          </section>

          <aside className="p-4 space-y-3">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Resumen</h3>
            <div className="max-h-60 overflow-auto space-y-2">
              {items.map((it) => (
                <div key={it.id} className="flex items-center justify-between text-sm">
                  <span className="text-gray-800 dark:text-gray-200">{it.name} x{it.quantity || 1}</span>
                  <span className="text-gray-900 dark:text-gray-100 font-medium">${Number(it.price ?? 0).toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div className="flex items-center justify-between text-xl font-bold border-t pt-3 border-gray-200 dark:border-gray-700">
              <span className="text-gray-900 dark:text-gray-100">Total</span>
              <span className="text-brand">${total.toFixed(2)}</span>
            </div>
            <button type="button" onClick={handleConfirm} className="w-full btn-brand">Continuar</button>
          </aside>
        </div>
      </div>
    </div>
  )
}

ModalCheckout.propTypes = {
  open: PropTypes.bool,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  total: PropTypes.number,
  items: PropTypes.array,
}

export default ModalCheckout
