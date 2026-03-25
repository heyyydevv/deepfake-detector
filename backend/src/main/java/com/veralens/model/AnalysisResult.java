package com.veralens.detector.model;

import java.util.List;

/**
 * AnalysisResult — the JSON response returned to the frontend.
 *
 * {
 *   "result":     "REAL" | "FAKE",
 *   "confidence": 0-100,
 *   "rules":      [ { name, triggered, score } ]
 * }
 */
public class AnalysisResult {

    private String result;
    private int confidence;
    private List<RuleResult> rules;

    public AnalysisResult(String result, int confidence, List<RuleResult> rules) {
        this.result = result;
        this.confidence = confidence;
        this.rules = rules;
    }

    // Getters (required for Jackson serialisation)
    public String getResult()     { return result; }
    public int getConfidence()    { return confidence; }
    public List<RuleResult> getRules() { return rules; }

    // ---- Inner class ----
    public static class RuleResult {
        private String name;
        private boolean triggered;
        private int score;

        public RuleResult(String name, boolean triggered, int score) {
            this.name = name;
            this.triggered = triggered;
            this.score = score;
        }

        public String getName()      { return name; }
        public boolean isTriggered() { return triggered; }
        public int getScore()        { return score; }
    }
}
