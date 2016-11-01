/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.semantico;


/**
 *
 * @author jhone
 */
public class Simbolo {
    private String tipo;
    private Object conteudo;
    private boolean inicializado;
    private boolean retornoFuncao = false;

    public Simbolo(Object conteudo, String tipo) {
        this.conteudo = conteudo;
        this.inicializado = false;
        this.tipo = tipo;
    }
    
    public Simbolo(Object conteudo, String tipo, boolean inicializado) {
        this.conteudo = conteudo;
        this.inicializado = inicializado;
        this.tipo = tipo;
    }

    public Object getConteudo() {
        return conteudo;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isInicializado() {
        return inicializado;
    }

    public void setInicializado(boolean inicializado) {
        this.inicializado = inicializado;
    }

    public boolean isRetornoFuncao() {
        return retornoFuncao;
    }

    public void setRetornoFuncao(boolean retornoFuncao) {
        this.retornoFuncao = retornoFuncao;
    }
    
    
}
