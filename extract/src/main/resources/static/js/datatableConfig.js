/**
 * DataTables configuration utilities
 *
 * This module provides configuration helpers for DataTables with i18n support.
 * Configuration data is injected by the Thymeleaf template via EXTRACT_CONFIG global object.
 */

/**
 * Makes every table column sort alphabetically while ignoring the case AND the accents.
 *
 * The ordering that DataTables applies by default lowercases the text but then compares code points,
 * so an accented letter lands after "z" ("Ärgerlich" after "Zurich"). Delegating the comparison to
 * localeCompare with a base sensitivity puts it back next to its plain counterpart, and keeps the
 * tables consistent with the drop-downs, which are sorted the same way.
 *
 * Both the "string" and the "html" types are overridden, because DataTables detects a cell holding a
 * link (the name column of most tables) as HTML rather than as plain text.
 */
(function useAlphabeticalOrderingInTables() {

    if (typeof $ === 'undefined' || !$.fn || !$.fn.dataTable) {
        return;
    }

    var compare = function(a, b) {
        return String(a).localeCompare(String(b), undefined, { sensitivity: 'base' });
    };

    var order = $.fn.dataTable.ext.type.order;
    order['string-asc'] = function(a, b) { return compare(a, b); };
    order['string-desc'] = function(a, b) { return compare(b, a); };
    order['html-asc'] = function(a, b) { return compare(a, b); };
    order['html-desc'] = function(a, b) { return compare(b, a); };
})();

/**
 * Gets the base DataTables configuration properties with i18n support
 *
 * @returns {Object} DataTables configuration object
 */
function getDataTableBaseProperties() {
    // Check if configuration has been injected by the template
    if (typeof EXTRACT_CONFIG === 'undefined' || !EXTRACT_CONFIG) {
        console.error('EXTRACT_CONFIG is not defined. DataTables i18n will not work properly.');
        return getDefaultDataTableProperties();
    }

    var languageCode = EXTRACT_CONFIG.language || 'fr';

    // Map language codes to DataTables i18n file names
    var languageFileMap = {
        'fr': 'fr-FR',
        'de': 'de-DE',
        'en': 'en-GB'
    };

    var languageFile = languageFileMap[languageCode] || 'fr-FR';
    var languageUrl = EXTRACT_CONFIG.datatables.i18nPath || '/lib/datatables.net-plugins/i18n/';

    return {
        "language": {
            "url": languageUrl + languageFile + '.json'
        },
        "pagingType": "simple_numbers",
        "info": false,
        "lengthChange": false,
        "layout": {
            "topEnd": null
        }
    };
}

/**
 * Fallback configuration when EXTRACT_CONFIG is not available
 * @private
 */
function getDefaultDataTableProperties() {
    return {
        "language": {
            "url": '/lib/datatables.net-plugins/i18n/fr-FR.json'
        },
        "pagingType": "simple_numbers",
        "info": false,
        "lengthChange": false,
        "layout": {
            "topEnd": null
        }
    };
}
