import { useState, useEffect } from 'react';
import { getVentas, getEstadisticas } from '../services/apiService';
import { formatCurrency, formatDateTime } from '../utils/formatters';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, LineElement, PointElement, Title, Tooltip, Legend } from 'chart.js';
import { Bar, Line } from 'react-chartjs-2';
import * as XLSX from 'xlsx';
import Button from '../components/common/Button';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Title, Tooltip, Legend);

const ReportesPage = () => {
  const [ventas, setVentas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [stats, setStats] = useState(null);

  useEffect(() => {
    const today = new Date();
    const lastWeek = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
    setDateFrom(lastWeek.toISOString().split('T')[0]);
    setDateTo(today.toISOString().split('T')[0]);
  }, []);

  useEffect(() => {
    if (dateFrom && dateTo) {
      loadData();
    }
  }, [dateFrom, dateTo]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ventasData, statsData] = await Promise.all([
        getVentas({ from: dateFrom, to: dateTo }),
        getEstadisticas({ from: dateFrom, to: dateTo }),
      ]);
      setVentas(Array.isArray(ventasData) ? ventasData : ventasData.content || []);
      setStats(statsData);
    } catch (error) {
      console.error('Error loading data:', error);
      setVentas([]);
    } finally {
      setLoading(false);
    }
  };

  const exportToExcel = () => {
    const data = ventas.map(venta => ({
      Fecha: formatDateTime(venta.fecha),
      Total: venta.total,
      Items: venta.items?.length || 0,
    }));

    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Ventas');
    XLSX.writeFile(wb, `ventas_${dateFrom}_${dateTo}.xlsx`);
  };

  const chartData = {
    labels: ventas.slice(0, 10).map(v => new Date(v.fecha).toLocaleDateString()),
    datasets: [
      {
        label: 'Ventas',
        data: ventas.slice(0, 10).map(v => v.total),
        backgroundColor: 'rgba(197, 22, 29, 0.8)',
        borderColor: 'rgba(197, 22, 29, 1)',
        borderWidth: 2,
      },
    ],
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Reportes</h1>
          <p className="text-gray-600">Análisis de ventas y estadísticas</p>
        </div>
        <Button onClick={exportToExcel} disabled={ventas.length === 0}>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Exportar a Excel
        </Button>
      </div>

      <div className="bg-white rounded-xl shadow-md p-6">
        <h3 className="text-lg font-bold text-gray-900 mb-4">Filtros</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Desde</label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Hasta</label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>
          <div className="flex items-end">
            <Button onClick={loadData} className="w-full">Aplicar Filtros</Button>
          </div>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="bg-white rounded-xl shadow-md p-6">
            <p className="text-sm font-medium text-gray-600 mb-1">Total Ventas</p>
            <p className="text-3xl font-bold text-gray-900">{formatCurrency(stats.ventasHoy || 0)}</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <p className="text-sm font-medium text-gray-600 mb-1">Órdenes</p>
            <p className="text-3xl font-bold text-gray-900">{stats.ordenesHoy || 0}</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <p className="text-sm font-medium text-gray-600 mb-1">Ticket Promedio</p>
            <p className="text-3xl font-bold text-gray-900">{formatCurrency(stats.ticketPromedio || 0)}</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <p className="text-sm font-medium text-gray-600 mb-1">Productos</p>
            <p className="text-3xl font-bold text-gray-900">{stats.totalProductos || 0}</p>
          </div>
        </div>
      )}

      <div className="bg-white rounded-xl shadow-md p-6">
        <h3 className="text-lg font-bold text-gray-900 mb-4">Gráfico de Ventas</h3>
        <div className="h-80">
          <Bar data={chartData} options={{ responsive: true, maintainAspectRatio: false }} />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <div className="p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Historial de Ventas</h3>
        </div>
        
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="text-center">
              <div className="w-16 h-16 border-4 border-brand border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
              <p className="text-gray-600">Cargando ventas...</p>
            </div>
          </div>
        ) : ventas.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500">No hay ventas en el período seleccionado</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Fecha</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Items</th>
                  <th className="px-6 py-3 text-right text-xs font-semibold text-gray-600 uppercase">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {ventas.map((venta, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm text-gray-900">{formatDateTime(venta.fecha)}</td>
                    <td className="px-6 py-4 text-sm text-gray-600">{venta.items?.length || 0} productos</td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900 text-right">{formatCurrency(venta.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReportesPage;
