package com.ucacue.bar.util;

import org.springframework.stereotype.Component;

@Component
public class EcuadorIdValidator {
    
    public boolean isValid(String cedula) {
        if (cedula == null || cedula.length() != 10) {
            return false;
        }
        
        try {
            // Validar que todos los caracteres sean dígitos
            for (char c : cedula.toCharArray()) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            
            // Extraer provincia (primeros 2 dígitos)
            int provincia = Integer.parseInt(cedula.substring(0, 2));
            if (provincia < 1 || provincia > 24) {
                return false;
            }
            
            // El tercer dígito debe ser menor a 6 para cédulas
            int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
            if (tercerDigito >= 6) {
                return false;
            }
            
            // Algoritmo de validación módulo 10
            int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
            int suma = 0;
            
            for (int i = 0; i < 9; i++) {
                int digito = Integer.parseInt(cedula.substring(i, i + 1));
                int producto = digito * coeficientes[i];
                
                if (producto >= 10) {
                    producto -= 9;
                }
                
                suma += producto;
            }
            
            // Calcular el dígito verificador
            int modulo = suma % 10;
            int digitoVerificadorCalculado = modulo == 0 ? 0 : 10 - modulo;
            int digitoVerificadorReal = Integer.parseInt(cedula.substring(9, 10));
            
            return digitoVerificadorCalculado == digitoVerificadorReal;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
