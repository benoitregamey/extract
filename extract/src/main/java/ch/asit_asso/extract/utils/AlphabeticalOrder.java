/*
 * Copyright (C) 2025 asit-asso
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.asit_asso.extract.utils;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Sorts the entries of the application drop-down lists alphabetically, ignoring the case and the accents
 * (so that <code>épsilon</code> comes between <code>beta</code> and <code>zebra</code>, and not after
 * <code>Zulu</code>).
 *
 * <p>The sorting is deliberately done in Java rather than by the database: a SQL <code>ORDER BY</code>
 * delegates to the collation of the server, which differs from one Extract installation to the next (a
 * glibc-based PostgreSQL sorts accented letters next to their plain counterpart, whereas a musl-based one
 * falls back to the byte order and pushes them after <code>z</code>). Sorting in the JVM makes the order
 * identical on every installation, and consistent with the client-side sorting performed by
 * <code>localeCompare(..., {sensitivity: 'base'})</code>.</p>
 *
 * <p>The lists concerned (connectors, processes, user groups, predefined remarks, users) hold at most a
 * few hundred entries, so sorting them in memory is inexpensive.</p>
 *
 * @author Bruno Alves
 */
public final class AlphabeticalOrder {

    /**
     * The locale whose alphabetical rules are applied. The application defaults to French and, at the
     * primary strength used here, the Latin-script locales agree on the ordering anyway.
     */
    private static final Locale ORDERING_LOCALE = Locale.FRENCH;


    /**
     * This class only exposes static members.
     */
    private AlphabeticalOrder() {
    }


    /**
     * Builds a comparator that orders strings alphabetically, ignoring the case and the accents. Entries
     * whose value is <code>null</code> are sorted last.
     *
     * <p>A new comparator (and thus a new collator) is returned on each call, because a
     * {@link Collator} is not safe to share between threads.</p>
     *
     * @return the comparator
     */
    public static Comparator<String> comparator() {
        final Collator collator = Collator.getInstance(AlphabeticalOrder.ORDERING_LOCALE);
        // PRIMARY only tells base letters apart: "a", "A" and "à" are considered equal.
        collator.setStrength(Collator.PRIMARY);

        return Comparator.nullsLast(collator::compare);
    }


    /**
     * Sorts items alphabetically based on a name, ignoring the case and the accents.
     *
     * @param <T>          the type of the items to sort
     * @param items        the items to sort
     * @param nameAccessor the function that reads the name to sort an item on
     * @return a new list that contains the items in alphabetical order
     */
    public static <T> List<T> sort(final Iterable<T> items, final Function<T, String> nameAccessor) {
        return StreamSupport.stream(items.spliterator(), false)
                            .sorted(Comparator.comparing(nameAccessor, AlphabeticalOrder.comparator()))
                            .toList();
    }
}
