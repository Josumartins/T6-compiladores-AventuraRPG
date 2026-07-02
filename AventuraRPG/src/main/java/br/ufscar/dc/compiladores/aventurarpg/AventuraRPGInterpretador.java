package br.ufscar.dc.compiladores.aventurarpg;

import java.util.*;

public class AventuraRPGInterpretador extends AventuraRPGBaseVisitor<Void> {

    // ── Infra ────────────────────────────────────────────────────────────
    private final Escopos escopos;
    private final Scanner scanner;

    public AventuraRPGInterpretador(Escopos escopos, Scanner scanner) {
        this.escopos = escopos;
        this.scanner = scanner;
    }

    // ── Cores ANSI ───────────────────────────────────────────────────────
    private static final String RESET    = "\u001B[0m";
    private static final String NEGRITO  = "\u001B[1m";
    private static final String VERMELHO = "\u001B[31m";
    private static final String VERDE    = "\u001B[32m";
    private static final String AMARELO  = "\u001B[33m";
    private static final String AZUL     = "\u001B[34m";
    private static final String ROXO     = "\u001B[35m";
    private static final String CIANO    = "\u001B[36m";
    private static final String BRANCO   = "\u001B[37m";

    // ── Modelos de dados
    private record DadosItem(String nome, String tipo, String efeito, int valor) {}
    private record Inimigo(String nome, int quantidade) {}
    private record DadosEncontro(String nome, List<Inimigo> inimigos, List<String> itensRecomp,
                                 int ouroRecomp, int xpRecomp, String dificuldade) {}
    private record DadosMissao(String nome, List<String> passos, List<String> itensRecomp,
                               int ouroRecomp, int xpRecomp) {}

    private static class Personagem {
        String nomePersonagem, classe, nomeJogador;
        int nivel, pv, pvMaximo;
        Map<String, Integer> atributos = new LinkedHashMap<>();
        List<String> inventario = new ArrayList<>();
        int xp = 0, ouro = 0;
        Personagem(String nome) { this.nomePersonagem = nome; }
    }

    // ── Estado global
    private final Map<String, Personagem>   personagens = new LinkedHashMap<>();
    private final Map<String, DadosItem>    itens       = new LinkedHashMap<>();
    private final Map<String, DadosEncontro> encontros  = new LinkedHashMap<>();
    private final List<DadosMissao>         missoes     = new ArrayList<>();
    private String nomeCampanha;
    private final Random sorteio = new Random();

    // ────────────────────────────────────────────────────────────────────
    //  VISITANTES — coleta declaracoes
    // ────────────────────────────────────────────────────────────────────

    @Override
    public Void visitCampanha(AventuraRPGParser.CampanhaContext ctx) {
        nomeCampanha = AventuraRPGSemanticoUtils.removerAspas(ctx.STRING().getText());
        visitChildren(ctx);
        iniciarJogo();
        return null;
    }

    @Override
    public Void visitDeclPersonagem(AventuraRPGParser.DeclPersonagemContext ctx) {
        Personagem p = new Personagem(ctx.ID().getText());
        for (var campo : ctx.campoPersonagem()) {
            if (campo instanceof AventuraRPGParser.PerClasseContext pc)
                p.classe = pc.tipoClasse().getText();
            if (campo instanceof AventuraRPGParser.PerNivelContext pn)
                p.nivel = Integer.parseInt(pn.INT().getText());
            if (campo instanceof AventuraRPGParser.PerPvContext pp) {
                p.pv = Integer.parseInt(pp.INT().getText());
                p.pvMaximo = p.pv;
            }
            if (campo instanceof AventuraRPGParser.PerAtributosContext pa)
                for (var attr : pa.listaAtributos().entradaAtributo())
                    p.atributos.put(attr.nomeAtributo().getText(),
                                    Integer.parseInt(attr.INT().getText()));
            if (campo instanceof AventuraRPGParser.PerInventarioContext pi)
                for (var s : pi.listaRefs().STRING())
                    p.inventario.add(AventuraRPGSemanticoUtils.removerAspas(s.getText()));
        }
        personagens.put(p.nomePersonagem, p);
        return null;
    }

    @Override
    public Void visitDeclItem(AventuraRPGParser.DeclItemContext ctx) {
        String nome = AventuraRPGSemanticoUtils.removerAspas(ctx.STRING().getText());
        String tipo = "", efeito = "";
        int valor = 0;
        for (var campo : ctx.campoItem()) {
            if (campo instanceof AventuraRPGParser.ItemTipoContext it)
                tipo = it.tipoItem().getText();
            if (campo instanceof AventuraRPGParser.ItemEfeitoContext ie)
                efeito = ie.efeito().getText();
            if (campo instanceof AventuraRPGParser.ItemValorContext iv)
                valor = Integer.parseInt(iv.INT().getText());
        }
        itens.put(nome, new DadosItem(nome, tipo, efeito, valor));
        return null;
    }

    @Override
    public Void visitDeclEncontro(AventuraRPGParser.DeclEncontroContext ctx) {
        String nome = AventuraRPGSemanticoUtils.removerAspas(ctx.STRING().getText());
        List<Inimigo> listaIni = new ArrayList<>();
        List<String> itensR = new ArrayList<>();
        int ouro = 0, xp = 0;
        String dif = "medio";

        for (var campo : ctx.campoEncontro()) {
            if (campo instanceof AventuraRPGParser.EncontroInimigosContext ei)
                for (var e : ei.listaInimigos().entradaInimigo()) {
                    int qtd = e.MULT() != null ? Integer.parseInt(e.MULT().getText().substring(1)) : 1;
                    listaIni.add(new Inimigo(e.ID().getText(), qtd));
                }
            if (campo instanceof AventuraRPGParser.EncontroRecompensaContext er)
                for (var r : er.listaRecompensa().entradaRecompensa()) {
                    if (r instanceof AventuraRPGParser.RecompItemContext ri)
                        itensR.add(AventuraRPGSemanticoUtils.removerAspas(ri.STRING().getText()));
                    if (r instanceof AventuraRPGParser.RecompOuroContext ro)
                        ouro = Integer.parseInt(ro.INT().getText());
                    if (r instanceof AventuraRPGParser.RecompXpContext rx)
                        xp = Integer.parseInt(rx.INT().getText());
                }
            if (campo instanceof AventuraRPGParser.EncontroDificuldadeContext ed)
                dif = ed.nivelDificuldade().getText();
        }
        encontros.put(nome, new DadosEncontro(nome, listaIni, itensR, ouro, xp, dif));
        return null;
    }

    @Override
    public Void visitDeclMissao(AventuraRPGParser.DeclMissaoContext ctx) {
        String nome = AventuraRPGSemanticoUtils.removerAspas(ctx.STRING().getText());
        List<String> passos = new ArrayList<>();
        List<String> itensR = new ArrayList<>();
        int ouro = 0, xp = 0;

        for (var campo : ctx.campoMissao()) {
            if (campo instanceof AventuraRPGParser.MissaoPassosContext mp)
                for (var passo : mp.passo()) passos.add(passo.getText());
            if (campo instanceof AventuraRPGParser.MissaoRecompensaContext mr)
                for (var r : mr.listaRecompensa().entradaRecompensa()) {
                    if (r instanceof AventuraRPGParser.RecompItemContext ri)
                        itensR.add(AventuraRPGSemanticoUtils.removerAspas(ri.STRING().getText()));
                    if (r instanceof AventuraRPGParser.RecompOuroContext ro)
                        ouro = Integer.parseInt(ro.INT().getText());
                    if (r instanceof AventuraRPGParser.RecompXpContext rx)
                        xp = Integer.parseInt(rx.INT().getText());
                }
        }
        missoes.add(new DadosMissao(nome, passos, itensR, ouro, xp));
        return null;
    }

    // FLUXO PRINCIPAL

    private void iniciarJogo() {
        limparTela();
        telaTitulo();
        fichaDePersonagem();
        aguardarEnter("Pressione ENTER para comecar a aventura...");
        executarCampanha();
    }

    private void telaTitulo() {
        escrever(NEGRITO + AMARELO + """
                  ╔══════════════════════════════════════════╗
                  ║            Sistema de RPG                ║
                  ╚══════════════════════════════════════════╝
                """ + RESET);
        escrever(NEGRITO + CIANO + "  🐉  " + nomeCampanha + RESET);
        separador();
    }

    //  CRIACAO DE PERSONAGEM

    private void fichaDePersonagem() {
        escrever(NEGRITO + "\n  ══ CRIACAO DE PERSONAGEM ══\n" + RESET);

        if (personagens.isEmpty()) {
            escrever(VERMELHO + "  Nenhum personagem declarado no arquivo." + RESET);
            return;
        }

        List<Personagem> lista = new ArrayList<>(personagens.values());
        escrever("  Escolha seu personagem:\n");

        for (int i = 0; i < lista.size(); i++) {
            Personagem p = lista.get(i);
            escrever(String.format("  %s[%d]%s %s%-12s%s %s | Nivel %d | PV %d",
                AMARELO, i + 1, RESET,
                NEGRITO, p.nomePersonagem, RESET,
                emojiClasse(p.classe) + " " + traduzirClasse(p.classe),
                p.nivel, p.pvMaximo));

            if (!p.atributos.isEmpty()) {
                StringBuilder ab = new StringBuilder("       ");
                p.atributos.forEach((k, v) ->
                    ab.append(traduzirAtributoAbrev(k)).append(":").append(v).append("  "));
                escrever(AZUL + ab.toString() + RESET);
            }
            if (!p.inventario.isEmpty())
                escrever(CIANO + "       Inventario inicial: " + String.join(", ", p.inventario) + RESET);
            escrever("");
        }

        int escolha = lerOpcao("  Seu personagem", 1, lista.size());
        Personagem escolhido = lista.get(escolha - 1);

        escrever("\n" + NEGRITO + "  Como seu aventureiro se chama? " + RESET);
        imprimirSemQuebra("  > ");
        String nomeJogador = scanner.nextLine().trim();
        if (nomeJogador.isBlank()) nomeJogador = escolhido.nomePersonagem;
        escolhido.nomeJogador = nomeJogador;

        // Exibe ficha final
        escrever("\n");
        separador();
        escrever(NEGRITO + AMARELO + "  ⚔  FICHA DO AVENTUREIRO  ⚔" + RESET);
        separador();
        escrever(String.format("  Nome do Jogador : %s%s%s",          NEGRITO, nomeJogador, RESET));
        escrever(String.format("  Personagem      : %s%s%s",          NEGRITO, escolhido.nomePersonagem, RESET));
        escrever(String.format("  Classe          : %s %s",           emojiClasse(escolhido.classe), traduzirClasse(escolhido.classe)));
        escrever(String.format("  Nivel           : %d",              escolhido.nivel));
        escrever(String.format("  Pontos de Vida  : %s%d/%d%s",       VERDE, escolhido.pv, escolhido.pvMaximo, RESET));

        if (!escolhido.atributos.isEmpty()) {
            escrever("");
            escrever("  Atributos:");
            escolhido.atributos.forEach((k, v) ->
                escrever(String.format("    %-16s %s%d%s %s",
                    traduzirAtributo(k) + ":", NEGRITO, v, RESET, barraAtributo(v))));
        }
        if (!escolhido.inventario.isEmpty()) {
            escrever("");
            escrever("  Inventario inicial:");
            escolhido.inventario.forEach(item -> escrever("    📦 " + item));
        }
        separador();

        // Manter o personagem escolhido
        personagens.clear();
        personagens.put(escolhido.nomePersonagem, escolhido);
    }

    //  CAMPANHA

    private void executarCampanha() {
        limparTela();
        Personagem jogador = personagens.values().iterator().next();
        escrever(NEGRITO + VERDE + "\n  Bem-vindo(a), " + jogador.nomeJogador + "!\n" + RESET);
        escrever("  A aventura de " + NEGRITO + jogador.nomePersonagem + RESET + " comeca agora...");
        aguardar(1200);

        for (int i = 0; i < missoes.size(); i++) {
            if (!grupoVivo()) break;
            executarMissao(missoes.get(i), i + 1, missoes.size());
        }

        if (grupoVivo()) telaVitoria();
        else telaDerrota();
    }

    // ────────────────────────────────────────────────────────────────────
    //  MISSAO
    // ────────────────────────────────────────────────────────────────────

    private void executarMissao(DadosMissao missao, int num, int total) {
        limparTela();
        escrever(NEGRITO + ROXO + "\n  ╔══════════════════════════════════════╗" + RESET);
        escrever(NEGRITO + ROXO + String.format("  ║  MISSAO %d/%d: %-25s ║",
            num, total,
            missao.nome().length() > 25 ? missao.nome().substring(0, 22) + "..." : missao.nome()) + RESET);
        escrever(NEGRITO + ROXO + "  ╚══════════════════════════════════════╝\n" + RESET);

        Personagem p = personagens.values().iterator().next();
        escrever(String.format("  %s%s%s [%s] | PV: %s%d/%d%s | XP: %d | Ouro: %d 🪙",
            NEGRITO, p.nomeJogador != null ? p.nomeJogador : p.nomePersonagem, RESET,
            traduzirClasse(p.classe),
            VERDE, p.pv, p.pvMaximo, RESET, p.xp, p.ouro));
        separador();

        aguardarEnter("  Pressione ENTER para iniciar a missao...");

        for (String passo : missao.passos()) {
            if (!grupoVivo()) break;
            executarPasso(passo);
        }

        if (grupoVivo()) {
            escrever(NEGRITO + VERDE + "\n   Missao \"" + missao.nome() + "\" concluida!" + RESET);
            distribuirRecompensas(missao.itensRecomp(), missao.ouroRecomp(), missao.xpRecomp());
            aguardar(800);
            aguardarEnter("\n  Pressione ENTER para continuar...");
        }
    }

    //  PASSO

    private void executarPasso(String passo) {
        escrever("");
        if (passo.startsWith("entrar")) {
            String nomeEnc = extrairArgString(passo);
            DadosEncontro enc = encontros.get(nomeEnc);
            if (enc != null) executarEncontro(enc);

        } else if (passo.startsWith("coletar")) {
            String nomeItem = extrairArgString(passo);
            escrever(AMARELO + "  Voce encontrou: " + NEGRITO + nomeItem + RESET);
            int op = menuOpcoes("  O que deseja fazer?",
                List.of("Pegar o item", "Deixar para la"));
            if (op == 1) {
                darItemAoJogador(nomeItem);
                escrever(VERDE + "  Item adicionado ao inventario!" + RESET);
            } else {
                escrever("  Voce deixou o item para tras.");
            }

        } else if (passo.startsWith("usar")) {
            String nomeItem = extrairArgString(passo);
            Personagem p = personagens.values().iterator().next();
            if (p.inventario.contains(nomeItem)) {
                escrever(CIANO + "  🔮 Usando: " + nomeItem + RESET);
                aplicarEfeito(nomeItem, p);
            } else {
                escrever(VERMELHO + "  Voce nao possui \"" + nomeItem + "\" no inventario." + RESET);
            }

        } else if (passo.startsWith("retornar")) {
            String destino = extrairArgString(passo);
            escrever(VERDE + "    Retornando para " + NEGRITO + destino + RESET);
            aguardar(600);

        } else if (passo.startsWith("falar_com")) {
            String npc = passo.replace("falar_com", "").trim();
            executarDialogo(npc);
        }
    }

    //  ENCONTRO COM ESCOLHAS

    private void executarEncontro(DadosEncontro enc) {
        separador();
        escrever(NEGRITO + VERMELHO + "   ENCONTRO: " + enc.nome()
            + " [" + traduzirDificuldade(enc.dificuldade()).toUpperCase() + "]" + RESET);
        escrever("  Inimigos: " + formatarInimigos(enc.inimigos()));
        separador();
        aguardar(600);

        Personagem p = personagens.values().iterator().next();
        int inimigosVivos = enc.inimigos().stream().mapToInt(Inimigo::quantidade).sum();
        int modDif = modificadorDificuldade(enc.dificuldade());
        int rodada = 1;

        while (inimigosVivos > 0 && p.pv > 0) {
            escrever(NEGRITO + "\n  ── Rodada " + rodada + " ──" + RESET);
            mostrarStatusBatalha(p, inimigosVivos);

            // Opcoes de acao
            List<String> acoes = new ArrayList<>(List.of("Atacar", "Defender (+2 de defesa nesta rodada)"));
            boolean temConsumivel = p.inventario.stream().anyMatch(i -> {
                DadosItem it = itens.get(i);
                return it != null && it.tipo().equals("consumivel");
            });
            if (temConsumivel) acoes.add("Usar item de cura");

            int acao = menuOpcoes("  Sua acao:", acoes);
            boolean defendendo = (acao == 2);

            // Ataque do jogador
            int modAtaque = modificador(
                p.atributos.getOrDefault("forca",
                p.atributos.getOrDefault("inteligencia",
                p.atributos.getOrDefault("destreza", 10))));
            int dano = rolarDado(8) + modAtaque;
            escrever(String.format("    %s ataca → %s%d de dano%s (1d8%+d)",
                p.nomePersonagem, VERMELHO, dano, RESET, modAtaque));

            if (dano >= 4) {
                inimigosVivos = Math.max(0, inimigosVivos - 1);
                if (inimigosVivos > 0)
                    escrever(AMARELO + "  Inimigo abatido! Restam: " + inimigosVivos + RESET);
                else
                    escrever(VERDE + "  Ultimo inimigo derrotado!" + RESET);
            } else {
                escrever("  O golpe acertou mas nao foi suficiente para abater.");
            }

            // Usar item durante batalha
            if (acao == 3) {
                String itemUsado = escolherConsumivel(p);
                if (itemUsado != null) {
                    aplicarEfeito(itemUsado, p);
                    p.inventario.remove(itemUsado);
                }
            }

            // Contra-ataque dos inimigos
            if (inimigosVivos > 0) {
                int danoInimigo = Math.max(1, rolarDado(6) + modDif - (defendendo ? 2 : 0));
                p.pv = Math.max(0, p.pv - danoInimigo);
                escrever(String.format("   Inimigos atacam %s → %s%d de dano%s  | PV: %s%d/%d%s",
                    p.nomePersonagem, VERMELHO, danoInimigo, RESET,
                    p.pv > p.pvMaximo / 4 ? VERDE : VERMELHO, p.pv, p.pvMaximo, RESET));
                if (defendendo) escrever(AZUL + "  (Sua defesa reduziu o dano!)" + RESET);
            }
            rodada++;
        }

        if (p.pv > 0) {
            escrever(VERDE + "\n  Vitoria! Todos os inimigos foram derrotados." + RESET);
            distribuirRecompensas(enc.itensRecomp(), enc.ouroRecomp(), enc.xpRecomp());
        } else {
            escrever(VERMELHO + "\n  " + p.nomePersonagem + " foi derrotado..." + RESET);
        }
        separador();
        aguardarEnter("  Pressione ENTER para continuar...");
    }

    //  DIALOGO
    
    private void executarDialogo(String npc) {
        escrever(CIANO + "\n  💬 " + NEGRITO + npc + RESET + CIANO + " se aproxima..." + RESET);
        aguardar(600);

        List<String> falas = List.of(
            "Aventureiro(a)! Que bom que chegou. Temos problemas serios por aqui.",
            "Cuidado la fora. Nao e para qualquer um.",
            "Ouvi dizer que ha recompensas generosas para quem for corajoso o suficiente.",
            "Boa sorte na sua jornada. Voce vai precisar de toda ajuda possivel."
        );
        escrever("  " + npc + ": \"" + falas.get(sorteio.nextInt(falas.size())) + "\"");
        escrever("");

        int op = menuOpcoes("  Voce responde:",
            List.of("\"Pode contar comigo!\"",
                    "\"O que esta acontecendo aqui?\"",
                    "\"Tenho pressa, preciso ir.\""));

        switch (op) {
            case 1 -> escrever(CIANO + "  " + npc + ": \"Sabia que podia contar com voce!\"" + RESET);
            case 2 -> escrever(CIANO + "  " + npc + ": \"E uma longa historia... fique atento nas proximidades.\"" + RESET);
            case 3 -> escrever(CIANO + "  " + npc + ": \"Entendo. Boa sorte entao!\"" + RESET);
        }
        aguardar(400);
    }

    // ────────────────────────────────────────────────────────────────────
    //  RECOMPENSAS
    // ────────────────────────────────────────────────────────────────────

    private void distribuirRecompensas(List<String> itensR, int ouro, int xp) {
        Personagem p = personagens.values().iterator().next();
        escrever("\n  " + NEGRITO + AMARELO + "── Recompensas ──" + RESET);
        if (xp > 0)   { p.xp += xp;    escrever(String.format("  Experiencia : %s+%d XP%s",  AMARELO, xp,   RESET)); }
        if (ouro > 0) { p.ouro += ouro; escrever(String.format("  Ouro        : %s+%d 🪙%s", AMARELO, ouro, RESET)); }
        for (String item : itensR) {
            escrever(String.format("  Item        : %s%s%s", CIANO, item, RESET));
            darItemAoJogador(item);
        }
    }

    private void darItemAoJogador(String nome) {
        personagens.values().iterator().next().inventario.add(nome);
    }

    // ────────────────────────────────────────────────────────────────────
    //  TELAS FINAIS
    // ────────────────────────────────────────────────────────────────────

    private void telaVitoria() {
        limparTela();
        Personagem p = personagens.values().iterator().next();
        escrever(NEGRITO + AMARELO + """
                  ╔══════════════════════════════════════════╗
                  ║                 VITORIA!                 ║
                  ╚══════════════════════════════════════════╝
                """ + RESET);
        escrever(String.format("  %s%s%s completou a campanha \"%s\"!\n",
            NEGRITO, p.nomeJogador != null ? p.nomeJogador : p.nomePersonagem, RESET, nomeCampanha));
        separador();
        escrever(NEGRITO + "  RESUMO FINAL" + RESET);
        escrever(String.format("  Personagem      : %s", p.nomePersonagem));
        escrever(String.format("  Classe          : %s %s", emojiClasse(p.classe), traduzirClasse(p.classe)));
        escrever(String.format("  Pontos de Vida  : %d/%d", p.pv, p.pvMaximo));
        escrever(String.format("  Experiencia     : %s%d XP%s",  AMARELO, p.xp,   RESET));
        escrever(String.format("  Ouro            : %s%d 🪙%s",  AMARELO, p.ouro, RESET));
        escrever(String.format("  Itens coletados : %d", p.inventario.size()));
        if (!p.inventario.isEmpty())
            p.inventario.forEach(i -> escrever("     " + i));
        separador();
        escrever("");
    }

    private void telaDerrota() {
        limparTela();
        escrever(NEGRITO + VERMELHO + """
                  ╔══════════════════════════════════════════╗
                  ║                FIM DE JOGO               ║
                  ╚══════════════════════════════════════════╝
                """ + RESET);
        escrever("  Sua jornada terminou... por enquanto.");
        escrever("  Tente novamente e honre o nome do seu personagem!\n");
    }


    //  INTERFACE COM O JOGADOR

    private int menuOpcoes(String titulo, List<String> opcoes) {
        escrever("\n" + NEGRITO + titulo + RESET);
        for (int i = 0; i < opcoes.size(); i++)
            escrever(String.format("  %s[%d]%s %s", AMARELO, i + 1, RESET, opcoes.get(i)));
        return lerOpcao("  > ", 1, opcoes.size());
    }

    private int lerOpcao(String aviso, int minimo, int maximo) {
        while (true) {
            imprimirSemQuebra(NEGRITO + aviso + " [" + minimo + "-" + maximo + "]: " + RESET);
            String linha = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(linha);
                if (v >= minimo && v <= maximo) return v;
            } catch (NumberFormatException ignorado) {}
            escrever(VERMELHO + "  Opcao invalida. Tente novamente." + RESET);
        }
    }

    private void aguardarEnter(String msg) {
        imprimirSemQuebra(AZUL + "\n  " + msg + RESET);
        scanner.nextLine();
    }

    private void mostrarStatusBatalha(Personagem p, int inimigosVivos) {
        double pct = (double) p.pv / p.pvMaximo;
        String cor = pct > 0.5 ? VERDE : pct > 0.25 ? AMARELO : VERMELHO;
        escrever(String.format("  %s PV: %s%d/%d%s  |  Inimigos restantes: %s%d%s",
            NEGRITO, cor, p.pv, p.pvMaximo, RESET, VERMELHO, inimigosVivos, RESET));
    }

    private String escolherConsumivel(Personagem p) {
        List<String> consumiveis = p.inventario.stream()
            .filter(i -> { DadosItem it = itens.get(i); return it != null && it.tipo().equals("consumivel"); })
            .toList();
        if (consumiveis.isEmpty()) return null;
        escrever(CIANO + "  Escolha o item para usar:" + RESET);
        for (int i = 0; i < consumiveis.size(); i++)
            escrever(String.format("  [%d] %s", i + 1, consumiveis.get(i)));
        int op = lerOpcao("  > ", 1, consumiveis.size());
        return consumiveis.get(op - 1);
    }

    private void aplicarEfeito(String nomeItem, Personagem p) {
        DadosItem it = itens.get(nomeItem);
        if (it == null) return;
        String ef = it.efeito();
        if (ef.startsWith("curar")) {
            int val = Integer.parseInt(ef.replaceAll("[^0-9]", ""));
            int antes = p.pv;
            p.pv = Math.min(p.pvMaximo, p.pv + val);
            escrever(String.format(VERDE + "  ✚ Cura aplicada: +%d PV  (%d → %d)" + RESET,
                p.pv - antes, antes, p.pv));
        } else if (ef.startsWith("fortalecer")) {
            escrever(ROXO + "  ✨ Fortalecimento aplicado: " + ef + RESET);
        }
    }

    //  TRADUCOES

    private String traduzirClasse(String classe) {
        if (classe == null) return "Desconhecido";
        return switch (classe) {
            case "guerreiro"   -> "Guerreiro";
            case "mago"        -> "Mago";
            case "ladrao"      -> "Ladrao";
            case "clerigo"     -> "Clerigo";
            case "patrulheiro" -> "Patrulheiro";
            default            -> classe;
        };
    }

    private String emojiClasse(String classe) {
        if (classe == null) return "⚔";
        return switch (classe) {
            case "guerreiro"   -> "⚔️";
            case "mago"        -> "🔮";
            case "ladrao"      -> "🗡";
            case "clerigo"     -> "✨";
            case "patrulheiro" -> "🏹";
            default            -> "⚔";
        };
    }

    private String traduzirAtributo(String attr) {
        return switch (attr) {
            case "forca"        -> "Forca";
            case "destreza"     -> "Destreza";
            case "inteligencia" -> "Inteligencia";
            case "sabedoria"    -> "Sabedoria";
            case "carisma"      -> "Carisma";
            case "constituicao" -> "Constituicao";
            default             -> attr;
        };
    }

    private String traduzirAtributoAbrev(String attr) {
        return switch (attr) {
            case "forca"        -> "FOR";
            case "destreza"     -> "DES";
            case "inteligencia" -> "INT";
            case "sabedoria"    -> "SAB";
            case "carisma"      -> "CAR";
            case "constituicao" -> "CON";
            default             -> attr.substring(0, Math.min(3, attr.length())).toUpperCase();
        };
    }

    private String traduzirDificuldade(String dif) {
        return switch (dif) {
            case "facil"   -> "Facil";
            case "medio"   -> "Medio";
            case "dificil" -> "Dificil";
            case "letal"   -> "Letal";
            default        -> dif;
        };
    }

    //  UTILITARIOS GERAIS

    private int rolarDado(int faces)  { return sorteio.nextInt(faces) + 1; }

    private int modificador(int stat) { return (stat - 10) / 2; }
    private boolean grupoVivo()       { return personagens.values().stream().anyMatch(p -> p.pv > 0); }

    private int modificadorDificuldade(String dif) {
        return switch (dif) {
            case "facil"   -> 0;
            case "medio"   -> 1;
            case "dificil" -> 2;
            case "letal"   -> 4;
            default        -> 1;
        };
    }

    private String barraAtributo(int val) {
        int blocos = Math.max(0, Math.min(20, val)) / 2;
        return CIANO + "█".repeat(blocos) + RESET + "░".repeat(10 - blocos) + " " + val;
    }

    private String formatarInimigos(List<Inimigo> inimigos) {
        StringBuilder sb = new StringBuilder();
        for (Inimigo i : inimigos)
            sb.append(i.nome()).append(" x").append(i.quantidade()).append("  ");
        return sb.toString().trim();
    }

    private String extrairArgString(String passo) {
        int ini = passo.indexOf('"'), fim = passo.lastIndexOf('"');
        if (ini >= 0 && fim > ini) return passo.substring(ini + 1, fim);
        return passo;
    }

    private void separador()               { escrever(BRANCO + "  ─────────────────────────────────────────" + RESET); }
    private void escrever(String s)        { System.out.println(s); }
    private void imprimirSemQuebra(String s) { System.out.print(s); System.out.flush(); }
    private void limparTela()             { System.out.print("\033[H\033[2J"); System.out.flush(); }
    private void aguardar(int ms)         { try { Thread.sleep(ms); } catch (InterruptedException ignorado) {} }
}

