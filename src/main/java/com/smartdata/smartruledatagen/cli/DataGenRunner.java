package com.smartdata.smartruledatagen.cli;

import com.smartdata.smartruledatagen.config.GeneratorConfigLoader;
import com.smartdata.smartruledatagen.generator.GenericDataGenerator;
import com.smartdata.smartruledatagen.model.rules.GeneratorDefinition;
import com.smartdata.smartruledatagen.service.database.JdbcExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class DataGenRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataGenRunner.class);

    private final GeneratorConfigLoader configLoader;
    private final GenericDataGenerator genericDataGenerator;
    private final JdbcExecutor jdbcExecutor;

    public DataGenRunner(GeneratorConfigLoader configLoader, GenericDataGenerator genericDataGenerator, JdbcExecutor jdbcExecutor) {
        this.configLoader = configLoader;
        this.genericDataGenerator = genericDataGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public void run(String... args) throws Exception {
        String auto = System.getProperty("autogen");
        if (auto != null && !auto.isEmpty()) {
            Map<String, GeneratorDefinition> generators = configLoader.getRuleConfig().getGenerators();
            List<String> generatorKeys = generators.keySet().stream().sorted().collect(Collectors.toList());
            String[] parts = auto.split(",");
            int choiceIdx = Integer.parseInt(parts[0]) - 1;
            if (choiceIdx < 0 || choiceIdx >= generatorKeys.size()) {
                System.err.println("无效的选择。");
                System.exit(1);
                return;
            }
            String selectedGeneratorKey = generatorKeys.get(choiceIdx);
            GeneratorDefinition generatorDefinition = generators.get(selectedGeneratorKey);
            int count = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 10;
            Map<String, Object> params = new HashMap<>();
            if (parts.length > 2 && !parts[2].isEmpty()) {
                try {
                    params.put("baseDate", LocalDate.parse(parts[2]));
                } catch (DateTimeParseException ignore) {}
            }
            if (parts.length > 3 && !parts[3].isEmpty()) {
                params.put("bigRegionCode", parts[3]);
            }
            String executeChoice = parts.length > 4 ? parts[4] : "no";
            log.info("开始生成 {} 条数据，使用生成器定义: {}", count, selectedGeneratorKey);
            try {
                List<String> sqls = genericDataGenerator.generateSqls(generatorDefinition, count, params);
                log.info("数据生成完成，共 {} 条 SQL 语句。", sqls.size());
                // 导出到文件（如果指定或使用默认路径）
                String exportPathProp = System.getProperty("exportSql");
                String exportFileName = selectedGeneratorKey + "_" + System.currentTimeMillis() + ".sql";
                Path exportPath = Path.of(exportPathProp != null && !exportPathProp.isEmpty()
                        ? exportPathProp
                        : "target/generated-sql/" + exportFileName);
                try {
                    Files.createDirectories(exportPath.getParent());
                    String content = sqls.stream().map(s -> s + ";\n").collect(Collectors.joining());
                    Files.writeString(exportPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("已导出生成的SQL到文件: " + exportPath.toAbsolutePath());
                } catch (Exception e) {
                    log.error("导出SQL到文件失败: {}", e.getMessage());
                }
                if ("yes".equalsIgnoreCase(executeChoice)) {
                    String dbKey = generatorDefinition.getDbKey();
                    System.out.println("准备将数据插入到数据库: " + dbKey);
                    jdbcExecutor.executeBatchSql(dbKey, sqls);
                    System.out.println("数据入库完成！");
                } else {
                    sqls.forEach(System.out::println);
                }
            } catch (Exception e) {
                log.error("数据生成或入库失败: ", e);
                System.err.println("发生错误: " + e.getMessage());
            } finally {
                System.exit(0);
            }
            return;
        }

        String cliInteractive = System.getProperty("cli.interactive");
        if (!"true".equalsIgnoreCase(cliInteractive)) {
            log.info("CLI interactive mode disabled. Web server is running.");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("欢迎使用智能测试数据生成工具！");
        System.out.println("请选择要生成数据的类型：");

        Map<String, GeneratorDefinition> generators = configLoader.getRuleConfig().getGenerators();
        if (generators.isEmpty()) {
            System.err.println("未找到任何数据生成器定义。请检查 generator-rules.yml 文件。");
            return;
        }

        List<String> generatorKeys = generators.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < generatorKeys.size(); i++) {
            System.out.println((i + 1) + ". " + generatorKeys.get(i) + " (Table: " + generators.get(generatorKeys.get(i)).getTableName() + ")");
        }

        System.out.print("请输入序号选择 (例如 1): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.err.println("无效输入，请输入数字。");
            return;
        }


        if (choice < 0 || choice >= generatorKeys.size()) {
            System.err.println("无效的选择。");
            return;
        }

        String selectedGeneratorKey = generatorKeys.get(choice);
        GeneratorDefinition generatorDefinition = generators.get(selectedGeneratorKey);

        System.out.print("请输入要生成的数据条数 (例如 10): ");
        int count;
        try {
            count = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.err.println("无效输入，请输入数字。");
            return;
        }


        Map<String, Object> params = new HashMap<>();
        System.out.print("请输入基准日期 (yyyy-MM-dd, 留空则使用默认/当前日期): ");
        String dateStr = scanner.nextLine();
        if (!dateStr.isEmpty()) {
            try {
                params.put("baseDate", LocalDate.parse(dateStr));
            } catch (DateTimeParseException e) {
                System.err.println("日期格式错误，将使用默认日期。");
            }
        }

        System.out.print("请输入要指定的大区代码 (留空则随机): ");
        String bigRegionCode = scanner.nextLine();
        if (!bigRegionCode.isEmpty()) {
            params.put("bigRegionCode", bigRegionCode);
        }

        log.info("开始生成 {} 条数据，使用生成器定义: {}", count, selectedGeneratorKey);
        try {
            List<String> sqls = genericDataGenerator.generateSqls(generatorDefinition, count, params);
            log.info("数据生成完成，共 {} 条 SQL 语句。", sqls.size());

            System.out.print("是否将生成的SQL语句执行入库？(yes/no): ");
            String executeChoice = scanner.nextLine();

            if ("yes".equalsIgnoreCase(executeChoice)) {
                String dbKey = generatorDefinition.getDbKey();
                System.out.println("准备将数据插入到数据库: " + dbKey);
                jdbcExecutor.executeBatchSql(dbKey, sqls);
                System.out.println("数据入库完成！");
            } else {
                System.out.println("生成的SQL语句已打印到日志或控制台，未执行入库。");
                sqls.forEach(System.out::println); // 打印SQL到控制台
            }
        } catch (Exception e) {
            log.error("数据生成或入库失败: ", e);
            System.err.println("发生错误: " + e.getMessage());
        } finally {
            scanner.close();
            // System.exit(0); // 退出应用 - 在交互模式下可能需要，但为了安全起见移除，让Spring容器管理生命周期
        }
    }
}
