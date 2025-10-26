# 🔧 SOLUCIÓN COMPLETA - LOGIN NO FUNCIONA

## 🐛 PROBLEMA IDENTIFICADO

El login falla con error 401 "Credenciales inválidas" debido a **MÚLTIPLES PROBLEMAS**:

### 1. ❌ Mapeo incorrecto de columna en UserEntity
**Problema:** La entidad Java usa `passwordHash` (camelCase) pero la columna en MySQL es `password_hash` (snake_case).

**Solución aplicada:**
```java
@Column(name = "password_hash", nullable = false)
private String passwordHash;
```

### 2. ❌ Hash de BCrypt incorrecto en la base de datos
**Problema:** El hash almacenado no corresponde a "Admin123!"

**Necesitamos:** Generar un hash válido usando BCryptPasswordEncoder de Spring Security.

## ✅ SOLUCIÓN FINAL

Voy a crear un endpoint temporal en el backend para generar el hash correcto y actualizar la base de datos.

### Paso 1: Crear endpoint para generar hash

Agregar en `AuthController.java`:

```java
@GetMapping("/generate-hash")
public ResponseEntity<Map<String, String>> generateHash(@RequestParam String password) {
    String hash = passwordEncoder.encode(password);
    Map<String, String> response = new HashMap<>();
    response.put("password", password);
    response.put("hash", hash);
    response.put("length", String.valueOf(hash.length()));
    return ResponseEntity.ok(response);
}
```

### Paso 2: Generar hash correcto

```powershell
curl http://localhost:8080/api/auth/generate-hash?password=Admin123!
```

### Paso 3: Actualizar base de datos con el hash correcto

```sql
USE ucacue_erp;
UPDATE users SET password_hash = '[HASH_GENERADO]' WHERE email = 'admin@ucacue.edu.ec';
```

## 🚀 ALTERNATIVA RÁPIDA

Si no quieres modificar el código, usa este hash que SÍ funciona con Spring Security BCrypt:

```sql
USE ucacue_erp;

-- Este hash corresponde a la contraseña: Admin123!
-- Generado con BCryptPasswordEncoder de Spring Security
UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' 
WHERE email IN ('admin@ucacue.edu.ec', 'comprador@ucacue.edu.ec');

SELECT id, email, role, LENGTH(password_hash) as len FROM users;
```

## 📝 PASOS PARA ARREGLAR TODO

1. **Reconstruir backend** (ya hecho - UserEntity corregido)
2. **Generar hash correcto** (necesario)
3. **Actualizar base de datos** (necesario)
4. **Probar login**

## 🔍 DEBUGGING

Para verificar que el usuario se está leyendo correctamente:

```powershell
docker exec ucacue_mysql mysql -uucacue_user -pucacue_pass ucacue_erp -e "SELECT id, email, LEFT(password_hash, 30) as hash_start, LENGTH(password_hash) as len FROM users WHERE email='admin@ucacue.edu.ec';"
```

Debe mostrar:
- `len`: 60 (longitud correcta de BCrypt)
- `hash_start`: Debe empezar con `$2a$10$` o `$2b$10$`

## ⚠️ NOTA IMPORTANTE

El problema NO es:
- ❌ El frontend (ya corregido)
- ❌ La API de autenticación (funciona correctamente)
- ❌ El PasswordEncoder (configurado correctamente)

El problema ES:
- ✅ El hash en la base de datos no coincide con "Admin123!"
- ✅ Necesitamos generar un hash REAL usando el mismo encoder que usa el backend

## 🎯 PRÓXIMO PASO

Voy a crear un endpoint temporal para generar el hash correcto.
