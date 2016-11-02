/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.semantico;

import compilador.lexico.Token;
import compilador.sintatico.EndTokensException;
import compilador.sintatico.Funcao;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Jhone e Matheus Castro
 */
public class Semantico {
    private LinkedList<Token> tokens;
    private int atual;
    
    //variaveis usadas no Sematico 
    private String tipo;
    private Funcao funcao;
    private String parametro;
    private TabelaSimbolos simbolos;
    private LinkedList<Erro> erros;
    private LinkedList<Funcao> funcoes;
    private LinkedList<String> funcoesLidas;
    private int aritmeticaValor = 0;
    private boolean atribuicaoTipo[] = {false, false, false, false, false};//inteiro - real - booleano - caractere - cadeia
    private boolean expError =false, opArit = false;
    
    public Semantico() {
        
    }
 
    public void start(LinkedList<Token> tokens, LinkedList<Funcao> funcoes, String nomeArq) throws IOException{
        this.tokens = new LinkedList<Token>();
        this.erros = new LinkedList<Erro>();
        this.simbolos = new TabelaSimbolos(null);
        this.funcoes = funcoes;
        this.funcoesLidas = new LinkedList<String>();
        tipo =null;
        funcao =null;
        parametro = null;
        nomeArq = nomeArq.split(".txt")[0];
        atual=0;
        this.tokens.addAll(tokens);        
        //tirando os comentarios
        for(Token tok: tokens){
            if(tok.getTipo().equals("comentario"))
                this.tokens.remove(tok);
        }
        
        System.out.println("----------------- Analise Semantica -----------------\nArquivo: "+nomeArq+"\n");
        programa();
        HashMap<String, Funcao> funcoesAux = new HashMap<String, Funcao>();
        for(Funcao funcao: funcoes){
            if(!funcao.isOk())
                erros.add(new Erro("Instrucao de retorno nao encontrada",funcao.getLinha()));
            if (funcoesAux.containsKey(funcao.getNome()))
                erros.add(new Erro("Funcao \""+funcao.getNome()+"\" ja declarada",funcao.getLinha()));
            else
                funcoesAux.put(funcao.getNome(), funcao);
        }
        if (erros.isEmpty())
            System.out.println("Sucesso!");
        else{
            Collections.sort (erros, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Erro e1 = (Erro) o1;
                    Erro e2 = (Erro) o2;
                    return e1.getLinha() < e2.getLinha() ? -1 : (e1.getLinha() > e2.getLinha() ? +1 : 0);
                }
            });
            for(Erro error: erros){
                System.out.println(error.getLinha()+" "+error.getMensagem());               
            }
        }
        
        //gravar no txt
        File file = new File("saida/"+nomeArq+"_semantico.txt");

        if (!file.exists()){
            new File("saida/"+nomeArq+"_semantico.txt").createNewFile();
            file = new File("saida/"+nomeArq+"_semantico.txt");
        }

        PrintWriter gravarArq = new PrintWriter(new FileWriter(file));
        if (erros.isEmpty())
            gravarArq.printf("Sucesso!%n");
        else{
            for(Erro error: erros){
                gravarArq.printf(error.getLinha()+" "+error.getMensagem()+"%n");
            }
        }
        gravarArq.close();
    }
    //vê o token atual
    private Token ver() throws EndTokensException{
        return verLLX(0);
    }
    //vê qualquer token que vósmicê quiseres
    private Token verLLX(int x) throws EndTokensException{
        if (atual + x >= tokens.size())
            throw new EndTokensException("final não esperado");// mudar tipo de excecao depois
        return tokens.get(atual + x);
    }
    //consome o token atual incrementando o valor de "atual"
    private void consumir(){
        atual++;
    }
    
    //super equals
    private boolean igual(String... s){
        String referencia = s[0];
        for(int i=1; i<s.length;i++){
            if (referencia.equals(s[i])){
                return true;
            }
        }            
        return false;
    }
    /////////////////////////    BAGACEIRA    //////////////////////////
    
    // var c | c
    private void programa(){
        variaveis();
        c();
    }
    
    // c programa | programa
    private void c(){
        constantes();
        p();        
    }
    
    // programa | programa funcoes 
    private void p(){
        //programa p
        try {
            if (ver().getLexema().equals("programa")){
                consumir();
                bloco(simbolos);                
            }
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }  
        funcoes();
    }
    
    // funcao funcoes | vazio
    private void funcoes(){
        try {
            //System.out.println(ver().getLexema());// so pra checar se o arquivo acabou, se sim, dara a exception
            funcao_decl(simbolos);// tratar fim de arquivo dentro desta tb, pois ai seria erro
            funcoes();
        } catch (EndTokensException ex) {
            //fim de arquivo
        }  
    }
    
    ////////////////////////    variaveis    /////////////////////////////
    
    // var inicio <varlist> fim
    private void variaveis(){
        try {
            if (ver().getLexema().equals("var")){//se nao tem var pula pra c()                
                consumir();//consome var
                if (ver().getLexema().equals("inicio")){
                    consumir();
                    if (!ver().getLexema().equals("fim")) // caso esteja vazio
                        varlist(simbolos);// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                    else if (igual(ver().getLexema(),"programa", "const", "inicio")){
                        //nothing, deixar passar
                    }
                }              
                if (ver().getLexema().equals("fim")){
                    consumir();
                }
            } 
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }
    }

    // <tipo> <R> <varlist> | <>
    private void varlist(TabelaSimbolos simbolos) throws EndTokensException {
        if (igual(ver().getLexema(),"fim","programa","inicio")){
            return;
        }
        if (isTipo()){
            tipo = ver().getLexema();
            consumir();
            R(simbolos);
            varlist(simbolos);
        }
        
    }
    
    //id <vetor> <R2>
    private void R(TabelaSimbolos simbolos) throws EndTokensException {
        if (ver().getTipo().equals("identificador")){
            Token token = ver();
            id_vetor(simbolos);
            if(simbolos.get(token.getLexema())==null){//nao declarado
                simbolos.put(token.getLexema(), new Simbolo(token, tipo));//global
            }else{//já declarado
                erros.add(new Erro("Variavel \""+token.getLexema()+"\" ja declarada", token.getLinha()));
            }
            tipo = tipo.split("Vetor")[0];
            R2(simbolos);
        }
    }
    //','<R> | ';'
    private void R2(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals(",")){
            consumir();
            R(simbolos);
        } else if(ver().getLexema().equals(";")){
            consumir();
        } 
    }
    //'<<<'<Exp_Aritmetica><vetor2>'>>>' | <>
    private void vetor(TabelaSimbolos simbolos) throws EndTokensException {
        if(ver().getLexema().equals("<<<")){
            tipo = tipo+"Vetor";
            parametro = parametro+"Vetor";
            consumir();
            expError = false;
            exp_aritmetica(simbolos);  
            if(aritmeticaValor!=1 &&aritmeticaValor!=0){
                erros.add(new Erro("Vetor mal declarado, esperava um valor do tipo inteiro", ver().getLinha()));
            }
            aritmeticaValor = 0;
            vetor2(simbolos);
            if(ver().getLexema().equals(">>>")){
                consumir();                
            }
        }
    }
    
    //','<Exp_Aritmetica><Vetor2> | <>
    private void vetor2(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals(",")){
            consumir();
            expError = false;
            exp_aritmetica(simbolos);
            if(aritmeticaValor!=1 && aritmeticaValor!=0){
                erros.add(new Erro("Vetor mal declarado, esperava um valor do tipo inteiro", ver().getLinha()));
            }
            aritmeticaValor = 0;
            vetor2(simbolos);
        }
    }
    
    ////////////////////////    constantes    /////////////////////////////
    
    //const inicio <constlist> fim
    private void constantes(){
        try {
            if (ver().getLexema().equals("const")){//se nao tem var pula pra c()                
                consumir();//consome var
                if (ver().getLexema().equals("inicio")){
                    consumir();
                    if (!ver().getLexema().equals("fim"))// se nao tiver vazio
                        constlist(simbolos);// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                    else if (igual(ver().getLexema(),"programa","inicio")){
                        //nothing
                    }
                }               
                if (ver().getLexema().equals("fim")){
                    consumir();
                }
            } 
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }
    }
    
    //<Tipo><Const_Decl><Constlist> | <>
    private void constlist(TabelaSimbolos simbolos) throws EndTokensException{
        if (igual(ver().getLexema(),"fim","programa","inicio")){
            return;
        }
        if (isTipo()){
            tipo = ver().getLexema();
            consumir();
            const_decl(simbolos);
            constlist(simbolos);
        }       
    }
    
    //id'<<'<Literal><Const_Decl2>
    private void const_decl(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            if(simbolos.get(ver().getLexema())==null){//nao declarado
                simbolos.put(ver().getLexema(), new Simbolo(ver(), tipo, true));//global
            }else{//já declarado
                erros.add(new Erro("Variavel \""+ver().getLexema()+"\" ja declarada", ver().getLinha()));
            }
            consumir();  
            if(ver().getLexema().equals("<<")){
                consumir(); 
                if(isLiteral()){
                    if(!tipo.equals(checarTipo()) ){                        
                        if(!(tipo.equals("real")&&checarTipo().equals("inteiro")))
                            erros.add(new Erro("Tipos incompativeis, "+tipo+" e "+ver().getLexema(), ver().getLinha()));
                    }
                    consumir();
                    const_decl2(simbolos);
                }
            }          
        }
    }
    // ','id'<<'<Literal><Const_Decl2> | ';'
    private void const_decl2(TabelaSimbolos simbolos) throws EndTokensException{
        if (ver().getLexema().equals(",")){
            consumir();
            if(ver().getTipo().equals("identificador")){
                if(simbolos.get(ver().getLexema())==null){//nao declarado
                    simbolos.put(ver().getLexema(), new Simbolo(ver(), tipo, true));
                }else{//já declarado
                    erros.add(new Erro("Variavel \""+ver().getLexema()+"\" ja declarada", ver().getLinha()));
                }
                consumir();
                if (ver().getLexema().equals("<<")){
                    consumir();     
                    if(isLiteral()){
                        if(!tipo.equals(checarTipo()) ){                        
                            if(!(tipo.equals("real")&&checarTipo().equals("inteiro")))
                                erros.add(new Erro("Tipos incompativeis, "+tipo+" e "+ver().getLexema(), ver().getLinha()));
                        }
                        consumir();
                        const_decl2(simbolos);
                    }
                }
            }            
        }else if(ver().getLexema().equals(";")){
            consumir();
        }
    }
    
    ////////////////////////    Bloco      /////////////////////////////
    
    //'inicio'<Corpo_Bloco>'fim'
    private void bloco(TabelaSimbolos simbolosAnterior) throws EndTokensException{
        //escopo
        TabelaSimbolos simbolosNovo = new TabelaSimbolos(simbolosAnterior);
        
        if(ver().getLexema().equals("inicio")){
            consumir();
            corpo_bloco(simbolosNovo);
        }
        if(ver().getLexema().equals("fim")){
            consumir();
        }
    }
    //'inicio'<Corpo_Bloco>'fim'
    private void bloco(TabelaSimbolos simbolos, String nomeFuncao) throws EndTokensException{
        //escopo
        simbolos.setEscopo(nomeFuncao);
        if(ver().getLexema().equals("inicio")){
            consumir();
            corpo_bloco(simbolos);
        }
        if(ver().getLexema().equals("fim")){
            consumir();
        }
    }
    //<Var_Local><Corpo_Bloco> | <Comando><Corpo_Bloco> 
    // | <Chamada_Funcao>';'<Corpo_Bloco> | <Atribuicao><Corpo_Bloco> | <>
    private void corpo_bloco(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println("corpo_bloco");       
        
        if (igual(ver().getLexema(),"fim", "funcao")){            
            return;
        }
        if (igual(ver().getLexema(), "var","inteiro", "real", "booleano", "caractere", "cadeia")){
            var_local(simbolos);            
        } else if (igual(ver().getLexema(), "escreva", "leia", "se", "enquanto")){
            comando(simbolos);
        } else if (ver().getTipo().equals("identificador") ){
            if (verLLX(1).getLexema().equals("(")){
                chamada_funcao(simbolos);
                if(ver().getLexema().equals(";"))
                    consumir();
                else{
                    //panico - ;
                    //deixa passar somente ai corpo_bloco resolvera
                }
            }else{
                atribuicao(simbolos);
            }
        }
        corpo_bloco(simbolos);
    }
    ////////////////////////    variaveis locais    //////////////////////////
    
    //'var'<Tipo><Id_Vetor><Var_Local2>
    private void var_local(TabelaSimbolos simbolos) throws EndTokensException{
        if (ver().getLexema().equals("var")){
            consumir();
            if(isTipo()){
                tipo = ver().getLexema();
                consumir();
                Token token = ver();
                id_vetor(simbolos);
                if(simbolos.get(token.getLexema())==null){//nao declarado
                    simbolos.put(token.getLexema(), new Simbolo(token, tipo));//global
                }else{//já declarado
                    erros.add(new Erro("Variavel \""+token.getLexema()+"\" ja declarada", token.getLinha()));
                }
                tipo = tipo.split("Vetor")[0];
                var_local2(simbolos);
            }
        }
    }
    
    //','<Id_Vetor><Var_Local2> | ';'
    private void var_local2(TabelaSimbolos simbolos) throws EndTokensException{
        if (ver().getLexema().equals(",")){
            consumir();
            Token token = ver();
            id_vetor(simbolos);
            if(simbolos.get(token.getLexema())==null){//nao declarado
                simbolos.put(token.getLexema(), new Simbolo(token, tipo));//global
            }else{//já declarado
                erros.add(new Erro("Variavel \""+token.getLexema()+"\" ja declarada", token.getLinha()));
            }
            tipo = tipo.split("Vetor")[0];
            var_local2(simbolos);
        }else if(ver().getLexema().equals(";")){
            consumir();
        }
    }
    ////////////////////////    comandos    /////////////////////////////
    
    //<Escreva> | <Leia> | <Se> | <Enquanto>
    private void comando(TabelaSimbolos simbolos) throws EndTokensException{              
        if (ver().getLexema().equals("escreva")){
            escreva(simbolos);
        }else if(ver().getLexema().equals("leia")){
            leia(simbolos);
        }else if (ver().getLexema().equals("se")){
            se(simbolos);
        }else if (ver().getLexema().equals("enquanto")){
            enquanto(simbolos);
        }        
    }
    
    //'escreva''('<Escreva_Params>')'';'
    private void escreva(TabelaSimbolos simbolos) throws EndTokensException{
        if (ver().getLexema().equals("escreva")){               
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                escreva_params(simbolos);
                if(ver().getLexema().equals(")"))
                    consumir();
                
                if(ver().getLexema().equals(";"))
                    consumir();
                
            }
        }     
    }
    //<Exp_Aritmetica><Escreva_Param2> | caractere_t<Escreva_Param2> | cadeia_t<Escreva_Param2>
    private void escreva_params(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" escreva_params");
        if(ver().getTipo().equals("cadeia")){
            consumir();
            escreva_params2(simbolos);
        } else if(ver().getTipo().equals("caractere")){
            consumir();
            escreva_params2(simbolos);
        }else{
            expError = false;
            exp_aritmetica(simbolos); 
            escreva_params2(simbolos);
        }
    }

    //<Escreva_Param2> ::= ','<Escreva_Params> | <>
    private void escreva_params2(TabelaSimbolos simbolos) throws EndTokensException {
        if(ver().getLexema().equals(","))
            escreva_params(simbolos);    
    }
    
    //'leia''('<Leia_Params>')'';'    
    private void leia(TabelaSimbolos simbolos) throws EndTokensException{
        if (ver().getLexema().equals("leia")){               
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                leia_params(simbolos);
                if(ver().getLexema().equals(")"))
                    consumir();
                
                if(ver().getLexema().equals(";"))
                    consumir();
                
            }
        }        
    }
    
    //<Id_Vetor><Leia_Param2>
    private void leia_params(TabelaSimbolos simbolos) throws EndTokensException {
        if(ver().getLexema().equals(")"))
            return;
        else{
            TabelaSimbolos simbs = simbolos;
            Simbolo simb = null;
            Token token = ver();
            //verificar variavel em escopos
            while (simbs!=null && simb==null){
                simb = simbs.get(token.getLexema());
                simbs = simbs.getAnterior();
            }
            tipo = "";
            id_vetor(simbolos);
            if(simb!=null){// declarado
                if(!simb.isRetornoFuncao()){
                    if (!(tipo.contains("Vetor") && simb.getTipo().contains("Vetor") 
                            || !tipo.contains("Vetor") && !simb.getTipo().contains("Vetor"))){//sao diferentes
                        if(tipo.equals(""))
                            tipo = simb.getTipo().split("Vetor")[0];
                        erros.add(new Erro("Tipos incompativeis, "+tipo+" declarado como "+simb.getTipo(), token.getLinha()));
                    }else{
                        simb.setInicializado(true);
                    }
                }else{
                    erros.add(new Erro("Variavel \""+token.getLexema()+"\" nao declarada", token.getLinha()));
                }
            }else{//nao declarado
                erros.add(new Erro("Variavel \""+token.getLexema()+"\" nao declarada", token.getLinha()));
            }
            leia_params2(simbolos);
        }
    }
    
    //','<Leia_Params> | <>
    private void leia_params2(TabelaSimbolos simbolos) throws EndTokensException {
        if(ver().getLexema().equals(",")){
            consumir();
            leia_params(simbolos);
        }     
    }
    
    //'se''('<Exp_Logica>')''entao'<Bloco><Senao>
    private void se(TabelaSimbolos simbolos) throws EndTokensException {
        if (ver().getLexema().equals("se")){
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                exp_logica(simbolos);
                if(!atribuicaoTipo[2]){
                    erros.add(new Erro("Condicao invalida, espava valor booleano", ver().getLinha()));
                }
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("entao")){
                        consumir();
                        bloco(simbolos);
                        senao(simbolos);
                    }
                }
            }            
        }
    }
    
    //<Senao> ::= 'senao'<Bloco> | <>
    private void senao(TabelaSimbolos simbolos) throws EndTokensException {
        if(ver().getLexema().equals("senao")){
            consumir();
            bloco(simbolos);
        }
            
    }
    
    //<Enquanto> ::= 'enquanto''('<Exp_Logica>')''faca'<Bloco>
    private void enquanto(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals("enquanto")){
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                exp_logica(simbolos);
                if(!atribuicaoTipo[2]){
                    erros.add(new Erro("Condicao invalida, espava valor booleano", ver().getLinha()));
                }
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("faca")){
                        consumir();
                        bloco(simbolos);
                    }
                }    
            }
        }
    }
    ////////////////////////    atribuicao    /////////////////////////////
    
    //<Id_Vetor>'<<'<Valor>';'
    private void atribuicao(TabelaSimbolos simbolos) throws EndTokensException{
        TabelaSimbolos simbs = simbolos;
        Simbolo simb = null;
        Token token = ver();
        //verificar variavel em escopos
        while (simbs!=null && simb==null){
            simb = simbs.get(token.getLexema());
            simbs = simbs.getAnterior();
        }
        tipo = "";
        id_vetor(simbolos);
        if(simb!=null){// declarado
            if (!(tipo.contains("Vetor") && simb.getTipo().contains("Vetor") 
                    || !tipo.contains("Vetor") && !simb.getTipo().contains("Vetor"))){//sao diferentes
                if(tipo.equals(""))
                    tipo = simb.getTipo().split("Vetor")[0];
                erros.add(new Erro("Tipos incompativeis, "+tipo+" declarado como "+simb.getTipo(), token.getLinha()));
            }
            
            if(ver().getLexema().equals("<<")){
                consumir();
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                valor(simbolos);
                String tipoAtribuicao = "vazio";
                if(atribuicaoTipo[2]==true)
                    tipoAtribuicao = "booleano";
                else if(atribuicaoTipo[4]==true)
                    tipoAtribuicao = "cadeia";
                else if(atribuicaoTipo[3]==true)
                    tipoAtribuicao = "caractere";
                else if(atribuicaoTipo[1]==true)
                    tipoAtribuicao = "real";
                else if(atribuicaoTipo[0]==true)
                    tipoAtribuicao = "inteiro";
                atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
                tipo = simb.getTipo().split("Vetor")[0];
                if(!tipo.equals(tipoAtribuicao))
                    erros.add(new Erro("Atribuicao mal feita, esperava um valor "+tipo, ver().getLinha()));
                else{
                    simb.setInicializado(true);
                }

                tipoAtribuicao = null;
                tipo="";
                if(ver().getLexema().equals(";")){
                    consumir();
                }
            }
        }else{
            erros.add(new Erro("Variavel \""+token.getLexema()+"\" nao declarada", token.getLinha()));
            if(ver().getLexema().equals("<<")){
                consumir();
                valor(simbolos);
                if(ver().getLexema().equals(";")){
                    consumir();
                }
            }
            
        }
        
    }
    
    ////////////////////////    funcao    /////////////////////////////
    
    //'funcao'<Funcao_Decl2>id'('<Param_Decl_List>')'<Bloco>
    private void funcao_decl(TabelaSimbolos simbolosAnterior) throws EndTokensException {
        TabelaSimbolos simbolos = new TabelaSimbolos(simbolosAnterior);
        if(ver().getLexema().equals("funcao")){
            consumir();
            String tipo = funcao_decl2();            
            if(ver().getTipo().equals("identificador")){
                String nomeFuncao = ver().getLexema();
                funcoesLidas.add(nomeFuncao);
                if (tipo != null){
                    Simbolo si = new Simbolo(new Token(nomeFuncao, 2, true),tipo);
                    si.setRetornoFuncao(true);
                    simbolos.put(nomeFuncao, si);
                }
                consumir();
                if(ver().getLexema().equals("(")){
                    consumir();
                    param_decl_list(simbolos);
                    if(ver().getLexema().equals(")")){
                        consumir();
                        bloco(simbolos, nomeFuncao);
                    }
                }
            }
        }
    }
    
    //<Tipo> | <>
    private String funcao_decl2() throws EndTokensException{
        String tipo =null;
        if(isTipo()){
            tipo = ver().getLexema();
            consumir();
        }
        return tipo;
    }
    
    //<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list(TabelaSimbolos simbolos) throws EndTokensException{
        if(igual(ver().getLexema(), "inicio", "var", 
                                "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(")"))//sem parametros
            return;
        if(isTipo()){
            tipo = ver().getLexema();
            consumir();
            Token token = ver();
            id_vetor(simbolos);
            if(simbolos.get(token.getLexema())==null){//nao declarado
                Simbolo sim = new Simbolo(token, tipo);
                sim.setInicializado(true);
                simbolos.put(token.getLexema(), sim);
            }else{//já declarado
                erros.add(new Erro("Variavel \""+token.getLexema()+"\" ja declarada", token.getLinha()));
            }
            tipo = tipo.split("Vetor")[0];
            param_decl_list2(simbolos);
        }
    }

    //','<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list2(TabelaSimbolos simbolos) throws EndTokensException{
        if(igual(ver().getLexema(), ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(",")){
            consumir();
            if(isTipo()){
                tipo = ver().getLexema();
                consumir();
                Token token = ver();
                id_vetor(simbolos);
                if(simbolos.get(token.getLexema())==null){//nao declarado
                    Simbolo sim = new Simbolo(token, tipo);
                    sim.setInicializado(true);
                    simbolos.put(token.getLexema(), sim);
                }else{//já declarado
                    erros.add(new Erro("Variavel \""+token.getLexema()+"\" ja declarada", token.getLinha()));
                }
                tipo = tipo.split("Vetor")[0];
                param_decl_list2(simbolos);
            }
        }
    }
    
    //id'('<Param_Cham_List>')'
    private void chamada_funcao(TabelaSimbolos simbolos) throws EndTokensException{
        Funcao funcao = funcoesGet(ver().getLexema());
        if(ver().getTipo().equals("identificador")){
            consumir();
        }
        if(ver().getLexema().equals("(")){
            consumir();
            param_cham_list(simbolos, funcao);
        }
        if(ver().getLexema().equals(")"))
            consumir();
        
    }
    
    //<Valor><Param_Cham_List2> | <>
    private void param_cham_list(TabelaSimbolos simbolos, Funcao funcao) throws EndTokensException{
        //System.out.println(ver().getLexema()+" param_cham_list");
        String isVetor ="";
        boolean paramError = false;
        if(ver().getLexema().equals(")"))
            return;
        atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
        if(ver().getTipo().equals("identificador") && (verLLX(1).getLexema().equals(",")||verLLX(1).getLexema().equals(")"))){
            TabelaSimbolos simbs = simbolos;
            Simbolo simb = null;
            Token token = ver();
            //verificar variavel em escopos
            while (simbs!=null && simb==null){
                simb = simbs.get(token.getLexema());
                simbs = simbs.getAnterior();
            }
            if(simb!=null){
                if (simb.getTipo().contains("Vetor"))
                    isVetor ="Vetor";
            }
        }
            
        valor(simbolos);
        String tipoAtribuicao = "vazio";
        if(atribuicaoTipo[2]==true)
            tipoAtribuicao = "booleano";
        else if(atribuicaoTipo[4]==true)
            tipoAtribuicao = "cadeia";
        else if(atribuicaoTipo[3]==true)
            tipoAtribuicao = "caractere";
        else if(atribuicaoTipo[1]==true)
            tipoAtribuicao = "real";
        else if(atribuicaoTipo[0]==true)
            tipoAtribuicao = "inteiro";
        tipoAtribuicao = tipoAtribuicao.concat(isVetor);
        atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
        if(funcao!=null){
            LinkedList<String> parametros = funcao.getParametros();
                if(!parametros.isEmpty()){
                    String tipo = parametros.getFirst();
                    if(!tipo.equals(tipoAtribuicao)){
                        erros.add(new Erro("Parametro invalido, esperava um valor "+tipo, ver().getLinha()));
                        paramError=true;
                    }
                }else{
                    erros.add(new Erro("Parametros inesperados, funcao "+funcao.getNome(), ver().getLinha()));
                    paramError =true;
                }
        }   
        //System.out.println(ver().getLexema()+" param_cham_list2");
        param_cham_list2(simbolos, funcao, paramError, 1);
    }
    
    //','<Valor><Param_Cham_List2> | <>
    private void param_cham_list2(TabelaSimbolos simbolos, Funcao funcao, boolean paramError, int index) throws EndTokensException{
        if(igual(ver().getLexema(), ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(",")){
            consumir();
            String isVetor ="";
            atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
            if(ver().getTipo().equals("identificador") && (verLLX(1).getLexema().equals(",")||verLLX(1).getLexema().equals(")"))){
                TabelaSimbolos simbs = simbolos;
                Simbolo simb = null;
                Token token = ver();
                //verificar variavel em escopos
                while (simbs!=null && simb==null){
                    simb = simbs.get(token.getLexema());
                    simbs = simbs.getAnterior();
                }
                if(simb!=null){
                    if (simb.getTipo().contains("Vetor"))
                        isVetor ="Vetor";
                }
            }

            valor(simbolos);
            String tipoAtribuicao = "vazio";
            if(atribuicaoTipo[2]==true)
                tipoAtribuicao = "booleano";
            else if(atribuicaoTipo[4]==true)
                tipoAtribuicao = "cadeia";
            else if(atribuicaoTipo[3]==true)
                tipoAtribuicao = "caractere";
            else if(atribuicaoTipo[1]==true)
                tipoAtribuicao = "real";
            else if(atribuicaoTipo[0]==true)
                tipoAtribuicao = "inteiro";
            tipoAtribuicao = tipoAtribuicao.concat(isVetor);
            atribuicaoTipo[0]=atribuicaoTipo[1]=atribuicaoTipo[2]=atribuicaoTipo[3]=atribuicaoTipo[4]=false;
            if(funcao!=null){
                LinkedList<String> parametros = funcao.getParametros();
                if(!parametros.isEmpty()){
                    String tipo = parametros.get(index);
                    if(!tipo.equals(tipoAtribuicao)){
                        if(!paramError){
                            erros.add(new Erro("Parametro invalido, esperava um valor "+tipo, ver().getLinha()));
                            paramError =true;
                        }
                    }
                }else{
                    if(!paramError){
                        erros.add(new Erro("Parametros inesperados, funcao "+funcao.getNome(), ver().getLinha()));
                        paramError =true;
                    }
                }
            }
            index++;
            param_cham_list2(simbolos, funcao, paramError, index);            
        }
    }
    
    ////////////////////////    expressoes    /////////////////////////////
    
    //<Exp_Aritmetica2><Exp_SomaSub>
    private void exp_aritmetica(TabelaSimbolos simbolos) throws EndTokensException {
        exp_aritmetica2(simbolos);
        exp_somasub(simbolos);
    }
    
    //<Operador_A1><Exp_Aritmetica2><Exp_SomaSub> | <>
    private void exp_somasub(TabelaSimbolos simbolos) throws EndTokensException{
        if (igual(ver().getLexema(),"+", "-")){//<operador_a1> ::= + | -
            consumir();
            exp_aritmetica2(simbolos);
            exp_somasub(simbolos);
        }        
    }
    
    //<Valor_Numerico><Exp_MultDiv>
    private void exp_aritmetica2(TabelaSimbolos simbolos) throws EndTokensException{
        valor_numerico(simbolos);
        exp_muldiv(simbolos);
    }
    
    //<Operador_A2><Valor_Numerico><Exp_MultDiv> | <>
    private void exp_muldiv(TabelaSimbolos simbolos) throws EndTokensException{
        if (igual(ver().getLexema(),"/", "*")){//<operador_a2> ::= * | /
            //if(aritmeticaValor==0){//o anterior ao + ou - nao foi um numero
            //    if(!expError){
            //        erros.add(new Erro("Expressao mal formada",ver().getLinha()));
            //        expError = true;
            //    }
            //}
            //opArit = true;
            consumir();
            valor_numerico(simbolos);
            exp_muldiv(simbolos);
        }
    }
    
    //'('<Exp_Aritmetica>')' | <Id_Vetor> | <Chamada_Funcao> | numero_t
    private void valor_numerico(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals("(")){
            consumir();
            exp_aritmetica(simbolos);
            if(ver().getLexema().equals(")")){
                consumir();
            }
        }else if(ver().getTipo().equals("identificador")){
            if(verLLX(1).getLexema().equals("(")){                
                chamada_funcao(simbolos);
                Funcao funcao = funcoesGet(ver().getLexema());
                String tipo = funcao.getTipoRetorno().split("Vetor")[0];
                if(funcao!=null){//funcao existe
                    if(tipo.equals("inteiro")){
                        aritmeticaValor = 1;
                    }else if(tipo.equals("real")){
                        aritmeticaValor = 2;
                    }else{//nao é um numero
                        if(!expError){
                            erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        aritmeticaValor = 0;
                    }
                }else{
                    erros.add(new Erro("Funcao nao declarada", ver().getLinha()));
                    aritmeticaValor = 0;
                }
            }else{
                Simbolo simb = null;
                TabelaSimbolos simbs = simbolos;
                //verificar variavel em escopos
                while (simbs!=null && simb==null){
                    simb = simbs.get(ver().getLexema());
                    simbs = simbs.getAnterior();
                }
                if(simb!=null){  
                    String tipo = simb.getTipo().split("Vetor")[0];
                    if(simb.isRetornoFuncao()){
                        erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao declarada", ver().getLinha()));
                        aritmeticaValor = 0;
                    }else if(tipo.equals("inteiro") && simb.isInicializado()){
                        aritmeticaValor = 1;
                    }else if(tipo.equals("real")&& simb.isInicializado()){
                        aritmeticaValor = 2;
                    }else if((tipo.equals("real")||tipo.equals("inteiro"))&&!simb.isInicializado()){
                        aritmeticaValor = 0;
                        erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao inicializada", ver().getLinha()));
                    }else {
                        if(!expError){
                            erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        aritmeticaValor = 0;
                    }
                }else{
                    erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao declarada", ver().getLinha()));
                    aritmeticaValor = 0;
                }
                id_vetor(simbolos);
            }
        }else if (ver().getTipo().equals("numero")){
            if(checarTipo().equals("inteiro"))
                aritmeticaValor = 1;
            else
                aritmeticaValor = 2;
            consumir();
        }
            
    }
    
    //<Exp_Logica2><Exp_Ou>
    private void exp_logica(TabelaSimbolos simbolos) throws EndTokensException {
        exp_logica2(simbolos);
        exp_ou(simbolos);
        if(igual(ver().getLexema(), "=", "<>")){
            atribuicaoTipo[2]=true;
            consumir();
            exp_logica(simbolos);
        }
    }
    
    //'ou'<Exp_Logica2><Exp_Ou> | <>
    private void exp_ou(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals("ou")){
            atribuicaoTipo[2]=true;
            consumir();
            exp_logica2(simbolos);
            exp_ou(simbolos);
        }
    }
    
    //<Exp_Nao><Exp_E>
    private void exp_logica2(TabelaSimbolos simbolos) throws EndTokensException{
        exp_nao(simbolos);
        exp_e(simbolos);
    }
    
    //'e'<Exp_Nao><Exp_E> | <>
    private void exp_e(TabelaSimbolos simbolos) throws EndTokensException{
        if(ver().getLexema().equals("e")){
            atribuicaoTipo[2]=true;
            consumir();
            exp_nao(simbolos);
            exp_e(simbolos);
        }
    }
    
    //'nao'<Valor_Booleano> | <Valor_Booleano>
    private void exp_nao(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" nao");
        if(ver().getLexema().equals("nao")){
            atribuicaoTipo[2]=true;
            consumir();
            valor_booleano(simbolos);
        }else
            valor_booleano(simbolos);
    }
    
    //booleano_t | <Exp_Relacional>
    private void valor_booleano(TabelaSimbolos simbolos) throws EndTokensException{
        if(igual(ver().getLexema(),"verdadeiro", "falso")){
            atribuicaoTipo[2]=true;
            consumir();
        }else
            exp_relacional(simbolos);
    }
    
    //<Exp_Aritm_Logica><Exp_Relacional2>
    private void exp_relacional(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" exp_relacional");
        exp_arim_logica(simbolos);
        exp_relacional2(simbolos);
    }
    
    //<Operador_Relacional><Exp_Aritm_Logica> | <>
    private void exp_relacional2(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" exp_relacional 2");
        if(igual(ver().getLexema(), "<", "<=", ">", ">=", "<>", "=")){
            atribuicaoTipo[2]=true;
            consumir();
            exp_arim_logica(simbolos);
        }
    }
    
    //<Exp_Aritm_Logica2><Exp_SomaSub_Logica>
    private void exp_arim_logica(TabelaSimbolos simbolos) throws EndTokensException{
        exp_arim_logica2(simbolos);
        exp_somasub_logica(simbolos);
    }
    
    //<Operador_A1><Exp_Aritm_Logica2><Exp_SomaSub_Logica> | <>
    private void exp_somasub_logica(TabelaSimbolos simbolos) throws EndTokensException{
        if(igual(ver().getLexema(), "+", "-")){
            opArit =true;
            consumir();
            exp_arim_logica2(simbolos);
            exp_somasub_logica(simbolos);
        }        
    }
        
    //<Numerico_Logico><Exp_MultDiv_Logica>
    private void exp_arim_logica2(TabelaSimbolos simbolos) throws EndTokensException{
        numero_logico(simbolos);
        exp_multdiv_logica(simbolos);
    }
    
    //<Operador_A2><Numerico_Logico><Exp_MultDiv_Logica> | <>
    private void exp_multdiv_logica(TabelaSimbolos simbolos) throws EndTokensException{
        if(igual(ver().getLexema(), "/", "*")){
            opArit =true;
            consumir();
            numero_logico(simbolos);
            exp_multdiv_logica(simbolos);
        }
    }
    
    //'('<Exp_Aritm_Logica>')' | <Id_Vetor> | <Chamada_Funcao> | numero_t
    private void numero_logico(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" numero_logico");
        if(ver().getLexema().equals("(")){
            consumir();
            if (igual(verLLX(1).getLexema(),"+", "-", "/", "*"))
                exp_arim_logica(simbolos);
            else
                exp_logica(simbolos);
            //System.out.println(ver().getLexema()+" -------------------------------------------");
            if(ver().getLexema().equals(")")){
                consumir();
            }
            exp_logica(simbolos);
        }else if(ver().getTipo().equals("identificador")){
            if(verLLX(1).getLexema().equals("(")){
                
                Funcao funcao = funcoesGet(ver().getLexema());
                
                if(funcao!=null){
                    String tipo = funcao.getTipoRetorno();
                    if(tipo.equals("inteiro")){
                        atribuicaoTipo[0]=true;//inteiro
                        opArit =false;
                    }else if(tipo.equals("real")){
                        atribuicaoTipo[1]=true;//real
                        opArit =false;
                    }else if(tipo.equals("booleano")){
                        atribuicaoTipo[2]=true;//booleano
                        if(opArit){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                    }else if(tipo.equals("caractere")){
                        atribuicaoTipo[3]=true;//caractere
                        if(opArit){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }            
                    }else if(tipo.equals("cadeia")){
                        atribuicaoTipo[4]=true;//cadeia
                        if(opArit){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){                            
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }                        
                    }
                }else{
                    erros.add(new Erro("Funcao \""+ver().getLexema()+"\" nao declarada",ver().getLinha()));
                }
                    
                chamada_funcao(simbolos);                
            }else{
                Simbolo simb = null;
                TabelaSimbolos simbs = simbolos;
                //verificar variavel em escopos
                while (simbs!=null && simb==null){
                    simb = simbs.get(ver().getLexema());
                    simbs = simbs.getAnterior();
                }
                if(simb!=null){
                    String tipo = simb.getTipo().split("Vetor")[0];
                    
                    if(tipo.equals("inteiro")){
                        atribuicaoTipo[0]=true;//inteiro
                        opArit =false;
                    }else if(tipo.equals("real")){
                        atribuicaoTipo[1]=true;//real
                        opArit =false;
                    }else if(tipo.equals("booleano")){   
                        atribuicaoTipo[2]=true;//booleano
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){                            
                            if(opArit){
                                if(!expError)
                                    erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                                expError = true;
                            }
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }                        
                    }else if(tipo.equals("caractere")){
                        atribuicaoTipo[3]=true;//caractere
                        if(opArit){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }                        
                    }else if(tipo.equals("cadeia")){
                        atribuicaoTipo[4]=true;//cadeia
                        if(opArit){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                            }
                        if (igual(verLLX(1).getLexema(),"+", "-", "/", "*")){
                            if(!expError)
                                erros.add(new Erro("Expressao mal formada, "+tipo+" inesperado", ver().getLinha()));
                            expError = true;
                        }                        
                    }
                    if(!simb.isRetornoFuncao()){
                        if(!simb.isInicializado())
                            erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao inicializada",ver().getLinha()));
                    }else
                        erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao declarada",ver().getLinha()));
                }else{
                    erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao declarada",ver().getLinha()));
                }
                id_vetor(simbolos);
            }
        }else if(ver().getTipo().equals("numero")){
            if(ver().getLexema().contains("."))
                atribuicaoTipo[1]=true;//real
            else
                atribuicaoTipo[0]=true;//inteiro
            consumir();
        }else if (igual(ver().getLexema(),"verdadeiro", "falso")){
            atribuicaoTipo[2]=true;
            valor_booleano(simbolos);
        }
    }
    ////////////////////////    outros    /////////////////////////////
    
    // inteiro | real | booleano | cadeia | caractere
    private boolean isTipo() throws EndTokensException{
        return igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia");
    }
    // caractere | cadeia | numero | verdadeiro | falso
    private boolean isLiteral() throws EndTokensException {
        return igual(ver().getTipo(), "caractere", "cadeia", "numero")|| igual(ver().getLexema(), "verdadeiro", "falso");
    }
    
    //usada para fins semanticos
    private String checarTipo() throws EndTokensException{
        String vTip = ver().getTipo();
        String vLex = ver().getLexema();
        String tipo = vTip;
        
        if(vTip.equals("numero")){
            if(vLex.contains(".")){
                tipo = "real";
            }else{
                tipo = "inteiro";
            }
        }else if(igual(vLex, "verdadeiro", "falso")){
            tipo = "booleano";
        }            
        return tipo;
    }
    //vetor definido em variaveis
    
    //<Exp_Logica> | caractere_t | cadeia_t
    private void valor(TabelaSimbolos simbolos) throws EndTokensException{
        //System.out.println(ver().getLexema()+" valor");
        if (igual(ver().getTipo(),"caractere", "cadeia")){
            if(ver().getTipo().equals("cadeia"))
                atribuicaoTipo[4] = true;
            else
                atribuicaoTipo[3] = true;
            consumir();
        }else{
            opArit =expError=false;
            exp_logica(simbolos);
            opArit =expError=false;
        }
    }
    //id<Vetor>
    private void id_vetor(TabelaSimbolos simbolos) throws EndTokensException{        
        if(ver().getTipo().equals("identificador")){
            consumir();
            if(verLLX(1).getLexema().equals("<<<")){
                if(!simbolos.get(ver().getLexema()).getTipo().contains("Vetor")){
                    erros.add(new Erro("Variavel \""+ver().getLexema()+"\" nao é um vetor",ver().getLinha()));
                }
            }
            vetor(simbolos);
        }
    }
    
    private boolean funcoesContainsKey(String id) {
        for(Funcao funcao: funcoes){
            if (funcao.getNome().equals(id))
                return true;
        }
        return false;
    }

    private Funcao funcoesGet(String id) {
        Funcao r = null;
        for(Funcao funcao: funcoes){
            if (funcao.getNome().equals(id))
                r = funcao;
        }
        return r;
    }
}

