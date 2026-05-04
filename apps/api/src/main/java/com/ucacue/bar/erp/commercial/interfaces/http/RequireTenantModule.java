package com.ucacue.bar.erp.commercial.interfaces.http;

import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireTenantModule {
    ModuleCode value();
}
