
package pages;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * page_url = https://vaadin-form-example.demo.vaadin.com/
 * Formulário "Signup form"
 */
public class FormPage extends BasePage {

    private void ensureFormLoaded() {
        $("h2").should(appear).shouldHave(text("Signup form"));
    }

    // Hosts por índice (rótulos ficam dentro do Shadow DOM, não confie em [label='...'] no host)
    private SelenideElement firstNameHost()      { return $$("vaadin-text-field").get(0).should(exist); }
    private SelenideElement lastNameHost()       { return $$("vaadin-text-field").get(1).should(exist); }
    private SelenideElement userHandleHost()     { return $$("vaadin-text-field").get(2).should(exist); }

    private SelenideElement wantedPasswordHost() { return $$("vaadin-password-field").get(0).should(exist); }
    private SelenideElement passwordAgainHost()  { return $$("vaadin-password-field").get(1).should(exist); }

    private SelenideElement marketingCheckbox()  { return $("vaadin-checkbox").should(exist); }
    private SelenideElement joinButton()         { return $("vaadin-button[theme*='primary']").should(exist); }

    private SelenideElement notificationCard()   { return $("vaadin-notification-card"); }

    // -------- Interações --------
    public void setFirstName(String value) {
        ensureFormLoaded();
        getShadowInput(firstNameHost()).setValue(value);
    }

    public void setLastName(String value) {
        ensureFormLoaded();
        getShadowInput(lastNameHost()).setValue(value);
    }

    public void setUserHandle(String value) {
        ensureFormLoaded();
        getShadowInput(userHandleHost()).setValue(value);
    }

    public void setWantedPassword(String value) {
        ensureFormLoaded();
        getShadowInput(wantedPasswordHost()).setValue(value);
    }

    public void setPasswordAgain(String value) {
        ensureFormLoaded();
        getShadowInput(passwordAgainHost()).setValue(value);
    }

    public void setAllowMarketing(boolean desired) {
        ensureFormLoaded();
        SelenideElement cb = getShadowCheckboxInput(marketingCheckbox());
        boolean current = cb.isSelected();
        if (current != desired) {
            marketingCheckbox().click(); // clicar no host altera o estado
        }
    }

    public void submit() {
        ensureFormLoaded();
        joinButton().shouldBe(enabled).click();
    }

    public String waitAndGetNotificationText() {
        notificationCard().should(appear);
        return notificationCard().getText();
    }

    // -------- Utilitários Shadow DOM --------

    private SelenideElement getShadowInput(SelenideElement host) {
        org.openqa.selenium.WebElement we = Selenide.executeJavaScript(
                "return arguments[0] && arguments[0].shadowRoot && arguments[0].shadowRoot.querySelector('input')",
                host
        );
        if (we == null) {
            we = Selenide.executeJavaScript(
                    "const r=arguments[0].shadowRoot; if(!r) return null;" +
                            "const s=r.querySelector('slot[name=\"input\"]'); if(!s) return null;" +
                            "const assigned=s.assignedElements(); return assigned && assigned.length? assigned[0] : null;",
                    host
            );
        }
        if (we == null) {
            throw new IllegalStateException("Não foi possível localizar o <input> no shadowRoot do componente Vaadin.");
        }
        return $(we); // envolver para virar SelenideElement
    }

    private SelenideElement getShadowCheckboxInput(SelenideElement checkboxHost) {
        org.openqa.selenium.WebElement we = Selenide.executeJavaScript(
                "return arguments[0] && arguments[0].shadowRoot && arguments[0].shadowRoot.querySelector('input[type=\"checkbox\"]')",
                checkboxHost
        );
        if (we == null) {
            throw new IllegalStateException("Não foi possível localizar <input type='checkbox'> no shadowRoot do vaadin-checkbox.");
        }
        return $(we);
    }
}