package com.sachin.onlinecompiler.dto;

public record CompileResponse(boolean success, String output, String error) {
}
