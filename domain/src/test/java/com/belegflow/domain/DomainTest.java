package com.belegflow.domain;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.stream.Stream;
public class DomainTest {
    @TestFactory Stream<DynamicTest> allBusinessRules() {
        return DomainChecks.cases().stream().map(c -> DynamicTest.dynamicTest(c.name(), c.body()::run));
    }
}
