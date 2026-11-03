package com.pdp.store;

/** Source of the policy data decisions are made against. */
public interface PolicyStore {

    /** The latest successfully loaded data; never {@code null}. */
    PolicySnapshot current();

    /** Load state, for health reporting. */
    StoreStatus status();

    /**
     * @param lastError why the most recent load failed, or {@code null} if it succeeded
     */
    record StoreStatus(boolean available, PolicySnapshot snapshot, String lastError) {
    }
}
