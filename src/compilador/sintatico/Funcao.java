/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.sintatico;

import java.util.LinkedList;

/**
 *
 * @author jhone e castro
 */
public class Funcao {
    private String nome;
    private LinkedList<String> parametros;
    private String tipoRetorno;
    private boolean ok;
    private int linha;

    public Funcao(String nome, String tipoRetorno, int linha) {
        this.nome = nome;
        this.parametros = new LinkedList<String>();
        this.tipoRetorno = tipoRetorno;
        if(tipoRetorno.equals("vazio"))
            this.ok = true;
        else
            this.ok = false;
        this.linha = linha;
    }

    public String getNome() {
        return nome;
    }

    public LinkedList<String> getParametros() {
        return parametros;
    }

    public void addParametros(String parametro) {
        this.parametros.add(parametro);
    }

    public String getTipoRetorno() {
        return tipoRetorno;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getLinha() {
        return linha;
    }
    
    
}
