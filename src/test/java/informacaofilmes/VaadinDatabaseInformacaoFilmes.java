package informacaofilmes;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;

// page_url = https://vaadin-database-example.demo.vaadin.com/
public class VaadinDatabaseInformacaoFilmes {
    private final String URL = "https://vaadin-database-example.demo.vaadin.com/";

    // Abre a página da aplicação de exemplo Vaadin
    public void openPage() {
        open(URL);
    }

    // Retorna o elemento de grelha Vaadin (tentativa com alguns seletores de fallback)
    public SelenideElement vaadinGrid() {
        SelenideElement el = $("vaadin-grid");
        if (!el.exists()) {
            el = $("div[role='grid']");
        }
        return el;
    }

    // Verifica se a grelha está visível
    public boolean isGridVisible() {
        try {
            vaadinGrid().shouldBe(visible, Duration.ofSeconds(10));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Tenta localizar linhas na grelha (vários seletores de fallback)
    public ElementsCollection rows() {
        ElementsCollection rows = $$("vaadin-grid vaadin-grid-row");
        if (rows.isEmpty()) rows = $$("vaadin-grid table tr");
        if (rows.isEmpty()) rows = $$("tr");
        return rows;
    }

    // Filtra por texto usando um input disponível na página (tenta vários seletores)
    public void filterBy(String text) {
        SelenideElement input = $("input[placeholder*='Filter']");
        if (!input.exists()) input = $("input[aria-label*='filter']");
        if (!input.exists()) input = $("input[type='search']");
        if (!input.exists()) input = $("input");

        if (input.exists()) {
            input.clear();
            // set value and press enter to ensure applications that need a commit react
            input.setValue(text).pressEnter();

            // após definir o filtro, aguardar até que apareça algum conteúdo correspondente
            try {
                // tenta encontrar células com conteúdo dentro do grid que contenham o texto
                ElementsCollection candidates = $$("vaadin-grid-cell-content");
                if (!candidates.isEmpty()) {
                    // aguarda até que exista pelo menos uma célula contendo o texto
                    candidates.findBy(text(text)).shouldBe(visible, Duration.ofSeconds(7));
                    return;
                }

                // fallback: aguardar por qualquer <td> que contenha o texto
                ElementsCollection tds = $$("td");
                if (!tds.isEmpty()) {
                    tds.findBy(text(text)).shouldBe(visible, Duration.ofSeconds(7));
                    return;
                }

                // fallback geral: aguardar por qualquer elemento que contenha o texto
                $$("*").findBy(text(text)).shouldBe(visible, Duration.ofSeconds(7));
            } catch (Exception ignore) {
                // ignora, o teste que usa firstRowText fará a verificação final
            }
        }
    }

    // Retorna o texto da primeira linha encontrada (ou string vazia)
    public String firstRowText() {
        // aguarda por células ou linhas visíveis
        try {
            ElementsCollection visibleCells = $$("vaadin-grid-cell-content").filter(visible);
            if (!visibleCells.isEmpty()) {
                for (SelenideElement el : visibleCells) {
                    String t = el.getText();
                    if (!t.trim().isEmpty()) return t.trim();
                }
            }

            // fallback: aguardar por linhas/tables comuns
            ElementsCollection trs = $$("vaadin-grid table tr, tr, td, li, p, span, div").filter(visible);
            for (SelenideElement el : trs) {
                String t = el.getText();
                if (!t.trim().isEmpty()) return t.trim();
            }

            // fallback final: procurar por qualquer elemento visível com texto
            SelenideElement any = $$("*").filter(visible).findBy(not(empty));
            if (any.exists()) {
                String t = any.getText();
                return t.trim();
            }
        } catch (Exception ignore) {
        }

        return "";
    }
}
