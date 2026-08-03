import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            Path defaultFile = Path.of("sample.log");
            if (defaultFile.toFile().exists()) {
                args = new String[]{"sample.log"};
                System.out.println("未指定文件路径，使用默认文件: sample.log");
                System.out.println("提示: 可在 Run Configuration 的 Program arguments 中指定其他文件");
                System.out.println();
            } else {
                System.err.println("用法: java Main <文件路径>");
                System.err.println("示例: java Main sample.log");
                System.exit(1);
            }
        }

        Path filePath = Path.of(args[0]);
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".log") && !fileName.endsWith(".txt")) {
            System.err.println("仅支持 .log 或 .txt 文件");
            System.exit(1);
        }

        if (!filePath.toFile().exists()) {
            System.err.println("文件不存在: " + filePath);
            System.exit(1);
        }

        try {
            TextLogAnalyzer.AnalysisResult result = TextLogAnalyzer.analyze(filePath);
            TextLogAnalyzer.printReport(result);
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
            System.exit(1);
        }
    }
}
