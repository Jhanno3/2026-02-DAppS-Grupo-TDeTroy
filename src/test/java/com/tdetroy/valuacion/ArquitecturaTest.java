package com.tdetroy.valuacion;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/** Reglas de arquitectura de constitution.md §2 y §3. */
@AnalyzeClasses(
        packages = "com.tdetroy.valuacion",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String CONTROLLERS = "..controllers..";
    private static final String SERVICES = "..services..";
    private static final String REPOSITORIES = "..repositories..";
    private static final String ENTITY = "..entity..";
    private static final String MODEL = "..model..";
    private static final String DTO = "..dto..";
    private static final String CONFIG = "..config..";

    // Flujo de capas: Controller -> Service -> Repository -> Entity, nunca al revés.

    @ArchTest
    static final ArchRule controllers_noAccedenALaPersistencia =
            noClasses()
                    .that()
                    .resideInAPackage(CONTROLLERS)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(REPOSITORIES, ENTITY);

    @ArchTest
    static final ArchRule services_noConocenHttp =
            noClasses()
                    .that()
                    .resideInAPackage(SERVICES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            CONTROLLERS,
                            DTO,
                            "jakarta.servlet..",
                            "org.springframework.http..",
                            "org.springframework.web..");

    @ArchTest
    static final ArchRule repositories_noConocenCapasSuperiores =
            noClasses()
                    .that()
                    .resideInAPackage(REPOSITORIES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(CONTROLLERS, SERVICES, DTO);

    @ArchTest
    static final ArchRule entity_noConoceCapasSuperiores =
            noClasses()
                    .that()
                    .resideInAPackage(ENTITY)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(CONTROLLERS, SERVICES, REPOSITORIES, DTO);

    @ArchTest
    static final ArchRule model_esDominioPuro =
            noClasses()
                    .that()
                    .resideInAPackage(MODEL)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            CONTROLLERS,
                            SERVICES,
                            REPOSITORIES,
                            ENTITY,
                            DTO,
                            CONFIG,
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "org.springframework..");

    @ArchTest
    static final ArchRule common_noDependeDeOtrasCapas =
            noClasses()
                    .that()
                    .resideInAPackage("..common..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(CONTROLLERS, SERVICES, REPOSITORIES, ENTITY, DTO, CONFIG);

    @ArchTest
    static final ArchRule services_sinCiclosEntreDominios =
            slices().matching("..services.(*)..").should().beFreeOfCycles();

    // Convenciones de nombres y forma.

    @ArchTest
    static final ArchRule controllers_terminanEnController =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .resideInAPackage(CONTROLLERS)
                    .andShould()
                    .haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule entidadesJpa_vivenEnEntity =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage(ENTITY)
                    .andShould()
                    .haveSimpleNameEndingWith("Entity");

    @ArchTest
    static final ArchRule serviceImpl_implementaSuInterfaz =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("ServiceImpl")
                    .should()
                    .resideInAPackage(SERVICES)
                    .andShould(implementarSuInterfaz());

    @ArchTest
    static final ArchRule dtos_sonRecords =
            classes()
                    .that()
                    .resideInAPackage(DTO)
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(serRecord());

    @ArchTest
    static final ArchRule inyeccion_porConstructor =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    /** {@code XServiceImpl} implementa {@code XService}. */
    private static ArchCondition<JavaClass> implementarSuInterfaz() {
        return new ArchCondition<>("implementar la interfaz de su mismo nombre sin Impl") {
            @Override
            public void check(JavaClass clase, ConditionEvents eventos) {
                String interfaz = clase.getSimpleName().replaceFirst("Impl$", "");
                boolean implementa =
                        clase.getRawInterfaces().stream()
                                .anyMatch(i -> i.getSimpleName().equals(interfaz));
                eventos.add(
                        new SimpleConditionEvent(
                                clase, implementa, clase.getName() + " no implementa " + interfaz));
            }
        };
    }

    private static ArchCondition<JavaClass> serRecord() {
        return new ArchCondition<>("ser un record") {
            @Override
            public void check(JavaClass clase, ConditionEvents eventos) {
                eventos.add(
                        new SimpleConditionEvent(
                                clase,
                                clase.reflect().isRecord(),
                                clase.getName() + " no es un record"));
            }
        };
    }
}
