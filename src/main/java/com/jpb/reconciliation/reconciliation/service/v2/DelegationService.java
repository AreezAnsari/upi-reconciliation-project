package com.jpb.reconciliation.reconciliation.service.v2;

public interface DelegationService {

    /** Called when user goes INACTIVE — transfers children to parent, records delegation */
    void delegateOnInactivate(Long delegatorUserId, String triggeredBy);

    /** Called when user is reactivated — restores children back to original parent */
    void restoreDelegationOnReactivate(Long delegatorUserId, String triggeredBy);

    /** Called when delegator's block is scheduled (BLOCK_PENDING) — notify delegatee */
    void notifyDelegateeBlockPending(Long delegatorUserId, String blockAt);

    /** Called when delegator is permanently BLOCKED — notify delegatee */
    void notifyDelegateeBlocked(Long delegatorUserId);

    /** Called when delegator is unblocked — notify delegatee */
    void notifyDelegateeUnblocked(Long delegatorUserId);
}
