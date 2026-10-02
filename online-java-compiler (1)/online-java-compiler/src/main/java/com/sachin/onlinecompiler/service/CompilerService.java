package com.sachin.onlinecompiler.service;

import com.sachin.onlinecompiler.dto.CompileResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CompilerService {
    private static final int MAX_SOURCE_LENGTH = 50_000;
    private static final int MAX_INPUT_LENGTH = 10_000;
    private static final int MAX_OUTPUT_LENGTH = 20_000;

    public CompileResponse compileAndRun(String sourceCode, String stdin) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return new CompileResponse(false, "", "Please enter Java code.");
        }
        if (sourceCode.length() > MAX_SOURCE_LENGTH) {
            return new CompileResponse(false, "", "Source code is too long (maximum 50,000 characters).");
        }
        if (stdin != null && stdin.length() > MAX_INPUT_LENGTH) {
            return new CompileResponse(false, "", "Input is too long (maximum 10,000 characters).");
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("online-java-compiler-");
            Files.writeString(workDir.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);

            Process compiler = new ProcessBuilder("javac", "Main.java")
                    .directory(workDir.toFile()).start();
            ProcessOutput compilerOutput = collectOutput(compiler);

            if (!compiler.waitFor(10, TimeUnit.SECONDS)) {
                stopProcess(compiler);
                return new CompileResponse(false, "", "Compilation timed out after 10 seconds.");
            }

            String compileErrors = compilerOutput.stderr().join();
            String compileStdout = compilerOutput.stdout().join();
            if (compiler.exitValue() != 0) {
                return new CompileResponse(false, "",
                        limit(compileErrors.isBlank() ? compileStdout : compileErrors));
            }

            Process program = new ProcessBuilder("java", "-Xmx64m", "-XX:ActiveProcessorCount=1", "Main")
                    .directory(workDir.toFile()).start();
            ProcessOutput programOutput = collectOutput(program);

            try (var input = program.getOutputStream()) {
                input.write((stdin == null ? "" : stdin).getBytes(StandardCharsets.UTF_8));
            }

            if (!program.waitFor(5, TimeUnit.SECONDS)) {
                stopProcess(program);
                return new CompileResponse(false, limit(programOutput.stdout().join()),
                        "Program timed out after 5 seconds.");
            }

            String output = limit(programOutput.stdout().join());
            String errors = limit(programOutput.stderr().join());
            if (program.exitValue() != 0) {
                return new CompileResponse(false, output,
                        errors.isBlank() ? "Program exited with code " + program.exitValue() : errors);
            }
            return new CompileResponse(true, output, errors);

        } catch (IOException e) {
            return new CompileResponse(false, "", "Could not start Java compiler/runtime. Check that JDK 25 is installed and both 'java' and 'javac' are on PATH. Details: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompileResponse(false, "", "The compilation request was interrupted.");
        } finally {
            if (workDir != null) deleteDirectory(workDir);
        }
    }

    private ProcessOutput collectOutput(Process process) {
        CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readLimited(process.getInputStream()));
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readLimited(process.getErrorStream()));
        return new ProcessOutput(stdout, stderr);
    }

    private String readLimited(InputStream stream) {
        try (stream) {
            byte[] buffer = new byte[2048];
            StringBuilder result = new StringBuilder();
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (result.length() < MAX_OUTPUT_LENGTH) {
                    int remaining = MAX_OUTPUT_LENGTH - result.length();
                    result.append(new String(buffer, 0, Math.min(count, remaining), StandardCharsets.UTF_8));
                }
            }
            if (result.length() >= MAX_OUTPUT_LENGTH) result.append("\n[Output truncated]");
            return result.toString();
        } catch (IOException e) {
            return "Unable to read process output: " + e.getMessage();
        }
    }

    private String limit(String text) {
        if (text == null) return "";
        return text.length() > MAX_OUTPUT_LENGTH
                ? text.substring(0, MAX_OUTPUT_LENGTH) + "\n[Output truncated]"
                : text;
    }

    private void stopProcess(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void deleteDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private record ProcessOutput(CompletableFuture<String> stdout, CompletableFuture<String> stderr) { }
}
