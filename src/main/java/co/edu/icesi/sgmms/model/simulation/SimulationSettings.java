package co.edu.icesi.sgmms.model.simulation;

import java.io.Serializable;

/** Tunable parameters of the simulation (probabilities, traffic and limits). */
public class SimulationSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private double robberyProbability = 0.10;
    private double fireProbability = 0.05;
    private int civilianCars = 16;
    private int maxActiveIncidents = 12;
    private int accidentCooldownMinutes = 6;
    private long seed = System.nanoTime();

    /** Probability that a robbery appears in a given simulated minute. */
    public double getRobberyProbability() {
        return robberyProbability;
    }

    public void setRobberyProbability(double robberyProbability) {
        this.robberyProbability = robberyProbability;
    }

    /** Probability that a fire appears in a given simulated minute. */
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

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
