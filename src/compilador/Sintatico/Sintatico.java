/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.Sintatico;

import compilador.Lexico.Token;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Jhone e Matheus Castro
 */
public class Sintatico {
    private LinkedList<Token> tokens;
    private LinkedList<String> erros;
    private int atual;

    public Sintatico() {
        erros = new LinkedList<String>();
    }
 
    public void start(LinkedList tokens){
        this.tokens = tokens;     
        atual=0;
        var();
    }
    //vê o token atual
    private Token ver(){
        return verLLX(1);
    }
    //vê qualquer token que vósmicê quiseres
    private Token verLLX(int x){
        if (atual + x >= tokens.size())
            throw new RuntimeException("final não esperado");// mudar tipo de excecao depois
        return tokens.get(atual + x);
    }
    //consome o token atual incrementando o valor de "atual"
    private void consume(){
        atual++;
    }
    // modo panico
    // vai consumir ate o atual se tornar um token de sync passado por parametro
    private void sincronizar(String... syncC){   
        List<String> sync = Arrays.asList(syncC);
    	while(!sync.contains(ver().getLexema()) || !sync.contains(ver().getTipo())){
    		System.out.println("Pulou Token: " + ver().getLexema());
    		consume();
    	}
    }
    ///// BAGACEIRA //////
    
    // var | c
    private void var(){
        if (ver().getTipo().equals("var")){
            consume();//consome var
            //codigo do var
        }
        c();
    }
    
    // c | programa
    private void c(){
        if (ver().getTipo().equals("const")){
            consume();
            // codigo de const
        }            
        programa();        
    }
    
    // programa
    private void programa(){
        if (ver().getTipo().equals("programa")){
            //bloco
        }else{
            erros.add("Esperava \"program\", linha "+ver().getLinha());
            sincronizar("inicio", ";", "sync1", "sync2", "sync3");//escolher tokens sync para programa
            //sincronizar leitura com token que vai para bloco
            if (ver().getLexema().equals("inicio")||ver().getLexema().equals(";")){//vericicar todos os tokens que vao pra bloco
                //bloco();
            }
            //sincronizar leitura com token que vai para funcao
            else if (ver().getLexema().equals("funcao")){
                //funcao();
            }
            System.out.println("Houston, temos um problema!");
        }            
    }
}
