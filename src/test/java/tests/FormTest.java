
package tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import io.qameta.allure.Description;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pages.FormPage;

import static org.testng.Assert.assertTrue;

public class FormTest {

    private FormPage formPage;

    @BeforeClass
    public void setup() {
        Configuration.browserSize = "1920x1080";
        Configuration.timeout = 8000; // tempo para animações/Shadow DOM
        formPage = new FormPage();
        formPage.openUrl("https://vaadin-form-example.demo.vaadin.com/");
    }

    @Test(description = "Verifica se o formulário envia dados corretamente")
    @Description("Teste de aceitação: preencher o formulário e submeter")
    public void testSubmitForm() {
        // Preenche os campos
        formPage.setFirstName("João");
        formPage.setLastName("Silva");
        formPage.setUserHandle("joaosilva");
        formPage.setWantedPassword("SenhaFort3!");
        formPage.setPasswordAgain("SenhaFort3!");

        // Submete com dois cliques (a demo por vezes valida no 1º clique)
        formPage.submit();
        Selenide.sleep(300);
        formPage.submit();

        // Lê a notificação
        String notification = formPage.waitAndGetNotificationText();
        String n = notification.toLowerCase();

        // Aceita as mensagens conhecidas da demo
        boolean ok =
                n.contains("data saved, welcome") ||
                        n.contains("thank") ||
                        n.contains("success") ||
                        n.contains("joined");

        assertTrue(ok, "Texto da notificação inesperado: " + notification);
    }
}
