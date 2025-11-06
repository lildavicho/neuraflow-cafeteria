import { useRef } from 'react'
import PropTypes from 'prop-types'

const Receipt = ({ order, onClose }) => {
  const receiptRef = useRef(null)

  const handlePrint = () => {
    globalThis.print()
  }

  const handleDownloadPDF = () => {
    globalThis.print()
  }

  const formatDate = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  }

  const formatTime = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleTimeString('es-EC', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <div className="fixed inset-0 bg-black/60 z-[9999] flex items-center justify-center p-4 print:bg-white">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-md w-full print:shadow-none print:max-w-full">
        <div className="p-6 print:p-0">
          <div ref={receiptRef} className="space-y-4">
            <div className="text-center border-b-2 border-dashed border-gray-300 dark:border-gray-600 pb-4 print:border-black">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 print:text-black">Cafetería</h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 print:text-black">El Bar de las Águilas Rojas</p>
              <p className="text-xs text-gray-500 dark:text-gray-500 mt-2 print:text-black">Universidad Católica de Cuenca</p>
            </div>

            <div className="border-b border-dashed border-gray-300 dark:border-gray-600 pb-4 print:border-black">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400 print:text-black">Fecha:</span>
                <span className="font-medium text-gray-900 dark:text-gray-100 print:text-black">
                  {formatDate(order.createdAt)}
                </span>
              </div>
              <div className="flex justify-between text-sm mt-1">
                <span className="text-gray-600 dark:text-gray-400 print:text-black">Hora:</span>
                <span className="font-medium text-gray-900 dark:text-gray-100 print:text-black">
                  {formatTime(order.createdAt)}
                </span>
              </div>
              <div className="flex justify-between text-sm mt-1">
                <span className="text-gray-600 dark:text-gray-400 print:text-black">Orden #:</span>
                <span className="font-medium text-gray-900 dark:text-gray-100 print:text-black">
                  {order.id}
                </span>
              </div>
            </div>

            <div className="border-b border-dashed border-gray-300 dark:border-gray-600 pb-4 print:border-black">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-300 dark:border-gray-600 print:border-black">
                    <th className="text-left py-2 text-gray-700 dark:text-gray-300 print:text-black">Producto</th>
                    <th className="text-center py-2 text-gray-700 dark:text-gray-300 print:text-black">Cant</th>
                    <th className="text-right py-2 text-gray-700 dark:text-gray-300 print:text-black">Precio</th>
                    <th className="text-right py-2 text-gray-700 dark:text-gray-300 print:text-black">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items?.map((item, idx) => {
                    const name = item.productName ?? item.name ?? '-'
                    const price = Number(item.unitPrice ?? item.price ?? 0)
                    const qty = Number(item.quantity ?? 1)
                    const line = item.lineTotal != null ? Number(item.lineTotal) : (price * qty)
                    return (
                      <tr key={(name || '') + idx} className="border-b border-gray-200 dark:border-gray-700 print:border-gray-300">
                        <td className="py-2 text-gray-900 dark:text-gray-100 print:text-black">{name}</td>
                        <td className="py-2 text-center text-gray-900 dark:text-gray-100 print:text-black">{qty}</td>
                        <td className="py-2 text-right text-gray-900 dark:text-gray-100 print:text-black">
                          ${price.toFixed(2)}
                        </td>
                        <td className="py-2 text-right font-medium text-gray-900 dark:text-gray-100 print:text-black">
                          ${line.toFixed(2)}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {order.pointsRedeemed > 0 && (
              <div className="flex justify-between text-sm text-success-600 dark:text-success-400 print:text-black">
                <span>Puntos Canjeados:</span>
                <span className="font-medium">-${order.pointsRedeemed.toFixed(2)}</span>
              </div>
            )}

            <div className="border-t-2 border-gray-300 dark:border-gray-600 pt-4 print:border-black">
              <div className="flex justify-between text-xl font-bold">
                <span className="text-gray-900 dark:text-gray-100 print:text-black">TOTAL:</span>
                <span className="text-brand print:text-black">${Number(order.total || 0).toFixed(2)}</span>
              </div>
            </div>

            <div className="border-t border-dashed border-gray-300 dark:border-gray-600 pt-4 text-center print:border-black">
              <p className="text-sm text-gray-600 dark:text-gray-400 italic print:text-black">
                Te llegará una notificación cuando tu pedido esté listo.
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-500 mt-2 print:text-black">
                ¡Gracias por tu preferencia!
              </p>
            </div>
          </div>

          <div className="mt-6 flex gap-3 print:hidden">
            <button
              onClick={handlePrint}
              className="flex-1 btn-brand"
            >
              <i className="bi bi-printer"></i>
              <span>Imprimir</span>
            </button>
            <button
              onClick={handleDownloadPDF}
              className="flex-1 btn-secondary"
            >
              <i className="bi bi-download"></i>
              <span>Descargar PDF</span>
            </button>
            <button
              onClick={onClose}
              className="btn-outline"
            >
              <i className="bi bi-x-lg"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

Receipt.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    createdAt: PropTypes.string,
    items: PropTypes.arrayOf(PropTypes.shape({
      name: PropTypes.string,
      quantity: PropTypes.number,
      price: PropTypes.number
    })),
    total: PropTypes.number,
    pointsRedeemed: PropTypes.number
  }).isRequired,
  onClose: PropTypes.func.isRequired
}

export default Receipt
