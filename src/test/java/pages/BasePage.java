package pages;

import com.codeborne.selenide.Selenide;

public class BasePage {

    public void openUrl(String url) {
        Selenide.open(url);
    }
}
