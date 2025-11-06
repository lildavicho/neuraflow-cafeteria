import { Navigate } from 'react-router-dom'
import PropTypes from 'prop-types'
import { useAuth } from '../../hooks/useAuth'
import Loader from '../common/Loader'

const AdminRoute = ({ children }) => {
  const { user, loading } = useAuth()

  if (loading) {
    return <Loader />
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/pos" replace />
  }

  return children
}

AdminRoute.propTypes = {
  children: PropTypes.node.isRequired
}

export default AdminRoute
