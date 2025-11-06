import { useEffect, useState } from 'react'
import PropTypes from 'prop-types'

const QuantityModal = ({ open, title = 'Editar cantidad', value, max = 1000, onSave, onCancel }) => {
  const [internalValue, setInternalValue] = useState(() => Math.min(Math.max(value ?? 1, 1), max))

  useEffect(() => {
    if (open) {
      setInternalValue(Math.min(Math.max(value ?? 1, 1), max))
    }
  }, [open, value, max])

  const clamp = (raw) => {
    const numeric = Number.isFinite(raw) ? raw : Number(raw)
    if (Number.isNaN(numeric)) return 1
    return Math.min(Math.max(Math.round(numeric), 1), max)
  }

  const handleSave = () => {
    onSave(clamp(internalValue))
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      handleSave()
    }
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-[10000] bg-black/60 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-sm">
        <header className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h2>
          <button type="button" onClick={onCancel} className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
            <i className="bi bi-x-lg" />
          </button>
        </header>
        <div className="p-4 space-y-3">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Cantidad</label>
          <input
            type="number"
            min={1}
            max={max}
            value={internalValue}
            onChange={(event) => {
              const next = event.target.valueAsNumber
              const numeric = Number.isFinite(next) ? next : Number(event.target.value)
              setInternalValue((prev) => (Number.isNaN(numeric) ? prev : numeric))
            }}
            onKeyDown={handleKeyDown}
            className="input-brand w-full"
          />
        </div>
        <footer className="p-4 flex items-center justify-end gap-2 border-t border-gray-200 dark:border-gray-700">
          <button type="button" onClick={onCancel} className="btn-secondary">Cancelar</button>
          <button type="button" onClick={handleSave} className="btn-brand">Guardar</button>
        </footer>
      </div>
    </div>
  )
}

QuantityModal.propTypes = {
  open: PropTypes.bool,
  title: PropTypes.string,
  value: PropTypes.number,
  max: PropTypes.number,
  onSave: PropTypes.func,
  onCancel: PropTypes.func,
}

export default QuantityModal
