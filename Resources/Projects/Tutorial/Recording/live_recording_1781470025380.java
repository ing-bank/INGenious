import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.util.*;

public class Example {
  public static void main(String[] args) {
    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
        .setHeadless(false));
      BrowserContext context = browser.newContext();
      Page page = context.newPage();
      page.navigate("https://www.ing.nl/en/personal/mortgage/calculate-mortgage/based-on-income?flow-step=1-my-situation-step");
      page.getByTestId("accept").click();
      page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Alone")).check();
      page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your age")).click();
      page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your age")).fill("36");
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Within 3 to 6 months")).check();
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      page.getByLabel("Your plans, Energy Label").selectOption("ENERGY_LABEL_A_4PLUS");
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Employment")).check();
      page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Permanent contract")).check();
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("What is your gross yearly")).click();
      page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("What is your gross yearly")).fill("100000");
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("No")).check();
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("My maximum mortgage"))).isVisible();
      assertThat(page.locator("ing-feat-sc-house-result-card-based-on-income")).containsText("€513,811");
    }
  }
}