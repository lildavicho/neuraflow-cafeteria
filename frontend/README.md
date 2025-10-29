# UCACUE Bar - Frontend SPA

Sistema de gestión y punto de venta construido con React + Vite + Tailwind CSS v3.

## 🚀 Inicio Rápido

### Prerequisitos
- Node.js 18+ y npm

### Instalación

```bash
# Instalar dependencias
npm install

# Desarrollo (puerto 5173)
npm run dev

# Build para producción
npm run build

# Preview del build
npm run preview
```

## 📁 Estructura del Proyecto

```
ucacue-bar-frontend/
├── src/
│   ├── assets/          # Imágenes y recursos estáticos
│   ├── components/      # Componentes reutilizables
│   ├── contexts/        # React Context (Auth, etc.)
│   ├── hooks/           # Custom hooks
│   ├── pages/           # Páginas de la aplicación
│   ├── services/        # API y Firebase
│   ├── utils/           # Utilidades
│   ├── App.jsx          # Router principal
│   ├── main.jsx         # Entry point
│   └── index.css        # Estilos globales + Tailwind
├── index.html           # HTML base (SPA)
├── vite.config.js       # Configuración Vite
├── tailwind.config.cjs  # Tema UCACUE
└── package.json
```

## 🎨 Sistema de Diseño

### Paleta UCACUE
- **Brand (Rojo)**: `#C5161D` - Color principal de la marca
- **Accent (Azul)**: `#1a73e8` - Acentos y enlaces
- **Success (Verde)**: `#10b981` - Acciones exitosas
- **Warning (Naranja)**: `#f59e0b` - Advertencias
- **Danger (Rojo)**: `#ef4444` - Errores y acciones destructivas

### Clases Utilitarias Personalizadas
- `.btn-brand` - Botón primario rojo
- `.btn-outline` - Botón con borde
- `.btn-secondary` - Botón secundario
- `.input-brand` - Input con focus brand
- `.card` - Tarjeta básica
- `.kpi` - Tarjeta de KPI
- `.chip` - Badge/chip

## 🔐 Rutas de Autenticación

- `/login` - Inicio de sesión
- `/register` - Registro de usuario
- `/forgot-password` - Recuperación de contraseña

## 🏠 Rutas Privadas (requieren autenticación)

- `/dashboard` - Panel principal
- `/inventario` - Gestión de productos
- `/pos` - Punto de venta
- `/reportes` - Reportes y estadísticas
- `/perfil` - Perfil de usuario
- `/notificaciones` - Centro de notificaciones
- `/ajustes` - Configuración del sistema

## 🐳 Despliegue con Docker

El frontend se sirve desde Nginx en Docker. Asegúrate de que:

1. **docker-compose.yml** monta el build:
   ```yaml
   volumes:
     - ../ucacue-bar-frontend/dist:/usr/share/nginx/html:ro
   ```

2. **nginx.conf** tiene la regla SPA:
   ```nginx
   location / {
     try_files $uri $uri/ /index.html;
   }
   ```

### Build y Deploy

```bash
# Opción 1: Script PowerShell (Windows)
.\build-and-deploy.ps1

# Opción 2: Script Bash (Linux/Mac)
chmod +x build-and-deploy.sh
./build-and-deploy.sh

# Opción 3: Manual
cd ucacue-bar-frontend
npm ci
npm run build
cd ../ops
docker-compose down
docker-compose up -d
```

## ✅ Verificación

Después del deploy, verifica:

1. **SPA funciona**: Navega a http://localhost:5173/login (debe cargar React, no HTML plano)
2. **Refresh funciona**: Recarga la página en cualquier ruta (no debe dar 404)
3. **Sin warnings**: `npm run build` debe completar sin warnings
4. **Logo visible**: El logo UCACUE debe aparecer en todas las páginas de auth

## 🔧 Configuración de Firebase

Crea un archivo `.env` en la raíz con:

```env
VITE_FIREBASE_API_KEY=tu_api_key
VITE_FIREBASE_AUTH_DOMAIN=tu_auth_domain
VITE_FIREBASE_PROJECT_ID=tu_project_id
VITE_FIREBASE_STORAGE_BUCKET=tu_storage_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=tu_sender_id
VITE_FIREBASE_APP_ID=tu_app_id
VITE_FIREBASE_VAPID_KEY=tu_vapid_key
VITE_API_BASE_URL=http://localhost:8080/api
```

## 📝 Notas Técnicas

- **React 19**: Usa las últimas características de React
- **Tailwind v3**: Sistema de diseño con tokens personalizados
- **GSAP**: Animaciones fluidas y profesionales
- **Firebase**: Auth, Firestore, Storage y FCM
- **Chart.js**: Gráficos y visualizaciones
- **React Router v6**: Navegación SPA

## 🐛 Solución de Problemas

### "vite" no se reconoce
```bash
npm install
```

### Warnings de @apply en el editor
Estos son del linter CSS del editor y no afectan el build. Para silenciarlos:
- Instala la extensión "Tailwind CSS IntelliSense"
- O añade en settings.json: `"css.lint.unknownAtRules": "ignore"`

### Logo no se ve
Verifica que `src/assets/logo.svg` existe y se importa correctamente:
```jsx
import logo from '../assets/logo.svg'
```

## 📦 Dependencias Principales

- **react** ^19.1.1
- **react-router-dom** ^6.25.1
- **firebase** ^12.4.0
- **tailwindcss** ^3.4.18
- **vite** ^7.1.7
- **gsap** ^3.13.0
- **chart.js** ^4.5.1

---

**Desarrollado para UCACUE** 🎓
