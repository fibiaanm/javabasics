package com.example.javabasics.demo;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Programmatic transactions for the demos, so every demo shows exactly where a
 * transaction starts and ends (instead of hiding it behind @Transactional).
 */
@Component
public class Tx {

    private final TransactionTemplate required;
    private final TransactionTemplate requiresNew;

    public Tx(PlatformTransactionManager transactionManager) {
        this.required = new TransactionTemplate(transactionManager);
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Runs the action in a transaction and commits. */
    public <T> T run(Supplier<T> action) {
        return required.execute(status -> action.get());
    }

    public void runVoid(Runnable action) {
        required.executeWithoutResult(status -> action.run());
    }

    /** Runs the action in a transaction that is ALWAYS rolled back (demos leave no data behind). */
    public <T> T runAndRollback(Supplier<T> action) {
        return required.execute(status -> {
            T result = action.get();
            status.setRollbackOnly();
            return result;
        });
    }

    /** Runs the action in a brand new, independent transaction (suspends the current one). */
    public <T> T runNew(Supplier<T> action) {
        return requiresNew.execute(status -> action.get());
    }

    public <T> T runWithStatus(Function<TransactionStatus, T> action) {
        return required.execute(action::apply);
    }
}
