package com.zarahome.cuentas;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.zarahome.cuentas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {
    @ArchTest
    static final ArchRule dominioIndependiente = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages("java..", "..domain..");

    @ArchTest
    static final ArchRule aplicacionIndependiente = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideOutsideOfPackages("java..", "..domain..", "..application..");

    @ArchTest
    static final ArchRule restUsaPuertos = noClasses().that().resideInAPackage("..infrastructure.rest..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule restNoUsaServiciosConcretos = noClasses().that().resideInAPackage("..infrastructure.rest..")
            .should().dependOnClassesThat().haveSimpleNameStartingWith("Servicio");
}
