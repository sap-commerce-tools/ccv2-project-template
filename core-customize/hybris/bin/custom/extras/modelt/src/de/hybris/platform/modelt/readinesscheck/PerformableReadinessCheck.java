package de.hybris.platform.modelt.readinesscheck;
/**
 * Helper interface to make your readiness check "performable".
 */
public interface PerformableReadinessCheck extends ReadinessCheck
{
    boolean isEnabled();
    void perform();
}
