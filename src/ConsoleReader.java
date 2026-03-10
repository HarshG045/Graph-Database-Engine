import query.QueryAutoComplete;

import java.util.List;
import java.util.Scanner;

/**
 * Console Reader with Autocomplete Support
 *
 * Provides a REPL-style input reader with suggestion support.
 *
 * How it works:
 *   - Type a partial command and press TAB then ENTER, or end with '?'
 *     to see context-aware suggestions for what comes next.
 *   - If only one suggestion matches, it is auto-filled for you.
 *   - If multiple suggestions match, they are displayed and you can
 *     continue typing.
 *
 * Uses Scanner for reliable cross-platform input (works on Windows CMD,
 * PowerShell, and Unix terminals without native or JNI dependencies).
 */
public class ConsoleReader {

    private final QueryAutoComplete autoComplete;
    private final Scanner scanner;

    public ConsoleReader(QueryAutoComplete autoComplete) {
        this.autoComplete = autoComplete;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Reads a line of input with autocomplete support.
     * Returns null if the input stream is closed.
     */
    public String readLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            System.out.flush();

            if (!scanner.hasNextLine()) {
                return null;
            }

            String line = scanner.nextLine();

            // Check if the user is requesting suggestions:
            //   - Line ends with '?'
            //   - Line contains a Tab character (typed Tab then Enter)
            boolean wantsSuggestions = false;
            String partial = line;

            if (line.endsWith("?")) {
                wantsSuggestions = true;
                partial = line.substring(0, line.length() - 1);
            } else if (line.contains("\t")) {
                wantsSuggestions = true;
                // Take everything up to the first tab as the partial input
                partial = line.substring(0, line.indexOf('\t'));
            }

            if (!wantsSuggestions) {
                return line;
            }

            // ── Show suggestions ──
            List<String> suggestions = autoComplete.suggest(partial);

            if (suggestions.isEmpty()) {
                System.out.println("  (no suggestions)");
                System.out.println();
                continue;  // re-prompt
            }

            if (suggestions.size() == 1) {
                // Single match — auto-fill and return the completed line
                String completed = buildCompletion(partial, suggestions.get(0));
                System.out.println("  >> " + completed);
                System.out.println();

                // Re-prompt with the auto-filled text so the user can extend it
                // If the completed text forms a full keyword, re-prompt for more
                return readLineWithPrefill(prompt, completed);
            }

            // Multiple matches — display them neatly
            System.out.println();
            printSuggestions(suggestions);
            System.out.println();

            // Fill common prefix and re-prompt
            String common = longestCommonPrefix(suggestions);
            String filled = buildCompletion(partial, common);

            // Re-prompt with the common prefix filled in
            return readLineWithPrefill(prompt, filled);
        }
    }

    /**
     * Re-prompts the user, showing a pre-filled value they can extend or replace.
     * Uses iterative approach to avoid stack overflow from deep autocomplete chains.
     */
    private String readLineWithPrefill(String prompt, String prefill) {
        String currentPrefill = prefill;
        while (true) {
            System.out.print(prompt + currentPrefill);
            System.out.flush();

            if (!scanner.hasNextLine()) {
                return null;
            }

            String extra = scanner.nextLine();

            // Check if user wants more suggestions on the extended input
            String fullLine = currentPrefill + extra;

            boolean wantsSuggestions = false;
            String partial = fullLine;

            if (fullLine.endsWith("?")) {
                wantsSuggestions = true;
                partial = fullLine.substring(0, fullLine.length() - 1);
            } else if (fullLine.contains("\t")) {
                wantsSuggestions = true;
                partial = fullLine.substring(0, fullLine.indexOf('\t'));
            }

            if (!wantsSuggestions) {
                return fullLine;
            }

            List<String> suggestions = autoComplete.suggest(partial);

            if (suggestions.isEmpty()) {
                System.out.println("  (no suggestions)");
                System.out.println();
                currentPrefill = partial + " ";
                continue;
            }

            if (suggestions.size() == 1) {
                String completed = buildCompletion(partial, suggestions.get(0));
                System.out.println("  >> " + completed);
                System.out.println();
                currentPrefill = completed;
                continue;
            }

            System.out.println();
            printSuggestions(suggestions);
            System.out.println();

            String common = longestCommonPrefix(suggestions);
            String filled = buildCompletion(partial, common);
            currentPrefill = filled;
        }
    }

    /**
     * Builds the completed input line by appending the suggestion to the partial input.
     */
    private String buildCompletion(String partial, String suggestion) {
        if (partial == null || partial.isEmpty()) {
            return suggestion + " ";
        }

        String trimmed = partial.trim();
        if (trimmed.isEmpty()) {
            return suggestion + " ";
        }

        boolean endsWithSpace = partial.endsWith(" ");

        if (endsWithSpace) {
            // User finished a token, append suggestion as new token
            return trimmed + " " + suggestion + " ";
        }

        // User was mid-token — replace the last partial token with the suggestion
        String[] tokens = trimmed.split("\\s+");
        String lastToken = tokens[tokens.length - 1];

        if (suggestion.toUpperCase().startsWith(lastToken.toUpperCase())) {
            // Build the line up to (but not including) the last token, then add suggestion
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tokens.length - 1; i++) {
                sb.append(tokens[i]).append(" ");
            }
            sb.append(suggestion).append(" ");
            return sb.toString();
        }

        // Fallback: just append
        return trimmed + " " + suggestion + " ";
    }

    /**
     * Prints suggestions in a clean formatted box.
     */
    private void printSuggestions(List<String> suggestions) {
        System.out.println("  ┌─ Suggestions ──────────────────────────");
        // Print in rows of up to 4 items
        StringBuilder row = new StringBuilder("  │  ");
        int col = 0;
        for (String s : suggestions) {
            row.append(String.format("%-18s", s));
            col++;
            if (col >= 4) {
                System.out.println(row.toString());
                row = new StringBuilder("  │  ");
                col = 0;
            }
        }
        if (col > 0) {
            System.out.println(row.toString());
        }
        System.out.println("  └──────────────────────────────────────────");
    }

    /**
     * Finds the longest common prefix among suggestions (case-insensitive,
     * preserves case of the first suggestion).
     */
    private String longestCommonPrefix(List<String> strings) {
        if (strings == null || strings.isEmpty()) return "";
        String first = strings.get(0);
        int prefixLen = first.length();

        for (int i = 1; i < strings.size(); i++) {
            String s = strings.get(i);
            prefixLen = Math.min(prefixLen, s.length());
            for (int j = 0; j < prefixLen; j++) {
                if (Character.toUpperCase(first.charAt(j)) != Character.toUpperCase(s.charAt(j))) {
                    prefixLen = j;
                    break;
                }
            }
        }

        return first.substring(0, prefixLen);
    }
}
