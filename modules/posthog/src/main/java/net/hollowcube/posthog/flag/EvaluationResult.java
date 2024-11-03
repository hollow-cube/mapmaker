package net.hollowcube.posthog.flag;

import org.jetbrains.annotations.NotNull;

public interface EvaluationResult {
    @NotNull EvaluationResult INCONCLUSIVE = new EvaluationResult() {
    };
    @NotNull EvaluationResult FALSE = new EvaluationResult() {
    };
    @NotNull EvaluationResult TRUE = new EvaluationResult() {
    };

}
