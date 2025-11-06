import { Component } from 'react'
import PropTypes from 'prop-types'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, message: '' }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, message: error?.message || 'Error inesperado' }
  }

  componentDidCatch(error, info) {
    console.error('UI crash:', error, info)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex flex-col items-center justify-center gap-4 text-center px-6">
          <h2 className="text-2xl font-semibold text-gray-900 dark:text-gray-100">
            Ocurrió un error en esta vista
          </h2>
          <pre className="bg-gray-100 dark:bg-gray-800 text-sm p-4 rounded-lg text-left whitespace-pre-wrap w-full max-w-2xl text-gray-700 dark:text-gray-300">
            {this.state.message}
          </pre>
          <button type="button" onClick={this.handleReload} className="btn-brand">
            Recargar
          </button>
        </div>
      )
    }

    return this.props.children
  }
}

ErrorBoundary.propTypes = {
  children: PropTypes.node.isRequired,
}
