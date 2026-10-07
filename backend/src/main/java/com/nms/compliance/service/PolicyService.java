package com.nms.compliance.service;

import com.nms.compliance.dto.CreatePolicyRequest;
import com.nms.compliance.dto.PolicyDTO;
import com.nms.compliance.dto.PolicyRuleDTO;
import com.nms.compliance.dto.UpdatePolicyRequest;
import com.nms.compliance.entity.Device;
import com.nms.compliance.entity.Policy;
import com.nms.compliance.entity.PolicyRule;
import com.nms.compliance.exception.ResourceNotFoundException;
import com.nms.compliance.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public List<PolicyDTO> getAllPolicies() {
        return policyRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public PolicyDTO getPolicyById(Long id) {
        return mapToDTO(getPolicyEntity(id));
    }

    public Policy getPolicyEntity(Long id) {
        return policyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
    }

    @Transactional
    public PolicyDTO createPolicy(CreatePolicyRequest request) {
        Policy policy = new Policy();
        policy.setPolicyName(request.getPolicyName());
        policy.setDeviceType(request.getDeviceType());
        policy.setVendor(request.getVendor());
        policy.setVersion(request.getVersion());
        policy.setDescription(request.getDescription());
        policy.setActive(request.isActive());
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());

        if (request.getRules() != null) {
            List<PolicyRule> rules = request.getRules().stream().map(r -> mapRuleToEntity(r, policy)).collect(Collectors.toList());
            policy.setRules(rules);
        }

        return mapToDTO(policyRepository.save(policy));
    }

    @Transactional
    public PolicyDTO updatePolicy(Long id, UpdatePolicyRequest request) {
        Policy policy = getPolicyEntity(id);
        policy.setPolicyName(request.getPolicyName());
        policy.setDeviceType(request.getDeviceType());
        policy.setVendor(request.getVendor());
        policy.setVersion(request.getVersion());
        policy.setDescription(request.getDescription());
        policy.setActive(request.isActive());
        policy.setUpdatedAt(LocalDateTime.now());

        if (request.getRules() != null) {
            policy.getRules().clear();
            List<PolicyRule> newRules = request.getRules().stream().map(r -> mapRuleToEntity(r, policy)).collect(Collectors.toList());
            policy.getRules().addAll(newRules);
        }

        return mapToDTO(policyRepository.save(policy));
    }

    @Transactional
    public void deletePolicy(Long id) {
        policyRepository.delete(getPolicyEntity(id));
    }

    public Policy findApplicablePolicy(Device device) {
        if (device.getAppliedPolicy() != null) return device.getAppliedPolicy();
        List<Policy> activePolicies = policyRepository.findByDeviceTypeAndIsActive(device.getDeviceType(), true);
        return activePolicies.isEmpty() ? null : activePolicies.get(0);
    }

    public Policy getPolicyWithRules(Long id) {
        return getPolicyEntity(id);
    }

    private PolicyRule mapRuleToEntity(PolicyRuleDTO dto, Policy policy) {
        PolicyRule rule = new PolicyRule();
        rule.setPolicy(policy);
        rule.setParameter(dto.getParameter());
        rule.setOperator(PolicyRule.RuleOperator.valueOf(dto.getOperator()));
        rule.setExpectedValue(dto.getExpectedValue());
        rule.setSeverity(PolicyRule.Severity.valueOf(dto.getSeverity()));
        rule.setDescription(dto.getDescription());
        rule.setRemediation(dto.getRemediation());
        rule.setRuleOrder(dto.getRuleOrder());
        rule.setCreatedAt(LocalDateTime.now());
        return rule;
    }

    private PolicyDTO mapToDTO(Policy policy) {
        PolicyDTO dto = new PolicyDTO();
        dto.setId(policy.getId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setDeviceType(policy.getDeviceType() != null ? policy.getDeviceType().name() : null);
        dto.setVendor(policy.getVendor());
        dto.setVersion(policy.getVersion());
        dto.setDescription(policy.getDescription());
        dto.setActive(policy.isActive());
        if (policy.getRules() != null) {
            dto.setRules(policy.getRules().stream().map(this::mapRuleToDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    private PolicyRuleDTO mapRuleToDTO(PolicyRule rule) {
        PolicyRuleDTO dto = new PolicyRuleDTO();
        dto.setId(rule.getId());
        dto.setParameter(rule.getParameter());
        dto.setOperator(rule.getOperator() != null ? rule.getOperator().name() : null);
        dto.setExpectedValue(rule.getExpectedValue());
        dto.setSeverity(rule.getSeverity() != null ? rule.getSeverity().name() : null);
        dto.setDescription(rule.getDescription());
        dto.setRemediation(rule.getRemediation());
        dto.setRuleOrder(rule.getRuleOrder());
        return dto;
    }
}
