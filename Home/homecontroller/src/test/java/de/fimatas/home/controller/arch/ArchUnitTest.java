package de.fimatas.home.controller.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.fimatas.home.controller.dao.ApplicationDatabaseDAO;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "de.fimatas.home.controller")
public class ArchUnitTest {

    private static final DescribedPredicate<JavaClass> USE_JDBC_OR_DAO =
            new DescribedPredicate<>("uses JdbcTemplate or ApplicationDatabaseDAO") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getDirectDependenciesFromSelf().stream()
                            .anyMatch(dependency -> {
                                JavaClass target = dependency.getTargetClass();
                                return target.isEquivalentTo(JdbcTemplate.class)
                                        || target.isEquivalentTo(ApplicationDatabaseDAO.class);
                            });
                }
            };

    private static final ArchCondition<JavaClass> HAVE_CORRECT_DEPENDS_ON =
            new ArchCondition<>("be annotated with @DependsOn containing ApplicationDatabaseDAO") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    if (!javaClass.isAnnotatedWith(DependsOn.class)) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                javaClass.getName() + " requires dependency but lacks @DependsOn"));
                        return;
                    }

                    DependsOn annotation = javaClass.getAnnotationOfType(DependsOn.class);
                    boolean containsDao = Arrays.asList(annotation.value())
                            .contains(ApplicationDatabaseDAO.APPLICATION_DATABASE_DAO);

                    if (!containsDao) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                javaClass.getName() + " lacks " + ApplicationDatabaseDAO.APPLICATION_DATABASE_DAO + " in @DependsOn"));
                    }
                }
            };

    @ArchTest
    static final ArchRule components_using_jdbc_or_dao_must_depend_on_dao =
            classes()
                    .that().areAnnotatedWith(Component.class)
                    .or().areAnnotatedWith(Service.class)
                    .and(USE_JDBC_OR_DAO)
                    .should(HAVE_CORRECT_DEPENDS_ON);
}