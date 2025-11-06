import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  fetchSalesAnalytics,
  fetchPeopleAnalytics,
  fetchSalesVsPeople,
  exportAnalytics,
} from '../services/apiService'
import { formatCurrency, formatDate } from '../utils/formatters'
import Button from '../components/common/Button'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js'
import { Line } from 'react-chartjs-2'
import { useToast } from '../hooks/useToast'
import Toast from '../components/common/Toast'
import { useLanguage } from '../hooks/useLanguage'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const initialRange = () => {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - 7)
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  }
}

const ReportesPage = () => {
  const { language } = useLanguage()
  const { toasts, error, success, removeToast } = useToast()
  const [range, setRange] = useState(initialRange)
  const [loading, setLoading] = useState(true)
  const [salesData, setSalesData] = useState([])
  const [peopleData, setPeopleData] = useState([])
  const [correlatedData, setCorrelatedData] = useState([])

  const loadAnalytics = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        from: range.from ? `${range.from}T00:00:00` : undefined,
        to: range.to ? `${range.to}T23:59:59` : undefined,
      }
      const [sales, people, combined] = await Promise.all([
        fetchSalesAnalytics(params),
        fetchPeopleAnalytics(params),
        fetchSalesVsPeople(params),
      ])
      setSalesData(sales?.series ?? [])
      setPeopleData(people?.series ?? [])
      setCorrelatedData(combined?.series ?? [])
    } catch (err) {
      console.error(err)
      error('No se pudieron cargar los reportes')
    } finally {
      setLoading(false)
    }
  }, [error, range])

  useEffect(() => {
    loadAnalytics()
  }, [loadAnalytics])

  const salesChart = useMemo(() => {
    const labels = salesData.map((item) => formatDate(item.date || item.timestamp, language))
    return {
      labels,
      datasets: [
        {
          label: 'Ventas',
          data: salesData.map((item) => Number(item.total ?? 0)),
          borderColor: '#c5161d',
          backgroundColor: 'rgba(197, 22, 29, 0.15)',
          tension: 0.35,
        },
      ],
    }
  }, [salesData, language])

  const peopleChart = useMemo(() => {
    const labels = peopleData.map((item) => formatDate(item.timestamp || item.hourStart, language))
    return {
      labels,
      datasets: [
        {
          label: 'Afluencia',
          data: peopleData.map((item) => Number(item.count ?? item.avg ?? 0)),
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.12)',
          tension: 0.35,
        },
      ],
    }
  }, [peopleData, language])

  const downloadReport = async (format) => {
    try {
      const params = {
        from: range.from ? `${range.from}T00:00:00` : undefined,
        to: range.to ? `${range.to}T23:59:59` : undefined,
      }
      const blob = await exportAnalytics(format, params)
      const url = window.URL.createObjectURL(new Blob([blob]))
      const link = document.createElement('a')
      link.href = url
      link.download = `analytics_${range.from}_${range.to}.${format === 'pdf' ? 'pdf' : 'xlsx'}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      success(`Reporte ${format.toUpperCase()} generado`)
    } catch (err) {
      console.error(err)
      error('No se pudo exportar el reporte')
    }
  }

  return (
    <div className="p-6 space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-50">Reportes</h1>
          <p className="text-gray-600 dark:text-gray-400">Analítica de ventas y afluencia</p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" onClick={() => downloadReport('excel')}>
            Exportar Excel
          </Button>
          <Button onClick={() => downloadReport('pdf')}>Exportar PDF</Button>
        </div>
      </header>

      <section className="card-brand">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="text-sm font-medium text-gray-600 dark:text-gray-400">Desde</label>
            <input
              type="date"
              value={range.from}
              onChange={(evt) => setRange((prev) => ({ ...prev, from: evt.target.value }))}
              className="w-full mt-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600 dark:text-gray-400">Hasta</label>
            <input
              type="date"
              value={range.to}
              onChange={(evt) => setRange((prev) => ({ ...prev, to: evt.target.value }))}
              className="w-full mt-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800"
            />
          </div>
          <div className="md:col-span-2 flex items-end">
            <Button onClick={loadAnalytics} className="w-full md:w-auto">Actualizar</Button>
          </div>
        </div>
      </section>

      {loading ? (
        <div className="flex items-center justify-center h-56">
          <div className="w-14 h-14 border-4 border-brand border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card-brand h-80">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">Ventas</h2>
            <Line data={salesChart} options={{ responsive: true, maintainAspectRatio: false }} />
          </div>
          <div className="card-brand h-80">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">Afluencia</h2>
            <Line data={peopleChart} options={{ responsive: true, maintainAspectRatio: false }} />
          </div>
        </section>
      )}

      <section className="card-brand">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Relación ventas vs afluencia</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 dark:bg-gray-900/30">
              <tr>
                <th className="px-4 py-2 text-left text-gray-600 dark:text-gray-300">Fecha</th>
                <th className="px-4 py-2 text-right text-gray-600 dark:text-gray-300">Ventas</th>
                <th className="px-4 py-2 text-right text-gray-600 dark:text-gray-300">Personas</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {correlatedData.map((point, index) => (
                <tr key={index}>
                  <td className="px-4 py-2 text-gray-900 dark:text-gray-100">{formatDate(point.date, language)}</td>
                  <td className="px-4 py-2 text-right text-gray-900 dark:text-gray-100">{formatCurrency(point.sales, language)}</td>
                  <td className="px-4 py-2 text-right text-gray-900 dark:text-gray-100">{point.people}</td>
                </tr>
              ))}
              {correlatedData.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-4 py-6 text-center text-gray-500 dark:text-gray-400">
                    No hay datos en el rango seleccionado
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

export default ReportesPage
