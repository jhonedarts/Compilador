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
    private Simbolo anterior = null;

    public Simbolo(Object conteudo, String tipo) {
        this.conteudo = conteudo;
    }

    public Object getConteudo() {
        return conteudo;
    }

    public String getTipo() {
        return tipo;
    }

    public Simbolo getAnterior() {
        return anterior;
    }

    public void setAnterior(Simbolo anterior) {
        this.anterior = anterior;
    }
    
    
}
