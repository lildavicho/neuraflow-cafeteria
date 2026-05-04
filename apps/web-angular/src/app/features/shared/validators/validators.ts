import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

/**
 * Validadores reutilizables para formularios del ERP.
 * Todos los mensajes se traducen en `ValidationMessages.describe()`.
 */

/** Cédula/RUC ecuatoriano: 10 dígitos (CI) o 13 con terminación 001 (RUC). */
export function ecIdentification(control: AbstractControl): ValidationErrors | null {
  const value = (control.value ?? '').toString().trim();
  if (!value) return null;
  if (!/^\d{10}$/.test(value) && !/^\d{13}$/.test(value)) {
    return { ecIdentification: true };
  }
  if (value.length === 13 && !value.endsWith('001')) {
    return { ecIdentification: true };
  }
  const province = Number(value.substring(0, 2));
  if (province < 1 || province > 24) {
    return { ecIdentification: true };
  }
  return null;
}

/** Teléfono EC: 7-10 dígitos, acepta prefijo +593 y espacios/guiones. */
export function ecPhone(control: AbstractControl): ValidationErrors | null {
  const raw = (control.value ?? '').toString().trim();
  if (!raw) return null;
  const digits = raw.replace(/[^\d+]/g, '');
  if (!/^(\+593)?\d{7,10}$/.test(digits)) {
    return { ecPhone: true };
  }
  return null;
}

/** Mínimo numérico estricto (> min, no >=). */
export function greaterThan(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === undefined || value === '') return null;
    const num = Number(value);
    if (Number.isNaN(num) || num <= min) {
      return { greaterThan: { min, actual: num } };
    }
    return null;
  };
}

/** Cantidad positiva (>= 0). */
export function nonNegativeNumber(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined || value === '') return null;
  const num = Number(value);
  if (Number.isNaN(num) || num < 0) {
    return { nonNegativeNumber: true };
  }
  return null;
}

/** Entero (sin parte decimal). */
export function integerOnly(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined || value === '') return null;
  const num = Number(value);
  if (!Number.isInteger(num)) {
    return { integerOnly: true };
  }
  return null;
}

/** Contraseña fuerte: min 8, al menos 1 letra y 1 número. */
export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const value = (control.value ?? '').toString();
  if (!value) return null;
  if (value.length < 8 || !/[A-Za-zÀ-ÿ]/.test(value) || !/\d/.test(value)) {
    return { strongPassword: true };
  }
  return null;
}

/** Coincidencia entre dos controles (password/confirm). */
export function matchControl(otherControlName: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const parent = control.parent;
    if (!parent) return null;
    const other = parent.get(otherControlName);
    if (!other) return null;
    return other.value === control.value ? null : { mismatch: true };
  };
}

/** Re-export de validadores estándar para usar desde un sólo import. */
export const AppValidators = {
  required: Validators.required,
  email: Validators.email,
  minLength: Validators.minLength,
  maxLength: Validators.maxLength,
  min: Validators.min,
  max: Validators.max,
  pattern: Validators.pattern,
  ecIdentification,
  ecPhone,
  greaterThan,
  nonNegativeNumber,
  integerOnly,
  strongPassword,
  matchControl,
};

/** Utilidad para describir errores en mensajes legibles. */
export class ValidationMessages {
  static describe(errors: ValidationErrors | null | undefined): string | null {
    if (!errors) return null;
    if (errors['required']) return 'Este campo es obligatorio';
    if (errors['email']) return 'Correo electrónico inválido';
    if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres`;
    if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres`;
    if (errors['min']) return `Valor mínimo: ${errors['min'].min}`;
    if (errors['max']) return `Valor máximo: ${errors['max'].max}`;
    if (errors['pattern']) return 'Formato inválido';
    if (errors['ecIdentification']) return 'Cédula o RUC inválido';
    if (errors['ecPhone']) return 'Teléfono inválido';
    if (errors['greaterThan']) return `Debe ser mayor a ${errors['greaterThan'].min}`;
    if (errors['nonNegativeNumber']) return 'No puede ser negativo';
    if (errors['integerOnly']) return 'Debe ser un número entero';
    if (errors['strongPassword']) return 'Mínimo 8 caracteres, con letras y números';
    if (errors['mismatch']) return 'Los valores no coinciden';
    return 'Valor inválido';
  }
}
