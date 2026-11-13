import java.util.*;

public class AhoCorasick {

    public static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        TrieNode failLink = null;
        List<String> output = new ArrayList<>();
        int depth = 0;
    }

    public static class Match {
        public final String pattern;
        public final int endIndex;

        public Match(String pattern, int endIndex) {
            this.pattern = pattern;
            this.endIndex = endIndex;
        }

        @Override
        public String toString() {
            return "Match{pattern='" + pattern + "', endIndex=" + endIndex + "}";
        }
    }

    private final TrieNode root;

    public AhoCorasick() {
        this.root = new TrieNode();
    }

    public void insert(String pattern) {
        if (pattern == null || pattern.isEmpty()) return;
        TrieNode current = root;
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            TrieNode next = current.children.get(ch);
            if (next == null) {
                next = new TrieNode();
                next.depth = current.depth + 1;
                current.children.put(ch, next);
            }
            current = next;
        }
        current.output.add(pattern);
    }

    public void buildFailureLinks() {
        Queue<TrieNode> queue = new LinkedList<>();

        // Level 1 nodes fail back to root
        for (TrieNode child : root.children.values()) {
            child.failLink = root;
            queue.offer(child);
        }

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char ch = entry.getKey();
                TrieNode child = entry.getValue();
                queue.offer(child);

                // Find failure link for child
                TrieNode fallback = current.failLink;
                while (fallback != null && !fallback.children.containsKey(ch)) {
                    fallback = fallback.failLink;
                }

                if (fallback != null) {
                    child.failLink = fallback.children.get(ch);
                } else {
                    child.failLink = root;
                }

                // Merge output lists (Output Links equivalent)
                if (child.failLink != null) {
                    child.output.addAll(child.failLink.output);
                }
            }
        }
    }

    public List<Match> search(String text) {
        List<Match> results = new ArrayList<>();
        if (text == null || text.isEmpty()) return results;

        TrieNode current = root;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            while (current != null && !current.children.containsKey(ch)) {
                current = current.failLink;
            }

            if (current == null) {
                current = root;
            } else {
                current = current.children.get(ch);
            }

            for (String pattern : current.output) {
                results.add(new Match(pattern, i));
            }
        }
        return results;
    }


    public static void main(String[] args) {
        int[] patternCounts = {10, 100, 1000};
        int[] textLengths = {1000, 10000, 100000};
        int runs = 5;

        Random random = new Random(42);

        System.out.println("==================================================================================");
        System.out.println(" AHO-CORASICK BENCHMARK RESULTS (Averaged over 5 runs using System.nanoTime())");
        System.out.println("==================================================================================");
        System.out.printf("%-10s | %-12s | %-20s | %-20s%n", "Patterns", "Text Length", "Avg Build Time (ms)", "Avg Search Time (ms)");
        System.out.println("----------------------------------------------------------------------------------");

        for (int pCount : patternCounts) {
            // Generate random patterns of length 4 to 8
            List<String> patterns = new ArrayList<>();
            for (int i = 0; i < pCount; i++) {
                StringBuilder sb = new StringBuilder();
                int len = 4 + random.nextInt(5);
                for (int j = 0; j < len; j++) {
                    sb.append((char) ('a' + random.nextInt(4))); // restricted alphabet for dense matching
                }
                patterns.add(sb.toString());
            }

            for (int tLen : textLengths) {
                // Generate random text
                StringBuilder textBuilder = new StringBuilder();
                for (int i = 0; i < tLen; i++) {
                    textBuilder.append((char) ('a' + random.nextInt(4)));
                }
                String text = textBuilder.toString();

                long totalBuildNs = 0;
                long totalSearchNs = 0;

                for (int r = 0; r < runs; r++) {
                    long startBuild = System.nanoTime();
                    AhoCorasick ac = new AhoCorasick();
                    for (String pat : patterns) {
                        ac.insert(pat);
                    }
                    ac.buildFailureLinks();
                    long endBuild = System.nanoTime();
                    totalBuildNs += (endBuild - startBuild);

                    long startSearch = System.nanoTime();
                    List<Match> matches = ac.search(text);
                    long endSearch = System.nanoTime();
                    totalSearchNs += (endSearch - startSearch);
                }

                double avgBuildMs = (totalBuildNs / (double) runs) / 1_000_000.0;
                double avgSearchMs = (totalSearchNs / (double) runs) / 1_000_000.0;

                System.out.printf("%-10d | %-12d | %-20.4f | %-20.4f%n", pCount, tLen, avgBuildMs, avgSearchMs);
            }
        }
        System.out.println("==================================================================================");
    }
}