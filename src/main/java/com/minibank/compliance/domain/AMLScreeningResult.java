package com.minibank.compliance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AMLScreeningResult {
    private final UUID id;
    private final UUID transactionId;
    private final UUID accountId;
    private final ScreeningType screeningType;
    private final ScreeningStatus status;
    private final RiskLevel riskLevel;
    private final double riskScore;
    private final List<RiskIndicator> riskIndicators;
    private final List<MatchResult> sanctionsMatches;
    private final List<MatchResult> pepsMatches;
    private final boolean requiresManualReview;
    private final String screeningProvider;
    private final Instant screenedAt;
    private final String notes;

    public AMLScreeningResult(UUID id, UUID transactionId, UUID accountId, ScreeningType screeningType,
                             ScreeningStatus status, RiskLevel riskLevel, double riskScore,
                             List<RiskIndicator> riskIndicators, List<MatchResult> sanctionsMatches,
                             List<MatchResult> pepsMatches, boolean requiresManualReview,
                             String screeningProvider, Instant screenedAt, String notes) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.transactionId = transactionId; // Can be null for account-level screening
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.screeningType = Objects.requireNonNull(screeningType, "Screening type cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.riskLevel = Objects.requireNonNull(riskLevel, "Risk level cannot be null");
        this.riskScore = riskScore;
        this.riskIndicators = List.copyOf(riskIndicators != null ? riskIndicators : List.of());
        this.sanctionsMatches = List.copyOf(sanctionsMatches != null ? sanctionsMatches : List.of());
        this.pepsMatches = List.copyOf(pepsMatches != null ? pepsMatches : List.of());
        this.requiresManualReview = requiresManualReview;
        this.screeningProvider = Objects.requireNonNull(screeningProvider, "Screening provider cannot be null");
        this.screenedAt = Objects.requireNonNull(screenedAt, "Screened at cannot be null");
        this.notes = notes;
    }

    public static AMLScreeningResult createPassed(UUID accountId, UUID transactionId, ScreeningType type,
                                                 String provider, double riskScore) {
        return new AMLScreeningResult(
            UUID.randomUUID(),
            transactionId,
            accountId,
            type,
            ScreeningStatus.PASSED,
            determineRiskLevel(riskScore),
            riskScore,
            List.of(),
            List.of(),
            List.of(),
            false,
            provider,
            Instant.now(),
            "Screening passed with no significant risk indicators"
        );
    }

    public static AMLScreeningResult createFailed(UUID accountId, UUID transactionId, ScreeningType type,
                                                 String provider, double riskScore, List<RiskIndicator> indicators,
                                                 List<MatchResult> sanctionsMatches, List<MatchResult> pepsMatches) {
        boolean requiresReview = riskScore >= 70.0 || !sanctionsMatches.isEmpty() || !pepsMatches.isEmpty();
        
        return new AMLScreeningResult(
            UUID.randomUUID(),
            transactionId,
            accountId,
            type,
            ScreeningStatus.FAILED,
            determineRiskLevel(riskScore),
            riskScore,
            indicators,
            sanctionsMatches,
            pepsMatches,
            requiresReview,
            provider,
            Instant.now(),
            "Screening failed due to risk indicators or matches"
        );
    }

    public static AMLScreeningResult createReviewRequired(UUID accountId, UUID transactionId, ScreeningType type,
                                                         String provider, double riskScore, List<RiskIndicator> indicators,
                                                         String reviewReason) {
        return new AMLScreeningResult(
            UUID.randomUUID(),
            transactionId,
            accountId,
            type,
            ScreeningStatus.REQUIRES_REVIEW,
            determineRiskLevel(riskScore),
            riskScore,
            indicators,
            List.of(),
            List.of(),
            true,
            provider,
            Instant.now(),
            reviewReason
        );
    }

    public boolean isPassed() {
        return status == ScreeningStatus.PASSED;
    }

    public boolean isFailed() {
        return status == ScreeningStatus.FAILED;
    }

    public boolean requiresReview() {
        return requiresManualReview || status == ScreeningStatus.REQUIRES_REVIEW;
    }

    public boolean hasHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    public boolean hasSanctionsMatches() {
        return !sanctionsMatches.isEmpty();
    }

    public boolean hasPEPsMatches() {
        return !pepsMatches.isEmpty();
    }

    private static RiskLevel determineRiskLevel(double riskScore) {
        if (riskScore >= 90.0) {
            return RiskLevel.CRITICAL;
        } else if (riskScore >= 70.0) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 40.0) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
    public ScreeningType getScreeningType() { return screeningType; }
    public ScreeningStatus getStatus() { return status; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public double getRiskScore() { return riskScore; }
    public List<RiskIndicator> getRiskIndicators() { return riskIndicators; }
    public List<MatchResult> getSanctionsMatches() { return sanctionsMatches; }
    public List<MatchResult> getPepsMatches() { return pepsMatches; }
    public boolean isRequiresManualReview() { return requiresManualReview; }
    public String getScreeningProvider() { return screeningProvider; }
    public Instant getScreenedAt() { return screenedAt; }
    public String getNotes() { return notes; }

    public enum ScreeningType {
        ACCOUNT_OPENING,
        TRANSACTION_MONITORING,
        PERIODIC_REVIEW,
        ENHANCED_DUE_DILIGENCE,
        CROSS_BORDER_PAYMENT
    }

    public enum ScreeningStatus {
        PASSED,
        FAILED,
        REQUIRES_REVIEW,
        IN_PROGRESS,
        ERROR
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static class RiskIndicator {
        private final String type;
        private final String description;
        private final int severity; // 1-10 scale
        private final String evidence;

        public RiskIndicator(String type, String description, int severity, String evidence) {
            this.type = Objects.requireNonNull(type, "Type cannot be null");
            this.description = Objects.requireNonNull(description, "Description cannot be null");
            this.severity = severity;
            this.evidence = evidence;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public int getSeverity() { return severity; }
        public String getEvidence() { return evidence; }
    }

    public static class MatchResult {
        private final String matchType;
        private final String entityName;
        private final double matchScore;
        private final String listSource;
        private final String matchDetails;

        public MatchResult(String matchType, String entityName, double matchScore, 
                          String listSource, String matchDetails) {
            this.matchType = Objects.requireNonNull(matchType, "Match type cannot be null");
            this.entityName = Objects.requireNonNull(entityName, "Entity name cannot be null");
            this.matchScore = matchScore;
            this.listSource = Objects.requireNonNull(listSource, "List source cannot be null");
            this.matchDetails = matchDetails;
        }

        public String getMatchType() { return matchType; }
        public String getEntityName() { return entityName; }
        public double getMatchScore() { return matchScore; }
        public String getListSource() { return listSource; }
        public String getMatchDetails() { return matchDetails; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AMLScreeningResult that = (AMLScreeningResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("AMLScreeningResult{accountId=%s, status=%s, riskLevel=%s, score=%.2f}",
                           accountId, status, riskLevel, riskScore);
    }
}