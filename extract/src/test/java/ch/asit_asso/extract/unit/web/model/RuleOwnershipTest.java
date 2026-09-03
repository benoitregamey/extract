/*
 * Copyright (C) 2026 asit-asso
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
package ch.asit_asso.extract.unit.web.model;

import java.util.ArrayList;
import java.util.List;
import ch.asit_asso.extract.domain.Connector;
import ch.asit_asso.extract.domain.Rule;
import ch.asit_asso.extract.web.model.ConnectorModel;
import ch.asit_asso.extract.web.model.RuleModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that saving a connector cannot write into a rule of another connector (issue #428).
 *
 * The identifier that the form carries for a rule is not trustworthy: a rule added through the interface gets a
 * temporary identifier computed from the rules of the form, and the browser can restore a value of its own into a
 * field of the same name, the forms of two connectors being identical. Only the rules that the edited connector
 * holds may be written to, and what makes a rule new is its tag, never its identifier.
 *
 * @author Bruno Alves
 */
@DisplayName("Rule Ownership Tests (issue #428)")
class RuleOwnershipTest {

    /**
     * The identifier of the rule that belongs to another connector.
     */
    private static final int FOREIGN_RULE_ID = 1;



    @Nested
    @DisplayName("1. What makes a rule new")
    class NewRuleTests {

        @Test
        @DisplayName("1.1 - A rule tagged as added is new, whatever its identifier")
        void aTaggedRuleIsNew() {
            final RuleModel ruleModel = new RuleModel();
            ruleModel.setTag(RuleModel.TAG_ADDED);
            ruleModel.setId(RuleOwnershipTest.FOREIGN_RULE_ID);

            assertTrue(ruleModel.isNew(), "A rule carrying the added tag must be treated as a new one.");
        }



        @Test
        @DisplayName("1.2 - An untagged rule is not new, even without a usable identifier")
        void anUntaggedRuleIsNotNew() {
            final RuleModel withoutId = new RuleModel();
            final RuleModel withNegativeId = new RuleModel();
            withNegativeId.setId(-1);

            assertFalse(withoutId.isNew(), "An untagged rule must not be taken for a new one because its"
                    + " identifier is missing: the submission has to be refused instead.");
            assertFalse(withNegativeId.isNew(), "An untagged rule must not be taken for a new one because its"
                    + " identifier is invalid, or the check on the submitted identifiers could be walked around.");
        }
    }



    @Nested
    @DisplayName("2. Checking the submitted rules")
    class ForeignRuleIdsTests {

        @Test
        @DisplayName("2.1 - A submitted rule that belongs to another connector is reported")
        void aForeignRuleIsReported() {
            final Connector editedConnector = RuleOwnershipTest.this.connector(12);
            final ConnectorModel connectorModel
                    = RuleOwnershipTest.this.connectorModel(RuleOwnershipTest.FOREIGN_RULE_ID);

            assertArrayEquals(new Integer[]{RuleOwnershipTest.FOREIGN_RULE_ID},
                              connectorModel.getForeignRuleIds(editedConnector));
        }



        @Test
        @DisplayName("2.2 - Rules that all belong to the connector are accepted")
        void ownRulesAreAccepted() {
            final Connector editedConnector = RuleOwnershipTest.this.connector(12);
            RuleOwnershipTest.this.rule(15, editedConnector);
            RuleOwnershipTest.this.rule(16, editedConnector);
            final ConnectorModel connectorModel = RuleOwnershipTest.this.connectorModel(15, 16);

            assertArrayEquals(new Integer[]{}, connectorModel.getForeignRuleIds(editedConnector));
        }



        @Test
        @DisplayName("2.3 - The temporary identifier of an added rule is not reported")
        void anAddedRuleIsNotReported() {
            final Connector editedConnector = RuleOwnershipTest.this.connector(12);
            final ConnectorModel connectorModel = new ConnectorModel();
            final RuleModel addedRule = new RuleModel();
            addedRule.setTag(RuleModel.TAG_ADDED);
            addedRule.setId(RuleOwnershipTest.FOREIGN_RULE_ID);
            connectorModel.setRules(new RuleModel[]{addedRule});

            assertArrayEquals(new Integer[]{}, connectorModel.getForeignRuleIds(editedConnector));
        }



        @Test
        @DisplayName("2.4 - An untagged rule with an invalid identifier is reported, not created")
        void anUntaggedRuleWithAnInvalidIdentifierIsReported() {
            final Connector editedConnector = RuleOwnershipTest.this.connector(12);
            final ConnectorModel connectorModel = RuleOwnershipTest.this.connectorModel(-1);

            assertArrayEquals(new Integer[]{-1}, connectorModel.getForeignRuleIds(editedConnector));
        }



        @Test
        @DisplayName("2.5 - A rule claiming an identifier is reported when the connector is being created")
        void aRuleWithAnIdentifierIsReportedOnCreation() {
            final ConnectorModel connectorModel
                    = RuleOwnershipTest.this.connectorModel(RuleOwnershipTest.FOREIGN_RULE_ID);

            assertArrayEquals(new Integer[]{RuleOwnershipTest.FOREIGN_RULE_ID},
                              connectorModel.getForeignRuleIds());
        }



        @Test
        @DisplayName("2.6 - The rules of a connector being created are accepted when they are all new")
        void addedRulesAreAcceptedOnCreation() {
            final ConnectorModel connectorModel = new ConnectorModel();
            final RuleModel addedRule = new RuleModel();
            addedRule.setTag(RuleModel.TAG_ADDED);
            addedRule.setId(RuleOwnershipTest.FOREIGN_RULE_ID);
            connectorModel.setRules(new RuleModel[]{addedRule});

            assertArrayEquals(new Integer[]{}, connectorModel.getForeignRuleIds());
        }
    }



    /**
     * Makes a connector data object that holds no rule yet.
     *
     * @param connectorId the identifier of the connector
     * @return the connector data object
     */
    private Connector connector(final int connectorId) {
        final Connector domainConnector = new Connector(connectorId);
        domainConnector.setRulesCollection(new ArrayList<>());

        return domainConnector;
    }



    /**
     * Makes a rule and adds it to a connector.
     *
     * @param ruleId          the identifier of the rule
     * @param domainConnector the connector that the rule is part of
     * @return the rule data object
     */
    private Rule rule(final int ruleId, final Connector domainConnector) {
        final Rule domainRule = new Rule(ruleId);
        domainRule.setConnector(domainConnector);
        domainRule.setPosition(domainConnector.getRulesCollection().size() + 1);
        domainConnector.getRulesCollection().add(domainRule);

        return domainRule;
    }



    /**
     * Makes the model of a connector that submits untagged rules with the given identifiers.
     *
     * @param ruleIds the identifiers carried by the submitted rules
     * @return the connector model
     */
    private ConnectorModel connectorModel(final int... ruleIds) {
        final ConnectorModel connectorModel = new ConnectorModel();
        final List<RuleModel> rules = new ArrayList<>();

        for (int ruleId : ruleIds) {
            final RuleModel ruleModel = new RuleModel();
            ruleModel.setId(ruleId);
            rules.add(ruleModel);
        }

        connectorModel.setRules(rules.toArray(RuleModel[]::new));

        return connectorModel;
    }
}
