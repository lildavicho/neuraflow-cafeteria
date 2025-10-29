import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { getEstadisticas, getVentas } from '../services/apiService';
import { formatCurrency } from '../utils/formatters';
import StatCard from '../components/common/StatCard';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, LineElement, PointElement, ArcElement, Title, Tooltip, Legend } from 'chart.js';
import { Bar, Line, Doughnut } from 'react-chartjs-2';
import gsap from 'gsap';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, ArcElement, Title, Tooltip, Legend);

const DashboardPage = () => {
  const [stats, setStats] = useState({
    ventasHoy: 0,
    ordenesHoy: 0,
    totalProductos: 0,
    ticketPromedio: 0,
  });
  const [loading, setLoading] = useState(true);
  const headerRef = useRef(null);
  const actionsRef = useRef(null);

  useEffect(() => {
    loadData();
    
    gsap.fromTo(
      headerRef.current,
      { opacity: 0, y: -20 },
      { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }
    );

    gsap.fromTo(
      actionsRef.current?.children || [],
      { opacity: 0, y: 20 },
      { opacity: 1, y: 0, stagger: 0.1, duration: 0.4, delay: 0.6, ease: 'power2.out' }
    );
  }, []);

  const loadData = async () => {
    try {
      const data = await getEstadisticas();
      setStats(data);
    } catch (error) {
      console.error('Error loading stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const salesData = {
    labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
    datasets: [
      {
        label: 'Ventas',
        data: [120, 190, 300, 250, 200, 350, 280],
        backgroundColor: 'rgba(197, 22, 29, 0.8)',
        borderColor: 'rgba(197, 22, 29, 1)',
        borderWidth: 2,
      },
    ],
  };

  const trendData = {
    labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
    datasets: [
      {
        label: 'Tendencia de Ventas',
        data: [3000, 4200, 3800, 5100, 4900, 6200],
        borderColor: 'rgba(197, 22, 29, 1)',
        backgroundColor: 'rgba(197, 22, 29, 0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  };

  const categoryData = {
    labels: ['Bebidas', 'Snacks', 'Comidas', 'Postres'],
    datasets: [
      {
        data: [35, 25, 30, 10],
        backgroundColor: [
          'rgba(197, 22, 29, 0.8)',
          'rgba(59, 130, 246, 0.8)',
          'rgba(16, 185, 129, 0.8)',
          'rgba(245, 158, 11, 0.8)',
        ],
        borderWidth: 0,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
      },
    },
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-brand border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Cargando datos...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div ref={headerRef}>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Dashboard</h1>
        <p className="text-gray-600">Bienvenido al sistema UCACUE Bar</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Ventas Hoy"
          value={formatCurrency(stats.ventasHoy)}
          change="+12% vs ayer"
          icon="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          color="green"
          index={0}
        />
        <StatCard
          title="Órdenes Hoy"
          value={stats.ordenesHoy}
          change="+8% vs ayer"
          icon="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
          color="blue"
          index={1}
        />
        <StatCard
          title="Productos"
          value={stats.totalProductos}
          change="En inventario"
          icon="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          color="purple"
          index={2}
        />
        <StatCard
          title="Ticket Promedio"
          value={formatCurrency(stats.ticketPromedio)}
          change="Por venta"
          icon="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"
          color="orange"
          index={3}
        />
      </div>

      <div ref={actionsRef} className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Link
          to="/pos"
          className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-all hover:-translate-y-1"
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-brand rounded-lg flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Punto de Venta</h3>
              <p className="text-sm text-gray-600">Realizar ventas</p>
            </div>
          </div>
        </Link>

        <Link
          to="/inventario"
          className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-all hover:-translate-y-1"
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-500 rounded-lg flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Inventario</h3>
              <p className="text-sm text-gray-600">Gestionar productos</p>
            </div>
          </div>
        </Link>

        <Link
          to="/reportes"
          className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-all hover:-translate-y-1"
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-green-500 rounded-lg flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Reportes</h3>
              <p className="text-sm text-gray-600">Ver estadísticas</p>
            </div>
          </div>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-md p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Ventas de la Semana</h3>
          <div className="h-64">
            <Bar data={salesData} options={chartOptions} />
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-md p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Tendencia Mensual</h3>
          <div className="h-64">
            <Line data={trendData} options={chartOptions} />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl shadow-md p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Ventas por Categoría</h3>
          <div className="h-64">
            <Doughnut data={categoryData} options={{ responsive: true, maintainAspectRatio: false }} />
          </div>
        </div>

        <div className="lg:col-span-2 bg-gradient-to-r from-brand to-brand-600 rounded-xl shadow-lg p-8 text-white">
          <h2 className="text-2xl font-bold mb-4">¡Bienvenido al Sistema UCACUE Bar!</h2>
          <p className="text-white/90 mb-6">
            Sistema completo de gestión y punto de venta. Todas las funcionalidades están listas para usar.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex items-start gap-3">
              <svg className="w-6 h-6 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
              <div>
                <h4 className="font-semibold">Autenticación Firebase</h4>
                <p className="text-sm text-white/80">Login, registro y recuperación</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <svg className="w-6 h-6 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
              <div>
                <h4 className="font-semibold">Punto de Venta</h4>
                <p className="text-sm text-white/80">Carrito funcional completo</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <svg className="w-6 h-6 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
              <div>
                <h4 className="font-semibold">Gestión de Inventario</h4>
                <p className="text-sm text-white/80">CRUD completo de productos</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <svg className="w-6 h-6 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
              <div>
                <h4 className="font-semibold">Reportes y Analytics</h4>
                <p className="text-sm text-white/80">Estadísticas en tiempo real</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
