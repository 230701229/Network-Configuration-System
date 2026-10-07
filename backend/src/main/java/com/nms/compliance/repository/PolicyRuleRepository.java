package com.nms.compliance.repository;

import com.nms.compliance.entity.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {
    List<PolicyRule> findByPolicyIdOrderByRuleOrder(Long policyId);
}
