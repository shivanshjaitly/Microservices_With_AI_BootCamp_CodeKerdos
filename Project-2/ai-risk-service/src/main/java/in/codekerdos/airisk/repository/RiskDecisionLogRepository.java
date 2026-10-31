package in.codekerdos.airisk.repository;

import in.codekerdos.airisk.entity.RiskDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskDecisionLogRepository extends JpaRepository<RiskDecisionLog, Long> {

    List<RiskDecisionLog> findByOrderId(String orderId);
}
