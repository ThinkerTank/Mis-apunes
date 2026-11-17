package co.edu.icesi.sgmms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    private static AssignmentRecord optimal() {
        return new AssignmentRecord("INC", "A-1", 0, 5, 5, 5, 5);
    }

    @Test
    void highSeverityWithinTimeWithTheBestVehicleEarnsTheStatementExamplePlusRouting() {
        Incident high = TestCity.incident("INC", IncidentType.ACCIDENT, Severity.HIGH, 0, 1);

        ScoreBreakdown points = calculator.score(high, optimal(), 10);

        assertEquals(100, points.getBase());
        assertEquals(20, points.getTimeBonus());
        assertEquals(130, points.total());
    }

    @Test
    void lateResolutionEarnsOnlyTheBasePoints() {
        Incident high = TestCity.incident("INC", IncidentType.ACCIDENT, Severity.HIGH, 0, 1);

        ScoreBreakdown points = calculator.score(high, null, Severity.HIGH.getMaxAttentionMinutes() + 1);

        assertEquals(100, points.total());
    }

    @Test
    void baseScoreDependsOnSeverity() {
        assertEquals(70, calculator.score(TestCity.incident("M", IncidentType.ROBBERY, Severity.MEDIUM, 0, 1), null, 999).total());
        assertEquals(40, calculator.score(TestCity.incident("L", IncidentType.FIRE, Severity.LOW, 0, 1), null, 999).total());
    }

    @Test
    void farVehicleIsPenalizedAndScoreNeverGoesNegative() {
        Incident low = TestCity.incident("INC", IncidentType.ROBBERY, Severity.LOW, 0, 1);
        AssignmentRecord far = new AssignmentRecord("INC", "P-9", 0, 20, 4, 20, 4);

        ScoreBreakdown points = calculator.score(low, far, 999);

        assertEquals(0, points.getRoutingBonus());
        assertEquals(15, points.getRoutingPenalty());
        assertEquals(25, points.total());
    }
}
