package AdicionarCategoria;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;

import java.time.Duration;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

public class PaginaBookstoreAdmin {




    // ----------------------------
    // LOGIN
    // ----------------------------
    @FindBy(css = "input[name='username']")
    private SelenideElement campoUsername;

    @FindBy(css = "input[name='password']")
    private SelenideElement campoPassword;

    @FindBy(css = "vaadin-button[part='vaadin-login-submit']")
    private SelenideElement botaoLogin;

    //Notificaçao
    private SelenideElement notificationCard()   { return $("vaadin-notification-card"); }

    public void loginComoAdmin() {
        campoUsername.shouldBe(visible).setValue("admin");
        campoPassword.shouldBe(visible).setValue("admin");
        botaoLogin.shouldBe(visible).click();
    }

    // ----------------------------
    // MENU HAMBURGER
    // ----------------------------
    // MENU HAMBURGER
    @FindBy(css = "vaadin-drawer-toggle.menu-toggle")
    private SelenideElement botaoMenu;



    // ITEM ADMIN
    @FindBy(xpath = "//a[@class='menu-link' and normalize-space()='Admin']")
    private SelenideElement linkAdmin;


    // MÉTODO ABRIR ADMIN
    public void abrirAdmin() {
        botaoMenu.shouldBe(visible).click();
        linkAdmin.shouldBe(visible).click();
    }


    // ----------------------------
    // ADICIONAR CATEGORIA
    // ----------------------------
    // Localizador para o botão "Add new category"

    public void adicionarCategoria(String nomeCategoria) {
        System.out.println("=== ADICIONANDO CATEGORIA: " + nomeCategoria + " ===");

        // 1. Clica no botão
        clicarBotaoAddCategoria();

        // 2. Pequena pausa para garantir que o foco está no campo
        sleep(500);

        // 3. Digita a categoria e pressiona Enter - SIMPLES ASSIM!
        digitarCategoriaEConfirmar(nomeCategoria);

        // 4. Verifica resultado
        sleep(2000);

        System.out.println("Resultado: " + notificationCard().getText());
    }

    private void clicarBotaoAddCategoria() {
        System.out.println("Clicando em 'Add new category'...");
        $x("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add new category')]")
                .shouldBe(visible, enabled)
                .click();
        System.out.println("Botão clicado! O foco deve ir automaticamente para o campo.");
    }

    // MÉTODO SIMPLES: Apenas digita e pressiona Enter
    private void digitarCategoriaEConfirmar(String nomeCategoria) {
        System.out.println("Digitando categoria: " + nomeCategoria);

        // ESTRATÉGIA 1: Usa Actions API - a mais direta
        actions()
                .sendKeys(nomeCategoria)
                .pause(500)
                .sendKeys(Keys.ENTER)
                .perform();

        System.out.println("Categoria digitada e Enter pressionado!");
    }

    // MÉTODO ALTERNATIVO: Usando JavaScript para garantir
    public void adicionarCategoriaJavaScript(String nomeCategoria) {
        System.out.println("=== ADICIONANDO COM JAVASCRIPT ===");

        // 1. Clica no botão
        clicarBotaoAddCategoria();

        // 2. Aguarda 500ms
        sleep(5000);

        // 3. JavaScript direto - digita no elemento ativo
        executeJavaScript(
                "// Digita no elemento que está em foco\n" +
                        "var elementoAtivo = document.activeElement;\n" +
                        "console.log('Elemento ativo:', elementoAtivo.tagName, elementoAtivo.type || 'sem type');\n" +
                        "\n" +
                        "if (elementoAtivo && (elementoAtivo.tagName === 'INPUT' || elementoAtivo.tagName === 'TEXTAREA')) {\n" +
                        "    // Limpa e preenche\n" +
                        "    elementoAtivo.value = arguments[0];\n" +
                        "    \n" +
                        "    // Dispara eventos\n" +
                        "    var inputEvent = new Event('input', { bubbles: true });\n" +
                        "    elementoAtivo.dispatchEvent(inputEvent);\n" +
                        "    \n" +
                        "    var changeEvent = new Event('change', { bubbles: true });\n" +
                        "    elementoAtivo.dispatchEvent(changeEvent);\n" +
                        "    \n" +
                        "    // Pressiona Enter\n" +
                        "    var enterEvent = new KeyboardEvent('keydown', {\n" +
                        "        key: 'Enter',\n" +
                        "        code: 'Enter',\n" +
                        "        keyCode: 13,\n" +
                        "        bubbles: true\n" +
                        "    });\n" +
                        "    \n" +
                        "    elementoAtivo.dispatchEvent(enterEvent);\n" +
                        "    \n" +
                        "    console.log('Categoria preenchida via JS: ' + arguments[0]);\n" +
                        "    return 'sucesso';\n" +
                        "} else {\n" +
                        "    console.log('Elemento ativo não é input/textarea:', elementoAtivo);\n" +
                        "    return 'elemento-ativo-nao-e-input';\n" +
                        "}",
                nomeCategoria
        );

        System.out.println("JavaScript executado!");
    }

    // MÉTODO SUPER SIMPLES: Apenas sendKeys na página
    public void adicionarCategoriaSendKeys(String nomeCategoria) {
        System.out.println("=== ADICIONANDO COM SENDKEYS ===");

        // 1. Clica no botão
        clicarBotaoAddCategoria();

        // 2. Aguarda foco
        sleep(3000);

        // 3. Digita diretamente (o foco já está no campo correto)
        $("body").sendKeys(nomeCategoria + Keys.ENTER);

        System.out.println("SendKeys executado!");
    }

    // MÉTODO COM MÚLTIPLAS TENTATIVAS
    public void adicionarCategoriaRobusta(String nomeCategoria) {
        System.out.println("=== ADICIONANDO CATEGORIA (ROBUSTA) ===");

        clicarBotaoAddCategoria();
        sleep(500);

        // Tenta várias estratégias
        boolean sucesso = false;

        // Estratégia 1: Actions API
        try {
            System.out.println("Tentando Actions API...");
            actions().sendKeys(nomeCategoria).pause(200).sendKeys(Keys.ENTER).perform();
            sucesso = true;
            System.out.println("Sucesso com Actions API!");
        } catch (Exception e) {
            System.out.println("Actions API falhou: " + e.getMessage());
        }

        // Estratégia 2: Se falhou, tenta JavaScript
        if (!sucesso) {
            try {
                System.out.println("Tentando JavaScript...");
                executeJavaScript(
                        "document.activeElement.value = arguments[0];" +
                                "var e = new Event('input', { bubbles: true });" +
                                "document.activeElement.dispatchEvent(e);" +
                                "var k = new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13, bubbles: true });" +
                                "document.activeElement.dispatchEvent(k);",
                        nomeCategoria
                );
                sucesso = true;
                System.out.println("Sucesso com JavaScript!");
            } catch (Exception e) {
                System.out.println("JavaScript falhou: " + e.getMessage());
            }
        }

        // Estratégia 3: Se ainda falhou, tenta enviar para o body
        if (!sucesso) {
            try {
                System.out.println("Tentando sendKeys no body...");
                $("body").sendKeys(nomeCategoria + Keys.ENTER);
                System.out.println("Sucesso com sendKeys no body!");
            } catch (Exception e) {
                System.out.println("Tudo falhou: " + e.getMessage());
            }
        }
    }

    // MÉTODO QUE VOCÊ PODE TESTAR AGORA
    public void adicionarCategoriaFinal(String nomeCategoria) {
        System.out.println("=== TESTE FINAL ===");
        System.out.println("Clicando no botão...");

        clicarBotaoAddCategoria();

        System.out.println("Aguardando 1 segundo...");
        sleep(1000);

        System.out.println("Digitando: " + nomeCategoria);

        // APENAS ISSO:
        actions().sendKeys(nomeCategoria).perform();
        sleep(300);
        actions().sendKeys(Keys.ENTER).perform();

        System.out.println("Concluído!");
    }






    // ----------------------------
    // ABRIR URL
    // ----------------------------
    public void abrirPagina() {

        open("https://vaadin-bookstore-example.demo.vaadin.com/");
    }


}
