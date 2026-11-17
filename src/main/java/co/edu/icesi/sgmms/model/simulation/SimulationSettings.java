package co.edu.icesi.sgmms.model.simulation;

import java.io.Serializable;

/** Tunable parameters of the simulation (probabilities, traffic and limits). */
public class SimulationSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private double robberyProbability = 0.05;
    private double fireProbability = 0.025;
    private int civilianCars = 14;
    private int maxActiveIncidents = 12;
    private int accidentCooldownMinutes = 8;
    private double collisionProbability = 0.20;
    private long seed = System.nanoTime();

    /**
     * Probability that a robbery appears in a given simulated minute.
     *
     * <p>Complexity — time O(1), space O(1).
     */
    public double getRobberyProbability() {
        return robberyProbability;
    }

    public void setRobberyProbability(double robberyProbability) {
        this.robberyProbability = robberyProbability;
    }

    /**
     * Probability that a fire appears in a given simulated minute.
     *
     * <p>Complexity — time O(1), space O(1).
     */
    public double getFireProbability() {
        return fireProbability;
    }

    public void setFireProbability(double fireProbability) {
        this.fireProbability = fireProbability;
    }

    /** Number of private cars driving around the city. */
    public int getCivilianCars() {
        return civilianCars;
    }

    public void setCivilianCars(int civilianCars) {
        this.civilianCars = civilianCars;
    }

    /** No new incidents are generated while this many are active, so the game stays playable. */
    public int getMaxActiveIncidents() {
        return maxActiveIncidents;
    }

    public void setMaxActiveIncidents(int maxActiveIncidents) {
        this.maxActiveIncidents = maxActiveIncidents;
    }

    /** Minutes before the same criterion can raise another accident in the same place or district. */
    public int getAccidentCooldownMinutes() {
        return accidentCooldownMinutes;
    }

    public void setAccidentCooldownMinutes(int accidentCooldownMinutes) {
        this.accidentCooldownMinutes = accidentCooldownMinutes;
    }

    /**
     * Probability that two vehicles heading for the same cell actually crash: usually one of them
     * yields, so only a fraction of these conflicts end in a collision.
     */
    public double getCollisionProbability() {
        return collisionProbability;
    }

    public void setCollisionProbability(double collisionProbability) {
        this.collisionProbability = collisionProbability;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
