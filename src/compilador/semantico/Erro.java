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
class Erro {
    private String mensagem;
    private int linha;

    public Erro(String mensagem, int linha) {
        this.mensagem = mensagem;
        this.linha = linha;
    }

    public String getMensagem() {
        return mensagem;
    }

    public int getLinha() {
        return linha;
    }
    
    
}
