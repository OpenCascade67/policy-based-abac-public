package com.pap.repository;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/** Culture-aware name ordering (e.g. "apple" &lt; "Banana"), equivalent to .NET's default string ordering. */
public final class NameOrder implements Comparator<String> {

    public static final NameOrder INSTANCE = new NameOrder();

    private final Collator collator;

    private NameOrder() {
        collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.TERTIARY);
    }

    @Override
    public int compare(String a, String b) {
        // Collator is not thread-safe
        synchronized (collator) {
            return collator.compare(a, b);
        }
    }
}
