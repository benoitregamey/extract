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
package ch.asit_asso.extract.functional.lists;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.asit_asso.extract.domain.Process;
import ch.asit_asso.extract.domain.UserGroup;
import ch.asit_asso.extract.functional.pages.LoginPage;
import ch.asit_asso.extract.persistence.ProcessesRepository;
import ch.asit_asso.extract.persistence.UserGroupsRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for the ordering of the tables (issue #381).
 *
 * The tables are sorted by DataTables, in the browser: no Java test can reach that code, and the
 * project has no JavaScript test harness. Driving a real browser is therefore the only way to cover the
 * ordering override installed by datatableConfig.js.
 *
 * The ordering must ignore the case AND the accents. The default DataTables ordering lowercases the text
 * (so the case was already handled) but then compares code points, which pushed every accented letter
 * after "z": "Ärger" ended up after "Zulu" instead of coming right after "Alpha".
 *
 * The rows are seeded behind a marker and the assertions only bear on those, so that the rows already
 * present in the database cannot make the test flaky.
 *
 * @author Bruno Alves
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("functional")
@DisplayName("Table Ordering Functional Tests (issue #381)")
public class TableOrderingFunctionalTest {

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD = "motdepasse21";

    private static final String APPLICATION_URL = "http://127.0.0.1:8080/extract";

    /**
     * The title that every page of the application carries. It tells the application apart from the 404
     * page that the server answers while the application is redeploying.
     */
    private static final String APPLICATION_TITLE = "Extract";

    /**
     * The class DataTables sets on the header of the column it sorts in ascending order.
     */
    private static final String ASCENDING_CLASS = "dt-ordering-asc";

    /**
     * The class DataTables sets on the header of the column it sorts in descending order.
     */
    private static final String DESCENDING_CLASS = "dt-ordering-desc";

    /**
     * How many times the header of a column is clicked before giving up on sorting it. Clicking a header
     * cycles through the ascending order, the descending one, then no order at all, so three clicks are
     * enough to reach any of them whatever the state the table starts in.
     */
    private static final int MAXIMUM_SORT_CLICKS = 3;

    /**
     * Prefixes the seeded names. It sorts after the existing rows, and lets the assertions ignore them.
     */
    private static final String MARKER = "ZZ381-";

    /**
     * The names to seed. Their order here is the one a code-point comparison would NOT produce.
     */
    private static final List<String> SEEDED_NAMES = List.of("zebra", "Alpha", "Zulu", "Ärger", "beta",
                                                             "épsilon");

    /**
     * The order the seeded names must be displayed in: the case and the accents are ignored, so "Ärger"
     * sits next to "Alpha" and "épsilon" between "beta" and "zebra".
     */
    private static final List<String> EXPECTED_ORDER = List.of("Alpha", "Ärger", "beta", "épsilon",
                                                               "zebra", "Zulu");

    @Autowired
    private ProcessesRepository processesRepository;

    @Autowired
    private UserGroupsRepository userGroupsRepository;

    private WebDriver driver;

    private final List<Integer> seededProcessIds = new ArrayList<>();

    private final List<Integer> seededGroupIds = new ArrayList<>();


    @BeforeAll
    public static void setUpClass() {
        WebDriverManager.chromedriver().setup();
    }


    @BeforeEach
    public void setUp() {
        this.seedRows();

        ChromeOptions options = new ChromeOptions();
        options.addArguments(List.of("--disable-gpu", "--window-size=1920,1200",
                                     "--ignore-certificate-errors", "--disable-extensions", "--no-sandbox",
                                     "--disable-dev-shm-usage", "--headless", "--remote-allow-origins=*",
                                     "--disable-logging", "--log-level=OFF"));

        this.driver = new ChromeDriver(options);
        this.driver.manage().timeouts().implicitlyWait(Duration.of(10, ChronoUnit.SECONDS));

        this.waitForTheApplicationToBeDeployed();
        new LoginPage(this.driver).loginAs(TableOrderingFunctionalTest.ADMIN_USERNAME,
                                           TableOrderingFunctionalTest.ADMIN_PASSWORD);
    }


    /**
     * Opens the application, waiting for it to answer.
     *
     * The build repackages the WAR that the application server deploys, so the application can still be
     * redeploying when the test starts, and it then answers a 404. Rather than assume it is up, the page
     * is requested again until it is served.
     */
    private void waitForTheApplicationToBeDeployed() {
        new WebDriverWait(this.driver, Duration.of(90, ChronoUnit.SECONDS))
                .pollingEvery(Duration.of(2, ChronoUnit.SECONDS))
                .ignoring(WebDriverException.class)
                .until(webDriver -> {
                    webDriver.get(TableOrderingFunctionalTest.APPLICATION_URL);

                    return TableOrderingFunctionalTest.APPLICATION_TITLE.equals(webDriver.getTitle());
                });
    }


    @AfterEach
    public void tearDown() {
        if (this.driver != null) {
            this.driver.quit();
        }

        this.seededProcessIds.forEach(id -> this.processesRepository.deleteById(id));
        this.seededProcessIds.clear();
        this.seededGroupIds.forEach(id -> this.userGroupsRepository.deleteById(id));
        this.seededGroupIds.clear();
    }


    @Test
    @DisplayName("The processes table ignores the case and the accents")
    public void processesTableIgnoresCaseAndAccents() {
        this.driver.get(TableOrderingFunctionalTest.APPLICATION_URL + "/processes");

        assertEquals(TableOrderingFunctionalTest.EXPECTED_ORDER, this.readSeededNamesOfSortedTable(),
                     "The processes table must sort ignoring the case and the accents");
    }


    @Test
    @DisplayName("The user groups table ignores the case and the accents")
    public void userGroupsTableIgnoresCaseAndAccents() {
        this.driver.get(TableOrderingFunctionalTest.APPLICATION_URL + "/userGroups");

        assertEquals(TableOrderingFunctionalTest.EXPECTED_ORDER, this.readSeededNamesOfSortedTable(),
                     "The user groups table must sort ignoring the case and the accents");
    }


    @Test
    @DisplayName("Sorting the processes table in reverse yields the opposite order")
    public void processesTableCanBeSortedInReverse() {
        this.driver.get(TableOrderingFunctionalTest.APPLICATION_URL + "/processes");
        this.sortOnTheNameColumn(TableOrderingFunctionalTest.DESCENDING_CLASS);

        List<String> expectedReversed = new ArrayList<>(TableOrderingFunctionalTest.EXPECTED_ORDER);
        Collections.reverse(expectedReversed);

        assertEquals(expectedReversed, this.readSeededNames(),
                     "Sorting in reverse must yield the opposite order, still ignoring case and accents");
    }


    /**
     * Sorts the displayed table on its name column, then reads the names that this test seeded.
     *
     * @return the seeded names, in the order the table displays them
     */
    private List<String> readSeededNamesOfSortedTable() {
        this.sortOnTheNameColumn(TableOrderingFunctionalTest.ASCENDING_CLASS);

        return this.readSeededNames();
    }


    /**
     * Sorts the displayed table on its name column, which is the first one of every list table.
     *
     * The tables do not all start in the same state: clicking the header once yields a descending order
     * on a table that was already sorted ascending. The header is therefore clicked until it carries the
     * class that stands for the wanted direction, rather than a fixed number of times.
     *
     * @param wantedClass the class DataTables sets on the header of the column it sorts on
     */
    private void sortOnTheNameColumn(final String wantedClass) {

        for (int attempt = 0; attempt < TableOrderingFunctionalTest.MAXIMUM_SORT_CLICKS; attempt++) {
            WebElement header = this.driver.findElement(By.cssSelector(
                    "table.dataTable thead th:first-child"));

            if (header.getAttribute("class").contains(wantedClass)) {
                return;
            }

            header.click();
        }

        throw new IllegalStateException(String.format("The name column could not be sorted %s.",
                                                      wantedClass));
    }


    /**
     * Reads the name column of the displayed table, keeping only the rows that this test seeded and
     * stripping the marker that prefixes them.
     *
     * @return the seeded names, in the order the table displays them
     */
    private List<String> readSeededNames() {
        List<WebElement> cells
                = this.driver.findElements(By.cssSelector("table.dataTable tbody tr td:first-child"));

        return cells.stream()
                    .map(cell -> cell.getText().trim())
                    .filter(name -> name.startsWith(TableOrderingFunctionalTest.MARKER))
                    .map(name -> name.substring(TableOrderingFunctionalTest.MARKER.length()))
                    .toList();
    }


    /**
     * Adds the rows whose ordering this test checks.
     */
    private void seedRows() {

        for (String name : TableOrderingFunctionalTest.SEEDED_NAMES) {
            Process process = new Process();
            process.setName(TableOrderingFunctionalTest.MARKER + name);
            this.seededProcessIds.add(this.processesRepository.save(process).getId());

            UserGroup group = new UserGroup();
            group.setName(TableOrderingFunctionalTest.MARKER + name);
            group.setUsersCollection(new ArrayList<>());
            group.setProcessesCollection(new ArrayList<>());
            this.seededGroupIds.add(this.userGroupsRepository.save(group).getId());
        }
    }
}
