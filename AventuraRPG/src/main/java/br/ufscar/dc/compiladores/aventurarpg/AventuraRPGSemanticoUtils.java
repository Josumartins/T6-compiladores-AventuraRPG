package br.ufscar.dc.compiladores.aventurarpg;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;

public class AventuraRPGSemanticoUtils {
    public static class ExcecaoSemantica extends RuntimeException {
        public ExcecaoSemantica(String mensagem) { super(mensagem); }
    }

    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        errosSemanticos.add(String.format("Linha %d: %s", t.getLine(), mensagem));
    }

    public static void adicionarErroSemantico(int linha, String mensagem) {
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    public static String removerAspas(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s;
    }
}
