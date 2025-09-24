package editor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Controller
public class EditorController {
    
    // Хранилище задач (вместо БД)
    private static final List<Problem> PROBLEMS = Arrays.asList(
        new Problem(1, "Сумма элементов массива", 
            "Напишите программу, которая вычисляет сумму элементов массива.\n\n" +
            "Входные данные: первое число - размер массива, затем элементы массива\n" +
            "Выходные данные: сумма элементов\n\n" +
            "Пример:\nВход: 5\n1 2 3 4 5\nВыход: 15",
            "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        // Ваш код здесь\n    }\n}"),
        
        new Problem(2, "Поиск максимального элемента", 
            "Найдите максимальный элемент в массиве.\n\n" +
            "Входные данные: первое число - размер массива, затем элементы массива\n" +
            "Выходные данные: максимальный элемент\n\n" +
            "Пример:\nВход: 5\n1 5 3 2 4\nВыход: 5",
            "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        // Ваш код здесь\n    }\n}"),
        
        new Problem(3, "Проверка палиндрома", 
            "Проверьте, является ли строка палиндромом.\n\n" +
            "Входные данные: строка\n" +
            "Выходные данные: 'YES' если палиндром, 'NO' если нет\n\n" +
            "Пример:\nВход: radar\nВыход: YES",
            "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        // Ваш код здесь\n    }\n}")
    );
    
    // Тест-кейсы для каждой задачи
    static final Map<Integer, List<TestCase>> TEST_CASES = new HashMap<>();
    static {
        // Тесты для задачи 1 (Сумма массива)
        TEST_CASES.put(1, Arrays.asList(
            new TestCase("5\n1 2 3 4 5", "15", false),
            new TestCase("3\n10 20 30", "60", false),
            new TestCase("1\n100", "100", false),
            new TestCase("4\n0 0 0 0", "0", true)
        ));
        
        // Тесты для задачи 2 (Максимальный элемент)
        TEST_CASES.put(2, Arrays.asList(
            new TestCase("5\n1 5 3 2 4", "5", false),
            new TestCase("3\n-10 -20 -5", "-5", false),
            new TestCase("1\n42", "42", false),
            new TestCase("6\n10 20 30 25 15 5", "30", true)
        ));
        
        // Тесты для задачи 3 (Палиндром)
        TEST_CASES.put(3, Arrays.asList(
            new TestCase("radar", "YES", false),
            new TestCase("hello", "NO", false),
            new TestCase("a", "YES", false),
            new TestCase("A man a plan a canal Panama", "YES", true)
        ));
    }
    
    @GetMapping("/")
    public String editor() {
        return "editor";
    }
    
    @GetMapping("/problems")
    @ResponseBody
    public List<Problem> getProblems() {
        return PROBLEMS;
    }
    
    @GetMapping("/problem/{id}")
    @ResponseBody
    public Problem getProblem(@PathVariable int id) {
        return PROBLEMS.stream()
            .filter(p -> p.getId() == id)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Problem not found"));
    }
    
    @PostMapping("/execute")
    @ResponseBody
    public ExecutionResult executeJavaCode(@RequestBody CodeRequest request) {
        return JavaExecutor.execute(request.getCode());
    }
    
    @PostMapping("/test")
    @ResponseBody
    public TestExecutionResult testSolution(@RequestBody TestRequest request) {
        return JavaTester.testSolution(request.getCode(), request.getProblemId());
    }
    
    // DTO классы
    public static class CodeRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
    
    public static class TestRequest {
        private String code;
        private int problemId;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public int getProblemId() { return problemId; }
        public void setProblemId(int problemId) { this.problemId = problemId; }
    }
    
    public static class ExecutionResult {
        private boolean success;
        private String output;
        private String error;
        
        public ExecutionResult() {}
        public ExecutionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class TestExecutionResult {
        private boolean success;
        private List<TestResult> testResults;
        private int passedTests;
        private int totalTests;
        private String summary;
        
        public TestExecutionResult() {}
        public TestExecutionResult(boolean success, List<TestResult> testResults, 
                                 int passedTests, int totalTests, String summary) {
            this.success = success;
            this.testResults = testResults;
            this.passedTests = passedTests;
            this.totalTests = totalTests;
            this.summary = summary;
        }
        
        // getters/setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<TestResult> getTestResults() { return testResults; }
        public void setTestResults(List<TestResult> testResults) { this.testResults = testResults; }
        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
    
    public static class TestResult {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
        private boolean hidden;
        
        public TestResult() {}
        public TestResult(String input, String expectedOutput, String actualOutput, 
                         boolean passed, boolean hidden) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.passed = passed;
            this.hidden = hidden;
        }
        
        // getters/setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        public String getActualOutput() { return actualOutput; }
        public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public boolean isHidden() { return hidden; }
        public void setHidden(boolean hidden) { this.hidden = hidden; }
    }
    
    public static class Problem {
        private int id;
        private String title;
        private String description;
        private String initialCode;
        
        public Problem() {}
        public Problem(int id, String title, String description, String initialCode) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.initialCode = initialCode;
        }
        
        // getters/setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getInitialCode() { return initialCode; }
        public void setInitialCode(String initialCode) { this.initialCode = initialCode; }
    }
    
    public static class TestCase {
        private String input;
        private String expectedOutput;
        private boolean hidden;
        
        public TestCase() {}
        public TestCase(String input, String expectedOutput, boolean hidden) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.hidden = hidden;
        }
        
        // getters/setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        public boolean isHidden() { return hidden; }
        public void setHidden(boolean hidden) { this.hidden = hidden; }
    }
}

class JavaTester {
    
    public static EditorController.TestExecutionResult testSolution(String code, int problemId) {
        List<EditorController.TestCase> testCases = EditorController.TEST_CASES.get(problemId);
        if (testCases == null) {
            return new EditorController.TestExecutionResult(false, new ArrayList<>(), 0, 0, 
                "❌ Тесты для задачи не найдены");
        }
        
        List<EditorController.TestResult> testResults = new ArrayList<>();
        int passedCount = 0;
        
        for (EditorController.TestCase testCase : testCases) {
            EditorController.TestResult testResult = runSingleTest(code, testCase);
            testResults.add(testResult);
            if (testResult.isPassed()) {
                passedCount++;
            }
        }
        
        String summary = String.format("✅ Пройдено %d из %d тестов", passedCount, testCases.size());
        boolean allPassed = passedCount == testCases.size();
        
        return new EditorController.TestExecutionResult(allPassed, testResults, 
            passedCount, testCases.size(), summary);
    }
    
    private static EditorController.TestResult runSingleTest(String code, EditorController.TestCase testCase) {
        try {
            // Создаем код с входными данными
            String testCode = injectInputIntoCode(code, testCase.getInput());
            
            // Выполняем код
            EditorController.ExecutionResult result = JavaExecutor.execute(testCode);
            
            if (!result.isSuccess()) {
                return new EditorController.TestResult(
                    testCase.isHidden() ? "***" : testCase.getInput(),
                    testCase.isHidden() ? "***" : testCase.getExpectedOutput(),
                    "Ошибка выполнения: " + result.getError(),
                    false,
                    testCase.isHidden()
                );
            }
            
            // Нормализуем вывод для сравнения
            String actualOutput = normalizeOutput(result.getOutput());
            String expectedOutput = normalizeOutput(testCase.getExpectedOutput());
            
            boolean passed = actualOutput.equals(expectedOutput);
            
            return new EditorController.TestResult(
                testCase.isHidden() ? "***" : testCase.getInput(),
                testCase.isHidden() ? "***" : testCase.getExpectedOutput(),
                testCase.isHidden() && !passed ? "***" : actualOutput,
                passed,
                testCase.isHidden()
            );
            
        } catch (Exception e) {
            return new EditorController.TestResult(
                testCase.isHidden() ? "***" : testCase.getInput(),
                testCase.isHidden() ? "***" : testCase.getExpectedOutput(),
                "Ошибка тестирования: " + e.getMessage(),
                false,
                testCase.isHidden()
            );
        }
    }
    
    private static String injectInputIntoCode(String code, String input) {
        // Заменяем System.in на наш ввод
        String inputReplacement = 
            "java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(\"" +
            input.replace("\n", "\\n").replace("\"", "\\\"") +
            "\".getBytes());\n" +
            "System.setIn(bais);";
        
        if (code.contains("public static void main")) {
            // Вставляем после начала main метода
            return code.replace("public static void main(String[] args) {", 
                "public static void main(String[] args) {\n" + inputReplacement);
        } else {
            return code;
        }
    }
    
    private static String normalizeOutput(String output) {
        return output.trim()
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\s+", " ")
            .trim();
    }
}

class JavaExecutor {
    
    public static EditorController.ExecutionResult execute(String code) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            PrintStream customOut = new PrintStream(outputStream, true);
            System.setOut(customOut);
            System.setErr(customOut);
            
            String normalizedCode = normalizeJavaCode(code);
            String className = extractClassName(normalizedCode);
            
            EditorController.ExecutionResult result = compileAndRunJava(normalizedCode, className);
            
            if (result.isSuccess()) {
                String capturedOutput = outputStream.toString("UTF-8");
                if (!capturedOutput.isEmpty()) {
                    result.setOutput(capturedOutput);
                } else {
                    result.setOutput("Программа выполнена без вывода");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            return new EditorController.ExecutionResult(false, "", 
                "❌ Ошибка: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static String normalizeJavaCode(String code) {
        code = code.trim();
        
        if (code.contains("class") && code.contains("main(String[] args)")) {
            return code;
        }
        
        if (code.contains("class") && !code.contains("main(String[] args)")) {
            int lastBrace = code.lastIndexOf('}');
            if (lastBrace != -1) {
                String mainMethod = 
                    "\n    public static void main(String[] args) {\n" +
                    "        new " + extractClassName(code) + "().run();\n" +
                    "    }\n" +
                    "    \n" +
                    "    public void run() {\n" +
                    "    }";
                code = code.substring(0, lastBrace) + mainMethod + "\n}";
            }
            return code;
        }
        
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        " + code.replace("\n", "\n        ") + "\n" +
               "    }\n" +
               "}";
    }
    
    private static String extractClassName(String code) {
        if (code.contains("public class ")) {
            int start = code.indexOf("public class ") + 13;
            int end = code.indexOf("{", start);
            if (end > start) {
                String className = code.substring(start, end).trim();
                if (className.contains(" ")) {
                    className = className.substring(0, className.indexOf(" "));
                }
                return className.trim();
            }
        } else if (code.contains("class ")) {
            int start = code.indexOf("class ") + 6;
            int end = code.indexOf("{", start);
            if (end > start) {
                String className = code.substring(start, end).trim();
                if (className.contains(" ")) {
                    className = className.substring(0, className.indexOf(" "));
                }
                return className.trim();
            }
        }
        return "Main";
    }
    
    private static EditorController.ExecutionResult compileAndRunJava(String code, String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new EditorController.ExecutionResult(false, "", 
                "❌ Java компилятор не доступен!");
        }
        
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "java_editor");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            File sourceFile = new File(tempDir, className + ".java");
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }
            
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
            
            List<String> options = List.of("-d", tempDir.getAbsolutePath());
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits);
            
            boolean success = task.call();
            fileManager.close();
            
            if (!success) {
                StringBuilder errorMsg = new StringBuilder("❌ Ошибки компиляции:\n");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    errorMsg.append("Строка ")
                           .append(diagnostic.getLineNumber())
                           .append(": ")
                           .append(diagnostic.getMessage(null))
                           .append("\n");
                }
                return new EditorController.ExecutionResult(false, "", errorMsg.toString());
            }
            
            URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toURI().toURL()});
            Class<?> loadedClass = classLoader.loadClass(className);
            
            java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
            
            classLoader.close();
            
            return new EditorController.ExecutionResult(true, "", "");
            
        } catch (Exception e) {
            return new EditorController.ExecutionResult(false, "", 
                "❌ Ошибка выполнения:\n" + e.getMessage());
        }
    }
}