package model;

/**
 * Property Value — Type-aware value wrapper
 *
 * All property values are stored as strings, but this utility class
 * provides type-aware comparison operations for queries.
 *
 * Supported types (auto-detected from string representation):
 *   - INTEGER:  whole numbers (e.g., "25", "-3")
 *   - FLOAT:    decimal numbers (e.g., "3.14", "-0.5")
 *   - BOOLEAN:  "true" or "false" (case-insensitive)
 *   - STRING:   everything else
 */
public class PropertyValue {

    public enum DataType { INTEGER, FLOAT, BOOLEAN, STRING }

    /**
     * Auto-detect the data type of a string value.
     */
    public static DataType detectType(String value) {
        if (value == null) return DataType.STRING;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return DataType.BOOLEAN;
        }
        try {
            Long.parseLong(value);
            return DataType.INTEGER;
        } catch (NumberFormatException ignored) {}
        try {
            Double.parseDouble(value);
            return DataType.FLOAT;
        } catch (NumberFormatException ignored) {}
        return DataType.STRING;
    }

    /**
     * Compares two string values numerically if both are numeric,
     * otherwise falls back to lexicographic comparison.
     *
     * @return negative if a < b, zero if a == b, positive if a > b
     */
    public static int compare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        DataType typeA = detectType(a);
        DataType typeB = detectType(b);

        // If both are numeric (int or float), compare numerically
        if (isNumeric(typeA) && isNumeric(typeB)) {
            double da = Double.parseDouble(a);
            double db = Double.parseDouble(b);
            return Double.compare(da, db);
        }

        // Both boolean
        if (typeA == DataType.BOOLEAN && typeB == DataType.BOOLEAN) {
            boolean ba = Boolean.parseBoolean(a);
            boolean bb = Boolean.parseBoolean(b);
            return Boolean.compare(ba, bb);
        }

        // Fallback: string comparison
        return a.compareTo(b);
    }

    /**
     * Checks equality with type awareness.
     */
    public static boolean equals(String a, String b) {
        return compare(a, b) == 0;
    }

    /**
     * Evaluates a comparison operator between two values.
     *
     * @param left     the property value from the node
     * @param operator one of: =, !=, <, <=, >, >=
     * @param right    the value from the query condition
     * @return true if the condition holds
     */
    public static boolean evaluate(String left, String operator, String right) {
        int cmp = compare(left, right);
        switch (operator) {
            case "=":  return cmp == 0;
            case "!=": return cmp != 0;
            case "<":  return cmp < 0;
            case "<=": return cmp <= 0;
            case ">":  return cmp > 0;
            case ">=": return cmp >= 0;
            default:   return false;
        }
    }

    private static boolean isNumeric(DataType type) {
        return type == DataType.INTEGER || type == DataType.FLOAT;
    }
}
