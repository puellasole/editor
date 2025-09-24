package editor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class EditorController {

	// Хранилище задач БЕЗ готовых решений
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
    
    // Тест-кейсы остаются без изменений
    static final Map<Integer, List<TestCase>> TEST_CASES = new HashMap<>();
    static {
        TEST_CASES.put(1, Arrays.asList(
            new TestCase("5\n1 2 3 4 5", "15", false),
            new TestCase("3\n10 20 30", "60", false),
            new TestCase("1\n100", "100", false),
            new TestCase("4\n0 0 0 0", "0", true)
        ));
        
        TEST_CASES.put(2, Arrays.asList(
            new TestCase("5\n1 5 3 2 4", "5", false),
            new TestCase("3\n-10 -20 -5", "-5", false),
            new TestCase("1\n42", "42", false),
            new TestCase("6\n10 20 30 25 15 5", "30", true)
        ));
        
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
        // Режим обычного редактора (как в первой программе)
        return JavaExecutor.executeInteractive(request.getCode());
    }

    @PostMapping("/test")
    @ResponseBody
    public TestExecutionResult testSolution(@RequestBody TestRequest request) {
        // Режим тестирования (как во второй программе)
        return JavaTester.testSolution(request.getCode(), request.getProblemId());
    }
    
    // DTO классы (остаются без изменений)
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
        
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        public boolean isHidden() { return hidden; }
        public void setHidden(boolean hidden) { this.hidden = hidden; }
    }
}



class JavaExecutor {
    
    // РЕЖИМ 1: Для обычного редактора (красивый вывод с эмодзи)
    public static EditorController.ExecutionResult executeInteractive(String code) {
        editor.EditorController.ExecutionResult result = executeInternal(code, true);
        
        if (result.isSuccess()) {
            String output = result.getOutput();
            if (output != null && !output.trim().isEmpty()) {
                result.setOutput("✅ Программа выполнена успешно!\n\n" + output);
            } else {
                result.setOutput("✅ Программа выполнена успешно, но вывод отсутствует.");
            }
        } else {
            result.setError("❌ " + result.getError());
        }
        
        return result;
    }
    
    // РЕЖИМ 2: Для тестирования (чистый вывод без украшений)
    public static EditorController.ExecutionResult executeForTesting(String code) {
        return executeInternal(code, false);
    }
    
    // Общая внутренняя логика выполнения
    private static EditorController.ExecutionResult executeInternal(String code, boolean interactive) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            // Перехватываем System.out и System.err
            PrintStream customOut = new PrintStream(outputStream, true, "UTF-8");
            System.setOut(customOut);
            System.setErr(customOut);
            
            // Нормализуем код (БЕЗ отладочного вывода!)
            String normalizedCode = normalizeJavaCode(code);
            String className = extractClassName(normalizedCode);
            
            // Компилируем и выполняем
            EditorController.ExecutionResult result = compileAndRunJava(normalizedCode, className);
            
            // Добавляем перехваченный вывод
            String capturedOutput = outputStream.toString("UTF-8");
            if (result.isSuccess()) {
                result.setOutput(capturedOutput);
            } else {
                result.setError(result.getError() + (interactive ? "\n\nВывод программы:\n" + capturedOutput : ""));
            }
            
            return result;
            
        } catch (Exception e) {
            return new EditorController.ExecutionResult(false, "", 
                "Ошибка выполнения: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        } finally {
            // Восстанавливаем оригинальные потоки
            System.setOut(originalOut);
            System.setErr(originalErr);
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Упрощенная нормализация кода
    private static String normalizeJavaCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "public class Main {\n    public static void main(String[] args) {\n        // Пустая программа\n    }\n}";
        }
        
        code = code.trim();
        
        // Если код уже содержит класс с main методом - возвращаем как есть
        if (code.contains("class") && code.contains("main(String[] args)")) {
            return code;
        }
        
        // Для простого кода без класса - создаем полную структуру
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        " + code.replace("\n", "\n        ") + "\n" +
               "    }\n" +
               "}";
    }
    
    // Метод извлечения имени класса
    private static String extractClassName(String code) {
        try {
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
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return "Main";
    }
    
    // Метод компиляции и выполнения
    private static EditorController.ExecutionResult compileAndRunJava(String code, String className) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return new EditorController.ExecutionResult(false, "", 
                    "Java компилятор не доступен! Убедитесь, что используется JDK.");
            }
            
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            
            // Создаем временную директорию
            File tempDir = Files.createTempDirectory("java_editor").toFile();
            tempDir.deleteOnExit();
            
            File sourceFile = new File(tempDir, className + ".java");
            sourceFile.deleteOnExit();
            
            // Записываем код в файл
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }
            
            // Компилируем
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
            
            List<String> options = Arrays.asList("-d", tempDir.getAbsolutePath());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
            
            boolean success = task.call();
            fileManager.close();
            
            if (!success) {
                StringBuilder errorMsg = new StringBuilder("Ошибки компиляции:\n");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    errorMsg.append("Строка ")
                           .append(diagnostic.getLineNumber())
                           .append(": ")
                           .append(diagnostic.getMessage(null))
                           .append("\n");
                }
                return new EditorController.ExecutionResult(false, "", errorMsg.toString());
            }
            
            // Загружаем и выполняем класс
            URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toURI().toURL()});
            Class<?> loadedClass = classLoader.loadClass(className);
            
            // Вызываем main метод
            Method mainMethod = loadedClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
            
            classLoader.close();
            
            return new EditorController.ExecutionResult(true, "", "");
            
        } catch (Exception e) {
            String errorMessage = "Ошибка выполнения: ";
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            errorMessage += cause.getMessage();
            return new EditorController.ExecutionResult(false, "", errorMessage);
        }
    }
}

class JavaTester {
    
    public static EditorController.TestExecutionResult testSolution(String code, int problemId) {
        List<EditorController.TestCase> testCases = EditorController.TEST_CASES.get(problemId);
        if (testCases == null) {
            return new EditorController.TestExecutionResult(false, new ArrayList<>(), 0, 0, 
                "Тесты для задачи не найдены");
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
        
        String summary = String.format("Пройдено %d из %d тестов", passedCount, testCases.size());
        boolean allPassed = passedCount == testCases.size();
        
        return new EditorController.TestExecutionResult(allPassed, testResults, 
            passedCount, testCases.size(), summary);
    }
    
    private static EditorController.TestResult runSingleTest(String code, EditorController.TestCase testCase) {
        try {
            // Инжектируем входные данные ПРАВИЛЬНО
            String testCode = injectInputIntoCode(code, testCase.getInput());
            
            // Выполняем код
            EditorController.ExecutionResult result = JavaExecutor.executeForTesting(testCode);
            
            if (!result.isSuccess()) {
                return new EditorController.TestResult(
                    testCase.isHidden() ? "***" : testCase.getInput(),
                    testCase.isHidden() ? "***" : testCase.getExpectedOutput(),
                    "Ошибка: " + result.getError(),
                    false,
                    testCase.isHidden()
                );
            }
            
            // Сравниваем вывод
            String actualOutput = normalizeOutput(result.getOutput());
            String expectedOutput = normalizeOutput(testCase.getExpectedOutput());
            
            boolean passed = actualOutput.trim().equals(expectedOutput.trim());
            
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
        // Правильное экранирование
        String escapedInput = input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r");
        
        // Создаем код для подмены System.in
        String inputCode = 
            "java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(\"" + 
            escapedInput + "\".getBytes(java.nio.charset.StandardCharsets.UTF_8));\n" +
            "System.setIn(inputStream);";
        
        // Если код уже содержит main метод, вставляем в начало
        if (code.contains("public static void main(String[] args) {")) {
            int mainStart = code.indexOf("public static void main(String[] args) {") + 42;
            return code.substring(0, mainStart) + "\n        " + inputCode + code.substring(mainStart);
        }
        
        // Если нет main метода, создаем полную структуру
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        " + inputCode + "\n" +
               "        " + code.replace("\n", "\n        ") + "\n" +
               "    }\n" +
               "}";
    }
    
    private static String normalizeOutput(String output) {
        if (output == null) return "";
        return output.replaceAll("\\r\\n", "\n")
                    .replaceAll("\\r", "\n")
                    .trim();
    }
}