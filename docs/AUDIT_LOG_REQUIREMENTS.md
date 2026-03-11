# Audit Log Requirements (Admin Actions)

Per BACKEND_IMPLEMENTATION_PLAN §5.10: "audit log where required" for admin APIs. All **state-changing** admin actions are logged via `AuditLogService` to `audit.log`.

## Admin Actions That Require Audit (All Implemented)

| Action | Entity | Audit Action | Location |
|--------|--------|--------------|----------|
| Verify agent | Agent | ADMIN_AGENT_VERIFIED | AdminService.verifyAgent |
| Suspend agent | Agent | ADMIN_AGENT_SUSPENDED | AdminService.suspendAgent |
| Activate agent | Agent | ADMIN_AGENT_ACTIVATED | AdminService.activateAgent |
| Set agent tier | Agent | ADMIN_AGENT_TIER | AdminService.setAgentTier |
| Approve application | KycApplication | ADMIN_APPLICATION_APPROVED | AdminService.approveApplication |
| Reject application | KycApplication | ADMIN_APPLICATION_REJECTED | AdminService.rejectApplication |
| Manual review application | KycApplication | ADMIN_APPLICATION_MANUAL_REVIEW | AdminService.manualReviewApplication |
| Clear compliance flag | AdminFlag | ADMIN_COMPLIANCE_CLEARED | AdminService.clearComplianceFlag |
| Block fraud | AdminFlag | ADMIN_FRAUD_BLOCKED | AdminService.blockFraud |

Read-only admin endpoints (list agents, get request, list deposits, etc.) do **not** require audit log entries.

## Logging

- Logger: `com.quickcash.audit`
- File: `logs/audit.log` (see logback-spring.xml)
- Each log entry includes: action, entityType, entityId, actorId, actorType, details.
