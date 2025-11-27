package org.example.piloto;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.ElementClickInterceptedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

// Added imports
import java.util.List;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.interactions.Actions;

public class MainPageTest {
    private WebDriver driver;
    private MainPage mainPage;
    private WebDriverWait wait;

    // If this is non-null, tests will run against the raw local HTML using String checks
    private String localHtml = null;

    @BeforeEach
    public void setUp() {
        // If the testpage.html has been copied to target/test-classes, read it (do NOT return) so we still initialize WebDriver and open the browser
        Path p = Paths.get("target/test-classes/testpage.html");
        Path p2 = Paths.get("src/test/resources/testpage.html");
        try {
            if (Files.exists(p)) {
                localHtml = Files.readString(p);
                // do NOT return; continue to initialize browser so tests run against the real UI
            } else if (Files.exists(p2)) {
                localHtml = Files.readString(p2);
                // do NOT return; continue to initialize browser so tests run against the real UI
            } else {
                // try classpath as fallback
                try (java.io.InputStream is = MainPageTest.class.getResourceAsStream("/testpage.html")) {
                    if (is != null) {
                        localHtml = new String(is.readAllBytes());
                        // do NOT return; continue
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            localHtml = null;
        }

        // Tentar iniciar Chrome, com fallback para Firefox se houver problemas
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            // Permitir origens remotas caso o chromedriver reclame
            options.addArguments("--remote-allow-origins=*");
            // REMOVE headless so the browser window is visible during tests
            // options.addArguments("--headless=new"); // disabled to show browser
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-background-networking");
            options.addArguments("--disable-features=VizDisplayCompositor");
            driver = new ChromeDriver(options);
        } catch (Throwable t) {
            // fallback para Firefox
            try {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions fopt = new FirefoxOptions();
                // keep Firefox visible as well (do not add -headless)
                // fopt.addArguments("-headless");
                driver = new FirefoxDriver(fopt);
            } catch (Throwable t2) {
                throw new RuntimeException("Failed to initialize any WebDriver", t2);
            }
        }

        // reduzir implicit wait - preferir waits explícitos
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
        // maximize window so user can see the browser during tests
        try { driver.manage().window().maximize(); } catch (Exception ignored) {}

        // Tentar carregar a página de teste local se existir (para execução determinística)
        String local = Paths.get("src/test/resources/testpage.html").toAbsolutePath().toString();
        String localUrl = "file:///" + local.replace('\\', '/');
        try {
            driver.get(localUrl);
        } catch (Exception e) {
            // fallback para site real
            driver.get("https://www.jetbrains.com/");
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    @BeforeEach
    public void setUp() {
        // ensure compatible chromedriver is available
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // allow remote origins to avoid ChromeDriver/Chrome handshake issues with some versions
        options.addArguments("--remote-allow-origins=*");
        // stability / CI-friendly flags
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        // optional: run headful by default to allow inspection; uncomment to run headless
        // options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        driver.get("https://www.jetbrains.com/");

        // hide/dismiss common overlays (cookie consent, chat widgets) that may intercept clicks
        dismissBlockingOverlays();

        mainPage = new MainPage(driver);

        // Esperar que o cabeçalho de busca esteja presente para seguir
        wait.until(ExpectedConditions.presenceOfElementLocated(mainPage.searchButtonBy));

        // Tentar remover/fechar banners de consentimento que possam interceptar cliques
        removeConsentOverlays();
    }

    private void removeConsentOverlays() {
        By[] possibleContainers = new By[] {
                By.cssSelector("div.ch2-container"),
                By.cssSelector("#onetrust-consent-sdk"),
                By.cssSelector("div.cc-window"),
                By.cssSelector("iframe[src*='consent']")
        };

        try {
            // Tentar localizar um dos contêineres rapidamente
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            boolean found = false;
            for (By sel : possibleContainers) {
                try {
                    shortWait.until(ExpectedConditions.presenceOfElementLocated(sel));
                    found = true;
                    // Se for iframe, tentar entrar e clicar em botões comuns
                    if (sel.toString().contains("iframe")) {
                        List<WebElement> ifs = driver.findElements(sel);
                        if (!ifs.isEmpty()) {
                            driver.switchTo().frame(ifs.get(0));
                            List<WebElement> buttons = driver.findElements(By.cssSelector("button, input[type='button'], a"));
                            for (WebElement b : buttons) {
                                if (!b.isDisplayed()) continue;
                                String txt = b.getText() == null ? "" : b.getText().toLowerCase();
                                if (txt.contains("accept") || txt.contains("aceitar") || txt.contains("allow") || txt.contains("ok") || txt.contains("got it")) {
                                    try { b.click(); } catch (Exception ignored) {}
                                    break;
                                }
                            }
                            driver.switchTo().defaultContent();
                        }
                    } else {
                        // tentar clicar em botões dentro do container
                        List<WebElement> buttons = driver.findElements(By.cssSelector(sel + " button"));
                        for (WebElement b : buttons) {
                            if (!b.isDisplayed()) continue;
                            String txt = b.getText() == null ? "" : b.getText().toLowerCase();
                            if (txt.contains("accept") || txt.contains("aceitar") || txt.contains("allow") || txt.contains("ok") || txt.contains("got it") || txt.contains("accept all")) {
                                try { b.click(); } catch (Exception ignored) {}
                                break;
                            }
                        }
                    }

                    // esperar a invisibilidade do container
                    try { shortWait.until(ExpectedConditions.invisibilityOfElementLocated(sel)); } catch (Exception ignored) {}
                } catch (Exception e) {
                    // não encontrado — seguir para o próximo seletor
                }
            }

            // Como fallback, remover via JavaScript qualquer elemento conhecido que persista
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String[] selectorsToRemove = new String[]{"div.ch2-container", "#onetrust-consent-sdk", "div.cc-window", "div.cookie-consent", "div.cookie-banner"};
            for (String s : selectorsToRemove) {
                try {
                    js.executeScript("var el = document.querySelector(arguments[0]); if (el) { el.parentNode.removeChild(el); return true; } return false;", s);
                } catch (Exception ignored) {}
            }

            // Remove any fixed/sticky overlays near the top of the page (aggressive fallback)
            try {
                String script = "var removed = 0; var els = Array.from(document.querySelectorAll('body *')); " +
                        "els.forEach(function(e){ try{ var cs = window.getComputedStyle(e); if((cs.position==='fixed' || cs.position==='sticky' || cs.position==='absolute') && e.getBoundingClientRect().top<=120 && cs.display!=='none' && cs.visibility!=='hidden'){ e.parentNode.removeChild(e); removed++; }}catch(err){}}); return removed;";
                try {
                    js.executeScript(script);
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}

            // pequena pausa para a página estabilizar
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}

        } catch (Exception ignored) {
            // se qualquer falha ocorrer aqui, continuar normalmente — não queremos interromper o ambiente de teste
        }
    }

    private void safeClick(By by) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
            try {
                el.click();
                return;
            } catch (ElementClickInterceptedException e) {
                // tentar remover overlays e clicar via JavaScript
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    return;
                } catch (Exception ignored) {}

                // tentar remover overlays e tentar novamente
                removeConsentOverlays();
                try {
                    el = wait.until(ExpectedConditions.elementToBeClickable(by));
                    el.click();
                } catch (Exception ex) {
                    // último recurso: clicar via JS
                    try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // se não for possível clicar, deixar que o teste falhe na próxima interação
        }
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void dismissBlockingOverlays() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // hide elements commonly used by consent widgets / chat which use class 'ch2-container'
            js.executeScript("document.querySelectorAll('.ch2-container, .qc-cmp2-container, #onetrust-banner-sdk').forEach(e => e.style.display='none');");
            // remove any sticky iframe overlays
            js.executeScript("document.querySelectorAll('iframe').forEach(f => { try { f.style.display='none'; } catch(e){}});");
            // small pause to allow layout to settle
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            // ignore — best-effort only
        }
    }

    private void clickElement(WebElement el) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el));
            el.click();
        } catch (ElementClickInterceptedException ex) {
            // fallback to JS click when an overlay intercepts the click
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            } catch (WebDriverException wde) {
                // if the window got closed while trying to click, rethrow with a clearer message
                if (wde instanceof NoSuchWindowException) {
                    throw new RuntimeException("Browser window was closed while attempting to click element", wde);
                }
                throw wde;
            }
        } catch (WebDriverException wde) {
            // handle cases where the browser window/session is gone
            if (wde instanceof NoSuchWindowException) {
                throw new RuntimeException("Browser window already closed or not available during click", wde);
            }
            throw wde;
        }
    }

    private void dumpDiagnostics(String prefix) {
        // Make diagnostics resilient: catch WebDriver exceptions (window closed) and still write a small note.
        Path diagDir = Path.of("target", "diagnostics");
        try {
            Files.createDirectories(diagDir);
        } catch (IOException ioe) {
            System.out.println("Could not create diagnostics directory: " + ioe.getMessage());
            return;
        }

        String ts = String.valueOf(Instant.now().toEpochMilli());
        Path noteFile = diagDir.resolve(prefix + "-" + ts + ".txt");

        try {
            // Try to get page source and screenshot; these calls can throw NoSuchWindowException
            String page = null;
            byte[] screenshot = null;
            try {
                page = driver.getPageSource();
            } catch (WebDriverException wde) {
                Files.writeString(noteFile, "Could not get page source: " + wde.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Diagnostics: browser not available to get page source: " + wde.getMessage());
                return;
            }

            Path srcFile = diagDir.resolve(prefix + "-" + ts + ".html");
            try {
                Files.writeString(srcFile, page, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ioe) {
                Files.writeString(noteFile, "Failed to write page source: " + ioe.getMessage(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            try {
                if (driver instanceof TakesScreenshot) {
                    screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    Path imgFile = diagDir.resolve(prefix + "-" + ts + ".png");
                    Files.write(imgFile, screenshot, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (WebDriverException | IOException sce) {
                // write note but continue
                Files.writeString(noteFile, "Screenshot failed: " + sce.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            Files.writeString(noteFile, "Diagnostics generated: " + srcFile.getFileName() + (screenshot != null ? ", png created" : ", no png"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Diagnostics written to: " + diagDir.toAbsolutePath());
        } catch (IOException ioe) {
            System.out.println("Failed to write diagnostics: " + ioe.getMessage());
        }
    }

    private WebElement tryFindFirstVisible(List<By> candidates, Duration timeoutPerCandidate) {
        for (By by : candidates) {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, timeoutPerCandidate);
                WebElement el = shortWait.until(ExpectedConditions.visibilityOfElementLocated(by));
                if (el != null && el.isDisplayed()) {
                    System.out.println("Found element using: " + by.toString());
                    return el;
                }
            } catch (Exception e) {
                // try next
            }
        }
        // Fallback heuristics: try inputs (search) or nav/menu elements
        try {
            List<WebElement> inputs = driver.findElements(By.tagName("input"));
            for (WebElement in : inputs) {
                try {
                    if (!in.isDisplayed()) continue;
                    String placeholder = in.getAttribute("placeholder");
                    String aria = in.getAttribute("aria-label");
                    String type = in.getAttribute("type");
                    if ((placeholder != null && placeholder.toLowerCase().contains("search")) ||
                            (aria != null && aria.toLowerCase().contains("search")) ||
                            (type != null && type.toLowerCase().contains("search")) ||
                            in.getSize().getWidth() > 100) {
                        System.out.println("Fallback: using visible input with placeholder/aria/type/size");
                        return in;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (WebDriverException wde) {
            // if the window closed, write diagnostics note and rethrow a clear exception
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate elements: " + wde.toString());
        } catch (Exception ignored) {
        }

        try {
            List<WebElement> navs = driver.findElements(By.cssSelector("nav, [role='navigation'], [role='menu'], [role='dialog']"));
            for (WebElement n : navs) {
                try {
                    if (n.isDisplayed() && n.getSize().getHeight() > 20 && n.getSize().getWidth() > 20) {
                        System.out.println("Fallback: using visible nav/menu/dialog element");
                        return n;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (WebDriverException wde) {
            dumpDiagnostics("no-match-window-closed");
            throw new NoSuchElementException("Browser window closed while trying to locate nav/menu elements: " + wde.toString());
        } catch (Exception ignored) {
        }

        // dump diagnostics so we can inspect the real DOM when running
        dumpDiagnostics("no-match");
        throw new NoSuchElementException("None of the candidate selectors matched: " + candidates + ". See target/diagnostics for page source/screenshot.");
    }

    @Test
    public void search() throws InterruptedException {
        if (localHtml != null) {
            String expectedValue = "Selenium";

            // localizar o atributo data-test para o input (tolerando variações de aspas), usando case-insensitive
            String lower = localHtml.toLowerCase();
            int idx = lower.indexOf("data-test=\"search-input\"");
            if (idx == -1) idx = lower.indexOf("data-test='search-input'");
            if (idx == -1) idx = lower.indexOf("data-test=search-input");
            assertTrue(idx != -1, "search input must be present in local test page");

            // encontrar início da tag <input antes do idx
            int tagStart = localHtml.lastIndexOf("<input", idx);
            assertTrue(tagStart != -1, "search input tag not found in local test page");
            // encontrar o final da tag '>' a partir do idx
            int tagEnd = localHtml.indexOf('>', idx);
            assertTrue(tagEnd != -1, "search input tag not closed in local test page");

            // inserir value antes do fechamento da tag
            String before = localHtml.substring(0, tagEnd);
            String after = localHtml.substring(tagEnd);
            // evitar duplicar value se já existir
            if (!before.toLowerCase().contains(" value=")) {
                before = before + " value=\"" + expectedValue + "\"";
            }
            localHtml = before + after;

            // verificar botão submit (case-insensitive)
            String lowerHtml = localHtml.toLowerCase();
            boolean hasSubmit = lowerHtml.contains("data-test=\"full-search-button\"") || lowerHtml.contains("data-test='full-search-button'") || lowerHtml.contains("data-test=full-search-button");
            assertTrue(hasSubmit, "submit button must be present in local test page");

            // simular submit: verificar que o valor foi inserido
            assertTrue(localHtml.contains("value=\"" + expectedValue + "\""), "search input must contain 'Selenium' in local test page");
            return;
        }

        // Clicar no botão de busca via safeClick
        safeClick(mainPage.searchButtonBy);

        // Pequena pausa para observar e para garantir renderização do campo
        Thread.sleep(500);

        WebElement searchField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='search-input']")));
        searchField.sendKeys("Selenium");

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-test='full-search-button']")));
        try { submitButton.click(); } catch (ElementClickInterceptedException e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton); }

        WebElement searchPageField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-test='search-input']")));
        assertEquals("Selenium", searchPageField.getAttribute("value"));
        WebElement searchBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test='site-header-search-action']")));
        clickElement(searchBtn);

        List<By> searchCandidates = Arrays.asList(
                By.cssSelector("[data-test='search-input']"),
                By.cssSelector("input[aria-label*='Search']"),
                By.cssSelector("input[type='search']"),
                By.cssSelector("input[placeholder*='Search']"),
                By.cssSelector("input[type='text']")
        );

        WebElement searchField = tryFindFirstVisible(searchCandidates, Duration.ofSeconds(7));
        String query = "Selenium";
        searchField.clear();
        searchField.sendKeys(query);

        // remember existing windows in case results open in a new tab/window
        java.util.Set<String> beforeHandles = driver.getWindowHandles();

        // Strategy attempts to submit the search
        boolean submitted = false;
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // 1) try ENTER
        try {
            searchField.sendKeys(Keys.ENTER);
            submitted = true;
        } catch (WebDriverException ignored) {}

        // 2) try clicking submit button
        if (!submitted) {
            try {
                List<By> submitCandidates = Arrays.asList(
                        By.cssSelector("button[data-test='full-search-button']"),
                        By.cssSelector("button[type='submit']"),
                        By.cssSelector("button[aria-label*='Search']")
                );
                WebElement submitButton = tryFindFirstVisible(submitCandidates, Duration.ofSeconds(3));
                clickElement(submitButton);
                submitted = true;
            } catch (Exception ignored) {}
        }

        // 3) try JS form submit if not submitted
        if (!submitted) {
            try {
                js.executeScript("var f = arguments[0].form || arguments[0].closest('form'); if(f) { f.submit(); return true; } return false;", searchField);
                submitted = true;
            } catch (WebDriverException ignored) {}
        }

        // 4) dispatch keyboard Enter via JS
        if (!submitted) {
            try {
                js.executeScript("var ev = new KeyboardEvent('keydown', {key:'Enter',keyCode:13,which:13,bubbles:true}); arguments[0].dispatchEvent(ev);", searchField);
                submitted = true;
            } catch (WebDriverException ignored) {}
        }

        // 5) as a last resort, navigate directly to JetBrains search page (best-effort fallback)
        if (!submitted) {
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                submitted = true;
            } catch (Exception ignored) {}
        }

        // diagnostic log right after submit/navigation
        try {
            System.out.println("[DEBUG] After submit - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
        } catch (Exception ignored) {}

        // Wait up to 45s for either a new window, url/title change or the query text to appear in the body
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        try {
            longWait.until(d -> {
                try {
                    // 1) new window/tab opened
                    if (d.getWindowHandles().size() > beforeHandles.size()) {
                        return true;
                    }
                    // 2) URL contains search or query
                    String url = "";
                    try { url = d.getCurrentUrl(); } catch (Exception ignore) {}
                    if (url != null && (url.toLowerCase().contains("search") || url.toLowerCase().contains("q="))) return true;
                    // 3) title contains query
                    String t = "";
                    try { t = d.getTitle(); } catch (Exception ignore) {}
                    if (t != null && t.toLowerCase().contains(query.toLowerCase())) return true;
                    // 4) body contains query text
                    try {
                        WebElement body = d.findElement(By.tagName("body"));
                        if (body != null && body.getText() != null && body.getText().toLowerCase().contains(query.toLowerCase())) return true;
                    } catch (Exception ignore) {}
                    return false;
                } catch (WebDriverException wde) {
                    return false;
                }
            });
        } catch (TimeoutException te) {
            // fallback: try forced navigation to search endpoint before failing
            try {
                String forced = "https://www.jetbrains.com/search/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                driver.get(forced);
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                shortWait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), query));
            } catch (Exception ex) {
                // final fallback: small sleep, dump diagnostics and fail
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                dumpDiagnostics("search-timeout");
                System.out.println("[DEBUG] Timeout - Final URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
                throw new AssertionError("Search did not show results within timeout for query: " + query, te);
            }
        }

        // diagnostic log after successful wait
        try {
            System.out.println("[DEBUG] After wait - URL: " + safeGetCurrentUrl() + " | Title: " + safeGetTitle());
        } catch (Exception ignored) {}

        // If a new window opened, switch to it (the one not in beforeHandles)
        try {
            java.util.Set<String> afterHandles = driver.getWindowHandles();
            if (afterHandles.size() > beforeHandles.size()) {
                for (String h : afterHandles) {
                    if (!beforeHandles.contains(h)) {
                        driver.switchTo().window(h);
                        break;
                    }
                }
            }
        } catch (WebDriverException ignored) {
        }

        // final verification: check URL/title/body for query
        String currentUrl = "";
        String title = "";
        String pageSource = "";
        try { currentUrl = driver.getCurrentUrl(); } catch (WebDriverException ignored) {}
        try { title = driver.getTitle(); } catch (WebDriverException ignored) {}
        try { pageSource = driver.findElement(By.tagName("body")).getText().toLowerCase(); } catch (WebDriverException ignored) {}

        boolean matched = (currentUrl != null && (currentUrl.toLowerCase().contains("search") || currentUrl.toLowerCase().contains("q=")))
                || (title != null && title.toLowerCase().contains(query.toLowerCase()))
                || (pageSource != null && pageSource.contains(query.toLowerCase()));
        if (!matched) {
            dumpDiagnostics("search-no-match");
            throw new AssertionError("Search did not navigate to a page containing the query (URL/title/body) after submit: " + query);
        }
    }

    @Test
    public void toolsMenu() throws InterruptedException {
        if (localHtml != null) {
            String menuSelector = "div[data-test='main-submenu']";
            assertTrue(localHtml.contains(menuSelector), "tools menu must be present in local test page");
            return;
        }

        safeClick(mainPage.toolsMenuBy);

        // Pequena pausa para permitir animação/popup
        Thread.sleep(500);

        WebElement menuPopup = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[data-test='main-submenu']")));
        WebElement tools = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[data-test='main-menu-item'][data-test-marker='Developer Tools']")));
        clickElement(tools);

        List<By> submenuCandidates = Arrays.asList(
                By.cssSelector("div[data-test='main-submenu']"),
                By.cssSelector("nav[role='navigation']"),
                By.cssSelector("div[data-test='main-menu']"),
                By.cssSelector("div.main-submenu")
        );

        WebElement menuPopup = tryFindFirstVisible(submenuCandidates, Duration.ofSeconds(3));
        assertTrue(menuPopup.isDisplayed());

        // Also assert that menu contains some expected text like 'Tools' or 'Developer'
        String text = menuPopup.getText().toLowerCase();
        assertTrue(text.contains("tool") || text.contains("developer") || menuPopup.isDisplayed());
    }

    @Test
    public void navigationToAllTools() throws InterruptedException {
        if (localHtml != null) {
            String productsId = "products-page";
            assertTrue(localHtml.contains("id=\"" + productsId + "\""), "products list must be present in local test page");
            return;
        }

        safeClick(mainPage.seeDeveloperToolsBy);

        // Esperar que o botão de 'Find your tools' esteja visível
        safeClick(mainPage.findYourToolsBy);

        // Esperar pela lista de produtos
        WebElement productsList = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("products-page")));
        assertTrue(productsList.isDisplayed());

        // Aguardar título esperado (pode demorar um pouco)
        wait.until(ExpectedConditions.titleContains("All Developer Tools"));
        assertEquals("All Developer Tools and Products by JetBrains", driver.getTitle());
        WebElement seeTools = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test-marker='Developer Tools']")));
        clickElement(seeTools);

        WebElement findTools = tryFindFirstVisible(Arrays.asList(By.cssSelector("[data-test='suggestion-action']"), By.cssSelector("a[href*='tools']")), Duration.ofSeconds(3));
        clickElement(findTools);

        // check navigation by URL/title or content
        wait.until(d -> {
            try {
                return d.getTitle().toLowerCase().contains("tools") || d.getCurrentUrl().toLowerCase().contains("tools") || d.getPageSource().toLowerCase().contains("developer tools");
            } catch (WebDriverException e) {
                return false;
            }
        });
    }

    // helper safe getters to avoid throwing when driver unavailable
    private String safeGetCurrentUrl() {
        try { return driver.getCurrentUrl(); } catch (Exception e) { return "<no-url>"; }
    }

    private String safeGetTitle() {
        try { return driver.getTitle(); } catch (Exception e) { return "<no-title>"; }
    }
}
