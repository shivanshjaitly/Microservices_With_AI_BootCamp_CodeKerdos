package in.codekerdos.airisk.controller;

import in.codekerdos.airisk.dto.RiskDecisionResponse;
import in.codekerdos.airisk.entity.RiskDecisionLog;
import in.codekerdos.airisk.exception.ResourceNotFoundException;
import in.codekerdos.airisk.repository.RiskDecisionLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskDecisionLogRepository riskDecisionLogRepository;

    public RiskController(RiskDecisionLogRepository riskDecisionLogRepository) {
        this.riskDecisionLogRepository = riskDecisionLogRepository;
    }

    /** Returns the most recent logged risk decision for the given order. */
    @GetMapping("/{orderId}")
    public RiskDecisionResponse getByOrderId(@PathVariable String orderId) {
        List<RiskDecisionLog> logs = riskDecisionLogRepository.findByOrderId(orderId);
        RiskDecisionLog latest = logs.stream()
                .max(Comparator.comparing(RiskDecisionLog::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("No risk decision found for order " + orderId));
        return RiskDecisionResponse.from(latest);
    }
}
