package com.example.atlasrh.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.atlasrh", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {
  @ArchTest
  static final ArchRule controllersUseOnlyServiceContracts = noClasses()
      .that().resideInAPackage("..controller..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..entity..", "..repository..", "..mapper..", "..service.impl..");

  @ArchTest
  static final ArchRule serviceContractsAreInterfaces = classes()
      .that().resideInAPackage("..service")
      .should().beInterfaces();

  @ArchTest
  static final ArchRule serviceContractsDoNotExposePersistence = noClasses()
      .that().resideInAPackage("..service")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..entity..", "..repository..", "..mapper..", "..service.impl..");

  @ArchTest
  static final ArchRule dtosDoNotDependOnPersistenceOrServices = noClasses()
      .that().resideInAPackage("..dto..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..entity..", "..repository..", "..mapper..", "..service..");

  @ArchTest
  static final ArchRule entitiesDoNotDependOnUpperLayers = noClasses()
      .that().resideInAPackage("..entity..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..", "..mapper..");
}
