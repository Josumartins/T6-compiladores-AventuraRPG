grammar AventuraRPG;

programa
    : campanha EOF
    ;

campanha
    : CAMPANHA STRING ACHAVE corposCampanha* FECHAVE
    ;

corposCampanha
    : declPersonagem
    | declItem
    | declEncontro
    | declMissao
    ;

// ── PERSONAGEM ───

declPersonagem
    : PERSONAGEM ID ACHAVE campoPersonagem* FECHAVE
    ;

campoPersonagem
    : CLASSE DOISPONTOS tipoClasse                  # perClasse
    | NIVEL DOISPONTOS INT                              # perNivel
    | PV DOISPONTOS INT                                 # perPv
    | ATRIBUTOS ACHAVE listaAtributos FECHAVE           # perAtributos
    | INVENTARIO ACHAVE listaRefs FECHAVE               # perInventario
    ;

tipoClasse
    : GUERREIRO | MAGO | LADRAO | CLERIGO | PATRULHEIRO
    ;

listaAtributos
    : entradaAtributo (VIRGULA entradaAtributo)*
    ;

entradaAtributo
    : nomeAtributo DOISPONTOS INT
    ;

nomeAtributo
    : FORCA | DESTREZA | INTELIGENCIA | SABEDORIA | CARISMA | CONSTITUICAO
    ;

listaRefs
    : STRING (VIRGULA STRING)*
    ;

// ── ITEM ───

declItem
    : ITEM STRING ACHAVE campoItem* FECHAVE
    ;

campoItem
    : TIPO DOISPONTOS tipoItem                          # itemTipo
    | EFEITO DOISPONTOS efeito                          # itemEfeito
    | VALOR DOISPONTOS INT                              # itemValor
    ;

tipoItem
    : CONSUMIVEL | ARMA | ARMADURA | ITEM_CHAVE | MISC
    ;

efeito
    : CURAR APAREN INT FPAREN
    | DANIFICAR APAREN INT FPAREN
    | FORTALECER APAREN nomeAtributo VIRGULA INT FPAREN
    | DESBLOQUEAR APAREN STRING FPAREN
    ;

// ── ENCONTRO ──

declEncontro
    : ENCONTRO STRING ACHAVE campoEncontro* FECHAVE
    ;

campoEncontro
    : INIMIGOS ACHAVE listaInimigos FECHAVE             # encontroInimigos
    | RECOMPENSA ACHAVE listaRecompensa FECHAVE         # encontroRecompensa
    | DIFICULDADE DOISPONTOS nivelDificuldade           # encontroDificuldade
    ;

listaInimigos
    : entradaInimigo (VIRGULA entradaInimigo)*
    ;

entradaInimigo
    : ID MULT?
    ;

listaRecompensa
    : entradaRecompensa (VIRGULA entradaRecompensa)*
    ;

entradaRecompensa
    : STRING MULT?                                      # recompItem
    | OURO DOISPONTOS INT                               # recompOuro
    | XP DOISPONTOS INT                                 # recompXp
    ;

nivelDificuldade
    : FACIL | MEDIO | DIFICIL | LETAL
    ;

// ── MISSAO ─

declMissao
    : MISSAO STRING ACHAVE campoMissao* FECHAVE
    ;

campoMissao
    : PASSOS ACHAVE passo* FECHAVE                      # missaoPassos
    | RECOMPENSA ACHAVE listaRecompensa FECHAVE         # missaoRecompensa
    | REQUER ACHAVE listaRequer FECHAVE                 # missaoRequer
    ;

passo
    : ENTRAR STRING                                     # passoEntrar
    | COLETAR STRING                                    # passosColetar
    | RETORNAR STRING                                   # passoRetornar
    | FALAR_COM ID                                      # passoFalar
    | USAR STRING                                       # passoUsar
    ;

listaRequer
    : STRING (VIRGULA STRING)*
    ;


//  REGRAS LEXER — Palavras-chave e Pontuação
CAMPANHA        : 'campanha'        ;
PERSONAGEM      : 'personagem'      ;
ITEM            : 'item'            ;
ENCONTRO        : 'encontro'        ;
MISSAO          : 'missao'          ;

CLASSE          : 'classe'          ;
NIVEL           : 'nivel'           ;
PV              : 'pv'              ;
ATRIBUTOS       : 'atributos'       ;
INVENTARIO      : 'inventario'      ;

TIPO            : 'tipo'            ;
EFEITO          : 'efeito'          ;
VALOR           : 'valor'           ;

INIMIGOS        : 'inimigos'        ;
RECOMPENSA      : 'recompensa'      ;
DIFICULDADE     : 'dificuldade'     ;

PASSOS          : 'passos'          ;
REQUER          : 'requer'          ;

GUERREIRO       : 'guerreiro'       ;
MAGO            : 'mago'            ;
LADRAO          : 'ladrao'          ;
CLERIGO         : 'clerigo'         ;
PATRULHEIRO     : 'patrulheiro'     ;

CONSUMIVEL      : 'consumivel'      ;
ARMA            : 'arma'            ;
ARMADURA        : 'armadura'        ;
ITEM_CHAVE      : 'item_chave'      ;
MISC            : 'misc'            ;

CURAR           : 'curar'           ;
DANIFICAR       : 'danificar'       ;
FORTALECER      : 'fortalecer'      ;
DESBLOQUEAR     : 'desbloquear'     ;

FACIL           : 'facil'           ;
MEDIO           : 'medio'           ;
DIFICIL         : 'dificil'         ;
LETAL           : 'letal'           ;

FORCA           : 'forca'           ;
DESTREZA        : 'destreza'        ;
INTELIGENCIA    : 'inteligencia'    ;
SABEDORIA       : 'sabedoria'       ;
CARISMA         : 'carisma'         ;
CONSTITUICAO    : 'constituicao'    ;

ENTRAR          : 'entrar'          ;
COLETAR         : 'coletar'         ;
RETORNAR        : 'retornar'        ;
FALAR_COM       : 'falar_com'       ;
USAR            : 'usar'            ;

OURO            : 'ouro'            ;
XP              : 'xp'              ;

ACHAVE          : '{'               ;
FECHAVE         : '}'               ;
APAREN          : '('               ;
FPAREN          : ')'               ;
DOISPONTOS      : ':'               ;
VIRGULA         : ','               ;


//  REGRAS LEXER — Expressões Regulares / Padrões

MULT            : 'x' [0-9]+        ;
ID              : [a-zA-Z_][a-zA-Z0-9_]* ;
INT             : [0-9]+            ;
STRING          : '"' (~["\r\n])* '"' ;
WS              : [ \t\r\n]+        -> skip ;
COMENTARIO      : '//' ~[\r\n]* -> skip ;
BLOCO_COMENT    : '/*' .*? '*/'     -> skip ;
