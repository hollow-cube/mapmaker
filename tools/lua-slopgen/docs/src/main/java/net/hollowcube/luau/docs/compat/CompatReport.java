package net.hollowcube.luau.docs.compat;

import java.util.List;

/// Result of an [EngineApiDiff] comparison: every finding plus a summary of how many of each
/// kind. The build fails iff any breaking finding is present.
public record CompatReport(List<CompatFinding> findings) {

    public CompatReport {
        findings = List.copyOf(findings);
    }

    public boolean hasBreakingChanges() {
        for (var f : findings) if (f.category().isBreaking()) return true;
        return false;
    }

    public List<CompatFinding> breakingFindings() {
        return findings.stream().filter(f -> f.category().isBreaking()).toList();
    }
}
