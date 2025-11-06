import { useCallback, useEffect, useState } from 'react'
import { updateProfile } from 'firebase/auth'
import { auth } from '../services/firebase'
import { uploadAvatar } from '../services/storageService'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import Button from '../components/common/Button'
import Toast from '../components/common/Toast'
import { fetchMyLoyalty, fetchLoyaltyLedger } from '../services/apiService'
import useRealtimeSync from '../hooks/useRealtimeSync'
import { formatNumber, formatDateTime } from '../utils/formatters'
import { useLanguage } from '../hooks/useLanguage'

const PerfilPage = () => {
  const { user } = useAuth()
  const { toasts, success, error, removeToast } = useToast()
  const { language } = useLanguage()
  const { subscribe } = useRealtimeSync()

  const [displayName, setDisplayName] = useState(user?.displayName || '')
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarPreview, setAvatarPreview] = useState(user?.photoURL || '')
  const [saving, setSaving] = useState(false)
  const [loyalty, setLoyalty] = useState({})
  const [ledger, setLedger] = useState([])

  const loadLoyalty = useCallback(async () => {
    try {
      const balance = await fetchMyLoyalty()
      setLoyalty(balance || {})
      if (balance?.userId) {
        const history = await fetchLoyaltyLedger(balance.userId)
        setLedger(history ?? [])
        await subscribe('/topic/loyalty', (payload) => {
          if (payload?.userId === balance.userId) {
            setLoyalty((prev) => ({ ...prev, points: payload.points, tier: payload.tier }))
          }
        })
      }
    } catch (err) {
      console.error(err)
    }
  }, [subscribe])

  useEffect(() => {
    loadLoyalty()
  }, [loadLoyalty])

  const handleAvatarChange = (evt) => {
    const file = evt.target.files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      error('La imagen debe ser menor a 2MB')
      return
    }
    setAvatarFile(file)
    setAvatarPreview(URL.createObjectURL(file))
  }

  const handleSubmit = async (evt) => {
    evt.preventDefault()
    setSaving(true)
    try {
      let photoURL = user?.photoURL
      if (avatarFile) {
        photoURL = await uploadAvatar(avatarFile)
      }
      await updateProfile(auth.currentUser, { displayName, photoURL })
      success('Perfil actualizado')
    } catch (err) {
      console.error(err)
      error('No se pudo actualizar el perfil')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="p-6 space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <header>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-50">Mi perfil</h1>
        <p className="text-gray-600 dark:text-gray-400">Actualiza tu información y consulta tu programa de lealtad</p>
      </header>

      <section className="card-brand">
        <form className="space-y-6" onSubmit={handleSubmit}>
          <div className="flex flex-col items-center gap-3">
            <div className="w-32 h-32 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
              {avatarPreview ? (
                <img src={avatarPreview} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                <span className="text-4xl font-semibold text-gray-500">
                  {user?.displayName?.[0] || user?.email?.[0] || '?'}
                </span>
              )}
            </div>
            <label className="inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-600 cursor-pointer">
              <span>Cambiar foto</span>
              <input type="file" accept="image/*" className="hidden" onChange={handleAvatarChange} />
            </label>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="displayName" className="text-sm font-medium text-gray-600 dark:text-gray-400">Nombre</label>
              <input
                type="text"
                value={displayName}
                onChange={(evt) => setDisplayName(evt.target.value)}
                id="displayName"
                className="w-full mt-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800"
              />
            </div>
            <div>
              <label htmlFor="emailAddress" className="text-sm font-medium text-gray-600 dark:text-gray-400">Correo</label>
              <input
                type="email"
                value={user?.email || ''}
                disabled
                id="emailAddress"
                className="w-full mt-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-100 text-gray-500"
              />
            </div>
          </div>

          <div className="flex items-center justify-end">
            <Button type="submit" loading={saving}>Guardar</Button>
          </div>
        </form>
      </section>

      <section className="card-brand">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">Lealtad</h2>
        {loyalty ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 rounded-lg bg-brand/10 text-brand">
              <p className="text-sm uppercase tracking-wide">Puntos</p>
              <p className="text-3xl font-semibold">{formatNumber(loyalty.points, language)}</p>
            </div>
            <div className="p-4 rounded-lg bg-success-500/10 text-success-600">
              <p className="text-sm uppercase tracking-wide">Nivel</p>
              <p className="text-2xl font-semibold">{loyalty.tier}</p>
            </div>
            <div className="p-4 rounded-lg bg-gray-100 dark:bg-gray-800">
              <p className="text-sm text-gray-600 dark:text-gray-400">Actualizado</p>
              <p className="text-md text-gray-900 dark:text-gray-100">{formatDateTime(loyalty.updatedAt, language)}</p>
            </div>
          </div>
        ) : (
          <p className="text-gray-500 dark:text-gray-400 text-sm">Cargando saldo...</p>
        )}
      </section>

      <section className="card-brand">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Historial de movimientos</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 dark:bg-gray-900/30">
              <tr>
                <th className="px-4 py-2 text-left text-gray-600 dark:text-gray-300">Fecha</th>
                <th className="px-4 py-2 text-right text-gray-600 dark:text-gray-300">Puntos</th>
                <th className="px-4 py-2 text-left text-gray-600 dark:text-gray-300">Motivo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {ledger.map((entry) => (
                <tr key={entry.id}>
                  <td className="px-4 py-2 text-gray-900 dark:text-gray-100">{formatDateTime(entry.createdAt, language)}</td>
                  <td className={`px-4 py-2 text-right ${entry.delta >= 0 ? 'text-success-600' : 'text-danger-500'}`}>
                    {entry.delta > 0 ? '+' : ''}{entry.delta}
                  </td>
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{entry.reason || 'Movimiento'}</td>
                </tr>
              ))}
              {ledger.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-4 py-6 text-center text-gray-500 dark:text-gray-400">
                    No existen movimientos todavía
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}

export default PerfilPage
