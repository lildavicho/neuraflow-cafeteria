import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { updateProfile } from 'firebase/auth';
import { auth } from '../services/firebase';
import { uploadAvatar } from '../services/storageService';
import { useToast } from '../hooks/useToast';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';

const PerfilPage = () => {
  const { user } = useAuth();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [loading, setLoading] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreview, setAvatarPreview] = useState(user?.photoURL || '');
  
  const { toasts, success, error, removeToast } = useToast();

  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        error('La imagen no debe superar 2MB');
        return;
      }
      setAvatarFile(file);
      setAvatarPreview(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      let photoURL = user?.photoURL;

      if (avatarFile) {
        photoURL = await uploadAvatar(avatarFile);
      }

      await updateProfile(auth.currentUser, {
        displayName,
        photoURL,
      });

      success('Perfil actualizado exitosamente');
    } catch (err) {
      error('Error al actualizar el perfil');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Mi Perfil</h1>
        <p className="text-gray-600">Gestiona tu información personal</p>
      </div>

      <div className="bg-white rounded-xl shadow-md p-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="flex flex-col items-center mb-8">
            <div className="relative">
              <div className="w-32 h-32 rounded-full overflow-hidden bg-gray-200 flex items-center justify-center">
                {avatarPreview ? (
                  <img src={avatarPreview} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                  <span className="text-4xl font-bold text-gray-400">
                    {user?.displayName?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'}
                  </span>
                )}
              </div>
              <label className="absolute bottom-0 right-0 w-10 h-10 bg-brand rounded-full flex items-center justify-center cursor-pointer hover:bg-brand-600 transition-colors">
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="hidden"
                />
              </label>
            </div>
            <p className="text-sm text-gray-500 mt-4">Haz clic en el ícono para cambiar tu foto</p>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Nombre Completo</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Correo Electrónico</label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg bg-gray-50 text-gray-500"
            />
            <p className="text-xs text-gray-500 mt-1">El correo no puede ser modificado</p>
          </div>

          <div className="pt-4">
            <Button type="submit" loading={loading} className="w-full md:w-auto">
              Guardar Cambios
            </Button>
          </div>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-md p-8">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Información de la Cuenta</h2>
        <div className="space-y-3">
          <div className="flex justify-between py-3 border-b border-gray-200">
            <span className="text-gray-600">Usuario ID</span>
            <span className="font-mono text-sm text-gray-900">{user?.uid}</span>
          </div>
          <div className="flex justify-between py-3 border-b border-gray-200">
            <span className="text-gray-600">Correo verificado</span>
            <span className={user?.emailVerified ? 'text-green-600' : 'text-orange-600'}>
              {user?.emailVerified ? 'Sí' : 'No'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PerfilPage;
