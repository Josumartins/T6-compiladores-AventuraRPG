package br.ufscar.dc.compiladores.aventurarpg;

import java.util.*;

public class TabelaDeSimbolos {

    public enum TipoSimbolo {
        PERSONAGEM,
        ITEM,
        ENCONTRO,
        MISSAO
    }

    public class EntradaTabelaDeSimbolos {
        String nome;
        TipoSimbolo tipo;
        int linha;

        public EntradaTabelaDeSimbolos(String nome, TipoSimbolo tipo, int linha) {
            this.nome  = nome;
            this.tipo  = tipo;
            this.linha = linha;
        }

        public String     getNome()  { return nome;  }
        public TipoSimbolo getTipo() { return tipo;  }
        public int        getLinha() { return linha; }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela = new LinkedHashMap<>();

    public void adicionar(String nome, TipoSimbolo tipo, int linha) {
        if (tabela.containsKey(nome)) {
            EntradaTabelaDeSimbolos existente = tabela.get(nome);
            throw new AventuraRPGSemanticoUtils.ExcecaoSemantica(
                String.format("Linha %d: nome '%s' ja foi declarado na linha %d como %s.",
                    linha, nome, existente.getLinha(), existente.getTipo()));
        }
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, linha));
    }

    public boolean existe(String nome)                      { return tabela.containsKey(nome); }
    public boolean existe(String nome, TipoSimbolo tipo)    {
        EntradaTabelaDeSimbolos s = tabela.get(nome);
        return s != null && s.getTipo() == tipo;
    }

    public EntradaTabelaDeSimbolos getEntrada(String nome)  { return tabela.get(nome); }

    public Optional<EntradaTabelaDeSimbolos> buscar(String nome) {
        return Optional.ofNullable(tabela.get(nome));
    }

    public Collection<EntradaTabelaDeSimbolos> todosDe(TipoSimbolo tipo) {
        return tabela.values().stream().filter(s -> s.getTipo() == tipo).toList();
    }

    public List<EntradaTabelaDeSimbolos> entradas() { return new ArrayList<>(tabela.values()); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("=== Tabela de Simbolos ===\n");
        for (EntradaTabelaDeSimbolos s : tabela.values())
            sb.append(String.format("  [%s] '%s' (linha %d)%n", s.getTipo(), s.getNome(), s.getLinha()));
        return sb.toString();
    }
}
