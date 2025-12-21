package com.yourcompany.a11y.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing the API response containing accessibility descriptions.
 */
public class A11yResult {
    private Map<String, LocalizedDescription> descriptions;

    public A11yResult() {
        this.descriptions = new HashMap<>();
    }

    public Map<String, LocalizedDescription> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Map<String, LocalizedDescription> descriptions) {
        this.descriptions = descriptions;
    }

    /**
     * Get the localized description for a specific locale.
     *
     * @param locale the locale code (e.g., "zh-CN", "en")
     * @return the localized description, or null if not found
     */
    public LocalizedDescription getForLocale(String locale) {
        if (descriptions == null) {
            return null;
        }
        return descriptions.get(locale);
    }

    /**
     * Add a localized description for a locale.
     *
     * @param locale the locale code
     * @param description the localized description
     */
    public void addDescription(String locale, LocalizedDescription description) {
        if (descriptions == null) {
            descriptions = new HashMap<>();
        }
        descriptions.put(locale, description);
    }

    /**
     * Check if this result has any descriptions.
     *
     * @return true if there are descriptions
     */
    public boolean hasDescriptions() {
        return descriptions != null && !descriptions.isEmpty();
    }

    @Override
    public String toString() {
        return "A11yResult{" +
                "descriptions=" + descriptions +
                '}';
    }
}
