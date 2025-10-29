import { useState, useEffect, useRef } from 'react';
import { getProductos, createProducto, updateProducto, deleteProducto } from '../services/apiService';
import { uploadImage } from '../services/storageService';
import { formatCurrency } from '../utils/formatters';
import { useToast } from '../hooks/useToast';
import Modal from '../components/common/Modal';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';
import gsap from 'gsap';

const InventarioPage = () => {
  const [productos, setProductos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [formData, setFormData] = useState({
    nombre: '',
    descripcion: '',
    precio: '',
    stock: '',
    categoria: '',
    imagenUrl: '',
  });
  const [imageFile, setImageFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  
  const { toasts, success, error, removeToast } = useToast();
  const gridRef = useRef(null);

  useEffect(() => {
    loadProductos();
  }, [search]);

  useEffect(() => {
    if (gridRef.current?.children) {
      gsap.fromTo(
        Array.from(gridRef.current.children),
        { opacity: 0, y: 20 },
        { opacity: 1, y: 0, stagger: 0.05, duration: 0.3, ease: 'power2.out' }
      );
    }
  }, [productos]);

  const loadProductos = async () => {
    try {
      setLoading(true);
      const data = await getProductos({ search });
      setProductos(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      error('Error al cargar productos');
      setProductos([]);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (product = null) => {
    if (product) {
      setEditingProduct(product);
      setFormData(product);
    } else {
      setEditingProduct(null);
      setFormData({
        nombre: '',
        descripcion: '',
        precio: '',
        stock: '',
        categoria: '',
        imagenUrl: '',
      });
    }
    setImageFile(null);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingProduct(null);
    setImageFile(null);
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        error('La imagen no debe superar 5MB');
        return;
      }
      setImageFile(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setUploading(true);

    try {
      let imagenUrl = formData.imagenUrl;

      if (imageFile) {
        imagenUrl = await uploadImage(imageFile);
      }

      const payload = {
        ...formData,
        precio: parseFloat(formData.precio),
        stock: parseInt(formData.stock),
        imagenUrl,
      };

      if (editingProduct) {
        await updateProducto(editingProduct.id, payload);
        success('Producto actualizado exitosamente');
      } else {
        await createProducto(payload);
        success('Producto creado exitosamente');
      }

      handleCloseModal();
      loadProductos();
    } catch (err) {
      error('Error al guardar el producto');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('¿Estás seguro de eliminar este producto?')) return;

    try {
      await deleteProducto(id);
      success('Producto eliminado exitosamente');
      loadProductos();
    } catch (err) {
      error('Error al eliminar el producto');
    }
  };

  return (
    <div className="p-6 space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Inventario</h1>
          <p className="text-gray-600">Gestión completa de productos</p>
        </div>
        <Button onClick={() => handleOpenModal()}>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          Nuevo Producto
        </Button>
      </div>

      <div className="bg-white rounded-xl shadow-md p-4">
        <div className="relative">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar productos..."
            className="w-full pl-10 pr-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand focus:border-transparent"
          />
          <svg className="w-5 h-5 text-gray-400 absolute left-3 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div className="w-16 h-16 border-4 border-brand border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-gray-600">Cargando productos...</p>
          </div>
        </div>
      ) : productos.length === 0 ? (
        <div className="bg-white rounded-xl shadow-md p-12 text-center">
          <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          <h3 className="text-xl font-bold text-gray-900 mb-2">No hay productos</h3>
          <p className="text-gray-600 mb-6">Comienza agregando tu primer producto</p>
          <Button onClick={() => handleOpenModal()}>Agregar Producto</Button>
        </div>
      ) : (
        <div ref={gridRef} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {productos.map((producto) => (
            <div key={producto.id} className="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow">
              <div className="h-48 bg-gray-200 relative">
                {producto.imagenUrl ? (
                  <img src={producto.imagenUrl} alt={producto.nombre} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <svg className="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                )}
                <div className="absolute top-2 right-2 bg-brand text-white px-2 py-1 rounded-lg text-xs font-semibold">
                  Stock: {producto.stock}
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-bold text-gray-900 mb-1 truncate">{producto.nombre}</h3>
                <p className="text-sm text-gray-600 mb-2 line-clamp-2">{producto.descripcion}</p>
                <p className="text-xs text-gray-500 mb-3">{producto.categoria}</p>
                <p className="text-2xl font-bold text-brand mb-4">{formatCurrency(producto.precio)}</p>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleOpenModal(producto)}
                    className="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600 transition-colors text-sm font-semibold"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(producto.id)}
                    className="flex-1 bg-red-500 text-white py-2 rounded-lg hover:bg-red-600 transition-colors text-sm font-semibold"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal isOpen={showModal} onClose={handleCloseModal} title={editingProduct ? 'Editar Producto' : 'Nuevo Producto'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Nombre</label>
            <input
              type="text"
              value={formData.nombre}
              onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
              required
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Descripción</label>
            <textarea
              value={formData.descripcion}
              onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
              rows="3"
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Precio</label>
              <input
                type="number"
                step="0.01"
                value={formData.precio}
                onChange={(e) => setFormData({ ...formData, precio: e.target.value })}
                required
                className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Stock</label>
              <input
                type="number"
                value={formData.stock}
                onChange={(e) => setFormData({ ...formData, stock: e.target.value })}
                required
                className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Categoría</label>
            <select
              value={formData.categoria}
              onChange={(e) => setFormData({ ...formData, categoria: e.target.value })}
              required
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            >
              <option value="">Seleccionar...</option>
              <option value="Bebidas">Bebidas</option>
              <option value="Snacks">Snacks</option>
              <option value="Comidas">Comidas</option>
              <option value="Postres">Postres</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Imagen</label>
            <input
              type="file"
              accept="image/*"
              onChange={handleImageChange}
              className="w-full px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
            {imageFile && <p className="text-sm text-gray-600 mt-2">Archivo seleccionado: {imageFile.name}</p>}
          </div>

          <div className="flex gap-3 pt-4">
            <Button type="button" variant="secondary" onClick={handleCloseModal} className="flex-1">
              Cancelar
            </Button>
            <Button type="submit" loading={uploading} className="flex-1">
              {editingProduct ? 'Actualizar' : 'Crear'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default InventarioPage;
