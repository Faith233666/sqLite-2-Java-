import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextLogAnalyzer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9]+|[\\u4e00-\\u9fff]+");

    public record WordCount(String word, int count) {}

    public record AnalysisResult(
            Path filePath,
            int lineCount,
            int charCount,
            List<WordCount> wordCounts
    ) {}

    public static AnalysisResult analyze(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        int lineCount = countLines(content);
        int charCount = content.length();
        List<WordCount> wordCounts = countWords(content);
        return new AnalysisResult(filePath, lineCount, charCount, wordCounts);
    }

    private static int countLines(String content) {
        if (content.isEmpty()) {
            return 0;
        }
        return content.split("\n", -1).length;
    }

    private static List<WordCount> countWords(String content) {
        Map<String, Integer> frequency = new HashMap<>();
        Matcher matcher = TOKEN_PATTERN.matcher(content);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.matches("\\d+")) {
                continue;
            }
            String key = token.matches("[a-zA-Z0-9]+")
                    ? token.toLowerCase(Locale.ROOT)
                    : token;
            frequency.merge(key, 1, Integer::sum);
        }

        List<WordCount> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            result.add(new WordCount(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator
                .comparingInt(WordCount::count).reversed()
                .thenComparing(WordCount::word));
        return result;
    }

    public static void printReport(AnalysisResult result) {
        System.out.println("=== 文本/日志分析结果 ===");
        System.out.println("文件: " + result.filePath());
        System.out.println("行数: " + result.lineCount());
        System.out.println("字符数: " + result.charCount());
        System.out.println();
        System.out.println("高频词汇（按出现次数倒序）:");

        if (result.wordCounts().isEmpty()) {
            System.out.println("  （未识别到词汇）");
            return;
        }

        for (WordCount wordCount : result.wordCounts()) {
            System.out.printf("  %-20s %d%n", wordCount.word(), wordCount.count());
        }
    }
}
