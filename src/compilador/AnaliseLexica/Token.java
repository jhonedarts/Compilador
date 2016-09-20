/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.AnaliseLexica;

/**
 *
 * @author Jhone e Matheus
 */
public class Token {
    private String lexema;
    private int tipo;
    private String tipos[] = {"palavra reservada", "identificador", "numero", "operador aritimetico",
        "operador relacional", "operador logico", "comentario", "delimitador", "cadeia de caracteres", "caracetere"} ;
    private String erros[] = {"caracter(es) nao suportado(s)","identificador mal formado", 
        "numero mal formado", "comentario mal formado", "cadeia de caracteres mal formada", "caractere mal formado"};
    private boolean valido;
    private int linha, coluna;

    public Token(String lexema, int tipo, boolean valido) {
        this.lexema = lexema;
        this.tipo = tipo;
        this.valido = valido;
        linha=0;
        coluna=0;
    }

    public String getLexema() {
        return lexema;
    }

    public String getTipo() {
        String tpo = null;
        if (valido)
            tpo = tipos[tipo-1];
        else{
            if(tipo==0)
                tpo = erros[tipo];            
            else if (tipo <= 3)
                tpo = erros[tipo-1];
            else if (tipo == 7)
                tpo = erros[tipo-4];
            else
                tpo = erros[tipo-5];
        }
            
        return tpo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
    }

    public int getLinha() {
        return linha;
    }

    public void setLinha(int linha) {
        this.linha = linha;
    }

    public int getColuna() {
        return coluna;
    }

    public void setColuna(int coluna) {
        this.coluna = coluna;
    }
    
    
}
