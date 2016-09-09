/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador;

/**
 *
 * @author Jhone e Matheus
 */
/*

*/
public class TokenTipo {
    private String letras;
    private String numeros;
    private String sinais;
    private String relacionais;
    private String reservadas[] = {"programa", "const", "var", "funcao", "inicio", "fim", "se", 
        "entao", "senao", "enquanto", "faca", "leia", "escreva", "real", "booleano", "verdadeiro", 
        "falso", "cadeia", "caractere"};
    private String logicos[] = {"e", "nao","ou"};
    private String delimitadores;
    public TokenTipo() {
        letras = "abcdefghijklmnopqrstuvxwyzABCDEFGHIJKLMNOPQRSTUVXWYZ";
        numeros= "0123456789";
        sinais = "-+*/";
        relacionais = "<> <= >="; 
        delimitadores = "( ),;";
    }
    
    public Token analise (String token){
        //operadores logicos
        for(int i=0; i<logicos.length;i++){
            if (token.equals(logicos[i]))
                return new Token(token, 6, true);
        }
        //palavras reservadas
        for(int i=0; i<reservadas.length;i++){
            if (token.equals(reservadas[i]))
                return new Token(token, 1, true);
        }
        
        //identificadores
        if (letras.contains(token.charAt(0)+"") || '_' == token.charAt(0)){
            if ('_' == token.charAt(0))
                return new Token(token, 2, false);
            else if (token.length()==1)
                return new Token(token,2, true);
            for (int i=1; i<token.length(); i++){
                if (letras.contains(token.charAt(i)+"") || numeros.contains(token.charAt(i)+"") 
                        || '_'==token.charAt(i))
                    return new Token(token,2, true); // identificador
            }
            return new Token(token, 2, false);
        }
        //numeros
        else if (numeros.contains(token.charAt(0)+"") || ('-'==token.charAt(0) && token.length()>1)){
            boolean temPonto = false;
            for (int i=1; i<token.length(); i++){
                if (!numeros.contains(token.charAt(i)+"") || (('.'==token.charAt(i)) && temPonto) ||
                        ('.'==token.charAt(0)) || (('-'==token.charAt(0)) && ('.'==token.charAt(1))))
                    return new Token(token, 3, false); // numero mal formado
                if ('.'==token.charAt(i))
                    temPonto = true;
            }
            return new Token(token, 3, true);
        }
        //operadores aritmeticos
        else if (sinais.contains(token))
            return new Token(token, 4, true);
        //delimitadores
        else if (delimitadores.contains(token))
            return new Token(token, 8, true);
        //cadeia de caracteres
        else if ('"' == token.charAt(0)){
            for (int i=1; i<token.length()-1; i++){//exceto o 1° e o ultimo
                if (!(letras.contains(token.charAt(i)+"") || numeros.contains(token.charAt(i)+"") || 
                        (' ' == token.charAt(i))))
                    return new Token(token, 9, false);// blablabla mal formado
            }
            if (!('"' == token.charAt(token.length()-1)))
                return new Token(token, 9, false);// blablabla mal formado faltou Aspas
            return new Token(token, 9, true);
        }
        //caracter
        else if ('\'' == token.charAt(0)){
            if (token.length()==3){
                if ((letras.contains(token.charAt(1)+"") || numeros.contains(token.charAt(1)+"")) && 
                       ('\'' == token.charAt(2)))
                    return new Token(token, 10, true);
            }
            if (token.length()==2){
                if ('\'' == token.charAt(1))
                    return new Token(token, 10, true);             
            }
            return new Token(token, 10, false);
            
        }
        //operacionais relacionais
        else if (relacionais.contains(token))
            return new Token(token, 5, true);        
        //comentarios
        else if ('{' == token.charAt(0)){            
            if (token.length()>1){
                if ('}' == token.charAt(token.length()-1))
                    return new Token(token, 7, true);// blablabla mal formado faltou }
            }else 
                return new Token(token, 7, false);
        }else if ('}' == token.charAt(0)){
            return new Token(token, 7, false);
        }
        
        //casos especificos de erros
        else if ('_' == token.charAt(0))//identificador mal formado
            return new Token(token, 2, false);
        else if ('.' == token.charAt(0)){
            if (token.length()==1)
                return new Token(token, 2, false);
            if (letras.contains(token.charAt(1)+""))
                return new Token(token, 2, false);//identificador mal formado
            if (numeros.contains(token.charAt(1)+""))
                return new Token(token, 3, false);//numero mal formado
        }
            //qualquer caralho fora do alfabeto        
            return new Token(token, 0, false);//caractere nao suportado
        
    }
}
