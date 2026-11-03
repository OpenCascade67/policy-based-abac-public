package com.pdp.evaluation;

/**
 * Wraps the input of a regex match and aborts it once a deadline passes, protecting the evaluator from
 * catastrophic backtracking in user-supplied patterns (Java's regex engine has no built-in timeout).
 */
final class DeadlineCharSequence implements CharSequence {

    static final class TimeoutException extends RuntimeException {
        TimeoutException() {
            super("Regular expression evaluation timed out.", null, false, false);
        }
    }

    private final CharSequence inner;
    private final long deadlineNanos;

    DeadlineCharSequence(CharSequence inner, long deadlineNanos) {
        this.inner = inner;
        this.deadlineNanos = deadlineNanos;
    }

    @Override
    public char charAt(int index) {
        if (System.nanoTime() > deadlineNanos) throw new TimeoutException();
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new DeadlineCharSequence(inner.subSequence(start, end), deadlineNanos);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
