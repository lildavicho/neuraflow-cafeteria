// Validation utilities for UCACUE Bar System

/**
 * Validates Ecuador ID (Cédula)
 * @param {string} cedula - 10 digit Ecuador ID
 * @returns {boolean} - true if valid
 */
export function validarCedulaEC(cedula) {
    if (!cedula || cedula.length !== 10) {
        return false;
    }

    // Check if all characters are digits
    if (!/^\d+$/.test(cedula)) {
        return false;
    }

    // Extract province (first 2 digits)
    const provincia = parseInt(cedula.substring(0, 2));
    if (provincia < 1 || provincia > 24) {
        return false;
    }

    // Third digit must be less than 6 for natural persons
    const tercerDigito = parseInt(cedula.substring(2, 3));
    if (tercerDigito >= 6) {
        return false;
    }

    // Validation algorithm (modulo 10)
    const coeficientes = [2, 1, 2, 1, 2, 1, 2, 1, 2];
    let suma = 0;

    for (let i = 0; i < 9; i++) {
        let digito = parseInt(cedula.charAt(i));
        let producto = digito * coeficientes[i];
        
        if (producto >= 10) {
            producto -= 9;
        }
        
        suma += producto;
    }

    const modulo = suma % 10;
    const digitoVerificadorCalculado = modulo === 0 ? 0 : 10 - modulo;
    const digitoVerificadorReal = parseInt(cedula.charAt(9));

    return digitoVerificadorCalculado === digitoVerificadorReal;
}

/**
 * Validates strong password
 * @param {string} password 
 * @returns {object} - { valid: boolean, errors: string[] }
 */
export function validatePassword(password) {
    const errors = [];
    
    if (!password || password.length < 8) {
        errors.push('La contraseña debe tener al menos 8 caracteres');
    }
    
    if (!/[A-Z]/.test(password)) {
        errors.push('Debe contener al menos una letra mayúscula');
    }
    
    if (!/[a-z]/.test(password)) {
        errors.push('Debe contener al menos una letra minúscula');
    }
    
    if (!/[0-9]/.test(password)) {
        errors.push('Debe contener al menos un número');
    }
    
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
        errors.push('Debe contener al menos un carácter especial');
    }
    
    return {
        valid: errors.length === 0,
        errors
    };
}

/**
 * Validates email format
 * @param {string} email 
 * @returns {boolean}
 */
export function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validates phone number (Ecuador format)
 * @param {string} phone 
 * @returns {boolean}
 */
export function validatePhone(phone) {
    // Ecuador phone: 09XXXXXXXX or 02XXXXXXX
    const phoneRegex = /^(09\d{8}|0[2-7]\d{7})$/;
    return phoneRegex.test(phone.replace(/[\s-]/g, ''));
}

/**
 * Validates RUC (Ecuador tax ID)
 * @param {string} ruc 
 * @returns {boolean}
 */
export function validateRUC(ruc) {
    if (!ruc || ruc.length !== 13) {
        return false;
    }
    
    // RUC must end with 001
    if (ruc.substring(10, 13) !== '001') {
        return false;
    }
    
    // First 10 digits are validated as cedula
    return validarCedulaEC(ruc.substring(0, 10));
}

/**
 * Validates product code format
 * @param {string} code 
 * @returns {boolean}
 */
export function validateProductCode(code) {
    // Format: XXX000 (3 letters + 3 numbers)
    const codeRegex = /^[A-Z]{3}\d{3}$/;
    return codeRegex.test(code);
}

/**
 * Validates monetary amount
 * @param {number|string} amount 
 * @returns {boolean}
 */
export function validateAmount(amount) {
    const numAmount = parseFloat(amount);
    return !isNaN(numAmount) && numAmount > 0 && numAmount < 1000000;
}

/**
 * Validates date range
 * @param {Date|string} startDate 
 * @param {Date|string} endDate 
 * @returns {boolean}
 */
export function validateDateRange(startDate, endDate) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    return start <= end && end <= new Date();
}

/**
 * Validates form data
 * @param {object} formData 
 * @param {object} rules 
 * @returns {object} - { valid: boolean, errors: object }
 */
export function validateForm(formData, rules) {
    const errors = {};
    
    for (const field in rules) {
        const value = formData[field];
        const fieldRules = rules[field];
        const fieldErrors = [];
        
        // Required validation
        if (fieldRules.required && !value) {
            fieldErrors.push(`${fieldRules.label || field} es requerido`);
        }
        
        // Min length validation
        if (fieldRules.minLength && value && value.length < fieldRules.minLength) {
            fieldErrors.push(`${fieldRules.label || field} debe tener al menos ${fieldRules.minLength} caracteres`);
        }
        
        // Max length validation
        if (fieldRules.maxLength && value && value.length > fieldRules.maxLength) {
            fieldErrors.push(`${fieldRules.label || field} debe tener máximo ${fieldRules.maxLength} caracteres`);
        }
        
        // Pattern validation
        if (fieldRules.pattern && value && !fieldRules.pattern.test(value)) {
            fieldErrors.push(`${fieldRules.label || field} tiene formato inválido`);
        }
        
        // Custom validation
        if (fieldRules.custom && value) {
            const customResult = fieldRules.custom(value, formData);
            if (customResult !== true) {
                fieldErrors.push(customResult);
            }
        }
        
        if (fieldErrors.length > 0) {
            errors[field] = fieldErrors;
        }
    }
    
    return {
        valid: Object.keys(errors).length === 0,
        errors
    };
}

/**
 * Sanitizes HTML input to prevent XSS
 * @param {string} input 
 * @returns {string}
 */
export function sanitizeHTML(input) {
    const div = document.createElement('div');
    div.textContent = input;
    return div.innerHTML;
}

/**
 * Formats and validates credit card number
 * @param {string} cardNumber 
 * @returns {object}
 */
export function validateCreditCard(cardNumber) {
    // Remove spaces and dashes
    const cleaned = cardNumber.replace(/[\s-]/g, '');
    
    if (!/^\d{13,19}$/.test(cleaned)) {
        return { valid: false, type: null };
    }
    
    // Luhn algorithm
    let sum = 0;
    let isEven = false;
    
    for (let i = cleaned.length - 1; i >= 0; i--) {
        let digit = parseInt(cleaned[i]);
        
        if (isEven) {
            digit *= 2;
            if (digit > 9) {
                digit -= 9;
            }
        }
        
        sum += digit;
        isEven = !isEven;
    }
    
    const valid = sum % 10 === 0;
    
    // Detect card type
    let type = null;
    if (/^4/.test(cleaned)) {
        type = 'visa';
    } else if (/^5[1-5]/.test(cleaned)) {
        type = 'mastercard';
    } else if (/^3[47]/.test(cleaned)) {
        type = 'amex';
    } else if (/^6011|65/.test(cleaned)) {
        type = 'discover';
    }
    
    return { valid, type };
}

/**
 * Validates inventory quantity
 * @param {number} quantity 
 * @param {number} currentStock 
 * @param {string} operation 
 * @returns {object}
 */
export function validateInventoryQuantity(quantity, currentStock, operation) {
    if (quantity <= 0) {
        return { valid: false, error: 'La cantidad debe ser mayor a 0' };
    }
    
    if (operation === 'OUT' && quantity > currentStock) {
        return { valid: false, error: 'Stock insuficiente' };
    }
    
    if (quantity > 10000) {
        return { valid: false, error: 'Cantidad excede el límite máximo' };
    }
    
    return { valid: true };
}

/**
 * Validates file upload
 * @param {File} file 
 * @param {object} options 
 * @returns {object}
 */
export function validateFileUpload(file, options = {}) {
    const {
        maxSize = 5 * 1024 * 1024, // 5MB default
        allowedTypes = ['image/jpeg', 'image/png', 'image/gif'],
        allowedExtensions = ['jpg', 'jpeg', 'png', 'gif']
    } = options;
    
    const errors = [];
    
    if (!file) {
        errors.push('No se ha seleccionado ningún archivo');
        return { valid: false, errors };
    }
    
    if (file.size > maxSize) {
        errors.push(`El archivo excede el tamaño máximo de ${maxSize / 1024 / 1024}MB`);
    }
    
    if (allowedTypes.length > 0 && !allowedTypes.includes(file.type)) {
        errors.push('Tipo de archivo no permitido');
    }
    
    const extension = file.name.split('.').pop().toLowerCase();
    if (allowedExtensions.length > 0 && !allowedExtensions.includes(extension)) {
        errors.push('Extensión de archivo no permitida');
    }
    
    return {
        valid: errors.length === 0,
        errors
    };
}

// Export all validators
export default {
    validarCedulaEC,
    validatePassword,
    validateEmail,
    validatePhone,
    validateRUC,
    validateProductCode,
    validateAmount,
    validateDateRange,
    validateForm,
    sanitizeHTML,
    validateCreditCard,
    validateInventoryQuantity,
    validateFileUpload
};
