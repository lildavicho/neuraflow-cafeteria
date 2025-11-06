import PropTypes from 'prop-types'

const ConfirmModal = ({ open, title = 'Confirmar', message, confirmText = 'Confirmar', cancelText = 'Cancelar', onConfirm, onCancel }) => {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-[10000] bg-black/60 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-md">
        <header className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h2>
          <button type="button" onClick={onCancel} className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
            <i className="bi bi-x-lg" />
          </button>
        </header>
        <div className="p-4 text-gray-700 dark:text-gray-300">
          {message}
        </div>
        <footer className="p-4 flex items-center justify-end gap-2 border-t border-gray-200 dark:border-gray-700">
          <button type="button" onClick={onCancel} className="btn-secondary">{cancelText}</button>
          <button type="button" onClick={onConfirm} className="btn-brand">{confirmText}</button>
        </footer>
      </div>
    </div>
  )
}

ConfirmModal.propTypes = {
  open: PropTypes.bool,
  title: PropTypes.string,
  message: PropTypes.node,
  confirmText: PropTypes.string,
  cancelText: PropTypes.string,
  onConfirm: PropTypes.func,
  onCancel: PropTypes.func,
}

export default ConfirmModal
