package com.goosethings.tools.task.profession;

/** Bounded public feedback; no answers or client classes enter a snapshot. */
public enum ProfessionFeedback {
    READY("Read the given data and configure this stage."),
    CALC_A("The first calculation does not match the selected configuration; check its formula and unit."),
    CALC_B("The second calculation is incorrect; check the unit and the stated tolerance."),
    CALC_C("The third calculation is incorrect; check signs, units and moisture basis."),
    MISSING("Configure every required component before checking."),
    OPTICAL("Received optical power must satisfy sensitivity + 3 dB and the overload limit."),
    MODULATION("This modulation does not meet the stated device SNR threshold."),
    BANDWIDTH("Occupied bandwidth exceeds the allocated spectrum."),
    THROUGHPUT("Net throughput must meet demand and stay below 80% of the theoretical capacity."),
    IQ("Calibrate phase, gain and both DC offsets; EVM must stay at or below 3.5%."),
    HOLD("Keep the instruments inside the acceptance window for 0.8 seconds."),
    INTERFERENCE("Adjacent stations need at least 10 MHz channel separation."),
    CIRCUIT("Identify the heat source, exchanger and heat sink; primary and secondary fluids stay separate."),
    SUBCRITICAL("Classify the neutron production/loss ratio correctly; shutdown still produces decay heat."),
    PUMP("Select the smallest flow rating with at least 10% reserve."),
    INDEPENDENCE("A and B must use different power buses and different heat sinks; both valves must be open."),
    COOLING("Each branch must independently remove 110–125% of the stated decay heat without exceeding its pump rating."),
    TESTING("Running normal operation, loss of A and loss of B; keep the temperature in the shown window."),
    TEMPERATURE("The cooling test left the 80–95 °C game window. Correct the configuration and retry."),
    DOSE("Complete the required work time and stay within the displayed integrated dose budget."),
    HAZARD("Match the hazards to the specified validated controls; distinguish GHP from CCP."),
    LETHALITY("The product cold point needs the required cumulative lethality without exceeding the quality limit."),
    CONTAMINATION("Separate raw, ready-to-eat and allergen flows, with dedicated tools and verified cleaning."),
    TRACE("Apply the given plan: failed-lot descendants held; irreversible chemical residue rejected; validated thermal rework; compliant records released."),
    LEVEL("Use the stated equal-length closure correction, with the correct backsight/foresight signs."),
    DIAGRAM("The shear diagram needs +R at the left and −R at the right; the midspan moment must match the load."),
    SECTION("Check stress, deflection and mass together; a section that passes strength alone is insufficient."),
    CONCRETE("Use the oven-dry moisture basis and a compliant curing/strength report."),
    SUCCESS("All four stages passed.");

    public final String fallback;
    ProfessionFeedback(String fallback) { this.fallback = fallback; }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
}
