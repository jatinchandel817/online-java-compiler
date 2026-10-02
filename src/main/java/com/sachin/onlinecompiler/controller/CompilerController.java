package com.sachin.onlinecompiler.controller;

import com.sachin.onlinecompiler.service.CompilerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CompilerController {
    private final CompilerService compilerService;

    public CompilerController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("sourceCode", defaultCode());
        model.addAttribute("stdin", "");
        model.addAttribute("result", null);
        return "index";
    }

    @PostMapping("/run")
    public String runCode(@RequestParam(defaultValue = "") String sourceCode,
                          @RequestParam(defaultValue = "") String stdin,
                          Model model) {
        model.addAttribute("sourceCode", sourceCode);
        model.addAttribute("stdin", stdin);
        model.addAttribute("result", compilerService.compileAndRun(sourceCode, stdin));
        return "index";
    }

    private String defaultCode() {
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        System.out.println(\"Hello, Java!\");\n" +
               "    }\n" +
               "}\n";
    }
}
