/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.semantico;

import java.util.HashMap;

/**
 *
 * @author jhone
 */
public class TabelaSimbolos {
    private String escopo =null;
    private HashMap<String, Simbolo> simbolos;
    private TabelaSimbolos anterior;

    public TabelaSimbolos(TabelaSimbolos anterior) {
        this.simbolos = new HashMap<String, Simbolo>();
        this.anterior = anterior;
    }

    public void put(String key, Simbolo simbolo){
        simbolos.put(key, simbolo);
    }

    public String getEscopo() {
        return escopo;
    }

    public void setEscopo(String escopo) {
        this.escopo = escopo;
    }
    
    public Simbolo get(String key){
        return simbolos.get(key);
    }
    
    public HashMap<String, Simbolo> getSimbolos() {
        return simbolos;
    }

    public TabelaSimbolos getAnterior() {
        return anterior;
    }

    public void setAnterior(TabelaSimbolos anterior) {
        this.anterior = anterior;
    }
    
}
