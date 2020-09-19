package com.company;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        RadixTree<String> radix = new RadixTree<>();

        List<String> lines = Files.readAllLines(Paths.get("dfa-data.txt"), StandardCharsets.UTF_8);
        for(String line: lines) {
            if(line.startsWith("+")) {
                String out = line.replaceAll("\\s|[+]", "");
                radix.add(out);
            }
        }

        radix.prepareResult();
        radix.prepareDot();
    }
}
