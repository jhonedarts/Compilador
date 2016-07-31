/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 *
 * @author Jhone e Matheus
 */
public class Lexico {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        String numeros = ".0123456789";
        String delimitadores = "-+*/=( );<>\"'{\r\t\n\0";//todos eles
        String delimitadoresEspeciais = "<>";//podem ter complementos
        String delimitadoresUnitarios = "+*/=();";//tokens unitarios em qualquer situação
        String tabulacao = "\r\t\n";
        LinkedList<Token> tokensList = new LinkedList();
        
        Analisador analisador = new Analisador();
        //leitor
        BufferedReader reader = new BufferedReader(new FileReader(new File("entrada.txt")));
        String line;
        int linha = 0;//linha 
        int v =0;//variacao da coluna
        boolean comentario = false;
        String lexema =null;
        while ((line = reader.readLine()) != null){//ler todas linhas
            //e para cada linha separar os tokens
            linha++;
            char a ;
            if(!comentario)//nao reseto o valor ate fechar o comentario
                lexema = null;
            
            System.out.println(line);
            System.out.println(line.length());
            //separação dos tokens da linha
            for (int i=0; i<line.length(); i++){
                System.out.println(i+"i");
                if (comentario){                    
                    if (line.charAt(i)=='}'){                        
                        comentario = false;
                        lexema = lexema.concat(line.charAt(i)+"");                        
                        System.out.println(lexema);
                        Token token = analisador.analise(lexema);
                        token.setColuna(v);
                        token.setLinha(linha);
                        tokensList.add(token);
                    } else 
                        lexema = lexema.concat(line.charAt(i)+"");
                }else{  
                    v=0;
                    a = line.charAt(i);//apenas para inicio de lexema
                    lexema = "";//reinicia a string lexema
                    // delimitadores unitários
                    if (delimitadoresUnitarios.contains(a+"")){
                        lexema = a+"";
                        System.out.println(i+"unitario");
                    }

                    else if(delimitadoresEspeciais.contains(a+"")){//sao binarios
                        lexema = a+"";
                        try{
                            if( (a == '<' && (line.charAt(i+1) == '>' || line.charAt(i+1) == '=')) ||
                                    (a == '>' && (line.charAt(i+1) == '=' )) ){
                                i++; v++;
                                lexema = lexema.concat(line.charAt(i)+"");
                                System.out.println(i+"<>");
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada
                            
                        }
                    }else if (a == '-'){//numeros negativos ou operador -
                        lexema = a+"";
                        try{
                            if (numeros.contains(line.charAt(i+1)+"")){
                                while(!delimitadores.contains(line.charAt(i+1)+"")){   
                                    i++; v++;
                                   lexema = lexema.concat(line.charAt(i)+""); 
                                   System.out.println(i+"-");
                                }          
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada
                            
                        }
                    //caractere
                    }else if (a == '\''){
                        lexema = a+"";
                        System.out.println(i+"\'");
                        try {
                            while(!(line.charAt(i+1) == '\'' || line.charAt(i+1) == ' ' || tabulacao.contains(line.charAt(i+1)+""))){   
                                i++; v++;
                               lexema = lexema.concat(line.charAt(i)+"");    
                               System.out.println(i);
                            }
                            if (line.charAt(i+1) == '\''){
                                i++; v++;
                               lexema = lexema.concat(line.charAt(i)+"");
                               System.out.println(i);
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de ' ser a ultima coisa do arquivo
                            
                        }
                    //cadeia de caractere  
                    }else if (a == '"'){
                        lexema = a+"";
                        System.out.println(i+"\"");
                        try{
                            while(!(line.charAt(i+1) == '"' || tabulacao.contains(line.charAt(i+1)+""))){   
                                i++; v++;
                                System.out.println(i);
                               lexema = lexema.concat(line.charAt(i)+"");                                              
                            }
                            if (line.charAt(i+1) == '"'){
                                i++; v++;
                                System.out.println(i);
                               lexema = lexema.concat(line.charAt(i)+"");
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada
                            
                        }
                    //comentarios
                    }else if (a=='{'){
                        lexema = a+"";
                        comentario = true;
                        v=i;//para usar v como coluna da posicao do {
                    }else if (a=='}'){//caractere perdido
                        lexema = a+"";
                    }else if(tabulacao.contains(a+"") || a==' '){
                        lexema = null;//so pra nao entra na formação do token, sera reiniciado no inicio do for
                    }
                    //qualquer coisa que vier
                    else {
                        //concatene ate um delimitador
                        
                        lexema = a+"";
                        try{
                            while(!delimitadores.contains(line.charAt(i+1)+"")){   
                                i++; v++;
                                System.out.println(i+" else");
                               lexema = lexema.concat(line.charAt(i)+"");                                              
                            } 
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada
                            
                        }
                        
                    }
                    
                    //lexema formado se nao for comentario ou tabulacao
                    if (!comentario && lexema!=null){
                        System.out.println(lexema);
                        Token token = analisador.analise(lexema);
                        System.out.println(i+" token formado");
                        token.setColuna(i-v);
                        token.setLinha(linha);
                        tokensList.add(token);
                    }
                }
            }
        }
        
        //gravar no txt
        File file = new File("saida.txt");        
        int r=0;
        
        if (file.exists()){
            while (file.exists()){//para criar e nao substituir o arquivo
                r++;
                file = new File("saida("+r+").txt"); 
            }
            new File("saida("+r+").txt").createNewFile();
            file = new File("saida("+r+").txt");
        }
        
        PrintWriter gravarArq = new PrintWriter(new FileWriter(file));
        
        LinkedList<Token> errosList = new LinkedList();
        //escrevendo no txt
        gravarArq.printf("Tabela de Simbolos:%n%nn# - lexema - tipo - linha%n");   
        System.out.println("Tabela de Simbolos:\n\nn# - lexema - tipo - linha");
        for(int i=0; i<tokensList.size();i++){
            Token t= tokensList.get(i);
            if(t.isValido()){
                gravarArq.printf((i+1)+" - "+t.getLexema()+" - "+t.getTipo()+" - "+t.getLinha()+"%n");
                System.out.println((i+1)+" - "+t.getLexema()+" - "+t.getTipo()+" - "+t.getLinha());
            }else
                errosList.add(t);
        }        
        gravarArq.printf("%n%nTabela de Erros:%n%nn# - lexema - tipo - linha%n");
        System.out.println("\n\nTabela de Erros:\n\nn# - lexema - tipo - linha");
        for(int i=0; i<errosList.size();i++){
            Token t= errosList.get(i);
            gravarArq.printf((i+1)+" - "+t.getLexema()+" - "+t.getTipo()+" - "+t.getLinha()+"%n");
            System.out.println((i+1)+" - "+t.getLexema()+" - "+t.getTipo()+" - "+t.getLinha());
        }
        gravarArq.close();
    }    
    
}
