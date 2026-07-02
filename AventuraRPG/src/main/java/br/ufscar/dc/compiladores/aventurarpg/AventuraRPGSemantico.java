package br.ufscar.dc.compiladores.aventurarpg;

import org.antlr.v4.runtime.tree.TerminalNode;

public class AventuraRPGSemantico extends AventuraRPGBaseVisitor<Void> {

    Escopos pilhaDeTabelas = new Escopos();

    private static final int PV_POR_NIVEL = 20;

    //Primeira passagem: registra todos os nomes
    public void primeiraPassagem(AventuraRPGParser.CampanhaContext ctx) {
        TabelaDeSimbolos escopo = pilhaDeTabelas.obterEscopoAtual();
        for (var corpo : ctx.corposCampanha()) {
            try {
                if (corpo.declPersonagem() != null) {
                    String nome  = corpo.declPersonagem().ID().getText();
                    int    linha = corpo.declPersonagem().ID().getSymbol().getLine();
                    escopo.adicionar(nome, TabelaDeSimbolos.TipoSimbolo.PERSONAGEM, linha);
                }
                if (corpo.declItem() != null) {
                    String nome  = AventuraRPGSemanticoUtils.removerAspas(corpo.declItem().STRING().getText());
                    int    linha = corpo.declItem().STRING().getSymbol().getLine();
                    escopo.adicionar(nome, TabelaDeSimbolos.TipoSimbolo.ITEM, linha);
                }
                if (corpo.declEncontro() != null) {
                    String nome  = AventuraRPGSemanticoUtils.removerAspas(corpo.declEncontro().STRING().getText());
                    int    linha = corpo.declEncontro().STRING().getSymbol().getLine();
                    escopo.adicionar(nome, TabelaDeSimbolos.TipoSimbolo.ENCONTRO, linha);
                }
                if (corpo.declMissao() != null) {
                    String nome  = AventuraRPGSemanticoUtils.removerAspas(corpo.declMissao().STRING().getText());
                    int    linha = corpo.declMissao().STRING().getSymbol().getLine();
                    escopo.adicionar(nome, TabelaDeSimbolos.TipoSimbolo.MISSAO, linha);
                }
            } catch (AventuraRPGSemanticoUtils.ExcecaoSemantica e) {
                // VERIFICACAO 1: Unicidade de nomes
                AventuraRPGSemanticoUtils.errosSemanticos.add("[ERRO SEMANTICO] " + e.getMessage());
            }
        }
    }

    // ── VERIFICACAO 2: Pontos deVida (PV) vs Nivel
    @Override
    public Void visitDeclPersonagem(AventuraRPGParser.DeclPersonagemContext ctx) {
        String nomePersonagem = ctx.ID().getText();
        int nivel = -1, pv = -1, linhaPv = -1;

        for (var campo : ctx.campoPersonagem()) {
            if (campo instanceof AventuraRPGParser.PerNivelContext pn)
                nivel = Integer.parseInt(pn.INT().getText());
            if (campo instanceof AventuraRPGParser.PerPvContext pp) {
                pv      = Integer.parseInt(pp.INT().getText());
                linhaPv = pp.INT().getSymbol().getLine();
            }
        }
        if (nivel > 0 && pv > 0) {
            int pvMaximo = nivel * PV_POR_NIVEL;
            if (pv > pvMaximo)
                AventuraRPGSemanticoUtils.adicionarErroSemantico(linhaPv,
                    String.format("personagem '%s' tem pv=%d mas nivel %d permite no maximo %d (nivel x %d).",
                        nomePersonagem, pv, nivel, pvMaximo, PV_POR_NIVEL));
        }
        return visitChildren(ctx);
    }

    //VERIFICACAO 3: Itens em recompensas devem estar declarados
    @Override
    public Void visitEncontroRecompensa(AventuraRPGParser.EncontroRecompensaContext ctx) {
        verificarItensRecompensa(ctx.listaRecompensa());
        return visitChildren(ctx);
    }

    @Override
    public Void visitMissaoRecompensa(AventuraRPGParser.MissaoRecompensaContext ctx) {
        verificarItensRecompensa(ctx.listaRecompensa());
        return visitChildren(ctx);
    }

    private void verificarItensRecompensa(AventuraRPGParser.ListaRecompensaContext lista) {
        if (lista == null) return;
        for (var entrada : lista.entradaRecompensa()) {
            if (entrada instanceof AventuraRPGParser.RecompItemContext ri) {
                String nomeItem = AventuraRPGSemanticoUtils.removerAspas(ri.STRING().getText());
                int    linha    = ri.STRING().getSymbol().getLine();
                if (!pilhaDeTabelas.obterEscopoAtual().existe(nomeItem, TabelaDeSimbolos.TipoSimbolo.ITEM))
                    AventuraRPGSemanticoUtils.adicionarErroSemantico(linha,
                        String.format("item '%s' usado em recompensa nao foi declarado na campanha.", nomeItem));
            }
        }
    }

    // ── VERIFICACAO 4: Passos 'entrar' referenciam encontros declarados ──
    @Override
    public Void visitPassoEntrar(AventuraRPGParser.PassoEntrarContext ctx) {
        String nomeEncontro = AventuraRPGSemanticoUtils.removerAspas(ctx.STRING().getText());
        int    linha        = ctx.STRING().getSymbol().getLine();
        if (!pilhaDeTabelas.obterEscopoAtual().existe(nomeEncontro, TabelaDeSimbolos.TipoSimbolo.ENCONTRO))
            AventuraRPGSemanticoUtils.adicionarErroSemantico(linha,
                String.format("passo 'entrar \"%s\"' referencia um encontro nao declarado.", nomeEncontro));
        return null;
    }

    // ── VERIFICACAO 5: Itens no inventario devem estar declaradoS
    @Override
    public Void visitPerInventario(AventuraRPGParser.PerInventarioContext ctx) {
        String nomePersonagem = ((AventuraRPGParser.DeclPersonagemContext) ctx.getParent()).ID().getText();
        for (TerminalNode str : ctx.listaRefs().STRING()) {
            String nomeItem = AventuraRPGSemanticoUtils.removerAspas(str.getText());
            int    linha    = str.getSymbol().getLine();
            if (!pilhaDeTabelas.obterEscopoAtual().existe(nomeItem, TabelaDeSimbolos.TipoSimbolo.ITEM))
                AventuraRPGSemanticoUtils.adicionarErroSemantico(linha,
                    String.format("personagem '%s' possui '%s' no inventario, mas esse item nao foi declarado.",
                        nomePersonagem, nomeItem));
        }
        return null;
    }
}

