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
package ch.asit_asso.extract.unit.utils;

import ch.asit_asso.extract.utils.AlphabeticalOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the AlphabeticalOrder class, which sorts the entries of the drop-downs and of the
 * tables of the application (issue #381).
 *
 * Tests:
 * - the ordering ignores the case
 * - the ordering ignores the accents, so that an accented letter is not pushed after "z"
 * - null values are sorted last instead of throwing
 * - the sort method keeps the source untouched and handles the empty and single-entry cases
 * - a fresh comparator is handed out on every call, because a Collator cannot be shared between
 *   threads
 *
 * @author Bruno Alves
 */
@DisplayName("AlphabeticalOrder Tests")
class AlphabeticalOrderTest {

    /**
     * A name holder, standing for the entities whose lists feed the drop-downs.
     */
    private record Entry(String name) {
    }


    @Nested
    @DisplayName("comparator() tests")
    class ComparatorTests {

        @Test
        @DisplayName("Ignores the case")
        void ignoresTheCase() {
            List<String> names = new ArrayList<>(Arrays.asList("zebra", "Alpha", "beta", "Zulu"));

            names.sort(AlphabeticalOrder.comparator());

            assertEquals(List.of("Alpha", "beta", "zebra", "Zulu"), names,
                         "A lowercase name must not be pushed after every uppercase one");
        }


        @Test
        @DisplayName("Ignores the accents")
        void ignoresTheAccents() {
            List<String> names = new ArrayList<>(Arrays.asList("Zurich", "élan", "Eau", "figue", "Ärger",
                                                               "avion"));

            names.sort(AlphabeticalOrder.comparator());

            assertEquals(List.of("Ärger", "avion", "Eau", "élan", "figue", "Zurich"), names,
                         "An accented letter must sort next to its base letter, not after z");
        }


        @Test
        @DisplayName("Considers a letter and its accented form as equal")
        void considersAccentedFormAsEqual() {
            Comparator<String> comparator = AlphabeticalOrder.comparator();

            assertEquals(0, comparator.compare("epsilon", "épsilon"), "é must compare as equal to e");
            assertEquals(0, comparator.compare("ARGER", "ärger"), "The case and the accent must be ignored");
        }


        @Test
        @DisplayName("Sorts the null values last")
        void sortsNullValuesLast() {
            List<String> names = new ArrayList<>(Arrays.asList("beta", null, "Alpha"));

            names.sort(AlphabeticalOrder.comparator());

            assertEquals(Arrays.asList("Alpha", "beta", null), names, "A null name must be sorted last");
        }


        @Test
        @DisplayName("Hands out a new comparator on every call")
        void handsOutANewComparatorOnEveryCall() {
            // A java.text.Collator is not safe to share between threads, so each caller must get its own.
            assertNotSame(AlphabeticalOrder.comparator(), AlphabeticalOrder.comparator());
        }
    }


    @Nested
    @DisplayName("sort() tests")
    class SortTests {

        @Test
        @DisplayName("Sorts the items on the accessed name")
        void sortsItemsOnTheAccessedName() {
            List<Entry> entries = List.of(new Entry("zebra"), new Entry("épsilon"), new Entry("Alpha"),
                                          new Entry("beta"), new Entry("Zulu"));

            List<Entry> sorted = AlphabeticalOrder.sort(entries, Entry::name);

            assertEquals(List.of("Alpha", "beta", "épsilon", "zebra", "Zulu"),
                         sorted.stream().map(Entry::name).toList(),
                         "The entries must be sorted ignoring the case and the accents");
        }


        @Test
        @DisplayName("Leaves the source collection untouched")
        void leavesTheSourceUntouched() {
            List<Entry> entries = new ArrayList<>(List.of(new Entry("zebra"), new Entry("Alpha")));

            AlphabeticalOrder.sort(entries, Entry::name);

            assertEquals(List.of("zebra", "Alpha"), entries.stream().map(Entry::name).toList(),
                         "Sorting must return a new list rather than reorder the source");
        }


        @Test
        @DisplayName("Handles an item whose name is null")
        void handlesANullName() {
            List<Entry> entries = List.of(new Entry("beta"), new Entry(null), new Entry("Alpha"));

            List<Entry> sorted = AlphabeticalOrder.sort(entries, Entry::name);

            assertEquals(Arrays.asList("Alpha", "beta", null), sorted.stream().map(Entry::name).toList(),
                         "An item without a name must be sorted last instead of throwing");
        }


        @Test
        @DisplayName("Handles the empty and the single-entry collections")
        void handlesEmptyAndSingleEntryCollections() {
            assertTrue(AlphabeticalOrder.sort(List.<Entry>of(), Entry::name).isEmpty(),
                       "An empty collection must yield an empty list");

            List<Entry> single = AlphabeticalOrder.sort(List.of(new Entry("only")), Entry::name);

            assertEquals(1, single.size());
            assertEquals("only", single.get(0).name());
        }
    }
}
