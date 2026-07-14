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
package ch.asit_asso.extract.integration.ordering;

import ch.asit_asso.extract.domain.Connector;
import ch.asit_asso.extract.domain.Process;
import ch.asit_asso.extract.domain.Remark;
import ch.asit_asso.extract.domain.User;
import ch.asit_asso.extract.domain.UserGroup;
import ch.asit_asso.extract.integration.DatabaseTestHelper;
import ch.asit_asso.extract.persistence.ConnectorsRepository;
import ch.asit_asso.extract.persistence.ProcessesRepository;
import ch.asit_asso.extract.persistence.RemarkRepository;
import ch.asit_asso.extract.persistence.UserGroupsRepository;
import ch.asit_asso.extract.persistence.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for issue #381: every data-driven dropdown must be sorted alphabetically,
 * ignoring the case.
 *
 * <p>The seeded names deliberately mix cases and include an accented one, so that the byte ordering a
 * plain SQL {@code ORDER BY name} would produce differs from the expected one on two counts: a byte sort
 * puts every uppercase letter before every lowercase one ({@code Zulu} before {@code beta}) and pushes
 * accented letters after {@code z} ({@code épsilon} after {@code Zulu}).</p>
 *
 * <p>The ordering is therefore performed in Java by {@link ch.asit_asso.extract.utils.AlphabeticalOrder},
 * which makes it identical on every installation regardless of the collation of the database server.</p>
 *
 * <p>Each test seeds its own entries behind a unique marker and only asserts on those, so that the rows
 * already present in the test database cannot make the assertions flaky.</p>
 *
 * @author Bruno Alves
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Dropdown Ordering Integration Tests (issue #381)")
class DropdownOrderingIntegrationTest {

    /**
     * The names seeded by the tests. Their order here is the one a byte sort would NOT produce.
     */
    private static final String[] MIXED_CASE_NAMES = {"zebra", "Alpha", "beta", "Zulu", "épsilon"};

    /**
     * The order the seeded names must come back in once sorted alphabetically, ignoring the case and the
     * accents. A byte sort would instead yield Alpha, Zulu, beta, zebra, épsilon.
     */
    private static final List<String> EXPECTED_ORDER = List.of("Alpha", "beta", "épsilon", "zebra", "Zulu");

    @Autowired
    private ConnectorsRepository connectorsRepository;

    @Autowired
    private ProcessesRepository processesRepository;

    @Autowired
    private UserGroupsRepository userGroupsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RemarkRepository remarkRepository;

    @Autowired
    private DatabaseTestHelper dbHelper;


    @Nested
    @DisplayName("1. Repository finders feeding the dropdowns")
    class RepositoryOrderingTests {

        @Test
        @DisplayName("1.1 - Connectors (home filter) are sorted ignoring the case")
        @Transactional
        void connectorsAreSortedIgnoringCase() {
            String marker = seedMarker("conn");
            Arrays.stream(MIXED_CASE_NAMES).forEach(name -> dbHelper.createTestConnector(marker + name));

            List<String> actual = seededNames(connectorsRepository.findAllSortedByName(),
                                              Connector::getName, marker);

            assertEquals(expectedOrder(marker), actual, "Connectors must be sorted ignoring the case");
        }


        @Test
        @DisplayName("1.2 - Processes (home filter and connector rules) are sorted ignoring the case")
        @Transactional
        void processesAreSortedIgnoringCase() {
            String marker = seedMarker("proc");
            Arrays.stream(MIXED_CASE_NAMES).forEach(name -> dbHelper.createTestProcess(marker + name));

            List<String> actual = seededNames(processesRepository.findAllSortedByName(),
                                              Process::getName, marker);

            assertEquals(expectedOrder(marker), actual, "Processes must be sorted ignoring the case");
        }


        @Test
        @DisplayName("1.3 - User groups (process and request operators) are sorted ignoring the case")
        @Transactional
        void userGroupsAreSortedIgnoringCase() {
            String marker = seedMarker("grp");
            Arrays.stream(MIXED_CASE_NAMES).forEach(name -> dbHelper.createTestUserGroup(marker + name));

            List<String> actual = seededNames(userGroupsRepository.findAllSortedByName(),
                                              UserGroup::getName, marker);

            assertEquals(expectedOrder(marker), actual, "User groups must be sorted ignoring the case");
        }


        @Test
        @DisplayName("1.4 - Predefined remarks are sorted by title ignoring the case")
        @Transactional
        void remarksAreSortedIgnoringCase() {
            String marker = seedMarker("rmk");

            for (String name : MIXED_CASE_NAMES) {
                Remark remark = new Remark();
                remark.setTitle(marker + name);
                remark.setContent("Content of " + name);
                remarkRepository.save(remark);
            }

            List<String> actual = seededNames(remarkRepository.findAllSortedByTitle(),
                                              Remark::getTitle, marker);

            assertEquals(expectedOrder(marker), actual, "Remarks must be sorted by title ignoring the case");
        }
    }


    @Nested
    @DisplayName("2. Active users finder (process, request and group operators)")
    class ActiveUsersOrderingTests {

        @Test
        @DisplayName("2.1 - Active application users are sorted by name ignoring the case")
        @Transactional
        void activeUsersAreSortedIgnoringCase() {
            String marker = seedMarker("usr");

            for (String name : MIXED_CASE_NAMES) {
                dbHelper.createTestOperator(marker + name, marker + name, marker + name + "@test.ch", true);
            }

            List<String> actual = seededNames(usersRepository.findAllActiveApplicationUsersSortedByName(),
                                              User::getName, marker);

            assertEquals(expectedOrder(marker), actual, "Active users must be sorted ignoring the case");
        }


        @Test
        @DisplayName("2.2 - Ordering does not resurrect inactive users nor the system user")
        @Transactional
        void orderingPreservesTheExistingFiltering() {
            String marker = seedMarker("filt");
            dbHelper.createTestOperator(marker + "active", marker + "active", marker + "a@test.ch", true);
            dbHelper.createTestOperator(marker + "inactive", marker + "inactive", marker + "i@test.ch", false);

            List<String> actual = seededNames(usersRepository.findAllActiveApplicationUsersSortedByName(),
                                              User::getName, marker);

            assertEquals(List.of(marker + "active"), actual, "Only the active seeded user must be returned");

            List<String> allLogins = new ArrayList<>();
            for (User user : usersRepository.findAllActiveApplicationUsersSortedByName()) {
                allLogins.add(user.getLogin());
            }
            assertFalse(allLogins.contains(User.SYSTEM_USER_LOGIN), "The system user must stay excluded");
        }


        @Test
        @DisplayName("2.3 - All application users (group members dropdown) are sorted ignoring the case")
        @Transactional
        void allApplicationUsersAreSortedIgnoringCase() {
            String marker = seedMarker("allusr");

            for (String name : MIXED_CASE_NAMES) {
                dbHelper.createTestOperator(marker + name, marker + name, marker + name + "@test.ch", true);
            }

            List<String> actual = seededNames(usersRepository.findAllApplicationUsersSortedByName(),
                                              User::getName, marker);

            assertEquals(expectedOrder(marker), actual, "All application users must be sorted ignoring the case");
        }
    }


    @Nested
    @DisplayName("3. Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("3.1 - A lowercase name is not pushed after every uppercase one")
        @Transactional
        void lowercaseNameIsNotPushedAfterEveryUppercaseOne() {
            String marker = seedMarker("case");
            dbHelper.createTestConnector(marker + "zurich");
            dbHelper.createTestConnector(marker + "Aarau");
            dbHelper.createTestConnector(marker + "Bern");

            List<String> actual = seededNames(connectorsRepository.findAllSortedByName(),
                                              Connector::getName, marker);

            assertEquals(List.of(marker + "Aarau", marker + "Bern", marker + "zurich"), actual,
                         "A lowercase name must not be pushed after every uppercase one");
        }


        /**
         * An accented letter must be ordered next to its plain counterpart, and NOT after "z" as a byte
         * ordering would have it. This is what the ordering in Java buys us: a SQL ORDER BY would answer
         * differently depending on the collation the PostgreSQL server was built with.
         */
        @Test
        @DisplayName("3.2 - An accented letter is ordered next to its unaccented counterpart, not after z")
        @Transactional
        void accentedLetterIsOrderedNextToItsUnaccentedCounterpart() {
            String marker = seedMarker("accent");
            dbHelper.createTestConnector(marker + "Zurich");
            dbHelper.createTestConnector(marker + "élan");
            dbHelper.createTestConnector(marker + "Eau");
            dbHelper.createTestConnector(marker + "figue");
            dbHelper.createTestConnector(marker + "Ärger");
            dbHelper.createTestConnector(marker + "avion");

            List<String> actual = seededNames(connectorsRepository.findAllSortedByName(),
                                              Connector::getName, marker);

            assertEquals(List.of(marker + "Ärger", marker + "avion", marker + "Eau", marker + "élan",
                                 marker + "figue", marker + "Zurich"),
                         actual,
                         "An accented letter must sort next to its plain counterpart, not after z");
        }


        @Test
        @DisplayName("3.2 - A single entry and an unmatched marker do not break the finder")
        @Transactional
        void singleAndEmptyResultsAreHandled() {
            String marker = seedMarker("single");
            dbHelper.createTestConnector(marker + "only");

            List<String> single = seededNames(connectorsRepository.findAllSortedByName(),
                                              Connector::getName, marker);
            assertEquals(List.of(marker + "only"), single, "A single entry must be returned as-is");

            List<String> none = seededNames(connectorsRepository.findAllSortedByName(),
                                            Connector::getName, seedMarker("absent"));
            assertTrue(none.isEmpty(), "An unmatched marker must yield no entry and no exception");
        }
    }


    // ==================== HELPER METHODS ====================

    /**
     * Builds a marker that is unique to a test run, so that the seeded rows can be told apart from the
     * ones already present in the test database.
     *
     * @param prefix a short prefix that identifies the seeded entity type
     * @return the marker to prepend to every seeded name
     */
    private static String seedMarker(final String prefix) {
        return String.format("ZZ381-%s-%d-", prefix, System.nanoTime());
    }


    /**
     * Builds the order in which the seeded names are expected to come back.
     *
     * @param marker the marker that prefixes the seeded names
     * @return the expected names, in the expected order
     */
    private static List<String> expectedOrder(final String marker) {
        return EXPECTED_ORDER.stream().map(name -> marker + name).toList();
    }


    /**
     * Keeps only the entries seeded by the current test, preserving the order returned by the finder.
     *
     * @param <T>          the type of the entries returned by the finder
     * @param entries      the entries returned by the finder
     * @param nameAccessor the function that reads the name to sort on
     * @param marker       the marker that prefixes the seeded names
     * @return the names of the seeded entries, in the order the finder returned them
     */
    private static <T> List<String> seededNames(final Iterable<T> entries, final Function<T, String> nameAccessor,
                                                final String marker) {
        return StreamSupport.stream(entries.spliterator(), false)
                            .map(nameAccessor)
                            .filter(name -> name != null && name.startsWith(marker))
                            .toList();
    }
}
