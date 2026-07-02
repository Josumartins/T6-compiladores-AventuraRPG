package br.ufscar.dc.compiladores.aventurarpg;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Principal {
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.err.println("Uso: java -jar questscript.jar <arquivo.txt>");
            System.exit(1);
        }

        CharStream cs = CharStreams.fromFileName(args[0]);

        AventuraRPGLexer lexer = new AventuraRPGLexer(cs);
        List<String> erros = new ArrayList<>();

        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o,
                                    int linha, int coluna, String msg, RecognitionException e) {
                erros.add(String.format("Linha %d:%d %s", linha, coluna, msg));
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AventuraRPGParser parser = new AventuraRPGParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o,
                                    int linha, int coluna, String msg, RecognitionException e) {
                erros.add(String.format("Linha %d:%d %s", linha, coluna, msg));
            }
        });

        ParseTree arvore = parser.programa();

        AventuraRPGSemantico semantico = new AventuraRPGSemantico();
        AventuraRPGParser.ProgramaContext programa = (AventuraRPGParser.ProgramaContext) arvore;

        // Primeira passagem: registra todos os nomes
        semantico.primeiraPassagem(programa.campanha());
        // Segunda passagem: verifica regras semanticas
        semantico.visit(arvore);
        erros.addAll(AventuraRPGSemanticoUtils.errosSemanticos);

        if (!erros.isEmpty()) {
            System.out.println("\n[ERROS ENCONTRADOS]");
            erros.forEach(System.out::println);
            System.out.println("Fim da compilacao");
            return;
        }

        // Leitura interativa
        try (Scanner scanner = new Scanner(System.in)) {
            AventuraRPGInterpretador interpretador =
                new AventuraRPGInterpretador(semantico.pilhaDeTabelas, scanner);
            interpretador.visit(arvore);
        }
    }
}

