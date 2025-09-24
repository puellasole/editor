package editor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Controller
public class EditorController {
    
    @GetMapping("/")
    public String editor() {
        return "editor";
    }
    
    @PostMapping("/execute")
    @ResponseBody
    public ExecutionResult executeJavaCode(@RequestBody CodeRequest request) {
        return JavaExecutor.execute(request.getCode());
    }
    
    public static class CodeRequest {
        private String code;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
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
}

class JavaExecutor {
    
    public static EditorController.ExecutionResult execute(String code) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            // Перехватываем System.out и System.err
            PrintStream customOut = new PrintStream(outputStream, true);
            System.setOut(customOut);
            System.setErr(customOut);
            
            // Нормализуем код
            String normalizedCode = normalizeJavaCode(code);
            String className = extractClassName(normalizedCode);
            
            // Компилируем и выполняем
            EditorController.ExecutionResult result = compileAndRunJava(normalizedCode, className);
            
            // Добавляем перехваченный вывод к результату
            if (result.isSuccess()) {
                String capturedOutput = outputStream.toString("UTF-8");
                if (!capturedOutput.isEmpty()) {
                    result.setOutput("✅ Программа выполнена успешно!\n\n" + capturedOutput);
                } else {
                    result.setOutput("✅ Программа выполнена успешно, но вывод отсутствует.");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            return new EditorController.ExecutionResult(false, "", 
                "❌ Ошибка: " + e.getMessage());
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
    
    private static String normalizeJavaCode(String code) {
        code = code.trim();
        
        // Если код уже содержит полный класс с main методом
        if (code.contains("class") && code.contains("main(String[] args)")) {
            return code;
        }
        
        // Если есть класс но нет main метода
        if (code.contains("class") && !code.contains("main(String[] args)")) {
            // Добавляем метод main
            int lastBrace = code.lastIndexOf('}');
            if (lastBrace != -1) {
                String mainMethod = 
                    "\n    public static void main(String[] args) {\n" +
                    "        // Автоматически добавленный main метод\n" +
                    "        new " + extractClassName(code) + "().run();\n" +
                    "    }\n" +
                    "    \n" +
                    "    public void run() {\n" +
                    "        // Ваш код будет выполнен здесь\n" +
                    "    }";
                code = code.substring(0, lastBrace) + mainMethod + "\n}";
            }
            return code;
        }
        
        // Для простого кода без класса - создаем полную структуру
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
                "❌ Java компилятор не доступен! Убедитесь, что используется JDK.");
        }
        
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        try {
            // Создаем временный файл для компиляции
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "java_editor");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            File sourceFile = new File(tempDir, className + ".java");
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }
            
            // Компилируем
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
            
            List<String> options = List.of("-d", tempDir.getAbsolutePath());
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits);
            
            boolean success = task.call();
            fileManager.close();
            
            // Проверяем ошибки компиляции
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
            
            // Загружаем и выполняем класс
            URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toURI().toURL()});
            Class<?> loadedClass = classLoader.loadClass(className);
            
            // Вызываем main метод
            java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
            
            classLoader.close();
            
            return new EditorController.ExecutionResult(true, "", "");
            
        } catch (Exception e) {
            return new EditorController.ExecutionResult(false, "", 
                "❌ Ошибка выполнения:\n" + e.getMessage());
        }
    }
    
    // Вспомогательные классы для компиляции в памяти (альтернативный вариант)
    static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;
        
        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}