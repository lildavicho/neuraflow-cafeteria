# Firebase & Algolia Integration Guide

## Overview
This document describes the Firebase Authentication and Algolia Search integration implemented in the UCACUE Bar system.

## Firebase Authentication

### Configuration
Firebase is configured using CDN-based ESM modules (no bundler required).

**Firebase Config** (`frontend/js/firebaseAuth.js`):
```javascript
const firebaseConfig = {
  apiKey: "AIzaSyBLRnzgRJpkibrEwXUi2qzRYNeV-8nT-3w",
  authDomain: "baru-fe8a3.firebaseapp.com",
  projectId: "baru-fe8a3",
  storageBucket: "baru-fe8a3.firebasestorage.app",
  messagingSenderId: "882339697819",
  appId: "1:882339697819:web:0b596cece88ab322ba623e",
  measurementId: "G-TL3EPYC1MC"
};
```

### Features Implemented

#### 1. Email/Password Authentication
- **Registration**: Creates Firebase user, then registers in backend
- **Login**: Authenticates with Firebase, syncs with backend JWT
- **Password Reset**: Uses Firebase's built-in password reset email

#### 2. Google Sign-In
- One-click authentication via Google
- Automatically creates backend user on first login
- Syncs Firebase ID token with backend

#### 3. Auth State Management
- Real-time auth state listener
- Automatic session sync between Firebase and backend
- Proper cleanup on logout

### Architecture

```
┌─────────────┐
│   Frontend  │
│   (Browser) │
└──────┬──────┘
       │
       ├─► Firebase Auth (Email/Password, Google)
       │   └─► Returns: Firebase ID Token
       │
       └─► Backend API (/api/auth/firebase)
           └─► Validates Firebase Token
           └─► Generates JWT
           └─► Returns: JWT + User Data
```

### Files Modified/Created

1. **`frontend/js/firebaseAuth.js`** - Firebase authentication module
2. **`frontend/js/authIntegration.js`** - Bridge between Firebase and backend
3. **`frontend/pages/login.html`** - Updated with Firebase integration
4. **`frontend/pages/forgot.html`** - Updated with Firebase password reset

### Usage Example

```javascript
import * as authIntegration from '/js/authIntegration.js';

// Login with email/password
const result = await authIntegration.login(email, password, rememberMe);

// Login with Google
const result = await authIntegration.loginWithGoogle();

// Password reset
await authIntegration.resetPassword(email);

// Logout
await authIntegration.logout();
```

## Algolia Search Integration

### Configuration

**Backend** (`application.yml`):
```yaml
algolia:
  application-id: EEGT1E6OKE
  api-key: 9c0c8a0e2f8f9d8b7c6a5e4d3c2b1a0f  # Write API Key (backend only)
  search-api-key: 4dea24d6f6c0f12ac5e8d9b4e2f890c0  # Search API Key (frontend)
  index-name: products
```

**Frontend** (`frontend/js/algoliaSearch.js`):
```javascript
const ALGOLIA_CONFIG = {
  applicationId: 'EEGT1E6OKE',
  searchApiKey: '4dea24d6f6c0f12ac5e8d9b4e2f890c0', // Read-only
  indexName: 'products'
};
```

### Security Model

#### Backend (Write Operations)
- Uses **Write API Key** (never exposed to frontend)
- Handles indexing, updating, and deleting products
- Automatic indexing on product CRUD operations

#### Frontend (Read Operations)
- Uses **Search API Key** (read-only, safe to expose)
- Performs searches and autocomplete
- Cannot modify index data

### Features Implemented

#### 1. Product Indexing (Backend)
```java
@Service
public class AlgoliaService {
    // Index product on creation/update
    public void indexProduct(ProductEntity product);
    
    // Delete product from index
    public void deleteProduct(String productId);
    
    // Bulk reindex
    public void reindexAllProducts(List<ProductEntity> products);
}
```

#### 2. Product Search (Frontend)
```javascript
import * as algolia from '/js/algoliaSearch.js';

// Basic search
const results = await algolia.searchProducts('coffee');

// Search with filters
const results = await algolia.searchProductsByCategory('coffee', 'Bebidas');

// Autocomplete suggestions
const suggestions = await algolia.getProductSuggestions('cof', 5);

// Price range search
const results = await algolia.searchProductsByPriceRange('', 1.0, 5.0);
```

### Architecture

```
┌──────────────────────────────────────────────────┐
│                   Algolia Cloud                   │
│                  (products index)                 │
└────────────┬─────────────────────┬────────────────┘
             │                     │
    Write API Key          Search API Key
    (Backend Only)         (Frontend Safe)
             │                     │
    ┌────────▼────────┐   ┌────────▼────────┐
    │  Spring Boot    │   │   Frontend JS   │
    │   Backend       │   │   (Browser)     │
    │                 │   │                 │
    │ - Index         │   │ - Search        │
    │ - Update        │   │ - Autocomplete  │
    │ - Delete        │   │ - Filters       │
    └─────────────────┘   └─────────────────┘
```

### Files Modified/Created

1. **`backend/src/main/java/com/ucacue/bar/service/AlgoliaService.java`** - Algolia backend service
2. **`frontend/js/algoliaSearch.js`** - Algolia frontend module
3. **`backend/src/main/resources/application.yml`** - Algolia configuration

## UI/UX Improvements

### 1. Logo Integration
- Created custom SVG logo (`frontend/assets/logo.svg`)
- Integrated in login and forgot password pages
- Responsive and theme-aware

### 2. Button Locking
- Prevents double-submission
- Visual feedback with spinners
- Disabled state during operations

### 3. Error Handling
- Clear, user-friendly error messages
- Animated error feedback
- Console logging for debugging

### 4. Dark Mode Support
- Comprehensive dark mode styles
- Proper contrast ratios
- Flowbite integration

## Environment Variables

### Backend (.env or docker-compose.yml)
```bash
# Firebase
FIREBASE_PROJECT_ID=baru-fe8a3
FIREBASE_SERVICE_ACCOUNT_PATH=classpath:firebase/service-account.json

# Algolia
ALGOLIA_APP_ID=EEGT1E6OKE
ALGOLIA_API_KEY=9c0c8a0e2f8f9d8b7c6a5e4d3c2b1a0f  # Write API Key
ALGOLIA_SEARCH_API_KEY=4dea24d6f6c0f12ac5e8d9b4e2f890c0  # Search API Key
ALGOLIA_INDEX_NAME=products
```

### Frontend
No environment variables needed - configuration is embedded in modules.

## Testing

### Firebase Auth Testing
1. **Email/Password Login**:
   - Navigate to `/pages/login.html`
   - Enter credentials
   - Verify redirect to dashboard/POS

2. **Google Sign-In**:
   - Click "Google" button
   - Complete Google OAuth flow
   - Verify backend user creation

3. **Password Reset**:
   - Navigate to `/pages/forgot.html`
   - Enter email
   - Check Firebase console for reset email

### Algolia Search Testing
1. **Backend Indexing**:
   - Create/update a product via API
   - Verify indexing in Algolia dashboard

2. **Frontend Search**:
   - Open browser console
   - Test search: `await algolia.searchProducts('test')`
   - Verify results returned

## Security Considerations

### ✅ Secure Practices
- Write API Key only on backend
- Search API Key (read-only) on frontend
- Firebase ID tokens validated on backend
- JWT tokens for session management
- HTTPS required in production

### ⚠️ Important Notes
- Never commit API keys to version control
- Use environment variables in production
- Rotate keys periodically
- Monitor Firebase/Algolia usage

## Troubleshooting

### Firebase Issues
**Problem**: "Firebase not initialized"
- **Solution**: Check browser console for CDN loading errors
- Verify Firebase config in `firebaseAuth.js`

**Problem**: "Invalid credentials"
- **Solution**: Check Firebase console for user status
- Verify email/password in Firebase Authentication

### Algolia Issues
**Problem**: "Search not working"
- **Solution**: Verify Search API Key is correct
- Check Algolia dashboard for index status
- Ensure products are indexed

**Problem**: "Index not found"
- **Solution**: Create index in Algolia dashboard
- Run backend reindex operation

## Next Steps

1. **Firebase Admin SDK**: Implement server-side user management
2. **Algolia Facets**: Add category and price range facets
3. **Analytics**: Integrate Firebase Analytics
4. **Performance**: Implement search result caching
5. **Testing**: Add unit and integration tests

## Support

For issues or questions:
- Firebase: https://firebase.google.com/docs
- Algolia: https://www.algolia.com/doc/
- Project: Contact development team
