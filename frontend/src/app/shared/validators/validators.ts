import { AbstractControl, ValidationErrors } from '@angular/forms';

const PHONE = /^(?:\+212|00212|0)[5-7]\d{8}$/;

/** Moroccan phone number (same rule as the API's @PhoneNumber); empty values are allowed. */
export function phoneValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '').replace(/[\s.\-()]/g, '');
  return value === '' || PHONE.test(value) ? null : { phone: true };
}

/** Same rule as the API's @StrongPassword: 8–72 characters with a letter and a digit. */
export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const v = String(control.value ?? '');
  return v.length >= 8 && v.length <= 72 && /\p{L}/u.test(v) && /\d/.test(v)
    ? null
    : { weak: true };
}
